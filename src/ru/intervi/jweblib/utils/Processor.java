package ru.intervi.jweblib.utils;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.file.StandardOpenOption;
import java.util.zip.GZIPOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Класс для работы с клиентским сокетом.
 */
public class Processor {
	/**
	* 
	* @param socket клиентский сокет
	* @throws NullPointerException
	* @throws IOException
	*/
	public Processor(SocketChannel channel) throws NullPointerException, IOException {
		if (channel == null) throw new NullPointerException("socket is null");
		CHANNEL = channel;
	}
	
	/**
	* клиентский сокет
	*/
	public final SocketChannel CHANNEL;
	/**
	* заголовок от клиента
	*/
	public final HashMap<String, String> HEADER = new HashMap<String, String>();
	/**
	* тип запроса
	*/
	public Type type;
	/**
	* запрашиваемый путь
	*/
	public String path;
	/**
	* версия http
	*/
	public String http;
	/**
	* стандартный код для ответа
	*/
	public static String RESPCODE = "http/1.1 200 OK";
	/**
	 * стандартный MIME тип для страниц: text/html; charset="UTF-8"
	 */
	public static String PLAIN = "text/html; charset=\"UTF-8\"";
	private int pheader = 0, dataLimit = 1024, wpos = 0, wbuf = 0;
	private long wlen = 0;
	private ByteArrayOutputStream data = new ByteArrayOutputStream();
	private FileChannel lfc;
	private boolean fwrite = false, fread = false, reading = false;
	private String charset = Charset.defaultCharset().name();
	
	/**
	 * получить respheader
	 * @param args чётные - ключи, не чётные - значения
	 * @return
	 */
	public static Map<String, String> getRespheader(String ... args) {
		HashMap<String, String> result = new HashMap<String, String>();
		for (int i = 0; i < args.length; i += 2) {
			if (i+1 == args.length) break;
			result.put(args[i], args[i+1]);
		}
		return result;
	}
	
	/**
	* отправка строки в ответ (автоматическая добавка Content-Length в заголовок)
	* @param response содержимое страницы
	* @param gzip true - упаковать содержимое (добавит Content-Encoding: gzip в заголовок)
	* @param mime MIME тип
	* @param respheader заголовок ответа
	* @throws NullPointerException
	* @throws IOException
	*/
	public void writeResponse(String response, boolean gzip, String mime, Map<String, String> respheader) throws NullPointerException, IOException {
		if (response == null) throw new NullPointerException("response is null");
		ByteArrayOutputStream bao = new ByteArrayOutputStream();
		if (gzip) {
			GZIPOutputStream gos = new GZIPOutputStream(bao);
			gos.write(response.getBytes(charset));
			gos.close();
			respheader.put("Content-Encoding", "gzip");
		}
		else bao.write(response.getBytes(charset));
		respheader.put("Content-Length", String.valueOf(bao.size()));
		bao.flush();
		byte b[] = bao.toByteArray();
		bao.reset();
		bao.write(getHeader(respheader, mime, RESPCODE));
		bao.write(b);
		bao.flush();
		CHANNEL.write(ByteBuffer.wrap(bao.toByteArray()));
	}
	
	/**
	* отправка файла в ответ
	* @param response файл
	* @param buffer по скольку байтов читать из файла за 1 итерацию цикла
	* @param longer true - постепенная не блокирующая отправка (по вызовам callWrite), false - целиковая (блокирующая)
	* @param mime MIME тип
	* @param respheader заголовок ответа
	* @throws NullPointerException
	* @throws IOException
	*/
	public void writeResponse(File response, int buffer, boolean longer, String mime, Map<String, String> respheader) throws NullPointerException, FileNotFoundException, IllegalArgumentException, IOException {
		if (response == null) throw new NullPointerException("response is null");
		if (buffer <= 0) throw new IllegalArgumentException("buffer <= 0");
		respheader.put("Content-Length", String.valueOf(response.length()));
		CHANNEL.write(ByteBuffer.wrap(getHeader(respheader, mime, RESPCODE)));
		if (longer) {
			lfc = FileChannel.open(response.toPath(), StandardOpenOption.READ);
			wpos = 0;
			wbuf = buffer;
			wlen = response.length();
			wpos += lfc.transferTo(wpos, wbuf, CHANNEL);
			reading = false;
		} else {
			FileChannel fc = FileChannel.open(response.toPath(), StandardOpenOption.READ);
			long pos = 0;
			while(pos < response.length())
				pos += fc.transferTo(pos, buffer, CHANNEL);
			fc.close();
		}
	}
	
	/**
	 * прочитать ответ в файл
	 * @param save файл для сохранения
	 * @param buffer по скольку байтов читать из канала за 1 итерацию цикла
	 * @param longer true - постепенное не блокирующее чтение (по вызовам callRead), false - целиковое (блокирующее)
	 * @param start начальные данные с HTTP заголовком
	 * @param length размер файла в байтах
	 * @throws IOException
	 */
	public void readResponse(File save, int buffer, boolean longer, byte[] start, long length) throws IOException {
		save.getParentFile().mkdirs();
		if (!save.isFile()) save.createNewFile();
		FileOutputStream fos = new FileOutputStream(save);
		byte d[] = Parser.separateData(start);
		fos.write(d);
		fos.close();
		if (longer) {
			lfc = FileChannel.open(save.toPath(), StandardOpenOption.APPEND);
			wpos = d.length;
			wbuf = buffer;
			wlen = length;
			lfc.position(wpos);
			reading = true;
		} else {
			FileChannel fc = FileChannel.open(save.toPath(), StandardOpenOption.APPEND);
			long pos = d.length;
			fc.position(pos);
			while(pos < length)
				pos += fc.transferFrom(CHANNEL, wpos, buffer);
			fc.close();
		}
	}
	
	/**
	* прочитать все доступные данные от клиента
	* @param allocate размер ByteBuffer
	* @return
	* @throws IOException
	*/
	public byte[] readAll(int allocate) throws IOException {
		ByteArrayOutputStream bao = new ByteArrayOutputStream();
		ByteBuffer buf = ByteBuffer.allocate(allocate);
		while(CHANNEL.read(buf) > 0) {
			buf.flip();
			byte[] b = new byte[buf.limit()];
			buf.get(b);
			bao.write(b);
			buf.clear();
		}
		return bao.toByteArray();
	}
	
	/**
	 * прочитать данные
	 * @param len количество байт (для ByteBuffer)
	 * @return
	 * @throws IOException
	 */
	public byte[] read(int len) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(len);
		CHANNEL.read(buf);
		buf.flip();
		byte[] b = new byte[buf.limit()];
		buf.get(b);
		return b;
	}
	
	/**
	* закрыть соединение
	* @throws IOException
	*/
	public void close() throws IOException {
		CHANNEL.close();
	}
	
	/**
	 * прочитать заголовок и заполнить переменные
	 * @param b полученные данные
	 * @return true в случае успеха
	 * @throws IOException
	 */
	public boolean callParseHeader(byte[] b) throws IOException {
		String request[] = parseHeader(b);
		if (request == null || request.length < 3) return false;
		if (request[0].trim().equals("GET")) type = Type.GET;
		else type = Type.POST;
		path = request[1].trim();
		http = request[2].trim();
		pheader++;
		return true;
	}
	
	/**
	 * получить количество прочтений заголовка
	 * @return
	 */
	public int getCallsParseHeader() {
		return pheader;
	}
	
	/**
	 * проверка наличия заголовка
	 * @param b полученные данные
	 * @return true если заголовок присутствует
	 * @throws UnsupportedEncodingException 
	 */
	public boolean isHeaderReady(byte[] b) throws UnsupportedEncodingException {
		if (Parser.getBreak(new String(b)) != -1) return true;
		return false;
	}
	
	/**
	 * получить размер буфера данных
	 * @return
	 */
	public int getDataSize() {
		return data.size();
	}
	
	/**
	 * прочитать данные из буфера, после чего он будет освобождён для записи новых данных
	 * @return
	 */
	public byte[] readData() {
		byte result[] = data.toByteArray();
		data.reset();
		return result;
	}
	
	/**
	 * получить данные из буфера без освобождения
	 * @return
	 */
	public byte[] getData() {
		return data.toByteArray();
	}
	
	/**
	 * вызывается для чтения данных по событию из Worker или аналогичного обработчика
	 * @throws IOException
	 */
	public void callRead() throws IOException {
		if (reading) {
			if (wpos < wlen) {
				wpos += lfc.transferFrom(CHANNEL, wpos, wbuf);
				return;
			}
			if (lfc != null && lfc.isOpen() && wpos >= wlen) {
				wpos = 0;
				wlen = 0;
				wbuf = 0;
				lfc.close();
				lfc = null;
				reading = false;
			}
			fread = true;
			return;
		}
		int len = dataLimit - data.size();
		if (len > 0) data.write(read(len));
	}
	
	/**
	 * вызывается для записи данных по событию из Worker или аналогичного обработчика
	 * @throws IOException
	 */
	public void callWrite() throws IOException {
		if (wpos < wlen) wpos += lfc.transferTo(wpos, wbuf, CHANNEL);
		if (lfc != null && lfc.isOpen() && wpos >= wlen) {
			wpos = 0;
			wlen = 0;
			wbuf = 0;
			lfc.close();
			lfc = null;
		}
		fwrite = true;
	}
	
	/**
	 * проверить, вызывался ли когда-либо callWrite
	 * @return true если да
	 */
	public boolean isWritten() {
		return fwrite;
	}
	
	/**
	 * проверить, вызывался ли когда-либо callRead для записи в файл
	 * @return true если да
	 */
	public boolean isReaded() {
		return fread;
	}
	
	/**
	 * проверить, ведётся ли отдача файла
	 * @return true если да
	 */
	public boolean isWriting() {
		if (wpos != 0 && !reading) return true;
		return false;
	}
	
	/**
	 * проверить, ведётся ли запись файла
	 * @return true если да
	 */
	public boolean isReading() {
		if (wpos != 0 && reading) return true;
		return false;
	}
	
	/**
	 * получить размер буфера данных, наполняющегося при вызовах callRead
	 * @return
	 */
	public int getDataLimit() {
		return dataLimit;
	}
	
	/**
	 * установить размер буфера данных
	 * @param limit
	 */
	public void setDataLimit(int limit) {
		dataLimit = limit;
	}
	
	/**
	 * получить теущую кодировку
	 * @return
	 */
	public String getCharset() {
		return charset;
	}
	
	/**
	 * установить кодировку
	 * @param ch
	 */
	public void setCharset(String ch) {
		charset = ch;
	}
	
	private String[] parseHeader(byte[] b) throws IOException {
		int br = Parser.getBreak(new String(b));
		if (br == -1) return null;
		byte sh[] = Parser.separateHeader(b);
		if (sh == null) return null;
		String str = new String(sh).trim();
		boolean first = true;
		String result[] = null;
		HEADER.clear();
		for (String s : str.split("\n")) {
			if (s == null || s.isEmpty() || s.indexOf(' ') == -1) continue;
			s = s.trim();
			if (first) {
				result = s.split(" ");
				first = false;
			} else {
				String value = s.substring(s.indexOf(' ') + 1).trim();
				String key = s.split(" ")[0].trim();
				if (key.isEmpty() || value.isEmpty()) continue;
				key = key.substring(0, key.length()-1);
				HEADER.put(key, value);
			}
		}
		return result;
	}
	
	private byte[] getHeader(Map<String, String> respheader, String mime, String respcode) throws IOException {
		ByteArrayOutputStream bao = new ByteArrayOutputStream();
		bao.write((respcode + "\r\n").getBytes(charset));
		for (Entry<String, String> entry : respheader.entrySet()) {
			String resp = entry.getKey() + ": " + entry.getValue() + "\r\n";
			bao.write(resp.getBytes(charset));
		}
		if (mime != null) bao.write(("Content-Type: " + mime + "\r\n").getBytes(charset));
		bao.write("\r\n".getBytes(charset));
		bao.flush();
		return bao.toByteArray();
	}
	
	/**
	 * тип запроса
	 */
	public enum Type {GET, POST}
}
