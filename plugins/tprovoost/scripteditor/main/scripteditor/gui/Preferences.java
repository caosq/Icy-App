package plugins.tprovoost.scripteditor.gui;

import icy.preferences.PluginsPreferences;
import icy.preferences.XMLPreferences;

public class Preferences
{
    private static Preferences singleton = new Preferences();

    private XMLPreferences prefs = PluginsPreferences.getPreferences().node("plugins.tprovoost.scripteditor.main.ScriptEditorPlugin");

    // private PreferencesTableModel tableModel;
    // private final String PREF_VAR_INTERPRET = "varinterp";
    // private final String PREF_OVERRIDE = "override";
    // private final String PREF_VERIF = "autoverif";
    // private final String PREF_STRICT = "strictmode";
    private final String PREF_INDENT_SPACES = "indent";
    private final String PREF_INDENT_SPACES_VALUE = "nbSpaces";
    private final String PREF_FULL_AUTOCOMPLETE = "fullautocomplete";
    private final String PREF_AUTOCLEAR_OUTPUT = "autoclearoutput";
    
    private int tabWidth;
    //private boolean varInterp;
    //private boolean override;
    //private boolean autoVerif;
    //private boolean strict;
    private boolean soft;
    private boolean autoClearOutput;
    //private boolean advanced;
    private boolean fullAutocomplete;

    private Preferences()
    {
        loadPrefs();
    }

    public static Preferences getPreferences()
    {
        return singleton;
    }

    public boolean isFullAutoCompleteEnabled()
    {
        return fullAutocomplete;
    }

    public boolean isVarInterpretationEnabled()
    {
         // return varInterp;
        return true;
    }

    public boolean isOverrideEnabled()
    {
        // return override;
        return true;
    }

    public boolean isAutoBuildEnabled()
    {
        // return autoVerif;
        return true;
    }

    public boolean isStrictModeEnabled()
    {
        // return strict;
        return false;
    }

    public boolean isAutoClearOutputEnabled()
    {
        return autoClearOutput;
    }

    public boolean isSoftTabsEnabled()
    {
        return soft;
    }

    public int indentSpacesCount()
    {
        return tabWidth;
    }

    // public class PreferencesTableModel extends DefaultTableModel
    // {
    //
    // /**
    // *
    // */
    // private static final long serialVersionUID = 1L;
    // Class<?>[] columnTypes = new Class[] {String.class, Boolean.class};
    //
    // public PreferencesTableModel()
    // {
    // super(new Object[][] {
    // {"Enable variable interpretation (beta)*", prefs.getBoolean(PREF_VAR_INTERPRET,
    // Boolean.FALSE)},
    // {"Override verification (javascript)", prefs.getBoolean(PREF_OVERRIDE, Boolean.TRUE)},
    // {"Enable auto verification (javascript/beta)*", prefs.getBoolean(PREF_VERIF, Boolean.FALSE)},
    // {"Enable Strict Mode (javascript)", prefs.getBoolean(PREF_STRICT, Boolean.FALSE)},
    // {"Soft tabs", prefs.getBoolean(PREF_INDENT_SPACES, Boolean.TRUE)},
    // {"Spaces count for soft tabs", prefs.getInt(PREF_INDENT_SPACES_VALUE, 8)},}, new String[] {
    // "Property", "Value"});
    // }
    //
    // public PreferencesTableModel(boolean release)
    // {
    // super(new Object[][] {{"Indentation by spaces", prefs.getBoolean(PREF_INDENT_SPACES,
    // Boolean.FALSE)}},
    // new String[] {"Property", "Value"});
    // }
    //
    // @Override
    // public Class<?> getColumnClass(int columnIndex)
    // {
    // return columnTypes[columnIndex];
    // }
    //
    // @Override
    // public void setValueAt(Object aValue, int row, int column)
    // {
    // super.setValueAt(aValue, row, column);
    // savePrefs();
    // }
    // }

    public void savePrefs()
    {
        // prefs.putBoolean(PREF_VAR_INTERPRET, isVarInterpretationEnabled());
        // prefs.putBoolean(PREF_OVERRIDE, isOverrideEnabled());
        // prefs.putBoolean(PREF_VERIF, isAutoBuildEnabled());
        // prefs.putBoolean(PREF_STRICT, isStrictModeEnabled());
        prefs.putBoolean(PREF_INDENT_SPACES, isSoftTabsEnabled());
        prefs.putBoolean(PREF_FULL_AUTOCOMPLETE, isFullAutoCompleteEnabled());
        prefs.putBoolean(PREF_AUTOCLEAR_OUTPUT, isAutoClearOutputEnabled());
        prefs.putInt(PREF_INDENT_SPACES_VALUE, indentSpacesCount());
    }

    public void loadPrefs()
    {
        // varInterp = prefs.getBoolean(PREF_VAR_INTERPRET, Boolean.TRUE);
        // override = prefs.getBoolean(PREF_OVERRIDE, Boolean.TRUE);
        // autoVerif = prefs.getBoolean(PREF_VERIF, Boolean.TRUE);
        // strict = prefs.getBoolean(PREF_STRICT, Boolean.FALSE);
        fullAutocomplete = prefs.getBoolean(PREF_FULL_AUTOCOMPLETE, Boolean.TRUE);
        autoClearOutput = prefs.getBoolean(PREF_AUTOCLEAR_OUTPUT, true);
        soft = prefs.getBoolean(PREF_INDENT_SPACES, Boolean.FALSE);
        tabWidth = prefs.getInt(PREF_INDENT_SPACES_VALUE, 8);
    }

	public void setFullAutoCompleteEnabled(boolean enabled) {
		fullAutocomplete = enabled;
	}
	
	public void setAutoClearOutputEnabled(boolean enabled)
	{
		autoClearOutput = enabled;
	}

	public void setSoftTabsEnabled(boolean enabled) {
		soft = enabled;	
	}

	public void setTabWidth(int tabWidth) {
		this.tabWidth = tabWidth;
	}
}
