package dk.au.daimi.tandrup.MPC.math.fields.polynomial.galois;

import dk.au.daimi.tandrup.MPC.math.fields.polynomial.galois.jean.ExtendedGaloisField;
import dk.au.daimi.tandrup.MPC.math.fields.polynomial.galois.jean.GaloisException;
import dk.au.daimi.tandrup.MPC.math.fields.polynomial.galois.jean.GaloisField;

public class ExtGF extends GF {
	public ExtGF(int p, int k) throws GaloisException {
		super(new ExtendedGaloisField(new GaloisField(p), 'a', k));
	}
}
