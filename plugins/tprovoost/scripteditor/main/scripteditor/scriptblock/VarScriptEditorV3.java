package plugins.tprovoost.scripteditor.scriptblock;

import icy.gui.component.button.IcyButton;
import icy.gui.frame.IcyFrame;
import icy.gui.frame.IcyFrameAdapter;
import icy.gui.frame.IcyFrameEvent;
import icy.gui.frame.IcyFrameListener;
import icy.resource.icon.IcyIcon;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import plugins.adufour.vars.gui.swing.SwingVarEditor;
import plugins.tprovoost.scripteditor.gui.ScriptingPanel;

public class VarScriptEditorV3 extends SwingVarEditor<String>
{
    protected ScriptingPanel panelOut;
    protected ScriptingPanel panelIn;
    protected IcyFrame frame;
    protected IcyFrameListener frameListener;

    public VarScriptEditorV3(VarScript varScript, String defaultValue)
    {
	super(varScript);
	panelIn.getTextArea().setText(defaultValue);
    }

    @Override
    public Dimension getPreferredSize()
    {
	return super.getPreferredSize();
    }

    @Override
    protected JComponent createEditorComponent()
    {
	panelIn = new ScriptingPanel("Internal Editor", "JavaScript");
	panelOut = new ScriptingPanel("External Editor", "JavaScript");
	panelOut.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

	frame = new IcyFrame("External Editor", true, true, true, true);
	frame.setContentPane(panelOut);
	frame.setSize(500, 500);
	frame.setVisible(true);
	frame.addToMainDesktopPane();
	frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
	frame.center();

	frameListener = new IcyFrameAdapter()
	{
	    @Override
	    public void icyFrameClosing(IcyFrameEvent e)
	    {
		RSyntaxTextArea textArea = panelIn.getTextArea();
		textArea.setEnabled(true);
		textArea.setText(panelOut.getTextArea().getText());
		textArea.repaint();
	    }
	};

	// building east component
	IcyButton buttonEdit = new IcyButton(new IcyIcon("redo.png", 12));
	buttonEdit.addActionListener(new ActionListener()
	{

	    @Override
	    public void actionPerformed(ActionEvent e)
	    {
		panelOut.getTextArea().setText(panelIn.getTextArea().getText());
		panelIn.getTextArea().setEnabled(false);
		frame.setVisible(true);
		frame.requestFocus();
	    }
	});
	JPanel eastPanel = new JPanel();
	eastPanel.setLayout(new BoxLayout(eastPanel, BoxLayout.Y_AXIS));
	eastPanel.add(Box.createVerticalGlue());
	eastPanel.add(buttonEdit);
	eastPanel.add(Box.createVerticalGlue());
	eastPanel.setOpaque(false);

	// to Return panel
	JPanel toReturn = new JPanel(new BorderLayout());
	toReturn.add(panelIn, BorderLayout.CENTER);
	toReturn.setOpaque(false);
	toReturn.add(eastPanel, BorderLayout.EAST);

	setComponentResizeable(true);
	panelIn.getTextArea().setCaretPosition(0);
	panelOut.getTextArea().setCaretPosition(0);
	return toReturn;
    }

    @Override
    public double getComponentVerticalResizeFactor()
    {
	return 1d;
    }

    @Override
    protected void activateListeners()
    {
	frame.addFrameListener(frameListener);
    }

    @Override
    protected void deactivateListeners()
    {
	frame.removeFrameListener(frameListener);
	frame.close();
    }

    @Override
    protected void updateInterfaceValue()
    {

    }

    public String getText()
    {
	if (panelIn == null || panelIn.getTextArea() == null)
	    return getVariable().getDefaultValue();
	return panelIn.getTextArea().getText();
    }

    public void setText(String newValue)
    {
	panelOut.getTextArea().setText(newValue);
	panelIn.getTextArea().setText(newValue);
    }

    public ScriptingPanel getPanelIn()
    {
	return panelIn;
    }

    public ScriptingPanel getPanelOut()
    {
	return panelOut;
    }

}
