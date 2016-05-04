package com.onion;

import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.net.*;
import java.security.*;
import javax.crypto.*;
import java.util.Collections;

/*
	CircuitSwitch.java

	Represents a single switch available. The switches are multithreaded
	and stateful -- a single switch can server as many connections as many
	threads it possesses and contains the state of a connection until
	it receives a poison message. For more about the protocol please see the 
	write-up or readme. A single switch (a single thread) services traffic 
	in both directions.
*/


public class CircuitSwitch {
	SwitchThreadPool serviceThreads;
	Config conf;

	CircuitSwitch(String configName){
		Common.log("[CircuitSwitch]: Initialize.");
		conf = new Config(configName);
		try { 
			serviceThreads = new SwitchThreadPool(new ServerSocket(Common.PORT));
			serviceThreads.start();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private class SwitchThreadPool {
	    // thread pool used by the switch
	    int poolSize;
	    LinkedBlockingQueue<Socket> requests;
	    ArrayList<Thread> threads;
	    ServerSocket welcomeSocket;
	    Key key;

	    public SwitchThreadPool(ServerSocket welcomeSocket) {
			this(7, welcomeSocket);
	    }

	    public SwitchThreadPool(int poolSize, ServerSocket welcomeSocket) {
			this.requests = new LinkedBlockingQueue<Socket>();
			this.threads = new ArrayList<Thread>();
			this.welcomeSocket = welcomeSocket;
			this.poolSize = poolSize;

			initPool();
		}

		public void start() throws Exception {
			
			for (Thread t : threads) {
			    t.start();
			}

			while(true) {
			    try {
                    Common.log("[CircuitSwitch]: Waiting for connection...");
                    requests.add(welcomeSocket.accept());    
                    Common.log("[CircuitSwitch]: Accepted connection...");
			    } catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
			    }
			}
	    }

	    private void initPool() {
		// spin off the threads.
		for (int i = 0; i < poolSize; i++) {
			threads.add(new Thread(new SwitchWorker(requests)));
	    	}
	    }	
	}	
	
	private class SwitchWorker implements Runnable {
		LinkedBlockingQueue<Socket> requests;
		// a single thread can service a single connection.
		Socket sck = null;
		Socket nextHop = null;
		Socket nextSendSocket = null;
		KeyPair keys = null;
		Key mySymKey = null;
		ArrayList<Key> hopKeys = null;

		public SwitchWorker(LinkedBlockingQueue<Socket> requests) {
			this.requests = requests;
		}

		public void run () {
			while(true) {
				try {
					// accept a new connection only if currently not serving one.
                    if(sck == null) {
                        sck = requests.take();
                        keys = CipherUtils.generateRsaKeyPair();	
                        Common.log("[CircuitSwitch]: Dequeued connection");
                    }

                    OnionMessage msg = null;
                    while(true) {
                    	try {
	                    	if(sck.getInputStream().available() > 0) {
		                    	msg = OnionMessage.unpack(sck);
		                    	nextSendSocket = nextHop;
	                    		break;
	                    	} else if (nextHop != null && nextHop.getInputStream().available() > 0) {
	                    		msg = OnionMessage.unpack(nextHop);
	                    		nextSendSocket = sck;
	                    		break;
	                    	}
	                    	Thread.sleep(100);
	                    } catch (Exception e) {
	                    	e.printStackTrace();
	                    	System.exit(1);
	                    }
	                }

					switch(msg.getType()) {
						case DATA:
							handleDataMessage(msg);
							break;
						case HOP_REQUEST:
							handleHopRequestMessage(msg);
							break;
						case POISON:
							handlePoisonMessage(msg);
							break;
						case KEY_REQUEST:
							handleKeyRequestMessage(msg);
							break;
						case KEY_REPLY:
							handleKeyReplyMessage(msg);
							break;
						default:
							System.out.println("[CircuitSwitch]: Illegal packet type");
							System.exit(1);    
					}
				} catch (Exception e) {
					// this is bad
					e.printStackTrace();
					System.exit(1);
				}
			}
		}

		// opens a socket to the next hop and responds with a success message.
		private void handleHopRequestMessage(OnionMessage msg) {
			Common.log("[CircuitSwitch]: Hop Request Message Received");
			CircuitHopRequestMessage chrm = (CircuitHopRequestMessage) CipherUtils.deserialize(msg.getData());
			if(chrm == null) {
				System.out.println("[CircuitSwitch]: handleHopRequestMessage: cannot deserialize.");
				System.exit(1);
			}

			try {
                Key symKey = 
                    CipherUtils.getCipher(CipherUtils.ASYM_ALGORITHM, Cipher.UNWRAP_MODE, keys.getPrivate())
                    .unwrap(chrm.getKeyData(), "AES", Cipher.SECRET_KEY);

                CircuitHopRequestMessage.Payload payload = 
                    (CircuitHopRequestMessage.Payload) 
                    CipherUtils.deserialize(CipherUtils.applyCipher(chrm.getPayloadData(), CipherUtils.SYM_ALGORITHM, Cipher.DECRYPT_MODE, symKey));


				nextHop = new Socket();
				int nextNode = payload.getNextNode();
				InetAddress nextNodeAddr = null;
				if(nextNode >= conf.getSwitchesCount()) {
					nextNode -= conf.getSwitchesCount();
					nextNodeAddr = conf.getEndpoint(nextNode); 
				} else {
					nextNodeAddr = conf.getSwitch(nextNode);
				}

				Common.log("[CircuitSwitch]: Establishing connection with: " + nextNodeAddr.getHostName());
				nextHop.connect(new InetSocketAddress(nextNodeAddr, Common.PORT));
				mySymKey = symKey;

				// create an answer message.
				CircuitHopReplyMessage resp = new CircuitHopReplyMessage();
				byte[] respBytes = CipherUtils.serialize(resp);

				OnionMessage response = new OnionMessage(OnionMessage.MsgType.HOP_REPLY, respBytes);
				byte[] responseEncr = CipherUtils.onionEncryptMessage(response, hopKeys);
				sck.getOutputStream().write(responseEncr, 0, responseEncr.length);
				Common.log("[CircuitSwitch]: Response Sent.");
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}		

		// returns the public key of the node to through the circuit
		private void handleKeyRequestMessage(OnionMessage msg) {
			Common.log("[CircuitSwitch]: Key Request Message.");
			CircuitHopKeyRequest req = (CircuitHopKeyRequest) CipherUtils.deserialize(msg.getData());
			
			if(req == null) {
				System.out.println("[CircuitSwitch]: Cannot deserialize a key request message.");
				System.exit(1);
			}

			// reverse the key list.
			hopKeys = (ArrayList<Key>)req.getKeys();
			Collections.reverse(hopKeys);

			// get the body of the response
			CircuitHopKeyReply resp = new CircuitHopKeyReply(keys.getPublic());
			byte[] respBytes = CipherUtils.serialize(resp);

			byte[] response = new OnionMessage(OnionMessage.MsgType.KEY_REPLY, respBytes).pack();
			
			// send.
			try {
				sck.getOutputStream().write(response, 0, response.length);
				Common.log("[CircuitSwitch]: Response Sent.");
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}

		// decrypts a layer and passes data on in the correct direction
		private void handleDataMessage(OnionMessage msg) {
			Common.log("[CircuitSwitch]: Data Message.");
			byte[] decryptedData = CipherUtils.applyCipher(msg.getData(), CipherUtils.SYM_ALGORITHM, Cipher.DECRYPT_MODE, mySymKey);	
			try {
				nextSendSocket.getOutputStream().write(decryptedData, 0 , decryptedData.length);	
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}

		// passes the poison message on and forgets the current circuit.
		private void handlePoisonMessage(OnionMessage msg) {
			Common.log("[CircuitSwitch]: Poison Message.");
			byte[] msgBytes = msg.pack();
			try {
				// forward the message
				nextSendSocket.getOutputStream().write(msgBytes, 0, msgBytes.length);
			
				// reset the state.
				if(nextSendSocket == sck){
					Common.log("[CircuitSwitch]: Poison came from initiator.");
					nextHop.close();
				} else {
					Common.log("[CircuitSwitch]: Poison came from source.");
					sck.close();
				}

				nextHop = null;
				sck = null;
				mySymKey = null;
				keys = null;
				hopKeys = null;
				nextSendSocket = null;
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	
		// passes a key reply on.
		private void handleKeyReplyMessage(OnionMessage msg) {
			// serialize and push through
			Common.log("[CircuitSwitch]: Key Reply Message.");
			byte[] data = msg.pack();
			try {
				sck.getOutputStream().write(data, 0, data.length);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
}
