package ru.intervi.jweblib.utils;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.file.StandardOpenOption;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
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
	//внутренняя магия
	private int pheader = 0, dataLimit = 1024, wpos = 0, wbuf = 0, wlevel = Deflater.BEST_COMPRESSION;
	private long wlen = 0;
	private ByteArrayOutputStream data = new ByteArrayOutputStream();
	private FileChannel lfc;
	private GZIPOutputStream wgos;
	private ByteArrayOutputStream wbao;
	private boolean fwrite = false, wlonger = false;
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
	* @param response содержимое страницы (может быть null)
	* @param gzip true - упаковать содержимое (добавит Content-Encoding: gzip в заголовок)
	* @param mime MIME тип (может быть null)
	* @param respheader заголовок ответа
	* @param code код ответа
	* @throws NullPointerException
	* @throws IOException
	*/
	public void writeResponse(String response, boolean gzip, String mime, Map<String, String> respheader, String code) throws NullPointerException, IOException {
		if (response == null) throw new NullPointerException("response is null");
		ByteArrayOutputStream bao = new ByteArrayOutputStream();
		if (gzip && response != null) {
			GZIPOutputStream gos = new GZIPOutputStream(bao);
			gos.write(response.getBytes(charset));
			gos.close();
			respheader.put("Content-Encoding", "gzip");
		}
		else if (response != null) bao.write(response.getBytes(charset));
		if (response != null) respheader.put("Content-Length", String.valueOf(bao.size()));
		bao.flush();
		byte b[] = bao.toByteArray();
		bao.reset();
		bao.write(getHeader(respheader, mime, code));
		if (response != null) bao.write(b);
		bao.flush();
		CHANNEL.write(ByteBuffer.wrap(bao.toByteArray()));
	}
	
	/**
	* отправка файла в ответ
	* @param response файл
	* @param gzip true - сжимать содержимое
	* @param buffer размер буфера для различных операций
	* @param longer true - постепенная не блокирующая отправка (по вызовам callWrite), false - целиковая (блокирующая)
	* @param mime MIME тип (может быть null)
	* @param respheader заголовок ответа
	* @param code код ответа
	* @throws NullPointerException
	* @throws IOException
	*/
	public void writeResponse(File response, boolean gzip, int buffer, boolean longer, String mime, Map<String, String> respheader, String code) throws NullPointerException, FileNotFoundException, IllegalArgumentException, IOException {
		if (response == null) throw new NullPointerException("response is null");
		if (buffer <= 0) throw new IllegalArgumentException("buffer <= 0");
		if (gzip) {
			respheader.put("Content-Encoding", "gzip");
			respheader.put("Transfer-Encoding", "chunked");
			wbao = new ByteArrayOutputStream();
			wgos = new GZIPOutputStream(wbao, buffer, true) {
				public GZIPOutputStream setLevel(int level) {
					this.def.setLevel(level);
					return this;
				}
			}.setLevel(wlevel);
		}
		else respheader.put("Content-Length", String.valueOf(response.length()));
		respheader.put("Last-Modified", new Date(response.lastModified()).toString());
		CHANNEL.write(ByteBuffer.wrap(getHeader(respheader, mime, code)));
		wlonger = longer;
		wpos = 0;
		wbuf = buffer;
		wlen = response.length();
		lfc = FileChannel.open(response.toPath(), StandardOpenOption.READ);
		while(wpos < wlen) {
			transFile();
			if (longer) return;
		}
		endTransFile();
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
		if (request == null) return false;
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
	 */
	public boolean isHeaderReady(byte[] b) {
		if (Parser.getBreak(b, charset) != -1) return true;
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
		int len = dataLimit - data.size();
		if (len > 0) data.write(read(len));
	}
	
	/**
	 * вызывается для записи данных по событию из Worker или аналогичного обработчика
	 * @throws IOException
	 */
	public void callWrite() throws IOException {
		if (!wlonger) return;
		if (wpos < wlen) transFile();
		if (lfc != null && lfc.isOpen() && wpos >= wlen) endTransFile();
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
	 * проверить, ведётся ли отдача файла
	 * @return true если да
	 */
	public boolean isWriting() {
		if (wpos != 0) return true;
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
	
	/**
	 * получить уровень компрессии
	 * @return
	 */
	public int getGZIPLevel() {
		return wlevel;
	}
	
	/**
	 * установить уровень компрессии
	 * @param level
	 */
	public void setGZIPLevel(int level) {
		wlevel = level;
	}
	
	private void transFile() throws IOException {
		if (wgos != null) {
			ByteBuffer bb = ByteBuffer.allocate((int) (wlen - wpos > wbuf ? wbuf : wlen - wpos));
			wpos += lfc.read(bb);
			wgos.write(bb.array());
			wgos.flush();
			byte[] b = wbao.toByteArray();
			wbao.reset();
			wbao.write((Integer.toHexString(b.length) + "\r\n").getBytes(charset));
			wbao.write(b);
			wbao.write("\r\n".getBytes(charset));
			wbao.flush();
			CHANNEL.write(ByteBuffer.wrap(wbao.toByteArray()));
			wbao.reset();
		} else if (lfc != null) wpos += lfc.transferTo(wpos, wbuf, CHANNEL);
	}
	
	private void endTransFile() throws UnsupportedEncodingException, IOException {
		if (wgos != null) {
			CHANNEL.write(ByteBuffer.wrap("0\r\n\r\n".getBytes(charset)));
			wgos.close();
		}
		wlonger = false;
		wpos = 0;
		wlen = 0;
		wbuf = 0;
		lfc.close();
		lfc = null;
		wgos = null;
		wbao = null;
	}
	
	private String[] parseHeader(byte[] b) throws IOException {
		String str = new String(b);
		int br = Parser.getBreak(str);
		if (br == -1) return null;
		boolean first = true;
		String result[] = null;
		HEADER.clear();
		for (String s : str.substring(0, br).trim().split("\n")) {
			if (s == null || s.isEmpty()) continue;
			s = s.trim();
			if (first) {
				result = s.split(" ");
				first = false;
			} else {
				String value = s.substring(s.indexOf(' ') + 1).trim();
				String key = s.split(" ")[0].trim();
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
