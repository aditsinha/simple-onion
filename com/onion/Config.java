package com.onion;

import java.util.ArrayList;
import java.io.*;
import java.net.*;

/*
	Config.java

	Contains the parsing functions for config and objects holding
	the necessary adresses specified within config. Config format:

	SWITCH_NAME_1\n
	SWITCH_NAME_2\n
	\n
	ENDPOINT_NAME_1\n
	ENDPOINT_NAME_2\n

	Example:

	lion.zoo.cs.yale.edu
	monkey.zoo.cs.yale.edu
	tiger.zoo.cs.yale.edu
	hare.zoo.cs.yale.edu

	tick.zoo.cs.yale.edu
	newt.zoo.cs.yale.edu
*/

public class Config {
	private ArrayList<InetAddress> switches;
	private ArrayList<InetAddress> endpoints;

	// Read in hosts and switches from configuration file and resolve addresses.
	public Config(String configName) {
		Common.log("[Config]: Reading Config File.");
		switches = new ArrayList<InetAddress>();
		endpoints = new ArrayList<InetAddress>();

		FileReader fileReader;
		try {
		    fileReader = new FileReader(new File(configName));

		    BufferedReader buffRead = new BufferedReader(fileReader);

			// populate the set of switches
			Common.log("[Config]: Switches: ");
			String configLine;
			while(((configLine = buffRead.readLine()) != null) && !configLine.equals("")) {
				try {
					Common.log("\t [Config]: " + configLine);
					switches.add(InetAddress.getByName(configLine));
				} catch (UnknownHostException e) {
					System.err.println("[Config]: Could not find host: " + configLine);
				}
			}

			Common.log("[Config]: Endpoints: ");
			// populate the set of endpoints
			while((configLine = buffRead.readLine()) != null) {
				try {
					Common.log("\t [Config]: " + configLine);
					endpoints.add(InetAddress.getByName(configLine));
				} catch (UnknownHostException e) {
					System.err.println("[Config]: Could not find host: " + configLine);
				}
			}
		} catch (FileNotFoundException e) {
		    System.err.println("[Config]: Cannot find config file: " + configName + ".");
		    System.exit(1);
		} catch (Exception e) {
		    System.err.println("[Config]: Error occured while reading in config.");
		    e.printStackTrace();
		    System.exit(1);
		}
	}

	public int getEndpointsCount() {
		return endpoints.size();
	}

	public InetAddress getEndpoint(int i) {
		return endpoints.get(i);
	}

	public int getSwitchesCount() {
		return switches.size();
	}

	public InetAddress getSwitch(int i) {
		return switches.get(i);
	}
}
