package plugins.tprovoost.scripteditor.scriptblock;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaObject;

public class JSScriptBlock
{

    public static Object transformScriptOutput(Object o)
    {
        Object toReturn = o;
        if (o instanceof NativeArray)
        {
            toReturn = convertNativeArraytoJSArray((NativeArray) o);
        }
        else if (o instanceof NativeJavaObject)
        {
            return ((NativeJavaObject) o).unwrap();
        }
        else
        {
            toReturn = Context.jsToJava(o, Object.class);
        }
        return toReturn;
    }

    public static Object transformInputForScript(Object o)
    {
        Object toReturn = o;
        // Context cx = Context.enter();
        // try
        // {
        // cx.setApplicationClassLoader(PluginLoader.getLoader());
        // toReturn = Context.javaToJS(toReturn, new ImporterTopLevel(cx));
        // }
        // finally
        // {
        // Context.exit();
        // }
        // if (toReturn instanceof NativeJavaArray)
        // {
        // toReturn = ((NativeJavaArray) toReturn).unwrap();
        // }
        return toReturn;
    }

    public static Object convertNativeArraytoJSArray(NativeArray array)
    {
        int len = (int) array.getLength();
        Object[] toReturn = new Object[len];
        for (int i = 0; i < array.getLength(); ++i)
        {
            toReturn[i] = transformScriptOutput(array.get(i, null));
        }
        return toReturn;
    }
}
