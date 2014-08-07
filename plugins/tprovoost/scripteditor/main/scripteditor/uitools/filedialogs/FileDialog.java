package plugins.tprovoost.scripteditor.uitools.filedialogs;

import icy.file.FileUtil;
import icy.main.Icy;
import icy.system.thread.ThreadUtil;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

public class FileDialog
{

	public static File open()
	{
		return open(null);
	}

	public static File open(String currentDirectory)
	{
		return open(currentDirectory, "", new String[0]);
	}

	public static File open(String currentDirectory, String filterDesc, String... ext)
	{
		FileDialogAWT fcawt = new FileDialogAWT(false, currentDirectory, filterDesc, ext);
		while (!fcawt.isReady())
			ThreadUtil.sleep(100);
		JFileChooser fc = fcawt.fc;
		int res = fcawt.result;

		if (res == JFileChooser.APPROVE_OPTION)
			return fc.getSelectedFile();

		return null;
	}

	public static File openFolder()
	{
		return openFolder("");
	}

	public static File openFolder(String currentDirectory)
	{
		FileDialogAWT fcawt = new FileDialogAWT(false, currentDirectory, "", new String[0]);
		fcawt.setFolderOnly(true);
		while (!fcawt.isReady())
			ThreadUtil.sleep(100);
		JFileChooser fc = fcawt.fc;
		int res = fcawt.result;

		if (res == JFileChooser.APPROVE_OPTION)
			return fc.getSelectedFile();

		return null;
	}

	public static File[] openMulti()
	{
		return openMulti(null);
	}

	public static File[] openMulti(String currentDirectory)
	{
		return openMulti(currentDirectory, "", new String[0]);
	}

	public static File[] openMulti(String currentDirectory, String filterDesc, String... ext)
	{
		FileDialogAWT fcawt = new FileDialogAWT(false, true, currentDirectory, filterDesc, ext);
		while (!fcawt.isReady())
			ThreadUtil.sleep(100);
		JFileChooser fc = fcawt.fc;
		int res = fcawt.result;

		if (res == JFileChooser.APPROVE_OPTION)
			return fc.getSelectedFiles();

		return null;
	}

	public static File save()
	{
		return save(null);
	}

	public static File save(String currentDirectory)
	{
		return save(currentDirectory, "", new String[0]);
	}

	public static File save(final String currentDirectory, final String filterDesc, final String... ext)
	{
		FileDialogAWT fcawt = new FileDialogAWT(true, currentDirectory, filterDesc, ext);
		while (!fcawt.isReady())
			ThreadUtil.sleep(100);
		JFileChooser fc = fcawt.fc;
		int res = fcawt.result;

		if (res == JFileChooser.APPROVE_OPTION)
		{
			String s = fc.getSelectedFile().getAbsolutePath();
			if (FileUtil.getFileExtension(s, false).isEmpty() && ext != null && ext.length > 0)
			{
				new File(s + "." + ext[0]);
			} else
			{
				return fc.getSelectedFile();
			}
		}
		return null;
	}

	public static File saveFolder()
	{
		return saveFolder(null);
	}

	public static File saveFolder(String currentDirectory)
	{
		FileDialogAWT fcawt = new FileDialogAWT(true, currentDirectory, "", new String[0]);
		fcawt.setFolderOnly(true);
		while (!fcawt.isReady())
			ThreadUtil.sleep(100);
		JFileChooser fc = fcawt.fc;
		int res = fcawt.result;

		if (res == JFileChooser.APPROVE_OPTION)
			return fc.getSelectedFile();
		return null;
	}

	private static class FileDialogAWT
	{
		private JFileChooser fc;
		private int result;
		private boolean ready;
		private boolean folderOnly = false;

		public FileDialogAWT(final boolean save, final String currentDirectory, final String filterDesc, final String... ext)
		{
			this(save, false, currentDirectory, filterDesc, ext);
		}

		public void setFolderOnly(boolean folderOnly)
		{
			this.folderOnly = folderOnly;
		}

		public FileDialogAWT(final boolean save, final boolean multi, final String currentDirectory, final String filterDesc, final String[] ext)
		{
			setReady(false);
			ThreadUtil.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					fc = new JFileChooser(currentDirectory);
					if (folderOnly)
					{
						fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					} else
					{
						if (ext != null && ext.length > 0)
							fc.setFileFilter(new FileNameExtensionFilter(filterDesc, ext));
						fc.setMultiSelectionEnabled(multi);
					}
					if (save)
						result = fc.showSaveDialog(Icy.getMainInterface().getMainFrame());
					else
						result = fc.showOpenDialog(Icy.getMainInterface().getMainFrame());
					setReady(true);
				}
			});
		}

		/**
		 * @return the ready
		 */
		private boolean isReady()
		{
			return ready;
		}

		/**
		 * @param ready
		 *            the ready to set
		 */
		private void setReady(boolean ready)
		{
			this.ready = ready;
		}

	}

}
