package plugins.tprovoost.scripteditor.completion;

import javax.swing.text.JTextComponent;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.CompletionProvider;

public abstract class IcyAutoCompletion extends AutoCompletion
{

    public IcyAutoCompletion(CompletionProvider provider)
    {
        super(provider);
    }

    public boolean packageExists(String neededPackage)
    {
        return getTextComponent().getText().contains(neededPackage);
    }

    public abstract boolean classAlreadyImported(String neededClass);

    /**
     * Add an import into the textcomponent if necessary.
     * 
     * @param tc
     *        : the text component
     * @param neededClass
     *        : the class to be added
     * @param isClass
     *        : is is a class or a package ?
     * @return
     */
    public abstract String addImport(JTextComponent tc, String neededClass, boolean isClass);
}
