package work.powercampus.spd.editor;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/** プラグインのライフサイクル管理 */
public class Activator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "work.powercampus.spd.editor";

    private static Activator plugin;

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    public static Activator getDefault() {
        return plugin;
    }

    /** エラーログ（ウィンドウ → ビューの表示 → エラー・ログ）へ書く */
    static void log(Throwable t) {
        plugin.getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, t.getMessage(), t));
    }
}
