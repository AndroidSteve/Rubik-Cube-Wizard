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

	// State Matrix
	private double[] xMatrix; // = new double[12];
	
	// Feed Forward Matrix
	private final double[][] aMatrix = idenity(12);

	// Project State Forward Matrix (But do not change state)
	private SimpleMatrix bSimpleMatrix = SimpleMatrix.identity(12);
	
	// State to Output Matrix
//	private final double[][] hMatrix = { { 0.0f } };
	
	// Input to State Matrix
	// 6x12 matrix
	private final double[][] cMatrix = { 
			{ 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
			{ 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
			{ 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
			{ 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
			{ 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
			{ 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
			{ 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
			{ 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
			{ 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
			{ 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
			{ 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
			{ 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
			};

	// Kalman Gain Matrix
//	private final double[][] kMatrix = { { 0.0f } };
	
	// Last reported (i.e., measured) cube pose
	private CubePose lastCubePoseState;

		
	
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
		if(xMatrix == null) {
			double [] initialState = {
					cubePoseMeasure.x,
					0,
					cubePoseMeasure.y,
					0,
					cubePoseMeasure.z,
					0,
					cubePoseMeasure.xRotation,
					0,
					cubePoseMeasure.yRotation,
					0,
					cubePoseMeasure.zRotation,
					0};
			xMatrix = initialState;
			return;
		}
		
		// Calculate duration between last update.
		long tau = time - measUpateTime;
		
		// Record this time for future reference.
		measUpateTime = time;
		
		// Input is 6 element column vector
		double [] u = {
				cubePoseMeasure.x, 
				cubePoseMeasure.y, 
				cubePoseMeasure.z, 
				cubePoseMeasure.xRotation, 
				cubePoseMeasure.yRotation, 
				cubePoseMeasure.zRotation};
		
		// Set coefficients of A matrix
		aMatrix[0][0] =  1.0 - alpha;
		aMatrix[0][1] = (1.0 - alpha) * tau;
		aMatrix[1][0] = -1.0 * alpha / tau;
		aMatrix[1][1] =  1.0 - alpha;
		aMatrix[2][2] =  1.0 - alpha;
		aMatrix[2][3] = (1.0 - alpha) * tau;
		aMatrix[3][2] = -1.0 * alpha / tau;
		aMatrix[3][3] =  1.0 - alpha;
		aMatrix[4][4] =  1.0 - alpha;
		aMatrix[4][5] = (1.0 - alpha) * tau;
		aMatrix[5][4] = -1.0 * alpha / tau;
		aMatrix[5][5] =  1.0 - alpha;
		aMatrix[6][6] =  1.0 - alpha;
		aMatrix[6][7] = (1.0 - alpha) * tau;
		aMatrix[7][6] = -1.0 * alpha / tau;
		aMatrix[7][7] =  1.0 - alpha;
		aMatrix[8][8] =  1.0 - alpha;
		aMatrix[8][9] = (1.0 - alpha) * tau;
		aMatrix[9][8] = -1.0 * alpha / tau;
		aMatrix[9][9] =  1.0 - alpha;
		aMatrix[10][10] =  1.0 - alpha;
		aMatrix[10][11] = (1.0 - alpha) * tau;
		aMatrix[11][10] = -1.0 * alpha / tau;
		aMatrix[11][11] =  1.0 - alpha;
		
		// Set coefficients of C matrix
		cMatrix[0][0] = alpha;
		cMatrix[1][0] = alpha/tau;
		cMatrix[2][1] = alpha;
		cMatrix[3][1] = alpha/tau;
		cMatrix[4][2] = alpha;
		cMatrix[5][2] = alpha/tau;
		cMatrix[6][3] = alpha;
		cMatrix[7][3] = alpha/tau;
		cMatrix[8][4] = alpha;
		cMatrix[9][4] = alpha/tau;
		cMatrix[10][5] = alpha;
		cMatrix[11][5] = alpha/tau;
		
		// Calculate new state
		// X(K+1) = A(alpha,tau) * X(k) + C(alpha) * U(k)
		double[] newState = add( multiply(aMatrix, xMatrix), multiply(cMatrix, u) );
		
		// Update State
		xMatrix = newState;
//		Log.v(Constants.KALMAN, String.format("xPos = %6f xVel = %6f", xMatrix[0], xMatrix[1]));

		
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
		
		if(xMatrix == null)
			return null;
		
		bSimpleMatrix.set(0, 1, tau);
		bSimpleMatrix.set(2, 3, tau);
		bSimpleMatrix.set(4, 5, tau);
		bSimpleMatrix.set(6, 7, tau);
		bSimpleMatrix.set(8, 9, tau);
		bSimpleMatrix.set(10, 11, tau);
//		bSimpleMatrix.print();
		
		SimpleMatrix xSimpleMatrix = new SimpleMatrix(12, 1, true, xMatrix);
//		xSimpleMatrix.print();
		
//		// Calculate projected state for specified time, but do not update state matrix.
//		// X(t + tau) = B(tau) * X(tau) 
		// Should be a 12 x 1
		SimpleMatrix projectedStateSimpleMatrix = bSimpleMatrix.mult(xSimpleMatrix);
		projectedStateSimpleMatrix.print();
		
		// Package up
		CubePose cubePose = new CubePose();
		cubePose.x = (float) projectedStateSimpleMatrix.get(0);
		cubePose.y = (float) projectedStateSimpleMatrix.get(2);
		cubePose.z = (float) projectedStateSimpleMatrix.get(4);
		cubePose.xRotation = projectedStateSimpleMatrix.get(6);
		cubePose.yRotation = projectedStateSimpleMatrix.get(8);
		cubePose.zRotation = projectedStateSimpleMatrix.get(10);
		
		return cubePose;
	}
	
	
//	/**
//	 * Return state as per the specified time stamp.
//	 * 
//	 * @param time
//	 * @return
//	 */
//	public CubePose projectStateOld(long time) {
//		
//		long tau = time - measUpateTime;
//		
//		if(MenuAndParams.kalmanFilter == false)
//			return lastCubePoseState;
//		
//		if(xMatrix == null)
//			return null;
//		
//		bMatrix[0][1] = tau;
//		bMatrix[2][3] = tau;
//		bMatrix[4][5] = tau;
//		bMatrix[6][7] = tau;
//		bMatrix[8][9] = tau;
//		bMatrix[10][11] = tau;
//		
//		// Calculate projected state for specified time, but do not update state matrix.
//		// X(t + tau) = B(tau) * X(tau) 
//		double [] projectedState =  multiply(bMatrix, xMatrix);
//		
//		// Package up
//		CubePose cubePose = new CubePose();
//		cubePose.x = (float) projectedState[0];
//		cubePose.y = (float) projectedState[2];
//		cubePose.z = (float) projectedState[4];
//		cubePose.xRotation = projectedState[6];
//		cubePose.yRotation = projectedState[8];
//		cubePose.zRotation = projectedState[10];
//		
//		return cubePose;
//	}
	
	
	
	
	
	
	
	
    // return C = A * B
    @SuppressWarnings("unused")
	private static double[][] multiply(double[][] A, double[][] B) {
        int mA = A.length;
        int nA = A[0].length;
        int mB = B.length;
        int nB = B[0].length;
        if (nA != mB) throw new RuntimeException("Illegal matrix dimensions.");
        double[][] C = new double[mA][nB];
        for (int i = 0; i < mA; i++)
            for (int j = 0; j < nB; j++)
                for (int k = 0; k < nA; k++)
                    C[i][j] += (A[i][k] * B[k][j]);
        return C;
    }

    
    // matrix-vector multiplication (y = A * x)
    /**
     * @param A  : [column][row] Matrix of size [m][n]
     * @param x  : column vector of length n
     * @return
     */
    private static double[] multiply(double[][] A, double[] x) {
        int m = A.length;
        int n = A[0].length;
        if (x.length != n) throw new RuntimeException("Illegal matrix dimensions.");
        double[] y = new double[m];
        for (int i = 0; i < m; i++)
            for (int j = 0; j < n; j++)
                y[i] += (A[i][j] * x[j]);
        return y;
    }
    
    // matrix-addition
    @SuppressWarnings("unused")
	private static double [][] add(double[][] A, double[][] B) {
        int mA = A.length;
        int nA = A[0].length;
        int mB = B.length;
        int nB = B[0].length;
        if (nA != mB) throw new RuntimeException("Illegal matrix dimensions.");
        double[][] C = new double[mA][nB];
        for (int i = 0; i < mA; i++)
            for (int j = 0; j < nB; j++)
                C[i][j] += (A[i][j] + B[i][j]);
        
    	return C;
    }
    
    // matrix-addition
    private static double [] add(double[] A, double[] B) {
        int mA = A.length;
        int mB = B.length;
        if (mA != mB) throw new RuntimeException("Illegal matrix dimensions.");
        double[] C = new double[mA];
        for (int i = 0; i < mA; i++)
                C[i] += (A[i] + B[i]);
        
    	return C;
    }
    
    // Return new idenity matrix
    private static double[][] idenity(int n) {
        double[][] M = new double[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
            	M[i][j] = (i == j) ? 1.0 : 0.0;
        return M;
    }

}
