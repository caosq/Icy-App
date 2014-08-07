package plugins.tprovoost.scripteditor.completion.types;

import japa.parser.ast.body.JavadocComment;
import japa.parser.ast.body.MethodDeclaration;

import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.Segment;

import org.fife.ui.autocomplete.CompletionProvider;

import plugins.tprovoost.scripteditor.javasource.ClassSource;
import plugins.tprovoost.scripteditor.javasource.JarAccess;
import plugins.tprovoost.scripteditor.scriptinghandlers.VariableType;

public class ScriptFunctionCompletion extends JavaFunctionCompletion
{
    private static HashMap<String, MethodDeclaration> cacheMetDecl = new HashMap<String, MethodDeclaration>();
    private static HashMap<String, List<Parameter>> cacheParams = new HashMap<String, List<Parameter>>();

    private Method method;
    private boolean isStatic;

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface BindingFunction
    {
        String value();
    }

    public ScriptFunctionCompletion(CompletionProvider provider, String name, Method method)
    {
        super(provider, name, method.getReturnType().isArray() ? method.getReturnType().getCanonicalName() : method
                .getReturnType().getName());
        this.method = method;
    }

    public ScriptFunctionCompletion(CompletionProvider provider, String name, Method method, VariableType type)
    {
        super(provider, name, type.getType());
        this.method = method;
    }

    /**
     * Get the correct function call in java.
     * 
     * @return
     */
    public String getMethodCall()
    {
        String parametersAsString = "";
        MethodDeclaration methodDecl = cacheMetDecl.get(method);
        if (methodDecl != null)
        {
            List<japa.parser.ast.body.Parameter> params = methodDecl.getParameters();
            for (int i = 0; i < params.size(); ++i)
            {
                japa.parser.ast.body.Parameter p = params.get(i);
                if (i != 0)
                    parametersAsString += " ," + p.getType() + " " + p.getId().getName();
                else
                    parametersAsString += p.getType() + " " + p.getId().getName();
            }
        }
        else
        {
            Class<?>[] paramTypes = method.getParameterTypes();
            for (int i = 0; i < paramTypes.length; ++i)
            {
                if (i != 0)
                    parametersAsString += " ,arg" + i;
                else
                    parametersAsString += "arg" + i;
            }
        }
        return "Packages." + method.getDeclaringClass().getName() + "." + method.getName() + "(" + parametersAsString
                + ");";
    }

    /**
     * Returns if the function should be accessed in a static way.
     * 
     * @return
     */
    public boolean isStatic()
    {
        if (method != null)
            return Modifier.isStatic(method.getModifiers());
        return false;
    }

    /**
     * @return
     */
    public Class<?> getOriginatingClass()
    {
        if (isStatic)
            return null;
        return method.getDeclaringClass();
    }

    public Method getMethod()
    {
        return method;
    }

    @Override
    public boolean equals(Object arg0)
    {
        if (!(arg0 instanceof ScriptFunctionCompletion))
            return false;
        return ((ScriptFunctionCompletion) arg0).getName().contentEquals(getName());
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

    @Override
    public Parameter getParam(int index)
    {
        List<Parameter> params = cacheParams.get(method.toGenericString());
        if (params != null)
        {
            return params.get(index);
        }
        if (!isParseDone)
        {
            populate();
        }
        return super.getParam(index);
    }

    @Override
    public String getSummary()
    {
        summary = cacheSummary.get(method.toGenericString());
        if (!isParseDone)
        {
            if (summary == null)
                populate();
        }
        StringBuilder sb = new StringBuilder();
        addDefinitionString(sb);
        if (!possiblyAddDescription(sb))
        {
            sb.append("<br><br><br>");
        }
        addParameters(sb);
        possiblyAddDefinedIn(sb);
        possiblyAddSource(sb);
        sb.append("</html>");
        String sumResult = sb.toString();
        return sumResult;
    }

    @Override
    protected void addParameters(StringBuilder sb)
    {
        int paramCount = getParamCount();
        if (paramCount > 0)
        {
            sb.append("<b>Parameters:</b><br>");
            sb.append("<center><table width='90%'><tr><td>");
            for (int i = 0; i < paramCount; i++)
            {
                Parameter param = getParam(i);
                sb.append("<b>");
                sb.append(param.getName() != null ? param.getName() : param.getType());
                sb.append(" : </b><br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
                String desc = param.getDescription();
                if (desc != null)
                {
                    sb.append(desc);
                }
                sb.append("<br>");
            }
            sb.append("</td></tr></table></center><br><br>");
        }
        String returnValDesc = getReturnValueDescription();
        if (returnValDesc != null)
        {
            sb.append("<b>Returns:</b><br><center><table width='90%'><tr><td>");
            sb.append(returnValDesc);
            sb.append("</td></tr></table></center><br><br>");
        }
    }

    private void possiblyAddSource(StringBuilder sb)
    {
        InputStream is = JarAccess.getJavaSourceInputStream(method.getDeclaringClass());
        if (is != null)
            sb.append("<hr><a href=\"SourceCodeLink\">View Source</a>");
    }

    protected void populate()
    {
        MethodDeclaration md = cacheMetDecl.get(method.toGenericString());
        if (md == null)
        {
            Class<?> currentClass = method.getDeclaringClass();
            while (md == null && currentClass != null)
            {
                try
                {
                    Method m = currentClass.getDeclaredMethod(this.method.getName(), this.method.getParameterTypes());
                    final ClassSource cs = ClassSource.getClassSource(currentClass);
                    if (!cs.isMethodsSet())
                    {
                        cs.populateMethods();
                    }
                    md = cs.getMethods().get(m.toGenericString());
                }
                catch (SecurityException e)
                {
                }
                catch (NoSuchMethodException e)
                {
                }
                currentClass = currentClass.getSuperclass();
            }
        }
        if (md != null)
        {
            JavadocComment comment = md.getJavaDoc();
            if (comment != null)
            {
                String content = comment.getContent();
                content = ClassSource.docCommentToHtml("/**" + content + "*/");
                summary = content;
            }
            HashMap<String, String> paramsHash = ClassSource.getParameters(summary);

            // Removes parameters from Doc since they are going to be handled in this.
            if (paramsHash.size() > 0 && summary != null)
            {
                int idx = summary.indexOf(ClassSource.PARAM_PATTERN);
                int idxEnd = summary.indexOf("</p>", idx);
                summary = summary.substring(0, idx) + summary.substring(idxEnd + "</p>".length());
            }

            // putting back the right parameters.
            ArrayList<Parameter> params = new ArrayList<Parameter>();
            List<japa.parser.ast.body.Parameter> list = md.getParameters();
            int size = getParamCount();
            if (list != null && list.size() == size)
            {
                for (int i = 0; i < getParamCount(); ++i)
                {
                    japa.parser.ast.body.Parameter sourceParam = list.get(i);
                    String name = sourceParam.getId().getName();
                    Parameter param = new Parameter(sourceParam.getType(), name);

                    params.add(param);
                    String desc = paramsHash.get(name);
                    if (desc != null)
                        param.setDescription(desc);
                }
                super.setParams(params);
                cacheParams.put(method.toGenericString(), params);
            }
            cacheSummary.put(method.toGenericString(), summary);
        }
        else
        {
            summary = "";
        }
        isParseDone = true;
    }

}
