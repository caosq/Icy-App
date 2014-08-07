package plugins.tprovoost.scripteditor.scriptinghandlers;

import java.lang.reflect.Method;

public class IcyFunctionBlock
{

    private String functionName;
    private int startOffset;
    private VariableType returnType;
    private Method m;

    /**
     * @param functionName
     * @param startOffset
     * @param endOffset
     * @param returnType
     */
    public IcyFunctionBlock(String functionName, int startOffset, VariableType returnType)
    {
        this.functionName = functionName;
        this.startOffset = startOffset;
        this.returnType = returnType;
    }

    public String getFunctionName()
    {
        return functionName;
    }

    public int getStartOffset()
    {
        return startOffset;
    }

    public VariableType getReturnType()
    {
        return returnType;
    }

    public void setStartOffset(int startOffset)
    {
        this.startOffset = startOffset;
    }

    // public void setReturnType(String returnType)
    // {
    // try
    // {
    // int idx = returnType.indexOf('<');
    // if (idx != -1)
    // {
    // Pattern p = Pattern.compile("(\\w|_)*\\s*<(.*)>");
    // Matcher m = p.matcher(returnType);
    // if (m.matches())
    // {
    // System.out.println("classname: " + m.group(0));
    // System.out.println("Anonymous: " + m.group(2));
    // }
    // returnType = returnType.substring(0, idx);
    // }
    // setReturnType(ClassUtil.findClass(returnType));
    // }
    // catch (ClassNotFoundException e)
    // {
    // returnType = null;
    // }
    // }

    public void setReturnType(VariableType returnType)
    {
        this.returnType = returnType;
    }

    public void setMethod(Method m)
    {
        this.m = m;
    }

    public Method getMethod()
    {
        return m;
    }

}
