package plugins.tprovoost.scripteditor.main;

import icy.gui.frame.IcyFrameAdapter;
import icy.gui.frame.IcyFrameEvent;
import icy.gui.frame.progress.FailedAnnounceFrame;
import icy.plugin.abstract_.PluginActionable;
import icy.system.thread.ThreadUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import javax.script.ScriptEngineManager;

import plugins.tprovoost.scripteditor.gui.ScriptingEditor;
import plugins.tprovoost.scripteditor.gui.ScriptingPanel;

public class ScriptEditorPlugin extends PluginActionable
{

	private static ArrayList<ScriptingEditor> editors = new ArrayList<ScriptingEditor>();

	@Override
	public void run()
	{
		// String jv = SystemUtil.getJavaName();
		// if (jv.contains("OpenJDK"))
		// {
		// MessageDialog
		// .showDialog("This Plugin is only compatible with Sun version, not OpenJDK for now. Compatibility coming soon with v1.0.");
		// return;
		// }
		if (new ScriptEngineManager().getEngineFactories().isEmpty())
		{
			new FailedAnnounceFrame("No interpreter found. Impossible to compile/run a script.");
			System.out.println("No interpreter found. Impossible to compile/run a script.");
			return;
		}
		final ScriptingEditor frame = new ScriptingEditor();
		frame.setSize(500, 500);
		frame.addToMainDesktopPane();
		frame.setVisible(true);
		frame.requestFocus();

		editors.add(frame);
		frame.addFrameListener(new IcyFrameAdapter()
		{
			@Override
			public void icyFrameClosed(IcyFrameEvent e)
			{
				frame.removeFrameListener(this);
				editors.remove(frame);
			}
		});
	}

	// @BindingFunction(value = "displayHelloString")
	// public static void displayHello(String h)
	// {
	// System.out.println("Hello " + h + "!");
	// }
	//
	// @BindingFunction(value = "displayHelloInt")
	// public void displayHello(Object a)
	// {
	// System.out.println("Hello " + a + "!");
	// }
	//
	// @BindingFunction(value = "getThisSEP")
	// public ScriptEditorPlugin getThis(String lol)
	// {
	// return this;
	// }
	//
	// @BindingFunction(value = "fnTest")
	// public static int testFuncStatic()
	// {
	// return 42;
	// }

	/**
	 * Creates a new tab from a file.
	 * 
	 * @param f
	 * @throws IOException
	 */
	public static void openInScriptEditor(final File f) throws IOException
	{
		// TODO gen text
		BufferedReader reader = new BufferedReader(new FileReader(f));
		String text = "";
		String line;
		while ((line = reader.readLine()) != null)
		{
			text += (line + "\n");
		}
		reader.close();
		openInScriptEditor(text, f.getName());
	}

	/**
	 * Creates a new tab containing the <code>text</code>, with no title.
	 * 
	 * @param text
	 */
	public static void openInScriptEditor(final String text)
	{
		openInScriptEditor(text, "Untitled*");
	}

	/**
	 * Creates a new tab with the <code>title</code> and containing the
	 * <code>text</code>.
	 * 
	 * @param text
	 * @param title
	 */
	public static void openInScriptEditor(final String text, final String title)
	{
		if (!editors.isEmpty())
		{
			ThreadUtil.invokeLater(new Runnable()
			{

				@Override
				public void run()
				{
					ScriptingPanel panel = editors.get(0).createNewPane(title);
					panel.getTextArea().setText(text);
				}
			});

		} else
		{
			ThreadUtil.invokeLater(new Runnable()
			{

				@Override
				public void run()
				{
					new ScriptEditorPlugin().run();
					ScriptingPanel panel = editors.get(0).createNewPane(title);
					panel.getTextArea().setText(text);
				}
			});
		}
	}
}
