package ru.intervi.jweblib.core;

import java.nio.channels.SelectionKey;

/**
 * Обработчик событий. onConnect, onRead и onWrite нужно регистрировать самостоятельно, указывая селектор из HTTPServer.
 */
public class Worker extends Thread {
	public Worker(HTTPServer server) throws NullPointerException {
		if (server == null) throw new NullPointerException("server is null");
		SERVER = server;
	}
	
	private final HTTPServer SERVER;
	
	@Override
	public void run() {
		while(SERVER.server.isOpen()) {
			try {
				SERVER.selector.select();
				for (SelectionKey key : SERVER.selector.selectedKeys()) {
					try {
						if (!key.isValid()) {
							SERVER.listener.onInvalid(key);
							key.channel().close();
							continue;
						}
						if (!key.channel().isOpen()) SERVER.listener.onClose(key);
						if (key.isConnectable()) SERVER.listener.onConnect(key);
						if (key.isAcceptable()) SERVER.listener.onAccept(key);
						if (key.isReadable()) SERVER.listener.onRead(key);
						if (key.isWritable()) SERVER.listener.onWrite(key);
					} catch(Exception e) {SERVER.listener.onException(key, e);}
				}
			} catch(Exception e) {SERVER.listener.onRunException(e);}
		}
	}
}
