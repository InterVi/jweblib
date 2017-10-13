package ru.intervi.jweblib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import ru.intervi.jweblib.api.Pass;
import ru.intervi.jweblib.core.HTTPServer;
import ru.intervi.jweblib.utils.Processor;

/**
 * выводит HelloWorld всем клиентам
 */
public class HelloWorld extends Pass {
	/**
	 * 
	 * @param path путь к текстовому файлу (его содержимое будет выводится клиентам), null - HelloWorld
	 * @param port порт сервера
	 */
	public HelloWorld(String host, String path, int port) {
		if (path == null) content = "<html><body><h1>Hello World!</h1></body></html>";
		else {
			try {
				File file = new File(path);
				BufferedReader reader = new BufferedReader(new FileReader(file));
				while(reader.ready()) content += (reader.readLine()) + '\n';
				reader.close();
			} catch(IOException e) {e.printStackTrace();}
		}
		SERVER = new HTTPServer(host, port, this);
		try {SERVER.start(); SERVER.startWorker();}
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
	private volatile HashMap<SocketAddress, Processor> map = new HashMap<SocketAddress, Processor>();
	
	@Override
	public void onStart() {
		System.out.println("WEB server started");
	}
	
	@Override
	public void onStop() {
		if (SERVER.isStarted())
			System.out.println("WEB server stopped");
	}
	
	@Override
	public void onAccept(SelectionKey key) {
		try {
			SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
			if (channel == null) return;
			channel.configureBlocking(false);
			channel.register(SERVER.selector, SelectionKey.OP_READ);
			map.put(channel.getRemoteAddress(), new Processor(channel));
		} catch(Exception e) {e.printStackTrace();}
	}
	
	@Override
	public void onRead(SelectionKey key) {
		try {
			SocketChannel channel = (SocketChannel) key.channel();
			if (!map.containsKey(channel.getRemoteAddress())) return;
			Processor proc = map.get(channel.getRemoteAddress());
			proc.callRead();
			if (proc.isHeaderReady(proc.getData())) proc.callParseHeader(proc.readData());
			else return;
			System.out.println("Connect: " + proc.CHANNEL.getRemoteAddress().toString());
			System.out.println(proc.type.toString() + ' ' + proc.path + ' ' + proc.http);
			for (Entry<String, String> entry : proc.HEADER.entrySet())
				System.out.println(entry.getKey() + ": " + entry.getValue());
			proc.writeResponse(content, false, Processor.PLAIN, 
					Processor.getRespheader("SERVER", "jweblib " + String.valueOf(Main.VERSION), "Connection", "close"));
			proc.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void clear() {
		Iterator<Entry<SocketAddress, Processor>> iter = map.entrySet().iterator();
		while(iter.hasNext()) {
			Entry<SocketAddress, Processor> entry = iter.next();
			try {
				if (!entry.getValue().CHANNEL.isOpen()) iter.remove();
			} catch(Exception e) {
				e.printStackTrace();
				iter.remove();
			}
		}
	}
	
	@Override
	public void onInvalid(SelectionKey key) {
		clear();
	}
	
	@Override
	public void onException(SelectionKey key, Exception e) {
		clear();
	}
	
	@Override
	public void onRunException(Exception e) {
		e.printStackTrace();
		map.clear();
		stop();
	}
	
	public synchronized void stop() {
		try {SERVER.stop();}
		catch(IOException e) {e.printStackTrace();}
	}
}
