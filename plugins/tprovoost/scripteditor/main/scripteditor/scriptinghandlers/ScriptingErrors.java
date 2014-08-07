package plugins.tprovoost.scripteditor.scriptinghandlers;

import icy.image.ImageUtil;
import icy.plugin.PluginLoader;
import icy.resource.icon.IcyIcon;

import java.util.ArrayList;

import javax.script.ScriptException;
import javax.swing.text.BadLocationException;

import org.fife.ui.rtextarea.Gutter;

/**
 * This class stores the errors and warnings found when parsing the files
 * and also the runtime error that may have occured when running the script.
 * 
 * @author tlecomte
 *
 */
public class ScriptingErrors {

	private static final IcyIcon ICON_ERROR_TOOLTIP = new IcyIcon(ImageUtil.load(PluginLoader
			.getResourceAsStream("plugins/tprovoost/scripteditor/resources/icons/quickfix_warning_obj.gif")), 16, false);

	private static final IcyIcon ICON_ERROR = new IcyIcon(ImageUtil.load(PluginLoader
			.getResourceAsStream("plugins/tprovoost/scripteditor/resources/icons/error.gif")), 15, false);
	
	/**
	 * {@link ArrayList} containing all the errors found when parsing the file.
	 */
	private final ArrayList<ScriptException> errors = new ArrayList<ScriptException>();
	/**
	 * {@link ArrayList} containing all the warnings found when parsing the file.
	 */
	private final ArrayList<ScriptException> warnings = new ArrayList<ScriptException>();
	/**
	 * The error that stopped the script, if any.
	 */
	private ScriptException runtimeError = null;
	
	public void displayOnGutter(Gutter gutter) {
		// The parser may have found multiple errors/warnings on each line
		// We walk the list to merge them.
		ArrayList<Integer> lines = new ArrayList<Integer>();
		ArrayList<Boolean> areWarning = new ArrayList<Boolean>();
		ArrayList<String> messages = new ArrayList<String>();
		
		if (runtimeError != null)
		{
			lines.add(runtimeError.getLineNumber());
			areWarning.add(false);
			
			// We will wrap the message in HTML to handle multiple errors on a single line
			// so first make sure we have nothing that could break HTML in the raw messages
			String normalizedMessage = runtimeError.getMessage().replaceAll("<", "").replaceAll(">", "");
			messages.add(normalizedMessage);
		}

		for (ScriptException see : errors)
		{
			// We will wrap the message in HTML to handle multiple errors on a single line
			// so first make sure we have nothing that could break HTML in the raw messages
			String normalizedMessage = see.getMessage().replaceAll("<", "").replaceAll(">", "");

			if (lines.contains(see.getLineNumber()))
			{
				// there was already an error on this line
				int index = lines.indexOf(see.getLineNumber());
				String message = messages.get(index) + "<br>" + normalizedMessage;
				messages.set(index, message);
				// if there is an error, make it an error, not a warning
				areWarning.set(index, false);
			}
			else
			{
				lines.add(see.getLineNumber());
				areWarning.add(false);
				messages.add(normalizedMessage);
			}
		}
		
		for (ScriptException see : warnings)
		{
			// We will wrap the message in HTML to handle multiple errors on a single line
			// so first make sure we have nothing that could break HTML in the raw messages
			String normalizedMessage = see.getMessage().replaceAll("<", "").replaceAll(">", "");

			if (lines.contains(see.getLineNumber()))
			{
				// there was already an error on this line
				int index = lines.indexOf(see.getLineNumber());
				String message = messages.get(index) + "<br>" + normalizedMessage;
				messages.set(index, message);
				// no need to change areWarning here
			}
			else
			{
				lines.add(see.getLineNumber());
				areWarning.add(false);
				messages.add(normalizedMessage);
			}
		}

		for (int i=0; i<lines.size(); i++)
		{
			IcyIcon icon;
			if (areWarning.get(i))
				icon = ICON_ERROR_TOOLTIP;
			else
				icon = ICON_ERROR;
			//wrap the tooltip in html to handle multi-lines
			String tooltip = "<html>" + messages.get(i) + "</html>";
			// if (tooltip.length() > 127)
			// {
			// tooltip = tooltip.substring(0, 127) + "...";
			// }

			// Warning! The Gutter displays lines starting from 1, BUT its
			// internal implementation is based on a JTextArea that expects
			// the line numbers to be counted from 0.
			int textAreaLineNumber = lines.get(i)-1;

			try
			{
				gutter.addLineTrackingIcon(textAreaLineNumber, icon, tooltip);
			} catch (BadLocationException e)
			{
				e.printStackTrace();
				System.out.println(tooltip);
			}

			gutter.repaint();
		}
	}

	public void setRuntimeError(ScriptException e) {
		runtimeError = e;
	}

	public void cleanup() {
		errors.clear();
		warnings.clear();
		runtimeError = null;
	}

	public void addWarning(ScriptException se) {
		warnings.add(se);
	}

	public void addError(ScriptException se) {
		errors.add(se);
	}
	
	
}
