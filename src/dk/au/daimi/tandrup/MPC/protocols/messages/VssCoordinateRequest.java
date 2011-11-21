package dk.au.daimi.tandrup.MPC.protocols.messages;

import java.io.Serializable;
import java.util.Set;

import dk.au.daimi.tandrup.MPC.protocols.VssCoordinate;

public class VssCoordinateRequest implements Serializable {
	private static final long serialVersionUID = 1L;
	private Set<VssCoordinate> vssCoordinates;
	private boolean sendPolys;

	public VssCoordinateRequest(Set<VssCoordinate> vssCoordinates, boolean sendPolynomials) {
		this.vssCoordinates = vssCoordinates;
		this.sendPolys = sendPolynomials;
	}
	
	public Set<VssCoordinate> getVssCoordinates() {
		return vssCoordinates;
	}

	public boolean sendPolynomials() {
		return sendPolys;
	}

	@Override
	public String toString() {
		String strCoors = null;
		
		if (vssCoordinates.isEmpty()) {
			strCoors = "Empty";
		} else {
			for (VssCoordinate coor : vssCoordinates) {
				if (strCoors != null)
					strCoors += ", " + coor.toString();
				else
					strCoors = coor.toString();
			}
		}
		
		return "VSS Coodinate Request: " + strCoors + ")";
	}
}
