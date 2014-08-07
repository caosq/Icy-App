package plugins.tprovoost.scripteditor.scriptinghandlers.js;

import icy.file.FileUtil;
import icy.plugin.PluginLoader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

import javax.script.ScriptException;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;

import plugins.tprovoost.scripteditor.scriptinghandlers.ScriptEngine;

public class JSScriptEngine extends ScriptEngine
{

	public ScriptableObject scriptable;
	public String lastFileName = "";
	
	// used to tell Rhino that it should count the lines starting from 1,
	// as in the GUI
	private static int LINE_NUMBER_START = 1;

	public JSScriptEngine()
	{
		Context context = Context.enter();
		context.setApplicationClassLoader(PluginLoader.getLoader());
		scriptable = new IcyImporterTopLevel(context);
		Context.exit();
	}

	@Override
	public void eval(String s) throws ScriptException
	{
		// uses Context from Rhino integrated in JRE or impossibility
		// to use already defined methods in ScriptEngine, such as println or
		// getImage
		Context context = Context.enter();
		context.setApplicationClassLoader(PluginLoader.getLoader());
		// context.setErrorReporter(errorReporter);
		try
		{
			// ScriptableObject scriptable = new ImporterTopLevel(context);
			// Bindings bs = engine.getBindings(ScriptContext.ENGINE_SCOPE);
			// for (String key : bs.keySet())
			// {
			// Object o = bs.get(key);
			// scriptable.put(key, scriptable, o);
			// }
			Script script = context.compileString(s, "script", LINE_NUMBER_START, null);
			script.exec(context, scriptable);
			// for (Object o : scriptable.getIds())
			// {
			// String key = (String) o;
			// bs.put(key, scriptable.get(key, scriptable));
			// }
		} catch (RhinoException e3)
		{
			getErrorWriter().write(e3.getMessage());
			throw new ScriptException(e3.getMessage(), e3.sourceName(), e3.lineNumber(), e3.columnNumber());
		} finally
		{
			bindings.clear();
			for (Object o : scriptable.getIds())
			{
				bindings.put((String) o, scriptable.get(o));
			}
			Context.exit();
		}
	}

	public void evalFile(String fileName) throws ScriptException
	{
		this.lastFileName = fileName;
		File f = new File(fileName);
		if (!f.exists())
		{
			throw new ScriptException("The script file could not be found, please check if it is correctly saved on the disk.", fileName, -1);
		}

		byte[] bytes = null;
		try
		{
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
			bytes = new byte[bis.available()];
			bis.read(bytes);
			bis.close();
		} catch (IOException e1)
		{
			throw new ScriptException(e1.getMessage(), fileName, -1);
		}
		String s = new String(bytes);

		Context context = Context.enter();
		context.setApplicationClassLoader(PluginLoader.getLoader());
		// context.setErrorReporter(errorReporter);
		try
		{
			Script script = context.compileString(s, "script", LINE_NUMBER_START, null);
			script.exec(context, scriptable);
		} catch (RhinoException e3)
		{
			getErrorWriter().write(e3.getMessage());
			throw new ScriptException(e3.getMessage(), e3.sourceName(), e3.lineNumber(), e3.columnNumber());
		} finally
		{
			bindings.clear();
			for (Object o : scriptable.getIds())
			{
				bindings.put((String) o, scriptable.get(o));
			}
			lastFileName = "";
			Context.exit();
		}
	}

	class IcyImporterTopLevel extends ImporterTopLevel
	{

		/**
         * 
         */
		private static final long serialVersionUID = 1L;

		public IcyImporterTopLevel(Context context)
		{
			super(context);
			String[] names = { "println", "print", "eval" };
			defineFunctionProperties(names, IcyImporterTopLevel.class, ScriptableObject.DONTENUM);
		}

		public void println(Object o)
		{
			getWriter().write(Context.toString(o) + "\n");
		}

		public void print(Object o)
		{
			getWriter().write(Context.toString(o));
		}

		public void eval(Object o) throws ScriptException, FileNotFoundException
		{
			File f;
			if (o instanceof NativeJavaObject && ((NativeJavaObject) o).unwrap() instanceof File)
				f = (File) ((NativeJavaObject) o).unwrap();
			else if (o instanceof String)
			{
				String s = (String) o;
				f = new File(s);
				if (!f.exists() && !s.contains(File.separator))
				{
					if (!s.endsWith(".js"))
						s += ".js";
					s = FileUtil.getDirectory(lastFileName) + File.separator + s;
					f = new File(s);
				}
			} else
			{
				// getErrorWriter().write("Argument must be a file of a string.");
				throw new FileNotFoundException("Argument must be a file of a string.");
			}
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
			byte[] bytes = null;
			try
			{
				bytes = new byte[bis.available()];
				bis.read(bytes);
				bis.close();
			} catch (IOException e1)
			{
				// getErrorWriter().write(e1.getMessage());
			}
			String s = new String(bytes);
			Context context = Context.enter();
			context.setApplicationClassLoader(PluginLoader.getLoader());
			try
			{
				Script script = context.compileString(s, "script", LINE_NUMBER_START, null);
				script.exec(context, scriptable);
			} catch (RhinoException e3)
			{
				// getErrorWriter().write(e3.getMessage());
				throw new ScriptException(e3.getMessage(), e3.sourceName(), e3.lineNumber(), e3.columnNumber());
			} finally
			{
				Context.exit();
			}
		}
	}

	@Override
	public String getName()
	{
		return "javascript";
	}

	@Override
	public void clear()
	{
		HashMap<String, Object> bindings = getBindings();
		for (String s : bindings.keySet())
		{
			bindings.put(s, null);
			scriptable.delete(s);
		}
	}

	@Override
	public HashMap<String, Object> getBindings()
	{
		return bindings;
	}

	@Override
	protected void putInRealEngine(String name, Object value)
	{
		scriptable.put(name, scriptable, value);
	}

	@Override
	protected void removeFromRealEngine(String name)
	{
		scriptable.delete(name);
	}
}