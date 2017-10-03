package ru.intervi.jweblib.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ru.intervi.jweblib.utils.FileObject.VolumeType;

/**
 * сканер директорий
 */
public class DirScanner {
	public DirScanner(File dir) throws NullPointerException, IOException {
		this(dir, VolumeType.MB, false, "dd-MM-yyyy HH:mm:ss");
	}
	
	public DirScanner(File dir, VolumeType type) throws NullPointerException, IOException {
		this(dir, type, false, "dd-MM-yyyy HH:mm:ss");
	}
	
	public DirScanner(File dir, boolean showHidden) throws NullPointerException, IOException {
		this(dir, VolumeType.MB, false, "dd-MM-yyyy HH:mm:ss");
	}
	
	public DirScanner(File dir, VolumeType type, boolean showHidden) throws NullPointerException, IOException {
		this(dir, type, showHidden, "dd-MM-yyyy HH:mm:ss");
	}
	
	public DirScanner(File dir, VolumeType type, boolean showHidden, String dateFormat) throws NullPointerException, IOException {
		if (dir == null) throw new NullPointerException("dir is null");
		if (type == null) throw new NullPointerException("type is null");
		if (!dir.isDirectory()) throw new FileNotFoundException("dir not found");
		DIR = dir;
		TYPE = type;
		SHOWHIDDEN = showHidden;
		FORMAT = dateFormat;
		parse();
	}
	
	private final File DIR;
	private final boolean SHOWHIDDEN;
	private final VolumeType TYPE;
	private final String FORMAT;
	private final ArrayList<FileObject> files = new ArrayList<FileObject>();
	private final ArrayList<FileObject> dirs = new ArrayList<FileObject>();
	
	private void parse() throws NullPointerException, IOException {
		for (File file : DIR.listFiles()) {
			if (file.isFile()) {
				if (SHOWHIDDEN || !file.isHidden()) files.add(new FileObject(file, TYPE, FORMAT));
			} else if (file.isDirectory()) {
				if (SHOWHIDDEN || !file.isHidden()) dirs.add(new FileObject(file, TYPE, FORMAT));
			}
		}
	}
	
	/**
	 * получить все файлы
	 * @return файлы в директории
	 */
	public List<FileObject> getFiles() {
		return files;
	}
	
	/**
	 * получить все директории
	 * @return директории в директории
	 */
	public List<FileObject> getDirs() {
		return dirs;
	}
}
