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
					requests.add(welcomeSocket.accept());    
			    } catch (Exception e) {
					e.printStackTrace();
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
		Key key;
		ArrayList<Key> hopKeys = null;

		public SwitchWorker(LinkedBlockingQueue<Socket> requests) {
			this.requests = requests;
		}

		public void run () {
			while(true) {
				try {
					// accept a new connection only if currently not serving one.
					if(sck == null)
						sck = requests.take();

					OnionMessage msg = OnionMessage.unpack(sck);
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
			byte[] decryptedData = CipherUtils.applyCipher(msg.getData(), "RSA/ECB/PKCSPadding", Cipher.DECRYPT_MODE, keys.getPrivate());
			CircuitHopRequestMessage chrm = (CircuitHopRequestMessage) CipherUtils.deserialize(msg.getData());
			if(chrm == null) {
				Common.log("[CircuitSwitch]: handleHopRequestMessage: cannot deserialize.");
				System.exit(1);
			}

			try {
				nextHop = new Socket();
				nextHop.connect(new InetSocketAddress(conf.getSwitch(chrm.getNextNode()), Common.PORT));
				key = chrm.getSecretKey();

				// create an answer message.
				CircuitHopReplyMessage resp = new CircuitHopReplyMessage();
				byte[] respBytes = CipherUtils.serialize(resp);

				OnionMessage response = new OnionMessage(OnionMessage.MsgType.HOP_REPLY, respBytes);
				byte[] responseEncr = CipherUtils.serialize(CipherUtils.onionEncryptMessage(response, hopKeys));

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
			
			if(req == null)
				Common.log("[CircuitSwitch]: Cannot deserialize the request.");

			// reverse the key list.
			hopKeys = (ArrayList<Key>)req.getKeys();
			Collections.reverse(hopKeys);

			// get the body of the response
			CircuitHopKeyResponse resp = new CircuitHopKeyResponse(keys.getPublic());
			byte[] respBytes = CipherUtils.serialize(resp);

			OnionMessage response = new OnionMessage(OnionMessage.MsgType.KEY_REPLY, respBytes);
			byte[] responseEncr = CipherUtils.serialize(CipherUtils.onionEncryptMessage(response, hopKeys));
			
			// send.
			try {
				sck.getOutputStream().write(responseEncr, 0, responseEncr.length);
				Common.log("[CircuitSwitch]: Response Sent.");
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}

		private void handleDataMessage(OnionMessage msg) {
			Common.log("[CircuitSwitch]: Data Message.");
			byte[] decryptedData = CipherUtils.applyCipher(msg.getData(), "AES/ECB/PKCS7Padding", Cipher.DECRYPT_MODE, key);	
			try {
				nextHop.getOutputStream().write(decryptedData, 0 , decryptedData.length);	
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
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
}
