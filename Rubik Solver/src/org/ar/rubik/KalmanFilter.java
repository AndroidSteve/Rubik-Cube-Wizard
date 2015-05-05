/**
 * Augmented Reality Rubik Cube Solver
 * 
 * Author: Steven P. Punte (aka Android Steve : android.steve@cl-sw.com)
 * Date:   April 25th 2015
 * 
 * Project Description:
 *   Android application developed on a commercial Smart Phone which, when run on a pair 
 *   of Smart Glasses, guides a user through the process of solving a Rubik Cube.
 *   
 * File Description:
 *   The 3D Model is maintained with a Kalman Filter using OpenCV Pose estimates
 *   and measurement inputs (an also half of state variables).
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
 * Class Kalman Filter
 * 
 * This Kalman Filter is constructed with twelve state variables: three are 
 * position, three are orientation, and six more are the first derivative
 * of these values: i.e., velocity and angular rotation rate.
 * 
 * A new Kalman Filter object is created when Gesture Recognition State
 * transitions to NEW_STABLE.  The current position and orientation are 
 * accepted into state; all first derivative state variables are set to
 * zero.
 * 
 * Upon the second Pose measurement, position and orientation are adopted,
 * and the first derivative state variables are set accordingly.
 * 
 * Thereafter, the Kalman Filter is operated in the normal manner.  
 * 
 * Some key aspects:
 * o  Measurement time intervals are not necessarily uniform, but close.
 * o  Rendering requests will be asynchronous and typically at a higher 
 *    frame rate: ~60 Hz.
 * o  Start up sequence as described above.
 * 
 * 
 * @author android.steve@cl-sw.com
 *
 */
public class KalmanFilter {
	
	public enum STATE {
		X_POS,
		Y_POS,
		Z_POS,
		X_POS_VELOCITY,
		Y_POS_VELOCITY,
		Z_POS_VELOCITY,
		X_AXIS_ROTATION,
		Y_AXIS_ROTATION,
		Z_AXIS_ROTATION,
		X_AXIS_RATE_OF_ROTATION,
		Y_AXIS_RATE_OF_ROTATION,
		Z_AXIS_RATE_OF_ROTATION
	};
	
	public float[] state = new float[12];
	
	private StateModel stateModel;

	public KalmanFilter(StateModel stateModel) {
		this.stateModel = stateModel;
	}

	public void measurementUpdate() {
		// TODO Auto-generated method stub
		
	}

}
