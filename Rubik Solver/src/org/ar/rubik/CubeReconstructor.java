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
 *   Cube location and orientation in GL space coordinates are reconstructed from Face information.
 *   
 *   The Rubik Cube is defined as a cube centered a the origin
 *   
 *   This class implements the OpenCV Frame Listener. 
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

import java.util.ArrayList;
import java.util.List;

import org.ar.rubik.RubikFace.FaceRecognitionStatusEnum;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point3;
import org.opencv.core.Point;
import android.util.Log;



/**
 * 
 * 
 * @author android.steve@testlens.com
 *
 */
public class CubeReconstructor {
	
	// Translation and Rotation as computed from OpenCV Pose Estimator
	public float x2;
	public float y2;
	public float z2;
	public float cubeXrotation2;  // degrees
	public float cubeYrotation2;  // degrees
	public float cubeZrotation2;  // degrees
	
	
	public float scale;
	public float x;
	public float y;
	public float cubeYrotation;  // degrees
	public float cubeXrotation;  // degrees
	
//	private Point3[][] topFaceTileCentersInGLSpace = {
//	        { new Point3(+0.666f, +1.0f, +0.666), new Point3(0.000f, +1.0f, +0.666), new Point3(-0.666f, +1.0f, +0.666) },
//	        { new Point3(+0.666f, +1.0f, +0.000), new Point3(0.000f, +1.0f, +0.000), new Point3(-0.666f, +1.0f, +0.000) },
//	        { new Point3(+0.666f, +1.0f, -0.666), new Point3(0.000f, +1.0f, -0.666), new Point3(-0.666f, +1.0f, -0.666) } };


	/**
	 * =+= this is temporary code
	 * 
	 * This function actually calculates, currently rather crudely, a 2D to 3D translation.
	 * That is, information from the Rubik Face object is used to deduce the 
	 * true location in OpenGL space of the cube and it's orientation.  
	 * 
	 * @param rubikFace
	 */
    public void reconstruct(RubikFace rubikFace) {
    	
		final float opecnCL2opencvRatio = 100.0f;
		final float xOffset = 650.0f;
		final float yOffset = 200.0f;
		
		if(rubikFace == null)
			return;
		
		if(rubikFace.faceRecognitionStatus != FaceRecognitionStatusEnum.SOLVED)
			return;
		
		LeastMeansSquare lmsResult = rubikFace.lmsResult;
		
		if(lmsResult == null)
			return;
		
				
		// This is very crude.
		this.scale = (float) Math.sqrt(Math.abs(rubikFace.alphaLatticLength * rubikFace.betaLatticLength)) / 70.0f;
		
		// =+= not necessarily correct, really should use X, Y rotations
		this.x = (float) ((lmsResult.origin.x - xOffset) / opecnCL2opencvRatio);
		this.y = (float) (-1 * (lmsResult.origin.y - yOffset) / opecnCL2opencvRatio);
		
		float alpha = 90.0f - (float) (rubikFace.alphaAngle * 180.0 / Math.PI);
		float beta = (float) (rubikFace.betaAngle * 180.0 / Math.PI) - 90.0f;
		
		
		// Very crude estimations of orientation.  These equations and number found empirically.
		// =+= We require a solution of two non-linear equations and two unknowns to correctly calculate
		// =+= X and Y 3D rotation values from 2D alpha and beta values.  Probably use of Newton successive
		// =+= approximation will produce good results.
		this.cubeYrotation = 45.0f + (alpha - beta) / 2.0f;
		this.cubeXrotation =  90.0f + ( (alpha - 45.0f) + (beta - 45.0f) )/ -0.5f;
		
//		reconstruct2(rubikFace);
    }
    
    
    /**
     * 
     * 
     * 
     * @param rubikFace
     * @param image 
     * @param stateModel 
     */
    public void reconstruct2(RubikFace rubikFace, Mat image, StateModel stateModel) {
    	
		if(rubikFace == null)
			return;
		
		if(rubikFace.faceRecognitionStatus != FaceRecognitionStatusEnum.SOLVED)
			return;
		
		LeastMeansSquare lmsResult = rubikFace.lmsResult;
		
		if(lmsResult == null)
			return;
		
		// OpenCV Pose Estimate requires at least four points.
		if(rubikFace.rhombusList.size() <= 4)
			return;
		

    	List<Point3> objectPointsList     = new ArrayList<Point3>(9);
		List<Point> imagePointsList      = new ArrayList<Point>(9);
		
		
		// Create list of image (in 2D) and object (in 3D) points.
		// Loop over Rubik Face Tiles/Rhombi
    	for(int n = 0; n<3; n++) {
    		for(int m=0; m<3; m++) {
    			
    			Rhombus rhombus = rubikFace.faceRhombusArray[n][m];
    			
    			// Only use if Rhombus was non null.
    			if(rhombus != null) {

    				// Obtain center of Rhombus in screen image coordinates
    				Point imagePoint = new Point( rhombus.center.x, rhombus.center.y);
    				imagePointsList.add(imagePoint);
    				
    				// N and M are actual not conceptual (as in design doc).
    				int mm = 2 - n;
    				int nn = 2 - m;
    				// above now matches design doc.
    				
    				// Calculate center of Tile in OpenGL World Space Coordinates
    				float x  = (1 - mm) * 0.66666f;
    				float y  = +1.0f;
    				float z  = (1 - nn) * 0.666666f;
    				Point3 objectPoint = new Point3(x, y, z);
    				objectPointsList.add(objectPoint);
    			}
    		}
    	}
		
    	// Cast image point list into OpenCV Matrix.
		MatOfPoint2f imagePoints  = new MatOfPoint2f();
		imagePoints.fromList(imagePointsList);
		
		// Cast object point list into OpenCV Matrix.
		MatOfPoint3f objectPoints = new MatOfPoint3f();
		objectPoints.fromList(objectPointsList);

		Mat cameraMatrix          = new Mat(3, 3, CvType.CV_64FC1);
		cameraMatrix.put(0, 0, stateModel.cameraParameters.focalLengthPixels);
		cameraMatrix.put(0, 1, 0.0);
		cameraMatrix.put(0, 2, 1280.0/2.0);
		cameraMatrix.put(1, 0, 0.0);
		cameraMatrix.put(1, 1, stateModel.cameraParameters.focalLengthPixels);
		cameraMatrix.put(1, 2, 720.0/2.0);
		cameraMatrix.put(2, 0, 0.0);
		cameraMatrix.put(2, 1, 0.0);
		cameraMatrix.put(2, 2, 1.0);

				
				
		MatOfDouble distCoeffs    = new MatOfDouble();
		Mat rvec                  = new Mat();
		Mat tvec                  = new Mat();	
		

//		Log.e(Constants.TAG, "Image Points: " + imagePoints.dump());
//		Log.e(Constants.TAG, "Object Points: " + objectPoints.dump());
		
		boolean result = Calib3d.solvePnP(objectPoints, imagePoints, cameraMatrix, distCoeffs, rvec, tvec);
		
		x2 = (float) tvec.get(0, 0)[0];
		y2 = (float) tvec.get(1, 0)[0];
		z2 = (float) tvec.get(2, 0)[0];
		cubeXrotation2 = (float) rvec.get(0, 0)[0];
		cubeYrotation2 = (float) rvec.get(1, 0)[0];
		cubeZrotation2 = (float) rvec.get(2, 0)[0];
		
    	
//		Log.e(Constants.TAG, "Result: " + result);
//		Log.e(Constants.TAG, "Camera: " + cameraMatrix.dump());
//		Log.e(Constants.TAG, "Rotation: " + rvec.dump());
//		Log.e(Constants.TAG, "Translation: " + tvec.dump());
		
		
		Core.rectangle(image, new Point(0, 50), new Point(1270, 150), Constants.ColorBlack, -1);
		Core.putText(image, String.format("Translation  x=%4.2f y=%4.2f z=%4.2f", tvec.get(0, 0)[0], tvec.get(1, 0)[0], tvec.get(2, 0)[0]), new Point(50, 100), Constants.FontFace, 3, Constants.ColorWhite, 3);
		double toDeg = 180.0 / Math.PI;
		Core.putText(image, String.format("Rotation     x=%4.0f y=%4.0f z=%4.0f", rvec.get(0, 0)[0] * toDeg, rvec.get(1, 0)[0] * toDeg, rvec.get(2, 0)[0] * toDeg), new Point(50, 150), Constants.FontFace, 3, Constants.ColorWhite, 3);

    }

}
