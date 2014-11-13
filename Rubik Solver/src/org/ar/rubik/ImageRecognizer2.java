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
 *   This class implements the OpenCV Frame Listener.  All of image processing
 *   is performed in/from this class.
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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.ar.rubik.Constants.ImageProcessModeEnum;
import org.ar.rubik.Constants.ImageSourceModeEnum;
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
public class ImageRecognizer2 implements CvCameraViewListener2 {

	private Controller2 newController = new Controller2();

	// Once an exception or error is encountered, display message from thence forth.
	Mat errorImage = null;

	private long startTimeStamp;
	private long greyscaleProcessTimeStamp;
	private long boxBlurProcessTimeStamp;
	private long cannyEdgeDetectionProcessTimeStamp;
	private long dialationProcessTimeStamp;
	private long contourGenerationTimeStamp;
	private long polygonDetectionTimeStamp;
	private long rhombusTileRecognitionTimeStamp;
	private long rubikFaceRecognitionTimeStamp;

	
	/**
	 * 
	 *  (non-Javadoc)
	 * @see org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2#onCameraViewStarted(int, int)
	 */
	@Override
	public void onCameraViewStarted(int width, int height) {
	}

	
	/**
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
	 *  (non-Javadoc)
	 * @see org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2#onCameraFrame(org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame)
	 */
	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

		if(errorImage != null)
			return errorImage;

		Mat original_image = inputFrame.rgba();
		Size imageSize = original_image.size();
		
		// Save or Recall image as requested
		switch( RubikMenuAndParameters.imageSourceMode) {

		case NORMAL:
			break;

		case SAVE_NEXT:
			Util.saveImage(original_image);
			RubikMenuAndParameters.imageSourceMode = ImageSourceModeEnum.NORMAL;
			break;

		case PLAYBACK:
			original_image = Util.recallImage();

		default:
			break;
		}

		
		try {


			/* **********************************************************************
			 * **********************************************************************
			 * Return Original Image
			 */
			if(RubikMenuAndParameters.imageProcessMode == ImageProcessModeEnum.DIRECT)
				return addAnnotation(original_image);


			startTimeStamp                     = System.currentTimeMillis();
			greyscaleProcessTimeStamp          = startTimeStamp;
			boxBlurProcessTimeStamp            = startTimeStamp;
			cannyEdgeDetectionProcessTimeStamp = startTimeStamp;
			dialationProcessTimeStamp          = startTimeStamp;
			contourGenerationTimeStamp         = startTimeStamp;
			polygonDetectionTimeStamp          = startTimeStamp;
			rhombusTileRecognitionTimeStamp    = startTimeStamp;
			rubikFaceRecognitionTimeStamp      = startTimeStamp;
			Log.i(Constants.TAG, "============================================================================");


			
			/* **********************************************************************
			 * **********************************************************************
			 * Process to Grey Scale
			 * 
			 * This algorithm finds highlights areas that are all of nearly
			 * the same hue.  In particular, cube faces should be highlighted.
			 */
			Mat greyscale_image = new Mat();
			Imgproc.cvtColor(original_image, greyscale_image, Imgproc.COLOR_BGR2GRAY);

			greyscaleProcessTimeStamp = System.currentTimeMillis();

			if(RubikMenuAndParameters.imageProcessMode == ImageProcessModeEnum.GREYSCALE)
				return addAnnotation(greyscale_image);



			/* **********************************************************************
			 * **********************************************************************
			 * Gaussian Filter Blur prevents getting a lot of false hits 
			 */
			Mat blur_image = new Mat(); 

			int kernelSize = (int) RubikMenuAndParameters.gaussianBlurKernelSizeParam.value;
			kernelSize = kernelSize % 2 == 0 ? kernelSize + 1 : kernelSize;  // make odd
			Imgproc.GaussianBlur(
					greyscale_image, 
					blur_image, 
					new Size(kernelSize, kernelSize), -1, -1);

			boxBlurProcessTimeStamp = System.currentTimeMillis();

			if(RubikMenuAndParameters.imageProcessMode == ImageProcessModeEnum.BOXBLUR)
				return addAnnotation(blur_image);



			/* **********************************************************************
			 * **********************************************************************
			 * Canny Edge Detection 
			 */
			Mat canny_image = new Mat();    	    
			Imgproc.Canny(
					blur_image, 
					canny_image, 
					RubikMenuAndParameters.cannyLowerThresholdParam.value, 
					RubikMenuAndParameters.cannyUpperThresholdParam.value,
					3,
					false);
			cannyEdgeDetectionProcessTimeStamp = System.currentTimeMillis();


			if(RubikMenuAndParameters.imageProcessMode == ImageProcessModeEnum.CANNY)
				return addAnnotation(canny_image);

			

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
									RubikMenuAndParameters.dilationKernelSizeParam.value, 
									RubikMenuAndParameters.dilationKernelSizeParam.value)));

			dialationProcessTimeStamp = System.currentTimeMillis();

			if(RubikMenuAndParameters.imageProcessMode == ImageProcessModeEnum.DILATION)
				return addAnnotation(dilate_image);



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
					Imgproc.CHAIN_APPROX_SIMPLE);

			contourGenerationTimeStamp = System.currentTimeMillis();

			if(RubikMenuAndParameters.imageProcessMode == ImageProcessModeEnum.CONTOUR) {

				// Create gray scale image but in RGB format for later annotation.
				Mat gray_image = new Mat(imageSize, CvType.CV_8UC4);
				Mat rgba_gray_image = new Mat(imageSize, CvType.CV_8UC4);
				Imgproc.cvtColor( original_image, gray_image, Imgproc.COLOR_RGB2GRAY);
				Imgproc.cvtColor(gray_image, rgba_gray_image, Imgproc.COLOR_GRAY2BGRA, 4);
				Imgproc.drawContours(rgba_gray_image, contours, -1, Constants.ColorYellow, 3);
				Core.putText(rgba_gray_image, "Num Contours: " + contours.size(),  new Point(50, 150), Constants.FontFace, 3, Constants.ColorYellow, 2);

				return addAnnotation(rgba_gray_image);
			}


			/* **********************************************************************
			 * **********************************************************************
			 * Polygon Detection
			 */	 
			List<Rhombus> polygonList = new LinkedList<Rhombus>();
			final double epsilon = RubikMenuAndParameters.polygonEpsilonParam.value;

			Iterator<MatOfPoint> contourItr = contours.iterator(); 
			while(contourItr.hasNext()) {

				MatOfPoint contour = contourItr.next();

				// Keep only counter clockwise contours.
				double contourArea = Imgproc.contourArea(contour, true);
				if(contourArea < 0.0)
					continue;

				// Keep only reasonable area contours
				if(contourArea < 100.0)
					continue;

				MatOfPoint2f contour2f = new MatOfPoint2f();
				contour.convertTo(contour2f, CvType.CV_32FC2);

				MatOfPoint2f polygone2f = new MatOfPoint2f();

				Imgproc.approxPolyDP(
						contour2f, 
						polygone2f,
						epsilon,
						true);

				MatOfPoint polygon = new MatOfPoint();
				polygone2f.convertTo(polygon, CvType.CV_32S);

				polygonList.add(new Rhombus(polygon, original_image));
			}

			polygonDetectionTimeStamp = System.currentTimeMillis();

			if(RubikMenuAndParameters.imageProcessMode == ImageProcessModeEnum.POLYGON) {

				// Create gray scale image but in RGB format for later annotation.
				Mat gray_image = new Mat(imageSize, CvType.CV_8UC4);
				Mat rgba_gray_image = new Mat(imageSize, CvType.CV_8UC4);
				Imgproc.cvtColor( original_image, gray_image, Imgproc.COLOR_RGB2GRAY);
				Imgproc.cvtColor(gray_image, rgba_gray_image, Imgproc.COLOR_GRAY2BGRA, 4);

				for(Rhombus polygon : polygonList)
					polygon.draw(rgba_gray_image, Constants.ColorYellow);

				Core.putText(rgba_gray_image, "Num Polygons: " + polygonList.size(),  new Point(50, 150), Constants.FontFace, 3, Constants.ColorYellow, 2);

				return addAnnotation(rgba_gray_image);
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

			Rhombus.removedOutlierRhombi(rhombusList);

			rhombusTileRecognitionTimeStamp = System.currentTimeMillis();

			if(RubikMenuAndParameters.imageProcessMode == ImageProcessModeEnum.RHOMBUS) {

				// Create gray scale image but in RGB format for later annotation.
				Mat gray_image = new Mat(imageSize, CvType.CV_8UC4);
				Mat rgba_gray_image = new Mat(imageSize, CvType.CV_8UC4);
				Imgproc.cvtColor( original_image, gray_image, Imgproc.COLOR_RGB2GRAY);
				Imgproc.cvtColor(gray_image, rgba_gray_image, Imgproc.COLOR_GRAY2BGRA, 4);

				for(Rhombus rhombus : rhombusList)
					rhombus.draw(rgba_gray_image, Constants.ColorGreen);

				Core.putText(rgba_gray_image, "Num Rhombus: " + rhombusList.size(),  new Point(50, 150), Constants.FontFace, 3, Constants.ColorGreen, 2);

				return addAnnotation(rgba_gray_image);
			}


			/* **********************************************************************
			 * **********************************************************************
			 * Face Recognition
			 * 
			 * 
			 */	 
			RubikFace2 newRubikFace = new RubikFace2();
			newRubikFace.processRhombuses(rhombusList);


			
			/* **********************************************************************
			 * **********************************************************************
			 * Controller
			 * 
			 * Will provide user instructions.
			 * Will determine when we are on-face and off-face
			 * Will determine when we are on-new-face
			 * Will change state 
			 */	
			newController.processFace(newRubikFace);



			return addAnnotation(original_image);


		} catch (CvException e) {
			e.printStackTrace();        	
			errorImage = new Mat(imageSize, CvType.CV_8UC4);
			Core.putText(errorImage, e.getMessage(), new Point(50, 50), Constants.FontFace, 2, Constants.ColorWhite, 2);
		} catch (Exception e) {
			e.printStackTrace();        	
			errorImage = new Mat(imageSize, CvType.CV_8UC4);
			Core.putText(errorImage, e.getMessage(), new Point(50, 50), Constants.FontFace, 2, Constants.ColorWhite, 2);
		}

		return addAnnotation(original_image);
	}



	/**
	 * Add Annotation
	 * 
	 * @param original_image
	 */
	private Mat addAnnotation(Mat original_image) {
		return original_image;
	}


}
