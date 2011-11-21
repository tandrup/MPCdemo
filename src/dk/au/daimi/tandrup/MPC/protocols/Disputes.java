package dk.au.daimi.tandrup.MPC.protocols;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;

import dk.au.daimi.tandrup.MPC.net.Participant;

public class Disputes {
	private Set<Pair> disputes = new HashSet<Pair>();
	private Set<Participant> disputeParts = new HashSet<Participant>();
	
	public void add(Participant x, Participant y) {
		disputes.add(new Pair(x,y));
		disputeParts.add(x);
		disputeParts.add(y);
	}

	public boolean contains(Participant x) {
		return disputeParts.contains(x);
	}
	
	public int size() {
		return disputes.size();
	}
	
	public Collection<Participant> getParticipants() {
		return Collections.unmodifiableCollection(disputeParts);
	}

	@Override
	public String toString() {
		return "Disputes " + disputes.toString();
	}

	private class Pair {
		private Participant x, y;

		public Pair(Participant x, Participant y) {
			super();
			this.x = x;
			this.y = y;
		}

		
		@Override
		public int hashCode() {
			return x.hashCode() + y.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			
			if (obj instanceof Disputes.Pair) {
				Pair other = (Pair)obj;
				
				return (this.x.equals(other.x) && this.y.equals(other.y)) ||
				       (this.x.equals(other.y) && this.y.equals(other.x));
			}
			
			return false;
		}

		@Override
		public String toString() {
			return "(" + x.toString() + ", " + y.toString() + ")";
		}
	}
}
