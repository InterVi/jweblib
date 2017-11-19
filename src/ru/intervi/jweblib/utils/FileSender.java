package ru.intervi.jweblib.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

/**
 * Класс для отправки файлов клиенту.
 */
public class FileSender {
	public FileSender(Processor proc) {
		PROC = proc;
	}
	
	public FileSender(Processor proc, String sendPath, Map<String, String> respheader, int buffer, String mime, boolean longer, boolean gzip) throws NullPointerException, FileNotFoundException, IllegalArgumentException, IOException {
		PROC = proc;
		sendFile(sendPath, respheader, gzip, buffer, mime, longer);
	}
	
	public FileSender(Processor proc, File sendFile, Map<String, String> respheader, int buffer, String mime, boolean longer, boolean gzip) throws NullPointerException, FileNotFoundException, IllegalArgumentException, IOException {
		PROC = proc;
		sendFile(sendFile, respheader, gzip, buffer, mime, longer);
	}
	
	private FileSender() { //костыль
		PROC = null;
	}
	
	public final Processor PROC;
	
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
	 * @param respheader заголовки
	 * @param gzip true - сжимать содержимое
	 * @param buffer см. {@link ru.intervi.jweblib.utils.Processor.writeResponse(File, boolean, int)}
	 * @param mime MIME-тип
	 * @param longer true - не блокирующая постепенная отправка
	 * @return true если файл не был найден
	 * @throws NullPointerException
	 * @throws FileNotFoundException
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	public boolean sendFile(File file, Map<String, String> respheader, boolean gzip, int buffer, String mime, boolean longer) throws NullPointerException, FileNotFoundException, IllegalArgumentException, IOException {
		if (file.isFile()) {
			PROC.writeResponse(file, gzip, buffer, longer, mime, respheader, Processor.RESPCODE);
			return true;
		} else return false;
	}
	
	/**
	 * отправить файл клиенту
	 * @param path путь к файлу
	 * @param respheader заголовки
	 * @param gzip true - сжимать содержимое
	 * @param buffer см. {@link ru.intervi.jweblib.utils.Processor.writeResponse(File, boolean, int)}
	 * @param mime MIME-тип
	 * @param longer true - не блокирующая постепенная отправка
	 * @return true если файл был отправлен
	 * @throws NullPointerException
	 * @throws FileNotFoundException
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	public boolean sendFile(String path, Map<String, String> respheader, boolean gzip, int buffer, String mime, boolean longer) throws NullPointerException, FileNotFoundException, IllegalArgumentException, IOException {
		return sendFile(new File(path), respheader, gzip, buffer, mime, longer);
	}
	
	/**
	 * должен вызываться по событию из Worker или аналогичного обработчика (для постепенной, не блокирующей отдачи файла)
	 * @throws IOException
	 */
	public void callWrite() throws IOException {
		PROC.callWrite();
	}
}
