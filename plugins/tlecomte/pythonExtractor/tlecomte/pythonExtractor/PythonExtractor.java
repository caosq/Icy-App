package plugins.tlecomte.pythonExtractor;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.python.core.Py;
import org.python.core.PySystemState;

import icy.file.xml.XMLPersistentHelper;
import icy.plugin.abstract_.Plugin;
import icy.plugin.interface_.PluginDaemon;
import icy.util.ClassUtil;
import icy.util.XMLUtil;

public class PythonExtractor extends Plugin implements PluginDaemon {

	final String TIMESTAMPS_FILENAME = "ExtractDescriptor";
	
	@Override
	public void init() {
		// load timestamps of last extraction from XML file
		ExtractionTimestamps timestamps = new ExtractionTimestamps();
		
		String qualifiedDir = new File(ClassUtil.getPathFromQualifiedName(getDescriptor().getClassName())).getParent();
		String timestampsFile   = qualifiedDir + File.separator + TIMESTAMPS_FILENAME + XMLUtil.FILE_DOT_EXTENSION;
		boolean loadSuccess = XMLPersistentHelper.loadFromXML(timestamps, timestampsFile);
		
		if (!loadSuccess) {
			File file = new File(timestampsFile);
			// if the file does not exist, the failure is normal
			if (file.exists()) {
				System.err.println("Failed to load the Python extraction timestamp file from " + timestampsFile);
			}
		}
		
		// do the hard work here
		ExtractionHelper.extractPyFiles(timestamps);
		
		// save timestamps
		boolean saveSucess = XMLPersistentHelper.saveToXML(timestamps, timestampsFile);
		
		if (!saveSucess) {
			System.err.println("Failed to save the Python extraction timestamp file to " + timestampsFile);
		}
	}

	@Override
	public void run() {

	}

	@Override
	public void stop() {
		// This is our last chance of cleanup before a classloader reload
		// If we do not ask for a cleanup, the registered shutdown hooks will
		// prevent the whole classloader from being garbage collected,
		// utlimately leading to a PermGenSpace error.
		
		// Note that we need to do that in a thread to prevent ThreadLocal variables from
		// being kept forever and preventing the classloader from being collected.
        
	    ExecutorService executor = Executors.newSingleThreadExecutor();
	    Runnable runnable = new Runnable() {
	        @Override
	        public void run() {
	        	// Cleanup to remove shutdown hooks that prevent the classloader from being garbage-collected.
	        	// Note: instantiating a new PySystemState will cleanup those that have been marked as garbage previously
	        	// and this new one must be cleaned up too immediately.
	    		new PySystemState().cleanup();
	    		
	    		// the defaultSystemState is defined static, so it is never marked as garbage.
	    		// We must clean it up manually.
	    		Py.defaultSystemState.cleanup();
	        }
	    };
	    Future<?> future = executor.submit(runnable);
	    executor.shutdown();
	    try {
			future.get();
		} catch (InterruptedException e) {
			// ignore
		} catch (ExecutionException e) {
			// ignore
		}
	}

}
