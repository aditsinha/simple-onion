package com.onion;

import java.io.*;
import java.util.*;

public class CircuitEstablishmentMessage implements Serializable {
    public CircuitEstablishmentMessage(List<HopSpec> hops) {
	this.hops = hops;
	    
    }

    private List<HopSpec> hops;

    public List<HopSpec> getHops() {
	return hops;
	    
    }
    
}
