package plugins.tprovoost.scripteditor.completion.types;

import japa.parser.ast.body.ConstructorDeclaration;
import japa.parser.ast.body.JavadocComment;

import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
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

public class NewInstanceCompletion extends JavaFunctionCompletion
{
    private static HashMap<String, ConstructorDeclaration> cacheConsDecl = new HashMap<String, ConstructorDeclaration>();
    private static HashMap<String, List<Parameter>> cacheParams = new HashMap<String, List<Parameter>>();

    private Constructor<?> constructor;
    private boolean isStatic;
    private boolean isParseDone = false;

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface BindingFunction
    {
        String value();
    }

    public NewInstanceCompletion(CompletionProvider provider, String name, Constructor<?> constructor)
    {
        super(provider, name, constructor.getDeclaringClass().getName());
        this.constructor = constructor;
    }

    /**
     * Get the correct function call in java.
     * 
     * @return
     */
    public String getMethodCall()
    {
        ConstructorDeclaration constDecl = cacheConsDecl.get(constructor);
        String parametersAsString = "";
        if (constDecl != null)
        {
            List<japa.parser.ast.body.Parameter> params = constDecl.getParameters();
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
            Class<?>[] paramTypes = constructor.getParameterTypes();
            for (int i = 0; i < paramTypes.length; ++i)
            {
                if (i != 0)
                    parametersAsString += " ,arg" + i;
                else
                    parametersAsString += "arg" + i;
            }
        }
        return "Packages." + constructor.getDeclaringClass().getName() + "." + constructor.getName() + "("
                + parametersAsString + ");";
    }

    /**
     * Returns if the function should be accessed in a static way.
     * 
     * @return
     */
    public boolean isStatic()
    {
        if (constructor != null)
            return Modifier.isStatic(constructor.getModifiers());
        return false;
    }

    /**
     * @return
     */
    public Class<?> getOriginatingClass()
    {
        if (isStatic)
            return null;
        return constructor.getDeclaringClass();
    }

    public Constructor<?> getConstructor()
    {
        return constructor;
    }

    @Override
    public boolean equals(Object arg0)
    {
        if (!(arg0 instanceof NewInstanceCompletion))
            return false;
        return ((NewInstanceCompletion) arg0).getName().contentEquals(getName());
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
        List<Parameter> params = cacheParams.get(constructor.toGenericString());
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
        summary = cacheSummary.get(constructor.toGenericString());
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
        return sb.toString();
    }

    private void populate()
    {
        ConstructorDeclaration dc = cacheConsDecl.get(constructor.toGenericString());
        if (dc == null)
        {
            Class<?> currentClass = constructor.getDeclaringClass();
            while (dc == null && currentClass != null)
            {
                try
                {
                    Constructor<?> c = currentClass.getDeclaredConstructor(constructor.getParameterTypes());
                    final ClassSource cs = ClassSource.getClassSource(currentClass);
                    if (!cs.isConstructorsSet())
                    {
                        cs.populateConstructors();
                    }
                    dc = cs.getConstructors().get(c.toGenericString());
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
        if (dc != null)
        {
            JavadocComment comment = dc.getJavaDoc();
            if (comment != null)
            {
                String content = comment.getContent();
                content = ClassSource.docCommentToHtml("/**" + content + "*/");
                summary = content;
            }
            HashMap<String, String> paramsHash = ClassSource.getParameters(summary);

            if (paramsHash.size() > 0 && summary != null)
            {
                int idx = summary.indexOf(ClassSource.PARAM_PATTERN);
                int idxEnd = summary.indexOf("</p>", idx);
                summary = summary.substring(0, idx) + summary.substring(idxEnd + "</p>".length());
            }

            // putting back the right parameters.
            ArrayList<Parameter> params = new ArrayList<Parameter>();
            List<japa.parser.ast.body.Parameter> list = dc.getParameters();
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
                cacheParams.put(constructor.toGenericString(), params);
            }
            cacheSummary.put(constructor.toGenericString(), summary);
        }
        else
        {
            summary = "";
        }
        isParseDone = true;
    }
    
    private void possiblyAddSource(StringBuilder sb)
    {
        InputStream is = JarAccess.getJavaSourceInputStream(constructor.getDeclaringClass());
        if (is != null)
            sb.append("<hr><a href=\"SourceCodeLink\">View Source</a>");
    }
    
}
