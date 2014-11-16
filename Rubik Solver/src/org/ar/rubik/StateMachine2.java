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
public class StateMachine2 {
	
	public enum ControllerStateEnum { 
		START,     // Ready
		GOT_IT,    // A Cube Face has been recognized and captured.
		ROTATE,    // Request user to rotate Cube.
		SEARCHING, // Attempting to lock onto new Cube Face.
		COMPLETE,  // All six faces have been captured, and we seem to have valid color.
		BAD_COLORS,// All six faces have been captured, but we do not have properly nine tiles of each color.
		VERIFIED,  // Two Phase solution has verified that the cube tile/colors/positions are a valid cube.
		WAITING,   // Waiting for TwoPhase Prune Tree generation to complete.
		INCORRECT, // Two Phase solution has analyzed the cube and found it to be invalid.
		SOLVED,    // Two Phase solution has analyzed the cube and found a solution.
		DO_MOVE,   // Inform user to perform a face rotation
		WAITING_FOR_MOVE_COMPLETE, // Wait for face rotation to complete
		DONE       // Cube should be completely physically solved.
		};
	
	private ControllerStateEnum controllerState = ControllerStateEnum.START;
	
	private StateModel2 stateModel2;

	/**
	 * @param stateModel2
	 */
    public StateMachine2(StateModel2 stateModel2) {
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
