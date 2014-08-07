package plugins.tprovoost.scripteditor.gui;

import icy.gui.dialog.MessageDialog;
import icy.main.Icy;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;

public class FindAndReplaceDialog extends JDialog implements ActionListener
{
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static FindAndReplaceDialog singleton;
    private JTextField searchField, replaceField;
    private JCheckBox matchCase, wholeWord, regex;
    private JButton btnFind, btnReplace, btnReplaceFind, btnReplaceAll, btnClose;
    private ScriptingEditor editor;
    private JRadioButton radioForward;
    private JRadioButton rdbtnBackward;

    private FindAndReplaceDialog(Frame parent)
    {
        super(parent);

        JPanel panelMain = new JPanel();
        panelMain.setBorder(new EmptyBorder(8, 8, 8, 8));
        setContentPane(panelMain);

        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        panelMain.setLayout(new BorderLayout(0, 0));

        JPanel panelTop = new JPanel();
        panelMain.add(panelTop, BorderLayout.NORTH);
        panelTop.setLayout(new BoxLayout(panelTop, BoxLayout.Y_AXIS));

        JPanel panelFind = new JPanel();
        panelTop.add(panelFind);
        panelFind.setBorder(new EmptyBorder(4, 4, 4, 4));
        panelFind.setLayout(new BoxLayout(panelFind, BoxLayout.X_AXIS));

        JLabel lblFind = new JLabel("Find:");
        panelFind.add(lblFind);

        panelFind.add(Box.createHorizontalStrut(61));

        searchField = new JTextField();
        panelFind.add(searchField);
        searchField.setColumns(10);

        JPanel panelReplace = new JPanel();
        panelTop.add(panelReplace);
        panelReplace.setBorder(new EmptyBorder(4, 4, 4, 4));
        panelReplace.setLayout(new BoxLayout(panelReplace, BoxLayout.X_AXIS));

        JLabel lblReplace = new JLabel("Replace with:");
        panelReplace.add(lblReplace);

        panelReplace.add(Box.createHorizontalStrut(20));

        replaceField = new JTextField();
        replaceField.setColumns(10);
        panelReplace.add(replaceField);

        JPanel panelDirection = new JPanel();
        panelTop.add(panelDirection);
        panelDirection
                .setBorder(new TitledBorder(null, "Direction", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        panelDirection.setLayout(new BoxLayout(panelDirection, BoxLayout.X_AXIS));

        ButtonGroup group = new ButtonGroup();
        radioForward = new JRadioButton("Forward");
        radioForward.setSelected(true);
        rdbtnBackward = new JRadioButton("Backward");

        group.add(radioForward);
        group.add(rdbtnBackward);
        panelDirection.add(radioForward);
        panelDirection.add(rdbtnBackward);
        panelDirection.add(Box.createHorizontalGlue());

        JPanel panelOptions = new JPanel();
        panelTop.add(panelOptions);
        panelOptions.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Options",
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        panelOptions.setLayout(new GridLayout(0, 1, 0, 0));

        JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(2, 2, 2, 2));
        panelOptions.add(panel);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        matchCase = new JCheckBox("Case sensitive");
        panel.add(matchCase);

        wholeWord = new JCheckBox("Whole word");
        panel.add(wholeWord);

        JPanel panel_1 = new JPanel();
        panel_1.setBorder(new EmptyBorder(2, 2, 2, 2));
        panelOptions.add(panel_1);
        panel_1.setLayout(new BoxLayout(panel_1, BoxLayout.X_AXIS));

        regex = new JCheckBox("Regular expressions");
        panel_1.add(regex);

        JPanel panelSouth = new JPanel();
        panelMain.add(panelSouth, BorderLayout.SOUTH);
        panelSouth.setLayout(new BoxLayout(panelSouth, BoxLayout.X_AXIS));

        panelSouth.add(Box.createHorizontalGlue());

        JPanel panelButtons = new JPanel();
        panelSouth.add(panelButtons);
        panelButtons.setLayout(new GridLayout(0, 2, 0, 0));

        btnFind = new JButton("Find");
        panelButtons.add(btnFind);

        btnReplaceFind = new JButton("Replace/Find");
        panelButtons.add(btnReplaceFind);

        btnReplace = new JButton("Replace");
        panelButtons.add(btnReplace);

        btnReplaceAll = new JButton("Replace All");
        panelButtons.add(btnReplaceAll);

        JLabel lblFiller = new JLabel("");
        panelButtons.add(lblFiller);

        btnClose = new JButton("Close");
        btnClose.addActionListener(new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                dispose();
            }
        });
        panelButtons.add(btnClose);

        setTitle("Find/Replace");
        getRootPane().setDefaultButton(btnFind);
        pack();
        setLocationRelativeTo(parent);

        // --------------
        // ACTIONS
        // ---------------
        btnFind.addActionListener(this);
        btnReplace.addActionListener(this);
        btnReplaceAll.addActionListener(this);
        btnReplaceFind.addActionListener(this);

        panelMain.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape");
        panelMain.getActionMap().put("escape", new AbstractAction()
        {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e)
            {
                dispose();
                singleton = null;
            }
        });
    }

    protected RSyntaxTextArea getTextArea()
    {
        if (editor != null)
        {
            Component comp = editor.getTabbedPane().getSelectedComponent();
            if (comp instanceof ScriptingPanel)
            {
                return ((ScriptingPanel) comp).getTextArea();
            }
        }
        return null;
    }

    public static void showDialog(ScriptingEditor editor)
    {
        FindAndReplaceDialog dialog = getInstance();
        dialog.editor = editor;
        RSyntaxTextArea textArea = dialog.getTextArea();
        if (textArea != null)
            dialog.searchField.setText(textArea.getSelectedText());
        dialog.searchField.selectAll();
        dialog.setVisible(true);
    }

    private static FindAndReplaceDialog getInstance()
    {
        if (singleton == null)
            singleton = new FindAndReplaceDialog(Icy.getMainInterface().getMainFrame());
        return singleton;
    }

    public void actionPerformed(ActionEvent e)
    {
        Object source = e.getSource();
        String text = searchField.getText();
        if (text.length() == 0)
            return;
        RSyntaxTextArea textArea = getTextArea();
        if (textArea == null)
        {
            MessageDialog.showDialog("No text to search in.");
            return;
        }
        if (source == btnFind)
            findOrReplace(false);
        else if (source == btnReplace)
            findOrReplace(true);
        else if (source == btnReplaceFind)
        {
            findOrReplace(true);
            findOrReplace(false);
        }
        else if (source == btnReplaceAll)
        {
            SearchContext cx = new SearchContext(text);
            cx.setRegularExpression(regex.isSelected());
            cx.setReplaceWith(replaceField.getText());
            cx.setMatchCase(matchCase.isSelected());
            cx.setWholeWord(wholeWord.isSelected());
            SearchResult result = SearchEngine.replaceAll(textArea, cx);
            JOptionPane.showMessageDialog(this, result.getCount() + " replacements made!");
        }
    }

    public boolean findOrReplace(boolean replace)
    {
        return findOrReplace(replace, radioForward.isSelected());
    }

    public boolean findOrReplace(boolean replace, boolean forward)
    {
        if (searchOrReplaceFromHere(replace, forward))
            return true;
        RSyntaxTextArea textArea = getTextArea();
        if (textArea == null)
            return false;
        int caret = textArea.getCaretPosition();
        textArea.setCaretPosition(forward ? 0 : textArea.getDocument().getLength());
        if (searchOrReplaceFromHere(replace, forward))
            return true;
        JOptionPane.showMessageDialog(this, "No match found!");
        textArea.setCaretPosition(caret);
        return false;
    }

    protected boolean searchOrReplaceFromHere(boolean replace)
    {
        return searchOrReplaceFromHere(radioForward.isSelected());
    }

    protected boolean searchOrReplaceFromHere(boolean replace, boolean forward)
    {
        SearchContext cx = new SearchContext(searchField.getText());
        cx.setRegularExpression(regex.isSelected());
        cx.setReplaceWith(replaceField.getText());
        cx.setMatchCase(matchCase.isSelected());
        cx.setSearchForward(forward);
        cx.setWholeWord(wholeWord.isSelected());
        if (replace)
        {
            return SearchEngine.replace(getTextArea(), cx).wasFound();
        }
        return SearchEngine.find(getTextArea(), cx).wasFound();
    }

    public boolean isReplace()
    {
        return btnReplace.isEnabled();
    }

    /**
     * Sets the content of the search field.
     * 
     * @param pattern
     *        The new content of the search field.
     */
    public void setSearchPattern(String pattern)
    {
        searchField.setText(pattern);
    }
}
