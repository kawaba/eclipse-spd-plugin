package work.powercampus.spd.editor;

import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.widgets.Composite;

/**
 * web/editor.html（既存のHTML/JavaScript製SPDエディタ）を表示する Browser と、
 * Java と JavaScript の約束事（詳細は CLAUDE.md）の Java 側。SpdEditor と SpdView で共用する。
 *
 *   Java → JS : loadSpd(text) / getSpdText()
 *   JS → Java : spdGetInitialText() / spdNotifyChanged() / spdRequestSave()
 */
class SpdBrowser {

    /** 画面の使い手（エディタ／ビュー）が実装する */
    interface Host {
        /** 最初に画面へ読み込む内容 */
        String initialText();

        /** 画面で編集された */
        void changed();

        /** 画面で Ctrl+S・「保存」ボタンが押された */
        void saveRequested();
    }

    /** JS 側に渡す画面の種類（editor.html の ECLIPSE_MODE） */
    static final String MODE_EDITOR = "editor";
    static final String MODE_VIEW = "view";

    private static final String HTML_PATH = "web/editor.html";

    private final Browser browser;

    SpdBrowser(Composite parent, String mode, Host host) {
        // Windows では Edge(WebView2) を明示的に使う
        int style = Platform.OS_WIN32.equals(Platform.getOS()) ? SWT.EDGE : SWT.NONE;
        browser = new Browser(parent, style);
        registerBrowserFunctions(host);

        // 読み込み完了後、Java から初期内容を渡す
        browser.addProgressListener(ProgressListener.completedAdapter(e ->
                browser.execute("if (typeof loadSpd === 'function') { loadSpd(spdGetInitialText()); }")));

        browser.setUrl(resolveHtmlUrl(mode));
    }

    /**
     * JS から呼び出せる Java 関数を登録する。
     * Edge(WebView2) では、JS から呼ばれた関数の処理中は JS 側が止まっているため、
     * その中で browser.evaluate() を呼ぶと null が返る。保存（getSpdText() の取得）や
     * 画面更新は asyncExec で JS 呼び出しが終わってから行う。
     */
    private void registerBrowserFunctions(Host host) {
        // 初期内容の受け渡し（文字列のエスケープ処理を避けるため、JS 側から取りに来させる）
        new BrowserFunction(browser, "spdGetInitialText") {
            @Override
            public Object function(Object[] args) {
                return host.initialText();
            }
        };
        // 編集されたことの通知
        new BrowserFunction(browser, "spdNotifyChanged") {
            @Override
            public Object function(Object[] args) {
                runAfterCallback(host::changed);
                return null;
            }
        };
        // Ctrl+S・「保存」ボタンをブラウザ側が受け取った場合の保存依頼
        new BrowserFunction(browser, "spdRequestSave") {
            @Override
            public Object function(Object[] args) {
                runAfterCallback(host::saveRequested);
                return null;
            }
        };
    }

    /** JS からの呼び出しが終わってから UI スレッドで実行する（画面が閉じられていたら何もしない） */
    private void runAfterCallback(Runnable r) {
        browser.getDisplay().asyncExec(() -> {
            if (!browser.isDisposed()) {
                r.run();
            }
        });
    }

    private static String resolveHtmlUrl(String mode) {
        try {
            URL entry = Activator.getDefault().getBundle().getEntry(HTML_PATH);
            // Eclipse-BundleShape: dir により、同じフォルダの CSS/JS も参照できる
            // ?host=eclipse で JS 側に Eclipse 内で動いていることを知らせる（自動保存・離脱確認を止める）
            return FileLocator.toFileURL(entry).toString() + "?host=eclipse&mode=" + mode;
        } catch (IOException e) {
            Activator.log(e);
            return "about:blank";
        }
    }

    /**
     * 現在の SPD テキスト（JSON）を返す。取得できなければ null。
     * BrowserFunction の処理中には呼ばないこと（Edge では null になる）。
     */
    String getText() {
        if (browser.isDisposed()) {
            return null;
        }
        Object result = browser.evaluate("return getSpdText();");
        if (!(result instanceof String)) {
            Activator.log(new IllegalStateException("getSpdText() が文字列を返しませんでした"));
            return null;
        }
        return (String) result;
    }

    boolean isDisposed() {
        return browser.isDisposed();
    }

    void setFocus() {
        if (!browser.isDisposed()) {
            browser.setFocus();
        }
    }
}
