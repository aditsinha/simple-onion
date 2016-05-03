package com.onion;

import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.net.*;
import java.security.*;
import javax.crypto.*;
import java.util.Collections;


public class CircuitSwitch {
	SwitchThreadPool serviceThreads;
	// connection map
	ConcurrentHashMap<Socket, Connection> connMap;
	Config conf;
	KeyPair keys;

	CircuitSwitch(String configName){
		keys = CipherUtils.generateRsaKeyPair();	
		conf = new Config(configName);
		try { 
			serviceThreads = new SwitchThreadPool(new ServerSocket(Common.PORT));
			connMap = new ConcurrentHashMap<Socket, Connection>(8, (float)0.75, 8);
			serviceThreads.start();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	// represents a single connection
	private class Connection {
		public Key key;
		public Socket nextHop;

		Connection(Key key, Socket nextHop) {
			this.key = key;
			this.nextHop = nextHop;
		}
	}

	private class SwitchThreadPool {
	    // thread pool used by the switch
	    int poolSize;
	    LinkedBlockingQueue<Socket> requests;
	    ArrayList<Thread> threads;
	    ServerSocket welcomeSocket;

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
				}
			}
		}

		private void handleHopRequestMessage(OnionMessage msg) {
			byte[] decryptedData = CipherUtils.applyCipher(msg.getData(), "RSA/ECB/PKCSPadding", Cipher.DECRYPT_MODE, keys.getPrivate());
			CircuitHopRequestMessage chrm = (CircuitHopRequestMessage) CipherUtils.deserialize(msg.getData());
			if(chrm == null) {
				Common.log("[CircuitSwitch]: handleHopRequestMessage: cannot deserialize.");
				System.exit(1);
			}

			try {
				Socket nextSwitch = new Socket();
				nextSwitch.connect(new InetSocketAddress(conf.getSwitch(chrm.getNextNode()), Common.PORT));
				Connection cnt = new Connection(chrm.getSecretKey(), nextSwitch);
				connMap.put(sck, cnt);
				Connection revCnt = new Connection(chrm.getSecretKey(), sck);
				connMap.put(nextSwitch, revCnt);			
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
			// TODO : serialize and encrypt. call onionEncryptMessage with the keys from circuit hope request. 
			// will be changed. Send response 
		}		

		private void handleKeyRequestMessage(OnionMessage msg) {
			Common.log("[CircuitSwitch]: Key Request Message.");
			CircuitHopKeyRequest req = (CircuitHopKeyRequest) CipherUtils.deserialize(msg.getData());
			
			// reverse the key list.
			ArrayList<Key> hopKeys = (ArrayList<Key>)req.getKeys();
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
			byte[] decryptedData = CipherUtils.applyCipher(msg.getData(), "AES/ECB/PKCS7Padding", Cipher.DECRYPT_MODE, connMap.get(sck).key);
			Socket nextHop = connMap.get(sck).nextHop;	
			try {
				nextHop.getOutputStream().write(decryptedData, 0 , decryptedData.length);	
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}

		private void handlePoisonMessage(OnionMessage msg) {
			handleDataMessage(msg);
			// now delete from your hashtable.
			try {
				Socket nextHop = connMap.get(sck).nextHop;
				connMap.remove(sck);
				connMap.remove(nextHop);
				sck.close();
				sck = null;
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
}
