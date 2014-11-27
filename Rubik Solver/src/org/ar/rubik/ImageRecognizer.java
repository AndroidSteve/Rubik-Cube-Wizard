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
 *   Function onFrameListener() really comprises the heart of the application "Controller".
 *   That is, every Camera Frame is like a event possibly resulting in a change in the
 *   application "State" ( or "Model" in the MVC vernacular).
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

import java.util.LinkedList;
import java.util.List;

import org.ar.rubik.Constants.ImageProcessModeEnum;
import org.ar.rubik.Constants.ImageSourceModeEnum;
import org.ar.rubik.RubikFace.FaceRecognitionStatusEnum;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.util.Log;

/**
 * @author android.steve@testlens.com
 *
 */
public class ImageRecognizer implements CvCameraViewListener2 {
	
	private AppStateMachine appStateMachine;
	private StateModel stateModel;
	private Annotation annotation;

	// Once an exception or error is encountered, display message from thence forth.
	// We cannot use Toast; it must be used on the UI thread and we are executing on the Frame thread.
	private Mat errorImage = null;

	
	/**
	 * Image Recognizer Constructor
	 * 
	 * @param appStateMachine
	 * @param stateModel
	 */
    public ImageRecognizer(AppStateMachine appStateMachine, StateModel stateModel) {
    	this.appStateMachine = appStateMachine;
    	this.stateModel = stateModel;
    	this.annotation = new Annotation(this.stateModel, this.appStateMachine);
    }


	/**
	 * On Camer View Started
	 * 
	 *  (non-Javadoc)
	 * @see org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2#onCameraViewStarted(int, int)
	 */
	@Override
	public void onCameraViewStarted(int width, int height) {
	}

	
	/**
	 * On Camera View Stopped
	 * 
	 *  (non-Javadoc)
	 * @see org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2#onCameraViewStopped()
	 */
	@Override
	public void onCameraViewStopped() {
	}

	
	/**
	 * On Camera Frame
	 * 
	 * Process frame image through Rubik Face recognition possibly resulting in a state change.
	 * 
	 *  (non-Javadoc)
	 * @see org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2#onCameraFrame(org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame)
	 */
	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		
//		Log.e(Constants.TAG, "CV Thread ID = " + Thread.currentThread().getId());

		// Just display error message if it is non-null.
		if(errorImage != null)
			return errorImage;

		Mat image = inputFrame.rgba();
		Size imageSize = image.size();
		
		// Save or Recall image as requested
		switch( MenuAndParams.imageSourceMode) {
		case NORMAL:
			break;
		case SAVE_NEXT:
			Util.saveImage(image);
			MenuAndParams.imageSourceMode = ImageSourceModeEnum.NORMAL;
			break;
		case PLAYBACK:
			image = Util.recallImage();
		default:
			break;
		}

		
		try {

			// Initialize
			RubikFace rubikFace = new RubikFace();
			rubikFace.profiler.markTime(Profiler.Event.START);
			Log.i(Constants.TAG, "============================================================================");


			/* **********************************************************************
			 * **********************************************************************
			 * Return Original Image
			 */
			if(MenuAndParams.imageProcessMode == ImageProcessModeEnum.DIRECT) {
				stateModel.activeRubikFace = rubikFace;
				rubikFace.profiler.markTime(Profiler.Event.TOTAL);
				return annotation.renderAnnotation(image);
			}

			
			
			/* **********************************************************************
			 * **********************************************************************
			 * Process to Grey Scale
			 * 
			 * This algorithm finds highlights areas that are all of nearly
			 * the same hue.  In particular, cube faces should be highlighted.
			 */
			Mat greyscale_image = new Mat();
			Imgproc.cvtColor(image, greyscale_image, Imgproc.COLOR_BGR2GRAY);
			rubikFace.profiler.markTime(Profiler.Event.GREYSCALE);
			if(MenuAndParams.imageProcessMode == ImageProcessModeEnum.GREYSCALE) {
				stateModel.activeRubikFace = rubikFace;
				rubikFace.profiler.markTime(Profiler.Event.TOTAL);
				return annotation.renderAnnotation(greyscale_image);
			}



			/* **********************************************************************
			 * **********************************************************************
			 * Gaussian Filter Blur prevents getting a lot of false hits 
			 */
			Mat blur_image = new Mat(); 

			int kernelSize = (int) MenuAndParams.gaussianBlurKernelSizeParam.value;
			kernelSize = kernelSize % 2 == 0 ? kernelSize + 1 : kernelSize;  // make odd
			Imgproc.GaussianBlur(
					greyscale_image, 
					blur_image, 
					new Size(kernelSize, kernelSize), -1, -1);
			rubikFace.profiler.markTime(Profiler.Event.GAUSSIAN);
			if(MenuAndParams.imageProcessMode == ImageProcessModeEnum.GAUSSIAN) {
				stateModel.activeRubikFace = rubikFace;
				rubikFace.profiler.markTime(Profiler.Event.TOTAL);
				return annotation.renderAnnotation(blur_image);
			}



			/* **********************************************************************
			 * **********************************************************************
			 * Canny Edge Detection 
			 */
			Mat canny_image = new Mat();    	    
			Imgproc.Canny(
					blur_image, 
					canny_image, 
					MenuAndParams.cannyLowerThresholdParam.value, 
					MenuAndParams.cannyUpperThresholdParam.value,
					3,         // Sobel Aperture size.  This seems to be typically value used in the literature: i.e., a 3x3 Sobel Matrix.
					false);    // use cheap gradient calculation: norm =|dI/dx|+|dI/dy|
			rubikFace.profiler.markTime(Profiler.Event.EDGE);
			if(MenuAndParams.imageProcessMode == ImageProcessModeEnum.CANNY) {
				stateModel.activeRubikFace = rubikFace;
				rubikFace.profiler.markTime(Profiler.Event.TOTAL);
				return annotation.renderAnnotation(canny_image);
			}

			

			/* **********************************************************************
			 * **********************************************************************
			 * Dilation Image Process
			 */
			Mat dilate_image = new Mat();
			Imgproc.dilate(
					canny_image,
					dilate_image,
					Imgproc.getStructuringElement(
							Imgproc.MORPH_RECT, 
							new Size(
									MenuAndParams.dilationKernelSizeParam.value, 
									MenuAndParams.dilationKernelSizeParam.value)));
			rubikFace.profiler.markTime(Profiler.Event.DILATION);
			if(MenuAndParams.imageProcessMode == ImageProcessModeEnum.DILATION) {
				stateModel.activeRubikFace = rubikFace;
				rubikFace.profiler.markTime(Profiler.Event.TOTAL);
				return annotation.renderAnnotation(dilate_image);
			}



			/* **********************************************************************
			 * **********************************************************************
			 * Contour Generation 
			 */	   
			List<MatOfPoint> contours = new LinkedList<MatOfPoint>();
			Mat heirarchy = new Mat();
			Imgproc.findContours(
					dilate_image,
					contours, 
					heirarchy,
					Imgproc.RETR_LIST,
					Imgproc.CHAIN_APPROX_SIMPLE); // Note: tried other TC89 options, but no significant change or improvement on cpu time.
			rubikFace.profiler.markTime(Profiler.Event.CONTOUR);

			// Create gray scale image but in RGB format, and then added yellow colored contours on top.
			if(MenuAndParams.imageProcessMode == ImageProcessModeEnum.CONTOUR) {
				stateModel.activeRubikFace = rubikFace;
				rubikFace.profiler.markTime(Profiler.Event.TOTAL);
				Mat gray_image = new Mat(imageSize, CvType.CV_8UC4);
				Mat rgba_gray_image = new Mat(imageSize, CvType.CV_8UC4);
				Imgproc.cvtColor( image, gray_image, Imgproc.COLOR_RGB2GRAY);
				Imgproc.cvtColor(gray_image, rgba_gray_image, Imgproc.COLOR_GRAY2BGRA, 3);
				Imgproc.drawContours(rgba_gray_image, contours, -1, Constants.ColorYellow, 3);
				Core.putText(rgba_gray_image, "Num Contours: " + contours.size(),  new Point(500, 50), Constants.FontFace, 4, Constants.ColorRed, 4);
				return annotation.renderAnnotation(rgba_gray_image);
			}
			


			/* **********************************************************************
			 * **********************************************************************
			 * Polygon Detection
			 */	 
			List<Rhombus> polygonList = new LinkedList<Rhombus>();
			for(MatOfPoint contour : contours) {

				// Keep only counter clockwise contours.  A clockwise contour is reported as a negative number.
				double contourArea = Imgproc.contourArea(contour, true);
				if(contourArea < 0.0)
					continue;

				// Keep only reasonable area contours
				if(contourArea < MenuAndParams.minimumContourAreaParam.value)
					continue;

				// Floating, instead of Double, for some reason required for approximate polygon detection algorithm.
				MatOfPoint2f contour2f = new MatOfPoint2f();
				MatOfPoint2f polygone2f = new MatOfPoint2f();
				MatOfPoint polygon = new MatOfPoint();

				// Make a Polygon out of a contour with provide Epsilon accuracy parameter.
				// It uses the Douglas-Peucker algorithm http://en.wikipedia.org/wiki/Ramer-Douglas-Peucker_algorithm
				contour.convertTo(contour2f, CvType.CV_32FC2);
				Imgproc.approxPolyDP(
						contour2f, 
						polygone2f,
						MenuAndParams.polygonEpsilonParam.value,  // The maximum distance between the original curve and its approximation.
						true);                                             // Resulting polygon representation is "closed:" its first and last vertices are connected.
				polygone2f.convertTo(polygon, CvType.CV_32S);

				polygonList.add(new Rhombus(polygon, image));
			}

			rubikFace.profiler.markTime(Profiler.Event.POLYGON);

			// Create gray scale image but in RGB format, and then add yellow colored polygons on top.
			if(MenuAndParams.imageProcessMode == ImageProcessModeEnum.POLYGON) {
				stateModel.activeRubikFace = rubikFace;
				rubikFace.profiler.markTime(Profiler.Event.TOTAL);
				Mat gray_image = new Mat(imageSize, CvType.CV_8UC4);
				Mat rgba_gray_image = new Mat(imageSize, CvType.CV_8UC4);
				Imgproc.cvtColor( image, gray_image, Imgproc.COLOR_RGB2GRAY);
				Imgproc.cvtColor(gray_image, rgba_gray_image, Imgproc.COLOR_GRAY2BGRA, 4);
				for(Rhombus polygon : polygonList)
					polygon.draw(rgba_gray_image, Constants.ColorYellow);
				Core.putText(rgba_gray_image, "Num Polygons: " + polygonList.size(),  new Point(500, 50), Constants.FontFace, 3, Constants.ColorRed, 4);
				return annotation.renderAnnotation(rgba_gray_image);
			}


			/* **********************************************************************
			 * **********************************************************************
			 * Rhombus Tile Recognition
			 * 
			 * From polygon list, produces a list of suitable Parallelograms (Rhombi).
			 */	 
			Log.i(Constants.TAG, String.format( "Rhombus:   X    Y   Area   a-a  b-a a-l b-l gamma"));
			List<Rhombus> rhombusList = new LinkedList<Rhombus>();
			// Get only valid Rhombus(es) : actually parallelograms.
			for(Rhombus rhombus : polygonList) {
				rhombus.qualify();
				if(rhombus.status == Rhombus.StatusEnum.VALID)
					rhombusList.add(rhombus);
			}

			// Filtering w.r.t. Rhmobus set characteristics
			Rhombus.removedOutlierRhombi(rhombusList);
			
			rubikFace.profiler.markTime(Profiler.Event.RHOMBUS);

			// Create gray scale image but in RGB format, and then add yellow colored Rhombi (parallelograms) on top.
			if(MenuAndParams.imageProcessMode == ImageProcessModeEnum.RHOMBUS) {
				stateModel.activeRubikFace = rubikFace;
				rubikFace.profiler.markTime(Profiler.Event.TOTAL);
				Mat gray_image = new Mat(imageSize, CvType.CV_8UC4);
				Mat rgba_gray_image = new Mat(imageSize, CvType.CV_8UC4);
				Imgproc.cvtColor( image, gray_image, Imgproc.COLOR_RGB2GRAY);
				Imgproc.cvtColor(gray_image, rgba_gray_image, Imgproc.COLOR_GRAY2BGRA, 4);
				for(Rhombus rhombus : rhombusList)
					rhombus.draw(rgba_gray_image, Constants.ColorYellow);
				Core.putText(rgba_gray_image, "Num Rhombus: " + rhombusList.size(),  new Point(500, 50), Constants.FontFace, 4, Constants.ColorRed, 4);
				return annotation.renderAnnotation(rgba_gray_image);
			}


			/* **********************************************************************
			 * **********************************************************************
			 * Face Recognition
			 * 
			 * Takes a collection of Rhombus objects and determines if a valid
			 * Rubik Face can be determined from them, and then also determines 
			 * initial color for all nine tiles. 
			 */	 
			rubikFace.processRhombuses(rhombusList, image);
			rubikFace.profiler.markTime(Profiler.Event.FACE);
			if(MenuAndParams.imageProcessMode == ImageProcessModeEnum.FACE_DETECT) {
				stateModel.activeRubikFace = rubikFace;
				rubikFace.profiler.markTime(Profiler.Event.TOTAL);
				return annotation.renderAnnotation(image);
			}
			
			
			/* **********************************************************************
			 * **********************************************************************
			 * Cube Recognition
			 * 
			 * Reconstruct Rubik Cube 3D location and orientation in GL space coordinates.
			 */
			if(rubikFace.faceRecognitionStatus == FaceRecognitionStatusEnum.SOLVED) {
				CubeReconstructor cubeReconstructor = new CubeReconstructor();
				cubeReconstructor.reconstruct(rubikFace);
				stateModel.cubeReconstructor = cubeReconstructor;
			}
			else
				stateModel.cubeReconstructor = null;
			
			
			/* **********************************************************************
			 * **********************************************************************
			 * Application State Machine
			 * 
			 * Will provide user instructions.
			 * Will determine when we are on-face and off-face
			 * Will determine when we are on-new-face
			 * Will change state 
			 */	
			appStateMachine.processFace(rubikFace);
			rubikFace.profiler.markTime(Profiler.Event.CONTROLLER);
			rubikFace.profiler.markTime(Profiler.Event.TOTAL);

			// Normal return point.
			stateModel.activeRubikFace = rubikFace;
			return annotation.renderAnnotation(image);

		} catch (CvException e) {
			e.printStackTrace();        	
			errorImage = new Mat(imageSize, CvType.CV_8UC4);
			Core.putText(errorImage, e.getMessage(), new Point(50, 50), Constants.FontFace, 2, Constants.ColorWhite, 2);
		} catch (Exception e) {
			e.printStackTrace();        	
			errorImage = new Mat(imageSize, CvType.CV_8UC4);
			Core.putText(errorImage, e.getMessage(), new Point(50, 50), Constants.FontFace, 2, Constants.ColorWhite, 2);
		} catch (Error e) {
			e.printStackTrace();        	
			errorImage = new Mat(imageSize, CvType.CV_8UC4);
			Core.putText(errorImage, e.getMessage(), new Point(50, 50), Constants.FontFace, 2, Constants.ColorWhite, 2);
		}

		return annotation.renderAnnotation(image);
	}

}
