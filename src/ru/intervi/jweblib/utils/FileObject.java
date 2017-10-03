package ru.intervi.jweblib.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * информация о файле
 */
public class FileObject {
	/**
	 * тип по-умолчанию: MB, формат даты: dd-MM-yyyy HH:mm:ss
	 * @param file
	 * @throws NullPointerException
	 * @throws IOException 
	 */
	public FileObject(File file) throws NullPointerException, IOException {
		this(file, VolumeType.MB, "dd-MM-yyyy HH:mm:ss");
	}
	
	public FileObject(File file, VolumeType type, String dateFormat) throws NullPointerException, IOException {
		if (file == null) throw new NullPointerException("file is null");
		if (type == null) throw new NullPointerException("type is null");
		if (dateFormat == null) throw new NullPointerException("dateFormat is null");
		if (!file.exists()) throw new FileNotFoundException("file not exists");
		READ = file.canRead();
		WRITE = file.canWrite();
		EXECUTE = file.canExecute();
		HIDDEN = file.isHidden();
		ISFILE = file.isFile();
		ISDIR = file.isDirectory();
		NAME = file.getName();
		FREE = getVolume(file.getFreeSpace(), type);
		TOTAL = getVolume(file.getTotalSpace(), type);
		USED = getVolume(file.getUsableSpace(), type);
		SIZE = getVolume(file.length(), type);
		MODIFED = getDate(file.lastModified(), dateFormat);
		MIME = Files.probeContentType(file.toPath());
		FILE = file;
	}
	
	public final boolean READ, WRITE, EXECUTE, HIDDEN, ISFILE, ISDIR;
	public final String NAME, MODIFED, MIME;
	public final double FREE, TOTAL, USED, SIZE;
	public final File FILE;
	
	/**
	 * еденицы измерения объёма данных
	 */
	public static enum VolumeType {
		KiB, MiB, GiB, TiB, PiB, EiB, ZiB, YiB,
		KB, MB, GB, TB, PB, EB, ZB, YB;
	}
	
	/**
	 * получить объём файла в выбранной единице
	 * @param bytes объём в байтах
	 * @param type еденица измерения
	 * @return
	 * @throws NullPointerException
	 */
	public static double getVolume(long bytes, VolumeType type) throws NullPointerException {
		if (type == null) throw new NullPointerException("type is null");
		switch(type) {
		case KiB:
			return bytes/Math.pow(2, 10);
		case MiB:
			return bytes/Math.pow(2, 20);
		case GiB:
			return bytes/Math.pow(2, 30);
		case TiB:
			return bytes/Math.pow(2, 40);
		case PiB:
			return bytes/Math.pow(2, 50);
		case EiB:
			return bytes/Math.pow(2, 60);
		case ZiB:
			return bytes/Math.pow(2, 70);
		case YiB:
			return bytes/Math.pow(2, 80);
		case KB:
			return bytes/Math.pow(10, 3);
		case MB:
			return bytes/Math.pow(10, 6);
		case GB:
			return bytes/Math.pow(10, 9);
		case TB:
			return bytes/Math.pow(10, 12);
		case PB:
			return bytes/Math.pow(10, 15);
		case EB:
			return bytes/Math.pow(10, 18);
		case ZB:
			return bytes/Math.pow(10, 21);
		case YB:
			return bytes/Math.pow(10, 24);
		default:
			return bytes;
		}
	}
	
	/**
	 * получить дату в строковом (человеческом) представлении
	 * @param millis время
	 * @param format формат (см. SimpleDateFormat)
	 * @return
	 * @throws NullPointerException
	 */
	public static String getDate(long millis, String format) throws NullPointerException {
		if (format == null) throw new NullPointerException("format is null");
		SimpleDateFormat sdf = new SimpleDateFormat(format);
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(millis);
        return sdf.format(cal.getTime());
	}
}
