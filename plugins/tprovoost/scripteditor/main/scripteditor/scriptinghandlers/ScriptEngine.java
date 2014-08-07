package plugins.tprovoost.scripteditor.scriptinghandlers;

import java.io.PrintWriter;
import java.util.HashMap;

import javax.script.ScriptException;

public abstract class ScriptEngine
{
	protected HashMap<String, Object> bindings = new HashMap<String, Object>();
	private PrintWriter pw;
	private PrintWriter pwE;

	protected ScriptEngine() {
		pw = new PrintWriter(System.out);
		pwE = new PrintWriter(System.err);
	}

	public void setWriter(PrintWriter pw)
	{
		this.pw = pw;
	}

	public void setErrorWriter(PrintWriter pwE)
	{
		this.pwE = pwE;
	}

	public abstract void eval(String string) throws ScriptException;

	public abstract void evalFile(String fileName) throws ScriptException;

	public PrintWriter getWriter()
	{
		return pw;
	}

	public PrintWriter getErrorWriter()
	{
		return pwE;
	}

	public abstract String getName();

	public void clear()
	{
		HashMap<String, Object> bindings = getBindings();
		for (String s : bindings.keySet())
		{
			bindings.put(s, null);
		}
		bindings.clear();
	}

	protected abstract void putInRealEngine(String name, Object value);

	protected abstract void removeFromRealEngine(String name);

	public void put(String name, Object value)
	{
		putInRealEngine(name, value);
		bindings.put(name, value);
	}

	public void deleteBinding(String name)
	{
		put(name, null);
		bindings.remove(name);
		removeFromRealEngine(name);
	}

	public Object get(String name)
	{
		return bindings.get(name);
	}

	public HashMap<String, Object> getBindings()
	{
		return bindings;
	}

}