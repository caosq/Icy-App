package plugins.tprovoost.scripteditor.gui;

import icy.preferences.XMLPreferences;
import icy.util.XMLUtil;

import java.io.File;
import java.util.ArrayList;

public class RecentFiles {
	private ArrayList<String> previousFiles = new ArrayList<String>();
	
	// remember up to MAX_RECENT_FILES files
	private final int MAX_RECENT_FILES = 20;
	
	XMLPreferences recentFilesPref;
	
	RecentFiles(XMLPreferences prefs)
	{
		this.recentFilesPref = prefs.node("recentFiles");
	}
	
	void add(File file)
	{
		add(file.getAbsolutePath());
	}
	
	void add(String fileName)
	{
		// remove the file if it is present
		previousFiles.remove(fileName);
		
		// insert the new file at the top of the list
		previousFiles.add(0, fileName);
		
		// trim the list
		if (previousFiles.size() > MAX_RECENT_FILES)
		{
			// remove the oldest
			previousFiles.remove(previousFiles.size()-1);
		}
	}
	
	/**
	 * Save the recent files to the XML file.
	 * Note that IcyPreferences.save() should be
	 * called explicitly after this.
	 */
	void save()
	{
		// remove previous settings
		// Note: I do not use recentFilesPref.removeChildren() because it leaves blank lines
		// in the XML file
		XMLUtil.removeAllChildren(recentFilesPref.getXMLNode());
		
		int i = 0;
		for (String fileName:previousFiles)
		{
			XMLPreferences key = recentFilesPref.node("entry" + i);
			key.put("file0", fileName);	
			i += 1;
		}
	}

	/**
	 * Load the recent files from the XML file.
	 */
	void load()
	{
		previousFiles.clear();
		
		for (XMLPreferences key : recentFilesPref.getChildren())
		{
			String fileName = key.get("file0", "");
			previousFiles.add(fileName);
		}
	}

	public ArrayList<String> getFiles() {
		return previousFiles;
	}
}
