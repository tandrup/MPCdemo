package dk.au.daimi.tandrup.MPC.protocols;

import java.io.Serializable;

import dk.au.daimi.tandrup.MPC.protocols.gates.AffineGate;
import dk.au.daimi.tandrup.MPC.protocols.gates.InputGate;
import dk.au.daimi.tandrup.MPC.protocols.gates.MultGate;
import dk.au.daimi.tandrup.MPC.protocols.gates.OutputGate;
import dk.au.daimi.tandrup.MPC.protocols.gates.RandomGate;

public class PreProcessResult implements Serializable {
	private static final long serialVersionUID = 1L;

	private RandomGate[] randomGates;
	private InputGate[] inputGates;
	private MultGate[] multGates;
	private AffineGate[] affineGates;
	private OutputGate[] outputGates;
	
	public PreProcessResult(int rCount, int iCount, int mCount, int aCount, int oCount) {
		randomGates = new RandomGate[rCount];
		inputGates = new InputGate[iCount];
		multGates = new MultGate[mCount];
		affineGates = new AffineGate[aCount];
		outputGates = new OutputGate[oCount];
	}
	
	public int getInputGateCount() {
		return inputGates.length;
	}
	public InputGate getInputGate(int i) {
		return inputGates[i];
	}
	void setInputGate(InputGate g) {
		inputGates[g.getID()] = g;
	}

	public int getMultGateCount() {
		return multGates.length;
	}
	public MultGate getMultGate(int i) {
		return multGates[i];
	}
	void setMultGate(MultGate g) {
		multGates[g.getID()] = g;
	}

	public int getRandomGateCount() {
		return randomGates.length;
	}
	public RandomGate getRandomGate(int i) {
		return randomGates[i];
	}
	void setRandomGate(RandomGate g) {
		randomGates[g.getID()] = g;
	}
	
	public int getAffineGateCount() {
		return affineGates.length;
	}
	public AffineGate getAffineGate(int i) {
		return affineGates[i];
	}
	void setAffineGate(AffineGate g) {
		affineGates[g.getID()] = g;
	}

	public int getOutputGateCount() {
		return outputGates.length;
	}
	public OutputGate getOutputGate(int i) {
		return outputGates[i];
	}
	void setOutputGate(OutputGate g) {
		outputGates[g.getID()] = g;
	}
}
