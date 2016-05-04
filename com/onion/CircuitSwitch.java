package com.onion;

import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.net.*;
import java.security.*;
import javax.crypto.*;
import java.util.Collections;


public class CircuitSwitch {
	SwitchThreadPool serviceThreads;
	Config conf;
	KeyPair keys;

	CircuitSwitch(String configName){
		Common.log("[CircuitSwitch]: Initialize.");
		keys = CipherUtils.generateRsaKeyPair();	
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
                    Common.log("[CircuitSwitch]: Wating for connection...");
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
		Key key;
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

		private void handleHopRequestMessage(OnionMessage msg) {
			Common.log("[CircuitSwitch]: Hop Request Message Received");
			CircuitHopRequestMessage chrm = (CircuitHopRequestMessage) CipherUtils.deserialize(msg.getData());
			if(chrm == null) {
				Common.log("[CircuitSwitch]: handleHopRequestMessage: cannot deserialize.");
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
				key = symKey;

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

		private void handleKeyRequestMessage(OnionMessage msg) {
			Common.log("[CircuitSwitch]: Key Request Message.");
			CircuitHopKeyRequest req = (CircuitHopKeyRequest) CipherUtils.deserialize(msg.getData());
			
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

		private void handleDataMessage(OnionMessage msg) {
			Common.log("[CircuitSwitch]: Data Message.");
			byte[] decryptedData = CipherUtils.applyCipher(msg.getData(), CipherUtils.SYM_ALGORITHM, Cipher.DECRYPT_MODE, key);	
			try {
				nextSendSocket.getOutputStream().write(decryptedData, 0 , decryptedData.length);	
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}

		private void handlePoisonMessage(OnionMessage msg) {
			Common.log("[CircuitSwitch]: Poison Message.");
			handleDataMessage(msg);
			try {
				// reset the state.
				sck.close();
				nextHop = null;
				sck = null;
				key = null;
				hopKeys = null;
				nextSendSocket = null;
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	
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
