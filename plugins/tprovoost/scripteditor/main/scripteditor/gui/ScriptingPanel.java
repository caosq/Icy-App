package plugins.tprovoost.scripteditor.gui;

import icy.file.FileUtil;
import icy.gui.frame.IcyFrame;
import icy.gui.frame.progress.AnnounceFrame;
import icy.gui.frame.progress.FailedAnnounceFrame;
import icy.main.Icy;
import icy.network.NetworkUtil;
import icy.plugin.PluginLoader;
import icy.plugin.PluginRepositoryLoader;
import icy.system.FileDrop;
import icy.system.FileDrop.FileDropListener;
import icy.system.thread.ThreadUtil;
import japa.parser.ast.body.ConstructorDeclaration;
import japa.parser.ast.body.MethodDeclaration;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.EventListener;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.EventListenerList;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;

import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.DescWindowCallback;
import org.fife.ui.autocomplete.ExternalURLHandler;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextScrollPane;

import plugins.tprovoost.scripteditor.completion.IcyAutoCompletion;
import plugins.tprovoost.scripteditor.completion.IcyCompletionCellRenderer;
import plugins.tprovoost.scripteditor.completion.IcyCompletionProvider;
import plugins.tprovoost.scripteditor.completion.IcyCompletionProviderPython;
import plugins.tprovoost.scripteditor.completion.JSAutoCompletion;
import plugins.tprovoost.scripteditor.completion.PythonAutoCompletion;
import plugins.tprovoost.scripteditor.completion.types.BasicJavaClassCompletion;
import plugins.tprovoost.scripteditor.completion.types.NewInstanceCompletion;
import plugins.tprovoost.scripteditor.completion.types.ScriptFunctionCompletion;
import plugins.tprovoost.scripteditor.javasource.ClassSource;
import plugins.tprovoost.scripteditor.javasource.JarAccess;
import plugins.tprovoost.scripteditor.main.ScriptListener;
import plugins.tprovoost.scripteditor.scriptblock.Javascript;
import plugins.tprovoost.scripteditor.scriptingconsole.BindingsScriptFrame;
import plugins.tprovoost.scripteditor.scriptinghandlers.ScriptingHandler;
import plugins.tprovoost.scripteditor.scriptinghandlers.js.JSScriptingHandlerRhino;
import plugins.tprovoost.scripteditor.scriptinghandlers.py.PythonScriptingHandler;

// import plugins.tprovoost.scripteditor.main.scriptinghandlers.JSScriptingHandler7;

public class ScriptingPanel extends JPanel implements ScriptListener
{
	/** */
	private static final long serialVersionUID = 1L;

	private ScriptingHandler scriptHandler;
	private RSyntaxTextArea textArea;
	private RTextScrollPane pane;
	private CommandPanel options;
	private String panelName;
	private String saveFileString = "";

	/** Default file used to save the content into */
	private File saveFile = null;

	/** Provider used for auto-completion. */
	private IcyCompletionProvider provider;
	/** Auto-completion system. Uses provider item. */
	private IcyAutoCompletion ac;

	private ConsoleOutput consoleOutput; // may be null !
	private boolean integrated;
	
	/**
	 * This listener is called when there are changes applied in the preferences window.
	 * It applies the preferences to the opened ScriptingPanels.
	 */
	private PreferencesListener applyPrefsListener = new PreferencesListener() {
		
		@Override
		public void preferencesChanged() {
			Preferences preferences = Preferences.getPreferences();

			textArea.setTabsEmulated(preferences.isSoftTabsEnabled());
			textArea.setTabSize(preferences.indentSpacesCount());
		}
	};
	
	// listen to changes of language in a child panel
	private ItemListener languageListener = new ItemListener()
	{

		@Override
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.SELECTED)
			{
				String language = (String) e.getItem();
				installLanguage(language);
			}
		}
	};
	
	private ActionListener runInNewListener = new ActionListener()
	{

		@Override
		public void actionPerformed(ActionEvent e)
		{
			runInNew();
		}
	};

	private ActionListener runInSameListener = new ActionListener()
	{

		@Override
		public void actionPerformed(ActionEvent e)
		{
			runInSame();
		}
	};
	
	private ActionListener stopListener = new ActionListener()
	{

		@Override
		public void actionPerformed(ActionEvent e)
		{
			stopScript();
		}
	};

	public ScriptingPanel(String name, String language)
	{
		this(name, language, true, null);
	}
	
	public ScriptingPanel(String name, String language, ConsoleOutput consoleOutput)
	{
		this(name, language, false, consoleOutput);
	}

	/**
	 * Creates a panel for scripting, using an {@link RSyntaxTextArea} for the
	 * text and {@link Gutter} to display line numbers and errors. Error is
	 * shown in the output window as a {@link JTextArea} in a
	 * {@link JScrollPane}.
	 * 
	 * @param name
	 */
	public ScriptingPanel(String name, String language, boolean integrated, ConsoleOutput consoleOutput)
	{
		this.panelName = name;
		this.integrated = integrated;
		this.consoleOutput = consoleOutput;
		
		setLayout(new BorderLayout());

		// creates the text area and set it up
		textArea = new RSyntaxTextArea(20, 60);
		textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
		// textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
		textArea.setCodeFoldingEnabled(true);
		textArea.setAntiAliasingEnabled(true);
		textArea.setAutoIndentEnabled(true);
		textArea.setCloseCurlyBraces(true);
		textArea.setMarkOccurrences(true);
		textArea.setCodeFoldingEnabled(true);
		textArea.setPaintMarkOccurrencesBorder(true);
		textArea.setPaintMatchedBracketPair(true);
		textArea.setPaintTabLines(true);
		textArea.setTabsEmulated(false);
		textArea.addHyperlinkListener(new HyperlinkListener()
		{

			@Override
			public void hyperlinkUpdate(HyperlinkEvent e)
			{
				NetworkUtil.openBrowser(e.getURL());
			}

		});
		textArea.getDocument().addDocumentListener(new DocumentListener() {
			
			@Override
			public void removeUpdate(DocumentEvent arg0) {
				updateTitle();
			}
			
			@Override
			public void insertUpdate(DocumentEvent arg0) {
				updateTitle();
			}
			
			@Override
			public void changedUpdate(DocumentEvent arg0) {
				// this is fired when the style changes
				// ignore
			}
		});

		pane = new RTextScrollPane(textArea);
		pane.setIconRowHeaderEnabled(true);

		// creates the options panel
		options = new CommandPanel(language);
		installLanguage(options.getLanguage());
		options.addLanguageListener(languageListener);
		options.addRunInSameListener(runInSameListener);
		options.addRunInNewListener(runInNewListener);
		options.addStopListener(stopListener);

		// set the default theme: eclipse.
		setTheme("eclipse");
		textArea.requestFocus();
		
		// load the preferences
		applyPrefsListener.preferencesChanged();
		
		// frame listener on preferences
		PreferencesWindow.getPreferencesWindow().addPreferencesListener(applyPrefsListener);
	}

	/**
	 * Getter for the provider
	 * 
	 * @return
	 */
	public IcyCompletionProvider getProvider()
	{
		return provider;
	}

	/**
	 * Setter for the provider.
	 * 
	 * @param provider
	 */
	public void setProvider(IcyCompletionProvider provider)
	{
		this.provider = provider;
	}

	public RSyntaxTextArea getTextArea()
	{
		return textArea;
	}

	public RTextScrollPane getPane()
	{
		return pane;
	}

	public String getPanelName()
	{
		return panelName;
	}

	public void setSyntax(String syntaxType)
	{
		textArea.setSyntaxEditingStyle(syntaxType);
	}

	/**
	 * Install the wanted theme.
	 * 
	 * @param s
	 */
	public void setTheme(String s)
	{
		try
		{
			Theme t = Theme.load(PluginLoader.getLoader().getResourceAsStream("plugins/tprovoost/scripteditor/resources/themes/" + s + ".xml"));
			t.apply(textArea);
		} catch (IOException e)
		{
			System.out.println("Couldn't load theme");
		}
	}

	/**
	 * Save the data into the file f.
	 * 
	 * @param f
	 * @return
	 */
	public boolean saveFileAs(File f)
	{
		if (f != null)
		{
			BufferedWriter writer = null;
			try
			{
				String s = textArea.getText();
				writer = new BufferedWriter(new FileWriter(f));
				writer.write(s);
				writer.close();
				saveFile = f;
				saveFileString = s;
				panelName = f.getName();
				scriptHandler.setFileName(f.getAbsolutePath());
				updateTitle();
				fireSavedAs(f);
				return true;
			} catch (IOException e)
			{
				new FailedAnnounceFrame(e.getLocalizedMessage());
				return false;
			}
		}
		return false;
	}
	
	// listener used to propagate the "Saved As" action up to the editor
	// without making the ScriptingPanel dependent on the editor
	public static interface SavedAsListener extends EventListener {
		void savedAs(File f);
	}
	
    private final EventListenerList savedAslisteners = new EventListenerList();
    
    public void addSavedAsListener(SavedAsListener listener) {
    	savedAslisteners.add(SavedAsListener.class, listener);
    }
 
    public void removeSavedAsListener(SavedAsListener listener) {
    	savedAslisteners.remove(SavedAsListener.class, listener);
    }
    
    protected void fireSavedAs(File f) {
    	for(SavedAsListener listener : getSavedAsListeners()) {
    		listener.savedAs(f);
    	}
    }
    
    public SavedAsListener[] getSavedAsListeners() {
        return savedAslisteners.getListeners(SavedAsListener.class);
    }

	/**
	 * Save the file is dirty. Returns true if success or not dirty.
	 * 
	 * @return
	 */
	public boolean saveFile()
	{
		if (!isDirty())
			return true;
		if (saveFile != null)
		{
			BufferedWriter writer = null;
			try
			{
				String s = textArea.getText();
				writer = new BufferedWriter(new FileWriter(saveFile));
				writer.write(s);
				writer.close();
				saveFileString = s;
				updateTitle();
				return true;
			} catch (IOException e)
			{
				new FailedAnnounceFrame(e.getLocalizedMessage());
				return false;
			}
		}
		return false;
	}

	/**
	 * Displays a JFileChoose and let the user choose its file, then open it.
	 * 
	 * @see ScriptingEditor#openFile(File)
	 */
	public boolean showSaveFileDialog(String currentDirectoryPath)
	{
		return showSaveFileDialog(currentDirectoryPath, panelName);
	}

	/**
	 * Displays a JFileChoose and let the user choose its file, then open it.
	 * 
	 * @see ScriptingEditor#openFile(File)
	 */
	public boolean showSaveFileDialog(String currentDirectoryPath, String defaultName)
	{
		final JFileChooser fc;
		if (currentDirectoryPath == "")
			fc = new JFileChooser();
		else
			fc = new JFileChooser(currentDirectoryPath);
		if (getLanguage().contentEquals("JavaScript"))
		{
			fc.setFileFilter(new FileNameExtensionFilter("Javascript files", "js"));
		} else if (getLanguage().contentEquals("Python"))
		{
			fc.setFileFilter(new FileNameExtensionFilter("Python files", "py"));
		}
		fc.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				fc.accept(fc.getSelectedFile());
				fc.removeKeyListener(this);
			}
		});
		if (!defaultName.isEmpty())
			fc.setSelectedFile(new File(defaultName));
		if (fc.showSaveDialog(Icy.getMainInterface().getMainFrame()) == JFileChooser.APPROVE_OPTION)
		{
			File file = fc.getSelectedFile();
			if (FileUtil.getFileExtension(file.getAbsolutePath(), false).isEmpty())
			{
				file = addExtension(file);
			}
			return saveFileAs(file);
		}
		return false;
	}

	private File addExtension(File file)
	{
		if (getLanguage().contentEquals("JavaScript"))
		{
			return new File(file.getAbsolutePath() + ".js");
		} else if (getLanguage().contentEquals("Python"))
		{
			return new File(file.getAbsolutePath() + ".py");
		}
		return file;
	}
	
	// listener used to propagate the "Saved As" action up to the editor
	// without making the ScriptingPanel dependent on the editor
	public static interface TitleChangedListener extends EventListener {
		void titleChanged(ScriptingPanel panel, String title);
	}
	
    private final EventListenerList titleChangedListeners = new EventListenerList();
    
    public void addTitleChangedListener(TitleChangedListener listener) {
    	titleChangedListeners.add(TitleChangedListener.class, listener);
    }
 
    public void removeTitleChangedListener(TitleChangedListener listener) {
    	titleChangedListeners.remove(TitleChangedListener.class, listener);
    }
    
    protected void fireTitleChanged(String title) {
    	for(TitleChangedListener listener : getTitleChangedListeners()) {
    		listener.titleChanged(this, title);
    	}
    }
    
    public TitleChangedListener[] getTitleChangedListeners() {
        return titleChangedListeners.getListeners(TitleChangedListener.class);
    }

	private void updateTitle()
	{
		String title;
		if (isDirty())
			title = panelName + "*";
		else
			title = panelName;
		fireTitleChanged(title);
	}
	
	/**
	 * Ask the user whether to save the files.
	 * 
	 * @param defaultSaveDirectory Default directory
	 * 
	 * @return false is the close operation is cancelled
	 */
	boolean promptSave(String defaultSaveDirectory)
	{
        int n = JOptionPane.showOptionDialog(Icy.getMainInterface().getMainFrame(),
                "Some work has not been saved, are you sure you want to close?", getPanelName(),
                JOptionPane.WARNING_MESSAGE, JOptionPane.QUESTION_MESSAGE, null, new Object[] {"Save",
                        "Discard Changes", "Cancel"}, "Cancel" + "");
        if (n == 2)
        	return false;
        if (n == 0)
        {
            if (getSaveFile() != null)
            	return saveFile();
            else
            	return showSaveFileDialog(defaultSaveDirectory);
        }
        
        return true;
	}
	
	/**
	 * Try to close this ScriptingPanel. The user will be asked whether to save the files
	 * if they are modified. If the operation is not cancelled, the listeners are removed.
	 * 
	 * @param defaultSaveDirectory Default directory used if a file need to be saved
	 * 
	 * @return false is the close operation is cancelled
	 */
	boolean close(String defaultSaveDirectory)
	{
		boolean canClose = true;
        if (isDirty())
        {
        	canClose = promptSave(defaultSaveDirectory);
        }

        if (canClose)
        {
        	cleanup();
    		PreferencesWindow.getPreferencesWindow().removePreferencesListener(applyPrefsListener);
    		options.removeLanguageListener(languageListener);
    		options.removeRunInSameListener(runInSameListener);
    		options.removeRunInNewListener(runInNewListener);
    		options.removeStopListener(stopListener);
        	return true;
        }
        else
        {
        	return false;
        }
	}
	
	/**
	 * Removes the script listeners, release completion objects...
	 */
	void cleanup()
	{
		// Autocompletion is done with the following item
		if (scriptHandler != null)
		{
			scriptHandler.stopThreads();
			scriptHandler.removeScriptListener(this);
			textArea.removeKeyListener(scriptHandler);
			PluginRepositoryLoader.removeListener(scriptHandler);
		}
		
		if (ac != null)
		{
			ac.uninstall();
		}
	}

	/**
	 * Install the wanted language in the text area: creates the provider and
	 * its default auto-complete words.
	 * 
	 * @param language
	 *            : javascript / ruby / python / etc.
	 */
	public synchronized void installLanguage(final String language)
	{
		final Preferences preferences = Preferences.getPreferences();

		cleanup();

		// the provider provides the results when hitting Ctrl + Space.
		if (provider == null)
		{
			if (language.contentEquals("Python"))
			{
				provider = new IcyCompletionProviderPython();
			} else
			{
				provider = new IcyCompletionProvider();
			}
			boolean autoActivate = preferences.isFullAutoCompleteEnabled();
			provider.setAutoActivationRules(autoActivate, ".");
			ThreadUtil.invokeLater(new Runnable()
			{

				@Override
				public void run()
				{
					provider.setListCellRenderer(new IcyCompletionCellRenderer());
				}
			});
		}
		provider.clear();

		// set the syntax
		if (language.contentEquals("JavaScript"))
		{
			// setSyntax(SyntaxConstants.SYNTAX_STYLE_JAVA);
			setSyntax(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
			ac = new JSAutoCompletion(provider);
			ThreadUtil.invokeLater(new Runnable()
			{

				@Override
				public void run()
				{
					options.setRunButtonsEnabled(true);
				}
			});
		} else if (language.contentEquals("Python"))
		{
			setSyntax(SyntaxConstants.SYNTAX_STYLE_PYTHON);
			ac = new PythonAutoCompletion(provider);
			ThreadUtil.invokeLater(new Runnable()
			{

				@Override
				public void run()
				{
					options.setRunButtonsEnabled(true);
				}
			});
		} else
		{
			setSyntax(SyntaxConstants.SYNTAX_STYLE_NONE);
			new AnnounceFrame("This language is not yet supported.");
			ThreadUtil.invokeLater(new Runnable()
			{

				@Override
				public void run()
				{
					options.setRunButtonsEnabled(false);
				}
			});
			return;
		}

		// install the default completion words: eg. "for", "while", etc.
		provider.installDefaultCompletions(language);

		// install the text area with the completion system.
		ac.install(textArea);
		ac.setAutoCompleteSingleChoices(false);
		ac.setParameterAssistanceEnabled(true);
		ac.setAutoActivationEnabled(true);
		ac.setAutoActivationDelay(500);
		ac.setShowDescWindow(true);
		ac.setExternalURLHandler(new ExternalURLHandler()
		{
			@Override
			public void urlClicked(HyperlinkEvent e, Completion c, DescWindowCallback callback)
			{
				if (e.getDescription().contentEquals("SourceCodeLink"))
				{
					Class<?> clazz = null;
					if (c instanceof BasicJavaClassCompletion)
					{
						clazz = ((BasicJavaClassCompletion) c).getJavaClass();
						openSource(clazz);
					} else if (c instanceof ScriptFunctionCompletion)
					{
						ScriptFunctionCompletion sfc = (ScriptFunctionCompletion) c;
						Method m = sfc.getMethod();
						clazz = m.getDeclaringClass();
						final ClassSource cs = ClassSource.getClassSource(clazz);
						MethodDeclaration md = cs.getMethods().get(m.toGenericString());
						openSource(clazz, md.getBeginLine() - 1, md.getEndLine() - 1);
					} else if (c instanceof NewInstanceCompletion)
					{
						Constructor<?> cons = ((NewInstanceCompletion) c).getConstructor();
						clazz = cons.getDeclaringClass();
						final ClassSource cs = ClassSource.getClassSource(clazz);
						ConstructorDeclaration cd = cs.getConstructors().get(cons.toGenericString());
						if (cd != null)
							openSource(clazz, cd.getBeginLine() - 1, cd.getEndLine() - 1);
						else
							System.out.println(clazz);
					}
				} else
				{
					System.out.println("Click:" + e.getDescription());
					// callback.showSummaryFor(new BasicJavaCl, "");
				}
			}
		});
		ThreadUtil.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				// add the scripting handler, which handles the compilation
				// and the parsing of the code for advanced features.
				if (language.contentEquals("JavaScript"))
				{
					// if
					// (System.getProperty("java.version").startsWith("1.6.")) {
					scriptHandler = new JSScriptingHandlerRhino(provider, textArea, pane.getGutter(), true);
					// } else {
					// scriptHandler = new JSScriptingHandlerSimple(provider,
					// textArea,
					// pane.getGutter(), true);
					// }
				} else if (language.contentEquals("Python"))
				{
					scriptHandler = new PythonScriptingHandler(provider, textArea, pane.getGutter(), true);
				} else
				{
					scriptHandler = null;
				}
				if (scriptHandler != null)
				{
					if (!integrated && consoleOutput != null)
						scriptHandler.setOutput(consoleOutput);
					
					scriptHandler.addScriptListener(ScriptingPanel.this);
					scriptHandler.setVarInterpretation(preferences.isVarInterpretationEnabled());
					scriptHandler.setStrict(preferences.isStrictModeEnabled());
					scriptHandler.setForceRun(preferences.isOverrideEnabled());
					// scriptHandler.interpret(false);
					provider.setHandler(scriptHandler);
					textArea.addKeyListener(scriptHandler);
					PluginRepositoryLoader.addListener(scriptHandler);

					BindingsScriptFrame frame = BindingsScriptFrame.getInstance();
					frame.setEngine(scriptHandler.getEngine());
				}
				rebuildGUI();
				textArea.requestFocus();
			}
		});
	}

	public void openSource(Class<?> clazz)
	{
		openSource(clazz, 0, 0);
	}

	public void openSource(Class<?> clazz, int lineBegin, int lineEnd)
	{
		InputStream jar = JarAccess.getJavaSourceInputStream(clazz);
		if (jar != null)
		{
			String res = null;
			try
			{
				byte b[] = new byte[jar.available()];
				jar.read(b);
				res = new String(b);
			} catch (IOException e1)
			{
			}
			if (res == null)
				return;

			// creates the dialog
			IcyFrame frame = new IcyFrame("Source code of: [" + clazz.getName() + "]", true, true, true, true);
			JPanel panel = new JPanel(new BorderLayout());

			// Generation of the RSyntaxTextArea
			final RSyntaxTextArea sourceTextArea = new RSyntaxTextArea(200, 200);
			sourceTextArea.setText(res);
			// sourceTextArea.setEditable(false);
			sourceTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
			sourceTextArea.setCodeFoldingEnabled(true);
			sourceTextArea.setAntiAliasingEnabled(true);
			sourceTextArea.setAutoIndentEnabled(true);
			sourceTextArea.setCloseCurlyBraces(true);
			sourceTextArea.setMarkOccurrences(true);
			sourceTextArea.setCodeFoldingEnabled(true);
			sourceTextArea.setPaintMarkOccurrencesBorder(true);
			sourceTextArea.setPaintMatchedBracketPair(true);
			sourceTextArea.setPaintTabLines(true);
			sourceTextArea.setTabsEmulated(false);

			// Change the theme to Eclipse
			try
			{
				Theme t = Theme.load(PluginLoader.getLoader().getResourceAsStream("plugins/tprovoost/scripteditor/resources/themes/eclipse.xml"));
				t.apply(sourceTextArea);
			} catch (IOException e2)
			{
			}

			// Add the RSyntaxTextArea to a scroll pane
			RTextScrollPane paneSource = new RTextScrollPane(sourceTextArea);
			panel.add(paneSource);
			frame.setContentPane(panel);
			frame.setSize(720, 640);
			frame.addToMainDesktopPane();
			frame.setVisible(true);

			// Put the cursor at the right place
			int posCaretBegin = 0;
			int posCaretEnd = 0;
			try
			{
				posCaretBegin = sourceTextArea.getLineStartOffset(lineBegin);
				posCaretEnd = sourceTextArea.getLineStartOffset(lineEnd);
			} catch (BadLocationException e)
			{
			}
			sourceTextArea.getCaret().setDot(posCaretEnd);
			final int begin = posCaretBegin;
			ThreadUtil.bgRun(new Runnable()
			{

				@Override
				public void run()
				{
					ThreadUtil.sleep(100);
					sourceTextArea.getCaret().setDot(begin);
				}
			});
		}
	}

	/**
	 * Getter of the handler.
	 * 
	 * @return
	 */
	public ScriptingHandler getScriptHandler()
	{
		return scriptHandler;
	}

	/**
	 * Setter for the handler. Should only be used for reference to the same
	 * script handler in different panels. For instance with the
	 * {@link Javascript} block.
	 * 
	 * @param scriptHandler
	 */
	public void setScriptHandler(ScriptingHandler scriptHandler)
	{
		this.scriptHandler = scriptHandler;
	}

	/**
	 * Get the defaut save location.
	 * 
	 * @return
	 */
	public File getSaveFile()
	{
		return saveFile;
	}

	/**
	 * Returns if the file has been modified since its last save.
	 * 
	 * @return
	 */
	public boolean isDirty()
	{
		String currentText = textArea.getText();
		return (saveFile == null && !currentText.isEmpty()) || !saveFileString.contentEquals(currentText);
	}

	/**
	 * Rebuild the whole GUI.
	 */
	private void rebuildGUI()
	{
		removeAll();
		add(pane);

		if (!integrated)
			add(options, BorderLayout.NORTH);
		revalidate();
	}

	/**
	 * Sets the text in the textArea.
	 * 
	 * @param text
	 */
	public void setText(String text)
	{
		textArea.setText(text);
	}

	/**
	 * Get the current selected language in the combobox.
	 * 
	 * @return
	 */
	public String getLanguage()
	{
		return (String) options.getLanguage();
	}

	/**
	 * Load the content of the file into the textArea. Also updates the
	 * {@link #saveFile()} and {@link #saveFileAs(File)} variables, used to know
	 * if the text is dirty.
	 * 
	 * @param f
	 *            : the file to load the code from.
	 * @throws IOException
	 *             : If the file could not be opened or read, an exception is
	 *             raised.
	 * @see {@link #isDirty()}
	 */
	public void openFile(File f) throws IOException
	{
		BufferedReader reader = new BufferedReader(new FileReader(f));
		String all = "";
		String line;
		while ((line = reader.readLine()) != null)
		{
			all += (line + "\n");
		}
		saveFile = f;
		saveFileString = all;
		textArea.setText(all);
		scriptHandler.setFileName(f.getAbsolutePath());
		reader.close();
	}

	public void openStream(InputStream stream) throws IOException
	{
		byte[] data = new byte[stream.available()];
		stream.read(data);
		String s = "";
		for (byte b : data)
			s += (char) b;
		textArea.setText(s);
		stream.close();
		ThreadUtil.bgRun(new Runnable()
		{

			@Override
			public void run()
			{
				while (scriptHandler == null)
					ThreadUtil.sleep(1000);
				scriptHandler.autoDownloadPlugins();
			}
		});
	}

	@Override
	public void evaluationStarted()
	{
	}

	@Override
	public void evaluationOver()
	{
		ThreadUtil.invokeLater(new Runnable()
		{

			@Override
			public void run()
			{
				options.setStopButtonEnabled(false);
			}
		});
	}

	/**
	 * Displays a modal dialog to go to a specific line.
	 */
	public void displayGotoLine()
	{
		int min = 1;
		int max = textArea.getLineCount();
		String res = JOptionPane.showInputDialog(Icy.getMainInterface().getMainFrame(), "Enter line number (" + min + "," + max + ")", "Go to Line",
				JOptionPane.QUESTION_MESSAGE);
		try
		{
			int line = Integer.parseInt(res);
			textArea.setCaretPosition(textArea.getLineStartOffset(line - 1));
		} catch (NumberFormatException e)
		{
		} catch (BadLocationException e)
		{
		}
	}

	/**
	 * Format the text in this panel.
	 * Do a space/tab conversion according to the preferences, and
	 * perform a language-specific beautification.
	 * 
	 */
	public void format() {
		// getScriptHandler().organizeImports();
		
		// do the tab-to-space (or space-to-tab) conversion
		Preferences preferences = Preferences.getPreferences();
		RSyntaxTextArea textArea = getTextArea();
		if (preferences.isSoftTabsEnabled())
		{
			textArea.convertTabsToSpaces();
		} else
		{
			textArea.convertSpacesToTabs();
		}
		
		// do a language-specific formatting
		getScriptHandler().format();
	}

	public void addFileDropListener(FileDropListener fileDropListener) {
		new FileDrop(textArea, fileDropListener);
	}

	public void removeFileDropListeners() {
		FileDrop.remove(textArea);
	}
	
	public void addHyperlinkListener(HyperlinkListener listener) {
		((RSyntaxTextArea) textArea).addHyperlinkListener(listener);
	}

	public void removeHyperlinkListener(HyperlinkListener listener) {
		((RSyntaxTextArea) textArea).removeHyperlinkListener(listener);
	}
	
	public void addLanguageListener(ItemListener listener) {
		options.addLanguageListener(listener);
	}

	public void removeLanguageListener(ItemListener listener) {
		options.removeLanguageListener(listener);
	}
	
	protected void runInNew()
	{
		runScript(true);
	}

	protected void runInSame()
	{
		runScript(false);
	}
	
	protected void runScript(boolean newEngine)
	{
		if (scriptHandler == null)
			return;

		if (!integrated)
		{
			if (isDirty())
			{
				if (saveFile != null)
				{
					saveFile();
				}
			}
		}
		
		Preferences preferences = Preferences.getPreferences();
		
		ThreadUtil.invokeLater(new Runnable()
		{

			@Override
			public void run()
			{
				options.setStopButtonEnabled(true);
			}
		});

		if (!integrated)
		{
			scriptHandler.setNewEngine(newEngine);
			scriptHandler.setForceRun(preferences.isOverrideEnabled());
			scriptHandler.setStrict(preferences.isStrictModeEnabled());
			scriptHandler.setVarInterpretation(preferences.isVarInterpretationEnabled());
			scriptHandler.interpret(true);
		}
	}
	
	protected void stopScript()
	{
		if (scriptHandler == null)
			return;

		if (scriptHandler.isRunning())
		{
			scriptHandler.killScript();
		}
	}
}
