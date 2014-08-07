package plugins.tprovoost.scripteditor.scriptblock;

import javax.script.ScriptException;

import org.w3c.dom.Node;

import plugins.adufour.vars.gui.VarEditor;
import plugins.adufour.vars.lang.VarString;
import plugins.tprovoost.scripteditor.scriptinghandlers.ScriptEngine;
import plugins.tprovoost.scripteditor.scriptinghandlers.ScriptEngineHandler;

public class VarScript extends VarString
{
	private VarScriptEditorV3 editor;

	public VarScript(String name, String defaultValue)
	{
		super(name, defaultValue);
		setEditor(new VarScriptEditorV3(this, defaultValue));
	}

	@Override
	public String getValue()
	{
		if (getEditor() != null)
			return getEditor().getText();
		return getDefaultValue();
	}

	@Override
	public void setValue(String newValue) throws IllegalAccessError
	{
		getEditor().setText(newValue);
	}

	@Override
	public VarEditor<String> createVarEditor()
	{
		return getEditor();
	}

	public void evaluate() throws ScriptException
	{
		ScriptEngine engine = ScriptEngineHandler.getEngine("javascript");
		engine.eval(getValue());
	}

	public ScriptEngine getEngine()
	{
		return getEditor().getPanelIn().getScriptHandler().getEngine();
	}

	public VarScriptEditorV3 getEditor()
	{
		return editor;
	}

	public void setEditor(VarScriptEditorV3 editor)
	{
		this.editor = editor;
	}

	@Override
	public boolean saveToXML(Node node) throws UnsupportedOperationException
	{
		boolean res = super.saveToXML(node);
		// XMLUtil.setAttributeValue((Element) node, "scriptTitle",
		// editor.getTitle());
		return res;
	}

	@Override
	public boolean loadFromXML(Node node)
	{
		boolean res = super.loadFromXML(node);
		// editor.setTitle(XMLUtil.getAttributeValue((Element) node,
		// "scriptTitle", "untitled"));
		return res;
	}
}
