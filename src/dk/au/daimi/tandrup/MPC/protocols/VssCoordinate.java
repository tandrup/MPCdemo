/**
 * 
 */
package dk.au.daimi.tandrup.MPC.protocols;

import java.io.Serializable;

public class VssCoordinate implements Serializable {
	private static final long serialVersionUID = 1L;
	private int i, j;
	public VssCoordinate(int i, int j) {
		this.i = i;
		this.j = j;
	}
	public int getI() {
		return i;
	}
	public int getJ() {
		return j;
	}
	@Override
	public String toString() {
		return "(" + i + ", " + j + ")";
	}
	
	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + i;
		result = PRIME * result + j;
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
		final VssCoordinate other = (VssCoordinate) obj;
		if (i != other.i)
			return false;
		if (j != other.j)
			return false;
		return true;
	}	
}