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
	
	// Rubik Solution 

}
