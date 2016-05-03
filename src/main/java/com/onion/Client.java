package com.onion;

import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.HashMap;
import java.util.ArrayList;

public class Client {
	// maps endpoint to the information necessary
	HashMap<Integer, Connection> connMap;
	Config config;	

	public Client(String configName) {
		config = new Config(configName);
		connMap = new HashMap<Integer, Connection>();	
	}

	private void establishConnection() {
		
        ArrayList<Integer> hops = getCircuitHops();
		int destination = getDestination();
		hops.add(destination);

		CircuitEstablishment ce = new CircuitEstablishment(hops, destination);
		
		byte[] msg = CipherUtils.serialize(ce.getFirstMessage());
		OnionMessage response = null;
		try {
			Socket sck = new Socket();
			sck.connect(new InetSocketAddress(config.getSwitch(hops.get(0)), Common.PORT));
			OutputStream os = sck.getOutputStream();

			while(!ce.isConnectionEstablished) {
				os.write(msg, 0, msg.length);
				response = OnionMessage.unpack(sck);
				response = ce.processMessage(response);
			}	
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		
	} 

	private ArrayList<Integer> getCircuitHops() {
		Random rnd = new Random();
		ArrayList<Integer> al = new ArrayList<Integer>();
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

	private int getDestination() {
		if(connMap.size() == config.getEndpointsCount())
			return -1;

		Random rnd = new Random();

		// single session between endpoints.
		int dest;
		do {
			dest = rnd.nextInt(config.getEndpointsCount());
		} while(!connMap.containsKey(dest));

		return dest;
	}

	private class Connection { 
		public Connection() {

		}
	}
}
