package plugins.tprovoost.scripteditor.gui;

import icy.common.listener.AcceptListener;
import icy.file.FileUtil;
import icy.file.Loader;
import icy.gui.frame.IcyFrame;
import icy.gui.frame.IcyFrameAdapter;
import icy.gui.frame.IcyFrameEvent;
import icy.gui.frame.IcyFrameListener;
import icy.gui.frame.progress.FailedAnnounceFrame;
import icy.main.Icy;
import icy.network.NetworkUtil;
import icy.plugin.PluginLoader;
import icy.preferences.IcyPreferences;
import icy.preferences.PluginsPreferences;
import icy.preferences.XMLPreferences;
import icy.resource.icon.IcyIcon;
import icy.system.FileDrop;
import icy.system.SystemUtil;
import icy.system.thread.ThreadUtil;
import icy.util.XMLUtil;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.ListIterator;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent.EventType;

import plugins.tprovoost.scripteditor.gui.ScriptingPanel.SavedAsListener;
import plugins.tprovoost.scripteditor.gui.ScriptingPanel.TitleChangedListener;
import plugins.tprovoost.scripteditor.scriptingconsole.BindingsScriptFrame;
import plugins.tprovoost.scripteditor.scriptingconsole.PythonScriptingconsole;
import plugins.tprovoost.scripteditor.scriptingconsole.Scriptingconsole;
import plugins.tprovoost.scripteditor.scriptinghandlers.ScriptEngineHandler;

/**
 * Main GUI of the class
 * 
 * @author tprovoost
 */
public class ScriptingEditor extends IcyFrame implements ActionListener
{

	// Scripting Panels
	private JTabbedPane tabbedPane;
	private JPanel addPanePanel;

	// Console
	private ConsoleOutput consoleOutput;
	private Scriptingconsole console;
	private JButton btnClearConsole;

	// Preferences and recent files
	private JMenu menuOpenRecent;
	static String currentDirectoryPath = "";
	private static final int ctrlMask = SystemUtil.getMenuCtrlMask();
	private static final String STRING_LAST_DIRECTORY = "lastDirectory";
	private XMLPreferences prefs = PluginsPreferences.getPreferences().node("plugins.tprovoost.scripteditor.main.ScriptEditorPlugin");

	private final RecentFiles recentFiles = new RecentFiles(prefs);
	
	// flag that makes saveEditorState() a no-op
	protected volatile boolean saveStateEnabled = true;

	private static final boolean IS_PYTHON_INSTALLED = ScriptEngineHandler.factory.getEngineByExtension("py") != null;
	private static final String PREF_IDX = "idxTab";

	private IcyFrameListener frameListener = new IcyFrameAdapter()
	{
		@Override
		public void icyFrameClosing(IcyFrameEvent e)
		{
			if (IcyFrame.getAllFrames(ScriptingEditor.class).size() == 1)
			{
				ScriptEngineHandler.clearEngines();
				// close the bindings frame and release the reference to the engine
				BindingsScriptFrame.getInstance().setVisible(false);
				BindingsScriptFrame.getInstance().setEngine(null);
			}
			console.close();
			
			// close all the tabs, asking the user confirmation
			// do not save tab state while closing (would break the saved state)
			saveStateEnabled = false;
			closeAll();
			saveStateEnabled = true;
		}
	};
	
	// listener called when Icy exits, asking for user confirmation
	private AcceptListener acceptlistener = new AcceptListener()
	{
		@Override
		public boolean accept(Object source)
		{
			// close all the tabs, asking the user confirmation
			// do not save tab state while closing (would break the saved state)
			saveStateEnabled = false;
			boolean canClose = closeAll();
			saveStateEnabled = true;
			return canClose;
		}
	};
	
	// listener called when a child ScriptingPanel saves a new file
	private SavedAsListener savedAsListener = new SavedAsListener()
	{

		@Override
		public void savedAs(File f) {
			addRecentFile(f);
		}
	};
	
	// listener called when a child ScriptingPanel wants its title to be changed
	private TitleChangedListener titleChangedListener = new TitleChangedListener()
	{

		@Override
		public void titleChanged(ScriptingPanel panel, String title)
		{
			int idx = tabbedPane.indexOfComponent(panel);
			if (idx != -1)
			{
				tabbedPane.setTitleAt(idx, title);
				Component c = tabbedPane.getTabComponentAt(idx);
				if (c instanceof JComponent)
					((JComponent) c).revalidate();
				else
					c.repaint();
			}
		}
	};
	
	private FileDrop.FileDropListener fileDropListener = new FileDrop.FileDropListener()
	{

		@Override
		public void filesDropped(File[] files)
		{
			for (File f : files)
				if (f.getName().endsWith(".js") || f.getName().endsWith(".py"))
					try
					{
						openFile(f);
					} catch (IOException e)
					{
					}
				else
					Loader.load(f, true);
		}
	};
	
	private HyperlinkListener hyperlinkListener = new HyperlinkListener()
	{

		@Override
		public void hyperlinkUpdate(HyperlinkEvent e) {
			if (e.getEventType() == EventType.ACTIVATED)
			{
				URL url = e.getURL();
				String res = url == null ? e.getDescription() : url.getFile();
				try
				{
					openFile(new File(res));
				}
				catch (IOException e1)
				{
				}
			}
		}
	};
	
	// listen to changes of language in a child panel
	public ItemListener languageListener = new ItemListener()
	{

		@Override
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.SELECTED)
			{
				String language = (String) e.getItem();
				changeConsoleLanguage(language);
			}
		}
	};
	
	private JPanel panelSouth;

	public ScriptingEditor()
	{
		super("Script Editor", true, true, true, true);

		setJMenuBar(createJMenuBar());

		JPanel mainPanel = new JPanel(new BorderLayout());
		tabbedPane = new JTabbedPane();
		tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		tabbedPane.addChangeListener(new ChangeListener()
		{

			@Override
			public void stateChanged(ChangeEvent arg0)
			{
				Component comp = tabbedPane.getSelectedComponent();
				if (!(comp instanceof ScriptingPanel)) {
					createNewPane();
					return;				
				}
				
				ScriptingPanel panel = (ScriptingPanel) comp;

				// save the editor state to be able to restore the currently-opened tab
				saveEditorState();
				
				// update the console language according to the selected panel
				changeConsoleLanguage(panel.getLanguage());

				// update the editor title with the absolute path of the selected tab
				File f = panel.getSaveFile();
				String s;
				if (f != null)
				{
					s = f.getAbsolutePath();
				}
				else
				{
					s = panel.getPanelName();
				}
				setTitle("Script Editor - " + s);
			}
		});
		
		new FileDrop(getExternalFrame(), fileDropListener);
		new FileDrop(getInternalFrame(), fileDropListener);

		// unset default FlowLayout' gaps
		addPanePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		addPanePanel.setOpaque(false);
        addPanePanel.add(new JLabel(new IcyIcon("plus")));
        
		consoleOutput = new ConsoleOutput();

		if (btnClearConsole != null)
			btnClearConsole.removeActionListener(ScriptingEditor.this);
		btnClearConsole = new JButton("Clear");
		btnClearConsole.addActionListener(ScriptingEditor.this);

		JPanel bottomPanel = new JPanel(new BorderLayout());
		bottomPanel.add(consoleOutput, BorderLayout.CENTER);

		panelSouth = new JPanel(new BorderLayout());
		bottomPanel.add(panelSouth, BorderLayout.SOUTH);
	
		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabbedPane, bottomPanel);
		split.setDividerLocation(0.75d);
		split.setResizeWeight(0.75d);
		split.setOneTouchExpandable(true);
		add(split, BorderLayout.CENTER);

		mainPanel.add(split);
		setContentPane(mainPanel);

		// ----------------------
		// setting listeners
		// ---------------------

		// exit listener
		Icy.getMainInterface().addCanExitListener(acceptlistener);

		restoreEditorState();
	}

	/**
	 * Getter on tabbedPane.
	 * 
	 * @return
	 */
	public JTabbedPane getTabbedPane()
	{
		return tabbedPane;
	}
	
	void restoreEditorState()
	{
		currentDirectoryPath = prefs.get(STRING_LAST_DIRECTORY, "");

		// load preferences
		final XMLPreferences openedFiles = prefs.node("openedFiles");
		final ArrayList<String> toOpen = new ArrayList<String>();
		for (XMLPreferences key : openedFiles.getChildren())
		{
			String fileName = key.get("file0", "");
			toOpen.add(fileName);
		}
		
		recentFiles.load();
		updateRecentFilesMenu();
		
		if (toOpen.isEmpty())
		{
			// No files to reopen, let's create by default a Javascript panel named "Untitled"
			// This will also create and add a console
			createNewPane();
		}
		
		ThreadUtil.invokeLater(new Runnable()
		{

			@Override
			public void run()
			{
				// disable the state saving (would break the state)
				saveStateEnabled = false;
				for (String s : toOpen)
				{
					try
					{
						// open the file, do not save editor state
						openFile(new File(s));
					} catch (IOException e1)
					{
					}
				}
				int idx = openedFiles.getInt(PREF_IDX, 0);
				// the maximum possible index is the tab count minus two
				// because we must count the '+' tab and the offset of one
				// between the count and the index
				int maxIdx = tabbedPane.getTabCount() - 2;
				if (idx <= maxIdx) {
					tabbedPane.setSelectedIndex(idx);
				}
				// re-enable the state saving
				saveStateEnabled = true;
			}
		});
	}

	/**
	 * Save the editor state (opened files, selected tab) to the disk
	 */
	private void saveEditorState()
	{
		// do nothing when disabled
		if (!saveStateEnabled)
			return;
		
		int idx = tabbedPane.getSelectedIndex();
		
		// Saving state of the opened files.
		XMLPreferences openedFiles = prefs.node("openedFiles");

		// remove previous settings
		// Note: I do not use openedFiles.removeChildren() because it leaves blank lines
		// in the XML file
		XMLUtil.removeAllChildren(openedFiles.getXMLNode());
		
		for (int i = 0; i < tabbedPane.getTabCount() - 1; ++i)
		{
			Component c = tabbedPane.getComponentAt(i);
			if (c instanceof ScriptingPanel)
			{
				File f = ((ScriptingPanel) c).getSaveFile();
				if (f != null)
				{
					XMLPreferences key = openedFiles.node("entry" + i);
					key.put("file0", f.getAbsolutePath());					
				}
			}
		}
		openedFiles.putInt(PREF_IDX, idx);
		
		recentFiles.save();
		
		// actually save on disk now
		IcyPreferences.save();
	}

	/**
	 * Close all tabs by dispatching the closing to each tab.
	 * 
	 * @return Returns if success in closing. False means the user decided to
	 *         cancel the closing.
	 */
	private boolean closeAll()
	{
		if (getInternalFrame().getDefaultCloseOperation() == WindowConstants.DO_NOTHING_ON_CLOSE)
		{
			if (!closeAllTabs())
				return false;

			setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			close();
		}
		return true;
	}

	public ScriptingPanel createNewPane()
	{
		return createNewPane("Untitled");
	}

	/**
	 * Creates a new Scripting Pane in the JTabbedPane and returns it. <br/>
	 * Automatically removes the "+" tab and re-add it after the panel.
	 * 
	 * @param name
	 *            : this is not a file path. Please use
	 *            {@link #openFile(String)} instead to open a file with a file
	 *            path.
	 * @return
	 * @see {@link #createNewPane()}, {@link #openFile(File)},
	 *      {@link #openFile(String)}
	 */
	public ScriptingPanel createNewPane(String name)
	{
		ScriptingPanel panelCreated;
		String ext = FileUtil.getFileExtension(name, false);
		if (ext.contentEquals("py"))
			panelCreated = new ScriptingPanel(name, "Python", consoleOutput);
		else
			panelCreated = new ScriptingPanel(name, "JavaScript", consoleOutput);
		
		panelCreated.addSavedAsListener(savedAsListener);
		panelCreated.addTitleChangedListener(titleChangedListener);
		panelCreated.addFileDropListener(fileDropListener);
		panelCreated.addHyperlinkListener(hyperlinkListener);
		panelCreated.addLanguageListener(languageListener);
		
		// initialize the console corresponding to this panel
		changeConsoleLanguage(panelCreated.getLanguage());
		
		int idx = tabbedPane.getTabCount() - 1;
		if (idx != -1)
			tabbedPane.removeTabAt(idx);
		else
			idx = 0;
		tabbedPane.addTab(name, panelCreated);
		tabbedPane.setTitleAt(idx, name);
		tabbedPane.repaint();
		tabbedPane.setTabComponentAt(idx, new TabComponentButton(this, panelCreated));
		tabbedPane.addTab("+", new JLabel());
		tabbedPane.setTabComponentAt(idx + 1, addPanePanel);
		tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 2);
		Insets i = addPanePanel.getInsets();
		i.bottom = 0;
		i.left = 0;
		i.right = 0;
		i.top = 0;
		return panelCreated;
	}

	/**
	 * Open the file f into the editor as a new tab.
	 * 
	 * @param f
	 * @throws IOException
	 */
	public void openFile(File f) throws IOException
	{
		if (!f.exists())
			return;
		String filename = f.getName();
		boolean exists = false;
		for (int i = 0; i < tabbedPane.getTabCount(); ++i)
			if (tabbedPane.getTitleAt(i).contentEquals(filename))
			{
				tabbedPane.setSelectedIndex(i);
				exists = true;
			}
		if (exists)
		{
			return;
		}
		
		ScriptingPanel panel = createNewPane(filename);
		panel.openFile(f);
		addRecentFile(f);
		removeUntitledFirstPane();
		saveEditorState();
	}

	private void removeUntitledFirstPane()
	{
		// the user has chosen to open a file,
		// while the editor only has the default "Untitled" file opened.
		// remove that pane if it is really empty

		// if there are only two tabs, it means that the second one is the '+' tab
		// removing the first one would select that '+' tab and recreate a new one...
		// Avoid that !
		if (tabbedPane.getTabCount() <= 2)
			return;

		ScriptingPanel panel = (ScriptingPanel) tabbedPane.getComponentAt(0);
		if (tabbedPane.getTitleAt(0).contentEquals("Untitled")
				&& panel.getTextArea().getText().isEmpty()
				&& !panel.isDirty())
		{
			closeTab(0);
		}
	}

	private void updateRecentFilesMenu()
	{
		menuOpenRecent.removeAll();
		for (int i = 0; i < recentFiles.getFiles().size(); ++i)
		{
			String path = recentFiles.getFiles().get(i);
			JMenuItem item = createRecentFileItem(path);
			if (item != null)
			{
				menuOpenRecent.add(item);
			}
		}
	}

	private JMenuItem createRecentFileItem(String filename)
	{
		final File f = new File(filename);
		JMenuItem toReturn = null;
		if (f.exists())
		{
			toReturn = new JMenuItem(f.getName() + " - " + f.getParent());
			toReturn.addActionListener(new ActionListener()
			{

				@Override
				public void actionPerformed(ActionEvent e)
				{
					try
					{
						openFile(f);
					} catch (IOException e1)
					{
						e1.printStackTrace();
					}
				}
			});
		}
		return toReturn;
	}

	/**
	 * Open the file f into the editor as a new tab.
	 * 
	 * @param f
	 * @throws IOException
	 */
	public void openStream(String name, InputStream stream) throws IOException
	{
		ScriptingPanel panel = createNewPane(name);
		panel.openStream(stream);
		removeUntitledFirstPane();
		saveEditorState();
	}

	/**
	 * Displays a JFileChoose and let the user choose its file, then open it.
	 * 
	 * @see ScriptingEditor#openFile(File)
	 */
	public void showOpenFileDialog()
	{
		JFileChooser fc;
		if (currentDirectoryPath == "")
			fc = new JFileChooser();
		else
			fc = new JFileChooser(currentDirectoryPath);
		fc.setMultiSelectionEnabled(true);
		if (fc.showOpenDialog(getFrame()) == JFileChooser.APPROVE_OPTION)
		{
			File[] files = fc.getSelectedFiles();
			for (File f : files)
			{
				String path = FileUtil.getDirectory(f.getAbsolutePath());
				currentDirectoryPath = path;
				prefs.put(STRING_LAST_DIRECTORY, path);
				try
				{
					openFile(f);
				} catch (IOException e)
				{
					new FailedAnnounceFrame(f.getName() + " is not a valid file");
				}
			}
		}
	}

	/**
	 * Creates the JMenuBar of the {@link ScriptingEditor}.
	 * 
	 * @return
	 */
	private JMenuBar createJMenuBar()
	{
		JMenuBar toReturn = new JMenuBar();

		// MENU FILE
		JMenu menuFile = new JMenu("File");
		JMenuItem menuNew = new JMenuItem("New");
		menuNew.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				createNewPane("Untitled");
			}
		});
		menuFile.add(menuNew);
		menuNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ctrlMask));

		JMenuItem menuOpen = new JMenuItem("Open...");
		menuOpen.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				showOpenFileDialog();
			}
		});
		menuOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ctrlMask));
		menuOpen.setDisplayedMnemonicIndex(0);
		menuFile.add(menuOpen);

		menuOpenRecent = new JMenu("Open Recent");
		menuFile.add(menuOpenRecent);

		JMenuItem menuSave = new JMenuItem("Save");
		menuSave.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Component comp = tabbedPane.getSelectedComponent();
				if (comp instanceof ScriptingPanel)
				{
					ScriptingPanel panel = ((ScriptingPanel) comp);
					if (panel.isDirty())
					{
						if (panel.getSaveFile() == null)
						{
							panel.showSaveFileDialog(currentDirectoryPath);
						} else
						{
							panel.saveFile();
						}
					}
				}
			}
		});
		menuSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ctrlMask));
		menuFile.add(menuSave);

		JMenuItem menuSaveAs = new JMenuItem("Save As...");
		menuSaveAs.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				Component comp = tabbedPane.getSelectedComponent();
				if (comp instanceof ScriptingPanel)
				{
					((ScriptingPanel) comp).showSaveFileDialog(currentDirectoryPath);
				}
			}
		});
		menuSaveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ctrlMask | InputEvent.SHIFT_DOWN_MASK));
		menuFile.add(menuSaveAs);

		menuFile.add(new JSeparator());

		JMenuItem menuClose = new JMenuItem("Close Tab");
		menuClose.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				int i = tabbedPane.getSelectedIndex();
				if (i >= 0 && i < tabbedPane.getTabCount() - 1)
				{
					closeTab(i);
				}
			}
		});
		menuClose.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, ctrlMask));
		menuFile.add(menuClose);

		JMenuItem menuCloseAll = new JMenuItem("Close All Tabs");
		menuCloseAll.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				closeAllTabs();
			}
		});
		menuFile.add(menuCloseAll);

		JMenuItem menuCloseAllOthers = new JMenuItem("Close Other Tabs");
		menuCloseAllOthers.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				int idx = tabbedPane.getSelectedIndex();
				int N = tabbedPane.getTabCount() - 1;

				ArrayList<Integer> tabsToClose = new ArrayList<Integer>();
				for (int i=0; i<N; i++)
					tabsToClose.add(i);
				tabsToClose.remove(idx);

				// remove tabs in reverse order to not change the tab indexes on the way
				ListIterator<Integer> iterator = tabsToClose.listIterator(tabsToClose.size());
				while(iterator.hasPrevious())
				{
					if (!closeTab(iterator.previous()))
						break;
				}
			}
		});
		menuFile.add(menuCloseAllOthers);

		// MENU EDIT
		JMenu menuEdit = new JMenu("Edit");
		JMenuItem menuUndo = new JMenuItem("Undo");
		menuUndo.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				Component comp = tabbedPane.getTabComponentAt(0);
				if (!(comp instanceof ScriptingPanel))
					return;
				ScriptingPanel panel = (ScriptingPanel) comp;
				panel.getTextArea().getActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ctrlMask)).actionPerformed(e);
			}
		});
		menuUndo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ctrlMask));
		menuEdit.add(menuUndo);

		JMenuItem menuRedo = new JMenuItem("Redo");
		menuRedo.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				Component comp = tabbedPane.getTabComponentAt(0);
				if (!(comp instanceof ScriptingPanel))
					return;
				ScriptingPanel panel = (ScriptingPanel) comp;
				panel.getTextArea().getActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_Y, ctrlMask)).actionPerformed(e);
			}
		});
		menuRedo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, ctrlMask));
		menuEdit.add(menuRedo);

		menuEdit.add(new JSeparator());
		JMenuItem menuFormat = new JMenuItem("Format");
		menuFormat.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				Component c = tabbedPane.getSelectedComponent();
				if (c instanceof ScriptingPanel)
				{
					ScriptingPanel panel = ((ScriptingPanel) c);
					panel.format();
				}
			}
		});
		menuFormat.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, ctrlMask | InputEvent.SHIFT_DOWN_MASK));
		menuEdit.add(menuFormat);
		
		JMenuItem menuAutoImport = new JMenuItem("Auto-Import");
		menuAutoImport.addActionListener(new ActionListener()
		{
			
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// TODO
				// parse the text, look for an Unknown
				// if it starts by a Capital letter, try to load it
				// if multiple choices, do nothing for now
				Component c = tabbedPane.getSelectedComponent();
				if (c instanceof ScriptingPanel)
				{
					ScriptingPanel panel = ((ScriptingPanel) c);
					panel.getScriptHandler().autoImport();
				}
			}
		});
		menuEdit.add(menuAutoImport);
		menuEdit.add(new JSeparator());

		// FIND FEATURE
		JMenuItem menuFind = new JMenuItem("Find");
		menuFind.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				displayFindReplace();
			}
		});
		menuFind.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, ctrlMask));
		// menuFind.setEnabled(false);
		menuEdit.add(menuFind);

		// REPLACE FEATURE
		JMenuItem menuReplace = new JMenuItem("Replace");
		menuReplace.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				displayFindReplace();
			}
		});
		menuReplace.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, ctrlMask));
		// menuReplace.setEnabled(false);
		menuEdit.add(menuReplace);

		// GOTO LINE FEATURE
		JMenuItem menuGotoLine = new JMenuItem("Go to Line...");
		menuGotoLine.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				Component c = tabbedPane.getSelectedComponent();
				if (c instanceof ScriptingPanel)
				{
					((ScriptingPanel) c).displayGotoLine();
				}
			}
		});
		menuGotoLine.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ctrlMask));
		menuEdit.add(menuGotoLine);

		// JMenu menuTools = new JMenu("Tools");
		// JMenuItem menuFindClass = new JMenuItem("Find Class...");
		// menuTools.add(menuFindClass);

		// MENU TEMPLATES
		JMenu menuTemplate = new JMenu("Templates");
		populateMenuTemplate(menuTemplate);

		JMenu menuOptions = new JMenu("Options");
		JMenuItem menuPreferences = new JMenuItem("Preferences");
		menuPreferences.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				final PreferencesWindow prefs = PreferencesWindow.getPreferencesWindow();
				prefs.addToMainDesktopPane();
				prefs.setVisible(true);
			}
		});
		menuOptions.add(menuPreferences);

		JMenuItem menuBindingsFrame = new JMenuItem("Bindings Frame");
		menuBindingsFrame.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				BindingsScriptFrame frame = BindingsScriptFrame.getInstance();
				frame.update();
				if (frame.isVisible())
					frame.setVisible(false);
				else
					frame.setVisible(true);
			}
		});
		menuOptions.add(menuBindingsFrame);

		JMenuItem menuHelp = new JMenuItem("Help (online)");
		menuHelp.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				NetworkUtil.openBrowser("http://icy.bioimageanalysis.org/plugin/Script_Editor#documentation");
			}
		});
		menuOptions.add(menuHelp);

		toReturn.add(menuFile);
		toReturn.add(menuEdit);
		toReturn.add(menuTemplate);
		// toReturn.add(menuTools);
		toReturn.add(menuOptions);

		updateRecentFilesMenu();

		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addFrameListener(frameListener);

		return toReturn;
	}

	public void displayFindReplace()
	{
		FindAndReplaceDialog.showDialog(this);
	}

	/**
	 * Tries to close all tabs.
	 * The use will be asked to save the changes, and given the opportunity to cancel.
	 *
	 * @return false if the operation is cancelled.
	 */
	private boolean closeAllTabs()
	{
		boolean ok = true;

		// number of files opened
		int N = tabbedPane.getTabCount() - 1;
		// close all tabs
		// Note: do not use a while loop on getTabCount() here, it could loop
		// indefinitely in case of a coding error involving the '+' tab...
		for (int i=0; i<N; i++)
		{
			if (!closeTab(0))
			{
				ok = false;
				break;
			}
		}
		return ok;
	}

	protected boolean closeTab(int i)
	{
		Component c = tabbedPane.getTabComponentAt(i);
		if (c instanceof TabComponentButton)
		{
			ScriptingPanel panel = ((TabComponentButton) c).getPanel(); 
			boolean ok = panel.close(getCurrentDirectory());
			
			if (ok)
			{
				panel.removeSavedAsListener(savedAsListener);
				panel.removeTitleChangedListener(titleChangedListener);
				panel.removeFileDropListeners();
				panel.removeHyperlinkListener(hyperlinkListener);
				panel.removeLanguageListener(languageListener);
				
	            if (i == tabbedPane.getSelectedIndex() && i >= (tabbedPane.getTabCount() - 2))
	            {
	            	// We are closing the last tab.
	            	// The next one is the virtual tab used a "plus" button to open a new tab.
	            	// We want to avoid selecting that tab artificially,
	            	// so go to the previous one.
	            	tabbedPane.setSelectedIndex(i-1);
	            }
	            
				// remove the tab
		        if (i != -1)
		        	tabbedPane.remove(i);
		        
		        saveEditorState();
				
			}
			return ok;
		}
		return true;
	}
	
	protected boolean closeTab(TabComponentButton tabComponent)
	{
		int i = tabbedPane.indexOfTabComponent(tabComponent);
		return closeTab(i);
	}

	/**
	 * Hard coded function populating the templates. May change to an automatic
	 * parsing of files.
	 * 
	 * @param menuTemplate
	 */
	private void populateMenuTemplate(JMenu menuTemplate)
	{
		JMenu menuTemplateJS = new JMenu("JavaScript");
		JMenuItem itemJSDuplicateSequence = new JMenuItem("Duplicate Sequence");

		itemJSDuplicateSequence.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				openJSTemplate("duplicateSequence.js");
			}
		});
		menuTemplateJS.add(itemJSDuplicateSequence);

		JMenuItem itemThreshold = new JMenuItem("Threshold");
		itemThreshold.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				openJSTemplate("threshold.js");
			}
		});
		menuTemplateJS.add(itemThreshold);

		JMenu menuTemplatePython = new JMenu("Python");
		JMenuItem itemPythonDuplicateSequence = new JMenuItem("Duplicate Sequence");

		itemPythonDuplicateSequence.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				openPythonTemplate("duplicateSequence.py");
			}
		});
		menuTemplatePython.add(itemPythonDuplicateSequence);

		// add JS templates.
		menuTemplate.add(menuTemplateJS);
		if (IS_PYTHON_INSTALLED)
			menuTemplate.add(menuTemplatePython);
	}

	private void openJSTemplate(String templateName)
	{
		openTemplate("js", templateName);
	}

	private void openPythonTemplate(String templateName)
	{
		openTemplate("python", templateName);
	}

	/**
	 * Open a template file contained in
	 * plugins/tprovoost/scripteditor/templates/" + type + "/" + templateName
	 * 
	 * @param type
	 * @param templateName
	 */
	private void openTemplate(String type, String templateName)
	{
		String current = new File(".").getAbsolutePath();
		current = current.substring(0, current.length() - 1);
		try
		{
			InputStream is = PluginLoader.getResourceAsStream("plugins/tprovoost/scripteditor/resources/templates/" + type + "/" + templateName);
			openStream(templateName, is);
		} catch (IOException e1)
		{
		}
	}

	public String getCurrentDirectory()
	{
		return currentDirectoryPath;
	}

	public void changeConsoleLanguage(String language)
	{
		// the Console uses the same engine but another script handler
		if (language.contentEquals("Python"))
		{
			console = new PythonScriptingconsole();
		} else
		{
			console = new Scriptingconsole();
		}

		// set the language for the console too.
		console.setLanguage(language);

		// a reference to the output.
		console.setOutput(consoleOutput);

		console.setFont(consoleOutput.getTextPane().getFont());

		if (panelSouth != null)
		{
			panelSouth.removeAll();
			panelSouth.add(console, BorderLayout.CENTER);
			panelSouth.add(btnClearConsole, BorderLayout.EAST);
			panelSouth.revalidate();
		}
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == btnClearConsole)
		{
			if (console != null)
				console.clear();
		}
	}

	public void addRecentFile(File f)
	{
		recentFiles.add(f);
		updateRecentFilesMenu();
	}

	public static String getDefaultFolder()
	{
		return "";
	}

	/**
	 * @return the consoleOutput
	 */
	public ConsoleOutput getConsoleOutput()
	{
		return consoleOutput;
	}
}