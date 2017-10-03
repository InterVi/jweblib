package ru.intervi.jweblib.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Класс для отправки файлов клиенту.
 */
public class FileSender {
	public FileSender(Processor proc) {
		PROC = proc;
	}
	
	public FileSender(Processor proc, String sendPath) throws NullPointerException, FileNotFoundException, IllegalArgumentException, IOException {
		PROC = proc;
		sendFile(sendPath, true, 1024, true);
	}
	
	public FileSender(Processor proc, File sendFile) throws NullPointerException, FileNotFoundException, IllegalArgumentException, IOException {
		PROC = proc;
		sendFile(sendFile, true, 1024, true);
	}
	
	private FileSender() { //костыль
		PROC = null;
	}
	
	private final Processor PROC;
	
	/**
	 * получить директорию с jar-файлом
	 * @return
	 */
	public static File getPath() {
		return new File(new FileSender().getClass().getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile();
	}
	
	/**
	 * отправить файл клиенту
	 * @param file файл
	 * @param gzip true чтобы использовать сжатие
	 * @param buffer см. {@link ru.intervi.jweblib.utils.Processor.writeResponse(File, boolean, int)}
	 * @param mime true чтобы определять MIME-типы
	 * @return true если файл был отправлен
	 * @throws NullPointerException
	 * @throws FileNotFoundException
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	public boolean sendFile(File file, boolean gzip, int buffer, boolean mime) throws NullPointerException, FileNotFoundException, IllegalArgumentException, IOException {
		if (file.isFile()) {
			if (mime) PROC.mime = Files.probeContentType(file.toPath());
			PROC.writeResponse(file, gzip, buffer);
			return true;
		} else return false;
	}
	
	/**
	 * отправить файл клиенту
	 * @param path путь к файлу
	 * @param gzip true чтобы использовать сжатие
	 * @param buffer см. {@link ru.intervi.jweblib.utils.Processor.writeResponse(File, boolean, int)}
	 * @param mime true чтобы определять MIME-типы
	 * @return true если файл был отправлен
	 * @throws NullPointerException
	 * @throws FileNotFoundException
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	public boolean sendFile(String path, boolean gzip, int buffer, boolean mime) throws NullPointerException, FileNotFoundException, IllegalArgumentException, IOException {
		return sendFile(new File(path), gzip, buffer, mime);
	}
}
