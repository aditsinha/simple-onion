package com.onion;

import java.util.*;
import java.io.*;
import java.security.*;

import javax.crypto.*;
import javax.crypto.spec.*;

import org.bouncycastle.util.io.pem.*;
import org.bouncycastle.crypto.*;

public class CircuitEstablishment {

    private List<Integer> hops;
    private int destination;

    private HopSpec outstandingHop;
    public List<HopSpec> establishedHops;

    private boolean isConnectionEstablished;

    public CircuitEstablishment(List<Integer> hops, int destination) {
	this.hops = hops;
	isConnectionEstablished = false;
	establishedHops = new ArrayList<>(hops.size());
	    
    }

    public byte[] encryptAsymmetric(byte[] msg, PublicKey key) {
	Cipher cipher;
	try {
	    cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");
	    cipher.init(Cipher.ENCRYPT_MODE, key);
	} catch (GeneralSecurityException e) {
	    e.printStackTrace();
	    return null;
	}

	return CipherUtils.applyCipher(msg, cipher);
    }

    public void processMessage(byte[] msg) {
	CircuitHopReplyMessage reply;
	// expect to receive a CircuitHopReplyMessage
	try {
	    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(msg));
	    reply = (CircuitHopReplyMessage) ois.readObject();
	            
	} catch (IOException | ClassNotFoundException e) {
	    // this is bad
	    e.printStackTrace();
	    return;
	}

	outstandingHop.setConnectionId(reply.getHopConnectionId());
	establishedHops.add(outstandingHop);
	outstandingHop = null;
	    
    }

    public byte[] getNextMessage() {
	Serializable msg = null;

	if (establishedHops.size() < hops.size()) {
	    // haven't established all of the hops yet
	    if (outstandingHop == null) {
		// ready to do another handshake
		int nextHopNode = (establishedHops.size() < hops.size() - 1) ? hops.get(establishedHops.size() + 1) : destination;
		byte[] keyData = CipherUtils.getRandomBytes(16);
		outstandingHop = new HopSpec();
		outstandingHop.setKey(new SecretKeySpec(keyData, "AES"));

		msg = new CircuitHopRequestMessage(nextHopNode, outstandingHop.getKey());
		            
	    }

	    // else we aren't ready to do another handshake
	            
	} else if (!isConnectionEstablished) {
	    // need to send the entire chain to the other end.  send it in reverse though
	    List<HopSpec> reversedCircuit = new ArrayList<>(establishedHops);
	    Collections.reverse(reversedCircuit);
	    isConnectionEstablished = true;
	    msg = new CircuitEstablishmentMessage(reversedCircuit);
	            
	} else {
	    assert false;
	            
	}

	if (msg == null)
	    return null;

	byte[] msgData = CipherUtils.serialize(msg);
	return CipherUtils.onionEncryptMessage(msgData, establishedHops);
	    
    }
    
}
