/**
 * Augmented Reality Rubik Cube Wizard
 * 
 * Author: Steven P. Punte (aka Android Steve : android.steve@cl-sw.com)
 * Date:   April 25th 2015
 * 
 * Project Description:
 *   Android application developed on a commercial Smart Phone which, when run on a pair 
 *   of Smart Glasses, guides a user through the process of solving a Rubik Cube.
 *   
 * File Description:
 * 
 * License:
 * 
 *  GPL
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.ar.rubik;

import org.opencv.core.Point;

/**
 * Least Means Square
 * 
 * 
 * @author android.steve@cl-sw.com
 *
 */
public class LeastMeansSquare {

	
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
	
	public LeastMeansSquare(double x, double y, double alphaLatice, Point[][] errorVectorArray, double sigma, boolean valid) {
		this.origin = new Point(x, y);
		this.alphaLattice = alphaLatice;
		this.errorVectorArray = errorVectorArray;
		this.sigma = sigma;
		this.valid = valid;
	}
	
	
	
}