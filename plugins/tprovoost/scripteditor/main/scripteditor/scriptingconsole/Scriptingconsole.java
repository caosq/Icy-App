package plugins.tprovoost.scripteditor.scriptingconsole;

import icy.util.DateUtil;
import icy.util.EventUtil;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;

import org.fife.ui.autocomplete.Completion;

import plugins.tprovoost.scripteditor.completion.IcyCompletionProvider;
import plugins.tprovoost.scripteditor.gui.ConsoleOutput;
import plugins.tprovoost.scripteditor.scriptinghandlers.ScriptingHandler;
import plugins.tprovoost.scripteditor.scriptinghandlers.js.JSScriptingHandlerRhino;
import plugins.tprovoost.scripteditor.scriptinghandlers.py.PythonScriptingHandler;

// import plugins.tprovoost.scripteditor.main.scriptinghandlers.JSScriptingHandler7;

public class Scriptingconsole extends JTextField implements KeyListener, MouseListener
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static final int MAX_PER_LINE = 5;
    protected IcyCompletionProvider provider;
    protected ScriptingHandler scriptHandler;
    protected ArrayList<String> history = new ArrayList<String>();
    protected int posInHistory = 0;
    protected ConsoleOutput output;

    public Scriptingconsole()
    {
        addKeyListener(this);
        addMouseListener(this);

        provider = new IcyCompletionProvider();
        provider.installDefaultCompletions("JavaScript");
        // // if (System.getProperty("java.version").startsWith("1.6.")) {
        // scriptHandler = new JSScriptingHandler62(provider, this, null,
        // false);
        // // } else {
        scriptHandler = new JSScriptingHandlerRhino(provider, this, null, false);
        // // }
        scriptHandler.setNewEngine(false);
        scriptHandler.setForceRun(false);
        scriptHandler.setStrict(false);
        scriptHandler.setVarInterpretation(false);

        setMinimumSize(new Dimension(0, 25));
        setPreferredSize(new Dimension(0, 25));
    }

    public void close()
    {
        removeKeyListener(this);
        removeMouseListener(this);
    }

    public void setLanguage(String language)
    {
        provider.clear();
        if (language.contentEquals("JavaScript"))
        {
            provider = new IcyCompletionProvider();
            provider.installDefaultCompletions("javascript");
            // if (System.getProperty("java.version").startsWith("1.6."))
            // scriptHandler = new JSScriptingHandler62(provider, this, null,
            // false);
            // else
            scriptHandler = new JSScriptingHandlerRhino(provider, this, null, false);
        }
        else if (language.contentEquals("Python"))
        {
            provider = new IcyCompletionProvider();
            provider.installDefaultCompletions("Python");
            scriptHandler = new PythonScriptingHandler(provider, this, null, false);
        }
        else
        {
            scriptHandler = null;
        }
        if (scriptHandler != null)
        {
            scriptHandler.setNewEngine(false);
            scriptHandler.setForceRun(false);
            scriptHandler.setStrict(false);
            scriptHandler.setVarInterpretation(false);
        }
    }

    @Override
    public void keyTyped(KeyEvent e)
    {
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        String text = getText();
        switch (e.getKeyCode())
        {
            case KeyEvent.VK_SPACE:
                e.consume();
                List<Completion> completions = provider.getCompletions(this);
                if (completions == null || !e.isControlDown())
                {
                    return;
                }
                if (completions.size() == 1)
                {
                    this.setText(completions.get(0).getReplacementText());
                }
                else
                {
                    int i = 0;
                    String s = "";
                    for (Completion c : completions)
                    {
                        s += c.getReplacementText() + "\t";
                        if (i != 0 && i % MAX_PER_LINE == 0)
                            s += "\n";
                        ++i;
                    }
                    if (!s.endsWith("\n"))
                        s += "\n";
                    output.append(s);
                    System.out.println(s);
                }
                break;
            case KeyEvent.VK_UP:
                if (posInHistory < history.size() - 1)
                {
                    ++posInHistory;
                    setText(history.get(posInHistory));
                    e.consume();
                }
                break;

            case KeyEvent.VK_DOWN:
                if (posInHistory > 0)
                {
                    --posInHistory;
                    setText(history.get(posInHistory));
                    e.consume();
                }
                break;

            case KeyEvent.VK_ENTER:
                if (!text.isEmpty())
                {
                    String time = DateUtil.now("HH:mm:ss");
                    if (output != null)
                    {
                        output.append("> " + text + "\n");
                    }
                    else
                        System.out.println(time + ": " + text);
                    if (scriptHandler != null)
                        scriptHandler.interpret(true);
                    history.add(0, text);
                    setText("");
                    posInHistory = -1;
                    BindingsScriptFrame.getInstance().update();
                    e.consume();
                }
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e)
    {

    }

    public void setOutput(ConsoleOutput consoleOutput)
    {
        this.output = consoleOutput;
        if (scriptHandler != null)
            scriptHandler.setOutput(consoleOutput);
    }

    public void clear()
    {
        if (output != null)
            output.clear();
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        if (EventUtil.isRightMouseButton(e))
        {
            JPopupMenu popup = new JPopupMenu();
            JMenuItem itemPaste = new JMenuItem("Paste");
            itemPaste.addActionListener(new ActionListener()
            {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    paste();
                }
            });
            popup.add(itemPaste);
            popup.show(this, e.getX(), e.getY());
            e.consume();
        }
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
    }

}
