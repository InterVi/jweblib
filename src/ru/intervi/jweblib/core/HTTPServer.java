package ru.intervi.jweblib.core;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.io.IOException;
import java.lang.SecurityException;
import java.lang.IllegalArgumentException;

import ru.intervi.jweblib.api.Listener;

/**
 * HTTP сервер
 */
public class HTTPServer {
	/**
	 * 
	 * @param port
	 * @param listener обработчик событий
	 * @throws NullPointerException
	 * @throws IllegalArgumentException
	 */
	public HTTPServer(String host, int port, Listener listener) throws NullPointerException, IllegalArgumentException {
		if (port < 0 || port > 65535) throw new IllegalArgumentException("port < 0 or port > 65535");
		if (listener == null) throw new NullPointerException("listener is null");
		HOST = host;
		PORT = port;
		this.listener = listener;
	}
	
	public final String HOST;
	public final int PORT;
	public volatile Listener listener = null;
	/**
	 * серверный сокет
	 */
	public volatile ServerSocketChannel server = null;
	public volatile Selector selector;
	/**
	 * поток, принимающий соединения
	 */
	public Worker worker = null;
	private boolean start = false;
	
	/**
	 * запуск сервера
	 * @throws IOException
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws NullPointerException
	 */
	public void start() throws IOException, SecurityException, IllegalArgumentException, NullPointerException {
		if (listener == null) throw new NullPointerException("wrong init HTTPServer");
		server = ServerSocketChannel.open();
		server.bind(new InetSocketAddress(HOST, PORT));
		server.configureBlocking(false);
		selector = Selector.open();
		server.register(selector, SelectionKey.OP_ACCEPT);
		listener.onStart();
		start = true;
	}
	
	public void startWorker() {
		worker = new Worker(this);
		worker.start();
	}
	
	public void stopWorker() {
		if (worker != null && worker.isAlive()) worker.interrupt();
	}
	
	/**
	 * остановка сервера
	 * @throws IOException
	 * @throws SecurityException
	 */
	public void stop() throws IOException, SecurityException {
		listener.onStop();
		stopWorker();
		server.close();
		start = false;
	}
	
	/**
	 * проверка, запущен ли сервер
	 * @return true - запущен
	 */
	public boolean isStarted() {
		return start;
	}
}
