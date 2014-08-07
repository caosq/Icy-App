package plugins.tprovoost.scripteditor.scriptingconsole;

import icy.util.DateUtil;
import icy.util.GraphicsUtil;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.python.core.PyException;
import org.python.util.InteractiveConsole;

import plugins.tprovoost.scripteditor.gui.ConsoleOutput;
import plugins.tprovoost.scripteditor.scriptinghandlers.py.PythonScriptingHandler;

public class PythonScriptingconsole extends Scriptingconsole
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static final String STRING_INPUT = ">>> ";
    private static final String STRING_INPUT_MORE = "... ";

    private InteractiveConsole console;
    private boolean waitingForMore;

    public PythonScriptingconsole()
    {
        // Note: there is no way to reset the system state for an
        // InteractiveConsole,
        // so sys.path (for example) is shared for all instances of them!
        console = new InteractiveConsole();

        if (PythonScriptingHandler.getInterpreter() == null)
        {
            PythonScriptingHandler.setInterpreter(console);
        }

        // setText(STRING_INPUT);

        setMinimumSize(new Dimension(0, 25));
        setPreferredSize(new Dimension(0, 25));

        Insets insets = getMargin();
        if (insets != null)
        {
            setMargin(new Insets(insets.top, 30, insets.bottom, insets.right));
        }
        else
        {
            setMargin(new Insets(0, 30, 0, 0));
        }

    }

    @Override
    public boolean isEditable()
    {
        return super.isEditable();
    }

    public void setLanguage(String language)
    {
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
                        if (waitingForMore)
                            output.append(STRING_INPUT_MORE + getText() + "\n");
                        else
                            output.append(STRING_INPUT + getText() + "\n");
                    }
                    else
                        System.out.println(time + ": " + text);
                    try
                    {
                        waitingForMore = console.push(text);
                    }
                    catch (PyException pe)
                    {
                        if (scriptHandler != null)
                            scriptHandler.getEngine().getWriter().write(pe.toString());
                    }
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

    public void setOutput(ConsoleOutput outputNew)
    {
        output = outputNew;
        final StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true)
        {
            @Override
            public void write(String s)
            {
                if (output != null)
                {
                    output.append(s);
                }
            }
        };
        console.setOut(pw);
        console.setErr(pw);
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(Color.BLACK);
        if (waitingForMore)
            GraphicsUtil.drawCenteredString(g2, STRING_INPUT_MORE, getMargin().left - 10, getHeight() / 2, false);
        else
            GraphicsUtil.drawCenteredString(g2, STRING_INPUT, getMargin().left - 10, getHeight() / 2, false);
        g2.dispose();
    }
}
