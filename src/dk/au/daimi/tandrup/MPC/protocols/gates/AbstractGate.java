package dk.au.daimi.tandrup.MPC.protocols.gates;

import java.io.Serializable;
import java.util.Collection;

import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;

public abstract class AbstractGate implements Comparable<AbstractGate>, Serializable {
	private int id;
	public AbstractGate(int id) {
		this.id = id;
	}
	
	public int getID() {
		return id;
	}

	public abstract FieldElement output();
	public abstract boolean isConfigured();
	public abstract boolean isReady();
	public abstract boolean isComputed();
	public abstract Collection<AbstractGate> ingressGates();
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + id;
	}

	public int compareTo(AbstractGate other) {
		int nameComparison = this.getClass().getName().compareTo(other.getClass().getName());
		if (nameComparison == 0) {
			if (this.getID() == other.getID())
				return 0;

			if (this.getID() < other.getID())
				return -1;

			return 1;		
		}
		
		return nameComparison;
	}
}
