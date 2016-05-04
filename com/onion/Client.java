package com.onion;

import java.io.*;
import java.net.*;
import java.security.*;
import java.lang.StringBuffer;
import java.util.Random;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class Client {
	// maps endpoint to the information necessary
	// only the main thread accesses it.
	ConcurrentHashMap<Integer, Connection> connMap;
	Config config;	
	ServerSocket welcomeSocket;
	boolean autoResponse = false;
    Random rand = new Random();
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
						int targetHost = Integer.parseInt(command[1]);

						if(targetHost < 0) {
							System.out.println("Illegal target host number.");
							continue;
						} else if (!connMap.containsKey(targetHost)) {
							System.out.println("Connection with this host not established");
							continue;
						}

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
						int targetHost = Integer.parseInt(command[1]);

						if(targetHost < 0 || targetHost >= config.getEndpointsCount()) {
							System.out.println("Illegal target host number");
							continue;
						}

						if(connMap.containsKey(targetHost)) {
							System.out.println("Connection with this host already established");
							continue;
						}		

						establishConnection(targetHost);
						continue;
					} catch (Exception e) {
						System.out.println("Illegal target host number");
						System.out.println("Usage: connect <ID-of-host>");
						continue;
					}
				} else if (command[0].equals("disconnect")) {
					try {
						int targetHost = Integer.parseInt(command[1]);

						if(!connMap.containsKey(targetHost)) {
							System.out.println("Uknown host: " + targetHost);
							throw new Exception();
						}

						Connection c = connMap.get(targetHost);
						// notice that this removes the connection from the hashtable too.
						c.disconnect();
						continue;
					} catch (Exception e) {
						System.out.println("Illegal target host number");
						System.out.println("Usage: disconnect <ID-of-host>");
						continue;
					}
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
                        // send the keys to the destination node
                        os.write(msg, 0, msg.length);

			Common.log("[Client]: Circuit established.");
			// the circuit has been established
			// save the symmetric keys
			ArrayList<Key> symmetricKeys = (ArrayList<Key>) ce.keyList;
			String name = config.getEndpoint(destination).getHostName();
			name = name.substring(0, name.indexOf("."));
			Connection c = new Connection(destination, sck, symmetricKeys, name);
			Common.log("[Client]: Connection Established.");
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
		int numberOfHops = config.getSwitchesCount();
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
					Common.log("[Client]: Waiting to accept connection");
					Socket newConn = sck.accept();
					Common.log("[Client]: Accepting connection");
					OnionMessage omsg = OnionMessage.unpack(newConn);
					if(omsg.getType() == OnionMessage.MsgType.KEY_REQUEST) {
						CircuitHopKeyRequest chkr = (CircuitHopKeyRequest) CipherUtils.deserialize(omsg.getData());
						// this will definitely be unique.
                                                int hashKey;
                                                do {
                                                    hashKey = config.getEndpointsCount() + rand.nextInt(1000 - config.getEndpointsCount());
                                                } while (connMap.contains(hashKey));

                                                List<Key> secretKeys = chkr.getKeys();
                                                Collections.reverse(secretKeys);

						Connection c = new Connection(hashKey, newConn, secretKeys);
						connMap.put(hashKey, c);
						Common.log("[Client]: Connection accepted with Anonymous" + hashKey);
						// TODO write about this somehow	
					} else {
						// TODO handle error
					}
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
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
			r = new Thread(new Reader(sck, name, this, connKey));
			wr.start();
			r.start();
		}


		public Connection(int connKey, Socket sck, List<Key> symKeys) {
			this(connKey, sck, symKeys, "Anonymous" + connKey);	
		}

		public void write(String msg) {
			try {
				msgsToSend.put(msg);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// shutsdown the connection.
		public void removeConnection() {
			wr.interrupt();
			r.interrupt();
			try {
				sck.close();
				connMap.remove(connKey);
			} catch (Exception e) {
				// ignore this exception.
				return;
			}
		}

		// sends the poison message
		public void disconnect() {
			try {
				// note -- connection is closed by the first hop, not us.
				byte[] poisonMsg = new OnionMessage(OnionMessage.MsgType.POISON, new byte[0]).pack();
				sck.getOutputStream().write(poisonMsg, 0, poisonMsg.length);
				wr.interrupt();
				r.interrupt();
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
		int connKey;

		public Reader(Socket sck, String name, Connection c, int connKey) {
			this.sck = sck;
			this.name = name;
			this.c = c;
			this.connKey = connKey;
		}

		public void run() { 
			OnionMessage incoming;
			String message;
			while(true) {
				try {
					incoming = OnionMessage.unpack(sck);

					// socket has been closed.
					if(incoming == null || sck.isClosed())
						break;

					if(incoming.getType() == OnionMessage.MsgType.DATA) {
						message = new String(incoming.getData(), "US-ASCII");
						// TODO Have a lock on stdin so that we don't get garbage.
						System.out.println(name +"(" + connKey + ")" + " says: " + message);
					} else if (incoming.getType() == OnionMessage.MsgType.POISON) {
						Common.log("[Client]: Received Poison Message. Disconnect.");
						c.removeConnection();
					} else {
						Common.log("[Client]: Unknown message type received.");
						continue;
					}
				} catch (Exception e) {
					e.printStackTrace();
					break;
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
				this.symKeys = new ArrayList<>(symKeys);
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
			byte[] encrMsg = CipherUtils.onionEncryptMessage(omsg, symKeys);
			os.write(encrMsg, 0, encrMsg.length);
		}
	}
}
