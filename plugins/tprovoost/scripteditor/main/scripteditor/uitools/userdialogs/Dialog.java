package plugins.tprovoost.scripteditor.uitools.userdialogs;

import icy.main.Icy;
import icy.system.thread.ThreadUtil;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

public class Dialog
{
    private static final int DEFAULT_COLUMN_SIZE = 8;

    private ArrayList<Item> listItems = new ArrayList<Item>();

    private LinkedList<Item> listNumbers = new LinkedList<Item>();
    private LinkedList<Item> listStrings = new LinkedList<Item>();
    private LinkedList<Item> listcboxes = new LinkedList<Item>();
    private LinkedList<Item> listChoices = new LinkedList<Item>();

    private boolean ok = false;
    private JDialog dialog;
    private boolean working;

    public Dialog(final String name)
    {
        ThreadUtil.invokeLater(new Runnable()
        {

            @Override
            public void run()
            {
                dialog = new JDialog(Icy.getMainInterface().getMainFrame(), name);
            }
        });
    }

    public void addNumber(String label, double defaultValue)
    {
        addNumber(label, defaultValue, DEFAULT_COLUMN_SIZE, "");
    }

    public void addNumber(final String label, final double defaultValue, final int columns, final String unit)
    {
        ThreadUtil.invokeLater(new Runnable()
        {

            @Override
            public void run()
            {
                ItemNumber item = new ItemNumber(label, defaultValue, columns, unit);
                listItems.add(item);
                listNumbers.add(item);
            }
        });
    }

    public void show()
    {
        if (isWorking())
            return;
        setWorking(true);
        ThreadUtil.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                // generate the UI
                dialog.setModal(true);

                // MIDDLE PANEL
                JPanel panelMiddle = new JPanel();
                panelMiddle.setLayout(new BoxLayout(panelMiddle, BoxLayout.Y_AXIS));

                for (Item item : listItems)
                    panelMiddle.add(item);

                // SOUTH PANEL
                JPanel panelSouth = new JPanel();
                panelSouth.setLayout(new BoxLayout(panelSouth, BoxLayout.X_AXIS));

                JButton btnOK = new JButton("OK");
                btnOK.addActionListener(new ActionListener()
                {

                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        ok = true;
                        dialog.setVisible(false);
                    }
                });

                JButton btnCancel = new JButton("Cancel");
                btnCancel.addActionListener(new ActionListener()
                {

                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        ok = false;
                        dialog.setVisible(false);
                    }
                });

                panelSouth.add(Box.createHorizontalGlue());
                panelSouth.add(btnOK);
                panelSouth.add(Box.createHorizontalStrut(4));
                panelSouth.add(btnCancel);

                // CREATE CONTENT PANE
                JPanel contentPane = new JPanel(new BorderLayout());
                contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                contentPane.add(panelMiddle, BorderLayout.CENTER);
                contentPane.add(panelSouth, BorderLayout.SOUTH);

                dialog.setContentPane(contentPane);
                dialog.pack();
                dialog.setLocationRelativeTo(Icy.getMainInterface().getMainFrame());
                dialog.setVisible(true);
                dialog = null;

                setWorking(false);
            }
        });
    }

    public boolean getResult()
    {
        while (isWorking())
            ThreadUtil.sleep(100);
        return ok;
    }

    public void addMessage(String label)
    {
        listItems.add(new ItemMessage(label));
    }

    public void addString(final String label, final String initialText)
    {
        ThreadUtil.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                Item item = new ItemString(label, initialText);
                listItems.add(item);
                listStrings.add(item);
            }
        });
    }

    public void addString(final String label, final String initialText, final int columns)
    {
        ThreadUtil.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                Item item = new ItemString(label, initialText, columns);
                listItems.add(item);
                listStrings.add(item);
            }
        });
    }

    public void addSlider(final String label, final int min, final int max, final int defaultValue)
    {
        ThreadUtil.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                Item item = new ItemSlider(label, min, max, defaultValue);
                listItems.add(item);
                listNumbers.add(item);
            }
        });
    }

    public void addCheckbox(final String label, final boolean defaultValue)
    {
        ThreadUtil.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                Item item = new ItemCheckbox(label, defaultValue);
                listItems.add(item);
                listcboxes.add(item);
            }
        });
    }

    public void addChoice(String label, String[] items)
    {
        addChoice(label, items, items[0]);
    }

    public void addChoice(final String label, final String[] items, final String defaultValue)
    {
        ThreadUtil.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                Item item = new ItemCombo(label, items, defaultValue);
                listItems.add(item);
                listChoices.add(item);
            }
        });
    }

    public Number getNumber()
    {
        return (Number) listNumbers.pop().getValue();
    }

    public String getString()
    {
        return (String) listStrings.pop().getValue();
    }

    public Boolean getCheckBox()
    {
        return (Boolean) listcboxes.pop().getValue();
    }

    public String getChoice()
    {
        return (String) listChoices.pop().getValue();
    }

    /**
     * Returns if the current Dialog is being displayed.
     * 
     * @return working
     */
    private boolean isWorking()
    {
        return working;
    }

    /**
     * Synchronized setter.
     * 
     * @param working
     *        the working to set
     */
    private synchronized void setWorking(boolean working)
    {
        this.working = working;
    }
}
