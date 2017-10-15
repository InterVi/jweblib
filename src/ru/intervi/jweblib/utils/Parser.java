package ru.intervi.jweblib.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
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
				map.put(e[0].trim(), e[1].trim());
			}
		} else if (cookie.indexOf('=') != -1) {
			String e[] = cookie.split("=");
			map.put(e[0].trim(), e[1].trim());
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
			if (args == null || args.length < 2) continue;
			map.put(args[0].replace('+', ' '), args[1].replace('+', ' '));
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
		for (int i = 0; i < arr.length; i++) {
			if (arr[i] == '\n') {
				if (last == i - 1 || last == i - 2) return i;
				else last = i;
			}
		}
		return -1;
	}
	
	/**
	 * получить позицию окончания заголовка (двойной перевод строки)
	 * @param b
	 * @param charsetName кодировка
	 * @return -1 если не найдена
	 */
	public static int getBreak(byte b[], String charsetName) {
		byte s[] = Charset.forName(charsetName).encode(CharBuffer.wrap(new char[] {'\n'})).array();
		int last = -1;
		for (int i = 0; i < b.length; i += s.length) {
			if (i+s.length >= b.length) break;
			for (int n = i, a = 0; a < s.length; n++, a++) {
				if (b[n] != s[a]) continue;
			}
			if (last == i - 1 || last == i - 2) return i;
			else last = i;
		}
		return -1;
	}
	
	/**
	 * отделить переданые данные от заголовка
	 * @param b
	 * @param charsetName кодировка
	 * @return отделённые данные или null
	 * @throws IOException
	 */
	public static byte[] separateData(byte b[], String charsetName) throws IOException {
		ByteArrayOutputStream bao = new ByteArrayOutputStream();
		int pos = getBreak(b, charsetName);
		if (pos == -1) return null;
		for (int n = pos+1; n < b.length; n++) bao.write(b[n]);
		bao.flush();
		if (bao.size() == 0) return b;
		return bao.toByteArray();
	}
}
