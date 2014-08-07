package plugins.tprovoost.scripteditor.completion.types;

import java.util.HashMap;

import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.FunctionCompletion;

public class JavaFunctionCompletion extends FunctionCompletion
{
    protected static HashMap<String, String> cacheSummary = new HashMap<String, String>();
    protected String summary;
    protected boolean isParseDone;

    public JavaFunctionCompletion(CompletionProvider provider, String name, String returnType)
    {
        super(provider, name, returnType);
    }

    public boolean isPopulateDone()
    {
        return isParseDone;
    }

    @Override
    public String getShortDescription()
    {
        if (summary != null)
            return summary;
        return super.getShortDescription();
    }

}
