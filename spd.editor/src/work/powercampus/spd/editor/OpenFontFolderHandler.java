package work.powercampus.spd.editor;

import java.io.File;
import java.net.URL;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.program.Program;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * メニュー「SPD → フォントのフォルダを開く」。
 * プラグインに同梱した web/fonts/（PlemolJP HS の TTF・ライセンス・spd-font.epf・README.txt）を
 * エクスプローラー（mac は Finder）で開く。インストール済みのプラグインの中は利用者が探しにくいため。
 */
public class OpenFontFolderHandler extends AbstractHandler {

    private static final String FONT_DIR = "web/fonts/";

    @Override
    public Object execute(ExecutionEvent event) {
        String path = null;
        try {
            URL entry = Activator.getDefault().getBundle().getEntry(FONT_DIR);
            // toFileURL の URL は日本語・空白がエンコードされていないので、URIUtil で変換する
            File dir = URIUtil.toFile(URIUtil.toURI(FileLocator.toFileURL(entry)));
            path = dir.getAbsolutePath();
            if (dir.isDirectory() && Program.launch(path)) {
                return null;
            }
        } catch (Exception e) {
            Activator.log(e);
        }
        MessageDialog.openError(HandlerUtil.getActiveShell(event), "SPD",
                Messages.OpenFontFolder_failed
                + (path != null ? "\n" + NLS.bind(Messages.OpenFontFolder_openDirectly, path) : ""));
        return null;
    }
}
