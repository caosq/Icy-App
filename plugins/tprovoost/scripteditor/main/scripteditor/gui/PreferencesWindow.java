package plugins.tprovoost.scripteditor.gui;

import icy.gui.component.IcyTextField;
import icy.gui.frame.IcyFrame;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.EventListenerList;

public class PreferencesWindow extends IcyFrame
{
    private static PreferencesWindow singleton = new PreferencesWindow();
    
    private Preferences preferences = Preferences.getPreferences();
    private JPanel panel;

    private IcyTextField tfSpacesTab;
    private JCheckBox cboxVarInterp;
    private JCheckBox cboxOverride;
    private JCheckBox cboxAutoVerif;
    private JCheckBox cboxStrict;
    private JCheckBox cboxSoft;
    private JCheckBox cboxAutoClearOutput;
    private JCheckBox cboxAdvanced;
    private JCheckBox cboxFullAutocomplete;

    private PreferencesWindow()
    {
        super("Script Editor Preferences", false, true, false, true);
        setContentPane(createGUI());
    }

    public static PreferencesWindow getPreferencesWindow()
    {
        return singleton;
    }

    private JPanel createGUI()
    {
        JPanel toReturn = new JPanel();
        toReturn.setBorder(new EmptyBorder(4, 4, 4, 4));
        toReturn.setLayout(new BorderLayout(0, 0));

        JPanel panelButtons = new JPanel();
        panelButtons.setBorder(new EmptyBorder(4, 0, 4, 0));
        toReturn.add(panelButtons, BorderLayout.SOUTH);
        panelButtons.setLayout(new BoxLayout(panelButtons, BoxLayout.X_AXIS));

        panelButtons.add(Box.createHorizontalGlue());

        JButton btnApply = new JButton("Apply");
        btnApply.addActionListener(new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
            	applyPreferences();
            }

        });
        panelButtons.add(btnApply);
        panelButtons.add(Box.createHorizontalStrut(20));

        JButton btnOk = new JButton("Ok");
        btnOk.addActionListener(new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
            	applyPreferences();
                close();
            }

        });
        panelButtons.add(btnOk);

        panelButtons.add(Box.createHorizontalStrut(20));

        JButton btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
            	// restore the values
            	initGUI();
            	// do not save and do not tell listeners
                close();
            }

        });
        panelButtons.add(btnCancel);

        JLabel lblPreferences = new JLabel("<html><h2>Preferences</h2></html>");
        lblPreferences.setHorizontalAlignment(SwingConstants.CENTER);
        toReturn.add(lblPreferences, BorderLayout.NORTH);

        JPanel panelCenter = new JPanel();
        toReturn.add(panelCenter, BorderLayout.CENTER);

        panelCenter.setLayout(new BorderLayout(0, 0));
        panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        panelCenter.add(panel);

        // -------------------
        // Full autocomplete
        // -------------------
        JPanel panelFullAutoComplete = new JPanel();
        panel.add(panelFullAutoComplete);
        panelFullAutoComplete.setLayout(new BoxLayout(panelFullAutoComplete, BoxLayout.X_AXIS));
        JLabel lblFullAutocomplete = new JLabel("Enable Auto Complete after any character typed: ");
        panelFullAutoComplete.add(lblFullAutocomplete);
        cboxFullAutocomplete = new JCheckBox("");

        panelFullAutoComplete.add(cboxFullAutocomplete);
        panelFullAutoComplete.add(Box.createHorizontalGlue());

        // ----------------------------------
        // Auto verification (no longer used)
        // ----------------------------------
        JPanel panelAutoVerif = new JPanel();
        // if (!release)
        // panel.add(panelAutoVerif);
        panelAutoVerif.setLayout(new BoxLayout(panelAutoVerif, BoxLayout.X_AXIS));

        JLabel lblAutoVerif = new JLabel("Enable auto verification (javascript)*: ");
        panelAutoVerif.add(lblAutoVerif);

        cboxAutoVerif = new JCheckBox("");
        panelAutoVerif.add(cboxAutoVerif);
        panelAutoVerif.add(Box.createHorizontalGlue());

        // ----------------------------------
        // Var interpretation(no longer used)
        // ----------------------------------
        JPanel panelVarInterp = new JPanel();
        // if (!release)
        // panel.add(panelVarInterp);
        panelVarInterp.setLayout(new BoxLayout(panelVarInterp, BoxLayout.X_AXIS));
        JLabel lblVarInterp = new JLabel("Enable variable interpretation*: ");
        panelVarInterp.add(lblVarInterp);
        cboxVarInterp = new JCheckBox("");

        panelVarInterp.add(cboxVarInterp);
        panelVarInterp.add(Box.createHorizontalGlue());

        // -----------------------------------
        // Override verification (javascript) (no longer used)
        // -----------------------------------
        JPanel panelOverride = new JPanel();
        panelOverride.setLayout(new BoxLayout(panelOverride, BoxLayout.X_AXIS));
        // if (!release)
        // panel.add(panelOverride);

        JLabel lblOverride = new JLabel("Override verification (javascript): ");
        panelOverride.add(lblOverride);

        cboxOverride = new JCheckBox("");
        panelOverride.add(cboxOverride);

        panelOverride.add(Box.createHorizontalGlue());
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // ------------
        // Enable discovering all methods (no longer used)
        // ------------
        JPanel panelAdvancedMode = new JPanel();
        // if (!release)
        // panel.add(panelAdvancedMode);
        panelAdvancedMode.setLayout(new BoxLayout(panelAdvancedMode, BoxLayout.X_AXIS));

        JLabel lblAdvancedMode = new JLabel("Enable methods (javascript/beta)*: ");
        panelAdvancedMode.add(lblAdvancedMode);

        cboxAdvanced = new JCheckBox("");
        panelAdvancedMode.add(cboxAdvanced);
        panelAdvancedMode.add(Box.createHorizontalGlue());

        // ------------
        // Strict mode (no longer used)
        // ------------
        JPanel panelStrict = new JPanel();
        panelStrict.setLayout(new BoxLayout(panelStrict, BoxLayout.X_AXIS));
        // if (!release)
        // panel.add(panelStrict);

        JLabel lblStrict = new JLabel("Enable Strict Mode (javascript)");
        panelStrict.add(lblStrict);

        cboxStrict = new JCheckBox("");
        panelStrict.add(cboxStrict);

        panelStrict.add(Box.createHorizontalGlue());

        // ------------------
        // Auto verification
        // ------------------
        JPanel panelAutoClearOutput = new JPanel();
        // if (!release)
        panel.add(panelAutoClearOutput);
        panelAutoClearOutput.setLayout(new BoxLayout(panelAutoClearOutput, BoxLayout.X_AXIS));

        JLabel lblAutoClearOutput = new JLabel("Always clear the output when run is clicked: ");
        panelAutoClearOutput.add(lblAutoClearOutput);

        cboxAutoClearOutput = new JCheckBox("");
        panelAutoClearOutput.add(cboxAutoClearOutput);
        panelAutoClearOutput.add(Box.createHorizontalGlue());

        // ----------
        // Soft Tabs
        // ----------
        JPanel panelSoft = new JPanel();
        panelSoft.setLayout(new BoxLayout(panelSoft, BoxLayout.X_AXIS));
        panel.add(panelSoft);

        JLabel lblSoft = new JLabel("Use Soft tabs (auto-expand tabs to spaces): ");
        panelSoft.add(lblSoft);

        cboxSoft = new JCheckBox("");
        panelSoft.add(cboxSoft);

        panelSoft.add(Box.createHorizontalGlue());

        // ---------------------
        // Spaces for Soft Tabs
        // ---------------------
        JPanel panelSpacesTab = new JPanel();
        panel.add(panelSpacesTab);
        panelSpacesTab.setLayout(new BoxLayout(panelSpacesTab, BoxLayout.X_AXIS));

        JLabel lblSpacesTab = new JLabel("Tab width: ");
        panelSpacesTab.add(lblSpacesTab);

        tfSpacesTab = new IcyTextField();
        tfSpacesTab.setColumns(10);
        panelSpacesTab.add(tfSpacesTab);
        tfSpacesTab.setMaximumSize(new Dimension(60, 25));

        panelSpacesTab.add(Box.createHorizontalGlue());

        panel.add(Box.createVerticalGlue());

        // JLabel lblNeedsRestarting = new JLabel("* needs restarting Script Editor");
        // if (!release)
        // panelCenter.add(lblNeedsRestarting, BorderLayout.SOUTH);

        initGUI();
        
        return toReturn;
    }

    void initGUI()
    {
    	// cboxVarInterp.setSelected(preferences.isVarInterpretationEnabled());
    	// cboxOverride.setSelected(preferences.isOverrideEnabled());
    	// cboxAutoVerif.setSelected(preferences.isAutoBuildEnabled());
    	// cboxStrict.setSelected(preferences.isStrictModeEnabled());
    	cboxFullAutocomplete.setSelected(preferences.isFullAutoCompleteEnabled());
    	cboxAutoClearOutput.setSelected(preferences.isAutoClearOutputEnabled());
    	cboxSoft.setSelected(preferences.isSoftTabsEnabled());
    	tfSpacesTab.setValue("" + preferences.indentSpacesCount());
    }
    
    void applyPreferences()
    {
    	preferences.setFullAutoCompleteEnabled(cboxFullAutocomplete.isSelected());
    	preferences.setAutoClearOutputEnabled(cboxAutoClearOutput.isSelected());
    	preferences.setSoftTabsEnabled(cboxSoft.isSelected());
    	
    	int tabWidth;
    	try
    	{
    		tabWidth = Integer.valueOf(tfSpacesTab.getText());
    	}
    	catch (NumberFormatException e)
    	{
    		tabWidth = 8;
    		tfSpacesTab.setValue("8");
    	}
    	preferences.setTabWidth(tabWidth);
    	
    	preferences.savePrefs();
    	firePreferencesChanged();
    }
    
    private final EventListenerList listeners = new EventListenerList();
    
    public void addPreferencesListener(PreferencesListener listener) {
        listeners.add(PreferencesListener.class, listener);
    }
 
    public void removePreferencesListener(PreferencesListener listener) {
        listeners.remove(PreferencesListener.class, listener);
    }
    
    protected void firePreferencesChanged() {
    	for(PreferencesListener listener : getPreferencesListeners()) {
    		listener.preferencesChanged();
    	}
    }
    
    public PreferencesListener[] getPreferencesListeners() {
        return listeners.getListeners(PreferencesListener.class);
    }
}
