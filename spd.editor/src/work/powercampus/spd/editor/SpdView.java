package work.powercampus.spd.editor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.part.ViewPart;

/**
 * SPD エディタのビュー（SPD → SPD エディタを開く、またはツールバーのボタン）。
 * ファイルを作らずに使える。ソースコードとの受け渡しは画面の Import / Export で行い、
 * ファイルに残したいときだけ「保存…」（Ctrl+S）で名前を付けて .spd に保存する。
 * 内容はワークスペースの .metadata に自動で保管し、Eclipse を再起動しても前回の続きから使える。
 */
public class SpdView extends ViewPart {

    public static final String ID = "work.powercampus.spd.editor.SpdView";

    /** 自動保管先（ワークスペース/.metadata/.plugins/work.powercampus.spd.editor/） */
    private static final String STATE_FILE = "spd-view.json";
    /** 編集が止まってから自動保管するまでの時間（ミリ秒） */
    private static final int PERSIST_DELAY = 1000;

    private SpdBrowser spd;
    private boolean persistScheduled = false;

    @Override
    public void createPartControl(Composite parent) {
        spd = new SpdBrowser(parent, SpdBrowser.MODE_VIEW, new SpdBrowser.Host() {
            @Override
            public String initialText() {
                return readState();
            }

            @Override
            public void changed() {
                schedulePersist();
            }

            @Override
            public void saveRequested() {
                saveAs();
            }
        });
    }

    // ---------------------------------------------------------------
    // 自動保管（Eclipse を閉じても内容を残す）
    // ---------------------------------------------------------------

    private void schedulePersist() {
        if (persistScheduled) {
            return;
        }
        persistScheduled = true;
        getSite().getShell().getDisplay().timerExec(PERSIST_DELAY, () -> {
            persistScheduled = false;
            persist();
        });
    }

    private void persist() {
        if (spd == null || spd.isDisposed()) {
            return;
        }
        String text = spd.getText();
        if (text == null) {
            return;
        }
        try {
            Files.writeString(stateFile(), text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Activator.log(e);
        }
    }

    private static String readState() {
        Path file = stateFile();
        if (!Files.exists(file)) {
            return "";
        }
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Activator.log(e);
            return "";
        }
    }

    private static Path stateFile() {
        return Activator.getDefault().getStateLocation().append(STATE_FILE).toFile().toPath();
    }

    /** Eclipse の終了時に呼ばれる（画面がまだ生きているうちに最新の内容を保管する） */
    @Override
    public void saveState(IMemento memento) {
        super.saveState(memento);
        persist();
    }

    // ---------------------------------------------------------------
    // 名前を付けて .spd に保存
    // ---------------------------------------------------------------

    private void saveAs() {
        String text = spd.getText();
        if (text == null) {
            return;
        }
        SaveAsDialog dialog = new SaveAsDialog(getSite().getShell());
        dialog.setTitle(Messages.SpdView_saveTitle);
        IFile suggested = suggestFile();
        if (suggested != null) {
            dialog.setOriginalFile(suggested);
        } else {
            dialog.setOriginalName("spd.spd");
        }
        dialog.create();
        dialog.setMessage(Messages.SpdView_saveMessage);
        if (dialog.open() != Window.OK || dialog.getResult() == null) {
            return;
        }
        IPath path = dialog.getResult();
        if (!"spd".equalsIgnoreCase(path.getFileExtension())) {
            path = path.addFileExtension("spd");
        }
        IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
        try {
            writeFile(file, text);
            getViewSite().getActionBars().getStatusLineManager()
                    .setMessage(NLS.bind(Messages.SpdView_saved, file.getFullPath()));
        } catch (CoreException e) {
            Activator.log(e);
            MessageDialog.openError(getSite().getShell(), Messages.SpdView_saveTitle,
                    NLS.bind(Messages.SpdView_saveFailed, e.getMessage()));
        }
    }

    /** 最後に使っていたエディタのファイルと同じフォルダー・同じ名前（例：Hello.java → Hello.spd）を提案する */
    private IFile suggestFile() {
        IEditorPart editor = getSite().getPage().getActiveEditor();
        if (editor == null || !(editor.getEditorInput() instanceof IFileEditorInput)) {
            return null;
        }
        IFile source = ((IFileEditorInput) editor.getEditorInput()).getFile();
        IPath name = source.getFullPath().removeFileExtension().addFileExtension("spd");
        return ResourcesPlugin.getWorkspace().getRoot().getFile(name);
    }

    private static void writeFile(IFile file, String text) throws CoreException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);   // .spd は UTF-8（plugin.xml のコンテンツタイプ）
        if (file.exists()) {
            file.setContents(new ByteArrayInputStream(bytes), true, true, null);
            return;
        }
        createFolders(file.getParent());
        file.create(new ByteArrayInputStream(bytes), true, null);
    }

    /** 保存ダイアログで存在しないフォルダーが指定された場合に作る */
    private static void createFolders(IContainer container) throws CoreException {
        if (container.exists() || !(container instanceof IFolder)) {
            return;
        }
        createFolders(container.getParent());
        ((IFolder) container).create(true, true, null);
    }

    // ---------------------------------------------------------------
    // その他
    // ---------------------------------------------------------------

    @Override
    public void setFocus() {
        if (spd != null) {
            spd.setFocus();
        }
    }
}
