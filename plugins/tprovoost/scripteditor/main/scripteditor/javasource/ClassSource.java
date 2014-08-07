package plugins.tprovoost.scripteditor.javasource;

import icy.system.thread.ThreadUtil;
import icy.util.StringUtil;
import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.BodyDeclaration;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.ConstructorDeclaration;
import japa.parser.ast.body.EnumDeclaration;
import japa.parser.ast.body.FieldDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.VariableDeclarator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClassSource
{

    /**
     * Optional leading text for doc comment lines (except the first line) that
     * should be removed if it exists.
     */
    static final Pattern DOC_COMMENT_LINE_HEADER = Pattern.compile("\\s*\\n\\s*\\*");// ^\\s*\\*\\s*[/]?");

    /**
     * Used to detect where are the parameters in the JavaDoc.
     */
    public static final String PARAM_PATTERN = "<p><b>Parameters:</b><p class='indented'>";

    /**
     * Pattern matching a link in a "@link" tag. This should match the
     * following:
     * <ul>
     * <li>ClassName</li>
     * <li>fully.qualified.ClassName</li>
     * <li>#method</li>
     * <li>#method(int, int)</li>
     * <li>String#method</li>
     * <li>String#method(params)</li>
     * <li>fully.qualified.ClassName#method</li>
     * <li>fully.qualified.ClassName#method(params)</li>
     * </ul>
     */
    static final Pattern LINK_TAG_MEMBER_PATTERN = Pattern.compile("(?:\\w+\\.)*\\w+(?:#\\w+(?:\\([^\\)]*\\))?)?|"
            + "#\\w+(?:\\([^\\)]*\\))?");

    /**
     * Cache containing all source codes for a given class.
     */
    private static HashMap<Class<?>, ClassSource> allClassSources = new HashMap<Class<?>, ClassSource>();

    private Class<?> clazz;
    private CompilationUnit cu;
    private HashMap<String, ConstructorDeclaration> constructors = new HashMap<String, ConstructorDeclaration>();
    private HashMap<String, MethodDeclaration> methods = new HashMap<String, MethodDeclaration>();
    private HashMap<String, VariableDeclarator> fields = new HashMap<String, VariableDeclarator>();
    private HashMap<String, ClassOrInterfaceDeclaration> classOrInterfaces = new HashMap<String, ClassOrInterfaceDeclaration>();
    private HashMap<String, EnumDeclaration> enums = new HashMap<String, EnumDeclaration>();

    private boolean constructorsSet = false;
    private boolean methodsSet = false;
    private boolean enumSet = false;
    private boolean fieldsSet = false;
    private boolean classOrInterfacesSet = false;
    private boolean working = false;

    private boolean DEBUG = false;

    public static synchronized ClassSource getClassSource(final Class<?> clazz)
    {
        ClassSource cm = allClassSources.get(clazz);
        if (cm == null)
        {
            cm = new ClassSource(clazz);
            final ClassSource cmFinal = cm;
            allClassSources.put(clazz, cmFinal);
            // ThreadUtil.bgRun(new Runnable()
            // {
            //
            // @Override
            // public void run()
            // {
            InputStream is = JarAccess.getJavaSourceInputStream(clazz);
            if (is != null)
                try
                {
                    cmFinal.cu = JavaParser.parse(is);
                }
                catch (ParseException e)
                {
                }
            // }
            // });
        }
        return cm;
    }

    private ClassSource(Class<?> clazz)
    {
        this.clazz = clazz;
    }

    public Class<?> getSourceClass()
    {
        return clazz;
    }

    public HashMap<String, ClassOrInterfaceDeclaration> getClassOrInterfaces()
    {
        return classOrInterfaces;
    }

    public HashMap<String, ConstructorDeclaration> getConstructors()
    {
        return constructors;
    }

    public HashMap<String, VariableDeclarator> getFields()
    {
        return fields;
    }

    public HashMap<String, MethodDeclaration> getMethods()
    {
        return methods;
    }

    public HashMap<String, EnumDeclaration> getEnums()
    {
        return enums;
    }

    public synchronized boolean isFieldsSet()
    {
        return fieldsSet;
    }

    public synchronized boolean isEnumSet()
    {
        return enumSet;
    }

    public synchronized boolean isClassOrInterfacesSet()
    {
        return classOrInterfacesSet;
    }

    public synchronized boolean isConstructorsSet()
    {
        return constructorsSet;
    }

    public synchronized boolean isMethodsSet()
    {
        return methodsSet;
    }

    public boolean isAllSet()
    {
        return fieldsSet && classOrInterfacesSet && constructorsSet && methodsSet && enumSet;
    }

    public void waitForAllSet()
    {
        while (!isAllSet())
        {
            ThreadUtil.sleep(100);
            return;
        }
    }

    public synchronized boolean isWorking()
    {
        return working;
    }

    public synchronized void setWorking(boolean working)
    {
        this.working = working;
    }

    public void populateAll()
    {
        if (working)
        {
            waitForAllSet();
            return;
        }
        working = true;
        if (!classOrInterfacesSet)
        {
            populateClassesOrInterfaces();
        }
        if (!fieldsSet)
        {
            populateFields();
        }
        if (!constructorsSet)
        {
            populateConstructors();

        }
        if (!methodsSet)
        {
            populateMethods();
        }
        if (!enumSet)
        {
            populateEnums();
        }
    }

    @SuppressWarnings("unchecked")
    public void populateClassesOrInterfaces()
    {
        if (working)
        {
            return;
        }
        if (cu != null)
        {
            // JAVA SOURCE VERSION
            ClassOrInterfaceVisitor coi = new ClassOrInterfaceVisitor();
            coi.visit(cu, null);
            ArrayList<BodyDeclaration> list = coi.getList();
            if (list.isEmpty())
            {
                System.out.println("Empty Class/Interface Declaration in " + clazz.getName());
            }
            else
            {
                classOrInterfaces.put(clazz.getName(), (ClassOrInterfaceDeclaration) list.get(0));
            }

        }
        classOrInterfacesSet = true;
        startPopulating();
    }

    @SuppressWarnings("unchecked")
    public void populateFields()
    {
        if (working)
        {
            return;
        }
        if (cu != null)
        {
            FieldVisitor coi = new FieldVisitor();
            coi.visit(cu, null);
            for (BodyDeclaration bd : coi.getList())
            {
                FieldDeclaration fd = (FieldDeclaration) bd;
                for (VariableDeclarator vd : fd.getVariables())
                {
                    fields.put(vd.getId().getName(), vd);
                }
            }
        }
        startPopulating();
        fieldsSet = true;
    }

    @SuppressWarnings("unchecked")
    public void populateEnums()
    {
        if (working)
            return;
        if (cu != null)
        {
            // JAVA SOURCE VERSION
            EnumVisitor ev = new EnumVisitor();
            ev.visit(cu, null);
            for (BodyDeclaration bd : ev.getList())
            {
                EnumDeclaration ed = (EnumDeclaration) bd;
                enums.put(ed.getName(), ed);
            }
        }
        startPopulating();
        fieldsSet = true;
    }

    @SuppressWarnings("unchecked")
    public void populateConstructors()
    {
        if (working)
            return;
        if (cu != null)
        {
            ConstructorVisitor cv = new ConstructorVisitor();
            cv.visit(cu, null);
            for (Constructor<?> c : clazz.getDeclaredConstructors())
            {
                L1: for (BodyDeclaration bd : cv.getList())
                {
                    ConstructorDeclaration cd = (ConstructorDeclaration) bd;
                    if (!cd.getName().contentEquals(clazz.getSimpleName()))
                        continue;
                    List<japa.parser.ast.body.Parameter> paramsSource = cd.getParameters();
                    Class<?> paramsReflect[] = c.getParameterTypes();
                    // ArrayList<Parameter> params = new ArrayList<Parameter>();
                    if (paramsSource != null && paramsSource.size() == paramsReflect.length)
                    {
                        for (int i = 0; i < paramsSource.size(); ++i)
                        {
                            japa.parser.ast.body.Parameter paramSource = paramsSource.get(i);
                            Class<?> paramReflect = paramsReflect[i];
                            String className;
                            if (paramReflect.isArray())
                            {
                                className = paramReflect.getCanonicalName();
                            }
                            else
                            {
                                className = paramReflect.getName();
                            }
                            String paramSourceType = paramSource.getType().toString();
                            int idx = paramSourceType.indexOf('<');
                            if (idx != -1)
                                paramSourceType = paramSourceType.substring(0, idx);
                            if (!className.contains(paramSourceType))
                                continue L1;
                            // else
                            // params.add(new Parameter(ClassUtil.getSimpleClassName(className),
                            // paramSource
                            // .getId().getName()));
                        }
                        constructors.put(c.toGenericString(), cd);
                    }
                }

                if (constructors.get(c.toGenericString()) == null && DEBUG)
                    System.out.println("No matching constructor in java: " + c.toGenericString());

            }
        }
        startPopulating();
        constructorsSet = true;
    }

    @SuppressWarnings("unchecked")
    public void populateMethods()
    {
        if (working)
            waitForAllSet();
        if (cu != null)
        {
            // JAVA SOURCE VERSION

            MethodVisitor mv = new MethodVisitor();
            mv.visit(cu, null);
            for (Method method : clazz.getDeclaredMethods())
            {
                L1: for (BodyDeclaration cd : mv.getList())
                {
                    MethodDeclaration md = ((MethodDeclaration) cd);
                    if (!md.getName().contentEquals(method.getName()))
                        continue;
                    List<japa.parser.ast.body.Parameter> paramsSource = md.getParameters();
                    Class<?> paramsReflect[] = method.getParameterTypes();
                    if (paramsSource == null || paramsSource.size() == paramsReflect.length)
                    {
                        if (paramsSource != null)
                        {
                            for (int i = 0; i < paramsSource.size(); ++i)
                            {
                                japa.parser.ast.body.Parameter paramSource = paramsSource.get(i);
                                Class<?> paramReflect = paramsReflect[i];
                                String className;
                                if (paramReflect.isArray())
                                {
                                    className = paramReflect.getCanonicalName();
                                }
                                else
                                {
                                    className = paramReflect.getName();
                                }
                                String paramSourceType = paramSource.getType().toString();
                                int idx = paramSourceType.indexOf('<');
                                if (idx != -1)
                                    paramSourceType = paramSourceType.substring(0, idx);
                                if (!className.contains(paramSourceType))
                                    continue L1;
                            }
                        }
                        methods.put(method.toGenericString(), (MethodDeclaration) cd);
                    }
                }
                if (methods.get(method.toGenericString()) == null && DEBUG)
                    System.out.println("No matching method in java: " + method.toGenericString());
            }
        }
        startPopulating();
        methodsSet = true;
    }

    /**
     * Reduces code size for the thread.
     */
    private void startPopulating()
    {
        ThreadUtil.bgRun(new Runnable()
        {

            @Override
            public void run()
            {
                populateAll();
            }
        });
    }

    private static final void appendDocCommentTail(StringBuffer sb, StringBuffer tail)
    {

        StringBuffer params = null;
        StringBuffer returns = null;
        StringBuffer throwsItems = null;
        StringBuffer see = null;
        StringBuffer seeTemp = null;
        StringBuffer since = null;
        StringBuffer author = null;
        StringBuffer version = null;
        StringBuffer unknowns = null;
        boolean inParams = false, inThrows = false, inReturns = false, inSeeAlso = false, inSince = false, inAuthor = false, inVersion = false, inUnknowns = false;

        String[] st = tail.toString().split("[ \t\r\n\f]+");
        String token = null;

        int i = 0;
        while (i < st.length && (token = st[i++]) != null)
        {
            if ("@param".equals(token) && i < st.length)
            {
                token = st[i++]; // Actual parameter
                if (params == null)
                {
                    params = new StringBuffer("<b>Parameters:</b><p class='indented'>");
                }
                else
                {
                    params.append("<br>");
                }
                params.append("<b>").append(token).append("</b> ");
                inSeeAlso = false;
                inParams = true;
                inReturns = false;
                inThrows = false;
                inSince = false;
                inAuthor = false;
                inVersion = false;
                inUnknowns = false;
            }
            else if ("@return".equals(token) && i < st.length)
            {
                if (returns == null)
                {
                    returns = new StringBuffer("<b>Returns:</b><p class='indented'>");
                }
                inSeeAlso = false;
                inReturns = true;
                inParams = false;
                inThrows = false;
                inSince = false;
                inAuthor = false;
                inVersion = false;
                inUnknowns = false;
            }
            else if ("@see".equals(token) && i < st.length)
            {
                if (see == null)
                {
                    see = new StringBuffer("<b>See Also:</b><p class='indented'>");
                    seeTemp = new StringBuffer();
                }
                else
                {
                    if (seeTemp.length() > 0)
                    {
                        String temp = seeTemp.substring(0, seeTemp.length() - 1);
                        // syntax is exactly the same as link
                        appendLinkTagText(see, temp);
                    }
                    see.append("<br>");
                    seeTemp.setLength(0);
                    // see.append("<br>");
                }
                inSeeAlso = true;
                inReturns = false;
                inParams = false;
                inThrows = false;
                inSince = false;
                inAuthor = false;
                inVersion = false;
                inUnknowns = false;
            }
            else if (("@throws".equals(token)) || ("@exception".equals(token)) && i < st.length)
            {
                token = st[i++]; // Actual throwable
                if (throwsItems == null)
                {
                    throwsItems = new StringBuffer("<b>Throws:</b><p class='indented'>");
                }
                else
                {
                    throwsItems.append("<br>");
                }
                throwsItems.append("<b>").append(token).append("</b> ");
                inSeeAlso = false;
                inParams = false;
                inReturns = false;
                inThrows = true;
                inSince = false;
                inAuthor = false;
                inVersion = false;
                inUnknowns = false;
            }
            else if ("@since".equals(token) && i < st.length)
            {
                if (since == null)
                {
                    since = new StringBuffer("<b>Since:</b><p class='indented'>");
                }
                inSeeAlso = false;
                inReturns = false;
                inParams = false;
                inThrows = false;
                inSince = true;
                inAuthor = false;
                inVersion = false;
                inUnknowns = false;
            }
            else if ("@author".equals(token) && i < st.length)
            {
                if (author == null)
                {
                    author = new StringBuffer("<b>Author:</b><p class='indented'>");
                }
                else
                {
                    author.append("<br>");
                }
                inSeeAlso = false;
                inReturns = false;
                inParams = false;
                inThrows = false;
                inSince = false;
                inAuthor = true;
                inVersion = false;
                inUnknowns = false;
            }
            else if ("@version".equals(token) && i < st.length)
            {
                if (version == null)
                {
                    version = new StringBuffer("<b>Version:</b><p class='indented'>");
                }
                else
                {
                    version.append("<br>");
                }
                inSeeAlso = false;
                inReturns = false;
                inParams = false;
                inThrows = false;
                inSince = false;
                inAuthor = false;
                inVersion = true;
                inUnknowns = false;
            }
            else if (token.startsWith("@") && token.length() > 1)
            {
                if (unknowns == null)
                {
                    unknowns = new StringBuffer();
                }
                else
                {
                    unknowns.append("</p>");
                }
                unknowns.append("<b>").append(token).append("</b><p class='indented'>");
                // Stop everything; unknown/unsupported tag
                inSeeAlso = false;
                inParams = false;
                inReturns = false;
                inThrows = false;
                inSince = false;
                inAuthor = false;
                inVersion = false;
                inUnknowns = true;
            }
            else if (inParams)
            {
                params.append(token).append(' ');
            }
            else if (inReturns)
            {
                returns.append(token).append(' ');
            }
            else if (inSeeAlso)
            {
                // see.append(token).append(' ');
                seeTemp.append(token).append(' ');
            }
            else if (inThrows)
            {
                throwsItems.append(token).append(' ');
            }
            else if (inSince)
            {
                since.append(token).append(' ');
            }
            else if (inAuthor)
            {
                author.append(token).append(' ');
            }
            else if (inVersion)
            {
                version.append(token).append(' ');
            }
            else if (inUnknowns)
            {
                unknowns.append(token).append(' ');
            }
        }

        sb.append("<p>");

        if (params != null)
        {
            sb.append(params).append("</p>");
        }
        if (returns != null)
        {
            sb.append(returns).append("</p>");
        }
        if (throwsItems != null)
        {
            sb.append(throwsItems).append("</p>");
        }
        if (see != null)
        {
            if (seeTemp.length() > 0)
            { // Last @see contents
                String temp = seeTemp.substring(0, seeTemp.length() - 1);
                // syntax is exactly the same as link
                appendLinkTagText(see, temp);
            }
            see.append("<br>");
            sb.append(see).append("</p>");
        }
        if (author != null)
        {
            sb.append(author).append("</p>");
        }
        if (version != null)
        {
            sb.append(version).append("</p>");
        }
        if (since != null)
        {
            sb.append(since).append("</p>");
        }
        if (unknowns != null)
        {
            sb.append(unknowns).append("</p>");
        }

    }

    /**
     * Appends HTML representing a "link" or "linkplain" Javadoc element to
     * a string buffer.
     * 
     * @param appendTo
     *        The buffer to append to.
     * @param linkContent
     *        The content of a "link" or "linkplain" item.
     */
    private static final void appendLinkTagText(StringBuffer appendTo, String linkContent)
    {
        appendTo.append("<a href='");
        linkContent = linkContent.trim(); // If "@link" and text on different lines
        Matcher m = LINK_TAG_MEMBER_PATTERN.matcher(linkContent);

        if (m.find() && m.start() == 0)
        {

            // System.out.println("Match!!! - '" + m.group(0));
            String match = m.group(0); // Prevents recalculation
            String link = match;
            // TODO: If this starts with '#', "link" must be prepended with
            // class name.

            String text = null;
            // No link "text" after the link location - just use link location
            if (match.length() == linkContent.length())
            {
                int pound = match.indexOf('#');
                if (pound == 0)
                { // Just a method or field in this class
                    text = match.substring(1);
                }
                else if (pound > 0)
                { // Not -1
                    String prefix = match.substring(0, pound);
                    if ("java.lang.Object".equals(prefix))
                    {
                        text = match.substring(pound + 1);
                    }
                }
                else
                { // Just use whole match (invalid link?)
                  // TODO: Could be just a class name. Find on classpath
                    text = match;
                }
            }
            else
            { // match.length() < linkContent.length()
                int offs = match.length();
                // Will usually skip just a single space
                while (offs < linkContent.length() && Character.isWhitespace(linkContent.charAt(offs)))
                {
                    offs++;
                }
                if (offs < linkContent.length())
                {
                    text = linkContent.substring(offs);
                }
            }

            // No "better" text for link found - just use match.
            if (text == null)
            {
                text = linkContent;// .substring(match.length());
            }

            // Replace the '#' sign, if any.
            text = fixLinkText(text);

            appendTo./* append("link://"). */append(link).append("'>").append(text);

        }
        else
        { // Malformed link tag
            System.out.println("Unmatched linkContent: " + linkContent);
            appendTo.append("'>").append(linkContent);
        }

        appendTo.append("</a>");

    }

    /**
     * Converts a Java documentation comment to HTML.
     * 
     * <pre>
     * This is a
     * pre block
     * </pre>
     * 
     * @param dc
     *        The documentation comment.
     * @return An HTML version of the comment.
     */
    public static final String docCommentToHtml(String dc)
    {

        if (dc == null)
        {
            return null;
        }
        if (dc.endsWith("*/"))
        {
            dc = dc.substring(0, dc.length() - 2);
        }

        // First, strip the line transitions. These always seem to be stripped
        // first from Javadoc, even when in between <pre> and </pre> tags.
        Matcher m = DOC_COMMENT_LINE_HEADER.matcher(dc);
        dc = m.replaceAll("\n");

        StringBuffer html = new StringBuffer();
        // "<html><style> .indented { margin-top: 0px; padding-left: 30pt; } </style><body>");
        StringBuffer tailBuf = null;

        BufferedReader r = new BufferedReader(new StringReader(dc));

        try
        {
            // Handle the first line (guaranteed to be at least 1 line).
            String line = r.readLine().substring(3);
            line = possiblyStripDocCommentTail(line);
            int offs = 0;
            while (offs < line.length() && Character.isWhitespace(line.charAt(offs)))
            {
                offs++;
            }
            if (offs < line.length())
            {
                html.append(line.substring(offs));
            }
            boolean inPreBlock = isInPreBlock(line, false);
            html.append(inPreBlock ? '\n' : ' ');

            // Read all subsequent lines.
            while ((line = r.readLine()) != null)
            {
                line = possiblyStripDocCommentTail(line);
                if (tailBuf != null)
                {
                    tailBuf.append(line).append(' ');
                }
                else if (line.trim().startsWith("@"))
                {
                    tailBuf = new StringBuffer();
                    tailBuf.append(line).append(' ');
                }
                else
                {
                    html.append(line);
                    inPreBlock = isInPreBlock(line, inPreBlock);
                    html.append(inPreBlock ? '\n' : ' ');
                }

            }

        }
        catch (IOException ioe)
        { // Never happens
            ioe.printStackTrace();
        }
        html = fixDocComment(html); // Fix stuff like "{@code}"
        if (tailBuf != null)
        {
            appendDocCommentTail(html, fixDocComment(tailBuf));
        }

        return html.toString();

    }

    public static String forXML(String aText)
    {
        final StringBuffer result = new StringBuffer();
        final StringCharacterIterator iterator = new StringCharacterIterator(aText);
        char character = iterator.current();
        while (character != CharacterIterator.DONE)
        {
            if (character == '<')
            {
                result.append("&lt;");
            }
            else if (character == '>')
            {
                result.append("&gt;");
            }
            else if (character == '\"')
            {
                result.append("&quot;");
            }
            else if (character == '\'')
            {
                result.append("&#039;");
            }
            else if (character == '&')
            {
                result.append("&amp;");
            }
            else
            {
                // the char is not a special one
                // add it to the result as is
                result.append(character);
            }
            character = iterator.next();
        }
        return result.toString();
    }

    private static final StringBuffer fixDocComment(StringBuffer text)
    {

        // Nothing to do.
        int index = text.indexOf("{@");
        if (index == -1)
        {
            return text;
        }

        // TODO: In Java 5, replace "sb.append(sb2.substring(...))"
        // calls with "sb.append(sb2, offs, len)".
        StringBuffer sb = new StringBuffer();
        int textOffs = 0;

        do
        {

            int closingBrace = indexOf('}', text, index + 2);
            if (closingBrace > -1)
            { // Should practically always be true

                sb.append(text.substring(textOffs, index));
                String content = text.substring(index + 2, closingBrace);
                index = textOffs = closingBrace + 1;

                if (content.startsWith("code "))
                {
                    sb.append("<code>").append(forXML(content.substring(5))).append("</code>");
                }

                else if (content.startsWith("link "))
                {
                    sb.append("<code>");
                    appendLinkTagText(sb, content.substring(5));
                    sb.append("</code>");
                }

                else if (content.startsWith("linkplain "))
                {
                    appendLinkTagText(sb, content.substring(10));
                }

                else if (content.startsWith("literal "))
                {
                    // TODO: Should escape HTML-breaking chars, such as '>'.
                    sb.append(content.substring(8));
                }

                else
                { // Unhandled Javadoc tag
                    sb.append("<code>").append(content).append("</code>");
                }

            }
            else
            {
                break; // Unclosed javadoc tag - just bail
            }

        }
        while ((index = text.indexOf("{@", index)) > -1);

        if (textOffs < text.length())
        {
            sb.append(text.substring(textOffs));
        }
        return sb;
    }

    /**
     * Tidies up a link's display text for use in a &lt;a&gt; tag.
     * 
     * @param text
     *        The text (a class, method, or field signature).
     * @return The display value for the signature.
     */
    private static final String fixLinkText(String text)
    {
        if (text.startsWith("#"))
        { // Method in the current class
            return text.substring(1);
        }
        return text.replace('#', '.');
    }

    /**
     * Returns the next location of a single character in a character sequence.
     * This method is here because <tt>StringBuilder</tt> doesn't get this
     * method added to it until Java 1.5.
     * 
     * @param ch
     *        The character to look for.
     * @param sb
     *        The character sequence.
     * @param offs
     *        The offset at which to start looking.
     * @return The next location of the character, or <tt>-1</tt> if it is not
     *         found.
     */
    private static final int indexOf(char ch, CharSequence sb, int offs)
    {
        while (offs < sb.length())
        {
            if (ch == sb.charAt(offs))
            {
                return offs;
            }
            offs++;
        }
        return -1;
    }

    /**
     * Returns whether this line ends in the middle of a pre-block.
     * 
     * @param line
     *        The line's contents.
     * @param prevValue
     *        Whether this line started in a pre-block.
     * @return Whether the line ends in a pre-block.
     */
    private static final boolean isInPreBlock(String line, boolean prevValue)
    {
        int lastPre = line.lastIndexOf("pre>");
        if (lastPre <= 0)
        {
            return prevValue;
        }
        char prevChar = line.charAt(lastPre - 1);
        if (prevChar == '<')
        {
            return true;
        }
        else if (prevChar == '/' && lastPre >= 2)
        {
            if (line.charAt(lastPre - 2) == '<')
            {
                return false;
            }
        }
        return prevValue;
    }

    /**
     * Removes the tail end of a documentation comment from a string, if it
     * exists.
     * 
     * @param str
     *        The string.
     * @return The string, possibly with the documentation comment tail
     *         removed.
     */
    private static final String possiblyStripDocCommentTail(String str)
    {
        if (str.endsWith("*/"))
        {
            str = str.substring(0, str.length() - 2);
        }
        return str;
    }

    public static HashMap<String, String> getParameters(String doc)
    {
        HashMap<String, String> toReturn = new HashMap<String, String>();
        if (doc != null && doc.contains("<b>Parameters:</b>"))
        {
            int idx = doc.indexOf(PARAM_PATTERN) + PARAM_PATTERN.length();

            doc = doc.substring(idx, doc.indexOf("</p>", idx));

            int end = doc.indexOf("<br>");

            while (!StringUtil.isEmpty(doc))
            {
                String parameterDoc = doc;
                if (end != -1)
                    parameterDoc = doc.substring(0, end);

                Pattern p = Pattern.compile("<b>(.*)</b>(.*)");
                Matcher m = p.matcher(parameterDoc);
                if (m.find())
                {
                    toReturn.put(m.group(1), m.group(2));
                }
                if (end != -1)
                {
                    doc = doc.substring(end + "<br>".length());
                    end = doc.indexOf("<br>");
                }
                else
                    doc = "";
            }
        }
        return toReturn;
    }
}
