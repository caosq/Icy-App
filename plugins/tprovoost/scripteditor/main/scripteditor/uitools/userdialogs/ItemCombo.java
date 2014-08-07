package plugins.tprovoost.scripteditor.uitools.userdialogs;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;

public class ItemCombo extends Item
{
    private static final long serialVersionUID = 1L;
    private JComboBox combo;

    public ItemCombo(String name, String[] items)
    {
        this(name, items, items[0]);
    }

    public ItemCombo(String name, String[] items, String defaultValue)
    {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        combo = new JComboBox(items);
        combo.setSelectedItem(defaultValue);
        add(new JLabel(name));
        add(Box.createHorizontalStrut(4));
        add(combo);
        add(Box.createHorizontalGlue());
    }

    @Override
    public String getValue()
    {
        return (String) combo.getSelectedItem();
    }

}
