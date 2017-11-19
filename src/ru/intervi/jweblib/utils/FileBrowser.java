package ru.intervi.jweblib.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

/**
 * Простейший файловый менеджер, позволяющий просматривать директории и скачивать файлы.
 */
public class FileBrowser {
	/**
	 * 
	 * @param proc
	 * @param respheader
	 * @param path корневая директория
	 * @param url запрашиваемый адрес, может быть null (возьмётся из Processor.path)
	 * @param send отправляемый файл, может быть null (возьмётся из FileBrowser.getPath)
	 * @throws NullPointerException
	 * @throws IOException
	 */
	public FileBrowser(Processor proc, Map<String, String> respheader, File path, String url, File send) throws NullPointerException, IOException {
		PROC = proc;
		RESP = respheader;
		URL = url != null ? url : PROC.path;
		PATH = send != null ? send : getPath(path, URL);
	}
	
	private final Map<String, String> RESP;
	public final Processor PROC;
	/**
	 * запрашиваемый файл или директория
	 */
	public final File PATH;
	/**
	 * адрес из GET запроса
	 */
	public final String URL;
	/**
	 * основной шаблон
	 */
	public String template = "<html><head><title>%s</title><meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" /><style>%s</style></head><body><div id=\"header\"><h1>%s</h1><hr/><a href=\"/\">...</a><hr/></div><div id=\"dirs\">%s</div><div id=\"content\">%s</div></body></html>";
	/**
	 * CSS
	 */
	public String style = "body{width:80%;text-align:center;margin:0 auto;}hr{margin:0px auto;padding:0px;width:60%;}p{margin:3px auto;padding:0px;}.item_name,.dir_name{font-size:14px;}.item_description{font-size:12px;}";
	/**
	 * шаблон элементов (файлов)
	 */
	public String item = "<p><a href=\"%s\"><b class=\"item_name\">%s</b></a><br/><i class=\"item_description\">Size: %s, Modifed: %s</i><hr/></p>";
	/**
	 * шаблон директорий
	 */
	public String dirItem = "<p><a href=\"%s\"><b class=\"dir_name\">%s</b></a><br/><hr/></p>";
	
	/**
	 * получить путь к запрашиваемой директории или файлу
	 * @param path корневой каталог
	 * @param url запрашиваемый адрес
	 * @return
	 */
	public static File getPath(File path, String url) {
		byte c = 0;
		if (url.indexOf("://") != -1) {
			for (int i = 0; i < url.length(); i++) {
				if (url.charAt(i) == '/') c++;
				if (c == 3) {
					return new File(path, url.substring(i+1).replace('/', File.separatorChar));
				}
			}
		} else return new File(path, url.substring(1).replace('/', File.separatorChar));
		return path;
	}
	
	/**
	 * отдача содержимого
	 * @param longer true - постепенная, не блокирующая отдача файлов
	 * @param gzip true - сжимать содержимое
	 * @param buffer размер буфера для различных операций
	 * @return
	 * @throws NullPointerException
	 * @throws FileNotFoundException
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	public FileSender run(boolean longer, boolean gzip, int buffer) throws NullPointerException, FileNotFoundException, IllegalArgumentException, IOException {
		if (PROC.type == Processor.Type.POST || !PATH.exists()) {
			PROC.close();
			return null;
		}
		if (PATH.isFile()) {
			FileSender fs = new FileSender(PROC, PATH, RESP, buffer, Files.probeContentType(PATH.toPath()), longer, gzip);
			return fs;
		}
		FileObject dir = new FileObject(PATH);
		String args[] = new String[5];
		args[0] = dir.NAME;
		args[1] = style;
		args[2] = dir.NAME;
		DirScanner ds = new DirScanner(PATH);
		String dirs = "";
		String files = "";
		for (FileObject fo : ds.getDirs()) dirs += String.format(dirItem, URL.charAt(URL.length()-1) == '/' ? URL + fo.NAME : URL + '/' + fo.NAME, fo.NAME);
		for (FileObject fo : ds.getFiles()) {
			String p = "MB";
			double s = Math.rint(100.0 * fo.SIZE) / 100.0;
			if (s > 1000) {
				s = Math.rint(100.0 * FileObject.getVolume(fo.FILE.length(), FileObject.VolumeType.GB)) / 100.0;
				p = "GB";
			}
			if (s < 1) {
				s = Math.rint(100.0 * FileObject.getVolume(fo.FILE.length(), FileObject.VolumeType.KB)) / 100.0;
				p = "KB";
			}
			if (s < 1) {
				s = fo.FILE.length();
				p = "B";
			}
			files += String.format(item, URL.charAt(URL.length()-1) == '/' ? URL + fo.NAME : URL + '/' + fo.NAME, fo.NAME, String.valueOf(s) + p, fo.MODIFED);
		}
		args[3] = dirs;
		args[4] = files;
		PROC.writeResponse(String.format(template, (Object[]) args), true, Processor.PLAIN, RESP, Processor.RESPCODE);
		PROC.close();
		return null;
	}
}
