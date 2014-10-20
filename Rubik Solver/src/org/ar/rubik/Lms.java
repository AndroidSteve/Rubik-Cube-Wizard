package org.ar.rubik;

import org.opencv.core.Point;

/**
 * Least Means Square
 * 
 * 
 * @author stevep
 *
 */
public class Lms {

	
	// Actually, this will/should be center of corner tile will lowest (i.e. smallest) value of Y.
	public Point origin;
	
	// =+= Migrate this to Lattice ?
	public double alphaLattice;
	
	//
	public Point[][] errorVectorArray;
	
	// Sum of all errors (RMS)
	public double sigma;
	
	// True if results are mathematically valid.
	public boolean valid;
	
	public Lms(double x, double y, double alphaLatice, Point[][] errorVectorArray, double sigma, boolean valid) {
		this.origin = new Point(x, y);
		this.alphaLattice = alphaLatice;
		this.errorVectorArray = errorVectorArray;
		this.sigma = sigma;
		this.valid = valid;
	}
	
	
	
}