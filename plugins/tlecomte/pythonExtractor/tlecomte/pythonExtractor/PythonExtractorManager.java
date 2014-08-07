package plugins.tlecomte.pythonExtractor;

/*
 * Copyright 2013 Institut Pasteur.
 * 
 * This file is part of ICY.
 * 
 * ICY is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ICY is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ICY. If not, see <http://www.gnu.org/licenses/>.
 */

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import plugins.adufour.ezplug.EzButton;
import plugins.adufour.ezplug.EzLabel;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVarText;

public class PythonExtractorManager extends EzPlug implements ActionListener {
	
	EzLabel description = new EzLabel("The Python Extractor walks the plugins list"
			+ " to extract all the Python scripts packaged inside the plugins."
			+ "\n\nThis operation is done automatically each time Icy starts and each"
			+ " time the plugins list is reloaded (after an update for example)."
			+ "\n\nIn some cases, you may want to force this operation manually."
			+ " This is useful when developping your own Python plugins inside"
			+ " Eclipse for example.");
	EzButton startButton = null;

	EzLabel folderDescription = new EzLabel("\nFor your information, the Python files"
			+ " are extracted to the following folder:");
	EzVarText folder = new EzVarText("Extraction path","");
	
	EzLabel customizeDescription = new EzLabel("\nYou can customize your Python installation"
			+ " by creating or editing the following file:");
	EzVarText sitecustomizePath = new EzVarText("Site customization script", "");
	EzLabel customizeDescription2 = new EzLabel("If it exists, this file is executed automatically"
			+ " each time the Python interpreter is launched. This file is a good place to extend the Python import"
			+ " path with the directories containing your own Python modules."
			+ "\n"
			+ "\n"
			+ "Please note that the embedded Python interpreter inside Icy also offers the"
			+ " other customization mechanisms offered by Python 2.7 (USERS_SITE, usercustomize.py, etc.)."
			+ " Look at http://docs.python.org/2/library/site.html for more information.");  
	
	public PythonExtractorManager() {
		startButton = new EzButton("Force extraction", this);
		description.setNumberOfColumns(20);
		description.setNumberOfRows(0);
		String pythonlibDir = ExtractionHelper.getPythonLibDir();
		folder.setValue(pythonlibDir);
		sitecustomizePath.setValue(pythonlibDir + File.separator + "site-packages" + File.separator + "sitecustomize.py");
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		// Launch the extraction with an empty timestamps object,
		// so that every plugin will be extracted again
		ExtractionHelper.extractPyFiles(new ExtractionTimestamps());
	}

	@Override
	public void clean() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void execute() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void initialize() {
		getUI().setActionPanelVisible(false);

		addEzComponent(description);
		addEzComponent(startButton);
		addEzComponent(folderDescription);
		addEzComponent(folder);
		addEzComponent(customizeDescription);
		addEzComponent(sitecustomizePath);
		addEzComponent(customizeDescription2);
		
		getUI().repack(true);
	}
}
