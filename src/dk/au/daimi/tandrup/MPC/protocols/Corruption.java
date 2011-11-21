package dk.au.daimi.tandrup.MPC.protocols;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import dk.au.daimi.tandrup.MPC.net.Participant;

public class Corruption {
	private Set<Participant> corrupt = new HashSet<Participant>();

	public void add(Participant p) {
		corrupt.add(p);
	}
	

	public Collection<Participant> getCorrupt() {
		return Collections.unmodifiableCollection(corrupt);
	}

	public Collection<Participant> getHonest(Collection<? extends Participant> participants) {
		Set<Participant> parts = new HashSet<Participant>(participants);
		parts.removeAll(corrupt);
		return parts;
	}
}
