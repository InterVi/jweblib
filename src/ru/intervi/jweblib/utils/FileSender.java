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
	
	public FileSender(Processor proc, String sendPath, boolean longer) throws NullPointerException, FileNotFoundException, IllegalArgumentException, IOException {
		PROC = proc;
		sendFile(sendPath, 1024, true, longer);
	}
	
	public FileSender(Processor proc, File sendFile, boolean longer) throws NullPointerException, FileNotFoundException, IllegalArgumentException, IOException {
		PROC = proc;
		sendFile(sendFile, 1024, true, longer);
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
	 * @param buffer см. {@link ru.intervi.jweblib.utils.Processor.writeResponse(File, boolean, int)}
	 * @param mime true чтобы определять MIME-типы
	 * @param longer true - не блокирующая постепенная отправка
	 * @return true если файл не был найден
	 * @throws NullPointerException
	 * @throws FileNotFoundException
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	public boolean sendFile(File file, int buffer, boolean mime, boolean longer) throws NullPointerException, FileNotFoundException, IllegalArgumentException, IOException {
		if (file.isFile()) {
			PROC.writeResponse(file, buffer, longer, Files.probeContentType(file.toPath()), Processor.getRespheader());
			return true;
		} else return false;
	}
	
	/**
	 * отправить файл клиенту
	 * @param path путь к файлу
	 * @param buffer см. {@link ru.intervi.jweblib.utils.Processor.writeResponse(File, boolean, int)}
	 * @param mime true чтобы определять MIME-типы
	 * @param longer true - не блокирующая постепенная отправка
	 * @return true если файл был отправлен
	 * @throws NullPointerException
	 * @throws FileNotFoundException
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	public boolean sendFile(String path, int buffer, boolean mime, boolean longer) throws NullPointerException, FileNotFoundException, IllegalArgumentException, IOException {
		return sendFile(new File(path), buffer, mime, longer);
	}
	
	/**
	 * должен вызываться по событию из Worker или аналогичного обработчика (для постепенной, не блокирующей отдачи файла)
	 * @throws IOException
	 */
	public void callWrite() throws IOException {
		PROC.callWrite();
	}
}
