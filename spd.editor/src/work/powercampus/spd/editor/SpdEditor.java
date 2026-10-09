package work.powercampus.spd.editor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

/**
 * SPDエディタ。
 * 画面は web/editor.html（既存のHTML/JavaScript製エディタ）を
 * SWT の Browser ウィジェットに表示し、Java 側はファイルの読み書きと
 * Eclipse との連携（保存・ダーティフラグ）だけを担当する。
 *
 * Java と JavaScript の約束事（詳細は CLAUDE.md）:
 *   Java → JS : loadSpd(text) / getSpdText()
 *   JS → Java : spdGetInitialText() / spdNotifyChanged() / spdRequestSave()
 */
public class SpdEditor extends EditorPart {

    public static final String ID = "work.powercampus.spd.editor.SpdEditor";
    private static final String HTML_PATH = "web/editor.html";

    private Browser browser;
    private String initialText = "";
    private boolean dirty = false;

    // ---------------------------------------------------------------
    // 初期化
    // ---------------------------------------------------------------

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        if (!(input instanceof IFileEditorInput)) {
            throw new PartInitException("SPDエディタはワークスペース内のファイルのみ開けます");
        }
        setSite(site);
        setInput(input);
        setPartName(input.getName());
        initialText = readFile(((IFileEditorInput) input).getFile());
    }

    @Override
    public void createPartControl(Composite parent) {
        // Windows では Edge(WebView2) を明示的に使う
        int style = Platform.OS_WIN32.equals(Platform.getOS()) ? SWT.EDGE : SWT.NONE;
        browser = new Browser(parent, style);

        registerBrowserFunctions();

        // 読み込み完了後、Java からファイル内容を渡す
        browser.addProgressListener(ProgressListener.completedAdapter(e ->
                browser.execute("if (typeof loadSpd === 'function') { loadSpd(spdGetInitialText()); }")));

        browser.setUrl(resolveHtmlUrl());
    }

    /**
     * JS から呼び出せる Java 関数を登録する。
     * Edge(WebView2) では、JS から呼ばれた関数の処理中は JS 側が止まっているため、
     * その中で browser.evaluate() を呼ぶと null が返る。保存（getSpdText() の取得）や
     * 画面更新は asyncExec で JS 呼び出しが終わってから行う。
     */
    private void registerBrowserFunctions() {
        // ファイル内容の受け渡し（文字列のエスケープ処理を避けるため、JS 側から取りに来させる）
        new BrowserFunction(browser, "spdGetInitialText") {
            @Override
            public Object function(Object[] args) {
                return initialText;
            }
        };
        // 編集されたことの通知
        new BrowserFunction(browser, "spdNotifyChanged") {
            @Override
            public Object function(Object[] args) {
                runAfterCallback(() -> setDirty(true));
                return null;
            }
        };
        // Ctrl+S・「保存」ボタンをブラウザ側が受け取った場合の保存依頼
        new BrowserFunction(browser, "spdRequestSave") {
            @Override
            public Object function(Object[] args) {
                runAfterCallback(() -> getSite().getPage().saveEditor(SpdEditor.this, false));
                return null;
            }
        };
    }

    /** JS からの呼び出しが終わってから UI スレッドで実行する（エディタが閉じられていたら何もしない） */
    private void runAfterCallback(Runnable r) {
        browser.getDisplay().asyncExec(() -> {
            if (!browser.isDisposed()) {
                r.run();
            }
        });
    }

    private String resolveHtmlUrl() {
        try {
            URL entry = Activator.getDefault().getBundle().getEntry(HTML_PATH);
            // Eclipse-BundleShape: dir により、同じフォルダの CSS/JS も参照できる
            // ?host=eclipse で JS 側に Eclipse 内で動いていることを知らせる（自動保存・離脱確認を止める）
            return FileLocator.toFileURL(entry).toString() + "?host=eclipse";
        } catch (IOException e) {
            log(e);
            return "about:blank";
        }
    }

    // ---------------------------------------------------------------
    // 保存
    // ---------------------------------------------------------------

    @Override
    public void doSave(IProgressMonitor monitor) {
        Object result = browser.evaluate("return getSpdText();");
        if (!(result instanceof String)) {
            log(new IllegalStateException("getSpdText() が文字列を返しませんでした"));
            return;
        }
        String text = (String) result;
        IFile file = ((IFileEditorInput) getEditorInput()).getFile();
        try {
            byte[] bytes = text.getBytes(file.getCharset());
            file.setContents(new ByteArrayInputStream(bytes), true, true, monitor);
            initialText = text;
            setDirty(false);
        } catch (CoreException | IOException e) {
            log(e);
        }
    }

    @Override
    public void doSaveAs() {
        // TODO: 名前を付けて保存（SaveAsDialog を使う）
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
        if (browser != null && !browser.isDisposed()) {
            browser.setFocus();
        }
    }

    private static String readFile(IFile file) throws PartInitException {
        try (InputStream in = file.getContents()) {
            return new String(in.readAllBytes(), file.getCharset());
        } catch (CoreException | IOException e) {
            throw new PartInitException("ファイルを読み込めません: " + file.getName(), e);
        }
    }

    private static void log(Throwable t) {
        Activator.getDefault().getLog().log(
                new Status(IStatus.ERROR, Activator.PLUGIN_ID, t.getMessage(), t));
    }
}
