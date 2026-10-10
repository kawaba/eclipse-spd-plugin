package work.powercampus.spd.editor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

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

            @Override
            public void saveRequested() {
                getSite().getPage().saveEditor(SpdEditor.this, false);
            }
        });
    }

    // ---------------------------------------------------------------
    // 保存
    // ---------------------------------------------------------------

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
        } catch (CoreException | IOException e) {
            Activator.log(e);
        }
    }

    @Override
    public void doSaveAs() {
        // TODO: 名前を付けて保存（SaveAsDialog を使う。SpdView.saveAs を参考に）
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
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
