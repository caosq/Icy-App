package plugins.tprovoost.scripteditor.completion.types;

import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.CompletionProvider;

public class PythonModuleCompletion extends BasicCompletion
{

	public PythonModuleCompletion(CompletionProvider provider, String replacementText)
	{
		super(provider, replacementText);
	}

}
