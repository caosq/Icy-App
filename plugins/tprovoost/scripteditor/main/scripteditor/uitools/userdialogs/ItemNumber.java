package plugins.tprovoost.scripteditor.uitools.userdialogs;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class ItemNumber extends Item
{
    private static final long serialVersionUID = 1L;
    private JTextField tf;

    public ItemNumber(final String name, final double valueDefault, final int columns, final String unit)
    {
        setLayout(new BoxLayout(ItemNumber.this, BoxLayout.X_AXIS));

        tf = new JTextField(columns);
        tf.setText("" + valueDefault);

        add(new JLabel(name));
        add(Box.createHorizontalStrut(4));
        add(tf);
        add(Box.createHorizontalStrut(4));
        add(new JLabel(unit));
        add(Box.createHorizontalGlue());
    }

    @Override
    public Double getValue()
    {
        return Double.parseDouble(tf.getText());
    }

}
