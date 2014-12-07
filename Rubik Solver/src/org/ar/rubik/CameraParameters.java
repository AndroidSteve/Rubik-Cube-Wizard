/**
 * 
 */
package org.ar.rubik;

import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;

/**
 * @author android.steve@testlens.com
 *
 */
public class CameraParameters {
	
	public double focalLengthPixels;

	public CameraParameters() {
		
		Camera camera = Camera.open();
		Parameters parameters = camera.getParameters();
		
		Size size = parameters.getPictureSize();
		int widthPixels = size.width;
		int heightPixels = size.height;
		
		double diagonalPixels = Math.sqrt( widthPixels * widthPixels + heightPixels * heightPixels);
		
		float fovX = parameters.getVerticalViewAngle() * (float)(Math.PI / 180.0);
		float fovY = parameters.getHorizontalViewAngle() * (float)(Math.PI / 180.0);
		
		double diagnoalFOV = Math.sqrt(fovX * fovX + fovY * fovY);
		
		camera.release();
		
		focalLengthPixels = diagonalPixels / ( 2.0 * Math.tan(0.5 * diagnoalFOV));
		
//		Mat cameraMatrix          = new Mat(3, 3, CvType.CV_64FC1);
//		cameraMatrix.put(0, 0, focalLengthPixels);
//		cameraMatrix.put(0, 1, 0.0);
//		cameraMatrix.put(0, 2, 1280.0/2.0);
//		cameraMatrix.put(1, 0, 0.0);
//		cameraMatrix.put(1, 1, focalLengthPixels);
//		cameraMatrix.put(1, 2, 720.0/2.0);
//		cameraMatrix.put(2, 0, 0.0);
//		cameraMatrix.put(2, 1, 0.0);
//		cameraMatrix.put(2, 2, 1.0);
 
//		Log.e(Constants.TAG, "dPOV=" + diagnoalFOV + " dPx=" + diagonalPixels);
//		Log.e(Constants.TAG, "Camera Focal Length in Pixels Calibration: " + focalLengthPixels);
	}

}
