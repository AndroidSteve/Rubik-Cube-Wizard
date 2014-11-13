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
 *   This class is provided a set of Rhombi and attempts, if possible, to deduce 
 *   a Rubik Face and associated feature parameters.
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

import java.util.List;

/**
 * @author android.steve@testlens.com
 *
 */
public class RubikFace2 {
	
	public enum FaceRecognitionStatusEnum {
		UNKNOWN,
		INSUFFICIENT,            // Insufficient Provided Rhombi to attempt solution
		BAD_METRICS,             // Metric Calculation did not produce reasonable results
		INCOMPLETE,              // Rhombi did not converge to proper solution
		INADEQUATE,              // We require at least one Rhombus in each row and column
		BLOCKED,                 // Attempt to improve Rhombi layout in face was blocked: incorrect move direction reported
		INVALID_MATH,            // LMS algorithm result in invalid math.
		UNSTABLE,                // Last Tile move resulted in a increase in the overall error (LMS).
		SOLVED }                 // Full and proper solution obtained.
	
	public FaceRecognitionStatusEnum faceRecognitionStatus = FaceRecognitionStatusEnum.UNKNOWN;

	/**
	 * Process Rhombuses
	 * 
	 * Given the Rhombus list, attempt to recognize the grid dimensions and orientation,
	 * and full tile color set.  Return true if a Rubik Face was properly recognized.
	 * 
	 * @param rhombusList
	 */
    public boolean processRhombuses(List<Rhombus> rhombusList) {
		
    	
    	return false;
    }

}
