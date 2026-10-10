package work.powercampus.spd.editor;

import org.eclipse.osgi.util.NLS;

/**
 * Java 側の画面の文言。英語は messages.properties、日本語は messages_ja.properties に書き、
 * Eclipse の表示言語（Pleiades で日本語化した Eclipse は ja）で切り替わる。
 * {0} などの差し込みは NLS.bind で行う。文言に ' を使わない（NLS.bind では特別な意味になる）。
 */
public final class Messages extends NLS {

    private static final String BUNDLE_NAME = "work.powercampus.spd.editor.messages";

    public static String SpdView_saveTitle;
    public static String SpdView_saveMessage;
    public static String SpdView_saved;
    public static String SpdView_saveFailed;

    public static String SpdEditor_workspaceOnly;
    public static String SpdEditor_cannotRead;

    public static String SpdNewFileWizard_windowTitle;
    public static String SpdNewFileWizard_pageTitle;
    public static String SpdNewFileWizard_pageDescription;
    public static String SpdNewFileWizard_lang;

    public static String OpenFontFolder_failed;
    public static String OpenFontFolder_openDirectly;

    static {
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
