package plugins.tprovoost.scripteditor.javasource;

import icy.util.ClassUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedList;

public class JarAccess
{
    private static LinkedList<String> toSource = new LinkedList<String>();

    public static InputStream getJavaSourceInputStream(Class<?> clazz)
    {
        String className;
        if (clazz.isArray())
            className = clazz.getCanonicalName();
        else
            className = clazz.getName();
        return getJavaSourceInputStream(className);
    }

    /**
     * Make a sensible effort to get the path of the source for a class.
     */
    public static InputStream getJavaSourceInputStream(String className)
    {
        // pauseSourcingProcess(); TODO

        // First, let's try to get the .jar file for said class.
        URL result = getURLJava(className);
        if (result == null)
            return null;

        try
        {
            InputStream stream = result.openStream();
            toSource.add(className);
            return stream;
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public static URL getURLJava(String className)
    {
        try
        {
            Class<?> clazz = ClassUtil.findClass(className);
            String baseName = className;
            int dot = baseName.lastIndexOf('.');
            if (dot > 0)
                baseName = baseName.substring(dot + 1);
            URL urlResource = clazz.getResource(baseName + ".java");
            if (urlResource != null)
            {
                return urlResource;
            }
        }
        catch (ClassNotFoundException e)
        {
        	// ignore
        }
        return null;
    }
}
