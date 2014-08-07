package plugins.tprovoost.scripteditor.scriptinghandlers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import plugins.tprovoost.scripteditor.completion.IcyCompletionProvider;

/**
 * Object containing the Class needed and if necessary the Type associated. Can
 * be extremely useful when the class is an Array.
 * 
 * @author Thomas Provoost
 */
public class VariableType
{
	private Class<?> cType;

	// many places use getType().isEmpty(), which would give a NullPointerException
	// if type is not initialized
	private String type = "";

	public VariableType(Class<?> cType)
	{
		this.cType = cType;
	}

	public VariableType(Class<?> cType, String type)
	{
		this.cType = cType;
		this.type = type;
	}

	public Class<?> getClazz()
	{
		return cType;
	}

	public void setClassType(Class<?> cType)
	{
		this.cType = cType;
	}

	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public String[] getTypeParameters()
	{
		String typeParams = type.toString();
		int idxB = typeParams.indexOf('<');
		int idxE = typeParams.indexOf('>');

		typeParams.substring(idxB + 1, idxE);
		return typeParams.split(",");
	}

	@Override
	public String toString()
	{
		String typeS = (type == null || type.contentEquals("")) ? "" : " of " + type;
		if (cType == null)
			return "";
		return IcyCompletionProvider.getType(cType, true) + typeS;
	}

	public static boolean isGeneric(Class<?> clazz)
	{
		return clazz.getTypeParameters().length != 0;
	}

	public static String getType(String s)
	{
		Pattern p = Pattern.compile("(\\w|_|\\.)*\\s*<(.*)>");
		Matcher m = p.matcher(s);
		if (m.matches())
		{
			return m.group(2);
		}
		return "";
	}

}
