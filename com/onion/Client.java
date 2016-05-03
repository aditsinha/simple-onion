package com.onion;

import java.io.*;
import java.net.*;
import java.security.*;
import java.lang.StringBuffer;
import java.util.Random;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class Client {
	// maps endpoint to the information necessary
	// only the main thread accesses it.
	ConcurrentHashMap<Integer, Connection> connMap;
	Config config;	
	ServerSocket welcomeSocket;
	boolean autoResponse = false;
	Thread acceptor;

	public Client(String configName) {
		config = new Config(configName);
		connMap = new ConcurrentHashMap<Integer, Connection>(32, (float)0.75, 2);	
		try {
			acceptor = new Thread(new Acceptor(new ServerSocket(Common.PORT)));
			acceptor.start();
			startCommandLoop();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private void startCommandLoop() {
		String[] command;
		BufferedReader bf = new BufferedReader(new InputStreamReader(System.in));
		try {
			while( (command = bf.readLine().split(" ")) != null) {
				if(command[0].equals("send")) {
					try {
						int targetHost = stringToInt(command[1], true);

						// merge the string back
						StringBuffer message = new StringBuffer();
						for (int i = 2; i < command.length; i++) {
						   message.append( command[i] );
						   message.append( " " );
						}

						connMap.get(targetHost).write(message.toString());
						continue;
					} catch (Exception e) {
						System.out.println("Illegal target host number");
						System.out.println("Usage: send <ID-of-host> <msg>");
						continue;
					}
				} else if (command[0].equals("connect")) {
					try {
						int targetHost = stringToInt(command[1], false);
						
						establishConnection(targetHost);
						continue;
					} catch (Exception e) {
						System.out.println("Illegal target host number");
						System.out.println("Usage: connect <ID-of-host>");
						continue;
					}
				} else if (command[0].equals("disconnect")) {
					// TODO disconnect
				}

				System.out.println("Wrong command given. Available commands:");
				System.out.println("\t -- \t send <Host Number>");
				System.out.println("\t -- \t connect <Host Number>");
				System.out.println("\t -- \t disconnect <Host Number>");
				
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private int stringToInt(String strInt, boolean contains) throws Exception {
		int targetHost = Integer.parseInt(strInt);
		if(targetHost < 0 || targetHost >= config.getEndpointsCount()) {
			System.out.println("Illegal target host number");
			System.out.println("Usage: connect <ID-of-host>");
			throw new Exception();
		}

		if(connMap.contains(targetHost) == contains) {
			System.out.println("Connection with this host already established");
			System.out.println("Usage: connect <ID-of-host>");
			throw new Exception();
		}

		return targetHost;
	}

	private void establishConnection(int destination) {
		ArrayList<Integer> hops = getCircuitHops();

		CircuitEstablishment ce = new CircuitEstablishment(hops, destination, config);
		
		byte[] msg = ce.getFirstMessage();
		OnionMessage response = null;
		try {
			Socket sck = new Socket();
			sck.connect(new InetSocketAddress(config.getSwitch(hops.get(0)), Common.PORT));
			OutputStream os = sck.getOutputStream();

			while(!ce.isConnectionEstablished) {
				os.write(msg, 0, msg.length);
				response = OnionMessage.unpack(sck);
				// get the new message
				msg = ce.processMessage(response);
			}

			Common.log("[Client]: Circuit established.");
			// the circuit has been established
			// save the symmetric keys
			ArrayList<Key> symmetricKeys = (ArrayList<Key>) ce.keyList;
			Connection c = new Connection(destination, sck, symmetricKeys);
			Common.log("Connection Established.");
			connMap.put(destination, c);

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
		Common.log("[Client]: Chose circuit:");
		for(int i = 0; i < numberOfHops; i++) {
			do {
				nextHop = rnd.nextInt(switchesAvailable);
			} while (al.contains(nextHop));
			
			al.add(nextHop);
			Common.log("\t " + i + " : " + config.getSwitch(nextHop).getHostName());
		}

		// new line.
		Common.log("");

		return al;
	}

	// accepts incomming connection requests
	// and spawns Reader/Writer pairs to handle communication
	private class Acceptor implements Runnable {
		ServerSocket sck;
		public Acceptor(ServerSocket sck) {
			this.sck = sck;
		}

		public void run() {
			while(true) {
				try {
					Socket newConn = sck.accept();
					OnionMessage omsg = OnionMessage.unpack(newConn);
					if(omsg.getType() == OnionMessage.MsgType.KEY_REQUEST) {
						CircuitHopKeyRequest chkr = (CircuitHopKeyRequest) CipherUtils.deserialize(omsg.getData());
						if(chkr == null){
							// TODO handle error 
						}
						// this will definitely be unique.
						int hashKey = sck.getLocalPort();
						Connection c = new Connection(hashKey, newConn, chkr.getKeys());
						connMap.put(hashKey, c);
						// TODO write about this somehow	
					} else {
						// TODO handle error
					}
				} catch (Exception e) {
					// TODO handle this.
				}
			}
		}
	}

	private class Connection {
		Socket sck;
		Thread wr;
		Thread r;
		LinkedBlockingQueue<String> msgsToSend;
		List<Key> symKeys;
		String name;
		// necessary to handle poison
		int connKey;
	
		public Connection(int connKey, Socket sck, List<Key> symKeys, String name) {
			this.msgsToSend = new LinkedBlockingQueue<String>();
			this.sck = sck;
			this.symKeys = symKeys;
			this.name = name;

			wr = new Thread(new Writer(sck, msgsToSend, symKeys));
			r = new Thread(new Reader(sck, name, this));
			wr.start();
			r.start();
		}


		public Connection(int connKey, Socket sck, List<Key> symKeys) {
			this(connKey, sck, symKeys, "Anonymous" + Integer.toString(connMap.size()));	
		}

		public void write(String msg) {
			try {
				msgsToSend.put(msg);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void killConnection() {
			try {
				wr.interrupt();
				r.interrupt();
				sck.close();
				connMap.remove(connKey);
			} catch (Exception e) {
				// TODO handle this
			}
		}
	}

	// handles reading the data from the socket 
	// and printing to the screen.
	private class Reader implements Runnable {
		Socket sck;
		String name;
		// back pointer to kill the connection if poison received
		Connection c;

		public Reader(Socket sck, String name, Connection c) {
			this.sck = sck;
			this.name = name;
			this.c = c;
		}

		public void run() { 
			OnionMessage incoming;
			String message;
			while(!Thread.currentThread().isInterrupted()) {
				try {
					incoming = OnionMessage.unpack(sck);
					if(incoming.getType() == OnionMessage.MsgType.DATA) {
						message = new String(incoming.getData(), "US-ACII");
						// TODO Have a lock on stdin so that we don't get garbage.
						System.out.println(name+" says: " + message);
					} else if (incoming.getType() == OnionMessage.MsgType.POISON) {
						c.killConnection();
					} else {
						// error
						// TODO handle
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	// handles taking input from the user 
	// and sending it to the receiver.
	private class Writer implements Runnable {
		Socket sck;
		LinkedBlockingQueue<String> msgToSend;
		List<Key> symKeys;
		OutputStream os;

		public Writer(Socket sck, LinkedBlockingQueue<String> msgToSend,
			List<Key> symKeys) {
			try {
				this.sck = sck;
				this.os = sck.getOutputStream();
				this.msgToSend = msgToSend;
				this.symKeys = symKeys;
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		public void run() {
			String toWrite = null;
			while(!Thread.currentThread().isInterrupted()) {
				try {
					toWrite = msgToSend.take();
					write(toWrite);
				} catch (InterruptedException e) {
					break;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		private void write(String message) throws Exception {
			byte[] msg = message.getBytes("US-ASCII");
			OnionMessage omsg = new OnionMessage(OnionMessage.MsgType.DATA, msg);
			byte[] encrMsg = CipherUtils.serialize(CipherUtils.onionEncryptMessage(omsg, symKeys));
			os.write(encrMsg, 0, encrMsg.length);
		}
	}
}
