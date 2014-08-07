package plugins.tprovoost.scripteditor.gui;

import icy.gui.component.button.IcyButton;
import icy.image.ImageUtil;
import icy.plugin.PluginLoader;
import icy.resource.icon.IcyIcon;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import plugins.tprovoost.scripteditor.gui.action.SplitButtonActionListener;
import plugins.tprovoost.scripteditor.scriptinghandlers.ScriptEngineHandler;

/**
 * This panel creates the tools needed to choose the language (for each
 * language) and run the code.
 * 
 * @author Thomas Provoost
 */
class CommandPanel extends JPanel
{

	/** */
	private static final long serialVersionUID = 1L;
	
	private static final BufferedImage imgPlayback2 = ImageUtil.load(PluginLoader
			.getResourceAsStream("plugins/tprovoost/scripteditor/resources/icons/playback_erase_play_alpha.png"));

	private static final int STRUT_SIZE = 4;
	
	private JComboBox comboLanguages;
	
	private JMenuItem btnRun;
	private JMenuItem btnRunNew2;
	private JSplitButton btnSplitRun;
	private JButton btnStop;

	private ActionListener runInNewListener = new ActionListener()
	{

		@Override
		public void actionPerformed(ActionEvent e)
		{
			if (!lastIsNew)
			{
				lastIsNew = true;
				btnSplitRun.setIcon(new IcyIcon("playback_play", 16));
				btnSplitRun.setToolTipText("Creates a new context and run the script. The previous context will be lost.");
				btnSplitRun.repaint();
			}
		}
	};

	private ActionListener runInSameListener = new ActionListener()
	{

		@Override
		public void actionPerformed(ActionEvent e)
		{
			if (lastIsNew)
			{
				lastIsNew = false;
				btnSplitRun.setIcon(new IcyIcon(imgPlayback2, 16));
				btnSplitRun.setToolTipText("All variables in the bindings are re-usable.");
				btnSplitRun.repaint();
			}
		}
	};
	protected boolean lastIsNew = true;

	public CommandPanel()
	{
		this("JavaScript");
	}

	public CommandPanel(String language)
	{
		// final JButton btnBuild = new JButton("Verify");
		btnRun = new JMenuItem("Run in Current Context", new IcyIcon(imgPlayback2, 16));
		btnRun.setToolTipText("All variables in the bindings are re-usable.");

		btnSplitRun = new JSplitButton("  ", new IcyIcon("playback_play", 16));
		btnSplitRun.setPreferredSize(new Dimension(45, 20));
		btnSplitRun.setToolTipText("Creates a new context and run the script. The previous context will be lost.");

		btnRunNew2 = new JMenuItem("Run in New Context", new IcyIcon("playback_play", 16));
		btnRunNew2.setToolTipText("Creates a new context and run the script. The previous context and its bindings will be lost.");

		JPopupMenu popupRun = new JPopupMenu();
		popupRun.add(btnRunNew2);
		popupRun.add(btnRun);
		btnSplitRun.setPopupMenu(popupRun);

		btnStop = new IcyButton(new IcyIcon("square_shape", 16));
		btnStop.setToolTipText("Stops the current script.");
		btnStop.setEnabled(false);

		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));

		ArrayList<String> values = new ArrayList<String>();
		ScriptEngineManager manager = new ScriptEngineManager(PluginLoader.getLoader());
		for (ScriptEngineFactory factory : manager.getEngineFactories())
		{
			values.add(ScriptEngineHandler.getLanguageName(factory));
		}
		comboLanguages = new JComboBox(values.toArray());
		comboLanguages.setSelectedItem(language);
		comboLanguages.setMaximumSize(comboLanguages.getPreferredSize());

		btnRun.addActionListener(runInSameListener);

		btnSplitRun.addSplitButtonActionListener(new SplitButtonActionListener()
		{

			@Override
			public void splitButtonClicked(ActionEvent e)
			{
			}

			@Override
			public void buttonClicked(ActionEvent e)
			{
				if (lastIsNew)
					btnRunNew2.doClick();
				else
					btnRun.doClick();
			}
		});
		btnRunNew2.addActionListener(runInNewListener);

		add(new JLabel("Lang: "));
		add(comboLanguages);
		add(Box.createHorizontalStrut(STRUT_SIZE));
		add(btnSplitRun);
		add(Box.createHorizontalStrut(STRUT_SIZE));
		add(btnStop);
		add(Box.createHorizontalGlue());
	}
	
	public void setRunButtonsEnabled(boolean b) {
		btnRun.setEnabled(b);
		btnSplitRun.setEnabled(b);
	}

	public void setStopButtonEnabled(boolean b) {
		btnRun.setEnabled(!b);
		btnSplitRun.setEnabled(!b);
		btnStop.setEnabled(b);
	}
	
	public void addLanguageListener(ItemListener listener) {
		comboLanguages.addItemListener(listener);
	}

	public void removeLanguageListener(ItemListener listener) {
		comboLanguages.removeItemListener(listener);
	}
	
	/**
	 * Get the current selected language in the combobox.
	 * 
	 * @return
	 */
	public String getLanguage()
	{
		return (String) comboLanguages.getSelectedItem();
	}

	public void removeRunInSameListener(ActionListener listener) {
		btnRun.removeActionListener(listener);
	}

	public void removeRunInNewListener(ActionListener listener) {
		btnRunNew2.removeActionListener(listener);
	}

	public void addRunInSameListener(ActionListener listener) {
		btnRun.addActionListener(listener);
	}

	public void addRunInNewListener(ActionListener listener) {
		btnRunNew2.addActionListener(listener);
	}

	public void addStopListener(ActionListener listener) {
		btnStop.addActionListener(listener);
	}

	public void removeStopListener(ActionListener listener) {
		btnStop.removeActionListener(listener);
	}
}
