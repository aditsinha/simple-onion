package com.onion;

import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.net.*;
import java.security.*;
import javax.crypto.*;


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

		public SwitchWorker(LinkedBlockingQueue<Socket> requests) {
			this.requests = requests;
		}

		public void run () {
			while(true) {
				try {
					Socket sck = requests.take();
					OnionMessage msg = OnionMessage.unpack(sck);
					switch(msg.getType()) {
						case DATA:
							handleDataMessage(msg, sck);
							break;
						case HOP_REQUEST:
							handleHopRequestMessage(msg, sck);
							break;
						case POISON:
							handlePoisonMessage(msg, sck);
							break;
						case KEY_REQUEST:
							handleKeyRequestMessage(msg, sck);
							break;	        
					}
				} catch (Exception e) {
					// this is bad
					e.printStackTrace();
				}
			}
		}

		private void handleHopRequestMessage(OnionMessage msg, Socket sck) {
			byte[] decryptedData = CipherUtils.applyCipher(msg.getData(), "RSA/ECB/PKCSPadding", Cipher.DECRYPT_MODE, keys.getPrivate());
			CircuitHopRequestMessage chrm = (CircuitHopRequestMessage) CipherUtils.deserialize(msg.getData());
			if(chrm == null) {
				System.out.println("[CircuitSwitch]: handleHopRequestMessage: cannot deserialize.");
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

		private void handleKeyRequestMessage(OnionMessage msg, Socket sck) {
			CircuitHopKeyRequest req = (CircuitHopKeyRequest) CipherUtils.deserialize(msg.getData());
			CircuitHopKeyResponse resp = new CircuitHopKeyResponse(keys.getPublic());
			// TODO: onionEncryptMessage this
			// then actually serialize and encrypt that.	
			// use sck to return that.
		}

		private void handleDataMessage(OnionMessage msg, Socket sck) {
			byte[] decryptedData = CipherUtils.applyCipher(msg.getData(), "AES/ECB/PKCS7Padding", Cipher.DECRYPT_MODE, connMap.get(sck).key);
			Socket nextHop = connMap.get(sck).nextHop;	
			try {
				nextHop.getOutputStream().write(decryptedData, 0 , decryptedData.length);	
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}

		private void handlePoisonMessage(OnionMessage msg, Socket sck) {
			handleDataMessage(msg, sck);
			// now delete from your hashtable.
			Socket nextHop = connMap.get(sck).nextHop;
			connMap.remove(sck);
			connMap.remove(nextHop);
			}
	}
}
