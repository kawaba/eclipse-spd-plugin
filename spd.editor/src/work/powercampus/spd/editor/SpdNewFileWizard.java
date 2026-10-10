package work.powercampus.spd.editor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

/**
 * 新規 SPD ファイル作成ウィザード（ファイル → 新規 → その他 → SPD → SPD ファイル）。
 * 言語を選んで空の .spd（CLAUDE.md の「.spd ファイル形式」の JSON）を作り、SPD エディタで開く。
 */
public class SpdNewFileWizard extends BasicNewResourceWizard {

    /** 言語の値（web/editor.html の LANGS と揃える）と表示名 */
    static final String[] LANGS = { "java", "thymeleaf", "python", "c" };
    private static final String[] LANG_NAMES = { "Java", "Thymeleaf", "Python", "C" };
    private static final String DEFAULT_LANG = "java";

    /** 前回選んだ言語を覚えておく設定のキー */
    private static final String SETTINGS_SECTION = "SpdNewFileWizard";
    private static final String SETTINGS_LANG = "lang";

    private NewFilePage page;

    @Override
    public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
        super.init(workbench, currentSelection);
        setWindowTitle(Messages.SpdNewFileWizard_windowTitle);
        setNeedsProgressMonitor(true);
    }

    @Override
    public void addPages() {
        page = new NewFilePage(getSelection());
        addPage(page);
    }

    @Override
    public boolean performFinish() {
        IFile file = page.createNewFile();
        if (file == null) {
            return false;
        }
        saveLang(page.lang);
        selectAndReveal(file);
        IWorkbenchPage wbPage = getWorkbench().getActiveWorkbenchWindow().getActivePage();
        if (wbPage != null) {
            try {
                IDE.openEditor(wbPage, file, SpdEditor.ID);
            } catch (PartInitException e) {
                Activator.log(e);
            }
        }
        return true;
    }

    /** 新規 .spd の中身（空のグリッド）。JS の JSON.stringify(..., null, 2) と同じ書式にする */
    static String initialJson(String lang) {
        return "{\n  \"lang\": \"" + lang + "\",\n  \"data\": {}\n}";
    }

    private IDialogSettings settings() {
        IDialogSettings root = Activator.getDefault().getDialogSettings();
        IDialogSettings section = root.getSection(SETTINGS_SECTION);
        return section != null ? section : root.addNewSection(SETTINGS_SECTION);
    }

    private String loadLang() {
        String lang = settings().get(SETTINGS_LANG);
        for (String l : LANGS) {
            if (l.equals(lang)) {
                return lang;
            }
        }
        return DEFAULT_LANG;
    }

    private void saveLang(String lang) {
        settings().put(SETTINGS_LANG, lang);
    }

    /** 保存先・ファイル名の入力に、言語の選択を加えたページ */
    private class NewFilePage extends WizardNewFileCreationPage {

        String lang = loadLang();

        NewFilePage(IStructuredSelection selection) {
            super("newSpdFile", selection);
            setTitle(Messages.SpdNewFileWizard_pageTitle);
            setDescription(Messages.SpdNewFileWizard_pageDescription);
            setFileExtension("spd");
        }

        @Override
        public void createControl(Composite parent) {
            super.createControl(parent);
            Composite control = (Composite) getControl();

            Group group = new Group(control, SWT.NONE);
            group.setText(Messages.SpdNewFileWizard_lang);
            group.setLayout(new GridLayout(LANGS.length, false));
            group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
            for (int i = 0; i < LANGS.length; i++) {
                String value = LANGS[i];
                Button radio = new Button(group, SWT.RADIO);
                radio.setText(LANG_NAMES[i]);
                radio.setSelection(value.equals(lang));
                radio.addListener(SWT.Selection, e -> {
                    if (radio.getSelection()) {
                        lang = value;
                    }
                });
            }
            control.layout(true, true);
        }

        @Override
        protected InputStream getInitialContents() {
            return new ByteArrayInputStream(initialJson(lang).getBytes(StandardCharsets.UTF_8));
        }
    }
}
