package plugins.tprovoost.scripteditor.uitools.userdialogs;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class ItemString extends Item
{
    private static final long serialVersionUID = 1L;
    private JTextField tf;

    public ItemString(final String name, final String initialText)
    {
        setLayout(new BoxLayout(ItemString.this, BoxLayout.X_AXIS));
        add(new JLabel(name));
        add(Box.createHorizontalStrut(4));
        tf = new JTextField(initialText);
        add(tf);
        add(Box.createHorizontalGlue());
    }

    public ItemString(String name, String initialText, int columns)
    {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(new JLabel(name));
        add(Box.createHorizontalStrut(4));
        add(new JTextField(initialText, columns));
        add(Box.createHorizontalGlue());
    }

    @Override
    public String getValue()
    {
        return tf.getText();
    }

}
