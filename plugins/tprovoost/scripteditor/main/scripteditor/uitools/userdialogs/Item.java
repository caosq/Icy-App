package plugins.tprovoost.scripteditor.uitools.userdialogs;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

public abstract class Item extends JPanel
{
    private static final long serialVersionUID = 1L;

    public Item()
    {
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    public abstract Object getValue();
}
