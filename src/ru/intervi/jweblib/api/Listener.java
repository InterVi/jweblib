package ru.intervi.jweblib.api;

import java.nio.channels.SelectionKey;

/**
 * Интерфейс, который должен наследовать обработчик событий. onRead и onWrite нужно регистрировать самостоятельно, указывая селектор из HTTPServer.
 */
public interface Listener {
	public void onStart();
	public void onStop();
	public void onConnect(SelectionKey key);
	public void onAccept(SelectionKey key);
	public void onRead(SelectionKey key);
	public void onWrite(SelectionKey key);
	public void onException(SelectionKey key, Exception e);
	public void onRunException();
	public void onInvalid(SelectionKey key);
}
