package ru.intervi.jweblib.utils;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * парсеры и утилиты для работы с заголовками
 */
public class Parser {
	/**
	 * получить печеньки
	 * @param cookie
	 * @return
	 */
	public static Map<String, String> parseCookie(String cookie) {
		HashMap<String, String> map = new HashMap<String, String>();
		String args[] = cookie.split(";");
		if (args.length > 1) {
			for (String a : args) {
				String e[] = a.split("=");
				if (e.length > 1) map.put(e[0].trim(), e[1].trim());
				else map.put(e[0].trim(), null);
			}
		} else if (cookie.indexOf('=') != -1) {
			String e[] = cookie.split("=");
			if (e.length > 1) map.put(e[0].trim(), e[1].trim());
			else map.put(e[0].trim(), null);
		}
		return map;
	}
	
	/**
	 * получить параметры из URL
	 * @param url
	 * @return
	 */
	public static Map<String, String> parseParams(String url) {
		int ind = url.indexOf('?');
		if (ind == -1) return null;
		HashMap<String, String> map = new HashMap<String, String>();
		String str = url.substring(ind+1);
		int pind = str.indexOf(' ');
		if (pind != -1) str = str.substring(0, pind);
		String params[] = str.indexOf('&') == -1 ? new String[] {str} : str.split("&");
		for (String p : params) {
			String args[] = p.split("=");
			if (args.length > 1)
				map.put(args[0].replace('+', ' '), args[1].replace('+', ' '));
			else
				map.put(args[0].replace('+', ' '), null);
		}
		return map;
	}
	
	/**
	 * получить позицию окончания заголовка (двойной перевод строки)
	 * @param str
	 * @return -1 если не найдена
	 */
	public static int getBreak(String str) {
		char arr[] = str.toCharArray();
		int last = -1;
		int result = -1;
		for (int i = 0; i < arr.length; i++) {
			if (arr[i] == '\n') {
				if (last == i - 1 || last == i - 2) result = i;
				else last = i;
			}
		}
		return result;
	}
	
	/**
	 * отделить переданые данные от заголовка
	 * @param b
	 * @return отделённые данные или null
	 * @throws IOException
	 */
	public static byte[] separateData(byte b[]) throws IOException {
		int pos = getBreak(new String(b));
		if (pos == -1) return null;
		return Arrays.copyOf(b, pos+1);
	}
	
	/**
	 * отделить заголовк от переданных данных
	 * @param b
	 * @return заголовок или null
	 * @throws IOException
	 */
	public static byte[] separateHeader(byte b[]) throws IOException {
		int pos = getBreak(new String(b));
		if (pos == -1) return null;
		return Arrays.copyOfRange(b, 0, pos);
	}
}
