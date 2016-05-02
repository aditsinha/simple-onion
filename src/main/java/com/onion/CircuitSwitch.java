package com.onion;

import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

public class CircuitSwitch {
	// Connection Map
	SwitchThreadPool serviceThreads;
	ConcurrentHashMap<int, Connection> connMap;
	Config conf;

	CircuitSwitch(String configName){
		conf = new Config(configName);
		serviceThreads = new SwitchThreadPool(ServerSocket(Common.PORT));
		connMap = new ConcurrentHashMap<int, Connection>(8, (float)0.75, 8));
		// TODO start the thread pool
	}

	// represents a single connection
	private class Connection {
		public KeyPair keys;
		public Socket nextHop;

		Connection(KeyPair keys, Socket nextHop) {
			this.keys = keys;
			this.nextHop = nextHop;
		}
	}

	// thread pool used by the switch
	private class SwitchThreadPool {
	    int poolSize;
	    LinkedBlockingQueue<Socket> requests;
	    ArrayList<Thread> threads;
	    ServerSocket welcomeSocket;

	    public ThreadPool(ServerSocket welcomeSocket) {
	        this(7, welcomeSocket);
	    }

	    public ThreadPool(int poolSize, ServerSocket welcomeSocket) {
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
	            threads.add(new Thread(new RequestWorker(requests)));
	        }
	    }
	}


	private class RequestWorker implements Runnable {
	    LinkedBlockingQueue<Socket> requests;

	    public RequestWorker(LinkedBlockingQueue<Socket> requests) {
	        this.requests = requests;
	    }

	    public void run () {
	    	while(true) {
	    		Socket sck = requests.take();
		        OnionMessage msg = OnionMessage.unpack(sck);
		        switch(msg.getType()) {
		        	case OnionMessage.MsgType.DATA:
		        		handleDataMessage(msg);
		        		break;
		        	case OnionMessage.MsgType.HOP_REQUEST:
		        		handleHopRequestMessage(msg);
		        		break;
		        	case OnionMessage.MsgType.HOP_REPLY:
		        		handleCircuitEstablishmentMessage(msg);
		        		break;
		        	case OnionMessage.MsgType.POISON:
		        		handlePoisonMessage(msg);
		        		break;
		        }
	    	}
		}

		private void handleHopRequestMessage(OnionMessage msg) {
			CircuitHopRequestMessage chrm = (CircuitHopRequestMessage) CipherUtils.deserialize(msg.getData());
			if(chrm == null) {
				System.err.println("[CircuitSwitch]: handleHopRequestMessage: cannot deserialize.");
				System.exit(1);
			}

			Socket localSck = new Socket();
		}

		private void handleCircuitEstablishmentMessage(OnionMessage msg) {

		}

		private void handleDataMessage(OnionMessage msg) {

		}

		private void handlePoisonMessage(OnionMessage msg) {

		}
	}
}
}

 LinkedBlockingQueue<Socket> requests;

    public RequestWorker(LinkedBlockingQueue<Socket> requests) {
        this.requests = requests;
    }


    public void run () {
        Socket connectionSocket;
        while(true) {   
            try {
                // blocks until finds a socket to take.
                connectionSocket = requests.take();
                Request mReq = RequestParser.parse(connectionSocket);
                
                // handle errors
                if(mReq.getErrorMsg() != null) {
                    Responder.sendSpecialMessage(connectionSocket, mReq);
                    continue;
                }

                if (mReq.getUri().equals("load")) {
                    if(Server.load.acceptsConn()) {
                        mReq.setErrorMsg("OK");
                        mReq.setErrorCode(200);
                        Responder.sendSpecialMessage(connectionSocket, mReq);
                    } else {
                        mReq.setErrorMsg("Server Overloaded");
                        mReq.setErrorCode(503);
                        Responder.sendSpecialMessage(connectionSocket, mReq);
                    }
                } else {
                    FileManager.getFile(mReq);

                    // respond.
                    Responder.respond(connectionSocket, mReq);
                }
            } catch (Exception e) {
                // ignore.
                e.printStackTrace();
            }   
        }
    }
}