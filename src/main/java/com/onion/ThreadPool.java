package com.onion;

import java.utils.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.net.*;

public class ThreadPool {
    // thread pool used by the switch
    int poolSize;
    LinkedBlockingQueue<Socket> requests;
    ArrayList<Thread> threads;
    ServerSocket welcomeSocket;
    Mode mode;

    public enum Mode {
	SWITCH, CLIENT	
    }

    public ThreadPool(ServerSocket welcomeSocket, Mode mode) {
	this(7, welcomeSocket, task, mode);
    }

    public ThreadPool(int poolSize, ServerSocket welcomeSocket, Mode mode) {
	this.requests = new LinkedBlockingQueue<Socket>();
	this.threads = new ArrayList<Thread>();
	this.welcomeSocket = welcomeSocket;
	this.poolSize = poolSize;
	this.mode = mode;

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
	    if(mode == SWITCH) {
		threads.add(new Thread(new (requests)));
	    } else {
	    	threads.add(new Thread(new RequestWorker(requests)));
	    }
    }
}


}
