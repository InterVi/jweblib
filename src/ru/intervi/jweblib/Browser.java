package ru.intervi.jweblib;

import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
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
	public Browser(String host, String path, int port, String index, boolean gzip, int buffer) {
		PATH = new File(path);
		INDEX = index;
		GZIP = gzip;
		BUFFER = buffer;
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
	private final String INDEX;
	private final boolean GZIP;
	private final int BUFFER;
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
			if (!map2.containsKey(channel.getRemoteAddress())) {
				channel.close();
				return;
			}
			Processor proc = map2.get(channel.getRemoteAddress());
			proc.callRead();
			if (proc.isHeaderReady(proc.getData())) proc.callParseHeader(proc.readData());
			else return;
			String url = proc.path;
			File path = null;
			if (INDEX != null) {
				path = FileBrowser.getPath(PATH, url);
				if (path.isDirectory()) {
					File file = new File(path, INDEX);
					if (file.isFile()) path = file;
				}
			}
			FileBrowser browser = new FileBrowser(proc, Processor.getRespheader(), PATH, url, path);
			System.out.println("Connect " + channel.getRemoteAddress().toString() + ", " + (browser.PROC.type == Processor.Type.GET ? "GET " : "POST ") + browser.PATH.getAbsolutePath());
			if (!browser.PATH.exists()) return;
			FileSender sender = browser.run(true, GZIP, BUFFER);
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
		Iterator<Entry<SocketAddress, FileSender>> iter = map.entrySet().iterator();
		while(iter.hasNext()) {
			Entry<SocketAddress, FileSender> entry = iter.next();
			try {
				if (!entry.getValue().PROC.CHANNEL.isOpen()) iter.remove();
			} catch(Exception e) {
				e.printStackTrace();
				iter.remove();
			}
		}
		Iterator<Entry<SocketAddress, Processor>> iter2 = map2.entrySet().iterator();
		while(iter2.hasNext()) {
			Entry<SocketAddress, Processor> entry = iter2.next();
			try {
				if (!entry.getValue().CHANNEL.isOpen()) iter2.remove();
			} catch(Exception e) {
				e.printStackTrace();
				iter2.remove();
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
		map2.clear();
		stop();
	}
	
	@Override
	public void onClose(SelectionKey key) {
		clear();
	}
	
	public synchronized void stop() {
		try {SERVER.stop(); map.clear(); map2.clear();}
		catch(IOException e) {e.printStackTrace();}
	}
}
