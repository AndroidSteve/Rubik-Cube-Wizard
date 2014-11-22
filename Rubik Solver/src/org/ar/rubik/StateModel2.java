/**
 * Augmented Reality Rubik Cube Solver
 * 
 * Author: Steven P. Punte (aka Android Steve)
 * Date:   Nov 1st 2014
 * 
 * Project Description:
 *   Android application developed on a commercial Smart Phone which, when run on a pair 
 *   of Smart Glasses, guides a user through the process of solving a Rubik Cube.
 *   
 * File Description:
 *   Represents state of Rubik Cube.
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

/**
 * @author android.steve@testlens.com
 *
 */
public class StateModel2 {
	
	// Rubik Face of latest processed frame: may or may not be any of the six state objects.
	public RubikFace2 activeRubikFace;
	
	/*
	 * This is "Rubik Cube State" or "Rubik Cube Model" in model-veiw-controller vernacular.
	 */
	public RubikFace2 upRubikFace;
	public RubikFace2 downRubikFace;
	public RubikFace2 leftRubikFace;
	public RubikFace2 rightRubikFace;
	public RubikFace2 frontRubikFace;
	public RubikFace2 backRubikFace;
	
	
	// We assume that faces will be explored in a particular sequence.
	private int adoptFaceCount = 0;
	
	/**
	 * Adopt Face
	 * 
	 * Adopt faces in a particular sequence dictated by the user directed instruction on
	 * how to rotate the code during the exploration phase.
	 * 
	 * @param candidateRubikFace2
	 */
    public void adopt(RubikFace2 candidateRubikFace2) {
	    
    	switch(adoptFaceCount) {
    	
    	case 0:
    		upRubikFace = candidateRubikFace2;
    		break;
    	case 1:
    		rightRubikFace = candidateRubikFace2;
    		break;
    	case 2:
    		frontRubikFace = candidateRubikFace2;
    		break;
    	case 3:
    		downRubikFace = candidateRubikFace2;
    		break;
    	case 4:
    		leftRubikFace = candidateRubikFace2;
    		break;
    	case 5:
    		backRubikFace = candidateRubikFace2;
    		break;
    		
    		default:
    	}
    	
    	adoptFaceCount++;
    	
    }
	
	// Rubik Solution 

}
