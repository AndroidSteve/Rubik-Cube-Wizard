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

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.util.Log;

/**
 * @author android.steve@cl-sw.com
 * 
 * =+= Important Issue: viewing angle has a factor of 2 w.r.t. trig functions.
 *
 */
public class CameraCalibration {
	
	public double focalLengthPixels;
	
	// Android Camera Parameters
    private Parameters parameters;
    
    // Image size.  =+= Will this always be the same as onFrame() arg?
    private Size size;

    // Image width in pixels
    private int widthPixels;

    // Image height in pixels
    private int heightPixels;

    // Field of horizontal axis view in radians
    private float fovX;
    
    // Field of vertical axis view in radians
    private float fovY;

//    private float projectionFieldOfView;

//    private int projectionAspect;

	/**
	 * Camera Parameters Constructor
	 */
	public CameraCalibration() {
		
		Camera camera = Camera.open();
		parameters = camera.getParameters();
		camera.release();
		
		size = parameters.getPictureSize();
        widthPixels = size.width;
        heightPixels = size.height;
        
        Log.v(Constants.TAG_CAL, "Reported image size from camera parameters: width=" + size.width + " height=" + size.height);
        
//        // =+= above produces camera dimensions.  Below is screen dimensions.
//        // =+= we have some confusions as to when to use which.
//        widthPixels = 1280;
//        heightPixels = 720;

		// Will be in radians
		fovY = parameters.getVerticalViewAngle() * (float)(Math.PI / 180.0);
		fovX = parameters.getHorizontalViewAngle() * (float)(Math.PI / 180.0);
		
//		// Will be in degrees
//		projectionFieldOfView = parameters.getVerticalViewAngle();
//		projectionAspect = widthPixels / heightPixels;

//		// Make calculations across diagonal
//		double diagonalPixels = Math.sqrt( widthPixels * widthPixels + heightPixels * heightPixels);
//		double diagnoalFOV = Math.sqrt(fovX * fovX + fovY * fovY);
//		
//		// =+= Why?  The formula obtained from "Android Applications Programming with OpenCV" book.
//		focalLengthPixels = diagonalPixels / ( 2.0 * Math.tan(0.5 * diagnoalFOV));

//      Log.e(Constants.TAG, "Width = " + widthPixels + " Height = " + heightPixels);  // 1920 by 1080 reported.
//      Log.e(Constants.TAG, "dPOV=" + diagnoalFOV + " dPx=" + diagonalPixels);
//		Log.e(Constants.TAG, "Camera Focal Length in Pixels Calibration: " + focalLengthPixels);
	}
	
	
	/**
	 * Get OpenCV Camera Matrix
	 * 
	 * This matrix represents the intrinsic properties of the camera.
	 * Matrix is basically:
	 * 
	 *    |   Fx    0    Cx  |
	 *    |    0   Fy    Cy  |
	 *    |    0    0     1  |
	 *    
	 *    Fx := X Focal Length
	 *    Fy := Y Focal Length
	 *    Cx := X Optical Center
	 *    Cy := Y Optical Center
	 * 
	 * =+= NOTE: Screen dimensions used below.  However, 
	 * 
	 * @return
	 */
	public Mat getOpenCVCameraMatrix () {

	    double focalLengthXPixels = widthPixels / ( 2.0 * Math.tan(0.5 * fovX));
	    double focalLengthYPixels = heightPixels / ( 2.0 * Math.tan(0.5 * fovY));

	    Mat cameraMatrix          = new Mat(3, 3, CvType.CV_64FC1);
	    cameraMatrix.put(0, 0, focalLengthXPixels);   // should be X focal length in pixels.
	    cameraMatrix.put(0, 1, 0.0);
	    cameraMatrix.put(0, 2, widthPixels/2.0);
	    cameraMatrix.put(1, 0, 0.0);
	    cameraMatrix.put(1, 1, focalLengthYPixels);  // should be Y focal length in pixels.
	    cameraMatrix.put(1, 2, heightPixels/2.0);
	    cameraMatrix.put(2, 0, 0.0);
	    cameraMatrix.put(2, 1, 0.0);
	    cameraMatrix.put(2, 2, 1.0);
	    
	    Log.v(Constants.TAG_CAL, "Samsung Camera Calibration Matrix: ");
	    Log.v(Constants.TAG_CAL, cameraMatrix.dump());

	    
	    cameraMatrix.put(0, 0, 1686.1);
	    cameraMatrix.put(0, 1, 0.0);
	    cameraMatrix.put(0, 2, 959.5);
	    cameraMatrix.put(1, 0, 0.0);
	    cameraMatrix.put(1, 1, 1686.1);
	    cameraMatrix.put(1, 2, 539.5);
	    cameraMatrix.put(2, 0, 0.0);
	    cameraMatrix.put(2, 1, 0.0);
	    cameraMatrix.put(2, 2, 1.0);

	    Log.v(Constants.TAG_CAL, "Live Camera Calibration Matrix: ");
	    Log.v(Constants.TAG_CAL, cameraMatrix.dump());
	    
	    
	    return cameraMatrix;
	}
	
	
	/**
	 * @return
	 */
	public double[] getDistortionCoefficients() {
		
		double [] distCoeff =  { 
				0.0940951391875556,
				0.856988256473992,
				0,
				0,
				-4.559694183079539};

		return distCoeff;

	}

//    /**
//     * Get OpenGL Projection Matrix
//     * 
//     * This is derived from the Android Camera Parameters.
//     * 
//     * @return
//     */
//    public float[] getOpenGLProjectionMatrix() {
//
//        float near = 1.0f;
//        float far  = 100.0f;
//        
//        float top =   (float)Math.tan(fovY * 0.5f);
//        float right = (float)Math.tan(fovX * 0.5f);
//
//        float [] glProjectionMatrix = new float[16];
//        
//        Matrix.frustumM( glProjectionMatrix, 0, -right, right, -top, top, near, far);
//        
//        Log.e(Constants.TAG_CAL, "GL Projection Matrix: " + glProjectionMatrix.toString() );
//
//        return glProjectionMatrix;
//    }
	

}
