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

import org.ar.rubik.Constants.ImageProcessModeEnum;
import org.ar.rubik.Constants.ConstantTile;
import org.ar.rubik.Constants.ConstantTileColorEnum;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import android.util.Log;

/**
 * Rubik Face
 * 
 * Processes a list (in arbitrary order) of Rhombus into the correct 3x3 matrix that compose a Rubik Face.
 * Also, evaluations Rhombus color and assigns correct Rubik Color Enum.
 * 
 * 
 * Typically, and initially the only orientation supported, the top surface of the Rubik face appears
 * as almost a Rhombis, but technically a parallelogram as below.  Note that the positive Y axis is
 * downwards:
 * 
 * 
 *    -----> X                    /  \
 *    |                      0  /      \  0
 *    |                       /          \
 *   \ /           M        / \         /  \         N
 *    Y                1  /     \     /      \  1
 *                      /         \ /          \
 *                    / \         / \        /   \
 *               2  /     \     /     \    /       \  2
 *                /         \ /         \ /          \
 *              / \         / \         / \          / \
 *            /     \     /     \     /     \      /     \
 *          /         \ /         \ /         \  /         \
 *        |_            \         / \          /            _| 
 *      Beta              \     /     \      /                Alpha
 *                          \ /         \  /
 *                            \          /
 *                              \      /
 *                                \  /
 *                      
 *                      
 *                      
 *                      
 *                      
 * @author stevep
 *
 */
public class DeprecatedRubikFace implements Serializable {
	
	// For purposes of serialization
	private static final long serialVersionUID = -8498294721543708545L;


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
	private transient Rhombus [][] faceRhombusArray = new Rhombus[3][3];
	
	// A 3x3 matrix of Logical Tiles.  All elements must be non-null for an appropriate Face solution.
	public ConstantTile [][] logicalTileArray = new ConstantTile[3][3];
	
	private double[][][] measuredColorArray = new double[3][3][4];

	// Angle of Alpha-Axis (N) stored in radians.
	public double alphaAngle        = 0.0;
	
	// Angle of Beta-Axis (M) stored in radians.
	public double betaAngle         = 0.0;
	
	// Length in pixels of Alpha Lattice (i.e. a tile size)
	public double alphaLatticLength = 0.0;

	// Length in pixels of Beta Lattice (i.e. a tile size)
	public double betaLatticLength  = 0.0;
	
	// Ratio of Beta Lattice to Alpha Lattice
	private double gammaRatio        = 0.0;
	
	// Luminous Offset: Added to luminous of tiles for better accuracy
	private double luminousOffset = 0.0;
	
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

	// Original RGB image
	private transient Mat original_image;
	
	private ImageProcessModeEnum imageProcessMode;

	// Exclusive-Or of logical tile hash codes.  This is (intended) to be unique of a given 
	// set of tiles (possible in their unique locations)
	public int hashCode = 0;

	// Sum of Color Error before Luminous correction
	private double colorErrorBeforeCorrection;

	// Sum of Color Error after Luminous correction
	private double colorErrorAfterCorrection;



	/**
	 * Rubik Face Constructor
	 * 
	 * @param imageProcessMode
	 * @param annotationMode
	 */
	public DeprecatedRubikFace(ImageProcessModeEnum imageProcessMode) {
		
		this.imageProcessMode = imageProcessMode;
		
		// Dummy data
		alphaAngle = 45.0 * Math.PI / 180.0;
		betaAngle  = 135.0 * Math.PI / 180.0;
		alphaLatticLength = 50.0;
		betaLatticLength  = 50.0;
	}



	/**
	 * Find Solution From Image
	 * 
	 * A Rubik Face solution is obtained from the provided image.
	 * A successful solution will set faceRecognitionStatus to SOLVED.
	 * An image with overlaid annotation will be returned.
	 * 
	 * @param image
	 * @return
	 */
	public Mat findSolutionFromImage(Mat image) {	
		
		this.original_image = image;
		
		// This will fill in rhombusList.
		Mat processImage = DeprecatedProcessImage.processImageIntoRhombiList(
			image, 
			null,
			imageProcessMode,
			rhombusList,
			this);
		
		if(imageProcessMode != ImageProcessModeEnum.NORMAL)
			return processImage;
		
		
		faceRecognitionStatus = findSolutionFromRhombiList();
		
		DeprecatedProcessImage.noteCompletionTime();
				
		Log.i(Constants.TAG, String.format("Face Solution=%12s Time=%dmS NumRhombi=%d Sigma=%4.0f NumMoves=%d",
				faceRecognitionStatus,
				DeprecatedProcessImage.getTotalComputionTime(),
				rhombusList.size(),
				lmsResult.sigma,
				numRhombusMoves) );
		

		return original_image;
	}
	
	
	
	/**
	 * Solve Face
	 * 
	 * Find correct location of Rhombus objects in Face Array.
	 */
	private FaceRecognitionStatusEnum findSolutionFromRhombiList() {
		
		final double sigmaRmsFaceThreasholdParam = 35; // 35 pixels RMS.
		
		// Don't even attempt if less than three rhombus are identified.
		if(rhombusList.size() < 3)
			return FaceRecognitionStatusEnum.INSUFFICIENT;
		
		// Calculate average alpha and beta angles, and also gamma ratio.
		// Sometimes (but probably only when certain bugs exist) can contain NaN data.
		if( calculateMetrics() == false)
			return FaceRecognitionStatusEnum.BAD_METRICS;
		
		// Layout Rhombi into Face Array
		if( TileLayoutAlgorithm.doInitialLayout(rhombusList, faceRhombusArray, alphaAngle, betaAngle) == false)
			return FaceRecognitionStatusEnum.INADEQUATE;
		
		// Evaluate Rhombi into Array fit RMS
		lmsResult = findOptimumFaceFit();
		if(lmsResult.valid == false)
			return FaceRecognitionStatusEnum.INVALID_MATH;
		
		alphaLatticLength = lmsResult.alphaLattice;
		betaLatticLength  = gammaRatio * lmsResult.alphaLattice;
		double lastSigma = lmsResult.sigma;
		
		// Loop until some resolution
		while(lmsResult.sigma > sigmaRmsFaceThreasholdParam) {
			
			if(numRhombusMoves > 5)
				return FaceRecognitionStatusEnum.INCOMPLETE;
			
			// Move a Rhombi
			if( findAndMoveArhombusToAbetterLocation() == false)
				return FaceRecognitionStatusEnum.BLOCKED;
			
			numRhombusMoves++;
			
			// Evaluate
			lmsResult = findOptimumFaceFit();
			if(lmsResult.valid == false)
				return FaceRecognitionStatusEnum.INVALID_MATH;
			alphaLatticLength = lmsResult.alphaLattice;
			betaLatticLength  = gammaRatio * lmsResult.alphaLattice;
			
			// RMS has increased, we are NOT converging on a solution.
			if(lmsResult.sigma > lastSigma)
				return FaceRecognitionStatusEnum.UNSTABLE;
		}
		
		// A good solution has been reached!
		
		// Obtain Logical Tiles
		findClosestLogicalTiles();

		
		// Calculate a hash code that is unique for the given collection of Logical Tiles.
		// Added right rotation because not that hard for duplicates.
		hashCode = 0;
		for(int n=0; n<3; n++)
			for(int m=0; m<3; m++)
				hashCode = logicalTileArray[n][m].hashCode() ^ Integer.rotateRight(hashCode, 1);
		
		return FaceRecognitionStatusEnum.SOLVED;
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
		Log.d(Constants.TAG, String.format( " 0  |%s|%s|%s|", dumpLoc(faceRhombusArray[0][0]), dumpLoc(faceRhombusArray[1][0]), dumpLoc(faceRhombusArray[2][0]))); 
		Log.d(Constants.TAG, String.format( " 0  |%s|%s|%s|", dumpPoint(getTileCenterInPixels(0, 0)), dumpPoint(getTileCenterInPixels(1, 0)), dumpPoint(getTileCenterInPixels(2, 0)))); 
		Log.d(Constants.TAG, String.format( " 0  |%s|%s|%s|", dumpPoint(errorVectorArray[0][0]), dumpPoint(errorVectorArray[1][0]), dumpPoint(errorVectorArray[2][0]))); 
		Log.d(Constants.TAG, String.format( " 0  |%11.0f|%11.0f|%11.0f|", errorArray[0][0], errorArray[1][0], errorArray[2][0])); 
		Log.d(Constants.TAG, String.format( " 1  |%s|%s|%s|", dumpLoc(faceRhombusArray[0][1]), dumpLoc(faceRhombusArray[1][1]), dumpLoc(faceRhombusArray[2][1]))); 
		Log.d(Constants.TAG, String.format( " 1  |%s|%s|%s|", dumpPoint(getTileCenterInPixels(0, 1)), dumpPoint(getTileCenterInPixels(1, 1)), dumpPoint(getTileCenterInPixels(2, 1)))); 
		Log.d(Constants.TAG, String.format( " 1  |%s|%s|%s|", dumpPoint(errorVectorArray[0][1]), dumpPoint(errorVectorArray[1][1]), dumpPoint(errorVectorArray[2][1]))); 
		Log.d(Constants.TAG, String.format( " 1  |%11.0f|%11.0f|%11.0f|", errorArray[0][1], errorArray[1][1], errorArray[2][1])); 
		Log.d(Constants.TAG, String.format( " 2  |%s|%s|%s|", dumpLoc(faceRhombusArray[0][2]), dumpLoc(faceRhombusArray[1][2]), dumpLoc(faceRhombusArray[2][2]))); 
		Log.d(Constants.TAG, String.format( " 2  |%s|%s|%s|", dumpPoint(getTileCenterInPixels(0, 2)), dumpPoint(getTileCenterInPixels(1, 2)), dumpPoint(getTileCenterInPixels(2, 2)))); 
		Log.d(Constants.TAG, String.format( " 2  |%s|%s|%s|", dumpPoint(errorVectorArray[0][2]), dumpPoint(errorVectorArray[1][2]), dumpPoint(errorVectorArray[2][2]))); 
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
	 * Find Closest Logical Tile
	 * 
	 * Two Pass algorithm:
	 * 1) Find closest fit using just U and V axis.
	 * 2) Calculate luminous correction value assuming above choices are correct (exclude Red and Orange)
	 * 3) Find closed fit again using Y, U and V axis where Y is corrected.
	 * 
	 * @return
	 */
	private void findClosestLogicalTiles() {
		
		double [][] colorError = new double[3][3];
		
		// Obtain actual measured tile color from image.
		for(int n=0; n<3; n++) {
			for(int m=0; m<3; m++) {

				Point tileCenter = getTileCenterInPixels(n, m);
				try {
					Mat mat = original_image.submat((int)(tileCenter.y - 10), (int)(tileCenter.y + 10), (int)(tileCenter.x - 10), (int)(tileCenter.x + 10));
					
					Scalar mean = Core.mean(mat);
					double [] actualTileColor = mean.val;
					measuredColorArray[n][m] = actualTileColor;
				}

				// Probably LMS calculations produced bogus tile location.
				catch(CvException cvException) {
					Log.e(Constants.TAG, "ERROR: x=" + tileCenter.x + " y=" + tileCenter.y + " img=" + original_image + " :" + cvException);
					measuredColorArray[n][m] = new double[4];				
				}
			}
		}

		
		// First Pass: Find closest logical color using only UV axis.
		for(int n=0; n<3; n++) {
			for(int m=0; m<3; m++) {

				double [] measuredColor = measuredColorArray[n][m];
				double [] measuredColorYUV   = Util.getYUVfromRGB(measuredColor);

				double smallestError = Double.MAX_VALUE;
				ConstantTile bestCandidate = null;
				for(int i=0; i<6; i++) {
					ConstantTile candidateTile = Constants.constantTileColorArray[i];
					double[] candidateColorYUV = Util.getYUVfromRGB(candidateTile.color.val);

					// Only examine U and V axis, and not luminous.
					double error =
							(candidateColorYUV[1] - measuredColorYUV[1]) * (candidateColorYUV[1] - measuredColorYUV[1]) +
							(candidateColorYUV[2] - measuredColorYUV[2]) * (candidateColorYUV[2] - measuredColorYUV[2]);

					colorError[n][m] = Math.sqrt(error);

					if(error < smallestError) {
						bestCandidate = candidateTile;
						smallestError = error;
					}
				}

//				Log.d(Constants.TAG, String.format( "Tile[%d][%d] has R=%3.0f, G=%3.0f B=%3.0f %c err=%4.0f", n, m, measuredColor[0], measuredColor[1], measuredColor[2], bestCandidate.character, smallestError));

				logicalTileArray[n][m] = bestCandidate;
			}
		}
		
		// Calculate and record LMS error (including luminous).
		for(int n=0; n<3; n++) {
			for(int m=0; m<3; m++) {
				double[] selectedColor = logicalTileArray[n][m].color.val;
				double[] measuredColor = measuredColorArray[n][m];
				colorErrorBeforeCorrection += calculateColorError(selectedColor, measuredColor, true, 0.0);
			}
		}
		
		// For each tile location print: measure RGB, measure YUV, logical RGB, logical YUV
		Log.d(Constants.TAG, "Table: Measure RGB, Measure YUV, Logical RGB, Logical YUV");
		Log.d(Constants.TAG, String.format( " m:n|----------0--------------|-----------1-------------|---------2---------------|") );
		Log.d(Constants.TAG, String.format( " 0  |%s|%s|%s|", Util.dumpRGB(measuredColorArray[0][0], colorError[0][0]), Util.dumpRGB(measuredColorArray[1][0], colorError[1][0]), Util.dumpRGB(measuredColorArray[2][0], colorError[2][0]) )); 
		Log.d(Constants.TAG, String.format( " 0  |%s|%s|%s|", Util.dumpYUV(measuredColorArray[0][0]), Util.dumpYUV(measuredColorArray[1][0]), Util.dumpYUV(measuredColorArray[2][0]) )); 
		Log.d(Constants.TAG, String.format( " 0  |%s|%s|%s|", Util.dumpRGB(logicalTileArray[0][0]), Util.dumpRGB(logicalTileArray[1][0]), Util.dumpRGB(logicalTileArray[2][0]) )); 
		Log.d(Constants.TAG, String.format( " 0  |%s|%s|%s|", Util.dumpYUV(logicalTileArray[0][0].color.val), Util.dumpYUV(logicalTileArray[1][0].color.val), Util.dumpYUV(logicalTileArray[2][0].color.val) )); 
		Log.d(Constants.TAG, String.format( "    |-------------------------|-------------------------|-------------------------|") );
		Log.d(Constants.TAG, String.format( " 1  |%s|%s|%s|", Util.dumpRGB(measuredColorArray[0][1], colorError[0][1]), Util.dumpRGB(measuredColorArray[1][1], colorError[1][1]), Util.dumpRGB(measuredColorArray[2][1], colorError[2][1]) )); 
		Log.d(Constants.TAG, String.format( " 1  |%s|%s|%s|", Util.dumpYUV(measuredColorArray[0][1]), Util.dumpYUV(measuredColorArray[1][1]), Util.dumpYUV(measuredColorArray[2][1]) )); 
		Log.d(Constants.TAG, String.format( " 1  |%s|%s|%s|", Util.dumpRGB(logicalTileArray[0][1]), Util.dumpRGB(logicalTileArray[1][1]), Util.dumpRGB(logicalTileArray[2][1]) )); 
		Log.d(Constants.TAG, String.format( " 1  |%s|%s|%s|", Util.dumpYUV(logicalTileArray[0][1].color.val), Util.dumpYUV(logicalTileArray[1][1].color.val), Util.dumpYUV(logicalTileArray[2][1].color.val) )); 
		Log.d(Constants.TAG, String.format( "    |-------------------------|-------------------------|-------------------------|") );
		Log.d(Constants.TAG, String.format( " 2  |%s|%s|%s|", Util.dumpRGB(measuredColorArray[0][2], colorError[0][2]), Util.dumpRGB(measuredColorArray[1][2], colorError[1][2]), Util.dumpRGB(measuredColorArray[2][2], colorError[2][2]) ));
		Log.d(Constants.TAG, String.format( " 2  |%s|%s|%s|", Util.dumpYUV(measuredColorArray[0][2]), Util.dumpYUV(measuredColorArray[1][2]), Util.dumpYUV(measuredColorArray[2][2]) ));
		Log.d(Constants.TAG, String.format( " 2  |%s|%s|%s|", Util.dumpRGB(logicalTileArray[0][2]), Util.dumpRGB(logicalTileArray[1][2]), Util.dumpRGB(logicalTileArray[2][2]) ));
		Log.d(Constants.TAG, String.format( " 2  |%s|%s|%s|", Util.dumpYUV(logicalTileArray[0][2].color.val), Util.dumpYUV(logicalTileArray[1][2].color.val), Util.dumpYUV(logicalTileArray[2][2].color.val) ));
		Log.d(Constants.TAG, String.format( "    |-------------------------|-------------------------|-------------------------|") );
		Log.d(Constants.TAG, "Total Color Error Before Correction: " + colorErrorBeforeCorrection);
		
		
		// Now compare Actual Luminous against expected luminous, and calculate an offset.
		// However, do not use Orange and Red because they are most likely to be miss-identified.
		// =+= TODO: Also, diminish weight on colors that are repeated.
		luminousOffset = 0.0;
		int count = 0;
		for(int n=0; n<3; n++) {
			for(int m=0; m<3; m++) {
				ConstantTile logicalTile = logicalTileArray[n][m];
				if(logicalTile.constantTileColor == ConstantTileColorEnum.RED || logicalTile.constantTileColor == ConstantTileColorEnum.ORANGE)
					continue;
				double measuredLuminousity = Util.getYUVfromRGB(measuredColorArray[n][m])[0];
				double expectedLuminousity = Util.getYUVfromRGB(logicalTile.color.val)[0];
				luminousOffset += (expectedLuminousity - measuredLuminousity);
				count++;
			}
		}
		luminousOffset = (count == 0) ? 0.0 : luminousOffset / count;
		Log.d(Constants.TAG, "Luminousity Offset: " + luminousOffset);
		
		// Second Pass: Find closest logical color using YUV but add luminousity offset to measured values.
		for(int n=0; n<3; n++) {
			for(int m=0; m<3; m++) {

				double [] measuredColor = measuredColorArray[n][m];
				double [] measuredColorYUV   = Util.getYUVfromRGB(measuredColor);

				double smallestError = Double.MAX_VALUE;
				ConstantTile bestCandidate = null;
				for(int i=0; i<6; i++) {
					ConstantTile candidateTile = Constants.constantTileColorArray[i];
					double[] candidateColorYUV = Util.getYUVfromRGB(candidateTile.color.val);

					// Only examine U and V axis, and not luminous.
					double error =
							(candidateColorYUV[0] - (measuredColorYUV[0] + luminousOffset)) * (candidateColorYUV[0] - (measuredColorYUV[0] + luminousOffset)) +
							(candidateColorYUV[1] -  measuredColorYUV[1]) * (candidateColorYUV[1] - measuredColorYUV[1]) +
							(candidateColorYUV[2] -  measuredColorYUV[2]) * (candidateColorYUV[2] - measuredColorYUV[2]);

					colorError[n][m] = Math.sqrt(error);

					if(error < smallestError) {
						bestCandidate = candidateTile;
						smallestError = error;
					}
				}

//				Log.d(Constants.TAG, String.format( "Tile[%d][%d] has R=%3.0f, G=%3.0f B=%3.0f %c err=%4.0f", n, m, measuredColor[0], measuredColor[1], measuredColor[2], bestCandidate.character, smallestError));

				if(bestCandidate != logicalTileArray[n][m]) {
					Log.i(Constants.TAG, String.format("Reclassiffying tile [%d][%d] from %c to %c", n, m, logicalTileArray[n][m].character, bestCandidate.character));
					logicalTileArray[n][m] = bestCandidate;
				}
			}
		}
		
		// Calculate and record LMS error (includeing LMS).
		for(int n=0; n<3; n++) {
			for(int m=0; m<3; m++) {
				double[] selectedColor = logicalTileArray[n][m].color.val;
				double[] measuredColor = measuredColorArray[n][m];
				colorErrorAfterCorrection += calculateColorError(selectedColor, measuredColor, true, luminousOffset);
			}
		}		
		
		Log.d(Constants.TAG, "Table: Measure RGB, Measure YUV, Logical RGB, Logical YUV");
		Log.d(Constants.TAG, String.format( " m:n|----------0--------------|-----------1-------------|---------2---------------|") );
		Log.d(Constants.TAG, String.format( " 0  |%s|%s|%s|", Util.dumpRGB(measuredColorArray[0][0], colorError[0][0]), Util.dumpRGB(measuredColorArray[1][0], colorError[1][0]), Util.dumpRGB(measuredColorArray[2][0], colorError[2][0]) )); 
		Log.d(Constants.TAG, String.format( " 0  |%s|%s|%s|", Util.dumpYUV(measuredColorArray[0][0]), Util.dumpYUV(measuredColorArray[1][0]), Util.dumpYUV(measuredColorArray[2][0]) )); 
		Log.d(Constants.TAG, String.format( " 0  |%s|%s|%s|", Util.dumpRGB(logicalTileArray[0][0]), Util.dumpRGB(logicalTileArray[1][0]), Util.dumpRGB(logicalTileArray[2][0]) )); 
		Log.d(Constants.TAG, String.format( " 0  |%s|%s|%s|", Util.dumpYUV(logicalTileArray[0][0].color.val), Util.dumpYUV(logicalTileArray[1][0].color.val), Util.dumpYUV(logicalTileArray[2][0].color.val) )); 
		Log.d(Constants.TAG, String.format( "    |-------------------------|-------------------------|-------------------------|") );
		Log.d(Constants.TAG, String.format( " 1  |%s|%s|%s|", Util.dumpRGB(measuredColorArray[0][1], colorError[0][1]), Util.dumpRGB(measuredColorArray[1][1], colorError[1][1]), Util.dumpRGB(measuredColorArray[2][1], colorError[2][1]) )); 
		Log.d(Constants.TAG, String.format( " 1  |%s|%s|%s|", Util.dumpYUV(measuredColorArray[0][1]), Util.dumpYUV(measuredColorArray[1][1]), Util.dumpYUV(measuredColorArray[2][1]) )); 
		Log.d(Constants.TAG, String.format( " 1  |%s|%s|%s|", Util.dumpRGB(logicalTileArray[0][1]), Util.dumpRGB(logicalTileArray[1][1]), Util.dumpRGB(logicalTileArray[2][1]) )); 
		Log.d(Constants.TAG, String.format( " 1  |%s|%s|%s|", Util.dumpYUV(logicalTileArray[0][1].color.val), Util.dumpYUV(logicalTileArray[1][1].color.val), Util.dumpYUV(logicalTileArray[2][1].color.val) )); 
		Log.d(Constants.TAG, String.format( "    |-------------------------|-------------------------|-------------------------|") );
		Log.d(Constants.TAG, String.format( " 2  |%s|%s|%s|", Util.dumpRGB(measuredColorArray[0][2], colorError[0][2]), Util.dumpRGB(measuredColorArray[1][2], colorError[1][2]), Util.dumpRGB(measuredColorArray[2][2], colorError[2][2]) ));
		Log.d(Constants.TAG, String.format( " 2  |%s|%s|%s|", Util.dumpYUV(measuredColorArray[0][2]), Util.dumpYUV(measuredColorArray[1][2]), Util.dumpYUV(measuredColorArray[2][2]) ));
		Log.d(Constants.TAG, String.format( " 2  |%s|%s|%s|", Util.dumpRGB(logicalTileArray[0][2]), Util.dumpRGB(logicalTileArray[1][2]), Util.dumpRGB(logicalTileArray[2][2]) ));
		Log.d(Constants.TAG, String.format( " 2  |%s|%s|%s|", Util.dumpYUV(logicalTileArray[0][2].color.val), Util.dumpYUV(logicalTileArray[1][2].color.val), Util.dumpYUV(logicalTileArray[2][2].color.val) ));
		Log.d(Constants.TAG, String.format( "    |-------------------------|-------------------------|-------------------------|") );

		Log.d(Constants.TAG, "Color Error After Correction: " + colorErrorAfterCorrection);

	}




	/**
	 * Calculate Color Error
	 * 
	 * Return distance between two colors.
	 * 
	 * @param slected
	 * @param measured
	 * @param useLuminous
	 * @param _luminousOffset 
	 * @return
	 */
	private double calculateColorError(double[] slected, double[] measured, boolean useLuminous, double _luminousOffset) {
		double error =
				(slected[0] - (measured[0] + _luminousOffset)) * (slected[0] - (measured[0] + _luminousOffset) ) +
				(slected[1] - measured[1]) * (slected[1] - measured[1]) +
				(slected[2] - measured[2]) * (slected[2] - measured[2]);
		return Math.sqrt(error);
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
	 * Draw
	 * 
	 * Draw annotated diagnostic information
	 * 
	 * @param img
	 * @param accepted 
	 */
	public void drawRubikCubeOverlay(Mat img, boolean accepted) {
		
		Scalar color = Constants.ColorBlack;
		switch(faceRecognitionStatus) {
		case UNKNOWN:
		case INSUFFICIENT:
		case INVALID_MATH:
			color = Constants.ColorRed;
			break;
		case BAD_METRICS:
		case INCOMPLETE:
		case INADEQUATE:
		case BLOCKED:
		case UNSTABLE:
			color = Constants.ColorOrange;
			break;
		case SOLVED:
			color = accepted ? Constants.ColorGreen : Constants.ColorYellow;
			break;
		}
		
		// Adjust drawing grid to start at edge of cube and not center of a tile.
		double x = lmsResult.origin.x - (alphaLatticLength * Math.cos(alphaAngle) + betaLatticLength * Math.cos(betaAngle) ) / 2;
		double y = lmsResult.origin.y - (alphaLatticLength * Math.sin(alphaAngle) + betaLatticLength * Math.sin(betaAngle) ) / 2;

		for(int n=0; n<4; n++) {
			Core.line(
					img,
					new Point(
							x + n * alphaLatticLength * Math.cos(alphaAngle),
							y + n * alphaLatticLength * Math.sin(alphaAngle) ), 
					new Point(
							x + (betaLatticLength * 3 * Math.cos(betaAngle)) + (n * alphaLatticLength * Math.cos(alphaAngle) ),
							y + (betaLatticLength * 3 * Math.sin(betaAngle)) + (n * alphaLatticLength * Math.sin(alphaAngle) ) ), 
					color, 
					3);
		}
		
		for(int m=0; m<4; m++) {
			Core.line(
					img,
					new Point(
							x + m * betaLatticLength * Math.cos(betaAngle),
							y + m * betaLatticLength * Math.sin(betaAngle) ), 
					new Point(
							x + (alphaLatticLength * 3 * Math.cos(alphaAngle)) + (m * betaLatticLength * Math.cos(betaAngle) ),
							y + (alphaLatticLength * 3 * Math.sin(alphaAngle)) + (m * betaLatticLength * Math.sin(betaAngle) ) ), 
					color, 
					3);
		}
		
//		// Draw a circule at the Rhombus reported center of each tile.
//		for(int n=0; n<3; n++) {
//			for(int m=0; m<3; m++) {
//				Rhombus rhombus = faceRhombusArray[n][m];
//				if(rhombus != null)
//					Core.circle(img, rhombus.center, 5, Constants.ColorBlue, 3);
//			}
//		}
//		
//		// Draw the error vector from center of tile to actual location of Rhombus.
//		for(int n=0; n<3; n++) {
//			for(int m=0; m<3; m++) {
//				Rhombus rhombus = faceRhombusArray[n][m];
//				if(rhombus != null) {
//					
//					Point tileCenter = getTileCenterInPixels(n, m);				
//					Core.line(img, tileCenter, rhombus.center, Constants.ColorRed, 3);
//					Core.circle(img, tileCenter, 5, Constants.ColorBlue, 1);
//				}
//			}
//		}
		
		// Draw reported Logical Tile Color Characters in center of each tile.
		if(faceRecognitionStatus == FaceRecognitionStatusEnum.SOLVED)
			for(int n=0; n<3; n++) {
				for(int m=0; m<3; m++) {

					// Draw tile character in UV plane
					Point tileCenterInPixels = getTileCenterInPixels(n, m);
					tileCenterInPixels.x -= 10.0;
					tileCenterInPixels.y += 10.0;
					String text = Character.toString(logicalTileArray[n][m].character);
					Core.putText(img, text, tileCenterInPixels, Constants.FontFace, 3, Constants.ColorBlack, 3);
				}
			}
		
		// Also draw recognized Rhombi for clarity.
		if(faceRecognitionStatus != FaceRecognitionStatusEnum.SOLVED)
			for(Rhombus rhombus : rhombusList)
				rhombus.draw(img, Constants.ColorGreen);
	}




	public void renderFaceRecognitionMetrics(Mat image) {
		
//		Core.rectangle(image, new Point(0, 0), new Point(500, 720), Constants.ColorBlack, -1);
		
		drawFlatFaceRepresentation(image, this, 50, 50, 50);

		Core.putText(image, "Status = " + faceRecognitionStatus,                              new Point(50, 300), Constants.FontFace, 2, Constants.ColorWhite, 2);
		Core.putText(image, String.format("AlphaA = %4.1f", alphaAngle * 180.0 / Math.PI),    new Point(50, 350), Constants.FontFace, 2, Constants.ColorWhite, 2);
		Core.putText(image, String.format("BetaA  = %4.1f", betaAngle  * 180.0 / Math.PI),    new Point(50, 400), Constants.FontFace, 2, Constants.ColorWhite, 2);
		Core.putText(image, String.format("AlphaL = %4.0f", alphaLatticLength),               new Point(50, 450), Constants.FontFace, 2, Constants.ColorWhite, 2);
		Core.putText(image, String.format("Beta L = %4.0f", betaLatticLength),                new Point(50, 500), Constants.FontFace, 2, Constants.ColorWhite, 2);
		Core.putText(image, String.format("Gamma  = %4.2f", gammaRatio),                      new Point(50, 550), Constants.FontFace, 2, Constants.ColorWhite, 2);
		Core.putText(image, String.format("Sigma  = %5.0f", lmsResult.sigma),                 new Point(50, 600), Constants.FontFace, 2, Constants.ColorWhite, 2);
		Core.putText(image, String.format("Moves  = %d",    numRhombusMoves),                 new Point(50, 650), Constants.FontFace, 2, Constants.ColorWhite, 2);
		Core.putText(image, String.format("#Rohmbi= %d",    rhombusList.size()),              new Point(50, 700), Constants.FontFace, 2, Constants.ColorWhite, 2);
	}
	
	
	
	/**
	 * Draw Flat Face Representation
	 * 
	 * This depicts the faces as recognized, but without any rotation.
	 * That is, rendering orientates faces with respect to N and M axis.
	 * 
	 * @param image
	 * @param tSize 
	 */
	public static void drawFlatFaceRepresentation(Mat image, DeprecatedRubikFace rubikFace, double x, double y, int tSize) {
		
		if(rubikFace == null) {
			Core.rectangle(image, new Point( x, y), new Point( x + 3*tSize, y + 3*tSize), Constants.ColorGrey, -1);
		}

		else if(rubikFace.faceRecognitionStatus != FaceRecognitionStatusEnum.SOLVED) {
			Core.rectangle(image, new Point( x, y), new Point( x + 3*tSize, y + 3*tSize), Constants.ColorGrey, -1);
		}
		else

			for(int n=0; n<3; n++) {
				for(int m=0; m<3; m++) {
					ConstantTile logicalTile = rubikFace.logicalTileArray[n][m];
					if(logicalTile != null)
						Core.rectangle(image, new Point( x + tSize * n, y + tSize * m), new Point( x + tSize * (n + 1), y + tSize * (m + 1)), logicalTile.color, -1);//Core.CV_FILLED);
				}
			}
	}

	
	/**
	 * Draw Logical Flat Face Representation
	 * 
	 * This depicts the face with provide new arrangement of tile, 
	 * typically a rotation.
	 * 
	 * @param image
	 * @param tSize 
	 */
	public static void drawLogicalFlatFaceRepresentation(Mat image, DeprecatedRubikFace rubikFace, ConstantTile[][] array, double x, double y, int tSize) {
		
		if(rubikFace == null) {
			Core.rectangle(image, new Point( x, y), new Point( x + 3*tSize, y + 3*tSize), Constants.ColorGrey, -1);
		}

		else if(rubikFace.faceRecognitionStatus != FaceRecognitionStatusEnum.SOLVED) {
			Core.rectangle(image, new Point( x, y), new Point( x + 3*tSize, y + 3*tSize), Constants.ColorGrey, -1);
		}
		else

			for(int n=0; n<3; n++) {
				for(int m=0; m<3; m++) {
					ConstantTile logicalTile = array[n][m];
					if(logicalTile != null)
						Core.rectangle(image, new Point( x + tSize * n, y + tSize * m), new Point( x + tSize * (n + 1), y + tSize * (m + 1)), logicalTile.color, -1);//Core.CV_FILLED);
				}
			}
	}
	

	
	
	
	
	
	

	private String dumpLoc(Rhombus rhombus) {
		if(rhombus == null)
			return "           ";
		else
			return String.format(" %4.0f,%4.0f ", rhombus.center.x, rhombus.center.y);
	}
	
	private Point getTileCenterInPixels(int n, int m) {
		return new Point(
				lmsResult.origin.x + n * alphaLatticLength * Math.cos(alphaAngle) + m * betaLatticLength * Math.cos(betaAngle),
				lmsResult.origin.y + n * alphaLatticLength * Math.sin(alphaAngle) + m * betaLatticLength * Math.sin(betaAngle)	);
	}
	private String dumpPoint(Point point) {
		if(point == null)
			return "           ";
		else
			return String.format(" %4.0f,%4.0f ", point.x, point.y);
	}




	/**
	 * Render Color Metrics
	 * 
	 *  Y is on vertical on left side
	 *  U is on X axis  blue
	 *  V ix on Y axis  red
	 * @param image
	 */
	public void renderColorMetrics(Mat image) {

		if(faceRecognitionStatus != FaceRecognitionStatusEnum.SOLVED)
			return;

		// Draw simple grid
		Core.rectangle(image, new Point(-256 + 256, -256 + 400), new Point(256 + 256, 256 + 400), Constants.ColorWhite);
		Core.line(image, new Point(0 + 256, -256 + 400), new Point(0 + 256, 256 + 400), Constants.ColorWhite);		
		Core.line(image, new Point(-256 + 256, 0 + 400), new Point(256 + 256, 0 + 400), Constants.ColorWhite);
		Core.putText(image, String.format("Luminosity Offset = %4.0f", luminousOffset), new Point(0, -256 + 400 - 60), Constants.FontFace, 2, Constants.ColorWhite, 2);
		Core.putText(image, String.format("Color Error Before Corr = %4.0f", colorErrorBeforeCorrection), new Point(0, -256 + 400 - 30), Constants.FontFace, 2, Constants.ColorWhite, 2);
		Core.putText(image, String.format("Color Error After Corr = %4.0f", colorErrorAfterCorrection), new Point(0, -256 + 400), Constants.FontFace, 2, Constants.ColorWhite, 2);

		for(int n=0; n<3; n++) {
			for(int m=0; m<3; m++) {

				double [] measuredTileColor = measuredColorArray[n][m];
//				Log.e(Constants.TAG, "RGB: " + logicalTileArray[n][m].character + "=" + actualTileColor[0] + "," + actualTileColor[1] + "," + actualTileColor[2] + " x=" + x + " y=" + y );
				double[] measuredTileColorYUV   = Util.getYUVfromRGB(measuredTileColor);

//				if(measuredTileColor == null)
//					return;

//				Log.e(Constants.TAG, "Lum: " + logicalTileArray[n][m].character + "=" + acutalTileYUV[0]);

				
				double luminousScaled     = measuredTileColorYUV[0] * 2 - 256;
				double uChromananceScaled = measuredTileColorYUV[1] * 2;
				double vChromananceScaled = measuredTileColorYUV[2] * 2;

				String text = Character.toString(logicalTileArray[n][m].character);
				
				// Draw tile character in UV plane
				Core.putText(image, text, new Point(uChromananceScaled + 256, vChromananceScaled + 400), Constants.FontFace, 3, logicalTileArray[n][m].color, 3);
				
				// Draw tile characters on right side for Y axis
				Core.putText(image, text, new Point(512 - 40, luminousScaled + 400 + luminousOffset), Constants.FontFace, 3, logicalTileArray[n][m].color, 3);
				Core.putText(image, text, new Point(512 + 20, luminousScaled + 400), Constants.FontFace, 3, logicalTileArray[n][m].color, 3);
//				Log.e(Constants.TAG, "Lum: " + logicalTileArray[n][m].character + "=" + luminousScaled);
			}
		}

		Scalar rubikRed    = Constants.constantTileColorArray[ConstantTileColorEnum.RED.ordinal()].color;
		Scalar rubikOrange = Constants.constantTileColorArray[ConstantTileColorEnum.ORANGE.ordinal()].color;
		Scalar rubikYellow = Constants.constantTileColorArray[ConstantTileColorEnum.YELLOW.ordinal()].color;
		Scalar rubikGreen  = Constants.constantTileColorArray[ConstantTileColorEnum.GREEN.ordinal()].color;
		Scalar rubikBlue   = Constants.constantTileColorArray[ConstantTileColorEnum.BLUE.ordinal()].color;
		Scalar rubikWhite  = Constants.constantTileColorArray[ConstantTileColorEnum.WHITE.ordinal()].color;

		
		// Render Color Calibration in UV plane as dots
		Core.circle(image, new Point(2*Util.getYUVfromRGB(rubikRed.val)[1] +    256, 2*Util.getYUVfromRGB(rubikRed.val)[2] + 400), 10, rubikRed, -1);
		Core.circle(image, new Point(2*Util.getYUVfromRGB(rubikOrange.val)[1] + 256, 2*Util.getYUVfromRGB(rubikOrange.val)[2] + 400), 10, rubikOrange, -1);
		Core.circle(image, new Point(2*Util.getYUVfromRGB(rubikYellow.val)[1] + 256, 2*Util.getYUVfromRGB(rubikYellow.val)[2] + 400), 10, rubikYellow, -1);
		Core.circle(image, new Point(2*Util.getYUVfromRGB(rubikGreen.val)[1] +  256, 2*Util.getYUVfromRGB(rubikGreen.val)[2] + 400), 10, rubikGreen, -1);
		Core.circle(image, new Point(2*Util.getYUVfromRGB(rubikBlue.val)[1] +   256, 2*Util.getYUVfromRGB(rubikBlue.val)[2] + 400), 10, rubikBlue, -1);
		Core.circle(image, new Point(2*Util.getYUVfromRGB(rubikWhite.val)[1] +  256, 2*Util.getYUVfromRGB(rubikWhite.val)[2] + 400), 10, rubikWhite, -1);

		// Render Color Calibration on right side Y axis as dots
		Core.line(image, new Point(502, -256 + 2*Util.getYUVfromRGB(rubikRed.val)[0] + 400),    new Point(522, -256 + 2*Util.getYUVfromRGB(rubikRed.val)[0] + 400), rubikRed, 3);
		Core.line(image, new Point(502, -256 + 2*Util.getYUVfromRGB(rubikOrange.val)[0] + 400), new Point(522, -256 + 2*Util.getYUVfromRGB(rubikOrange.val)[0] + 400), rubikOrange, 3);
		Core.line(image, new Point(502, -256 + 2*Util.getYUVfromRGB(rubikGreen.val)[0] + 400),  new Point(522, -256 + 2*Util.getYUVfromRGB(rubikGreen.val)[0] + 400), rubikGreen, 3);
		Core.line(image, new Point(502, -256 + 2*Util.getYUVfromRGB(rubikYellow.val)[0] + 400), new Point(522, -256 + 2*Util.getYUVfromRGB(rubikYellow.val)[0] + 400), rubikYellow, 3);
		Core.line(image, new Point(502, -256 + 2*Util.getYUVfromRGB(rubikBlue.val)[0] + 400),   new Point(522, -256 + 2*Util.getYUVfromRGB(rubikBlue.val)[0] + 400), rubikBlue, 3);
		Core.line(image, new Point(502, -256 + 2*Util.getYUVfromRGB(rubikWhite.val)[0] + 400),  new Point(522, -256 + 2*Util.getYUVfromRGB(rubikWhite.val)[0] + 400), rubikWhite, 3);
	}



	public boolean nearlyIdentical(DeprecatedRubikFace lastRubikFace) {
		// TODO Auto-generated method stub
		return false;
	}
}