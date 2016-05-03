package com.onion;

/**
 * Hello world!
 *
 */

import java.security.*;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class App 
{
    public static void main( String[] args )
    {

      Security.addProvider(new BouncyCastleProvider());
    	if(args.length != 2) {
    		System.err.println("Wrong number of arguments.");
    		System.err.println("Usage: java App <CLIENT|SWITCH> <config_file_name>");
            return;        
       	}

       	if(args[0].equals("CLIENT")) {
       		Client c = new Client(args[1]);

       	} else if (args[0].equals("SWITCH")) {
       		CircuitSwitch cs = new CircuitSwitch(args[1]);
       		while(true) {
       			try {
       				Thread.sleep(1000);
       			} catch (Exception e) {
       				//ignore
       			}
       		}
       } else {
         System.err.println("Usage: java App <CLIENT|SWITCH> <config_file_name>");
       }
    }
}
