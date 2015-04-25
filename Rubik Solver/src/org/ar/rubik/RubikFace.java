/**
 * Augmented Reality Rubik Cube Solver
 * 
 * Author: Steven P. Punte (aka Android Steve : android.steve@cl-sw.com)
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

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.ar.rubik.Constants.ColorTileEnum;
import org.ar.rubik.Constants.FaceNameEnum;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import android.util.Log;

/**
 * @author android.steve@cl-sw.com
 *
 */
public class RubikFace implements Serializable {
	
	// For purposes of serialization
	private static final long serialVersionUID = -8498294721543708545L;

	
	// A Rubik Face can exist in the following states:
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
	
	public transient List<Rhombus> rhombusList = new LinkedList<Rhombus>();

	// A 3x3 matrix of Rhombus elements.  This array will be sorted to achieve
	// final correct position arrangement of available Rhombus objects.  Some elements can be null.
	public transient Rhombus [][] faceRhombusArray = new Rhombus[3][3];
	
	// A 3x3 matrix of Logical Tiles.  All elements must be non-null for an appropriate Face solution.
	// The rotation of this array is the output of the Face Recognizer as per the current spatial
	// rotation of the cube.
	public ColorTileEnum [][] observedTileArray = new ColorTileEnum[3][3];
	
	// A 3x3 matrix of Logical Tiles.  All elements must be non-null for an appropriate Face solution.
	// The rotation of this array has been adjusted so that, in the final cube state, the faces are read
	// and rendered correctly with respect to the "unfolded cube layout convention."
	public ColorTileEnum [][] transformedTileArray = new ColorTileEnum[3][3];
	
	// Record actual RGB colors measured at the center of each tile.
	public double[][][] measuredColorArray = new double[3][3][4];

	// Angle of Alpha-Axis (N) stored in radians.
	public double alphaAngle        = 0.0;
	
	// Angle of Beta-Axis (M) stored in radians.
	public double betaAngle         = 0.0;
	
	// Length in pixels of Alpha Lattice (i.e. a tile size)
	public double alphaLatticLength = 0.0;

	// Length in pixels of Beta Lattice (i.e. a tile size)
	public double betaLatticLength  = 0.0;
	
	// Ratio of Beta Lattice to Alpha Lattice
	public double gammaRatio        = 0.0;
	
	// Least Means Square Result
	public transient LeastMeansSquare lmsResult =
			// Put some dummy data here.
			new LeastMeansSquare(
					800,    // X origin of Rubik Face (i.e. center of tile {0,0})
					200,    // Y origin of Rubik Face (i.e. center of tile {0,0})
					50,     // Length of Alpha Lattice
					null, 
					314,    // Sigma Error (i.e. RMS of know Rhombus to Tile centers)
					true);  // Allow these dummy results to be display even though they are false
	
	// Number of rhombus that were moved in order to obtain better LMS fit.
	public int numRhombusMoves = 0;
	
	// This is a proprietary hash code and NOT that of function hashCode().  This hash code is 
	// intended to be unique and repeatable for any given set of colored tiles in a specified set 
	// of locations on a Rubik Face.  It is used to determine if an identical Rubik Face is being
	// observed multiple times. Note, if a tiles color designation is changed due to a change in 
	// lighting conditions, the calculated hash code will be different.  A more robust strategy 
	// would be to require that only 8 or 9 tiles match in order to determine if an 
	// identical face is being presented.
	public int myHashCode = 0;
	
	// Profiles CPU Consumption
	public transient Profiler profiler = new Profiler();
	
	// Face Designation: i.e., Up, Down, ....
	public FaceNameEnum faceNameEnum;
	
	
	/**
	 * Rubik Face Constructor
	 * 
	 * @param imageProcessMode
	 * @param annotationMode
	 */
	public RubikFace() {
			
		// Dummy data
		alphaAngle = 45.0 * Math.PI / 180.0;
		betaAngle  = 135.0 * Math.PI / 180.0;
		alphaLatticLength = 50.0;
		betaLatticLength  = 50.0;
	}

	
	
	/**
	 * Process Rhombuses
	 * 
	 * Given the Rhombus list, attempt to recognize the grid dimensions and orientation,
	 * and full tile color set.
	 * 
	 * @param rhombusList
	 * @param image 
	 */
    public void processRhombuses(List<Rhombus> rhombusList, Mat image) {
    	
    	this.rhombusList = rhombusList;
		
		// Don't even attempt if less than three rhombus are identified.
		if(rhombusList.size() < 3) {
			faceRecognitionStatus = FaceRecognitionStatusEnum.INSUFFICIENT;
			return;
		}
		
		// Calculate average alpha and beta angles, and also gamma ratio.
		// Sometimes (but probably only when certain bugs exist) can contain NaN data.
		if( calculateMetrics() == false) {
			faceRecognitionStatus =  FaceRecognitionStatusEnum.BAD_METRICS;
			return;
		}
		
		// Layout Rhombi into Face Array
		if( TileLayoutAlgorithm.doInitialLayout(rhombusList, faceRhombusArray, alphaAngle, betaAngle) == false) {
			faceRecognitionStatus =  FaceRecognitionStatusEnum.INADEQUATE;
			return;
		}
		
		// Evaluate Rhombi into Array fit RMS
		lmsResult = findOptimumFaceFit();
		if(lmsResult.valid == false) {
			faceRecognitionStatus =  FaceRecognitionStatusEnum.INVALID_MATH;
			return;
		}
		
		alphaLatticLength = lmsResult.alphaLattice;
		betaLatticLength  = gammaRatio * lmsResult.alphaLattice;
		double lastSigma = lmsResult.sigma;
		
		// Loop until some resolution
		while(lmsResult.sigma > MenuAndParams.faceLmsThresholdParam.value) {
			
			if(numRhombusMoves > 5) {
				faceRecognitionStatus =  FaceRecognitionStatusEnum.INCOMPLETE;
				return;
			}
			
			// Move a Rhombi
			if( findAndMoveArhombusToAbetterLocation() == false) {
				faceRecognitionStatus =  FaceRecognitionStatusEnum.BLOCKED;
				return;
			}
			numRhombusMoves++;
			
			// Evaluate
			lmsResult = findOptimumFaceFit();
			if(lmsResult.valid == false) {
				faceRecognitionStatus =  FaceRecognitionStatusEnum.INVALID_MATH;
				return;
			}
			alphaLatticLength = lmsResult.alphaLattice;
			betaLatticLength  = gammaRatio * lmsResult.alphaLattice;
			
			// RMS has increased, we are NOT converging on a solution.
			if(lmsResult.sigma > lastSigma)
				faceRecognitionStatus =  FaceRecognitionStatusEnum.UNSTABLE;
		}
		
		// A good solution has been reached!
		
		// Obtain Logical Tiles
		new ColorRecognition.Face(this).faceTileColorRecognition(image);

		// Calculate a hash code that is unique for the given collection of Logical Tiles.
		// Added right rotation to obtain unique number with respect to locations.
		myHashCode = 0;
		for(int n=0; n<3; n++)
			for(int m=0; m<3; m++)
				myHashCode = observedTileArray[n][m].hashCode() ^ Integer.rotateRight(myHashCode, 1);
		
		
		faceRecognitionStatus =  FaceRecognitionStatusEnum.SOLVED;
    }


    
	/**
	 * Calculate Metrics
	 * 
	 * Obtain alpha beta and gammaRatio from Rhombi set.
	 * 
	 * =+= Initially, assume all provide Rhombus are in face, and simply take average.
	 * =+= Later provide more smart filtering.
	 */
	private boolean calculateMetrics() {

		int numElements = rhombusList.size();
		for(Rhombus rhombus : rhombusList) {

			alphaAngle += rhombus.alphaAngle;
			betaAngle  += rhombus.betaAngle;
			gammaRatio += rhombus.gammaRatio;
		}

		alphaAngle = alphaAngle / numElements * Math.PI / 180.0;
		betaAngle  = betaAngle / numElements * Math.PI / 180.0;
		gammaRatio = gammaRatio /numElements;
		
		Log.i(Constants.TAG, String.format( "RubikFace: alphaAngle=%4.0f betaAngle=%4.0f gamma=%4.2f", 
				alphaAngle * 180.0 / Math.PI, 
				betaAngle  * 180.0 / Math.PI, 
				gammaRatio));
		
		// =+= currently, always return OK
		return true;
	}

	

	/**
	 * Calculate the optimum fit for the given layout of Rhombus in the Face.
	 * 
	 * Set Up BIG Linear Equation: Y = AX
	 * Where:
	 *   Y is a 2k x 1 matrix of actual x and y location from rhombus centers (known values)
	 *   X is a 3 x 1  matrix of { x_origin, y_origin, and alpha_lattice } (values we wish to find)
	 *   A is a 2k x 3 matrix of coefficients derived from m, n, alpha, beta, and gamma. 
	 * 
	 * Notes:
	 *   k := Twice the number of available rhombus.
	 *   n := integer axis of the face.
	 *   m := integer axis of the face.
	 *   
	 *   gamma := ratio of beta to alpha lattice size.
	 * 
	 * Also, calculate sum of errors squared.
	 *   E = Y - AX
	 * @return
	 */
	private LeastMeansSquare findOptimumFaceFit() {

		// Count how many non-empty cell actually have a rhombus in it.
		int k = 0;
		for(int n=0; n<3; n++)
			for(int m=0; m<3; m++)
				if(faceRhombusArray[n][m] != null)
					k++;
		
		Log.i(Constants.TAG, "Big K: " + k);

		Mat bigAmatrix  = new Mat(2*k, 3,  CvType.CV_64FC1);
		Mat bigYmatrix  = new Mat(2*k, 1,  CvType.CV_64FC1);
		Mat bigXmatrix  = new Mat(  3, 1,  CvType.CV_64FC1);  //{ origin_x, origin_y, latticeAlpha }

		// Load up matrices Y and A 
		// X_k = X + n * L_alpha * cos(alpha) + m * L_beta * cos(beta)
		// Y_k = Y + n * L_alpha * sin(alpha) + m * L_beta * sin(beta)
		int index = 0;
		for(int n=0; n<3; n++) {
			for(int m=0; m<3; m++) {
				Rhombus rhombus = faceRhombusArray[n][m];
				if(rhombus != null) {

					{   
						// Actual X axis value of Rhombus in this location
						double bigY = rhombus.center.x;
						
						// Express expected X axis value : i.e. x = func( x_origin, n, m, alpha, beta, alphaLattice, gamma)
						double bigA = n * Math.cos(alphaAngle) + gammaRatio * m * Math.cos(betaAngle);

						bigYmatrix.put(index, 0, new double[]{bigY});
						
						bigAmatrix.put(index, 0, new double[]{1.0});
						bigAmatrix.put(index, 1, new double[]{0.0});
						bigAmatrix.put(index, 2, new double[]{bigA});

						index++;
					}


					{
						// Actual Y axis value of Rhombus in this location
						double bigY = rhombus.center.y;
						
						// Express expected Y axis value : i.e. y = func( y_origin, n, m, alpha, beta, alphaLattice, gamma)
						double bigA = n * Math.sin(alphaAngle) + gammaRatio * m * Math.sin(betaAngle);

						bigYmatrix.put(index, 0, new double[]{bigY});
						
						bigAmatrix.put(index, 0, new double[]{0.0});
						bigAmatrix.put(index, 1, new double[]{1.0});
						bigAmatrix.put(index, 2, new double[]{bigA});

						index++;
					}
				}
			}
		}

		
//		Log.v(Constants.TAG, "Big A Matrix: " + bigAmatrix.dump());
//		Log.v(Constants.TAG, "Big Y Matrix: " + bigYmatrix.dump());

		// Least Means Square Regression to find best values of origin_x, origin_y, and alpha_lattice.
		// Problem:  Y=AX  Known Y and A, but find X.
		// Tactic:   Find minimum | AX - Y | (actually sum square of elements?)
		// OpenCV:   Core.solve(Mat src1, Mat src2, Mat dst, int)
		// OpenCV:   dst = arg min _X|src1 * X - src2|
		// Thus:     src1 = A  { 2k rows and  3 columns }
		//           src2 = Y  { 2k rows and  1 column  }
		//           dst =  X  {  3 rows and  1 column  }
		//
		boolean solveFlag = Core.solve(bigAmatrix, bigYmatrix, bigXmatrix, Core.DECOMP_NORMAL);
		
//		Log.v(Constants.TAG, "Big X Matrix Result: " + bigXmatrix.dump());
		
		// Sum of error square
		// Given X from above, the Y_estimate = AX
		// E = Y - AX
		Mat bigEmatrix  = new Mat(2*k, 1,  CvType.CV_64FC1);
		for(int r=0; r<(2*k); r++) {
			double y = bigYmatrix.get(r, 0)[0];
			double error = y;
			for(int c=0; c<3; c++) {
				double a = bigAmatrix.get(r, c)[0];
				double x = bigXmatrix.get(c, 0)[0];
				error -= a * x;
			}
			bigEmatrix.put(r, 0, error);
		}

		// sigma^2 = diagonal_sum( Et * E)
		double sigma = 0;
		for(int r=0; r<(2*k); r++) {
			double error = bigEmatrix.get(r, 0)[0];
			sigma += error * error;
		}
		sigma = Math.sqrt(sigma);
		
//		Log.v(Constants.TAG, "Big E Matrix Result: " + bigEmatrix.dump());

		// =+= not currently in use, could be deleted.
		// Retrieve Error terms and compose an array of error vectors: one of each occupied
		// cell who's vector point from tile center to actual location of rhombus.
		Point [][] errorVectorArray = new Point[3][3];
		index = 0;
		for(int n=0; n<3; n++) {
			for(int m=0; m<3; m++) {
				Rhombus rhombus = faceRhombusArray[n][m];  // We expect this array to not have change from above.
				if(rhombus != null) {
					errorVectorArray[n][m] = 
							new Point(
									bigEmatrix.get(index++, 0)[0],
									bigEmatrix.get(index++, 0)[0] );
				}
			}
		}
		
				
		double x = bigXmatrix.get(0, 0)[0];
		double y = bigXmatrix.get(1, 0)[0];
		double alphaLatice = bigXmatrix.get(2, 0)[0];
		boolean valid = !Double.isNaN(x) && !Double.isNaN(y) && !Double.isNaN(alphaLatice) && !Double.isNaN(sigma);
		
		Log.i(Constants.TAG, String.format( "Rubik Solution: x=%4.0f y=%4.0f alphaLattice=%4.0f  sigma=%4.0f flag=%b",
				x,
				y,
				alphaLatice,
				sigma,
				solveFlag));
		
		return new LeastMeansSquare(
				x,
				y,
				alphaLatice,
				errorVectorArray,
				sigma,
				valid);
	}
	
	
	/**
	 * Find And Move A Rhombus To A Better Location
	 * 
	 * Returns true if a tile was move or swapped, otherwise returns false.
	 * 
	 * Find Tile-Rhombus (i.e. {n,m}) with largest error assuming findOptimumFaceFit() has been called.
	 * Determine which direction the Rhombus would like to move and swap it with that location.
	 */
	private boolean findAndMoveArhombusToAbetterLocation() {
		
		
		double errorArray[][] = new double[3][3];
		Point errorVectorArray[][] = new Point[3][3];
		
		// Identify Tile-Rhombus with largest error
		Rhombus largestErrorRhombus = null;
		double largetError = Double.NEGATIVE_INFINITY;
		int tile_n = 0;  // Record current location of Rhombus we wish to move.
		int tile_m = 0;
		for(int n=0; n<3; n++) {
			for(int m=0; m<3; m++) {
				Rhombus rhombus = faceRhombusArray[n][m];
				if(rhombus != null) {

					// X and Y location of the center of a tile {n,m}
					double tile_x = lmsResult.origin.x + n * alphaLatticLength * Math.cos(alphaAngle) + m * betaLatticLength * Math.cos(betaAngle);
					double tile_y =	lmsResult.origin.y + n * alphaLatticLength * Math.sin(alphaAngle) + m * betaLatticLength * Math.sin(betaAngle);

					// Error from center of tile to reported center of Rhombus
					double error = Math.sqrt(
							(rhombus.center.x - tile_x) * (rhombus.center.x - tile_x) +
							(rhombus.center.y - tile_y) * (rhombus.center.y - tile_y) );
					errorArray[n][m] = error;
					errorVectorArray[n][m] = new Point(rhombus.center.x - tile_x, rhombus.center.y - tile_y);

					// Record largest error found
					if(error > largetError) {
						largestErrorRhombus = rhombus;
						tile_n = n;
						tile_m = m;
						largetError = error;
					}
				}
			}
		}
		
		// For each tile location print: center of current Rhombus, center of tile, error vector, error magnitude.
		Log.d(Constants.TAG, String.format( " m:n|-----0-----|------1----|----2------|") );
		Log.d(Constants.TAG, String.format( " 0  |%s|%s|%s|", Util.dumpLoc(faceRhombusArray[0][0]), Util.dumpLoc(faceRhombusArray[1][0]), Util.dumpLoc(faceRhombusArray[2][0]))); 
		Log.d(Constants.TAG, String.format( " 0  |%s|%s|%s|", Util.dumpPoint(getTileCenterInPixels(0, 0)), Util.dumpPoint(getTileCenterInPixels(1, 0)), Util.dumpPoint(getTileCenterInPixels(2, 0)))); 
		Log.d(Constants.TAG, String.format( " 0  |%s|%s|%s|", Util.dumpPoint(errorVectorArray[0][0]), Util.dumpPoint(errorVectorArray[1][0]), Util.dumpPoint(errorVectorArray[2][0]))); 
		Log.d(Constants.TAG, String.format( " 0  |%11.0f|%11.0f|%11.0f|", errorArray[0][0], errorArray[1][0], errorArray[2][0])); 
		Log.d(Constants.TAG, String.format( " 1  |%s|%s|%s|", Util.dumpLoc(faceRhombusArray[0][1]), Util.dumpLoc(faceRhombusArray[1][1]), Util.dumpLoc(faceRhombusArray[2][1]))); 
		Log.d(Constants.TAG, String.format( " 1  |%s|%s|%s|", Util.dumpPoint(getTileCenterInPixels(0, 1)), Util.dumpPoint(getTileCenterInPixels(1, 1)), Util.dumpPoint(getTileCenterInPixels(2, 1)))); 
		Log.d(Constants.TAG, String.format( " 1  |%s|%s|%s|", Util.dumpPoint(errorVectorArray[0][1]), Util.dumpPoint(errorVectorArray[1][1]), Util.dumpPoint(errorVectorArray[2][1]))); 
		Log.d(Constants.TAG, String.format( " 1  |%11.0f|%11.0f|%11.0f|", errorArray[0][1], errorArray[1][1], errorArray[2][1])); 
		Log.d(Constants.TAG, String.format( " 2  |%s|%s|%s|", Util.dumpLoc(faceRhombusArray[0][2]), Util.dumpLoc(faceRhombusArray[1][2]), Util.dumpLoc(faceRhombusArray[2][2]))); 
		Log.d(Constants.TAG, String.format( " 2  |%s|%s|%s|", Util.dumpPoint(getTileCenterInPixels(0, 2)), Util.dumpPoint(getTileCenterInPixels(1, 2)), Util.dumpPoint(getTileCenterInPixels(2, 2)))); 
		Log.d(Constants.TAG, String.format( " 2  |%s|%s|%s|", Util.dumpPoint(errorVectorArray[0][2]), Util.dumpPoint(errorVectorArray[1][2]), Util.dumpPoint(errorVectorArray[2][2]))); 
		Log.d(Constants.TAG, String.format( " 2  |%11.0f|%11.0f|%11.0f|", errorArray[0][2], errorArray[1][2], errorArray[2][2])); 
		Log.d(Constants.TAG, String.format( "    |-----------|-----------|-----------|") );
		

		// Calculate vector error (from Tile to Rhombus) components along X and Y axis
		double error_x = largestErrorRhombus.center.x - (lmsResult.origin.x + tile_n * alphaLatticLength * Math.cos(alphaAngle) + tile_m * betaLatticLength * Math.cos(betaAngle));
		double error_y = largestErrorRhombus.center.y - (lmsResult.origin.y + tile_n * alphaLatticLength * Math.sin(alphaAngle) + tile_m * betaLatticLength * Math.sin(betaAngle));
		Log.d(Constants.TAG, String.format( "Tile at [%d][%d] has x error = %4.0f y error = %4.0f", tile_n, tile_m, error_x, error_y));

		// Project vector error (from Tile to Rhombus) components along alpha and beta directions.
		double alphaError = error_x *      Math.cos(alphaAngle) + error_y * Math.sin(alphaAngle);
		double betaError  = error_x *      Math.cos(betaAngle)  + error_y * Math.sin(betaAngle);
		Log.d(Constants.TAG, String.format( "Tile at [%d][%d] has alpha error = %4.0f beta error = %4.0f", tile_n, tile_m, alphaError, betaError));
		
		// Calculate index vector correction: i.e., preferred direction to move this tile.
		int delta_n = (int) Math.round(alphaError / alphaLatticLength); 
		int delta_m = (int) Math.round(betaError / betaLatticLength);
		Log.d(Constants.TAG, String.format( "Correction Index Vector: [%d][%d]", delta_n, delta_m));
		
		// Calculate new location of tile
		int new_n = tile_n + delta_n;
		int new_m = tile_m + delta_m;
		
		// Limit according to dimensions of face
		if(new_n < 0) new_n = 0;
		if(new_n > 2) new_n = 2;
		if(new_m < 0) new_m = 0;
		if(new_m > 2) new_m = 2;
		
		// Cannot move, move is to original location
		if(new_n == tile_n && new_m == tile_m) {
			Log.i(Constants.TAG, String.format( "Tile at [%d][%d] location NOT moved", tile_n, tile_m));
			return false;
		}
		
		// Move Tile or swap with tile in that location.
		else {
			Log.i(Constants.TAG, String.format( "Swapping Rhombi [%d][%d] with  [%d][%d]", tile_n, tile_m, new_n, new_m));
			Rhombus tmp = faceRhombusArray[new_n][new_m];
			faceRhombusArray[new_n][new_m] = faceRhombusArray[tile_n][tile_m];
			faceRhombusArray[tile_n][tile_m] = tmp;
			return true;
		}
		
	}

	
	/**
	 * 
	 * @param n
	 * @param m
	 * @return
	 */
	public Point getTileCenterInPixels(int n, int m) {
		return new Point(
				lmsResult.origin.x + n * alphaLatticLength * Math.cos(alphaAngle) + m * betaLatticLength * Math.cos(betaAngle),
				lmsResult.origin.y + n * alphaLatticLength * Math.sin(alphaAngle) + m * betaLatticLength * Math.sin(betaAngle)	);
	}
}
