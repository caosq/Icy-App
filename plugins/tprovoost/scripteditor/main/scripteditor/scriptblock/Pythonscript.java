package plugins.tprovoost.scripteditor.scriptblock;

import icy.plugin.abstract_.Plugin;
import icy.roi.ROI;
import icy.sequence.Sequence;
import icy.system.IcyHandledException;
import icy.system.thread.ThreadUtil;

import java.io.File;
import java.util.HashMap;

import javax.script.ScriptException;

import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.blocks.util.VarListListener;
import plugins.adufour.vars.gui.model.TypeSelectionModel;
import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.lang.VarTrigger;
import plugins.adufour.vars.util.TypeChangeListener;
import plugins.adufour.vars.util.VarReferencingPolicy;
import plugins.tprovoost.scripteditor.scriptinghandlers.ScriptEngine;
import plugins.tprovoost.scripteditor.scriptinghandlers.ScriptVariable;
import plugins.tprovoost.scripteditor.scriptinghandlers.ScriptingHandler;
import plugins.tprovoost.scripteditor.scriptinghandlers.VariableType;
import plugins.tprovoost.scripteditor.scriptinghandlers.py.PyScriptEngine;

public class Pythonscript extends Plugin implements Block, VarListListener
{
    private VarScriptPython inputScript = new VarScriptPython("Script",
            "# Click on the button\n# to edit in a frame.\n\noutput0 = input0 * 2");

    private VarList inputMap;
    private VarList outputMap;

    private int inputIdx = 0;
    private int outputIdx = 0;

    private VarTrigger triggerInput;

    private VarTrigger triggerOutput;

    public Pythonscript()
    {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void run()
    {
        ScriptingHandler handler = inputScript.getEditor().getPanelIn().getScriptHandler();
        ScriptEngine engine = (PyScriptEngine) handler.createNewEngine();
        // String language = inputScript.getEditor().panelIn.getLanguage();

        for (Var<?> var : inputMap)
        {
            Object value = var.getValue();
            String name = var.getName();
            if (name.contains("input"))
            {
                // For another language, remove this, or use the right one.
                value = PythonScriptBlock.transformInputForScript(value);

                // put in the engine the value.
                engine.put(name, value);
            }
        }
        try
        {
            inputScript.evaluate();
        }
        catch (ScriptException e)
        {
            throw new IcyHandledException(e.getMessage());
        }

        for (Var output : outputMap)
        {
            Object resObject = engine.get(output.getName());
            output.setValue(PythonScriptBlock.transformScriptOutput(resObject));
        }
    }

    @Override
    public void declareInput(VarList inputMap)
    {
        if (this.inputMap == null)
            this.inputMap = inputMap;
        inputMap.addVarListListener(this);
        triggerInput = new VarTrigger("Add Input", new VarTrigger.TriggerListener()
        {

            @Override
            public void valueChanged(Var<Integer> source, Integer oldValue, Integer newValue)
            {
                registerVariables();
            }

            @Override
            public void referenceChanged(Var<Integer> source, Var<? extends Integer> oldReference,
                    Var<? extends Integer> newReference)
            {
                registerVariables();
            }

            @Override
            public void triggered(VarTrigger source)
            {
                String name = "input" + inputIdx;
                VarMutableScript myVariable = new VarMutableScript(name, Object.class);
                myVariable.setDefaultEditorModel(new TypeSelectionModel(new Class<?>[] {Object.class, Object[].class,
                        Sequence.class, ROI[].class, Integer.class, Double.class, int[].class, double[].class,
                        String.class, File.class, File[].class}));
                myVariable.addTypeChangeListener(new TypeChangeListener()
                {

                    @Override
                    public void typeChanged(Object source, Class<?> oldType, Class<?> newType)
                    {
                        registerVariables();
                    }
                });
                Pythonscript.this.inputMap.addRuntimeVariable("" + myVariable.hashCode(), myVariable);
                registerVariables();

            }
        });
        triggerInput.setReferencingPolicy(VarReferencingPolicy.NONE);

        triggerOutput = new VarTrigger("Add output", new VarTrigger.TriggerListener()
        {

            @Override
            public void valueChanged(Var<Integer> source, Integer oldValue, Integer newValue)
            {
                registerVariables();
            }

            @Override
            public void referenceChanged(Var<Integer> source, Var<? extends Integer> oldReference,
                    Var<? extends Integer> newReference)
            {
                registerVariables();
            }

            @Override
            public void triggered(VarTrigger source)
            {
                String name = "output" + outputIdx;
                VarMutableScript myVariable = new VarMutableScript(name, Object.class);
                myVariable.setDefaultEditorModel(new TypeSelectionModel(new Class<?>[] {Object.class, Object[].class,
                        Sequence.class, ROI[].class, Integer.class, Double.class, int[].class, double[].class,
                        String.class, File.class, File[].class}));
                myVariable.addTypeChangeListener(new TypeChangeListener()
                {

                    @Override
                    public void typeChanged(Object source, Class<?> oldType, Class<?> newType)
                    {
                        registerVariables();
                    }
                });
                outputMap.addRuntimeVariable("" + myVariable.hashCode(), myVariable);
                registerVariables();
            }
        });
        triggerOutput.setReferencingPolicy(VarReferencingPolicy.NONE);

        inputScript.setReferencingPolicy(VarReferencingPolicy.NONE);

        inputMap.add(inputScript);
        inputMap.add(triggerInput);
        inputMap.add(triggerOutput);

        String name = "input" + inputIdx;
        VarMutableScript myVariable = new VarMutableScript(name, Object.class);
        myVariable.setDefaultEditorModel(new TypeSelectionModel(new Class<?>[] {Object.class, Object[].class,
                Sequence.class, ROI[].class, Boolean.class, Integer.class, Double.class, int[].class, double[].class,
                String.class, File.class, File[].class}));
        myVariable.addTypeChangeListener(new TypeChangeListener()
        {

            @Override
            public void typeChanged(Object source, Class<?> oldType, Class<?> newType)
            {
                registerVariables();
            }
        });
        inputMap.add(name, myVariable);
    }

    @Override
    public void declareOutput(final VarList outputMap)
    {
        this.outputMap = outputMap;
        outputMap.addVarListListener(this);
        String name = "output" + outputIdx;
        VarMutableScript myVariable = new VarMutableScript(name, Object.class);
        myVariable.setDefaultEditorModel(new TypeSelectionModel(new Class<?>[] {Object.class, Object[].class,
                Sequence.class, ROI[].class, Boolean.class, Integer.class, Double.class, int[].class, double[].class,
                String.class, File.class, File[].class}));
        myVariable.addTypeChangeListener(new TypeChangeListener()
        {

            @Override
            public void typeChanged(Object source, Class<?> oldType, Class<?> newType)
            {
                registerVariables();
            }
        });
        outputMap.add(name, myVariable);
        registerVariables();
    }

    private void registerVariables()
    {
        final ScriptingHandler handlerIn = inputScript.getEditor().getPanelIn().getScriptHandler();
        final ScriptingHandler handlerOut = inputScript.getEditor().getPanelOut().getScriptHandler();

        HashMap<String, ScriptVariable> variablesInt = handlerIn.getExternalVariables();
        HashMap<String, ScriptVariable> variablesExt = handlerOut.getExternalVariables();

        variablesInt.clear();
        variablesExt.clear();
        for (Var<?> v : inputMap)
        {
            if (v instanceof VarMutableScript)
            {
                variablesInt.put(v.getName(), new ScriptVariable(new VariableType(v.getType())));
                variablesExt.put(v.getName(), new ScriptVariable(new VariableType(v.getType())));
            }
        }
        for (Var<?> v : outputMap)
        {
            if (v instanceof VarMutableScript)
            {
                variablesInt.put(v.getName(), new ScriptVariable(new VariableType(v.getType())));
                variablesExt.put(v.getName(), new ScriptVariable(new VariableType(v.getType())));
            }
        }
        ThreadUtil.invokeLater(new Runnable()
        {

            @Override
            public void run()
            {
                handlerIn.interpret(false);
                handlerOut.interpret(false);
            }
        });
    }

    @Override
    public void variableAdded(VarList list, Var<?> variable)
    {
        if (list == inputMap && variable != triggerInput && variable != inputScript && variable != triggerOutput)
            inputIdx++;
        else if (list == outputMap)
            outputIdx++;
    }

    @Override
    public void variableRemoved(VarList list, Var<?> variable)
    {
    }
}
