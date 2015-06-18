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

import org.ejml.equation.Equation;
import org.ejml.equation.Sequence;
import org.ejml.simple.SimpleMatrix;

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
 * o  Note: matrices are row major.
 * 
 * Initial design:
 * o  Is simply a fixed, but time-interval-variant Kalman Filter Gain matrix.
 * o  Also records timestamp of last measurement update.
 * o  Forward, Kalman Gain, and Output are time-variant matrices, but linear to time.
 * 
 * 
 * @author android.steve@cl-sw.com
 *
 */
public class KalmanFilter {
	
// =+= not accurate
//	public enum STATE {
//		X_POS,
//		Y_POS,
//		Z_POS,
//		X_POS_VELOCITY,
//		Y_POS_VELOCITY,
//		Z_POS_VELOCITY,
//		X_AXIS_ROTATION,
//		Y_AXIS_ROTATION,
//		Z_AXIS_ROTATION,
//		X_AXIS_ANGULAR_ROTATION,
//		Y_AXIS_ANGULAR_ROTATION,
//		Z_AXIS_ANGULAR_ROTATION
//	};
//	

	@SuppressWarnings("unused")
	private StateModel stateModel;
	
	// Timestamp reference of state
	private long measUpateTime;
	
	// Feeback (or gain) Coefficient
	private double alpha = 1.0;

	// State Matrix.  State is 12 elements long: x_pos, x_vel, 
	private SimpleMatrix xSimpleMatrix;
	
	// Feed Forward Matrix
	private SimpleMatrix aSimpleMatrix = SimpleMatrix.identity(12);

	// Project State Forward Matrix (But do not change state)
	private SimpleMatrix bSimpleMatrix = SimpleMatrix.identity(12);
	
	// State to Output Matrix
//	private final double[][] hMatrix = { { 0.0f } };
	
	// Input to State Matrix
	private SimpleMatrix cSimpleMatrix = new SimpleMatrix(12, 6);

	// Kalman Gain Matrix
//	private final double[][] kMatrix = { { 0.0f } };

	// Projected state
	private SimpleMatrix zSimpleMatrix = new SimpleMatrix(12, 1);

	
	// Last reported (i.e., measured) cube pose
	private CubePose lastCubePoseState;
	
	// EJML Equations object
	private Equation equations = new Equation();

	// Equation z = B * x : calculate projected state.
	private Sequence projectStateEquation;
	

		
	
	/**
	 * Kalman Filter Constructor
	 * 
	 * Begin a new Kalman FIlter (actually, really new and fresh state variables).
	 * Initialize all state variables.
	 * 
	 * @param stateModel
	 */
	public KalmanFilter(StateModel stateModel) {
		
		this.stateModel = stateModel;
		
		// Maybe (or should be) done by constructor, but docs don't say.
		cSimpleMatrix.zero();
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
	public void measurementUpdate(CubePose cubePoseMeasure, long time) {
		
		lastCubePoseState = cubePoseMeasure;
		
		// If Kalman Filter not active, then just return.  Pose above will simply be used.
		if(MenuAndParams.kalmanFilter == false)
			return;
		
		// Sometimes happens when face is solved, but pose algorithm has problems.
		if(cubePoseMeasure == null)
			return;
		
		// Don't do any more if Kalman Filter is not active
		if(MenuAndParams.kalmanFilter == false)
			return;
		
		// First time through, simply set state to measurement.
		if(xSimpleMatrix == null) {

			xSimpleMatrix = new SimpleMatrix(12, 1, true, new double [] {
					cubePoseMeasure.x, 0,
					cubePoseMeasure.y, 0,
					cubePoseMeasure.z, 0,
					cubePoseMeasure.xRotation, 0,
					cubePoseMeasure.yRotation, 0,
					cubePoseMeasure.zRotation, 0 } );

			return;
		}
		
		// Calculate duration between last update.
		long tau = time - measUpateTime;
		
		// Record this time for future reference.
		measUpateTime = time;
		
		// Input is 6 element column vector
		SimpleMatrix uSimpleMatrix = new SimpleMatrix(6, 1, true, new double [] {
				cubePoseMeasure.x, 
				cubePoseMeasure.y, 
				cubePoseMeasure.z, 
				cubePoseMeasure.xRotation, 
				cubePoseMeasure.yRotation, 
				cubePoseMeasure.zRotation} );
		
		// Set coefficients of A matrix
		aSimpleMatrix.set(0, 0,  1.0 - alpha);
		aSimpleMatrix.set(0, 1, (1.0 - alpha) * tau);
		aSimpleMatrix.set(1, 0, -1.0 * alpha / tau);
		aSimpleMatrix.set(1, 1,  1.0 - alpha);
		aSimpleMatrix.set(2, 2,  1.0 - alpha);
		aSimpleMatrix.set(2, 3, (1.0 - alpha) * tau);
		aSimpleMatrix.set(3, 2, -1.0 * alpha / tau);
		aSimpleMatrix.set(3, 3,  1.0 - alpha);
		aSimpleMatrix.set(4, 4,  1.0 - alpha);
		aSimpleMatrix.set(4, 5, (1.0 - alpha) * tau);
		aSimpleMatrix.set(5, 4, -1.0 * alpha / tau);
		aSimpleMatrix.set(5, 5,  1.0 - alpha);
		aSimpleMatrix.set(6, 6,  1.0 - alpha);
		aSimpleMatrix.set(6, 7, (1.0 - alpha) * tau);
		aSimpleMatrix.set(7, 6, -1.0 * alpha / tau);
		aSimpleMatrix.set(7, 7,  1.0 - alpha);
		aSimpleMatrix.set(8, 8,  1.0 - alpha);
		aSimpleMatrix.set(8, 9, (1.0 - alpha) * tau);
		aSimpleMatrix.set(9, 8, -1.0 * alpha / tau);
		aSimpleMatrix.set(9, 9,  1.0 - alpha);
		aSimpleMatrix.set(10, 10,  1.0 - alpha);
		aSimpleMatrix.set(10, 11, (1.0 - alpha) * tau);
		aSimpleMatrix.set(11, 10, -1.0 * alpha / tau);
		aSimpleMatrix.set(11, 11,  1.0 - alpha);

		// Set coefficients of C matrix
		cSimpleMatrix.set(0, 0, alpha);
		cSimpleMatrix.set(1, 0, alpha/tau);
		cSimpleMatrix.set(2, 1, alpha);
		cSimpleMatrix.set(3, 1, alpha/tau);
		cSimpleMatrix.set(4, 2, alpha);
		cSimpleMatrix.set(5, 2, alpha/tau);
		cSimpleMatrix.set(6, 3, alpha);
		cSimpleMatrix.set(7, 3, alpha/tau);
		cSimpleMatrix.set(8, 4, alpha);
		cSimpleMatrix.set(9, 4, alpha/tau);
		cSimpleMatrix.set(10, 5, alpha);
		cSimpleMatrix.set(11, 5, alpha/tau);
		
		// Calculate new state
		// X(K+1) = A(alpha,tau) * X(k) + C(alpha) * U(k)		
		SimpleMatrix newState = (aSimpleMatrix.mult(xSimpleMatrix)).plus(cSimpleMatrix.mult(uSimpleMatrix));
		
		// Update State
		xSimpleMatrix = newState;
		equations.alias( xSimpleMatrix, "x");
		
		// =+= Crude control of feedback: goes from 100% to 25% and then stays there.
		// =+= Kalman Gain is supposed to be calculated algorithmically.
		if(alpha > 0.5)
			alpha = 0.5;
		else if(alpha > 0.25)
			alpha = 0.25;
	}
	
	
	/**
	 * Return state as per the specified time stamp.
	 * 
	 * @param time
	 * @return
	 */
	public CubePose projectState(long time) {
		
		long tau = time - measUpateTime;
		
		if(MenuAndParams.kalmanFilter == false)
			return lastCubePoseState;
		
		if(xSimpleMatrix == null)
			return null;
		
		bSimpleMatrix.set(0, 1, tau);
		bSimpleMatrix.set(2, 3, tau);
		bSimpleMatrix.set(4, 5, tau);
		bSimpleMatrix.set(6, 7, tau);
		bSimpleMatrix.set(8, 9, tau);
		
		if(projectStateEquation == null) {

			// Aliases for EJML
			equations.alias( xSimpleMatrix, "x", bSimpleMatrix, "B", zSimpleMatrix, "z");

			// Compile project state linear algebra equation
			projectStateEquation = equations.compile("z = B*x");
		}
		
		// Calculate projected state for specified time, but do not update state matrix.
		// z(t + tau) = B(tau) * x(t) 
		projectStateEquation.perform();
		
		// Package up
		CubePose cubePose = new CubePose();
		cubePose.x = (float) zSimpleMatrix.get(0);
		cubePose.y = (float) zSimpleMatrix.get(2);
		cubePose.z = (float) zSimpleMatrix.get(4);
		cubePose.xRotation = zSimpleMatrix.get(6);
		cubePose.yRotation = zSimpleMatrix.get(8);
		cubePose.zRotation = zSimpleMatrix.get(10);
		
		return cubePose;
	}
	
}