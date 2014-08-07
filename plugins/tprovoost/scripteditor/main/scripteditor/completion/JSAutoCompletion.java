package plugins.tprovoost.scripteditor.completion;

import icy.util.ClassUtil;

import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.CompletionProvider;

import plugins.tprovoost.scripteditor.completion.types.BasicJavaClassCompletion;
import plugins.tprovoost.scripteditor.completion.types.NewInstanceCompletion;
import plugins.tprovoost.scripteditor.completion.types.ScriptFunctionCompletion;

public class JSAutoCompletion extends IcyAutoCompletion
{

    public JSAutoCompletion(CompletionProvider provider)
    {
        super(provider);
    }

    @Override
    protected void insertCompletion(Completion c, boolean typedParamListStartChar)
    {

        if (c instanceof ScriptFunctionCompletion)
        {
            // get position before insertion
            // JTextComponent tc = getTextComponent();
            // ScriptFunctionCompletion sfc = ((ScriptFunctionCompletion) c);
            //
            // // get method added
            // Method m = sfc.getMethod();
            // if (m != null)
            // {
            // String neededClass = m.getDeclaringClass().getName();

            // test eventual needed importClasses
            // if (sfc.isStatic() && !classAlreadyImported(neededClass))
            // {
            // int caretPos = tc.getCaretPosition();
            //
            // // import the needed class + movement
            // caretPos += addImport(tc, neededClass, true).length();
            //
            // // put the caret in the right position
            // tc.getCaret().setDot(caretPos);
            // }
            // }
        }
        else if (c instanceof BasicJavaClassCompletion)
        {
            // get the current text
            JTextComponent tc = getTextComponent();

            // get the caret position
            int caretPos = tc.getCaretPosition();
            Class<?> clazz = ((BasicJavaClassCompletion) c).getJavaClass();
            String neededClass = clazz.getName();

            if (clazz.isMemberClass())
            {
                neededClass = ClassUtil.getBaseClassName(neededClass);
            }
            if (!classAlreadyImported(neededClass))
            {
                // import the needed class + movement
                caretPos += addImport(tc, neededClass, true).length();

                // put the caret in the right position
                tc.getCaret().setDot(caretPos);
            }
        }
        else if (c instanceof NewInstanceCompletion)
        {
            // get the current text
            JTextComponent tc = getTextComponent();

            // get the caret position
            int caretPos = tc.getCaretPosition();

            // class to import
            Class<?> clazz = ((NewInstanceCompletion) c).getConstructor().getDeclaringClass();
            String neededClass = clazz.getName();

            if (!classAlreadyImported(neededClass))
            {
                // import the needed class + movement
                caretPos += addImport(tc, neededClass, true).length();

                // put the caret in the right position
                tc.getCaret().setDot(caretPos);
            }
        }
        // do insertion
        super.insertCompletion(c, typedParamListStartChar);
    }

    @Override
    protected String getReplacementText(Completion c, Document doc, int start, int len)
    {
        String toReturn = super.getReplacementText(c, doc, start, len);
        if (c instanceof ScriptFunctionCompletion)
        {
            ScriptFunctionCompletion fc = (ScriptFunctionCompletion) c;
            String textBefore = "";
            if (!fc.isStatic())
            {
                CompletionProvider provider = getCompletionProvider();
                if (provider instanceof IcyCompletionProvider)
                {
                    textBefore = provider.getAlreadyEnteredText(getTextComponent());
                    int lastIdx = textBefore.lastIndexOf('.');
                    if (lastIdx != -1)
                        textBefore = textBefore.substring(0, lastIdx + 1);
                    else
                        textBefore = "";
                }
                toReturn = textBefore + fc.getMethod().getName();
            }
        }
        else if (c instanceof BasicJavaClassCompletion)
        {
            Class<?> clazz = ((BasicJavaClassCompletion) c).getJavaClass();

            String textBefore = "";
            CompletionProvider provider = getCompletionProvider();
            if (provider instanceof IcyCompletionProvider)
            {
                textBefore = ((IcyCompletionProvider) provider).getAlreadyEnteredTextWithFunc(getTextComponent());
                int lastIdx = textBefore.lastIndexOf('.');
                if (lastIdx != -1)
                    textBefore = textBefore.substring(0, lastIdx);
                else
                    textBefore = "";
            }
            if (textBefore == "")
            {
                toReturn = clazz.getName();
                toReturn = ClassUtil.getSimpleClassName(toReturn).replace('$', '.');
            }
            else
            {
                toReturn = clazz.getSimpleName();
            }
        }
        return toReturn;
    }

    public String addImport(JTextComponent tc, String neededClass, boolean isClass)
    {
        String resultingImport;
        if (isClass)
            resultingImport = "importClass(Packages." + neededClass + ")\n";
        else
            resultingImport = "importPackage(Packages." + ClassUtil.getPackageName(neededClass) + ")\n";

        if (!tc.getText().contains(resultingImport))
        {
            // FIXME: add after the last import insteand of beginning of file.
            tc.setText(resultingImport + tc.getText());
        }
        return resultingImport;
    }

    public String addBaseClass(JTextComponent tc, String neededBaseClass)
    {
        return neededBaseClass;
    }

    @Override
    public boolean classAlreadyImported(String neededClass)
    {
        String text = getTextComponent().getText();

        // test if contains the class or if contains the importPackage enclosing
        // the class
        if (neededClass.startsWith("java.") || neededClass.startsWith("javax."))
            return text.contains("importClass(" + neededClass + ")")
                    || text.contains("importPackage(" + ClassUtil.getPackageName(neededClass) + ")");
        return text.contains("importClass(Packages." + neededClass + ")")
                || text.contains("importPackage(Packages." + ClassUtil.getPackageName(neededClass) + ")");
    }

}
