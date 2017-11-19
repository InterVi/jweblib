package ru.intervi.jweblib.api;

import java.nio.channels.SelectionKey;

/**
 * Класс-заглушка, чтобы реализовывать только нужные методы. onRead и onWrite нужно регистрировать самостоятельно, указывая селектор из HTTPServer.
 */
public class Pass implements Listener {
	@Override
	public void onStart() {
		
	}
	
	@Override
	public void onStop() {
		
	}
	
	@Override
	public void onConnect(SelectionKey key) {
		
	}
	
	@Override
	public void onAccept(SelectionKey key) {
		
	}
	
	@Override
	public void onRead(SelectionKey key) {
		
	}
	
	@Override
	public void onWrite(SelectionKey key) {
		
	}
	
	@Override
	public void onException(SelectionKey key, Exception e) {
		
	}
	
	@Override
	public void onRunException(Exception e) {
		
	}
	
	@Override
	public void onInvalid(SelectionKey key) {
		
	}
	
	@Override
	public void onClose(SelectionKey key) {
		
	}
}
