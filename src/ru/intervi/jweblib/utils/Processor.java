package ru.intervi.jweblib.utils;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import java.util.HashMap;
import java.util.Map.Entry;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
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
	public Processor(Socket socket) throws NullPointerException, IOException {
		if (socket == null) throw new NullPointerException("socket is null");
		SOCKET = socket;
		IS = socket.getInputStream();
		OS = socket.getOutputStream();
		String request[] = parseHeader();
		if (request[0].trim().equals("GET")) TYPE = Type.GET;
		else TYPE = Type.POST;
		PATH = request[1].trim();
		HTTP = request[2].trim();
	}
	
	/**
	 * клиентский сокет
	 */
	public final Socket SOCKET;
	/**
	 * входящий поток данных
	 */
	public final InputStream IS;
	/**
	 * исходящий поток данных
	 */
    public final OutputStream OS;
    /**
     * заголовок от клиента
     */
    public final HashMap<String, String> HEADER = new HashMap<String, String>();
    /**
     * тип запроса
     */
    public final Type TYPE;
    /**
     * запрашиваемый путь
     */
    public final String PATH;
    /**
     * версия HTTP
     */
    public final String HTTP;
    /**
     * заголовок для ответа клиенту
     */
    public HashMap<String, String> respheader = new HashMap<String, String>();
    /**
     * код для ответа
     */
    public String respcode = "HTTP/1.1 200 OK";
    /**
     * MIME тип
     */
    public String mime = "text/html; charset=\"UTF-8\"";
    
    /**
     * отправка строки в ответ (автоматическая добавка Content-Length в заголовок)
     * @param response содержимое страницы
     * @param gzip true - упаковать содержимое (добавит Content-Encoding: gzip в заголовок)
     * @throws NullPointerException
     * @throws IOException
     */
    public void writeResponse(String response, boolean gzip) throws NullPointerException, IOException {
    	if (response == null) throw new NullPointerException("response is null");
    	byte b[] = null;
    	if (gzip) {
    		ByteArrayOutputStream bao = new ByteArrayOutputStream();
    		GZIPOutputStream gos = new GZIPOutputStream(bao);
    		gos.write(response.getBytes());
    		gos.close();
    		b = bao.toByteArray();
    		respheader.put("Content-Encoding", "gzip");
    	}
    	else b = response.getBytes();
    	respheader.put("Content-Length", String.valueOf(b.length));
    	writeHeader();
    	OS.write(b);
	}
    
    /**
     * отправка файла в ответ
     * @param response файл
     * @param gzip true - упаковать файл (добавит Content-Encoding: gzip в заголовок)
     * @param buffer по скольку байтов читать из файла за 1 итерацию цикла
     * @throws NullPointerException
     * @throws IOException
     */
    public void writeResponse(File response, boolean gzip, int buffer) throws NullPointerException, FileNotFoundException, IllegalArgumentException, IOException {
    	if (response == null) throw new NullPointerException("response is null");
    	if (buffer <= 0) throw new IllegalArgumentException("buffer <= 0");
    	if (gzip) respheader.put("Content-Encoding", "gzip");
    	else respheader.put("Content-Length", String.valueOf(response.length()));
    	writeHeader();
    	byte b[] = new byte[buffer];
    	FileInputStream fis = new FileInputStream(response);
    	if (gzip) {
    		GZIPOutputStream gos = new GZIPOutputStream(OS);
    		while(fis.available() > 0) {
    			fis.read(b);
    			gos.write(b);
    		}
    		gos.finish();
    	}
    	else {
    		while(fis.available() > 0) {
    			fis.read(b);
    			OS.write(b);
    		}
    	}
    	fis.close();
    }
	
    /**
     * прочитать данные от клиента
     * @return
     * @throws IOException
     */
	public List<String> read() throws IOException {
		ArrayList<String> result = new ArrayList<String>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(IS));
		while(reader.ready()) result.add(reader.readLine());
		return result;
	}
	
	/**
	 * закрыть соединение
	 * @throws IOException
	 */
	public void close() throws IOException {
		IS.close();
		OS.close();
		SOCKET.close();
	}
	
	private int getN(String str) {
		int result = 0;
		for (int i = str.length()-1; i >= 0; i--) {
			if (str.charAt(i) == '\n') result++;
		}
		return result;
	}
	
	/**
     * прочитать только заголовок
     * @return
     * @throws IOException
     */
	private List<String> readHeader() throws IOException {
		ArrayList<String> result = new ArrayList<String>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(IS));
		boolean f = false;
		while(reader.ready()) {
			String line = reader.readLine();
			result.add(line);
			int n = getN(line);
			if (n >= 2) break;
			else if (n >= 1 && f) break;
			else if (n >= 1) f = true;
		}
		return result;
	}
	
	/**
	 * прочитать данные от клиента и заполнить HEADER
	 * @return данные для заполнения TYPE, PATH и HTTP
	 * @throws IOException
	 */
	private String[] parseHeader() throws IOException {
		boolean first = true;
		String result[] = null;
		for (String s : readHeader()) {
			if (s == null || s.isEmpty()) continue;
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
	
	/**
	 * отправить клиенту заголовок из respheader и respcode
	 * @throws IOException
	 */
	private void writeHeader() throws IOException {
		OS.write((respcode + '\n').getBytes());
		for (Entry<String, String> entry : respheader.entrySet()) {
			String resp = entry.getKey() + ": " + entry.getValue() + '\n';
			OS.write(resp.getBytes());
		}
		if (mime != null) OS.write(("Content-Type: " + mime + '\n').getBytes());
		OS.write("\n".getBytes());
	}
	
	public enum Type {GET, POST}
}
