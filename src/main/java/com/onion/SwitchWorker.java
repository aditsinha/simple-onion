package com.onion.*;

public class SwitchWorker implements Runnable {
	LinkedBlockingQueue<Socket> requests;	

	public SwitchWorker(LinkedBlockingQueue<Socket> requests) {
		this.requests = requests;
	}

	public void run () {
		while(true) {
			Socket sck = requests.take();
			OnionMessage msg = OnionMessage.unpack(sck);
			switch(msg.getType()) {
		        	case OnionMessage.MsgType.DATA:
		        		handleDataMessage(msg, sck);
		        		break;
		        	case OnionMessage.MsgType.HOP_REQUEST:
		        		handleHopRequestMessage(msg, sck);
		        		break;
		        	case OnionMessage.MsgType.POISON:
		        		handlePoisonMessage(msg, sck);
		        		break;
				case OnionMessage.MsgType.KEY_REQUEST:
					handleKeyRequestMessage(msg, sck);
					break;	        
			}
	    	}
	}

	private void handleHopRequestMessage(OnionMessage msg, Socket sck) {
		byte[] decryptedData = CipherUtils.applyCipher(msg.data(), "RSA/ECB/PKCSPadding", Cipher.DECRYPT_MODE, keys.getPrivate());
		CircuitHopRequestMessage chrm = (CircuitHopRequestMessage) CipherUtils.deserialize(msg.getData());
		if(chrm == null) {
			System.err.println("[CircuitSwitch]: handleHopRequestMessage: cannot deserialize.");
			System.exit(1);
		}

		Socket nextSwitch = new Socket();
		nextSwitch.connect(config.getSwitch(chrm.getNextNode()));
		Connection cnt = new Connection(chrm.getKey(), nextSwitch);
		connMap.put(sck, cnt);
		Connection revCnt = new Connection(chrm.getKey(), sck);
		connMap.put(nextSwitch, revCnt);			
	
		// TODO : serialize and encrypt. call onionEncryptMessage with the keys from circuit hope request. 
		// will be changed. Send response 
	}		

	private void handleKeyRequestMessage(OnionMessage msg, Socket sck) {
		CircuitHopKeyRequest req = (CircuitHopKeyRequest) CipherUtils.deserialize(msg.getData());
		CircuitHopKeyResponse resp = new CircuitHopeKeyResponse(keys.getPublic());
		// TODO: onionEncryptMessage this
		// then actually serialize and encrypt that.	
		// use sck to return that.
	}

	private void handleDataMessage(OnionMessage msg, Socket sck) {
		byte[] decryptedData = CipherUtils.applyCipher(msg.data(), "AES/ECB/PKCS7Padding", Cipher.DECRYPT_MODE, connMap.get(sck).key);
		Socket nextHop = connMap.get(sck).nextHop;	
		nextHop.getOutputStream().write(decryptedData, 0 , decryptedData.length);
	}

	private void handlePoisonMessage(OnionMessage msg, Socket sck) {
		handledataMessage(msg, sck);
		// now delete from your hashtable.
		Socket nexthop = connMap.get(sck).nextHop;
		connMap.remove(sck);
		connMap.remove(nextHop);
	}
}
