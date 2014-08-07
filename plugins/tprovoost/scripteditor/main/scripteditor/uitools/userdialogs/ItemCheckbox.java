package plugins.tprovoost.scripteditor.uitools.userdialogs;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

public class ItemCheckbox extends Item
{
    private static final long serialVersionUID = 1L;
    private JCheckBox cbox;

    public ItemCheckbox(String name, boolean defaultValue)
    {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        cbox = new JCheckBox();
        cbox.setSelected(defaultValue);
        add(new JLabel(name));
        add(Box.createHorizontalStrut(4));
        add(cbox);
        add(Box.createHorizontalGlue());
    }

    @Override
    public Boolean getValue()
    {
        return cbox.isSelected();
    }

}
