package work.powercampus.spd.editor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;

/**
 * SPDエディタ（.spd ファイルを開いたときのエディタ）。
 * 画面は SpdBrowser（web/editor.html）に任せ、Java 側はファイルの読み書きと
 * Eclipse との連携（保存・ダーティフラグ）だけを担当する。
 * ファイルを作らずに使う画面は SpdView。
 */
public class SpdEditor extends EditorPart {

    public static final String ID = "work.powercampus.spd.editor.SpdEditor";

    private SpdBrowser spd;
    private String initialText = "";
    private boolean dirty = false;

    // ---------------------------------------------------------------
    // 初期化
    // ---------------------------------------------------------------

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        if (!(input instanceof IFileEditorInput)) {
            throw new PartInitException(Messages.SpdEditor_workspaceOnly);
        }
        setSite(site);
        setInput(input);
        setPartName(input.getName());
        initialText = readFile(((IFileEditorInput) input).getFile());
    }

    @Override
    public void createPartControl(Composite parent) {
        spd = new SpdBrowser(parent, SpdBrowser.MODE_EDITOR, new SpdBrowser.Host() {
            @Override
            public String initialText() {
                return initialText;
            }

            @Override
            public void changed() {
                setDirty(true);
            }

            /** 画面の「保存」ボタン・画面での Ctrl+S は、名前を付けて保存（今のファイル名を入れて開く） */
            @Override
            public void saveRequested() {
                doSaveAs();
            }
        });
    }

    // ---------------------------------------------------------------
    // 保存
    // ---------------------------------------------------------------

    /** 上書き保存（Eclipse のメニュー「ファイル → 保存」、画面の外での Ctrl+S、閉じるときの保存の確認） */
    @Override
    public void doSave(IProgressMonitor monitor) {
        String text = spd.getText();
        if (text == null) {
            return;
        }
        IFile file = ((IFileEditorInput) getEditorInput()).getFile();
        try {
            byte[] bytes = text.getBytes(file.getCharset());
            file.setContents(new ByteArrayInputStream(bytes), true, true, monitor);
            initialText = text;
            setDirty(false);
            showSaved(file);
        } catch (CoreException | IOException e) {
            Activator.log(e);
            showSaveFailed(e);
        }
    }

    /**
     * 名前を付けて保存。今のファイルのフォルダー・名前を入れたダイアログを開く。
     * 同じ名前のままなら上書き（ダイアログが置き換えの確認を出す）、別の名前ならそのファイルに保存して、エディタもそちらに切り替える。
     */
    @Override
    public void doSaveAs() {
        String text = spd.getText();
        if (text == null) {
            return;
        }
        IFile current = ((IFileEditorInput) getEditorInput()).getFile();
        SaveAsDialog dialog = new SaveAsDialog(getSite().getShell());
        dialog.setTitle(Messages.SpdView_saveTitle);
        dialog.setOriginalFile(current);
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
            SpdView.writeFile(file, text);
            SpdView.rememberLastSaved(file);
        } catch (CoreException e) {
            Activator.log(e);
            showSaveFailed(e);
            return;
        }
        if (!file.equals(current)) {
            setInput(new FileEditorInput(file));
            setPartName(file.getName());
            setTitleToolTip(file.getFullPath().makeRelative().toString());
        }
        initialText = text;
        setDirty(false);
        showSaved(file);
    }

    @Override
    public boolean isSaveAsAllowed() {
        return true;
    }

    private void showSaved(IFile file) {
        getEditorSite().getActionBars().getStatusLineManager()
                .setMessage(NLS.bind(Messages.SpdView_saved, file.getFullPath()));
    }

    private void showSaveFailed(Exception e) {
        MessageDialog.openError(getSite().getShell(), Messages.SpdView_saveTitle,
                NLS.bind(Messages.SpdView_saveFailed, e.getMessage()));
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    private void setDirty(boolean value) {
        if (dirty != value) {
            dirty = value;
            firePropertyChange(PROP_DIRTY);
        }
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

    private static String readFile(IFile file) throws PartInitException {
        try (InputStream in = file.getContents()) {
            return new String(in.readAllBytes(), file.getCharset());
        } catch (CoreException | IOException e) {
            throw new PartInitException(NLS.bind(Messages.SpdEditor_cannotRead, file.getName()), e);
        }
    }
}
