package ru.intervi.jweblib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map.Entry;
import java.net.Socket;

import ru.intervi.jweblib.api.Listener;
import ru.intervi.jweblib.core.HTTPServer;
import ru.intervi.jweblib.utils.Processor;

/**
 * выводит HelloWorld всем клиентам
 */
public class HelloWorld implements Listener {
	/**
	 * 
	 * @param path путь к текстовому файлу (его содержимое будет выводится клиентам), null - HelloWorld
	 * @param port порт сервера
	 */
	public HelloWorld(String path, short port) {
		if (path == null) content = "<html><body><h1>Hello World!</h1></body></html>";
		else {
			try {
				File file = new File(path);
				BufferedReader reader = new BufferedReader(new FileReader(file));
				while(reader.ready()) content += (reader.readLine()) + "\n";
				reader.close();
			} catch(IOException e) {e.printStackTrace();}
		}
		SERVER = new HTTPServer(port, this);
		try {SERVER.start();}
		catch(IOException e) {e.printStackTrace();}
		catch(SecurityException e) {e.printStackTrace();}
		catch(IllegalArgumentException e) {e.printStackTrace();}
		catch(NullPointerException e) {e.printStackTrace();}
		catch(Exception e) {e.printStackTrace();}
		if (SERVER != null && SERVER.isStarted()) System.out.println("SERVER started");
		else System.out.println("SERVER not started...");
	}
	
	private String content = "";
	private final HTTPServer SERVER;
	
	@Override
	public void onStart() {
		System.out.println("Accept thread started");
	}
	
	@Override
	public void onStop() {
		System.out.println("Accept thread stopped");
	}
	
	@Override
	public void onConnect(Socket sock) {
		Processor proc = null;
		try {
			proc = new Processor(sock);
			System.out.println("Connect: " + proc.SOCKET.getInetAddress().getHostAddress());
			System.out.println(proc.TYPE.toString() + ' ' + proc.PATH + ' ' + proc.HTTP);
			for (Entry<String, String> entry : proc.HEADER.entrySet())
				System.out.println(entry.getKey() + ": " + entry.getValue());
			proc.respheader.put("SERVER", "jweblib " + String.valueOf(Main.VERSION));
			proc.respheader.put("Connection", "close");
			proc.writeResponse(content, false);
			proc.OS.flush();
			proc.OS.close();
		} catch(IOException e) {e.printStackTrace();}
		finally {
			if (proc == null) return;
			try {proc.close();}
			catch(IOException e) {e.printStackTrace();}
		}
	}
	
	public void stop() {
		try {SERVER.stop();}
		catch(IOException e) {e.printStackTrace();}
	}
}
