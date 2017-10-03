package ru.intervi.jweblib.core;

import java.net.ServerSocket;

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
	public HTTPServer(short port, Listener listener) throws NullPointerException, IllegalArgumentException {
		if (port < 0 || port > 65535) throw new IllegalArgumentException("port < 0 or port > 65535");
		if (listener == null) throw new NullPointerException("listener is null");
		PORT = port;
		this.listener = listener;
	}
	
	public final short PORT;
	public volatile Listener listener = null;
	/**
	 * серверный сокет
	 */
	public volatile ServerSocket socket = null;
	/**
	 * поток, принимающий соединения
	 */
	public Acceptor acceptor = null;
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
		socket = new ServerSocket(PORT);
		acceptor = new Acceptor(this);
		listener.onStart();
		acceptor.start();
		start = true;
	}
	
	/**
	 * остановка сервера
	 * @throws IOException
	 * @throws SecurityException
	 */
	public void stop() throws IOException, SecurityException {
		listener.onStop();
		acceptor.interrupt();
		socket.close();
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
