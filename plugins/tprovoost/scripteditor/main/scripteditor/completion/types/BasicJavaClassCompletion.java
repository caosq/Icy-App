package plugins.tprovoost.scripteditor.completion.types;

import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.JavadocComment;

import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.HashMap;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.Segment;

import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.VariableCompletion;

import plugins.tprovoost.scripteditor.javasource.ClassSource;
import plugins.tprovoost.scripteditor.javasource.JarAccess;

public class BasicJavaClassCompletion extends VariableCompletion
{
    private static HashMap<Class<?>, String> cache = new HashMap<Class<?>, String>();
    private Class<?> clazz;
    private boolean importOnly;
    private boolean parsingDone = false;

    public BasicJavaClassCompletion(CompletionProvider provider, Class<?> clazz)
    {
        this(provider, clazz, false);
    }

    public BasicJavaClassCompletion(CompletionProvider provider, Class<?> clazz, boolean importOnly)
    {
        super(provider, clazz.getSimpleName(), clazz.getSimpleName());
        this.clazz = clazz;
        this.importOnly = importOnly;
    }

    public Class<?> getJavaClass()
    {
        return clazz;
    }

    public boolean isParsingDone()
    {
        return parsingDone;
    }

    public boolean isAbstract()
    {
        return Modifier.isAbstract(clazz.getModifiers());
    }

    @Override
    public String getSummary()
    {
        String summaryCache = cache.get(clazz);
        // return super.getSummary();
        if (!parsingDone)
        {
            if (summaryCache == null)
            {
                final ClassSource cs = ClassSource.getClassSource(clazz);
                if (!cs.isClassOrInterfacesSet())
                {
                    cs.populateClassesOrInterfaces();
                }
                ClassOrInterfaceDeclaration coi = cs.getClassOrInterfaces().get(clazz.getName());
                if (coi != null && coi.getJavaDoc() != null)
                {
                    JavadocComment comment = coi.getJavaDoc();
                    if (comment != null)
                    {
                        String content = comment.getContent();
                        content = ClassSource.docCommentToHtml("/**" + content + "*/");
                        summaryCache = content;
                    }
                }
                else
                {
                    summaryCache = "";
                }
                cache.put(clazz, summaryCache);
            }
            parsingDone = true;
            super.setSummary(summaryCache);
        }
        StringBuilder sb = new StringBuilder();
        addDefinitionString(sb);
        possiblyAddDescription(sb);
        possiblyAddDefinedIn(sb);
        possiblyAddSource(sb);
        sb.append("</html>");
        return sb.toString();
    }

    private void possiblyAddSource(StringBuilder sb)
    {
        InputStream is = JarAccess.getJavaSourceInputStream(clazz);
        if (is != null)
            sb.append("<hr><a href=\"SourceCodeLink\">View Source</a>"); // TODO: Localize me
    }

    @Override
    public String getShortDescription()
    {
        if (parsingDone && cache.get(clazz) != null)
            return cache.get(clazz);
        return super.getDefinitionString();
    }

    @Override
    public String getAlreadyEntered(JTextComponent comp)
    {
        // return super.getAlreadyEntered(comp);
        Document doc = comp.getDocument();
        Segment seg = new Segment();

        int dot = comp.getCaretPosition();
        Element root = doc.getDefaultRootElement();
        int index = root.getElementIndex(dot);
        Element elem = root.getElement(index);
        int start = elem.getStartOffset();
        int len = dot - start;
        try
        {
            doc.getText(start, len, seg);
        }
        catch (BadLocationException ble)
        {
            ble.printStackTrace();
            return "";
        }

        int segEnd = seg.offset + len;
        start = segEnd - 1;
        while (start >= seg.offset && isValidChar(seg.array[start]))
        {
            start--;
        }
        start++;

        len = segEnd - start;
        return len == 0 ? "" : new String(seg.array, start, len);
    }

    private boolean isValidChar(char ch)
    {
        return Character.isLetterOrDigit(ch) || ch == '_' || ch == '\"';
    }

    public boolean importOnly()
    {
        return importOnly;
    }

}
