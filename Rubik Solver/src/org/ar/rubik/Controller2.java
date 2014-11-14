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
 *   This class interprets the recognized Rubik Faces and determines how primary
 *   application state should change.
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
public class Controller2 {
	
	private StateModel2 stateModel2;

	/**
	 * @param stateModel2
	 */
    public Controller2(StateModel2 stateModel2) {
    	this.stateModel2 = stateModel2;
    }

	/**
	 * Process Rubik Face
	 * 
	 * Assuming that a Rubik Face is recognized, then update the Application State Machine.
	 * 
	 * @param rubikFace2
	 */
    public void processFace(RubikFace2 rubikFace2) {
    	
    		this.stateModel2.activeRubikFace = rubikFace2;
    }

}
