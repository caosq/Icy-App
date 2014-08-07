package plugins.tprovoost.scripteditor.uitools.userdialogs;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;

public class ItemMessage extends Item
{
    private static final long serialVersionUID = 1L;

    public ItemMessage(String name)
    {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(new JLabel(name));
        add(Box.createHorizontalGlue());
    }

    @Override
    public String getValue()
    {
        return "";
    }

}
