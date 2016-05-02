package com.onion;

import java.net.*;
import java.util.Random;

public class Client {
	ThreadPool serviceThreads;
	HashMap<Socket, Connection> connMap;
		
	Config conf;	

	public class Client(String configName) {
		config = new Config(configName);
		connMap = new HashMap<Socket, Connection>();	
	}

	private void establishConnection() {

	} 

	private ArrayList<int> getCircuitHops() {
		Random rnd = new Random();
		ArrayList<int> al = new ArrayList<int>();
		int switchesAvailable = config.getSwitchesCount();	
		int numberOfHops = config.getSwitchesCount() / 2;
		int addedHops = 0;
		int nextHop = 0;
		for(int i = 0; i < numberOfHops; i++) {
			do {
				nextHop = rnd.nextInt(switchesAvailable);
			} while (al.contains(nextHop));
			
			al.add(nextHop);
		}

		return al;
	}
}
