package plugins.tprovoost.scripteditor.scriptinghandlers.js;

import icy.gui.main.MainInterface;
import icy.image.IcyBufferedImage;
import icy.plugin.PluginDescriptor;
import icy.plugin.PluginInstaller;
import icy.plugin.PluginLoader;
import icy.plugin.PluginRepositoryLoader;
import icy.sequence.Sequence;
import icy.util.ClassUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptException;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.JTextComponent;

import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.FunctionCompletion;
import org.fife.ui.autocomplete.ParameterizedCompletion.Parameter;
import org.fife.ui.autocomplete.VariableCompletion;
import org.fife.ui.rsyntaxtextarea.LinkGeneratorResult;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.Gutter;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.Assignment;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.Block;
import org.mozilla.javascript.ast.ElementGet;
import org.mozilla.javascript.ast.ExpressionStatement;
import org.mozilla.javascript.ast.ForInLoop;
import org.mozilla.javascript.ast.ForLoop;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.IfStatement;
import org.mozilla.javascript.ast.InfixExpression;
import org.mozilla.javascript.ast.Label;
import org.mozilla.javascript.ast.LabeledStatement;
import org.mozilla.javascript.ast.Loop;
import org.mozilla.javascript.ast.NewExpression;
import org.mozilla.javascript.ast.PropertyGet;
import org.mozilla.javascript.ast.Scope;
import org.mozilla.javascript.ast.StringLiteral;
import org.mozilla.javascript.ast.SwitchCase;
import org.mozilla.javascript.ast.SwitchStatement;
import org.mozilla.javascript.ast.VariableDeclaration;
import org.mozilla.javascript.ast.VariableInitializer;

import plugins.tprovoost.scripteditor.completion.IcyCompletionProvider;
import plugins.tprovoost.scripteditor.completion.types.BasicJavaClassCompletion;
import plugins.tprovoost.scripteditor.completion.types.ScriptFunctionCompletion;
import plugins.tprovoost.scripteditor.completion.types.ScriptFunctionCompletion.BindingFunction;
import plugins.tprovoost.scripteditor.scriptinghandlers.IcyFunctionBlock;
import plugins.tprovoost.scripteditor.scriptinghandlers.ScriptEngine;
import plugins.tprovoost.scripteditor.scriptinghandlers.ScriptEngineHandler;
import plugins.tprovoost.scripteditor.scriptinghandlers.ScriptVariable;
import plugins.tprovoost.scripteditor.scriptinghandlers.ScriptingHandler;
import plugins.tprovoost.scripteditor.scriptinghandlers.VariableType;

public class JSScriptingHandlerRhino extends ScriptingHandler
{
    private String currentText;

    // used to tell Rhino that it should count the lines starting from 1,
    // as in the GUI
    private static int LINE_NUMBER_START = 1;

    /**
     * Registers all the warnings and errors that have been found by Rhino when parsing
     */
    class Reporter implements ErrorReporter
    {

        @Override
        public void warning(String message, String sourceName, int line, String lineSource, int lineOffset)
        {
            ScriptException se = new ScriptException(message, sourceName, line, lineOffset);
            errors.addWarning(se);
        }

        @Override
        public void error(String message, String sourceName, int line, String lineSource, int lineOffset)
        {
            ScriptException se = new ScriptException(message, sourceName, line, lineOffset);
            errors.addError(se);
        }

        @Override
        public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource,
                int lineOffset)
        {
            System.out.println("runtimeError " + message + " " + sourceName + " " + line + " " + lineSource + " "
                    + lineOffset);
            ScriptException se = new ScriptException(message, sourceName, line, lineOffset);
            errors.addError(se);
            return null;
        }
    }

    /**
     * Registers all the warnings and errors that have been found by Rhino when parsing
     */
    Reporter reporter = new Reporter();

    /**
     * Contains all functions created in {@link #resolveCallType(AstNode, String, boolean)}.
     */
    private LinkedList<IcyFunctionBlock> functionBlocksToResolve = new LinkedList<IcyFunctionBlock>();

    public JSScriptingHandlerRhino(DefaultCompletionProvider provider, JTextComponent textArea, Gutter gutter,
            boolean autocompilation)
    {
        super(provider, "javascript", textArea, gutter, autocompilation);
    }

    public void evalEngine(ScriptEngine engine, String s) throws ScriptException
    {
        if (fileName == null || fileName.isEmpty() || fileName.contentEquals("Untitled"))
            engine.eval(s);
        else
            engine.evalFile(fileName);
    }

    @Override
    public void installDefaultLanguageCompletions(String language)
    {
        ScriptEngine engine = getEngine();
        if (engine == null)
        {
            if (DEBUG)
                System.out.println("Engine is null.");
            return;
        }

        ScriptEngineHandler engineHandler = ScriptEngineHandler.getEngineHandler(getEngine());
        HashMap<String, VariableType> engineFunctions = engineHandler.getEngineFunctions();
        HashMap<String, VariableType> engineVariables = engineHandler.getEngineVariables();

        // IMPORT PACKAGES
        try
        {
            importJavaScriptPackages(getEngine());
        }
        catch (ScriptException e1)
        {
        }

        // ArrayList<Parameter> params = new ArrayList<Parameter>();

        // HARDCODED ITEMS, TO BE REMOVED OR ADDED IN AN XML
        String mainInterface = MainInterface.class.getName();
        try
        {
            engine.eval("function getSequence() { return Packages.icy.main.Icy.getMainInterface().getFocusedSequence() }");

            if (provider != null)
            {
                FunctionCompletion c = new FunctionCompletion(provider, "getSequence", Sequence.class.getName());
                c.setDefinedIn(mainInterface);
                c.setReturnValueDescription("The focused sequence is returned.");
                c.setShortDescription("Returns the sequence under focus. Returns null if no sequence opened.");

                // check if does not exist already
                // if not, add it to provider
                @SuppressWarnings("unchecked")
                List<Completion> list = provider.getCompletionByInputText("gui");
                if (list == null || !IcyCompletionProvider.exists(c, list))
                    provider.addCompletion(c);
            }

            engineFunctions.put("getSequence", new VariableType(Sequence.class));
        }
        catch (ScriptException e)
        {
            System.out.println(e.getMessage());
        }

        try
        {
            engine.eval("function getImage() { return Packages.icy.main.Icy.getMainInterface().getFocusedImage(); }");
            if (provider != null)
            {
                FunctionCompletion c = new FunctionCompletion(provider, "getImage", IcyBufferedImage.class.getName());
                c.setDefinedIn(mainInterface);
                c.setShortDescription("Returns the current image viewed in the focused sequence.");
                c.setReturnValueDescription("Returns the focused Image, returns null if no sequence opened");

                // check if does not exist already
                // if not, add it to provider
                @SuppressWarnings("unchecked")
                List<Completion> list = provider.getCompletionByInputText("gui");
                if (list == null || !IcyCompletionProvider.exists(c, list))
                    provider.addCompletion(c);
            }
            engineFunctions.put("getImage", new VariableType(IcyBufferedImage.class));
        }
        catch (ScriptException e)
        {
            System.out.println(e.getMessage());
        }

        try
        {
            engine.eval("gui = Packages.icy.main.Icy.getMainInterface()");
            if (provider != null)
            {
                VariableCompletion vc = new VariableCompletion(provider, "gui", mainInterface);
                vc.setDefinedIn(mainInterface);
                vc.setShortDescription("Returns the sequence under focus. Returns null if no sequence opened.");

                // check if does not exist already
                // if not, add it to provider
                @SuppressWarnings("unchecked")
                List<Completion> list = provider.getCompletionByInputText("gui");
                if (list == null || !IcyCompletionProvider.exists(vc, list))
                    provider.addCompletion(vc);
            }
            engineVariables.put("gui", new VariableType(MainInterface.class));
        }
        catch (ScriptException e)
        {
            System.out.println(e.getMessage());
        }

        // ADD JS FUNCTIONS
        engineFunctions.put("importClass", new VariableType(void.class));
        engineFunctions.put("importPackage", new VariableType(void.class));
        engineFunctions.put("eval", new VariableType(void.class));

        // IMPORT PLUGINS FUNCTIONS
        try
        {
            importFunctions();
        }
        catch (ScriptException e)
        {
            e.printStackTrace();
        }
    }

    public void importJavaScriptPackages(ScriptEngine engine) throws ScriptException
    {
    }

    @Override
    public void installMethods(ScriptEngine engine, ArrayList<Method> methods)
    {
        // hardcoded functions, to remove in the future
        try
        {
            engine.eval("function getSequence() { return Packages.icy.main.Icy.getMainInterface().getFocusedSequence() }");
        }
        catch (ScriptException e1)
        {
        }
        try
        {
            engine.eval("function getImage() { return Packages.icy.main.Icy.getMainInterface().getFocusedImage(); }");
        }
        catch (ScriptException e1)
        {
        }
        try
        {
            engine.eval("gui = Packages.icy.main.Icy.getMainInterface()");
        }
        catch (ScriptException e1)
        {
        }

        for (Method method : methods)
        {
            // is it an annotated with BindingFunction?
            BindingFunction blockFunction = method.getAnnotation(BindingFunction.class);
            if (blockFunction == null)
                continue;
            // Generate the function for the provider
            ArrayList<Parameter> fParams = new ArrayList<Parameter>();
            Class<?>[] paramTypes = method.getParameterTypes();

            // get the parameters
            String params = "";
            String functionName = blockFunction.value();
            // get the parameters
            for (int i = 0; i < paramTypes.length; ++i)
            {
                fParams.add(new Parameter(IcyCompletionProvider.getType(paramTypes[i], true), "arg" + i));
                params += ",arg" + i;
            }
            if (params.length() > 0)
                params = params.substring(1);

            // the object for the provider
            ScriptFunctionCompletion sfc;
            if (Modifier.isStatic(method.getModifiers()))
                sfc = new ScriptFunctionCompletion(null, functionName, method);
            else
                sfc = new ScriptFunctionCompletion(null, method.getName(), method);

            try
            {
                // FIXME?
                // if (engine instanceof RhinoScriptEngine)
                // {
                if (method.getReturnType() == void.class)
                {
                    engine.eval("function " + functionName + " (" + params + ") {\n\t" + sfc.getMethodCall() + "\n}");
                }
                else
                {
                    engine.eval("function " + functionName + " (" + params + ") {\n\treturn " + sfc.getMethodCall()
                            + "\n}");
                }
                // }
            }
            catch (ScriptException e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void registerImports()
    {
        String s = textArea.getText();
        Pattern patternClasses = Pattern.compile("importClass\\(\\s*(Packages\\.|)((\\w|\\.|\\$)+)\\s*\\)");
        Matcher m = patternClasses.matcher(s);
        int offset = 0;
        while (m.find(offset))
        {
            String foundString = m.group(0);
            String imported = m.group(2);
            scriptDeclaredImportClasses.add(imported);

            PluginDescriptor plugindesc = PluginRepositoryLoader.getPlugin(imported);
            if (plugindesc != null)
            {
                // method is in the exact plugin
                if (!plugindesc.isInstalled())
                    PluginInstaller.install(plugindesc, false);
            }
            else
            {
                // class around plugin
                for (PluginDescriptor pd : PluginRepositoryLoader.getPlugins())
                {
                    if (pd.getClassName().startsWith(imported) && !pd.isInstalled())
                        PluginInstaller.install(pd, false);
                }
            }

            try
            {
                if (provider != null && (provider.getCompletionByInputText(imported)) == null)
                {
                    provider.addCompletion(new BasicJavaClassCompletion(provider, ClassUtil.findClass(imported)));
                }
            }
            catch (ClassNotFoundException e)
            {
            }
            int idxString = s.indexOf(foundString, offset);
            if (idxString == -1)
                break;
            offset = idxString + foundString.length();
        }

        Pattern patternPackages = Pattern.compile("importPackage\\((Packages\\.|)((\\w|\\.)+)\\)");
        m = patternPackages.matcher(s);
        offset = 0;
        while (m.find(offset))
        {
            String foundString = m.group(0);
            String imported = m.group(2);
            scriptDeclaredImports.add(imported);

            for (PluginDescriptor pd : PluginRepositoryLoader.getPlugins())
            {
                if (pd.getClassName().startsWith(imported) && !pd.isInstalled())
                    PluginInstaller.install(pd, false);
            }

            int idxString = s.indexOf(foundString, offset);
            if (idxString == -1)
                break;
            offset = idxString + foundString.length();
        }
    }

    @Override
    public void organizeImports(JTextComponent tc)
    {
        organizeImportsStatic(tc);
    }

    /**
     * Static because of the call in the autocomplete. FIXME
     * 
     * @param tc
     */
    public static void organizeImportsStatic(JTextComponent tc)
    {
        ArrayList<String> listImportsClass = new ArrayList<String>();
        ArrayList<String> listImportsPackages = new ArrayList<String>();
        boolean errorHappened = false;

        String originalText = tc.getText();
        String text = "";
        while ((text = tc.getText()).contains("importClass(") && !errorHappened)
        {
            int idxStart = text.indexOf("importClass(");
            if (idxStart == -1) // should never happen because of the contains
                continue;
            int idxStop = text.indexOf(')', idxStart);
            if (idxStop == -1)
            { // something weird happened in the code, stop.
                errorHappened = true;
                break;
            }
            Caret c = tc.getCaret();
            c.setDot(idxStart);
            c.moveDot(idxStop + 1);
            listImportsClass.add(tc.getSelectedText());
            tc.replaceSelection("");
        }
        while ((text = tc.getText()).contains("importPackage(") && !errorHappened)
        {
            int idxStart = text.indexOf("importPackage(");
            if (idxStart == -1) // should never happen because of the contains
                continue;
            int idxStop = text.indexOf(')', idxStart);
            if (idxStop == -1)
            { // something weird happened in the code, stop.
                errorHappened = true;
                break;
            }
            Caret c = tc.getCaret();
            c.setDot(idxStart);
            c.moveDot(idxStop + 1);
            listImportsPackages.add(tc.getSelectedText());
            tc.replaceSelection("");
        }
        if (errorHappened)
        {
            tc.setText(originalText);
        }
        else
        {
            String result = "";
            Collections.sort(listImportsClass);
            Collections.sort(listImportsPackages);

            for (int i = 0; i < listImportsClass.size(); ++i)
            {
                if (i == 0)
                    result += listImportsClass.get(i);
                else
                    result += "\n" + listImportsClass.get(i);
            }
            for (int i = 0; i < listImportsPackages.size(); ++i)
            {
                if (i == 0)
                    result += "\n\n" + listImportsPackages.get(i);
                else
                    result += "\n" + listImportsPackages.get(i);
            }
            String leftText = tc.getText();
            char c;
            while (leftText.length() > 0 && ((c = leftText.charAt(0)) == ' ' || c == '\n' || c == '\t'))
                leftText = leftText.substring(1);
            tc.setText(result + "\n\n" + leftText);
        }
    }

    @Override
    public void autoDownloadPlugins()
    {
        String s = textArea.getText();
        Pattern patternClasses = Pattern.compile("importClass\\((Packages\\.|)((\\w|\\.)+)\\)");
        Matcher m = patternClasses.matcher(s);
        int offset = 0;
        while (m.find(offset))
        {
            String foundString = m.group(0);
            String imported = m.group(2);

            PluginDescriptor plugindesc = PluginRepositoryLoader.getPlugin(imported);
            if (plugindesc != null)
            {
                // method is in the exact plugin
                if (!plugindesc.isInstalled())
                    PluginInstaller.install(plugindesc, false);
            }
            else
            {
                // class around plugin
                for (PluginDescriptor pd : PluginRepositoryLoader.getPlugins())
                {
                    if (pd.getClassName().startsWith(imported) && !pd.isInstalled())
                        PluginInstaller.install(pd, false);
                }
            }

            int idxString = s.indexOf(foundString, offset);
            if (idxString == -1)
                break;
            offset = idxString + foundString.length();
        }

        Pattern patternPackages = Pattern.compile("importPackage\\((Packages\\.|)((\\w|\\.)+)\\)");
        m = patternPackages.matcher(s);
        offset = 0;
        while (m.find(offset))
        {
            String foundString = m.group(0);
            String imported = m.group(2);

            for (PluginDescriptor pd : PluginRepositoryLoader.getPlugins())
            {
                if (pd.getClassName().startsWith(imported) && !pd.isInstalled())
                    PluginInstaller.install(pd, false);
            }

            int idxString = s.indexOf(foundString, offset);
            if (idxString == -1)
                break;
            offset = idxString + foundString.length();
        }
    }

    // --------------------------------------------------------------------
    // --------------------------------------------------------------------
    // JAVASCRIPT DETECTION FEATURES AND ASSISTANCE
    // --------------------------------------------------------------------
    // --------------------------------------------------------------------
    @Override
    protected void detectVariables(String s) throws ScriptException
    {
        Context.enter();
        try
        {
            currentText = s;
            final CompilerEnvirons comp = CompilerEnvirons.ideEnvirons();
            // report errors and warnings through our own reporter class
            comp.setErrorReporter(reporter);
            // do not complain about missing commas when they are not strictly needed
            comp.setStrictMode(false);
            final Parser parser = new Parser(comp);
            AstRoot root = null;

            try
            {
                root = parser.parse(s, fileName, LINE_NUMBER_START);
            }
            catch (RhinoException e)
            {
                throw new ScriptException(e.getMessage(), e.sourceName(), e.lineNumber(), e.columnNumber());
            }

            if (root == null || !root.hasChildren())
                return;
            // no issue, removes variables
            if (provider != null)
            {
                for (Completion c : variableCompletions)
                    provider.removeCompletion(c);
            }
            variableCompletions.clear();

            // functionBlocksToResolve.clear();
            if (DEBUG)
                dumpTree(root, root, 1, "");

            // register external variables prio to detection.
            // Otherwise, references to external variables will not
            // be detected.
            addExternalVariables();

            // start variable registration
            registerVariables(root, root, s);

            // add the completions
            if (provider != null)
                provider.addCompletions(variableCompletions);
        }
        finally
        {
            Context.exit();
        }
    }

    /**
     * Register all variables in the successfully compiled script.
     * 
     * @param n
     * @param root
     * @throws ScriptException
     */
    private void registerVariables(AstNode n, AstRoot root, String text) throws ScriptException
    {
        if (n == null)
            return;
        if (DEBUG)
            System.out.println("current node: " + typeToName(n.getType()));
        // register current

        for (Completion c : generateCompletion(n, root, text))
        {
            if (c != null)
            {
                boolean alreadyExists = false;
                if (c instanceof VariableCompletion)
                {
                    ScriptVariable vc = localVariables.get(((VariableCompletion) c).getName());
                    if (vc != null && !vc.isInScope(textArea.getCaretPosition()))
                        alreadyExists = true;
                }
                for (int i = 0; i < variableCompletions.size() && !alreadyExists; ++i)
                {
                    if (variableCompletions.get(i).compareTo(c) == 0)
                    {
                        if (textArea.getCaret().getDot() > n.getAbsolutePosition())
                            variableCompletions.remove(i);
                        else
                            alreadyExists = true;
                    }
                }
                if (!alreadyExists)
                    variableCompletions.add(c);
            }
        }
        // recursive call on children (if any)
        if (n.hasChildren())
        {
            AstNode child = (AstNode) n.getFirstChild();
            while (child != null)
            {
                registerVariables(child, root, text);
                child = (AstNode) child.getNext();
            }
        }
    }

    /**
     * Automatically generates the completion for javascript: variable or
     * function.
     * 
     * @param n
     * @param root
     * @param commandStartOffset
     * @param commandEndOffset
     * @return
     * @throws ScriptException
     */
    private List<Completion> generateCompletion(AstNode n, AstRoot root, String text) throws ScriptException
    {
        List<Completion> toReturn = new ArrayList<Completion>();
        AstNode expression = null;

        switch (n.getType())
        {
            case Token.VAR:
                VariableDeclaration var = (VariableDeclaration) n;
                List<VariableInitializer> listVars = var.getVariables();

                // listVars cannot be null, and contains
                // all variables declared after the keyword "var"
                for (VariableInitializer v : listVars)
                {
                    AstNode target = v.getTarget();
                    AstNode init = v.getInitializer();

                    // only use variable declaration with names,
                    // not with Literals.
                    if (target.getType() == Token.NAME)
                    {
                        VariableType type = null;
                        if (init != null)
                            type = getRealType(init);
                        String typeString = "";
                        if (type != null)
                            typeString = type.toString();
                        VariableCompletion c = new VariableCompletion(provider, target.getString(), typeString);
                        c.setSummary("variable");
                        c.setDefinedIn("script");
                        c.setRelevance(RELEVANCE_HIGH);
                        int pos = n.getAbsolutePosition();
                        addVariableDeclaration(c.getName(), type, pos, pos + var.getParent().getLength());
                        toReturn.add(c);
                    }
                }
                break;                    
            case Token.EXPR_VOID:
                if (n instanceof LabeledStatement)
                {
                    LabeledStatement labelSt = (LabeledStatement) n;
                    for (Label label : labelSt.getLabels())
                        toReturn.add(new BasicCompletion(provider, label.getName(), "Label"));
                    break;
                }
            case Token.EXPR_RESULT:
                expression = ((ExpressionStatement) n).getExpression();
            case Token.ASSIGN:
                if (expression == null)
                    expression = n;
                if (expression instanceof Assignment)
                {
                    AstNode left = ((Assignment) expression).getLeft();
                    AstNode right = ((Assignment) expression).getRight();
                    VariableType type = getRealType(right);
                    if (type != null && type.getClazz() == void.class)
                    {
                        throw new ScriptException("This method returns \"void\" and cannot be assigned", fileName,
                                n.getLineno(), -1);
                    }
                    String typeString = "";
                    if (type != null)
                        typeString = type.toString();
                    if (left.getType() == Token.NAME)
                    {
                        VariableCompletion c = new VariableCompletion(provider, left.getString(), typeString);
                        c.setSummary("variable");
                        c.setDefinedIn("script");
                        c.setRelevance(RELEVANCE_HIGH);
                        addVariableDeclaration(c.getName(), type, n.getAbsolutePosition());
                        toReturn.add(c);
                    }
                }
                else if (expression instanceof FunctionCall)
                {
                    AstNode target = ((FunctionCall) expression).getTarget();
                    if (!(target.getType() == Token.NAME && (target.getString().contentEquals("importClass") || target
                            .getString().contentEquals("importPackage"))))
                    {
                        VariableType vt = resolveCallType(expression, text, false);
                        if (vt == null)
                            updateGutter();
                    }
                }
                else if (expression instanceof PropertyGet)
                {
                    // Do nothing
                }

                break;

            case Token.BLOCK:
                if (n instanceof Block)
                {
                    Block scope = (Block) n;
                    AstNode child = (AstNode) scope.getFirstChild();
                    while (child != null)
                    {
                        registerVariables(child, root, text);
                        child = (AstNode) child.getNext();
                    }
                }
                else if (n instanceof Scope)
                {
                    Scope scope = (Scope) n;
                    AstNode child = (AstNode) scope.getFirstChild();
                    while (child != null)
                    {
                        registerVariables(child, root, text);
                        child = (AstNode) child.getNext();
                    }
                }
                break;

            case Token.CALL:
                resolveCallType(n, text, false);
                break;

            case Token.FUNCTION:
                FunctionNode fn = (FunctionNode) n;
                FunctionCompletion fc = new FunctionCompletion(provider, fn.getName(), "");
                List<AstNode> paramsFn = fn.getParams();
                ArrayList<Parameter> params = new ArrayList<Parameter>();
                for (AstNode param : paramsFn)
                {
                    params.add(new Parameter("", param.toSource()));
                }
                fc.setParams(params);
                fc.setDefinedIn("script");
                fc.setRelevance(RELEVANCE_HIGH);
                localFunctions.put(fn.getName(), new VariableType(Void.class));
                toReturn.add(fc);
                registerVariables(fn.getBody(), root, text);
                break;

            case Token.IF:
                IfStatement nIf = (IfStatement) n;
                registerVariables(nIf.getThenPart(), root, text);
                registerVariables(nIf.getElsePart(), root, text);
                break;
            case Token.DO:
            case Token.FOR:
            {
                if (n instanceof ForInLoop)
                {
                    ForInLoop fl = (ForInLoop) n;
                    registerVariables(fl.getIterator(), root, text);
                }
                if (n instanceof ForLoop)
                {
                    ForLoop fl = (ForLoop) n;
                    registerVariables(fl.getInitializer(), root, text);
                }
            }
            case Token.WHILE:
                registerVariables(((Loop) n).getBody(), root, text);
                break;
            case Token.SWITCH:
                for (SwitchCase c : ((SwitchStatement) n).getCases())
                {
                    for (AstNode statement : c.getStatements())
                    {
                        registerVariables(statement, root, text);
                    }
                }
                break;
        }
        return toReturn;
    }

    // 22/03/13 1608
    private VariableType resolveCallType(AstNode n, String text, boolean noerror)
    {
        VariableType toReturn = null;
        ScriptEngineHandler engineHandler = ScriptEngineHandler.getEngineHandler(getEngine());
        int offset = n.getAbsolutePosition();
        String s = buildFunction2(n);
        // System.out.println(s);
        boolean containsNew = s.contains("new ");
        if (containsNew)
        {
            s = s.substring("new ".length());
        }

        // create a regex pattern
        Pattern p = Pattern.compile("\\w(\\w|\\.|\\[|\\])*\\((\\w|\\.|\\[|,|\\]|\\(|\\)| )*\\)");
        Matcher match = p.matcher(s);

        int idxP1 = 0;
        int idxP2;
        int decal = 0;
        boolean isField = false;
        if (match.find(0))
        {
            // TODO handle spot.points
            String firstCall = match.group(0);
            idxP1 = firstCall.indexOf('(');
            idxP2 = firstCall.indexOf(')');
            decal += idxP2 + 1;
            int lastDot = firstCall.substring(0, idxP1).lastIndexOf('.');
            VariableType vt = null;

            // get the className (or binding function name if it is the
            // case)
            String classNameOrFunctionNameOrVariable;
            if (lastDot != -1)
                classNameOrFunctionNameOrVariable = firstCall.substring(0, lastDot);
            else if (idxP1 != -1)
                classNameOrFunctionNameOrVariable = firstCall.substring(0, idxP1);
            else
                classNameOrFunctionNameOrVariable = firstCall.substring(0);

            if (classNameOrFunctionNameOrVariable.contains("."))
            {
                // is it a class?
                Class<?> clazz = resolveClassDeclaration(classNameOrFunctionNameOrVariable);
                if (clazz != null)
                    vt = new VariableType(clazz);
                else
                {
                    String res[] = classNameOrFunctionNameOrVariable.split("\\.");
                    classNameOrFunctionNameOrVariable = res[0];
                    isField = true;
                    for (int i = 1; i < res.length; ++i)
                    {
                        lastDot = classNameOrFunctionNameOrVariable.length() + 1;
                        decal = classNameOrFunctionNameOrVariable.length() + 1;
                    }
                }
            }

            if (vt == null)
            {
                // --------------------------
                // TEST IF IS FUNCTION BINDED
                // --------------------------
                vt = localFunctions.get(classNameOrFunctionNameOrVariable);
            }
            if (vt == null)
            {
                vt = ScriptEngineHandler.getEngineHandler(getEngine()).getEngineFunctions()
                        .get(classNameOrFunctionNameOrVariable);
                if (classNameOrFunctionNameOrVariable.contentEquals("println")
                        || classNameOrFunctionNameOrVariable.contentEquals("print"))
                    vt = new VariableType(void.class);
            }

            // -------------------------------------------
            // IT IS SOMETHING ELSE, PERFORM VARIOUS TESTS
            // -------------------------------------------

            // is it a script defined variable?
            if (vt == null)
                vt = getVariableDeclaration(classNameOrFunctionNameOrVariable, offset);

            // is it an engine variable?
            if (vt == null)
                vt = engineHandler.getEngineVariables().get(classNameOrFunctionNameOrVariable);

            // is it a class?
            if (vt == null)
            {
                Class<?> clazz = resolveClassDeclaration(classNameOrFunctionNameOrVariable);
                if (clazz != null)
                    vt = new VariableType(clazz);
            }

            // unknown type
            if (vt == null)
            {
                errors.addWarning(new ScriptException("Unknown field or method: " + classNameOrFunctionNameOrVariable,
                        null, n.getLineno()));
                return null;
            }

            // the first type!
            Class<?> returnType = vt.getClazz();

            if (returnType == null)
                return null;

            String call;
            if (decal < idxP1)
                call = firstCall.substring(lastDot + 1);
            else
                call = firstCall.substring(lastDot + 1, idxP1);

            // FIND THE CORRESPONDING METHOD
            String genericType = vt.getType();
            if (lastDot != -1)
            {
                if (!isField)
                {
                    try
                    {
                        // generate the Class<?> arguments
                        Class<?> clazzes[];

                        // get the arguments
                        String argsString = firstCall.substring(idxP1 + 1, idxP2);

                        // separate arguments
                        String[] args = argsString.split(",");

                        if (argsString.isEmpty())
                        {
                            clazzes = new Class<?>[0];
                        }
                        else
                        {
                            clazzes = new Class<?>[args.length];
                            for (int i = 0; i < clazzes.length; ++i)
                                clazzes[i] = resolveClassDeclaration(args[i]);
                            clazzes = getGenericNumberTypes(text, n, vt.getClazz(),
                                    firstCall.substring(lastDot + 1, idxP1), clazzes);
                        }
                        Method m = resolveMethod(returnType, call, clazzes);

                        String genericReturnType = m.getGenericReturnType().toString();
                        if (Pattern.matches("(\\[*)E", genericReturnType) && !genericType.isEmpty())
                        {
                            try
                            {
                                // TOOD array
                                returnType = ClassUtil.findClass(genericType);
                                genericType = "";
                            }
                            catch (ClassNotFoundException e)
                            {
                            }
                        }
                        else
                        {
                            // set the new return type.
                            returnType = m.getReturnType();
                            if (returnType.getTypeParameters().length > 0)
                            {
                                genericType = VariableType.getType(m.getGenericReturnType().toString());
                            }
                        }
                    }
                    catch (SecurityException e1)
                    {
                    }
                    catch (NoSuchMethodException e1)
                    {
                        try
                        {
                            Field f = returnType.getField(call);
                            returnType = f.getType();
                        }
                        catch (SecurityException e)
                        {
                            errors.addWarning(new ScriptException("Unknown field or method: " + call, null, n
                                    .getLineno()));
                            return null;
                        }
                        catch (NoSuchFieldException e)
                        {
                            errors.addWarning(new ScriptException("Unknown field or method: " + call, null, n
                                    .getLineno()));
                            return null;
                        }
                    }
                }
                else
                {
                    // TODO
                    // System.out.println("not a method");
                    Field f;
                    try
                    {
                        String next = firstCall.substring(decal);
                        next = next.substring(0, next.indexOf('.'));
                        decal += next.length();
                        f = returnType.getField(next);
                        returnType = f.getType();
                    }
                    catch (SecurityException e)
                    {
                        errors.addWarning(new ScriptException("Unknown field or method: " + call, null, n.getLineno()));
                        return null;
                    }
                    catch (NoSuchFieldException e)
                    {
                        errors.addWarning(new ScriptException("Unknown field or method: " + call, null, n.getLineno()));
                        return null;
                    }
                }

            }

            // Create the VariableType containing the result.
            toReturn = new VariableType(returnType, genericType);

            // Pop the function Block and set its type.
            IcyFunctionBlock fb = functionBlocksToResolve.pop();
            fb.setReturnType(toReturn);

            if (DEBUG)
                System.out.println("function edited: (" + (fb.getStartOffset() + n.getPosition()) + ") "
                        + text.substring(offset));

            // Add the function block to the index of blockFunctions.
            blockFunctions.put(fb.getStartOffset(), fb);

            // iterate over the next functions, based on the returnType
            while (match.find(decal) && !(firstCall = match.group()).isEmpty())
            {
                if (returnType == void.class)
                {
                    System.out.println("Void return, impossible to call something else on it. at line:"
                            + +n.getLineno());
                }
                idxP1 = firstCall.indexOf('(');
                idxP2 = firstCall.indexOf(')');
                decal += idxP2 + 2; // account for ) and .
                String argsString = firstCall.substring(idxP1 + 1, idxP2);
                String[] args = argsString.split(",");
                Class<?>[] clazzes;
                if (argsString.isEmpty())
                {
                    clazzes = new Class<?>[0];
                }
                else
                {
                    clazzes = new Class<?>[args.length];
                    for (int i = 0; i < clazzes.length; ++i)
                        clazzes[i] = resolveClassDeclaration(args[i]);
                    lastDot = firstCall.substring(0, idxP1).lastIndexOf('.');
                    if (lastDot < 0)
                    {
                        lastDot = -1; // in case of new for instance.
                    }
                    clazzes = getGenericNumberTypes(text, n, returnType, firstCall.substring(lastDot + 1, idxP1),
                            clazzes);
                }
                String call2;
                // if (lastDot != -1)
                // call2 = firstCall.substring(lastDot + 1, idxP1);
                // else
                call2 = firstCall.substring(0, idxP1);
                if (call2.contentEquals("newInstance"))
                {
                    try
                    {
                        returnType.getConstructor(clazzes);
                    }
                    catch (SecurityException e)
                    {
                    }
                    catch (NoSuchMethodException e)
                    {
                    }
                }
                else
                {
                    Method m;
                    try
                    {
                        m = resolveMethod(returnType, firstCall.substring(0, idxP1), clazzes);
                        // Check if the return type is E or [E or [[E, etc. That
                        // means that 'E'
                        // corresponds
                        // to the previous generic Type.
                        String genericReturnType = m.getGenericReturnType().toString();
                        if (Pattern.matches("(\\[*)E", genericReturnType) && !genericType.isEmpty())
                        {
                            try
                            {
                                // TOOD array
                                returnType = ClassUtil.findClass(genericType);
                                genericType = "";
                            }
                            catch (ClassNotFoundException e)
                            {
                            }
                        }
                        else
                        {
                            // set the new return type.
                            returnType = m.getReturnType();
                            if (returnType.getTypeParameters().length > 0)
                            {
                                genericType = VariableType.getType(m.getGenericReturnType().toString());
                            }
                        }
                    }
                    catch (SecurityException e1)
                    {
                    }
                    catch (NoSuchMethodException e1)
                    {
                        try
                        {
                            Field f = returnType.getField(call);
                            returnType = f.getType();
                        }
                        catch (SecurityException e)
                        {
                        }
                        catch (NoSuchFieldException e)
                        {
                            return null;
                        }
                    }

                }
                if (functionBlocksToResolve.isEmpty())
                    return null;
                // get the last function block to resolve and set its type
                fb = functionBlocksToResolve.pop();
                toReturn = new VariableType(returnType, genericType);
                fb.setReturnType(toReturn);

                if (DEBUG)
                    System.out.println("function edited: (" + (fb.getStartOffset() + n.getPosition()) + ") "
                            + text.substring(offset));

                // add the Function Block to the index of blockFunctions.
                blockFunctions.put(fb.getStartOffset(), fb);
            }
            return toReturn;
        }
        return null;
    }

    @SuppressWarnings("unused")
    private VariableType resolveCallTypeOld(AstNode n, String text, boolean noerror)
    {
        VariableType toReturn = null;
        ScriptEngineHandler engineHandler = ScriptEngineHandler.getEngineHandler(getEngine());
        int offset = n.getAbsolutePosition();
        String s = buildFunction2(n);
        // System.out.println(s);
        boolean containsNew = s.contains("new ");
        if (containsNew)
        {
            s = s.substring("new ".length());
        }

        // create a regex pattern
        Pattern p = Pattern.compile("\\w(\\w|\\.|\\[|\\])*\\((\\w|\\.|\\[|,|\\]|\\(|\\)| )*\\)");
        Matcher match = p.matcher(s);

        int idxP1 = 0;
        int idxP2;
        int decal = 0;
        if (match.find(0))
        {
            // TODO handle spot.points
            String firstCall = match.group(0);
            idxP1 = firstCall.indexOf('(');
            idxP2 = firstCall.indexOf(')');
            decal += idxP2 + 1;
            int lastDot = firstCall.substring(0, idxP1).lastIndexOf('.');
            VariableType vt = null;

            // get the className (or binding function name if it is the
            // case)
            String classNameOrFunctionNameOrVariable;
            if (lastDot != -1)
                classNameOrFunctionNameOrVariable = firstCall.substring(0, lastDot);
            else if (idxP1 != -1)
                classNameOrFunctionNameOrVariable = firstCall.substring(0, idxP1);
            else
                classNameOrFunctionNameOrVariable = firstCall.substring(0);

            if (classNameOrFunctionNameOrVariable.contains("."))
            {
                // is it a class?
                Class<?> clazz = resolveClassDeclaration(classNameOrFunctionNameOrVariable);
                if (clazz != null)
                    vt = new VariableType(clazz);
                else
                {
                    String res[] = classNameOrFunctionNameOrVariable.split("\\.");
                    classNameOrFunctionNameOrVariable = res[0];
                    for (int i = 1; i < res.length; ++i)
                    {
                        lastDot = classNameOrFunctionNameOrVariable.length() + 1;
                        decal = classNameOrFunctionNameOrVariable.length() + 1;
                    }
                }
            }

            // get the arguments
            String argsString = firstCall.substring(idxP1 + 1, idxP2);

            // separate arguments
            String[] args = argsString.split(",");

            // --------------------------
            // TEST IF IS FUNCTION BINDED
            // --------------------------
            vt = localFunctions.get(classNameOrFunctionNameOrVariable);
            if (vt == null)
            {
                vt = ScriptEngineHandler.getEngineHandler(getEngine()).getEngineFunctions()
                        .get(classNameOrFunctionNameOrVariable);
                if (classNameOrFunctionNameOrVariable.contentEquals("println")
                        || classNameOrFunctionNameOrVariable.contentEquals("print"))
                    vt = new VariableType(void.class);
            }

            // -------------------------------------------
            // IT IS SOMETHING ELSE, PERFORM VARIOUS TESTS
            // -------------------------------------------

            // is it a script defined variable?
            if (vt == null)
                vt = getVariableDeclaration(classNameOrFunctionNameOrVariable, offset);

            // is it an engine variable?
            if (vt == null)
                vt = engineHandler.getEngineVariables().get(classNameOrFunctionNameOrVariable);

            // is it a class?
            if (vt == null)
            {
                Class<?> clazz = resolveClassDeclaration(classNameOrFunctionNameOrVariable);
                if (clazz != null)
                    vt = new VariableType(clazz);
            }

            // unknown type
            if (vt == null)
            {
                System.out.println("Unknown: " + classNameOrFunctionNameOrVariable + " at line: " + n.getLineno());
                return null;
            }

            // generate the Class<?> arguments
            Class<?> clazzes[];
            if (argsString.isEmpty())
            {
                clazzes = new Class<?>[0];
            }
            else
            {
                clazzes = new Class<?>[args.length];
                for (int i = 0; i < clazzes.length; ++i)
                    clazzes[i] = resolveClassDeclaration(args[i]);
                clazzes = getGenericNumberTypes(text, n, vt.getClazz(), firstCall.substring(lastDot + 1, idxP1),
                        clazzes);
            }

            // the first type!
            Class<?> returnType = vt.getClazz();

            String call;
            if (decal < idxP1)
                call = firstCall.substring(lastDot + 1);
            else
                call = firstCall.substring(lastDot + 1, idxP1);

            // FIND THE CORRESPONDING METHOD
            String genericType = vt.getType();
            if (lastDot != -1)
            {
                try
                {
                    Method m = resolveMethod(returnType, call, clazzes);

                    String genericReturnType = m.getGenericReturnType().toString();
                    if (Pattern.matches("(\\[*)E", genericReturnType) && !genericType.isEmpty())
                    {
                        try
                        {
                            // TOOD array
                            returnType = ClassUtil.findClass(genericType);
                            genericType = "";
                        }
                        catch (ClassNotFoundException e)
                        {
                        }
                    }
                    else
                    {
                        // set the new return type.
                        returnType = m.getReturnType();
                        if (returnType.getTypeParameters().length > 0)
                        {
                            genericType = VariableType.getType(m.getGenericReturnType().toString());
                        }
                    }
                }
                catch (SecurityException e1)
                {
                }
                catch (NoSuchMethodException e1)
                {
                    try
                    {
                        Field f = returnType.getField(call);
                        returnType = f.getType();
                    }
                    catch (SecurityException e)
                    {
                    }
                    catch (NoSuchFieldException e)
                    {
                        return null;
                    }
                }
            }
            // Create the VariableType containing the result.
            toReturn = new VariableType(returnType, genericType);

            // Pop the function Block and set its type.
            IcyFunctionBlock fb = functionBlocksToResolve.pop();
            fb.setReturnType(toReturn);

            if (DEBUG)
                System.out.println("function edited: (" + (fb.getStartOffset() + n.getPosition()) + ") "
                        + text.substring(offset));

            // Add the function block to the index of blockFunctions.
            blockFunctions.put(fb.getStartOffset(), fb);

            // iterate over the next functions, based on the returnType
            while (match.find(decal) && !(firstCall = match.group()).isEmpty())
            {
                if (returnType == void.class)
                {
                    System.out.println("Void return, impossible to call something else on it. at line:"
                            + +n.getLineno());
                }
                idxP1 = firstCall.indexOf('(');
                idxP2 = firstCall.indexOf(')');
                decal += idxP2 + 2; // account for ) and .
                argsString = firstCall.substring(idxP1 + 1, idxP2);
                args = argsString.split(",");
                if (argsString.isEmpty())
                {
                    clazzes = new Class<?>[0];
                }
                else
                {
                    clazzes = new Class<?>[args.length];
                    for (int i = 0; i < clazzes.length; ++i)
                        clazzes[i] = resolveClassDeclaration(args[i]);
                    lastDot = firstCall.substring(0, idxP1).lastIndexOf('.');
                    if (lastDot < 0)
                    {
                        lastDot = -1; // in case of new for instance.
                    }
                    clazzes = getGenericNumberTypes(text, n, returnType, firstCall.substring(lastDot + 1, idxP1),
                            clazzes);
                }
                String call2;
                // if (lastDot != -1)
                // call2 = firstCall.substring(lastDot + 1, idxP1);
                // else
                call2 = firstCall.substring(0, idxP1);
                if (call2.contentEquals("newInstance"))
                {
                    try
                    {
                        returnType.getConstructor(clazzes);
                    }
                    catch (SecurityException e)
                    {
                    }
                    catch (NoSuchMethodException e)
                    {
                    }
                }
                else
                {
                    Method m;
                    try
                    {
                        m = resolveMethod(returnType, firstCall.substring(0, idxP1), clazzes);
                        // Check if the return type is E or [E or [[E, etc. That
                        // means that 'E'
                        // corresponds
                        // to the previous generic Type.
                        String genericReturnType = m.getGenericReturnType().toString();
                        if (Pattern.matches("(\\[*)E", genericReturnType) && !genericType.isEmpty())
                        {
                            try
                            {
                                // TOOD array
                                returnType = ClassUtil.findClass(genericType);
                                genericType = "";
                            }
                            catch (ClassNotFoundException e)
                            {
                            }
                        }
                        else
                        {
                            // set the new return type.
                            returnType = m.getReturnType();
                            if (returnType.getTypeParameters().length > 0)
                            {
                                genericType = VariableType.getType(m.getGenericReturnType().toString());
                            }
                        }
                    }
                    catch (SecurityException e1)
                    {
                    }
                    catch (NoSuchMethodException e1)
                    {
                        try
                        {
                            Field f = returnType.getField(call);
                            returnType = f.getType();
                        }
                        catch (SecurityException e)
                        {
                        }
                        catch (NoSuchFieldException e)
                        {
                            return null;
                        }
                    }

                }

                // get the last function block to resolve and set its type
                fb = functionBlocksToResolve.pop();
                toReturn = new VariableType(returnType, genericType);
                fb.setReturnType(toReturn);

                if (DEBUG)
                    System.out.println("function edited: (" + (fb.getStartOffset() + n.getPosition()) + ") "
                            + text.substring(offset));

                // add the Function Block to the index of blockFunctions.
                blockFunctions.put(fb.getStartOffset(), fb);
            }
            return toReturn;
        }
        return null;
    }

    /**
     * Fix: Issue with same name functions, will always use the first one.
     * 
     * @param clazz
     * @param function
     * @param argsClazzes
     * @param commandStartOffset
     * @param commandEndOffset
     * @return
     */
    private static Class<?>[] getGenericNumberTypes(String text, AstNode n, Class<?> clazz, String function,
            Class<?>[] argsClazzes)
    {
        Class<?>[] toReturn = new Class<?>[argsClazzes.length];
        int offset = n.getAbsolutePosition();
        String fullCommand = text.substring(offset, offset + n.getLength());
        int idxStart = fullCommand.indexOf(function);

        if (idxStart == -1)
            return argsClazzes;

        int idxP1 = fullCommand.indexOf('(', idxStart);
        int idxP2 = fullCommand.indexOf(')', idxStart);

        if (idxP1 == -1 || idxP2 == -1)
            return argsClazzes;

        String argumentsChained = fullCommand.substring(idxP1 + 1, idxP2);
        String[] args = argumentsChained.split(",");

        if (args.length != argsClazzes.length)
            return argsClazzes;

        // FIXME here
        boolean hasNumber = false;
        for (int i = 0; i < argsClazzes.length; ++i)
        {
            if (argsClazzes[i] == Number.class)
                hasNumber = true;
            toReturn[i] = argsClazzes[i];
        }
        if (hasNumber)
        {
            for (Method m : clazz.getMethods())
            {
                if (m.getName().contentEquals(function))
                {
                    Class<?> params[] = m.getParameterTypes();
                    if (params.length == argsClazzes.length)
                    {
                        for (int i = 0; i < params.length; ++i)
                        {
                            if (params[i] == null || argsClazzes[i] == null)
                                break;
                            if (params[i].isAssignableFrom(argsClazzes[i]))
                                toReturn[i] = params[i];
                            else if (params[i].isPrimitive())
                            {
                                if (!(params[i] == float.class || params[i] == double.class) && !args[i].contains("."))
                                    toReturn[i] = params[i];
                                else if (params[i] == float.class || params[i] == double.class)
                                    toReturn[i] = params[i];
                                else
                                    break;
                            }
                            else
                                break;
                        }
                    }
                }
            }
            return toReturn;
        }
        else
            return toReturn;
    }

    // private Class<?> resolveRight(AstNode right, String text) throws
    // ScriptException
    // {
    // switch (right.getType())
    // {
    // case Token.NAME:
    // return getVariableDeclaration(right.getString());
    //
    // case Token.STRING:
    // return String.class;
    //
    // case Token.NUMBER:
    // return Number.class;
    //
    // case Token.CALL:
    // return resolveCallType(right, text, false);
    //
    // case Token.FUNCTION:
    // return Void.class;
    //
    // case Token.NEW:
    // {
    // NewExpression nexp = (NewExpression) right;
    // AstNode target = nexp.getTarget();
    // if (target != null)
    // {
    // String className = generateClassName(target, "");
    // return resolveClassDeclaration(className);
    // }
    // }
    // case Token.ARRAYLIT:
    // return Object[].class;
    // case Token.GETPROP:
    // {
    // AstNode target = ((PropertyGet) right).getTarget();
    // if (target.getType() == Token.GETELEM)
    // {
    // // array
    // String rightStr = generateClassName(right, "");
    // // Class<?> clazz = resolveArrayItemTypeComponent(target);
    // // clazz = createArrayItemType(clazz, target);
    // if (rightStr.contentEquals("length"))
    // return int.class;
    // }
    // else
    // {
    // // class
    // String className = generateClassName(right, "");
    // Class<?> clazz = resolveClassDeclaration(className);
    // if (clazz != null)
    // return clazz;
    // // try if it is an enum
    // int idx = className.lastIndexOf('.');
    // if (idx != -1)
    // {
    // clazz = resolveClassDeclaration(className.substring(0, idx));
    // return clazz;
    // }
    // }
    // break;
    // }
    // case Token.GETELEM:
    // {
    // // access a table
    // ElementGet get = (ElementGet) right;
    // // AstNode index = get.getElement();
    // AstNode target = get.getTarget();
    // Class<?> clazz = resolveArrayItemTypeComponent(target);
    // clazz = createArrayItemType(clazz, target);
    // return clazz;
    // }
    // }
    // return null;
    // }

    private Class<?> resolveArrayItemTypeComponent(AstNode node)
    {
        int type = node.getType();
        if (type == Token.NAME)
        {
            String varname = node.getString();
            VariableType clazz = getVariableDeclaration(varname, node.getAbsolutePosition());
            return clazz == null ? null : clazz.getClazz();
        }
        else if (type == Token.GETELEM)
        {
            return resolveArrayItemTypeComponent(((ElementGet) node).getTarget());
        }
        return null;
    }

    private Class<?> createArrayItemType(Class<?> clazz, AstNode node)
    {
        int type = node.getType();
        if (type == Token.GETELEM)
        {
            return createArrayItemType(clazz.getComponentType(), ((ElementGet) node).getTarget());
        }
        else if (type == Token.NAME)
        {
            clazz = clazz.getComponentType();
        }
        return clazz;
    }

    protected void addVariableDeclaration(String name, VariableType type, int offset)
    {
        ScriptVariable vc = localVariables.get(name);
        if (vc == null)
        {
            vc = new ScriptVariable();
            localVariables.put(name, vc);
        }
        vc.addType(offset, type);
    }

    // TODO use for variables with Token.VAR variables.
    protected void addVariableDeclaration(String name, VariableType type, int offsetBegin, int offsetEnd)
    {
        ScriptVariable vc = localVariables.get(name);
        if (vc == null)
        {
            vc = new ScriptVariable();
            localVariables.put(name, vc);
        }
        vc.addType(offsetBegin, offsetEnd, type);
    }

    @Override
    public Class<?> resolveClassDeclaration(String type)
    {
        Class<?> toReturn = null;
        int arraySize = 0;
        while (type.endsWith("[]"))
        {
            type = type.substring(0, type.length() - 2);
            arraySize++;
        }
        // try absolute
        if (type.contentEquals("Array"))
        {
            return NativeArray.class;
        }
        else if (type.contentEquals("String"))
        {
            return String.class;
        }
        try
        {
            if (type.startsWith("Packages."))
                type = type.substring("Packages.".length());
            toReturn = ClassUtil.findClass(type);
        }
        catch (ClassNotFoundException e)
        {
        }
        if (toReturn == null)
            toReturn = super.resolveClassDeclaration(type);
        if (toReturn == null)
            toReturn = getNativeJSTypes(type);
        while (toReturn != null && arraySize > 0)
        {
            toReturn = Array.newInstance(toReturn, 1).getClass();
            arraySize--;
        }
        return toReturn;
    }

    // FIXME
    /**
     * This is a workaround for JS functions and their Java equivalent. However,
     * this doesn't work properly, as those Classes have different methods (Java
     * String and JS String have different methods for instance).
     * 
     * @param type
     * @return
     */
    private Class<?> getNativeJSTypes(String type)
    {
        if (type.contentEquals("Math"))
        {
            return Math.class;
        }
        else if (type.contentEquals("File"))
        {
            return File.class;
        }
        else if (type.contentEquals("String"))
        {
            return String.class;
        }
        return null;
    }

    /**
     * Specific constructor not important, just the type
     * 
     * @param n
     *        : current node.
     * @param toReturn
     *        : text that will be returned in the end.
     * @return
     */
    private String generateClassName(Node n, String toReturn)
    {

        if (n != null)
        {
            if (n.getType() == Token.GETPROP)
            {
                String left = generateClassName(((PropertyGet) n).getLeft(), toReturn);
                String right = generateClassName(((PropertyGet) n).getRight(), toReturn);
                toReturn += left + (left.contentEquals("") ? "" : ".") + right;
            }
            else if (n.getType() == Token.NAME)
            {
                return n.getString();
            }
            else if (n.getType() == Token.GETELEM)
            {
                return generateClassName(((ElementGet) n).getTarget(), toReturn);
            }
        }
        return toReturn;
    }

    /**
     * Given the fact that the text can be different from {@link #textArea} .getText(), this method
     * returns the offset in the original text,
     * corresponding to the commandStartOffset in the modified text.
     * 
     * @param text
     *        : text with or without line modifications
     * @param offset
     *        : the offset wanted
     * @return : Returns the same offset if textArea is not a JTextArea, returns
     *         the correct one if it is.
     * @throws BadLocationException
     *         : An exception is raised of the offset does not exist in the
     *         original text, which should never happen.
     */
    public int getTextAreaOffset(String text, int offset) throws BadLocationException
    {
        if (textArea instanceof JTextArea)
        {
            JTextArea txtTmp = new JTextArea(text);
            int line = txtTmp.getLineOfOffset(offset);
            int offsetFromLine = offset - txtTmp.getLineStartOffset(line);
            return ((JTextArea) textArea).getLineStartOffset(line) + offsetFromLine;
        }
        else
            return offset;
    }

    /**
     * Dump the whole tree in the cconsole.
     * 
     * @param n
     * @param root
     * @param commandIdx
     * @param decal
     */
    private void dumpTree(Node n, AstRoot root, int commandIdx, String decal)
    {
        if (n == null)
            return;
        System.out.print(commandIdx + ": " + decal + typeToName(n.getType()));
        switch (n.getType())
        {
            case Token.EXPR_RESULT:
                System.out.println();
                AstNode expression = ((ExpressionStatement) n).getExpression();
                if (expression instanceof Assignment)
                {
                    Node left = ((Assignment) expression).getLeft();
                    Node right = ((Assignment) expression).getRight();
                    System.out.println("");
                    int level = commandIdx + 1;
                    dumpTree(left, root, level, "-" + decal);
                    dumpTree(right, root, level, "-" + decal);
                }
                else if (expression instanceof FunctionCall)
                {
                    List<AstNode> args = ((FunctionCall) expression).getArguments();
                    dumpTree(((FunctionCall) expression).getTarget(), root, commandIdx, "-" + decal);
                    for (AstNode arg : args)
                    {
                        dumpTree(arg, root, commandIdx, "-" + decal);
                    }
                }
                break;
            case Token.GETPROP:
                Node propLeft = ((PropertyGet) n).getLeft();
                Node propRight = ((PropertyGet) n).getRight();
                System.out.println(":");
                int level = commandIdx + 1;
                dumpTree(propLeft, root, level, "-" + decal);
                dumpTree(propRight, root, level, "-" + decal);
                break;
            case Token.BINDNAME:
                // System.out.println("bindname");
            case Token.STRING:
                StringLiteral str = (StringLiteral) n;
                System.out.println(": " + str.getValue());
                break;
            case Token.NAME:
                System.out.println(": " + n.getString());
                break;
            case Token.NUMBER:
                System.out.println(": " + n.getDouble());
                break;
            case Token.CALL:
                System.out.println();
                level = commandIdx + 1;
                FunctionCall fn = ((FunctionCall) n);
                for (AstNode arg : fn.getArguments())
                {
                    dumpTree(arg, root, level, decal);
                }
                dumpTree(fn.getTarget(), root, level, "-" + decal);
                break;
            case Token.FUNCTION:
                FunctionNode fn2 = (FunctionNode) n;
                System.out.print(" " + fn2.getName());
                dumpTree(fn2.getBody(), root, commandIdx + 1, "-" + decal);
                break;
            case Token.IF:
                System.out.println();
                IfStatement nIf = (IfStatement) n;
                level = commandIdx + 1;
                dumpTree(nIf.getThenPart(), root, level, "-" + decal);
                dumpTree(nIf.getElsePart(), root, level, "-" + decal);
                break;
            case Token.BLOCK:
                System.out.println();
                if (n instanceof Block)
                {
                    Block scope = (Block) n;
                    Node child = scope.getFirstChild();
                    level = commandIdx + 1;
                    while (child != null)
                    {
                        dumpTree(child, root, level, "-" + decal);
                        child = child.getNext();
                    }
                }
                else if (n instanceof Scope)
                {
                    Scope scope = (Scope) n;
                    Node child = scope.getFirstChild();
                    level = commandIdx + 1;
                    while (child != null)
                    {
                        dumpTree(child, root, level, "-" + decal);
                        child = child.getNext();
                    }
                }
                break;
            case Token.GETELEM:
                System.out.println();
                level = commandIdx + 1;
                ElementGet get = (ElementGet) n;
                dumpTree(get.getElement(), root, level, "-" + decal);
                dumpTree(get.getTarget(), root, level, "-" + decal);
                break;

            case Token.DO:
            case Token.FOR:
            case Token.WHILE:
                level = commandIdx + 1;
                dumpTree(((Loop) n).getBody(), root, commandIdx + 1, "-" + decal);
                break;
            case Token.SWITCH:
                level = commandIdx + 1;
                for (SwitchCase c : ((SwitchStatement) n).getCases())
                {
                    for (AstNode statement : c.getStatements())
                    {
                        dumpTree(statement, root, commandIdx + 1, decal);
                    }
                }
                break;
            default:
                System.out.println();
        }
        if (n.hasChildren())
        {
            Node child = n.getFirstChild();
            while (child != null)
            {
                dumpTree(child, root, commandIdx, "-" + decal);
                child = child.getNext();
                if (n == root)
                {
                    ++commandIdx;
                    System.out.println();
                }
            }
        }
    }

    private String buildFunction2(AstNode n)
    {
        String callName = "";

        callName = buildFunctionRecursive(callName, n);
        if (!callName.isEmpty())
        {
            // removes the last dot
            if (callName.startsWith("."))
                callName = callName.substring(1);

            // removes eventual "Packages." String
            if (callName.startsWith("Packages.") || callName.startsWith("packages."))
                callName = callName.substring("Packages.".length());

        }
        return callName;
    }

    /**
     * Recursive version
     */
    private String buildFunctionRecursive(String elem, AstNode n)
    {
        if (n != null)
        {
            int type;
            if ((type = n.getType()) == Token.GETPROP)
            {
                AstNode propLeft = ((PropertyGet) n).getLeft();
                AstNode propRight = ((PropertyGet) n).getRight();

                String left = buildFunctionRecursive(elem, propLeft);
                String right = buildFunctionRecursive(elem, propRight);
                return left + right + elem;
            }
            else if (type == Token.CALL)
            {
                FunctionCall fn = ((FunctionCall) n);
                String args = "";
                args += "(";
                int i = 0;
                for (AstNode arg : fn.getArguments())
                {
                    if (i != 0)
                        args += ",";
                    VariableType typeC = getRealType(arg);
                    if (typeC != null && typeC.getClazz() != null)
                        args += typeC.getClazz().getName();
                    else
                        args += "unknown";
                    i++;
                }
                args += ")";
                String functionName = "";
                AstNode target = fn.getTarget();
                String toReturn;
                int targetType = target.getType();
                if (targetType == Token.NAME)
                {
                    functionName = target.getString();
                    toReturn = functionName + args;
                }
                else if (targetType == Token.GETPROP)
                {
                    functionName = ((PropertyGet) target).getRight().getString();
                    toReturn = buildFunctionRecursive(elem, ((PropertyGet) target).getLeft()) + "." + functionName
                            + args;
                }
                else if (targetType == Token.GETELEM)
                {
                    ElementGet get = (ElementGet) target;
                    elem = buildFunctionRecursive("", get.getElement());
                    functionName = elem.substring(0, elem.indexOf('('));
                    String targetName = buildFunctionRecursive("", get.getTarget());
                    toReturn = targetName + "." + elem;
                }
                else
                    toReturn = elem;
                int rp = fn.getRp();
                if (rp != -1)
                {
                    rp = n.getAbsolutePosition() + rp + 1;
                    if (DEBUG)
                        System.out.println("function found:" + functionName);
                    IcyFunctionBlock fb = new IcyFunctionBlock(functionName, rp, null);
                    functionBlocksToResolve.add(fb);
                }
                return toReturn;
            }
            else if (type == Token.NAME)
            {
                // if (functionNext)
                // callName = "." + n.getString() + "()" + callName;
                // else
                return "." + n.getString();
                // return buildFunctionRecursive(callName, n.getFirstChild());
            }
            else if (type == Token.NUMBER)
            {
                return Number.class.getName();
            }
            else if (type == Token.STRING)
            {
                return ((StringLiteral) n).getValue();
            }
            else if (type == Token.GETELEM)
            {
                Class<?> clazz = resolveArrayItemTypeComponent(n);
                if (clazz != null)
                    return clazz.getCanonicalName();
            }
        }
        return elem;
    }

    /**
     * Get the type of a variable definition.
     * 
     * @param n
     * @param commandStartOffset
     * @param commandEndOffset
     * @return
     * @throws ScriptException
     */
    public VariableType getRealType(AstNode n)
    {
        if (n == null)
            return null;
        switch (n.getType())
        {
            case Token.ADD:
                // test if string
                InfixExpression expr = (InfixExpression) n;
                VariableType typeLeft = getRealType(expr.getLeft());
                VariableType typeRight = getRealType(expr.getRight());
                if ((typeLeft != null && typeLeft.getClazz() == String.class)
                        || (typeRight != null && typeRight.getClazz() == String.class))
                    return new VariableType(String.class);
            case Token.DIV:
            case Token.SUB:
            case Token.MUL:
            case Token.NUMBER:
                return new VariableType(Number.class);
            case Token.STRING:
                return new VariableType(String.class);
            case Token.TRUE:
            case Token.FALSE:
                return new VariableType(boolean.class);
            case Token.NAME:
                return getVariableDeclaration(n.getString(), n.getAbsolutePosition());
            case Token.CALL:
            {
                VariableType vt = resolveCallType(n, currentText, false);
                if (vt == null)
                {
                    updateGutter();
                }
                return vt;
            }
            case Token.FUNCTION:
                return new VariableType(Void.class);
            case Token.GETPROP:
            {
                // class wanted
                AstNode target = ((PropertyGet) n).getTarget();
                if (target.getType() == Token.GETELEM)
                {
                    // array
                    String rightStr = generateClassName(n, "");
                    // Class<?> clazz = resolveArrayItemTypeComponent(target);
                    // clazz = createArrayItemType(clazz, target);
                    if (rightStr.contentEquals("length"))
                        return new VariableType(int.class);
                }
                else
                {
                    // class
                    String className = generateClassName(n, "");
                    Class<?> clazz = resolveClassDeclaration(className);
                    if (clazz != null)
                        return new VariableType(clazz);
                    // try if it is an enum
                    int idx = className.lastIndexOf('.');
                    if (idx != -1)
                    {
                        clazz = resolveClassDeclaration(className.substring(0, idx));
                        return clazz == null ? null : new VariableType(clazz);
                    }
                }
                break;
            }
            case Token.ARRAYLIT:
                return new VariableType(Object[].class);
            case Token.NEW:
            {
                NewExpression nexp = (NewExpression) n;
                AstNode target = nexp.getTarget();
                if (target != null)
                {
                    String className = generateClassName(target, "");
                    Class<?> clazz = resolveClassDeclaration(className);
                    return clazz == null ? null : new VariableType(clazz);
                }
            }
            case Token.GETELEM:
            {
                // access a table
                ElementGet get = (ElementGet) n;
                // AstNode index = get.getElement();
                AstNode target = get.getTarget();
                Class<?> clazz = resolveArrayItemTypeComponent(target);
                if (clazz != null)
                    clazz = createArrayItemType(clazz, target);
                return clazz == null ? null : new VariableType(clazz);
            }
        }
        return null;
    }

    /**
     * <i>Extracted from rhino for debugging puproses.</i><br/>
     * Always returns a human-readable string for the token name. For instance, {@link #FINALLY} has
     * the name "FINALLY".
     * 
     * @param token
     *        the token code
     * @return the actual name for the token code
     */
    public String typeToName(int token)
    {
        switch (token)
        {
            case Token.EOF:
                return "EOF";
            case Token.EOL:
                return "EOL";
            case Token.ENTERWITH:
                return "ENTERWITH";
            case Token.LEAVEWITH:
                return "LEAVEWITH";
            case Token.RETURN:
                return "RETURN";
            case Token.GOTO:
                return "GOTO";
            case Token.IFEQ:
                return "IFEQ";
            case Token.IFNE:
                return "IFNE";
            case Token.SETNAME:
                return "SETNAME";
            case Token.BITOR:
                return "BITOR";
            case Token.BITXOR:
                return "BITXOR";
            case Token.BITAND:
                return "BITAND";
            case Token.EQ:
                return "EQ";
            case Token.NE:
                return "NE";
            case Token.LT:
                return "LT";
            case Token.LE:
                return "LE";
            case Token.GT:
                return "GT";
            case Token.GE:
                return "GE";
            case Token.LSH:
                return "LSH";
            case Token.RSH:
                return "RSH";
            case Token.URSH:
                return "URSH";
            case Token.ADD:
                return "ADD";
            case Token.SUB:
                return "SUB";
            case Token.MUL:
                return "MUL";
            case Token.DIV:
                return "DIV";
            case Token.MOD:
                return "MOD";
            case Token.NOT:
                return "NOT";
            case Token.BITNOT:
                return "BITNOT";
            case Token.POS:
                return "POS";
            case Token.NEG:
                return "NEG";
            case Token.NEW:
                return "NEW";
            case Token.DELPROP:
                return "DELPROP";
            case Token.TYPEOF:
                return "TYPEOF";
            case Token.GETPROP:
                return "GETPROP";
            case Token.SETPROP:
                return "SETPROP";
            case Token.GETELEM:
                return "GETELEM";
            case Token.SETELEM:
                return "SETELEM";
            case Token.CALL:
                return "CALL";
            case Token.NAME:
                return "NAME";
            case Token.NUMBER:
                return "NUMBER";
            case Token.STRING:
                return "STRING";
            case Token.NULL:
                return "NULL";
            case Token.THIS:
                return "THIS";
            case Token.FALSE:
                return "FALSE";
            case Token.TRUE:
                return "TRUE";
            case Token.SHEQ:
                return "SHEQ";
            case Token.SHNE:
                return "SHNE";
            case Token.REGEXP:
                return "REGEXP";
            case Token.BINDNAME:
                return "BINDNAME";
            case Token.THROW:
                return "THROW";
            case Token.RETHROW:
                return "RETHROW";
            case Token.IN:
                return "IN";
            case Token.INSTANCEOF:
                return "INSTANCEOF";
            case Token.LOCAL_LOAD:
                return "LOCAL_LOAD";
            case Token.GETVAR:
                return "GETVAR";
            case Token.SETVAR:
                return "SETVAR";
            case Token.CATCH_SCOPE:
                return "CATCH_SCOPE";
            case Token.ENUM_INIT_KEYS:
                return "ENUM_INIT_KEYS";
            case Token.ENUM_INIT_VALUES:
                return "ENUM_INIT_VALUES";
            case Token.ENUM_NEXT:
                return "ENUM_NEXT";
            case Token.ENUM_ID:
                return "ENUM_ID";
            case Token.THISFN:
                return "THISFN";
            case Token.RETURN_RESULT:
                return "RETURN_RESULT";
            case Token.ARRAYLIT:
                return "ARRAYLIT";
            case Token.OBJECTLIT:
                return "OBJECTLIT";
            case Token.GET_REF:
                return "GET_REF";
            case Token.SET_REF:
                return "SET_REF";
            case Token.DEL_REF:
                return "DEL_REF";
            case Token.REF_CALL:
                return "REF_CALL";
            case Token.REF_SPECIAL:
                return "REF_SPECIAL";
            case Token.DEFAULTNAMESPACE:
                return "DEFAULTNAMESPACE";
            case Token.ESCXMLTEXT:
                return "ESCXMLTEXT";
            case Token.ESCXMLATTR:
                return "ESCXMLATTR";
            case Token.REF_MEMBER:
                return "REF_MEMBER";
            case Token.REF_NS_MEMBER:
                return "REF_NS_MEMBER";
            case Token.REF_NAME:
                return "REF_NAME";
            case Token.REF_NS_NAME:
                return "REF_NS_NAME";
            case Token.TRY:
                return "TRY";
            case Token.SEMI:
                return "SEMI";
            case Token.LB:
                return "LB";
            case Token.RB:
                return "RB";
            case Token.LC:
                return "LC";
            case Token.RC:
                return "RC";
            case Token.LP:
                return "LP";
            case Token.RP:
                return "RP";
            case Token.COMMA:
                return "COMMA";
            case Token.ASSIGN:
                return "ASSIGN";
            case Token.ASSIGN_BITOR:
                return "ASSIGN_BITOR";
            case Token.ASSIGN_BITXOR:
                return "ASSIGN_BITXOR";
            case Token.ASSIGN_BITAND:
                return "ASSIGN_BITAND";
            case Token.ASSIGN_LSH:
                return "ASSIGN_LSH";
            case Token.ASSIGN_RSH:
                return "ASSIGN_RSH";
            case Token.ASSIGN_URSH:
                return "ASSIGN_URSH";
            case Token.ASSIGN_ADD:
                return "ASSIGN_ADD";
            case Token.ASSIGN_SUB:
                return "ASSIGN_SUB";
            case Token.ASSIGN_MUL:
                return "ASSIGN_MUL";
            case Token.ASSIGN_DIV:
                return "ASSIGN_DIV";
            case Token.ASSIGN_MOD:
                return "ASSIGN_MOD";
            case Token.HOOK:
                return "HOOK";
            case Token.COLON:
                return "COLON";
            case Token.OR:
                return "OR";
            case Token.AND:
                return "AND";
            case Token.INC:
                return "INC";
            case Token.DEC:
                return "DEC";
            case Token.DOT:
                return "DOT";
            case Token.FUNCTION:
                return "FUNCTION";
            case Token.EXPORT:
                return "EXPORT";
            case Token.IMPORT:
                return "IMPORT";
            case Token.IF:
                return "IF";
            case Token.ELSE:
                return "ELSE";
            case Token.SWITCH:
                return "SWITCH";
            case Token.CASE:
                return "CASE";
            case Token.DEFAULT:
                return "DEFAULT";
            case Token.WHILE:
                return "WHILE";
            case Token.DO:
                return "DO";
            case Token.FOR:
                return "FOR";
            case Token.BREAK:
                return "BREAK";
            case Token.CONTINUE:
                return "CONTINUE";
            case Token.VAR:
                return "VAR";
            case Token.WITH:
                return "WITH";
            case Token.CATCH:
                return "CATCH";
            case Token.FINALLY:
                return "FINALLY";
            case Token.VOID:
                return "VOID";
            case Token.RESERVED:
                return "RESERVED";
            case Token.EMPTY:
                return "EMPTY";
            case Token.BLOCK:
                return "BLOCK";
            case Token.LABEL:
                return "LABEL";
            case Token.TARGET:
                return "TARGET";
            case Token.LOOP:
                return "LOOP";
            case Token.EXPR_VOID:
                return "EXPR_VOID";
            case Token.EXPR_RESULT:
                return "EXPR_RESULT";
            case Token.JSR:
                return "JSR";
            case Token.SCRIPT:
                return "SCRIPT";
            case Token.TYPEOFNAME:
                return "TYPEOFNAME";
            case Token.USE_STACK:
                return "USE_STACK";
            case Token.SETPROP_OP:
                return "SETPROP_OP";
            case Token.SETELEM_OP:
                return "SETELEM_OP";
            case Token.LOCAL_BLOCK:
                return "LOCAL_BLOCK";
            case Token.SET_REF_OP:
                return "SET_REF_OP";
            case Token.DOTDOT:
                return "DOTDOT";
            case Token.COLONCOLON:
                return "COLONCOLON";
            case Token.XML:
                return "XML";
            case Token.DOTQUERY:
                return "DOTQUERY";
            case Token.XMLATTR:
                return "XMLATTR";
            case Token.XMLEND:
                return "XMLEND";
            case Token.TO_OBJECT:
                return "TO_OBJECT";
            case Token.TO_DOUBLE:
                return "TO_DOUBLE";
        }

        // Token without name
        throw new IllegalStateException(String.valueOf(token));
    }

    /**
     * <i>Extracted from rhino for debugging puproses.</i> Convert a keyword
     * token to a name string for use with the
     * {@link Context.FEATURE_RESERVED_KEYWORD_AS_IDENTIFIER} feature.
     * 
     * @param token
     *        A token
     * @return the corresponding name string
     */
    public static String keywordToName(int token)
    {
        switch (token)
        {
            case Token.BREAK:
                return "break";
            case Token.CASE:
                return "case";
            case Token.CONTINUE:
                return "continue";
            case Token.DEFAULT:
                return "default";
            case Token.DELPROP:
                return "delete";
            case Token.DO:
                return "do";
            case Token.ELSE:
                return "else";
            case Token.FALSE:
                return "false";
            case Token.FOR:
                return "for";
            case Token.FUNCTION:
                return "function";
            case Token.IF:
                return "if";
            case Token.IN:
                return "in";
            case Token.NEW:
                return "new";
            case Token.NULL:
                return "null";
            case Token.RETURN:
                return "return";
            case Token.SWITCH:
                return "switch";
            case Token.THIS:
                return "this";
            case Token.TRUE:
                return "true";
            case Token.TYPEOF:
                return "typeof";
            case Token.VAR:
                return "var";
            case Token.VOID:
                return "void";
            case Token.WHILE:
                return "while";
            case Token.WITH:
                return "with";
            case Token.CATCH:
                return "catch";
            case Token.FINALLY:
                return "finally";
            case Token.INSTANCEOF:
                return "instanceof";
            case Token.THROW:
                return "throw";
            case Token.TRY:
                return "try";
            default:
                return null;
        }
    }

    @Override
    public void format()
    {
        /*
         * This formatter uses beautify.js by Einar Lielmanis,
         * <einar@jsbeautifier.org> http://jsbeautifier.org/
         */
        InputStream is = PluginLoader
                .getResourceAsStream("plugins/tprovoost/scripteditor/resources/beautify/beautify.js");
        Reader reader = new BufferedReader(new InputStreamReader(is));
        Context context = Context.enter();
        context.setLanguageVersion(Context.VERSION_1_6);
        org.mozilla.javascript.ScriptableObject scope = context.initStandardObjects();

        try
        {
            context.evaluateReader(scope, reader, "Beautify", 1, null);
        }
        catch (IOException e)
        {
            return;
        }
        Function fct = (Function) scope.get("js_beautify", scope);

        // boolean preserveNewLines =
        // JSBeautifyOptions.getInstance().getOption("preserveNewLines",
        // true);
        // boolean useTabs =
        // JSBeautifyOptions.getInstance().getOption("useTabs", false);
        // boolean spaceBeforeConditional =
        // JSBeautifyOptions.getInstance().getOption("spaceBeforeConditional",
        // true);
        // boolean jslintHappy =
        // JSBeautifyOptions.getInstance().getOption("jslintHappy", false);
        // boolean indentCase =
        // JSBeautifyOptions.getInstance().getOption("indentCase", false);
        // int indentSize =
        // JSBeautifyOptions.getInstance().getOption("indentSize", 1);
        // String braceStyle =
        // JSBeautifyOptions.getInstance().getOption("braceStyle", "collapse");

        boolean preserveNewLines = true;
        boolean useTabs = true;
        boolean spaceBeforeConditional = true;
        boolean jslintHappy = false;
        boolean indentCase = false;
        int indentSize = 1;
        String braceStyle = "collapse";

        NativeObject properties = new NativeObject();

        if (useTabs)
        {
            properties.defineProperty("indent_char", "\t", NativeObject.READONLY);
            properties.defineProperty("indent_size", 1, NativeObject.READONLY);
        }
        else
        {
            int size = 4;
            if (indentSize == 0)
            {
                size = 2;
            }
            else if (indentSize == 1)
            {
                size = 4;
            }
            else
            {
                size = 8;
            }
            properties.defineProperty("indent_size", size, NativeObject.READONLY);
        }

        properties.defineProperty("preserve_newlines", preserveNewLines, NativeObject.READONLY);
        properties.defineProperty("max_preserve_newlines", false, NativeObject.READONLY);
        properties.defineProperty("jslint_happy", jslintHappy, NativeObject.READONLY);
        properties.defineProperty("space_before_conditional", spaceBeforeConditional, NativeObject.READONLY);
        properties.defineProperty("indent_case", indentCase, NativeObject.READONLY);

        properties.defineProperty("brace_style", braceStyle, NativeObject.READONLY);

        Object result = fct.call(context, scope, scope, new Object[] {textArea.getText(), properties});

        String finalText = result.toString();
        int caretPos = textArea.getCaretPosition();
        textArea.setText(finalText);
        if (caretPos > 0 && caretPos < finalText.length())
            textArea.setCaretPosition(caretPos);
    }

    @Override
    public ScriptEngine getEngine()
    {
        return ScriptEngineHandler.getEngine("javascript");
    }

    @Override
    public LinkGeneratorResult isLinkAtOffset(RSyntaxTextArea textArea, int offs)
    {
        return null;
    }

    @Override
    public void autoImport()
    {
        try
        {
            detectVariables(currentText);
        }
        catch (ScriptException e)
        {
            errors.setRuntimeError(e);
        }
    }
}
