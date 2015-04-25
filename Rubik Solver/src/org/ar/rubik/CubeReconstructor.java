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
 *   Cube location and orientation in GL space coordinates are reconstructed from Face information.
 *   The Rubik Cube is defined as a cube centered a the origin with edge length of 2.0 units.
 *   
 * Accuracy
 *   Despite code seeming to be correct, we still observe some perceived error in the cube 
 *   overlay rending to actual rubik cube.   Six parameters (3 rotation and 3 translation) 
 *   fudge factors allows offsets to be applied to Pose Estimator solution.  Possible
 *   root causes of this error are:
 *   o  Intrinsic camera calibration parameters not accurate: these are being obtained from Android.
 *   o  Assumption that camera distortion can be ignored.
 *   o  Center of tile calculations: perhaps use first-order momentum on curve.
 *   o  Camera Perspective and/or Rendering Perspective not in agreement.
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
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point3;
import org.opencv.core.Point;

import android.util.Log;



/**
 * Class Cube Reconstructor
 * 
 *   Cube location and orientation in GL space coordinates are reconstructed from Face information.
 *   The Rubik Cube is defined as a cube centered a the origin with edge length of 2.0 units.
 * 
 * @author android.steve@cl-sw.com
 *
 */
public class CubeReconstructor {
	
	// Translation and Rotation as computed from OpenCV Pose Estimator
    // but expressed in OpenGL World Coordinate System.
	public float x;  // real world units
	public float y;  // real world units
	public float z;  // real world units
	
	// Open GL Rotation Matrix that expresses object to world rotation
    public float[] poseRotationMatrix = new float[16];
    
    
    /**
     * Pose Estimation
     * 
     * Deduce real world cube coordinates and rotation
     * 
     * @param rubikFace
     * @param image 
     * @param stateModel 
     */
    public void poseEstimation(RubikFace rubikFace, Mat image, StateModel stateModel) {
    	
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
		
		// List of real world point and screen points that correspond.
    	List<Point3> objectPointsList    = new ArrayList<Point3>(9);
		List<Point> imagePointsList      = new ArrayList<Point>(9);
		
		
		// Create list of image (in 2D) and object (in 3D) points.
		// Loop over Rubik Face Tiles/Rhombi
    	for(int n = 0; n<3; n++) {
    		for(int m=0; m<3; m++) {
    			
    			Rhombus rhombus = rubikFace.faceRhombusArray[n][m];
    			
    			// Only use if Rhombus was non null.
    			if(rhombus != null) {

    				// Obtain center of Rhombus in screen image coordinates
    			    // Convention:
    			    //  o X is zero on the left, and increases to the right.
    			    //  o Y is zero on the top and increases downward.
    				Point imagePoint = new Point( rhombus.center.x, rhombus.center.y);
    				imagePointsList.add(imagePoint);
    				
    				// N and M are actual not conceptual (as in design doc).
    				int mm = 2 - n;
    				int nn = 2 - m;
    				// above now matches design doc.
    				// that is:
    				//  o the nn vector is to the right and upwards.
    				//  o the mm vector is to the left and upwards.
    				
    				// Calculate center of Tile in OpenCV World Space Coordinates
    				// Convention:
                    //  o X is zero in the center, and increases to the left.
                    //  o Y is zero in the center and increases downward.
    				//  o Z is zero (at the world coordinate origin) and increase away for the camera.
    				float x  = (1 - mm) * 0.66666f;
    				float y  = -1.0f;
    				float z  = -1.0f * (1 - nn) * 0.666666f;
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

		Mat cameraMatrix          = stateModel.cameraParameters.getOpenCVCameraMatrix();
		MatOfDouble distCoeffs    = new MatOfDouble();
		Mat rvec                  = new Mat();
		Mat tvec                  = new Mat();	
		

//		Log.e(Constants.TAG, "Image Points: " + imagePoints.dump());
//		Log.e(Constants.TAG, "Object Points: " + objectPoints.dump());
		
//		=+= sometimes a "count >= 4" exception 
		Calib3d.solvePnP(objectPoints, imagePoints, cameraMatrix, distCoeffs, rvec, tvec);
		
	    Log.v(Constants.TAG, String.format("Open CV Rotation Vector x=%4.2f y=%4.2f z=%4.2f", rvec.get(0, 0)[0], rvec.get(1, 0)[0], rvec.get(2, 0)[0] ));
		
		// Convert from OpenCV to OpenGL World Coordinates
		x = +1.0f * (float) tvec.get(0, 0)[0];
		y = -1.0f * (float) tvec.get(1, 0)[0];
		z = -1.0f * (float) tvec.get(2, 0)[0];
		
        // =+= Add manual offset correction to translation  
        x += MenuAndParams.xTranslationOffsetParam.value;
        y += MenuAndParams.yTranslationOffsetParam.value;
        z += MenuAndParams.zTranslationOffsetParam.value;		
		
		
	    // Convert Rotation Vector from OpenCL polarity axes definition to OpenGL definition
        rvec.put(1, 0, -1.0f * rvec.get(1, 0)[0]);
        rvec.put(2, 0, -1.0f * rvec.get(2, 0)[0]);
 
        // =+= Add manual offset correction to Rotation
        rvec.put(0, 0, rvec.get(0, 0)[0] + MenuAndParams.xRotationOffsetParam.value * Math.PI / 180.0);  // X rotation
        rvec.put(1, 0, rvec.get(1, 0)[0] + MenuAndParams.yRotationOffsetParam.value * Math.PI / 180.0);  // Y rotation
        rvec.put(2, 0, rvec.get(2, 0)[0] + MenuAndParams.zRotationOffsetParam.value * Math.PI / 180.0);  // Z rotation
        
		// Create an OpenCV Rotation Matrix from a Rotation Vector
		Mat rMatrix = new Mat(4, 4, CvType.CV_32FC2);
		Calib3d.Rodrigues(rvec, rMatrix);
		Log.v(Constants.TAG, "Rodrigues Matrix: " + rMatrix.dump());


		/*
		 * Create an OpenGL Rotation Matrix
		 * Notes:
		 *   o  OpenGL is in column-row order (correct?).
		 *   o  OpenCV Rodrigues Rotation Matrix is 3x3 where OpenGL Rotation Matrix is 4x4.
		 */

        // Initialize all Rotational Matrix elements to zero.
		for(int i=0; i<16; i++)
		    poseRotationMatrix[i] = 0.0f; // Initialize to zero

		// Initialize element [3,3] to 1.0: i.e., "w" component in homogenous coordinates
        poseRotationMatrix[3*4 + 3] = 1.0f;

        // Copy OpenCV matrix to OpenGL matrix element by element.
        for(int r=0; r<3; r++)
            for(int c=0; c<3; c++)
                poseRotationMatrix[r + c*4] = (float)(rMatrix.get(r, c)[0]);
        
        // Diagnostics
        for(int r=0; r<4; r++)
            Log.v(Constants.TAG, String.format("Rotation Matrix  r=%d  [%5.2f  %5.2f  %5.2f  %5.2f]", r, poseRotationMatrix[r + 0], poseRotationMatrix[r+4], poseRotationMatrix[r+8], poseRotationMatrix[r+12]));

		
//		Log.e(Constants.TAG, "Result: " + result);
//		Log.e(Constants.TAG, "Camera: " + cameraMatrix.dump());
//		Log.e(Constants.TAG, "Rotation: " + rvec.dump());
//		Log.e(Constants.TAG, "Translation: " + tvec.dump());
		
//		// Reporting in OpenGL World Coordinates
//		Core.rectangle(image, new Point(0, 50), new Point(1270, 150), Constants.ColorBlack, -1);
//		Core.putText(image, String.format("Translation  x=%4.2f y=%4.2f z=%4.2f", x, y, z), new Point(50, 100), Constants.FontFace, 3, Constants.ColorWhite, 3);
//		Core.putText(image, String.format("Rotation     x=%4.0f y=%4.0f z=%4.0f", cubeXrotation, cubeYrotation, cubeZrotation), new Point(50, 150), Constants.FontFace, 3, Constants.ColorWhite, 3);
    }

}
