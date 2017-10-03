package ru.intervi.jweblib.core;

import java.io.IOException;
import java.net.SocketException;

public class Acceptor extends Thread {
	public Acceptor(HTTPServer server) throws NullPointerException {
		if (server == null) throw new NullPointerException("server is null");
		SERVER = server;
	}
	
	private final HTTPServer SERVER;
	
	@Override
	public void run() {
		while(true) {
			try {
				SERVER.listener.onConnect(SERVER.socket.accept());
			}
			catch(SocketException e) {
				if (!e.getMessage().equals("Socket closed")) e.printStackTrace();
				break;
			}
			catch(IOException e) {e.printStackTrace();}
			catch(Exception e) {e.printStackTrace();}
		}
	}
}
