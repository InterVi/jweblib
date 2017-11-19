package ru.intervi.jweblib;

import java.util.Scanner;

public class Main {
	public static final String VERSION = "1.4";
	
	/**
	 * 
	 * @param args 0 - short порт, 1 - путь к текстовому файлу, 2 - опция -b
	 */
	public static void main(String[] args) {
		String host = "127.0.0.1";
		int port = 8080;
		String path = null; //директория для файл менеджера
		boolean browser = false; //запуск файл менеджера
		String index = null; //индексный файл (откроется вместо директории)
		if (args != null) {
			if (args.length >= 1) {
				if (args[0].equals("--version") || args[0].equals("-v")) {
					System.out.println("jweblib version: " + VERSION);
					return;
				}
				else if (args[0].equals("--help") || args[0].equals("-h")) {
					System.out.println("Using: java -jar jweblib.jar [host] [port] [path] options");
					System.out.println("Using: java -jar jweblib.jar --version or --help");
					System.out.println("Example: java -jar jweblib.jar 0.0.0.0 8080 ./ -b");
					System.out.println("Options:");
					System.out.println("[-b] - start file browser");
					System.out.println("[-i] file - index file");
					return;
				}
				else host = args[0];
			}
			if (args.length >= 2) {
				if (args[1].matches("^[0-9]*$")) port = Integer.parseInt(args[1]);
			}
			if (args.length >= 3) path = args[2];
			if (args.length >= 4 && args[3].equals("-b")) browser = true;
			for (int i = 3; i < args.length; i++) {
				switch(args[i]) {
				case "-b":
					browser = true;
					break;
				case "-i":
					if (args.length > i + 1) {
						index = args[i+1];
						i++;
					} else {
						System.out.println("no argument for [-i]");
						return;
					}
				}
			}
		}
		Browser bro = null;
		HelloWorld hw = null;
		if (browser) bro = new Browser(host, path, port, index);
		else hw = new HelloWorld(host, path, port);
		Scanner in = new Scanner(System.in);
		while (true) {
			if (in.next().equalsIgnoreCase("stop")) {
				if (hw != null) hw.stop();
				if (bro != null) bro.stop();
				break;
			}
		}
		in.close();
	}
}
