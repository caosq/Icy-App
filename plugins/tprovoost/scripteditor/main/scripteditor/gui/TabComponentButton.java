package plugins.tprovoost.scripteditor.gui;

import icy.util.EventUtil;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.plaf.basic.BasicButtonUI;

/**
 * Component to be used as tabComponent; Contains a JLabel to show the text and
 * a JButton to close the tab it belongs to
 */
public class TabComponentButton extends JPanel
{
    /** */
    private static final long serialVersionUID = 1L;
    private final ScriptingPanel panel;
    private final ScriptingEditor editor;

    public TabComponentButton(ScriptingEditor scriptingEditor, final ScriptingPanel panelCreated)
    {
        // unset default FlowLayout' gaps
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
        if (panelCreated == null)
        {
            throw new NullPointerException("TabbedPane is null");
        }
        this.panel = panelCreated;
        this.editor = scriptingEditor;
        
        setOpaque(false);

        // make JLabel read titles from JTabbedPane
        JLabel label = new JLabel()
        {
            /**  */
            private static final long serialVersionUID = 1L;

            public String getText()
            {
                JTabbedPane pane = editor.getTabbedPane();
                int i = pane.indexOfTabComponent(TabComponentButton.this);
                if (i != -1)
                {
                    return pane.getTitleAt(i);
                }
                return "";
            }
        };

        add(label);
        // add more space between the label and the button
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        // tab button
        JButton button = new TabButton();
        add(button);
        // add more space to the top of the component
        setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));

        // addMouseListener(this);
    }

    private class TabButton extends JButton implements ActionListener
    {
        /** */
        private static final long serialVersionUID = 1L;

        public TabButton()
        {
            int size = 17;
            setPreferredSize(new Dimension(size, size));
            setToolTipText("Close this tab");
            // Make the button looks the same for all Laf's
            setUI(new BasicButtonUI());
            // Make it transparent
            setContentAreaFilled(false);
            // No need to be focusable
            setFocusable(false);
            setBorder(BorderFactory.createEtchedBorder());
            setBorderPainted(false);
            // Making nice rollover effect
            // we use the same listener for all buttons
            addMouseListener(buttonMouseListener);
            setRolloverEnabled(true);
            // Close the proper tab by clicking the button
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent e)
        {
            deletePane();
        }

        // we don't want to update UI for this button
        public void updateUI()
        {
        }

        // paint the cross
        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            // shift the image for pressed buttons
            if (getModel().isPressed())
            {
                g2.translate(1, 1);
            }
            g2.setStroke(new BasicStroke(2));
            g2.setColor(Color.BLACK);
            if (getModel().isRollover())
            {
                g2.setColor(Color.MAGENTA);
            }
            int delta = 6;
            g2.drawLine(delta, delta, getWidth() - delta - 1, getHeight() - delta - 1);
            g2.drawLine(getWidth() - delta - 1, delta, delta, getHeight() - delta - 1);
            g2.dispose();
        }
    }

    private final MouseListener buttonMouseListener = new MouseAdapter()
    {
        public void mouseEntered(MouseEvent e)
        {
            Component component = e.getComponent();
            if (component instanceof AbstractButton)
            {
                AbstractButton button = (AbstractButton) component;
                button.setBorderPainted(true);
            }
        }

        public void mouseExited(MouseEvent e)
        {
            Component component = e.getComponent();
            if (component instanceof AbstractButton)
            {
                AbstractButton button = (AbstractButton) component;
                button.setBorderPainted(false);
            }
        }

        public void mouseClicked(MouseEvent e)
        {
            if (EventUtil.isMiddleMouseButton(e))
            {
                deletePane();
                e.consume();
            }
            else
                super.mouseClicked(e);
        };
    };

    /**
     * Remove this pane from the {@link JTabbedPane}.
     * 
     * @return false if the user has cancelled the operation
     */
    public boolean deletePane()
    {
        return editor.closeTab(this);
    }

	public ScriptingPanel getPanel() {
		return panel;
	}
}