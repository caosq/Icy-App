package plugins.tprovoost.scripteditor.search;

import icy.file.xml.XMLPersistent;
import icy.plugin.PluginLoader;
import icy.util.XMLUtil;

import javax.swing.ImageIcon;

import org.w3c.dom.Node;

import plugins.tprovoost.scripteditor.main.ScriptEditorPlugin;

public class ScriptDescriptor implements XMLPersistent
{
	private String name;
	private String description;
	private String author;
	private String url;
	private String fileurl;
	private String extension;
	private String version;

	public ScriptDescriptor(Node node)
	{
		loadFromXML(node);
	}

	public ScriptDescriptor(String name, String url)
	{
		this.name = name;
		this.url = url;
	}

	public String getName()
	{
		return name;
	}

	public String getDescription()
	{
		return description;
	}

	public String getExtension()
	{
		return extension;
	}

	public String getAuthor()
	{
		return author;
	}
	
	public String getVersion()
	{
		return version;
	}

	public String getUrl()
	{
		return url;
	}

	public String getFileUrl()
	{
		return fileurl;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public void setAuthor(String author)
	{
		this.author = author;
	}

	public void setUrl(String url)
	{
		this.url = url;
	}

	@Override
	public boolean loadFromXML(Node node)
	{
		this.name = XMLUtil.getValue(XMLUtil.getElement(node, "name"), "No name");
		this.author = XMLUtil.getValue(XMLUtil.getElement(node, "author"), "No author");
		this.url = XMLUtil.getValue(XMLUtil.getElement(node, "url"), "");
		this.fileurl = XMLUtil.getValue(XMLUtil.getElement(node, "fileurl"), "");
		this.description = XMLUtil.getValue(XMLUtil.getElement(node, "shortDescription"), "No description");
		this.extension = XMLUtil.getValue(XMLUtil.getElement(node, "language"), "js");
		this.version = XMLUtil.getValue(XMLUtil.getElement(node, "version"), "1");
		return true;
	}

	@Override
	public boolean saveToXML(Node node)
	{
		return false;
	}

	public ImageIcon getIcon()
	{
		return PluginLoader.getPlugin(ScriptEditorPlugin.class.getName()).getIcon();
	}
}
