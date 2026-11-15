package work.powercampus.spd.editor;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * 設定ページ「ウィンドウ → 設定 → SPD」。SPD エディタの利用モード（元の仕様書 §6.14 LG-13・v1.29.0）を選ぶ。
 *   default ＝ Import と入力欄への貼り付けを使えない（既定）
 *   import  ＝ 使える
 *   exam    ＝ 試験
 * 選んだモードは SpdBrowser が web/editor.html の URL（&import・&exam）で渡す。画面を開くときに渡すので、
 * 開いている SPD エディタ・ビューには効かない（閉じて開き直す）。
 * 設定はワークスペースごと（インスタンス・スコープ）に保存される。
 */
public class SpdPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    /** 設定のキーと値（値は web/editor.html の SPD_MODES と揃える） */
    static final String KEY_MODE = "mode";
    static final String MODE_DEFAULT = "default";
    static final String MODE_IMPORT = "import";
    static final String MODE_EXAM = "exam";

    public SpdPreferencePage() {
        super(GRID);
    }

    @Override
    public void init(IWorkbench workbench) {
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
        setDescription(Messages.SpdPreferencePage_description);
    }

    @Override
    protected void createFieldEditors() {
        addField(new RadioGroupFieldEditor(KEY_MODE, Messages.SpdPreferencePage_mode, 1, new String[][] {
                { Messages.SpdPreferencePage_modeDefault, MODE_DEFAULT },
                { Messages.SpdPreferencePage_modeImport, MODE_IMPORT },
                { Messages.SpdPreferencePage_modeExam, MODE_EXAM },
        }, getFieldEditorParent(), true));
    }

    /** 既定値を設定する（Activator.start から呼ぶ） */
    static void initDefaults(IPreferenceStore store) {
        store.setDefault(KEY_MODE, MODE_DEFAULT);
    }

    /** 今の利用モード。知らない値なら default */
    static String mode() {
        String m = Activator.getDefault().getPreferenceStore().getString(KEY_MODE);
        return MODE_IMPORT.equals(m) || MODE_EXAM.equals(m) ? m : MODE_DEFAULT;
    }
}
