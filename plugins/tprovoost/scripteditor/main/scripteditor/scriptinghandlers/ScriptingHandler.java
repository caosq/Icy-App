package plugins.tprovoost.scripteditor.scriptinghandlers;

import icy.gui.frame.progress.ProgressFrame;
import icy.plugin.PluginDescriptor;
import icy.plugin.PluginRepositoryLoader.PluginRepositoryLoaderListener;
import icy.plugin.abstract_.Plugin;
import icy.system.thread.ThreadUtil;
import icy.util.ClassUtil;
import icy.util.EventUtil;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.script.ScriptException;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.VariableCompletion;
import org.fife.ui.rsyntaxtextarea.LinkGenerator;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextArea;

import plugins.tprovoost.scripteditor.completion.IcyCompletionProvider;
import plugins.tprovoost.scripteditor.completion.types.BasicJavaClassCompletion;
import plugins.tprovoost.scripteditor.completion.types.ScriptFunctionCompletion;
import plugins.tprovoost.scripteditor.completion.types.ScriptFunctionCompletion.BindingFunction;
import plugins.tprovoost.scripteditor.gui.ConsoleOutput;
import plugins.tprovoost.scripteditor.gui.Preferences;
import plugins.tprovoost.scripteditor.gui.ScriptingPanel;
import plugins.tprovoost.scripteditor.main.ScriptListener;
import plugins.tprovoost.scripteditor.scriptingconsole.BindingsScriptFrame;

/**
 * This class is in charge of the compilation of the script. It mostly depends
 * on the provider.
 * 
 * @author Thomas Provoost
 */
public abstract class ScriptingHandler implements KeyListener, PluginRepositoryLoaderListener, LinkGenerator
{
	/**
	 * Reference to errors and warnings found when parsing or running the file.
	 */
	protected ScriptingErrors errors = new ScriptingErrors();
	
	/**
	 * List of the variable completions found when script was parsed. Functions
	 * and classes are considered as Variables too.
	 */
	protected ArrayList<Completion> variableCompletions = new ArrayList<Completion>();

	/** Reference to the provider used for the autocompletion. */
	protected DefaultCompletionProvider provider;

	/**
	 * Is the compilation a success? The script will never be run if the
	 * compilation / parsing contains issues.
	 */
	private boolean compilationOk = false;

	/** Reference to the current engine. */

	/** Reference to the {@link RSyntaxTextArea} this item works on. */
	protected JTextComponent textArea;

	/** Contains all declared variables in the script. */
	protected HashMap<String, ScriptVariable> localVariables;

	protected HashMap<String, ScriptVariable> externalVariables = new HashMap<String, ScriptVariable>();

	/** Contains all declared variables in the script. */
	protected HashMap<String, VariableType> localFunctions = new HashMap<String, VariableType>();

	/** Contains all declared importPackages in the script. */
	protected ArrayList<String> scriptDeclaredImports = new ArrayList<String>();

	/** Contains all declared importClasses in the script. */
	protected ArrayList<String> scriptDeclaredImportClasses = new ArrayList<String>();

	/** A specific offset contains an Function. */
	protected HashMap<Integer, IcyFunctionBlock> blockFunctions = new HashMap<Integer, IcyFunctionBlock>();

	/**
	 * This is where the warning / errors are displayed, contained in this
	 * scrollpane.
	 */
	protected ConsoleOutput errorOutput;

	/** Reference to the textarea scrollpane gutter. */
	private Gutter gutter;

	/** Filename of the script */
	protected String fileName = "Untitled";

	/** for debug purposes: advance will be used to load or not all functions */
	private boolean advanced;

	private boolean forceRun = false;
	private boolean newEngine = true;
	private boolean strict = false;
	private boolean varInterpretation = false;

	/**
	 * Thread running the evaluation.
	 */
	public EvalThread thread;

	private ArrayList<ScriptListener> listeners = new ArrayList<ScriptListener>();

	/** Turn to true if you need to display more information in the console. */
	protected static final boolean DEBUG = false;

	private AutoVerify autoverify = new AutoVerify();

	// Different relevance of items. Simplify code, but integer values can
	// always be used.
	public static final int RELEVANCE_MIN = 1;
	public static final int RELEVANCE_LOW = 2;
	public static final int RELEVANCE_HIGH = 10;

	private StringWriter sw = new StringWriter();
	private PrintWriter pw = new PrintWriter(sw, true)
	{
		@Override
		public synchronized void write(final String s)
		{
			if (errorOutput != null)
			{
				EventQueue.invokeLater(new Runnable()
				{

					@Override
					public void run()
					{
						errorOutput.append(s);
					}
				});
			} else
			{
				System.out.print(s);
			}
		}
	};

	public ScriptingHandler(DefaultCompletionProvider provider, String engineType, JTextComponent textArea, Gutter gutter, boolean forceRun,
			ScriptingPanel scriptingPanel)
	{
		this.provider = provider;
		this.textArea = textArea;
		this.gutter = gutter;
		this.forceRun = forceRun;
		setLanguage(engineType);

		textArea.getDocument().addDocumentListener(autoverify);

		if (textArea instanceof RSyntaxTextArea)
		{
			((RSyntaxTextArea) textArea).setLinkGenerator(this);
		}

		localVariables = new HashMap<String, ScriptVariable>();

		ScriptEngine engine = getEngine();
		if (engine == null)
		{
			return;
		}
		engine.setWriter(pw);
		engine.setErrorWriter(pw);
	}

	/**
	 * @param provider
	 *            : reference to the provider used by autocomplete. Cannot be
	 *            null.
	 * @param engineType
	 *            : as of now, only "JavaScript" or "Python".
	 * @param textArea2
	 *            : reference to the textArea. Cannot be null.
	 * @param gutter
	 *            : reference to the gutter attached to the scrollpane of the
	 *            textArea.
	 * @param autocompilation2
	 */
	public ScriptingHandler(DefaultCompletionProvider provider, String engineType, JTextComponent textArea, Gutter gutter)
	{
		this(provider, engineType, textArea, gutter, false);
	}

	/**
	 * @param provider
	 *            : reference to the provider used by autocomplete. Cannot be
	 *            null.
	 * @param engineType
	 *            : as of now, only "JavaScript" or "Python".
	 * @param textArea2
	 *            : reference to the textArea. Cannot be null.
	 * @param gutter
	 *            : reference to the gutter attached to the scrollpane of the
	 *            textArea.
	 * @param autocompilation2
	 */
	public ScriptingHandler(DefaultCompletionProvider provider, String engineType, JTextComponent textArea, Gutter gutter, boolean forceRun)
	{
		this(provider, engineType, textArea, gutter, forceRun, null);
	}

	public void setOutput(ConsoleOutput consoleOutput)
	{
		this.errorOutput = consoleOutput;
	}

	/**
	 * Ex: script.py or script.js
	 * 
	 * @param fileName
	 */
	public void setFileName(String fileName)
	{
		this.fileName = fileName;
	}

	/**
	 * Set the language of the Handler.
	 * 
	 * @param engineType
	 */
	private void setLanguage(String engineType)
	{
		try
		{
			installDefaultLanguageCompletions(engineType);

		} catch (ScriptException e)
		{
			e.printStackTrace();
		}
	}

	public HashMap<String, VariableType> getLocalFunctions()
	{
		return localFunctions;
	}

	public HashMap<String, ScriptVariable> getLocalVariables()
	{
		return localVariables;
	}

	public HashMap<Integer, IcyFunctionBlock> getBlockFunctions()
	{
		return blockFunctions;
	}

	public HashMap<String, ScriptVariable> getExternalVariables()
	{
		return externalVariables;
	}

	/**
	 * Get the variable type.
	 * 
	 * @param name
	 * @return
	 */
	public VariableType getVariableDeclaration(String name)
	{
		return getVariableDeclaration(name, textArea.getCaretPosition());
	}

	/**
	 * Get a variable declaration according to a specific offset
	 * 
	 * @param name
	 * @return
	 */
	public VariableType getVariableDeclaration(String name, int offset)
	{
		boolean isArray = name.contains("[");
		String originalName = name;
		if (isArray)
		{
			name = name.substring(0, name.indexOf('['));
		}
		ScriptVariable sv = localVariables.get(name);
		if (sv == null)
			return null;
		VariableType type = sv.getVariableClassType(offset);
		Class<?> typeC = null;
		if (type == null)
		{
			ScriptEngineHandler engineHandler = ScriptEngineHandler.getEngineHandler(getEngine());
			type = engineHandler.getEngineVariables().get(name);
		}
		if (type != null)
			typeC = type.getClazz();
		if (typeC != null)
		{
			if (isArray)
			{
				int occ = originalName.split("\\[").length - 1;
				for (int i = 0; i < occ; ++i)
				{
					typeC = typeC.getComponentType();
				}
			}
			// else if (type.getTypeParameters().length > 0)
			// {
			// System.out.println(name + " has generic Types:");
			// for (TypeVariable<?> t : type.getTypeParameters())
			// {
			// System.out.println(t);
			// }
			// }
		}
		if (type != null)
		{
			VariableType vt = new VariableType(typeC);
			vt.setType(type.getType());
			return vt;

		}
		return new VariableType((Class<?>) typeC);
	}

	public abstract void installDefaultLanguageCompletions(String language) throws ScriptException;

	/**
	 * Import all functions annotated with the {@link BindingFunction}
	 * annotation.
	 * 
	 * @throws ScriptException
	 */
	public void importFunctions() throws ScriptException
	{
		if (!(provider instanceof IcyCompletionProvider))
			return;
		advanced = false;
		if (advanced)
		{
			ThreadUtil.bgRun(new Runnable()
			{

				@SuppressWarnings("deprecation")
				@Override
				public void run()
				{
					ProgressFrame frame = new ProgressFrame("Loading functions...");
					((IcyCompletionProvider) provider).findAllMethods(getEngine(), frame);
					frame.setVisible(false);
				}
			});
		} else
		{
			// install functions from ScriptEngineHandler
			ScriptEngineHandler handler = ScriptEngineHandler.getEngineHandler(getEngine());
			ArrayList<Method> functions = handler.getFunctions();

			((IcyCompletionProvider) provider).installMethods(functions);
			installMethods(getEngine(), functions);
		}
	}

	public abstract void installMethods(ScriptEngine engine, ArrayList<Method> functions);

	/**
	 * Returns if should execute the code or not.
	 * 
	 * @return
	 */
	public boolean isCompilationOk()
	{
		return compilationOk;
	}

	/**
	 * Sets if should execute the code or not.
	 * 
	 * @param compilationOk
	 */
	private void setCompilationOk(boolean compilationOk)
	{
		this.compilationOk = compilationOk;
	}

	public boolean isNewEngine()
	{
		return newEngine;
	}

	public void setNewEngine(boolean newEngine)
	{
		this.newEngine = newEngine;
	}

	public boolean isForceRun()
	{
		return forceRun;
	}

	public void setForceRun(boolean forceRun)
	{
		this.forceRun = forceRun;
	}

	public boolean isStrict()
	{
		return strict;
	}

	public void setStrict(boolean strict)
	{
		this.strict = strict;
	}

	public void setVarInterpretation(boolean varInterpretation)
	{
		this.varInterpretation = varInterpretation;
	}

	/**
	 * Interpret the script. If <code>exec</code> is true, will try to run the
	 * code if compile is successful. Be careful: a building code is not
	 * necessary a functionnal running code.
	 * 
	 * @param runAfterCompile
	 *            : if true, runs the code.
	 * @param b
	 */
	public void interpret(boolean exec)
	{
		// use either selected text if any or all text
		String s = textArea.getSelectedText();
		if (s == null)
			s = textArea.getText();

		// interpret the code
		if (exec && forceRun)
		{
			run();
		} else
		{
			interpret(s);
			if (exec && (isCompilationOk()))
				run();
		}
	}

	protected void updateGutter()
	{
		ThreadUtil.invokeLater(new Runnable()
		{

			@Override
			public void run()
			{
				if (gutter == null || !(textArea instanceof JTextArea))
					return;
				gutter.removeAllTrackingIcons();

				errors.displayOnGutter(gutter);
			}
		});
	}

	/**
	 * This method interprets the code in one or two steps (depending on the
	 * user willing to immediately run the code or not). First, the code is
	 * Parsed. If any error occurs, the line containing it will be highlighted
	 * in the gutter.
	 * 
	 * @param forceRun
	 * @param s
	 *            : the code to interpret.
	 * @param exec
	 *            : run after compile or not.
	 * @throws ScriptException 
	 */
	protected void interpret(String s)
	{
		RTextArea textArea = new RTextArea();
		textArea.setText(s);
		try
		{
			clearScriptVariables();
			registerImports();
			if (varInterpretation)
			{
				errors.cleanup();
				detectVariables(s);
			}
			setCompilationOk(true);
		} catch (ScriptException e)
		{
			errors.setRuntimeError(e);
		}
		// update the icons for warning/errors now that the script has been interpreted
		updateGutter();
	}
	
	protected void addExternalVariables()
	{
		for (String s : externalVariables.keySet())
		{
			ScriptVariable sv = externalVariables.get(s);
			String type = sv.getVariableClassType(0).toString();
			VariableCompletion c = new VariableCompletion(provider, s, type);
			c.setRelevance(RELEVANCE_HIGH);
			variableCompletions.add(c);
		}
		localVariables.putAll(externalVariables);
	}

	@SuppressWarnings("unchecked")
	private synchronized void clearScriptVariables()
	{
		localVariables.clear();
		localFunctions.clear();
		scriptDeclaredImports.clear();

		for (String s : scriptDeclaredImportClasses)
		{
			try
			{
				BasicJavaClassCompletion c = new BasicJavaClassCompletion(provider, ClassUtil.findClass(s));
				s = c.getName();
				List<Completion> list = provider.getCompletionByInputText(s);
				if (list == null)
					continue;
				for (Completion c2 : new ArrayList<Completion>(list))
				{
					if (c2 instanceof VariableCompletion)
					{
						if (((VariableCompletion) c2).getName().contentEquals(s))
							provider.removeCompletion(c2);
					}
				}
			} catch (ClassNotFoundException e)
			{
			}
		}
		scriptDeclaredImportClasses.clear();
	}

	public void run()
	{
		ScriptEngine engine;
		
		if (isNewEngine())
		{
			if (errorOutput != null)
			{
				// Graphics2D g = (Graphics2D) errorOutput.getGraphics();
				// int chW = g.getFontMetrics().charWidth('_');
				// int w = errorOutput.getWidth();
				// int charCount = w / chW;
				// String str = "";
				// for (int i = 0; i < charCount - 1; ++i)
				// str += '-';
				// errorOutput.append(str + "\n");
				// errorOutput.append("New Engine created" + "\n");
				// errorOutput.append(str + "\n");
				if (Preferences.getPreferences().isAutoClearOutputEnabled())
				{
					errorOutput.clear();
				}
				// g.dispose();
			}
			engine = createNewEngine();

		} else
		{
			engine = getEngine();
		}
		
		thread = new EvalThread(engine, textArea.getText());
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
	}

	/**
	 * Creates a new engine for the current language. Will delete the previous
	 * one.
	 * 
	 * @return
	 */
	public ScriptEngine createNewEngine()
	{
		ScriptEngine oldEngine = getEngine();
		
		if (oldEngine != null)
		{
			// retrieve the methods known to the old engine to transfert them to the new engine
			ScriptEngineHandler engineHandler = ScriptEngineHandler.getEngineHandler(oldEngine);
			ArrayList<Method> functions = engineHandler.getFunctions();

			// unregister the old engine (will do the housekeeping)
			ScriptEngineHandler.disposeEngine(oldEngine);
			
			// create a new engine
			String newEngineType = oldEngine.getName();
			ScriptEngine newEngine = ScriptEngineHandler.getEngine(newEngineType, true);
			installMethods(newEngine, functions);
			try
			{
				installDefaultLanguageCompletions(newEngineType);
			} catch (ScriptException e)
			{
			}
			newEngine.setWriter(pw);
			newEngine.setErrorWriter(pw);
			
			return newEngine;
		}
		else
		{
			// Failed to retrieve the current engine
			return null;	
		}
	}

	/**
	 * Register all imports contained in the script.
	 * 
	 * @param s
	 */
	public abstract void registerImports();

	public abstract void autoDownloadPlugins();

	/**
	 * This method will detect the variables and add them to the provider.
	 * 
	 * @param s
	 * @throws ScriptException
	 */
	protected abstract void detectVariables(String s) throws ScriptException;

	@Override
	public void keyTyped(KeyEvent e)
	{
		// System.out.println("coucou");
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		switch (e.getKeyCode())
		{
		case KeyEvent.VK_F:
			if (EventUtil.isControlDown(e) && EventUtil.isShiftDown(e))
			{
				format();
			}
			break;
		case KeyEvent.VK_ENTER:
			if (EventUtil.isControlDown(e))
			{
				interpret(false);
				e.consume();
				break;
			} else if (EventUtil.isShiftDown(e))
			{
				break;
			}
			break;

		case KeyEvent.VK_R:
			if (EventUtil.isControlDown(e))
				interpret(true);
			break;

		case KeyEvent.VK_M:
			// if (EventUtil.isControlDown(e))
			// {
			// Bindings bindings =
			// getEngine().getBindings(ScriptContext.ENGINE_SCOPE);
			// for (String s : bindings.keySet())
			// {
			// // try {
			// // Object o = bindings.get(s);
			// // if (o instanceof NativeFunction) {
			// // System.out.print(s + ": ");
			// // engine.eval("print(" + s + ")");
			// // } else {
			// Object o = bindings.get(s);
			// System.out.println(s + " : " + o);
			// // }
			// // } catch (ScriptException e1) {
			// // System.out.println(s + " : " + bindings.get(s));
			// // }
			// }
			// // for (String s : localVariables.keySet()) {
			// // System.out.println(s + ": " + localVariables.get(s));
			// // }
			// }
			break;
		default:
			break;
		}
	}

	public void organizeImports()
	{
		organizeImports(textArea);
	}

	public abstract void organizeImports(JTextComponent textArea);

	@Override
	public void keyReleased(KeyEvent e)
	{
	}

	/**
	 * Returns the class corresponding to the type, depending on the imports:<br/>
	 * Javascript example:
	 * 
	 * <pre>
	 * {@code
	 * importPackage(Packages.javax.swing)
	 * importClass(Packages.java.util.Math)
	 * }
	 * </pre>
	 * 
	 * If the type asked is JButton, this will return javax.swing.JButton class.
	 * If the type asked is Math, this will return java.util.Math class.
	 * 
	 * @param type
	 * @return
	 */
	public Class<?> resolveClassDeclaration(String type)
	{
		// try with declared in the script importClass
		for (String s : scriptDeclaredImportClasses)
		{
			String className = ClassUtil.getSimpleClassName(s);
			int idx = className.indexOf('$');
			if (idx != -1)
				className = className.substring(idx + 1);
			if (className.contentEquals(type))
				try
				{
					return ClassUtil.findClass(s);
				} catch (ClassNotFoundException e)
				{
					// System.out.println(e.getLocalizedMessage());
				} catch (NoClassDefFoundError e2)
				{
				}
			int idxDollar = type.indexOf("$");
			if (type.contains(className) && idxDollar != -1)
				try
				{
					return ClassUtil.findClass(s + type.substring(idxDollar));
				} catch (ClassNotFoundException e)
				{
					// System.out.println(e.getLocalizedMessage());
				} catch (NoClassDefFoundError e2)
				{
				}
		}

		// try with declared in the script importPackage
		for (String s : scriptDeclaredImports)
		{
			try
			{
				return ClassUtil.findClass(s + "." + type);
			} catch (ClassNotFoundException e)
			{
			} catch (NoClassDefFoundError e2)
			{
			}
		}

		if (strict)
		{
			// declared in engine
			ScriptEngineHandler engineHandler = ScriptEngineHandler.getEngineHandler(getEngine());

			// try with declared in the engine importClass
			for (String s : engineHandler.getEngineDeclaredImportClasses())
			{
				if (ClassUtil.getSimpleClassName(s).contentEquals(type))
					try
					{
						return ClassUtil.findClass(s);
					} catch (ClassNotFoundException e)
					{
					} catch (NoClassDefFoundError e2)
					{
					}
			}

			// try with declared in the script importPackage
			for (String s : engineHandler.getEngineDeclaredImports())
			{
				try
				{
					return ClassUtil.findClass(s + "." + type);
				} catch (ClassNotFoundException e)
				{
				} catch (NoClassDefFoundError e2)
				{
				}
			}
		}
		return null;
	}

	public abstract ScriptEngine getEngine();

	/**
	 * @author thomasprovoost
	 */
	private class AutoVerify extends FocusAdapter implements DocumentListener, ActionListener
	{

		private Timer timer;
		private boolean lastChange;

		public AutoVerify()
		{
			timer = new Timer(1000, this);
			timer.setRepeats(false);
		}

		@Override
		public void insertUpdate(DocumentEvent e)
		{
			try
			{
				Document doc = e.getDocument();
				int offset = e.getOffset();
				int len = e.getLength();
				if (doc.getText(offset, len).contentEquals("."))
				{
					timer.stop();
					lastChange = false;
					String fullTxt = doc.getText(0, doc.getLength());
					String s = fullTxt.substring(0, offset);
					s += fullTxt.substring(offset + len, doc.getLength());

					interpret(s);
				}
			} catch (BadLocationException e1)
			{
				e1.printStackTrace();
			}
		}

		@Override
		public void removeUpdate(DocumentEvent e)
		{
		}

		@Override
		public void changedUpdate(DocumentEvent e)
		{
			lastChange = true;
			// System.out.println("changedUpdate");
			if (Preferences.getPreferences().isAutoBuildEnabled())
				timer.restart();
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			lastChange = false;
			interpret(false);
		}

		@Override
		public void focusLost(FocusEvent e)
		{
			timer.stop();
		}

		@Override
		public void focusGained(FocusEvent e)
		{
			if (lastChange)
				timer.restart();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void pluginRepositeryLoaderChanged(PluginDescriptor plugin)
	{
		if (plugin == null)
			return;
		Class<? extends Plugin> clazz;
		try
		{
			clazz = (Class<? extends Plugin>) ClassUtil.findClass(plugin.getClassAsString());
		} catch (ClassNotFoundException e)
		{
			return;
		}

		if (!plugin.isInstalled())
		{
			// uninstalled the plugin
			ScriptEngineHandler engineHandler = ScriptEngineHandler.getEngineHandler(getEngine());
			HashMap<Class<?>, ArrayList<ScriptFunctionCompletion>> engineTypesMethod = engineHandler.getEngineTypesMethod();
			engineTypesMethod.remove(clazz);
		} else
		{
			// plugin installed
			if (provider instanceof IcyCompletionProvider)
			{
				// ((IcyCompletionProvider)
				// provider).findBindingsMethods(engine, clazz);
			}
		}
	}

	public abstract void evalEngine(ScriptEngine engine, String content) throws ScriptException;

	/**
	 * @author Thomas Provoost
	 */
	public class EvalThread extends Thread
	{

		private String s;
		private ScriptEngine evalEngine;

		public EvalThread(ScriptEngine engine, String script)
		{
			this.evalEngine = engine;
			this.s = script;
		}

		@Override
		public void run()
		{
			fireEvaluationStarted();
			if (evalEngine != getEngine())
			{
				evalEngine.setWriter(pw);
				evalEngine.setErrorWriter(pw);
			}
			try
			{
				evalEngine(evalEngine, s);

				ScriptEngineHandler engineHandler = ScriptEngineHandler.getEngineHandler(getEngine());

				for (String key : localVariables.keySet())
					engineHandler.getEngineVariables().put(key, localVariables.get(key).getVariableLastClassType());
				engineHandler.getEngineFunctions().putAll(localFunctions);
				engineHandler.getEngineDeclaredImportClasses().addAll(scriptDeclaredImportClasses);
				engineHandler.getEngineDeclaredImports().addAll(scriptDeclaredImports);
				BindingsScriptFrame frame = BindingsScriptFrame.getInstance();
				frame.setEngine(evalEngine);
				frame.update();
			} catch (ThreadDeath td)
			{
				System.out.println("shutdown");
			} catch (final ScriptException e)
			{
				ThreadUtil.invokeLater(new Runnable()
				{

					@Override
					public void run()
					{
						errors.setRuntimeError(e);
						updateGutter();
					}
				});
			} finally
			{
				fireEvaluationOver();
				thread = null;
			}
		}
	}

	@SuppressWarnings("deprecation")
	public void killScript()
	{
		// Something is Running !
		if (thread != null)
		{
			thread.stop();
			thread = null;
		}
	}

	public void fireEvaluationStarted()
	{
		for (ScriptListener listener : new ArrayList<ScriptListener>(listeners))
			listener.evaluationStarted();
	}

	public void fireEvaluationOver()
	{
		for (ScriptListener listener : new ArrayList<ScriptListener>(listeners))
			listener.evaluationOver();
	}

	public boolean isRunning()
	{
		return thread != null;
	}

	public void addScriptListener(ScriptListener listener)
	{
		listeners.add(listener);
	}

	public void removeScriptListener(ScriptListener listener)
	{
		listeners.remove(listener);
	}

	/**
	 * Formats the text according to the handler.
	 */
	public abstract void format();

	public void stopThreads()
	{
		textArea.getDocument().removeDocumentListener(autoverify);
		killScript();
	}

	public static Method resolveMethod(Class<?> clazz, String name, Class<?>[] parameterTypes) throws SecurityException, NoSuchMethodException
	{
		try
		{
			return clazz.getMethod(name, parameterTypes);
		} catch (SecurityException e)
		{
		} catch (NoSuchMethodException e)
		{
		}
		L1: for (Method m : clazz.getMethods())
		{
			Class<?>[] types = m.getParameterTypes();
			if (m.getName().contentEquals(name) && types.length == parameterTypes.length)
			{
				// check types super etc
				for (int i = 0; i < types.length; ++i)
				{
					// if (types[i] == null || parameterTypes[i] == null ||
					// !types[i].isAssignableFrom(parameterTypes[i]))
					if (types[i] != null && parameterTypes[i] != null
							&& !(parameterTypes[i].isAssignableFrom(types[i]) || types[i].isAssignableFrom(parameterTypes[i])))
						continue L1;
				}
				return m;
			}
		}
		return clazz.getMethod(name, parameterTypes);
	}

	public void autoImport()
	{
		// default : do nothing
	}

}
