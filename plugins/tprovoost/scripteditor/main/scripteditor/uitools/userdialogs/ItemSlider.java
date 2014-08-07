package plugins.tprovoost.scripteditor.uitools.userdialogs;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class ItemSlider extends Item
{
    private static final long serialVersionUID = 1L;
    private JSlider spinner;
    private JLabel lblValue;

    public ItemSlider(String name, int min, int max, int defaultValue)
    {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(new JLabel(name));

        spinner = new JSlider(min, max, defaultValue);
        spinner.addChangeListener(new ChangeListener()
        {

            @Override
            public void stateChanged(ChangeEvent e)
            {
                lblValue.setText("" + spinner.getValue());
            }
        });
        add(spinner);

        lblValue = new JLabel("" + defaultValue);
        add(lblValue);
    }

    @Override
    public Integer getValue()
    {
        return spinner.getValue();
    }

}
