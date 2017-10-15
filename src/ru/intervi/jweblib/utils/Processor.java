package ru.intervi.jweblib.utils;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
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
import java.io.IOException;

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
	private boolean fwrite = false;
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
			gos.write(response.getBytes());
			gos.close();
			respheader.put(Charset.forName(charset).encode("Content-Encoding").toString(), Charset.forName(charset).encode("gzip").toString());
		}
		else bao.write(response.getBytes());
		respheader.put(Charset.forName(charset).encode("Content-Length").toString(), String.valueOf(bao.size()));
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
		respheader.put(Charset.forName(charset).encode("Content-Length").toString(), String.valueOf(response.length()));
		CHANNEL.write(ByteBuffer.wrap(getHeader(respheader, mime, RESPCODE)));
		if (longer) {
			lfc = FileChannel.open(response.toPath(), StandardOpenOption.READ);
			wpos = 0;
			wbuf = buffer;
			wlen = response.length();
			wpos += lfc.transferTo(wpos, wbuf, CHANNEL);
		} else {
			FileChannel fc = FileChannel.open(response.toPath(), StandardOpenOption.READ);
			long pos = 0;
			while(pos < response.length())
				pos += fc.transferTo(pos, buffer, CHANNEL);
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
		if (request == null) return false;
		if (request[0].trim().equals(Charset.forName(charset).encode("GET").toString())) type = Type.GET;
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
	
	private String[] parseHeader(byte[] b) throws IOException {
		String str = new String(b);
		int br = Parser.getBreak(str);
		if (br == -1) return null;
		boolean first = true;
		String result[] = null;
		HEADER.clear();
		String p = Charset.forName(charset).encode(" ").toString();
		for (String s : str.substring(0, br).trim().split(Charset.forName(charset).encode("\n").toString())) {
			if (s == null || s.isEmpty()) continue;
			s = s.trim();
			if (first) {
				result = s.split(p);
				first = false;
			} else {
				String value = s.substring(s.indexOf(' ') + 1).trim();
				String key = s.split(p)[0].trim();
				key = key.substring(0, key.length()-1);
				HEADER.put(key, value);
			}
		}
		return result;
	}
	
	private byte[] getHeader(Map<String, String> respheader, String mime, String respcode) throws IOException {
		ByteArrayOutputStream bao = new ByteArrayOutputStream();
		byte rn[] = Charset.forName(charset).encode(CharBuffer.wrap(new char[] {'\r', '\n'})).array();
		bao.write((respcode + rn).getBytes());
		for (Entry<String, String> entry : respheader.entrySet()) {
			String resp = entry.getKey() + Charset.forName(charset).encode(": ").toString() + entry.getValue() + rn;
			bao.write(resp.getBytes());
		}
		if (mime != null) bao.write((Charset.forName(charset).encode("Content-Type: ").toString() + mime + rn).getBytes());
		bao.write(rn);
		bao.flush();
		return bao.toByteArray();
	}
	
	/**
	 * тип запроса
	 */
	public enum Type {GET, POST}
}
