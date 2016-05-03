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

    public List<Key> keyList;

    public boolean isConnectionEstablished;

    private Key hopPublicKey, hopSymmetricKey;

    private Config c;

    public CircuitEstablishment(List<Integer> hops, int destination, Config c) {
		this.hops = hops;
		isConnectionEstablished = false;
		keyList = new ArrayList<>();
	    this.c = c;
    }

    public byte[] processMessage(OnionMessage msg) {
        OnionMessage resp = null;
	switch (msg.getType()) {
	case HOP_REPLY:
	    // established connection.  make the next hop
	    keyList.add(hopSymmetricKey);
	    hopPublicKey = null;
	    hopSymmetricKey = null;

            if (keyList.size() == hops.size()) {
                isConnectionEstablished = true;
            }

	    resp = new OnionMessage(OnionMessage.MsgType.KEY_REQUEST,
                                    CipherUtils.serialize(new CircuitHopKeyRequest(keyList)));
            if (!isConnectionEstablished) {
                Common.log("[CircuitEstablishment]: KEY_REQUEST to switch: " + c.getSwitch(hops.get(keyList.size())).getHostName());
            } else {
                Common.log("[CircuitEstablishment]: KEY_REQUEST to endpoint: " + c.getEndpoint(destination).getHostName());
            }

        break;
	case KEY_REPLY:
	    CircuitHopKeyReply reply = (CircuitHopKeyReply) CipherUtils.deserialize(msg.getData());
	    hopPublicKey = reply.getKey();
	    hopSymmetricKey = new SecretKeySpec(CipherUtils.getRandomBytes(16), "AES");

            byte[] keyData = null;
            try {
                keyData = CipherUtils.getCipher(CipherUtils.ASYM_ALGORITHM, Cipher.WRAP_MODE, hopPublicKey)
                    .wrap(hopSymmetricKey);
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
                System.exit(1);
            }

	    int nextHop = (keyList.size() < hops.size() - 1) ? hops.get(keyList.size() + 1) : destination;

        CircuitHopRequestMessage.Payload payload = new CircuitHopRequestMessage.Payload(nextHop, keyList);

        byte[] encryptedPayload = CipherUtils.applyCipher(CipherUtils.serialize(payload), CipherUtils.SYM_ALGORITHM, Cipher.ENCRYPT_MODE, hopSymmetricKey);

        CircuitHopRequestMessage chrm = new CircuitHopRequestMessage(keyData, encryptedPayload);

        Common.log("[CircuitEstablishment]: HOP_REQUEST to switch: " + c.getSwitch(hops.get(keyList.size())).getHostName());

        resp = new OnionMessage(OnionMessage.MsgType.HOP_REQUEST, CipherUtils.serialize(chrm));
        break;
	default:
	    System.out.println("ERROR UNEXPECTED MESSAGE TYPE: " + msg.getType());
            System.exit(1);
	}
        return CipherUtils.onionEncryptMessage(resp, keyList);
    }

    public byte[] getFirstMessage() {
        Common.log("[CircuitEstablishment]: KEY_REQUEST to switch: " + c.getSwitch(hops.get(0)).getHostName());
	return new OnionMessage(OnionMessage.MsgType.KEY_REQUEST, CipherUtils.serialize(new CircuitHopKeyRequest(keyList))).pack();
    }
}
