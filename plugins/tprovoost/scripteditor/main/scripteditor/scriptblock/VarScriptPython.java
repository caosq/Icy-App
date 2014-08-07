package plugins.tprovoost.scripteditor.scriptblock;

import javax.script.ScriptException;

import plugins.tprovoost.scripteditor.scriptinghandlers.ScriptEngine;
import plugins.tprovoost.scripteditor.scriptinghandlers.ScriptEngineHandler;

public class VarScriptPython extends VarScript
{
	public VarScriptPython(String name, String defaultValue)
	{
		super(name, defaultValue);
		setEditor(new VarScriptEditorPython(this, defaultValue));
	}

	public void evaluate() throws ScriptException
	{
		ScriptEngine engine = ScriptEngineHandler.getEngine("python");
		engine.eval(getValue());
	}
}
