package dk.au.daimi.tandrup.MPC.protocols.messages;

import java.io.Serializable;
import java.util.Map;

import dk.au.daimi.tandrup.MPC.math.FieldPolynomial1D;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.protocols.VssCoordinate;

public class VssCoordinateReply implements Serializable {
	private static final long serialVersionUID = 1L;
	private Map<VssCoordinate, FieldElement> values;
	private FieldPolynomial1D f, g;

	public VssCoordinateReply(Map<VssCoordinate, FieldElement> values) {
		super();
		this.values = values;
		this.f = null;
		this.g = null;
	}
	
	public VssCoordinateReply(Map<VssCoordinate, FieldElement> values, FieldPolynomial1D f, FieldPolynomial1D g) {
		super();
		this.values = values;
		this.f = f;
		this.g = g;
	}

	public Map<VssCoordinate, FieldElement> getValues() {
		return values;
	}

	public FieldPolynomial1D getF() {
		return f;
	}

	public FieldPolynomial1D getG() {
		return g;
	}

	@Override
	public String toString() {
		String strCoors = null;
		
		for (VssCoordinate coor : values.keySet()) {
			if (strCoors != null)
				strCoors += ", " + coor + " = " + values.get(coor);
			else
				strCoors = coor + " = " + values.get(coor);
		}
		
		return "VSS Coodinate Reply: " + strCoors + " (" + super.toString() + ")";
	}
}
