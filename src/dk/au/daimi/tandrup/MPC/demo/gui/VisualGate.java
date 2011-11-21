package dk.au.daimi.tandrup.MPC.demo.gui;

import java.io.Serializable;
import java.util.SortedSet;
import java.util.TreeSet;

public class VisualGate implements Serializable, Comparable<VisualGate> {
	private static final long serialVersionUID = 1L;

	public enum Type {
		MULTIPLICATION ("*"),
		ADDITION ("+"),
		INPUT ("Input"),
		OUTPUT ("Output"),
		RANDOM ("Random");

		private final String label;
		Type (String label) {
			this.label = label;
		}

		public String getLabel() {
			return label;
		}
	}
	
	private final Type type;
	
	private final long ident;
	
	private SortedSet<VisualGate> ingressGates = new TreeSet<VisualGate>();
	private SortedSet<VisualGate> egressGates = new TreeSet<VisualGate>();

	public String getGraphNodeName() { return type.toString() + "_" + ident; }
	public SortedSet<VisualGate> getIngressGates() { return ingressGates; }
	public SortedSet<VisualGate> getEgressGates() { return egressGates; }
	public Type getType() { return type; }

	public void addIngressGate(VisualGate ingressGate) { 
		this.ingressGates.add(ingressGate);
		ingressGate.egressGates.add(this);
	}
	public void removeIngressGate(VisualGate ingressGate) { 
		this.ingressGates.remove(ingressGate); 
		ingressGate.egressGates.remove(this);
	}

	public VisualGate(Type type, long ident) {
		this.type = type;
		this.ident = ident;
	}
	
	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + (int) (ident ^ (ident >>> 32));
		result = PRIME * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final VisualGate other = (VisualGate) obj;
		if (ident != other.ident)
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}
	
	public int compareTo(VisualGate other) {
		if (this.ident < other.ident)
			return -1;

		if (this.ident > other.ident)
			return 1;
		
		return this.type.compareTo(other.type);
	}
}
