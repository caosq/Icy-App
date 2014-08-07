package plugins.tprovoost.scripteditor.scriptblock;

import org.python.core.PyObject;

public class PythonScriptBlock
{

    public static Object transformScriptOutput(Object o)
    {
        if (o instanceof PyObject)
            return ((PyObject) o).__tojava__(Object.class);
        return o;
    }

	public static Object transformInputForScript(Object value) {
		// place here any modification to a type
		// that is not natively supported.
		
		return value;
	}

}
