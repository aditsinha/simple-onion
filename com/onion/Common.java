package com.onion;

public class Common {
	// port on which all server sockets will operate.
	public static final int PORT = 6789;
	public static final boolean TEST = true;

	private Common(){}

	public static void log(String str) {
		if(Common.TEST)
			System.out.println(str);
	}
}