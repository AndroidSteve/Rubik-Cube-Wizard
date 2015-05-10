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
 * Initial design:
 * o  Is simply a fixed Kalman Filter Gain matrix.
 * o  Also records timestamp of last measurement update.
 * o  Forward, Kalman Gain, and Output are time-variant matrices, but linear to time.
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
		X_AXIS_ANGULAR_ROTATION,
		Y_AXIS_ANGULAR_ROTATION,
		Z_AXIS_ANGULAR_ROTATION
	};
	

	private StateModel stateModel;
	
	// Timestamp reference of state
	private long t;

	// State Matrix
	private float[] x = new float[12];
	
	// Feed Forward Matrix
	private final float[][] a = { { 0.0f } };

	// State to Output Matrix
	private final float[][] c = { { 0.0f } };

	// Kalman Gain Matrix
	private final float[][] k = { { 0.0f } };
	
	
	/**
	 * Kalman Filter Constructor
	 * 
	 * Begin a new Kalman FIlter (actually, really new and fresh state variables).
	 * Initialze all state variables.
	 * 
	 * @param stateModel
	 */
	public KalmanFilter(StateModel stateModel) {
		this.stateModel = stateModel;
	}

	
	/**
	 * 
	 * 
	 * Supply Kalman Filter with a new measurement update and a timestamp of when
	 * the measurements were valid.
	 * 
	 * @param cubeReconstructor
	 * @param time
	 */
	public void measurementUpdate(CubePose cubePose, long time) {
		
		// =+=
		// X(K+1) = A(t) * X(k) + K(t) * Y(k)
	}
	
	
	/**
	 * 
	 * Return state as per the specified time stamp.
	 * 
	 * @param time
	 * @return
	 */
	public CubePose getState(long time) {
		
		// =+=
		// Y(k) = C(t) * X(k)
		
		return null;
	}

}
