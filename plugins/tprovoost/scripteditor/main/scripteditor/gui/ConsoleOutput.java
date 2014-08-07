package plugins.tprovoost.scripteditor.gui;

import icy.util.EventUtil;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.BorderFactory;
import javax.swing.BoundedRangeModel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.border.BevelBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Document;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

@SuppressWarnings("serial")
public class ConsoleOutput extends JPanel {

	protected boolean scrollLocked;
	final JTextPane textPane;
	private JScrollPane scrollpane;

	ConsoleOutput()
	{
		setLayout(new BorderLayout());
		
		textPane = new JTextPane();
		textPane.setEditable(false);
		textPane.setFont(new Font("sansserif", Font.PLAIN, 12));
		textPane.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));

		// HANDLE RIGHT CLICK POPUP MENU
		textPane.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (EventUtil.isRightMouseButton(e))
				{
					JPopupMenu popup = new JPopupMenu();
					JMenuItem itemCopy = new JMenuItem("Copy");
					itemCopy.addActionListener(new ActionListener()
					{

						@Override
						public void actionPerformed(ActionEvent e)
						{
							textPane.copy();
						}
					});
					popup.add(itemCopy);
					popup.show(textPane, e.getX(), e.getY());
					e.consume();
				}
			}
		});

		// by default the JTextPane will autoscroll everytime something is modified
		// in the document. So in our case it would always autoscroll to the bottom
		// of the console. We want to disable that and do the scrolling at our discretion.
		class NonSrollingCaret extends DefaultCaret {
			public void adjustVisibility(Rectangle rec) {}
		}
		textPane.setCaret(new NonSrollingCaret());

		// Create the scrollpane around the output
		scrollpane = new JScrollPane(textPane);
		scrollpane.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		scrollpane.setAutoscrolls(true);
		scrollpane.setPreferredSize(new Dimension(400, 200));
		add(scrollpane);
		
		final JScrollBar scrollbar = scrollpane.getVerticalScrollBar();

		// Listener for the scrollbar, to achieve auto-scroll or scroll-lock
		// depending on the position of the scrollbar
		scrollbar.addAdjustmentListener(new AdjustmentListener()
		{
			final BoundedRangeModel brm = scrollbar.getModel();

			@Override
			public void adjustmentValueChanged(AdjustmentEvent e)
			{
				if (scrollbar.getValueIsAdjusting())
				{
					boolean atBottom = (scrollbar.getValue() + scrollbar.getVisibleAmount() == scrollbar.getMaximum());
					if (atBottom)
						setScrollLocked(false);
					else
						setScrollLocked(true);
				}
				if (!isScrollLocked() && !textPane.getText().isEmpty())
				{
					brm.setValue(brm.getMaximum());
				}
			}

		});

		scrollpane.addMouseWheelListener(new MouseWheelListener()
		{

			@Override
			public void mouseWheelMoved(MouseWheelEvent e)
			{
				boolean atBottom = (scrollbar.getValue() + scrollbar.getVisibleAmount() == scrollbar.getMaximum());
				if (atBottom && e.getWheelRotation() >= 0)
				{
					// we are at bottom and asking to go down => we should disable the auto-scroll lock
					// and let the console auto-scroll
					setScrollLocked(false);
				}
				else
					setScrollLocked(true);
			}
		});
	}

	private synchronized boolean isScrollLocked()
	{
		return scrollLocked;
	}

	private synchronized void setScrollLocked(boolean scrollLocked)
	{
		this.scrollLocked = scrollLocked;
	}

	public JTextPane getTextPane() {
		return textPane;
	}

	public void append(String s) {
		Document doc = textPane.getDocument();
		try
		{
			Style style = textPane.getStyle("normal");
			if (style == null)
				style = textPane.addStyle("normal", null);
			doc.insertString(doc.getLength(), s, style);
		} catch (BadLocationException e2)
		{
		}
	}

	public void clear() {
		textPane.setText("");
	}

	public void appendError(String s) {
		Document doc = textPane.getDocument();
		try
		{
			Style style = textPane.getStyle("error");
			if (style == null)
			{
				style = textPane.addStyle("error", null);
				StyleConstants.setForeground(style, Color.red);
			}
			doc.insertString(doc.getLength(), s, style);
		}
		catch (BadLocationException e)
		{
		}
	}
}
