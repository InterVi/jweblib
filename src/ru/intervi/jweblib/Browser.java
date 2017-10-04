package ru.intervi.jweblib;

import java.io.File;
import java.io.IOException;
import java.net.Socket;

import ru.intervi.jweblib.api.Listener;
import ru.intervi.jweblib.core.HTTPServer;
import ru.intervi.jweblib.utils.FileBrowser;
import ru.intervi.jweblib.utils.Processor;

/**
 * запускает простейший файловый менеджер
 */
public class Browser implements Listener {
	public Browser(String path, short port) {
		PATH = new File(path);
		SERVER = new HTTPServer(port, this);
		try {SERVER.start();}
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
	
	@Override
	public void onStart() {
		System.out.println("Start file browser on " + PATH.getAbsolutePath());
	}
	
	@Override
	public void onStop() {
		System.out.println("Stop file browser on " + PATH.getAbsolutePath());
	}
	
	@Override
	public void onConnect(Socket sock) {
		new Worker(sock).start();
	}
	
	public void stop() {
		try {SERVER.stop();}
		catch(IOException e) {e.printStackTrace();}
	}
	
	private class Worker extends Thread {
		public Worker(Socket sock) {
			SOCK = sock;
		}
		
		private final Socket SOCK;
		
		@Override
		public void run() {
			FileBrowser browser = null;
			try {
				browser = new FileBrowser(SOCK, PATH);
				System.out.println("Connect " + SOCK.getInetAddress().getHostAddress() + ", " + (browser.PROC.type == Processor.Type.GET ? "GET " : "POST ") + browser.PATH.getAbsolutePath());
				browser.run();
			} catch(Exception e) {e.printStackTrace();}
			finally {
				try {
					if (browser != null) browser.PROC.close();
				} catch(Exception e) {e.printStackTrace();}
			}
		}
	}
}
