package plugins.tlecomte.pythonExtractor;

import icy.file.FileUtil;
import icy.plugin.PluginDescriptor;
import icy.plugin.PluginLoader;
import icy.plugin.abstract_.Plugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.python.core.PySystemState;

class ExtractionHelper {
	final static String pythonDirName = "PythonLibs";
	final static String extractionDir = pythonDirName;

    /**
     * Walk the plugin list to look for plugins that contain py files
	 * and extract them.
	 * 
     * @return the list of dirs whose contents have been extracted
     */
	static void extractPyFiles(ExtractionTimestamps timestamps) {
		ArrayList<PluginDescriptor> pluginList = PluginLoader.getPlugins();
		
		String destDir = getPythonLibDir();
		if (destDir == null) {
			System.err.println("Failed to extract python library files because sys.prefix is not set");
			return;
		}
		
		// extract Python files
		for (PluginDescriptor plugin:pluginList) {
			if (plugin.getPluginClass().getAnnotation(PythonLibraries.class) != null) {
				try {
					extractPyFiles(plugin, destDir, timestamps);
				} catch (IOException e) {
					System.err.println("Failed to extract python library files for plugin " + plugin);
					e.printStackTrace();
				} catch (URISyntaxException e) {
					System.err.println("Failed to extract python library files for plugin " + plugin);
					e.printStackTrace();
				}
			}
		}
	}
	
	static String getPythonLibDir() {
		// Note: this is done in a thread, otherwise ThreadLocal variables in Jython
		// prevents the whole classloader from being garbage-collected. 
	    ExecutorService executor = Executors.newSingleThreadExecutor();
	    Callable<String> callable = new Callable<String>() {
	        @Override
	        public String call() {
	        	// create a new PySystemState (will initialize Jython state if not done already)
	    		PySystemState sys = new PySystemState();
	    		// retrieve Python prefix (i.e. where the 'Lib' folder will be looked for)
	            String sys_prefix = PySystemState.prefix.asString();
	            // cleanup immediately to remove shutdown hooks that prevent the classloader from being garbage-collected
	            sys.cleanup();
	            return sys_prefix;
	        }
	    };
	    Future<String> future = executor.submit(callable);
	    executor.shutdown();
	    executor = null;
	        
	    String sys_prefix;
		try {
			sys_prefix = future.get();
		} catch (InterruptedException e) {
			System.err.println("Failed to retrieve Python prefix");
			sys_prefix = null;
		} catch (ExecutionException e) {
			System.err.println("Failed to retrieve Python prefix");
			sys_prefix = null;
		}
		
		String Lib_prefix = null;
		if (sys_prefix == null) {
			System.err.println("Error: sys.prefix is None.");
			System.err.println("If you are developing in Eclipse, please set the 'python.home' property");
			System.err.println("on the Java command-line. For example:");
			System.err.println("	-Dpython.home=plugins");				
		} else {
			Lib_prefix = sys_prefix + File.separator + "Lib";
		}

		return Lib_prefix;
	}
	
	static void extractPyFiles(PluginDescriptor plugin, String destDir, ExtractionTimestamps timestamps) throws IOException, URISyntaxException {
		
		// Jython can use python files that are actually on disk
		// or files that are inside a jar.
		// However, files inside a jar will not be found by modules
		// like 'inspect' that are actively working on the source
		// code. This is preventing the interpreter from displaying
		// complete backtraces whenever they go through a file in a jar.
		// Additionally, it prevents execnet from correctly bootstrapping
		// itself.
		
		// To overcome this, we choose to make sure that all python files
		// provided by Icy are extracted to an actual folder on disk.
		
		// This will work on files provided by all plugins implementing PythonLibrariesBundle
		// JythonExtrasForIcy and JythonExecnetForIcy are first users of this interface.

		Class<? extends Plugin> klass = plugin.getPluginClass();
			
		URL classUrl = klass.getResource(klass.getSimpleName() + ".class");

		URLConnection connection = classUrl.openConnection();
		
		if (connection instanceof JarURLConnection)
		{
			JarURLConnection jarConnection = (JarURLConnection) connection;
			
			extractPyFilesFromJarIfNewer(jarConnection, plugin, destDir, timestamps);
		}
		else
		{
			// the files are already on disk !
			// (this happens when developing the plugins in Eclipse)

			System.out.println("Extracting Python files from disk for plugin " + plugin);
			String srcDir = findPythonFilesFromDisk(classUrl);
			FileUtil.copy(srcDir, destDir, true, true); // force and recursive
		}
	}
	
	static void extractPyFilesFromJarIfNewer(JarURLConnection jarConnection, PluginDescriptor plugin, String destDir, ExtractionTimestamps timestamps) throws IOException, URISyntaxException {
		URL jarUrl = jarConnection.getJarFileURL();

		URI jarURI = jarUrl.toURI();
		
		JarFile jarFile = new JarFile(new File(jarURI));
			
		long jarLastModified = new File(jarFile.getName()).lastModified();

		// retrieve the lastModified info of the last time we extracted
		Long extractionTimestamp = timestamps.getTimestamp(plugin);

		if (extractionTimestamp.longValue() == jarLastModified) {
			// no need to extract anything, the proper version exists
			return;
		}

		// the bundled version is newer, extract !
		System.out.println("Extracting Python files from plugin " + plugin);
		extractPyFilesFromJar(jarFile, destDir);

		// store the extracted version
		timestamps.setTimestamp(plugin, new Long(jarLastModified));
	}

	static void extractPyFilesFromJar(JarFile jarFile, String destDir) throws IOException {
		Enumeration<JarEntry> entries = jarFile.entries();
		while (entries.hasMoreElements()) {
			JarEntry jarEntry = entries.nextElement();
	
			String name = jarEntry.getName();
			
			// extract all files contained in a dir named $pythonDirName
			if (!jarEntry.isDirectory() && name.startsWith(pythonDirName)) {		
				String strippedName = name.substring(pythonDirName.length() + 1);
				String outName = destDir + File.separator + strippedName;
		
				InputStream inputStream = jarFile.getInputStream(jarEntry);
				File destFile = new File(outName);
				FileOutputStream fileOutputStream;
				try {
					fileOutputStream = new FileOutputStream(destFile);
				}
				catch (FileNotFoundException e) {
					destFile.getParentFile().mkdirs();
					fileOutputStream = new FileOutputStream(destFile);
				} 
				byte[] b = new byte[16384];
				int bytes;
				while ((bytes = inputStream.read(b)) > 0) {
					fileOutputStream.write(b, 0, bytes);
				}
				fileOutputStream.close();
				inputStream.close();
			}
		}
	}
		
	
	
	/**
	 * Given a directory, walk the sub-directories until it finds
	 * one that is named "PythonLibs"
	 * If no directory with that name is found, reset the starting
	 * directory with its parent and start again
	 * 
	 * @param connection
	 * @return the "PythonLibs" directory as a File object, null if none is found
	 * @throws URISyntaxException
	 */
	static String findPythonFilesFromDisk(URL classUrl) throws URISyntaxException {
		// the URL needs to be converted to an URI to handle spaces in paths
		File classFile = new File(classUrl.toURI());
		
		File pythonParent = null;
		File leaf = classFile;
		
		while (pythonParent == null) {
			leaf = leaf.getParentFile();
			pythonParent = findPythonFilesInSubDirs(leaf);
		}
		
		return pythonParent.getAbsolutePath();
	}
	
	/**
	 * Given a root directory, walk the sub-directories until it finds
	 * one that is named "PythonLibs"
	 * 
	 * @param connection
	 * @return the "PythonLibs" directory as a File object, null if none is found
	 */
	static File findPythonFilesInSubDirs(File root) {
		if (!root.isDirectory()) {
			return null;
		}
		
		for (File file : root.listFiles()) {
			if (file.isDirectory()) {
				if (file.getName().endsWith(pythonDirName)) {
					return file;
				} else {
					File res = findPythonFilesInSubDirs(file); // recursive !
					if (res != null) {
						return res;
					}
				}
			}
		}
		
		return null;
	}
}
