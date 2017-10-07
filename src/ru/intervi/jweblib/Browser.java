package ru.intervi.jweblib;

import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map.Entry;

import ru.intervi.jweblib.api.Pass;
import ru.intervi.jweblib.core.HTTPServer;
import ru.intervi.jweblib.utils.FileBrowser;
import ru.intervi.jweblib.utils.FileSender;
import ru.intervi.jweblib.utils.Processor;

/**
 * запускает простейший файловый менеджер
 */
public class Browser extends Pass {
	public Browser(String host, String path, int port) {
		PATH = new File(path);
		SERVER = new HTTPServer(host, port, this);
		try {SERVER.start(); SERVER.startWorker();}
		catch(IOException e) {e.printStackTrace();}
		catch(SecurityException e) {e.printStackTrace();}
		catch(IllegalArgumentException e) {e.printStackTrace();}
		catch(NullPointerException e) {e.printStackTrace();}
		catch(Exception e) {e.printStackTrace();}
		if (SERVER != null && SERVER.isStarted()) System.out.println("Server started");
		else System.out.println("Server not started...");
	}
	
	private final File PATH;
	private final HTTPServer SERVER;
	private volatile HashMap<SocketAddress, FileSender> map = new HashMap<SocketAddress, FileSender>();
	private volatile HashMap<SocketAddress, Processor> map2 = new HashMap<SocketAddress, Processor>();
	
	@Override
	public void onStart() {
		System.out.println("Start file browser on " + PATH.getAbsolutePath());
	}
	
	@Override
	public void onStop() {
		if (SERVER.isStarted())
			System.out.println("Stop file browser on " + PATH.getAbsolutePath());
	}
	
	@Override
	public void onAccept(SelectionKey key) {
		try {
			SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
			if (channel == null) return;
			channel.configureBlocking(false);
			channel.register(SERVER.selector, SelectionKey.OP_READ);
			map2.put(channel.getRemoteAddress(), new Processor(channel));
		} catch(Exception e) {e.printStackTrace();}
	}
	
	@Override
	public void onRead(SelectionKey key) {
		try {
			SocketChannel channel = (SocketChannel) key.channel();
			if (!map2.containsKey(channel.getRemoteAddress())) return;
			Processor proc = map2.get(channel.getRemoteAddress());
			proc.callRead();
			if (proc.isHeaderReady(proc.getData())) proc.callParseHeader(proc.readData());
			else return;
			FileBrowser browser = new FileBrowser(proc, PATH);
			System.out.println("Connect " + channel.getRemoteAddress().toString() + ", " + (browser.PROC.type == Processor.Type.GET ? "GET " : "POST ") + browser.PATH.getAbsolutePath());
			FileSender sender = browser.run(true);
			if (sender != null) {
				map.put(channel.getRemoteAddress(), sender);
				channel.register(SERVER.selector, SelectionKey.OP_WRITE);
				map2.remove(channel.getRemoteAddress());
			}
			else browser.PROC.close();
		} catch(Exception e) {e.printStackTrace();}
	}
	
	@Override
	public void onWrite(SelectionKey key) {
		SocketChannel channel = (SocketChannel) key.channel();
		try {
			if (channel == null) return;
			SocketAddress addr = channel.getRemoteAddress();
			if (!map.containsKey(addr)) return;
			FileSender fs = map.get(addr);
			fs.callWrite();
			if (!fs.PROC.isWriting()) {
				fs.PROC.close();
				map.remove(addr);
			}
		} catch(Exception e) {
			e.printStackTrace();
			try {
				key.channel().close();
				if (map.containsKey(channel.getRemoteAddress()))
					map.remove(channel.getRemoteAddress());
			} catch(Exception e2) {e2.printStackTrace();}
		}
	}
	
	private void clear() {
		for (Entry<SocketAddress, FileSender> entry : map.entrySet()) {
			try {
				if (!entry.getValue().PROC.CHANNEL.isOpen()) map.remove(entry.getKey());
			} catch(Exception e) {map.remove(entry.getKey());}
		}
		for (Entry<SocketAddress, Processor> entry : map2.entrySet()) {
			try {
				if (!entry.getValue().CHANNEL.isOpen()) map2.remove(entry.getKey());
			} catch(Exception e) {map2.remove(entry.getKey());}
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
	public void onRunException() {
		map.clear();
		map2.clear();
		stop();
	}
	
	public synchronized void stop() {
		try {SERVER.stop(); map.clear(); map2.clear();}
		catch(IOException e) {e.printStackTrace();}
	}
}
