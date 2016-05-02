package com.onion;

import java.util.ArrayList;
import java.io.*;
import java.net.*;

public class Config {
	private ArrayList<InetAddress> switches;

	// Read in hosts from configuration file and resolve addresses.
	public Config(String configName) {
		switches = new ArrayList<InetAddress>();

        FileReader fileReader;
        try {
            fileReader = new FileReader(new File(configName));
        } catch (FileNotFoundException e) {
            System.err.println("[Config]: Cannot find config file: " + configName + ".");
            System.exit(1);
        } catch (Exception e) {
            System.err.println("[Config]: Error occured while reading in config.");
            e.printStackTrace();
            System.exit(1);
        }
		
		BufferedReader buffRead = new BufferedReader(fileReader);

		// populate the set of switches
		String configLine;
		while((configLine = bufferedReader.readLine()) != null) {
			try {
				switches.add(InetAddress.getByName(configLine))
			} catch (UnknownHostException e) {
				System.err.println("[Config]: Could not find host: " + configLine);
			}
		}
	}

	public int getSwitchesCount() {
		return switches.size();
	}

	public InetAddress getSwitch(int i) {
		return switches.get(i);
	}
}