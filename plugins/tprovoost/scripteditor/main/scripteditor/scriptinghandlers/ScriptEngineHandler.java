package plugins.tprovoost.scripteditor.scriptinghandlers;

import icy.gui.frame.progress.ProgressFrame;
import icy.plugin.PluginDescriptor;
import icy.plugin.PluginInstaller.PluginInstallerListener;
import icy.plugin.PluginLoader;
import icy.plugin.classloader.JarClassLoader;
import icy.util.ClassUtil;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.fife.ui.autocomplete.ParameterizedCompletion.Parameter;

import plugins.tprovoost.scripteditor.completion.IcyCompletionProvider;
import plugins.tprovoost.scripteditor.completion.types.ScriptFunctionCompletion;
import plugins.tprovoost.scripteditor.completion.types.ScriptFunctionCompletion.BindingFunction;
import plugins.tprovoost.scripteditor.scriptinghandlers.js.JSScriptEngine;
import plugins.tprovoost.scripteditor.scriptinghandlers.py.PyScriptEngine;

public class ScriptEngineHandler implements PluginInstallerListener
{

	/*---------------
	 *	   static
	 *---------------*/
	/**
	 * This {@link HashMap} is used to avoid multiple different engines for the
	 * same language to be initialized.
	 */
	private static final HashMap<String, ScriptEngine> engines = new HashMap<String, ScriptEngine>();

	/** The factory contains all the engines. */
	public static final ScriptEngineManager factory = new ScriptEngineManager(PluginLoader.getLoader());
	private static HashMap<ScriptEngine, ScriptEngineHandler> engineHandlers = new HashMap<ScriptEngine, ScriptEngineHandler>();
	private static ScriptEngineHandler lastEngineHandler = null;
	private static ArrayList<Method> bindingFunctions;
	private static ArrayList<String> allClasses = new ArrayList<String>();

	/*
	 * ------------ non static ------------
	 */
	private HashMap<String, VariableType> engineVariables = new HashMap<String, VariableType>();
	private HashMap<String, VariableType> engineFunctions = new HashMap<String, VariableType>();
	private ArrayList<String> engineDeclaredImports = new ArrayList<String>();
	private ArrayList<String> engineDeclaredImportClasses = new ArrayList<String>();
	private HashMap<Class<?>, ArrayList<ScriptFunctionCompletion>> engineTypesMethod = new HashMap<Class<?>, ArrayList<ScriptFunctionCompletion>>();;

	private ScriptEngineHandler()
	{
		if (bindingFunctions == null)
		{
			bindingFunctions = new ArrayList<Method>();
			findBindingMethodsPlugins();
		}
	}

	public static ScriptEngineHandler getLastEngineHandler()
	{
		return lastEngineHandler;
	}

	public static void setEngine(String engineType, ScriptEngine engine)
	{
		engines.put(engineType, engine);
	}

	public static ScriptEngineHandler getEngineHandler(ScriptEngine engine)
	{
		ScriptEngineHandler engineHandler = engineHandlers.get(engine);
		if (engineHandler == null)
		{
			engineHandler = new ScriptEngineHandler();
			engineHandlers.put(engine, engineHandler);
		}
		lastEngineHandler = engineHandler;
		return engineHandler;
	}

	public static ScriptEngine getEngine(String engineType)
	{
		return getEngine(engineType, false);
	}

	public static ScriptEngine getEngine(String engineType, boolean create)
	{
		String engineTypeL = engineType.toLowerCase();
		ScriptEngine engineHash = engines.get(engineTypeL);
		if (engineHash == null || create)
		{
			// engineHash = factory.getEngineByName(engineType);
			if (engineTypeL.contentEquals("javascript"))
			{
				engineHash = new JSScriptEngine();
				engines.put(engineTypeL, engineHash);
			} else if (engineTypeL.contentEquals("python"))
			{
				engineHash = new PyScriptEngine();
				engines.put(engineTypeL, engineHash);
			}
		}
		return engineHash;
	}

	public static void disposeEngine(ScriptEngine engine)
	{
		engine.clear();

		// remove the references to the engine
		engineHandlers.remove(engine);
	}

	// public ScriptEngine generateNewEngine() {
	// String engineType = engine.getFactory().getLanguageName();
	// ScriptEngine engine = factory.getEngineByName(engineType);
	// engines.put(engineType, engine);
	// this.engine = engine;
	// return engine;
	// }

	public ArrayList<String> getEngineDeclaredImportClasses()
	{
		return engineDeclaredImportClasses;
	}

	public ArrayList<String> getEngineDeclaredImports()
	{
		return engineDeclaredImports;
	}

	public HashMap<String, VariableType> getEngineFunctions()
	{
		return engineFunctions;
	}

	public HashMap<String, VariableType> getEngineVariables()
	{
		return engineVariables;
	}

	public HashMap<Class<?>, ArrayList<ScriptFunctionCompletion>> getEngineTypesMethod()
	{
		return engineTypesMethod;
	}

	public static ScriptEngineManager getFactory()
	{
		return factory;
	}

	private void findBindingMethodsPlugins()
	{
		ProgressFrame frame = new ProgressFrame("Loading functions...");
		try
		{
			try
			{
				allClasses.addAll(ClassUtil.findClassNamesInPackage("icy", true));
			} catch (IOException e)
			{
			}
			if (getClass().getClassLoader() instanceof JarClassLoader)
			{
				// Collection<Class<?>> col =
				// PluginLoader.getLoadedClasses().values();
				Collection<Class<?>> col = PluginLoader.getLoadedClasses().values();
				frame.setLength(col.size());
				int i = 0;
				for (Class<?> clazz : new ArrayList<Class<?>>(col))
				{
					findBindingsMethods(clazz);
					allClasses.add(clazz.getName());
					++i;
					frame.setPosition(i);
				}
			} else
			{
				ArrayList<PluginDescriptor> list = PluginLoader.getPlugins();
				frame.setLength(list.size());
				int i = 0;
				for (PluginDescriptor pd : list)
				{
					Class<?> clazz = pd.getPluginClass();
					// System.out.println(pd);
					findBindingsMethods(clazz);
					allClasses.add(clazz.getName());
					++i;
					frame.setPosition(i);
				}
			}
			Collections.sort(allClasses);
		} finally
		{
			// chrono.displayInSeconds();
			frame.close();
		}
	}

	public void findBindingsMethods(Class<?> clazz)
	{
		if (clazz == null)
			return;
		// get the annotated methods
		Method[] methods;
		try
		{
			methods = clazz.getDeclaredMethods();
		} catch (Error e)
		{
			return;
		}

		for (final Method method : methods)
		{

			// make sure the method is public and annotated
			int modifiers = method.getModifiers();
			if (!Modifier.isPublic(modifiers))
				continue;

			// is it an annotated with BindingFunction?
			BindingFunction blockFunction = method.getAnnotation(BindingFunction.class);
			if (blockFunction == null)
				continue;
			bindingFunctions.add(method);

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
			sfc.setDefinedIn(clazz.getName());
			sfc.setParams(fParams);
			sfc.setRelevance(2);

			if (engineFunctions != null)
			{
				Class<?> returnType = method.getReturnType();
				if (VariableType.isGeneric(returnType))
					engineFunctions.put(functionName, new VariableType(returnType, VariableType.getType(method.getGenericReturnType().toString())));
				else
					engineFunctions.put(functionName, new VariableType(returnType));
			}
			if (engineTypesMethod != null)
			{
				ArrayList<ScriptFunctionCompletion> methodsExisting = engineTypesMethod.get(clazz);
				if (methodsExisting == null)
					methodsExisting = new ArrayList<ScriptFunctionCompletion>();
				if (methodsExisting.contains(sfc))
					methodsExisting.remove(sfc);
				methodsExisting.add(sfc);
				engineTypesMethod.put(clazz, methodsExisting);
			}
		}
	}

	public ArrayList<Method> getFunctions()
	{
		return bindingFunctions;
	}

	// FIXME Although this is defined, the listener is never added !!
	@Override
	public void pluginInstalled(PluginDescriptor plugin, boolean success)
	{
		if (success)
		{
			bindingFunctions.clear();
			engineFunctions.clear();
			engineTypesMethod.clear();
			allClasses.clear();
			findBindingMethodsPlugins();
			// ArrayList<IcyFrame> list =
			// IcyFrame.getAllFrames(ScriptingEditor.class);
			// if (list != null && !list.isEmpty())
			// new AnnounceFrame("Binded functions in the current");
		}
	}

	// FIXME Although this is defined, the listener is never added !!
	@Override
	public void pluginRemoved(PluginDescriptor plugin, boolean success)
	{
		if (success)
		{
			bindingFunctions.clear();
			engineFunctions.clear();
			engineTypesMethod.clear();
			allClasses.clear();
			findBindingMethodsPlugins();
		}

	}

	/**
	 * Get all classes declared in plugins and icy. Language independant.
	 * 
	 * @return
	 */
	public static ArrayList<String> getAllClasses()
	{
		return allClasses;
	}

	/**
	 * Get the String language corresponding to the engine factory.<br/>
	 * Ex: ECMAScript factory returns JavaScript.
	 * 
	 * @param factory
	 * @return
	 */
	public static String getLanguageName(ScriptEngineFactory factory)
	{
		String languageName = factory.getLanguageName();
		if (languageName.contentEquals("ECMAScript"))
			return "JavaScript";
		if (languageName.contentEquals("python"))
			return "Python";
		return languageName;
	}

	public static void clearEngines()
	{
		for (ScriptEngine engine : engines.values())
		{
			disposeEngine(engine);
		}
		engines.clear();
	}
}
