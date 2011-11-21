package dk.au.daimi.tandrup.MPC.demo.gui;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import att.grappa.Node;

public class VisualGateContainer implements Serializable {
	private static final long serialVersionUID = 1L;

	private final SortedSet<VisualGate> iGates = new TreeSet<VisualGate>();
	private final SortedSet<VisualGate> oGates = new TreeSet<VisualGate>();
	private final SortedSet<VisualGate> aGates = new TreeSet<VisualGate>();
	private final SortedSet<VisualGate> mGates = new TreeSet<VisualGate>();
	private final SortedSet<VisualGate> rGates = new TreeSet<VisualGate>();
	
	private transient Map<String, VisualGate> node2gate = new HashMap<String, VisualGate>();

	private final Map<VisualGate, String> inputProviders = new HashMap<VisualGate, String>();

	private long masterIdent = 0;
	
	public synchronized long generateIdent() {
		return masterIdent++;
	}

	public SortedSet<VisualGate> getAllGates() {
		SortedSet<VisualGate> allGates = new TreeSet<VisualGate>();
		
		allGates.addAll(iGates);
		allGates.addAll(oGates);
		allGates.addAll(aGates);
		allGates.addAll(mGates);
		allGates.addAll(rGates);
		
		return allGates;
	}
	
	public SortedSet<VisualGate> getAGates() {
		return aGates;
	}
	public SortedSet<VisualGate> getIGates() {
		return iGates;
	}
	public SortedSet<VisualGate> getMGates() {
		return mGates;
	}
	public SortedSet<VisualGate> getOGates() {
		return oGates;
	}
	public SortedSet<VisualGate> getRGates() {
		return rGates;
	}

	private void genNode2Gate() {
		if (node2gate == null)
			node2gate = new HashMap<String, VisualGate>();
	}
	
	public VisualGate getGate(Node node) {
		genNode2Gate();
		VisualGate gate = node2gate.get(node.getName());
		if (gate == null)
			System.out.println("Could not find gate for " + node.getName() + "\nnode2gate: " + node2gate);
		return gate;
	}

	public boolean contains(Node node) {
		genNode2Gate();
		return node2gate.containsKey(node.getName());
	}

	public void addGate(VisualGate gate) {
		switch (gate.getType()) {
		case INPUT:
			iGates.add(gate);
			break;
		case OUTPUT:
			oGates.add(gate);
			break;
		case ADDITION:
			aGates.add(gate);
			break;
		case MULTIPLICATION:
			mGates.add(gate);
			break;
		case RANDOM:
			rGates.add(gate);
			break;
		}
	}

	public void addNode(Node node, VisualGate gate) {
		genNode2Gate();
		
		node2gate.put(node.getName(), gate);
	}
	
	public void removeGate(VisualGate gate) {
		genNode2Gate();
		
		iGates.remove(gate);
		oGates.remove(gate);
		aGates.remove(gate);
		mGates.remove(gate);
		rGates.remove(gate);
		
		node2gate.remove(gate.getGraphNodeName());
		
		for (VisualGate otherGate : getAllGates()) {
			otherGate.removeIngressGate(gate);
		}
	}
	
	public int getGateNo(VisualGate gate) {
		SortedSet<VisualGate> gates = null;
		switch (gate.getType()) {
		case INPUT:
			gates = iGates;
			break;
		case OUTPUT:
			gates = oGates;
			break;
		case ADDITION:
			gates = aGates;
			break;
		case MULTIPLICATION:
			gates = mGates;
			break;
		case RANDOM:
			gates = rGates;
			break;
		}

		int i = 0;
		for (VisualGate gate2 : gates) {
			if (gate2.equals(gate))
				return i;
			i++;
		}
		
		return -1;
	}
	
	public void setInputProvider(VisualGate gate, String inputProvider) {
		inputProviders.put(gate, inputProvider);
	}

	public String getInputProvider(VisualGate gate) {
		return inputProviders.get(gate);
	}
}
