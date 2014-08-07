package plugins.tprovoost.scripteditor.completion;

import icy.util.ClassUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.text.JTextComponent;

import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.FunctionCompletion;
import org.fife.ui.autocomplete.ParameterizedCompletion.Parameter;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import plugins.tprovoost.scripteditor.completion.types.NewInstanceCompletion;
import plugins.tprovoost.scripteditor.completion.types.ScriptFunctionCompletion;
import plugins.tprovoost.scripteditor.scriptinghandlers.IcyFunctionBlock;
import plugins.tprovoost.scripteditor.scriptinghandlers.ScriptEngineHandler;
import plugins.tprovoost.scripteditor.scriptinghandlers.ScriptingHandler;
import plugins.tprovoost.scripteditor.scriptinghandlers.VariableType;
import plugins.tprovoost.scripteditor.scriptinghandlers.py.PythonScriptingHandler;

public class IcyCompletionProviderPython extends IcyCompletionProvider
{

	@Override
	protected List<Completion> getCompletionsImpl(JTextComponent comp)
	{
		// return completions;
		List<Completion> retVal = new ArrayList<Completion>();
		String text = getAlreadyEnteredTextWithFunc(comp);
		int lastIdx = text.lastIndexOf('.');

		ScriptEngineHandler engineHandler = ScriptEngineHandler.getLastEngineHandler();
		HashMap<String, VariableType> engineVariables = ScriptEngineHandler.getLastEngineHandler().getEngineVariables();

		// Cannot work directly because returns null on the provider.
		HashMap<Class<?>, ArrayList<ScriptFunctionCompletion>> engineTypesMethod = engineHandler.getEngineTypesMethod();
		HashMap<Integer, IcyFunctionBlock> localFunctions;
		if (handler != null)
			localFunctions = handler.getBlockFunctions();
		else
			localFunctions = new HashMap<Integer, IcyFunctionBlock>();

		if (text != null)
		{
			// test if inside parenthesis
			if (text.contains("("))
			{
				String text2 = String.copyValueOf(text.toCharArray());
				int idx;
				int i = 0;
				int pOpen = 0;
				int pClose = 0;
				while (i < text2.length() - 1 && (idx = text2.indexOf('(', i)) != -1)
				{
					++pOpen;
					i += idx + 1;
				}
				i = 0;
				while (i < text2.length() - 1 && (idx = text2.indexOf(')', i)) != -1)
				{
					++pClose;
					i += idx + 1;
				}
				int ppCount = pOpen - pClose;
				if (ppCount > 0)
				{
					text = text2.substring(text2.lastIndexOf('(') + 1);
					lastIdx = text.lastIndexOf('.');
				}
			}
			boolean containsNew = text.contains("new ");
			if (containsNew)
			{
				text = text.substring("new ".length());
				// add the classes
				if (text.length() > 0 && Character.isUpperCase(text.charAt(0)))
				{
					ArrayList<String> classes = ScriptEngineHandler.getAllClasses();
					for (String s : classes)
					{
						String nameFinal = ClassUtil.getSimpleClassName(s);
						int idxD = nameFinal.indexOf('$');
						if (idxD != -1)
						{
							nameFinal = nameFinal.substring(idxD + 1, nameFinal.length());
						}
						if (nameFinal.toLowerCase().startsWith(text.toLowerCase()))
						{
							try
							{
								Class<?> clazz = ClassUtil.findClass(s);
								if (Modifier.isStatic(clazz.getModifiers()))
									continue;
								for (Constructor<?> c : clazz.getConstructors())
								{
									int mod = c.getModifiers();
									if (Modifier.isPublic(mod))
									{
										NewInstanceCompletion fc = new NewInstanceCompletion(this, nameFinal, c);
										fc.setRelevance(ScriptingHandler.RELEVANCE_HIGH);

										// TODO relevance assignment = type /
										// expression = void
										fc.setDefinedIn(clazz.toString().replace('$', '.'));
										ArrayList<Parameter> params = new ArrayList<Parameter>();
										int i = 0;
										for (Class<?> clazzParam : c.getParameterTypes())
										{
											params.add(new Parameter(getType(clazzParam, true), "arg" + i));
											++i;
										}
										fc.setParams(params);
										retVal.add(fc);
									}
								}
							} catch (ClassNotFoundException e)
							{
							}
						}
					}
				}
			} else if (text.isEmpty() || lastIdx == -1)
			{
				doClassicCompletion(text, retVal);
			} else
			{
				// -----------------
				// Generate classes
				// -----------------
				if (text.startsWith("Packages."))
				{
					String clazzWanted = text.substring("Packages.".length());
					ArrayList<String> classes = ScriptEngineHandler.getAllClasses();
					for (String s : classes)
					{
						s = s.replace('$', '.');
						if (s.toLowerCase().startsWith(clazzWanted.toLowerCase()))
						{
							int startOffset = clazzWanted.lastIndexOf('.');
							int endOffset;
							BasicCompletion c;
							if (startOffset != -1)
							{
								endOffset = s.indexOf('.', startOffset + 1);
								if (endOffset != -1)
									c = new BasicCompletion(this, s.substring(startOffset + 1, endOffset));
								else
									c = new BasicCompletion(this, s.substring(startOffset + 1, s.length()));
							} else
							{
								endOffset = s.indexOf('.', 0);
								if (endOffset != -1)
									c = new BasicCompletion(this, s.substring(0, endOffset));
								else
									c = new BasicCompletion(this, s);
							}
							c.setRelevance(ScriptingHandler.RELEVANCE_MIN);
							if (!exists(c, retVal))
								retVal.add(c);
						}
					}
				}
				if (handler != null)
				{
					String command;
					if (lastIdx != -1)
					{
						command = text.substring(0, lastIdx);
						if (lastIdx <= text.length() - 1) // dot is before the
							// last
							text = text.substring(lastIdx + 1, text.length());
					} else
						command = text;
					ArrayList<ScriptFunctionCompletion> methods = null;

					// is the command a classname ?
					Class<?> clazz = handler.resolveClassDeclaration(command.replace('.', '$'));
					if (clazz != null)
					{
						// ----------------------------
						// STATIC ACCESS
						// ----------------------------
						if (containsNew)
						{
							populateWithConstructors(clazz, retVal);
						} else if ((methods = engineTypesMethod.get(clazz)) != null && !advanced)
						{
							for (ScriptFunctionCompletion complete : methods)
							{
								if (complete.isStatic() && (text.isEmpty() || complete.getName().toLowerCase().startsWith(text.toLowerCase())))
									retVal.add(generateSFCCopy(complete, true));
							}
						} else
						{
							populateClassTypes(new VariableType(clazz), text, retVal, true);
						}
					}

					// check in the local variables if it is a variable
					// if it is : propose depending on the variable type
					VariableType type = handler.getVariableDeclaration(command);
					if ((type != null && type.getClazz() != null) || ((type = engineVariables.get(command)) != null && type.getClazz() != null))
					{
						// ----------------------------
						// VARIABLE ACCESS
						// ----------------------------
						methods = engineTypesMethod.get(clazz);
						if (methods != null && !advanced)
						{
							for (ScriptFunctionCompletion complete : methods)
							{
								if (!complete.getName().toLowerCase().startsWith(text.toLowerCase()))
									continue;
								if (complete.isStatic())
									complete.setRelevance(ScriptingHandler.RELEVANCE_LOW);
								else if (!complete.isStatic())
									complete.setRelevance(ScriptingHandler.RELEVANCE_HIGH);
								if (text.isEmpty() || complete.getName().toLowerCase().startsWith(text.toLowerCase()))
									retVal.add(generateSFCCopy(complete));
							}
						} else
						{
							populateClassTypes(type, text, retVal);
						}
					}
					File f = ((PythonScriptingHandler) handler).getModules().get(command);
					if (f != null)
					{
						// TODO
						try
						{
							BufferedReader br = new BufferedReader(new FileReader(f));
							String textModule = "";
							String line;
							while ((line = br.readLine()) != null)
								textModule += line + "\n";

							RSyntaxTextArea textArea = new RSyntaxTextArea();
							textArea.setText(textModule);

							PythonScriptingHandler handlerModule = new PythonScriptingHandler(null, textArea, null, false);
							handlerModule.setVarInterpretation(true);
							handlerModule.setStrict(false);
							handlerModule.interpret(false);
							HashMap<String, VariableType> localFuncts = handlerModule.getLocalFunctions();

							for (String s : localFuncts.keySet())
							{
								type = localFuncts.get(s);
								FunctionCompletion fc = new FunctionCompletion(this, s, type == null ? "" : type.toString());
								fc.setDefinedIn(command);
								retVal.add(fc);
							}
							br.close();
						} catch (FileNotFoundException e)
						{
						} catch (IOException e)
						{
						}

					} else
					{
						// ----------------------------
						// FUNCTION ACCESS
						// ----------------------------
						// if not : look the type of the function (if declared).
						int startOffset = getStartOffset(comp) - 1;
						// System.out.println("offset:" + startOffset);
						// for (Integer i : localFunctions.keySet())
						// System.out.println(i);
						IcyFunctionBlock fb = localFunctions.get(startOffset);
						if (fb != null)
						{
							clazz = fb.getReturnType().getClazz();
							methods = engineTypesMethod.get(clazz);
							if (methods != null && !advanced)
							{
								for (ScriptFunctionCompletion complete : methods)
								{
									if (complete.isStatic())
										complete.setRelevance(ScriptingHandler.RELEVANCE_LOW);
									else if (!complete.isStatic())
										complete.setRelevance(ScriptingHandler.RELEVANCE_HIGH);
									if (text.isEmpty() || complete.getName().toLowerCase().startsWith(text.toLowerCase()))
										retVal.add(generateSFCCopy(complete));
								}
							} else
							{
								Type t = null;
								if (fb.getMethod() != null)
									t = fb.getMethod().getGenericReturnType();
								populateClassTypes(fb.getReturnType(), t, text, retVal, false);
							}
						} else
						{
							// Import feature.
							// doClassicCompletion(command, retVal, true);
						}
					}
				}
			}
		}
		return retVal;
	}
}
