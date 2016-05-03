package com.onion;

import java.util.ArrayList;
import java.io.*;
import java.net.*;

public class Config {
	private ArrayList<InetAddress> switches;
	private ArrayList<InetAddress> endpoints;

	// Read in hosts from configuration file and resolve addresses.
	//
	// The format of the config: a list of enter-separated switches
	// double new-line
	// a list of available end points
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
			while((configLine = buffRead.readLine()) != null || configLine == "") {
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
