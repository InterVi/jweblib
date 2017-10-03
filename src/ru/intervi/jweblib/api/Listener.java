package ru.intervi.jweblib.api;

import java.net.Socket;

/**
 * интерфейс, который должен наследовать обработчик событий
 */
public interface Listener {
	public void onStart();
	public void onStop();
	public void onConnect(Socket sock);
}
