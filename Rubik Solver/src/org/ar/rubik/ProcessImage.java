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


import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import org.ar.rubik.Constants.ImageProcessModeEnum;
import org.ar.rubik.Constants.ImageSourceModeEnum;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.util.Log;

public class ProcessImage {
	
	private static long startTimeStamp;
	private static long greyscaleProcessTimeStamp;
	private static long boxBlurProcessTimeStamp;
	private static long cannyEdgeDetectionProcessTimeStamp;
	private static long dialationProcessTimeStamp;
	private static long contourGenerationTimeStamp;
	private static long polygonDetectionTimeStamp;
	private static long rhombusTileRecognitionTimeStamp;
	private static long rubikFaceRecognitionTimeStamp;
	
	
	// Initial list of Rhombi that is not yet filtered, thus called polygon list.
	public static List<Rhombus> polygonList = null;



	/**
	 * Process Image
	 * 
	 * @param original_image
	 * @param imageSourceMode
	 * @param imageProcessMode
	 * @param rubikFace
	 * @return
	 */
	public static Mat processImageIntoRhombiList(
			Mat original_image,
			ImageSourceModeEnum imageSourceMode,
			ImageProcessModeEnum imageProcessMode,
			List<Rhombus> rhombusList,
			RubikFace rubikFace) {


		/* **********************************************************************
		 * Return Original Image
		 */
		if(imageProcessMode == ImageProcessModeEnum.DIRECT)
			return original_image;
		
		final Size imageSize = original_image.size();
		RubikCube.active = null;
		
		startTimeStamp = System.currentTimeMillis();
		greyscaleProcessTimeStamp = startTimeStamp;
		boxBlurProcessTimeStamp = startTimeStamp;
		cannyEdgeDetectionProcessTimeStamp = startTimeStamp;
		dialationProcessTimeStamp = startTimeStamp;
		contourGenerationTimeStamp = startTimeStamp;
		polygonDetectionTimeStamp = startTimeStamp;
		rhombusTileRecognitionTimeStamp = startTimeStamp;
		rubikFaceRecognitionTimeStamp = startTimeStamp;
		Log.i(Constants.TAG, "============================================================================");



		//		/* **********************************************************************
		//		 * Diagnostic Case: Generate Gray Scale Histogram
		//		 */
		//		if(imageProcessMode == ImageProcessModeEnum.GRAY_HISTOGRAM)
		//			return renderGrayScaleHistogram(gray_image);


		/* **********************************************************************
		 * Process to Grey Scale
		 * 
		 * This algorithm finds highlights areas that are all of nearly
		 * the same hue.  In particular, cube faces should be highlighted.
		 */
		Mat greyscale_image = new Mat();
		Imgproc.cvtColor(original_image, greyscale_image, Imgproc.COLOR_BGR2GRAY);
		
		greyscaleProcessTimeStamp = System.currentTimeMillis();

		if(imageProcessMode == ImageProcessModeEnum.GREYSCALE)
			return greyscale_image;
		
		

		/* **********************************************************************
		 * Box Filter Blur prevents getting a lot of false hits 
		 */
		Mat blur_image = new Mat(); 
//		int blurKernelSize = (int)MainActivity.boxBlurKernelSizeParam;
//		Imgproc.blur(greyscale_image, blur_image, new Size(blurKernelSize, blurKernelSize));
		
	    Imgproc.GaussianBlur(
	    		greyscale_image, 
	    		blur_image, 
	    		new Size(7, 7), -1, -1);
		
		boxBlurProcessTimeStamp = System.currentTimeMillis();

		if(imageProcessMode == ImageProcessModeEnum.BOXBLUR)
			return blur_image;

		

		/* **********************************************************************
		 * Canny Edge Detection 
		 */
		Mat canny_image = new Mat();    	    
		Imgproc.Canny(
				blur_image, 
				canny_image, 
				MainActivity.cannyLowerThresholdParam, 
				MainActivity.cannyUpperThresholdParam,
				3,
				false);
		cannyEdgeDetectionProcessTimeStamp = System.currentTimeMillis();

		if(imageProcessMode == ImageProcessModeEnum.CANNY)
			return canny_image;

		
		/* **********************************************************************
		 * Dilation Image Process
		 */
		Mat dilate_image = new Mat();
		Imgproc.dilate(
				canny_image,
				dilate_image,
				Imgproc.getStructuringElement(
						Imgproc.MORPH_RECT, 
						new Size(
								MainActivity.dilationKernelSize, 
								MainActivity.dilationKernelSize)));

		dialationProcessTimeStamp = System.currentTimeMillis();
		
//		Imgproc.erode(mInput, mInput, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2,2)));

		if(imageProcessMode == ImageProcessModeEnum.DILATION)
			return dilate_image;

		
		
		/* **********************************************************************
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

		if(imageProcessMode == ImageProcessModeEnum.CONTOUR) {

			// Create gray scale image but in RGB format for later annotation.
			Mat gray_image = new Mat(imageSize, CvType.CV_8UC4);
			Mat rgba_gray_image = new Mat(imageSize, CvType.CV_8UC4);
			Imgproc.cvtColor( original_image, gray_image, Imgproc.COLOR_RGB2GRAY);
			Imgproc.cvtColor(gray_image, rgba_gray_image, Imgproc.COLOR_GRAY2BGRA, 4);
			Imgproc.drawContours(rgba_gray_image, contours, -1, Constants.ColorYellow, 3);
			Core.putText(rgba_gray_image, "Num Contours: " + contours.size(),  new Point(50, 150), Constants.FontFace, 3, Constants.ColorYellow, 2);
			
			return rgba_gray_image;
		}


		/* **********************************************************************
		 * Polygon Detection
		 */	 
		polygonList = new LinkedList<Rhombus>();
		final double epsilon = MainActivity.polygonEpsilonParam;

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
		
		if(imageProcessMode == ImageProcessModeEnum.POLYGON) {

			// Create gray scale image but in RGB format for later annotation.
			Mat gray_image = new Mat(imageSize, CvType.CV_8UC4);
			Mat rgba_gray_image = new Mat(imageSize, CvType.CV_8UC4);
			Imgproc.cvtColor( original_image, gray_image, Imgproc.COLOR_RGB2GRAY);
			Imgproc.cvtColor(gray_image, rgba_gray_image, Imgproc.COLOR_GRAY2BGRA, 4);

			for(Rhombus polygon : polygonList)
				polygon.draw(rgba_gray_image, Constants.ColorYellow);
			
			Core.putText(rgba_gray_image, "Num Polygons: " + polygonList.size(),  new Point(50, 150), Constants.FontFace, 3, Constants.ColorYellow, 2);

			return rgba_gray_image;
		}
		
		
		/* **********************************************************************
		 * Rhombus Tile Recognition
		 */	 
		Log.i(Constants.TAG, String.format( "Rhombus:   X    Y   Area   a-a  b-a a-l b-l gamma"));
		// Get only valid Rhombus(es) : actually parallelograms.
		for(Rhombus rhombus : polygonList) {
			rhombus.qualify();
			if(rhombus.status == Rhombus.StatusEnum.VALID)
				rhombusList.add(rhombus);
		}
		
		Rhombus.removedOutlierRhombi(rhombusList);

		rhombusTileRecognitionTimeStamp = System.currentTimeMillis();
		
		if(imageProcessMode == ImageProcessModeEnum.RHOMBUS) {

			// Create gray scale image but in RGB format for later annotation.
			Mat gray_image = new Mat(imageSize, CvType.CV_8UC4);
			Mat rgba_gray_image = new Mat(imageSize, CvType.CV_8UC4);
			Imgproc.cvtColor( original_image, gray_image, Imgproc.COLOR_RGB2GRAY);
			Imgproc.cvtColor(gray_image, rgba_gray_image, Imgproc.COLOR_GRAY2BGRA, 4);

			for(Rhombus rhombus : rhombusList)
				rhombus.draw(rgba_gray_image, Constants.ColorGreen);
			
			Core.putText(rgba_gray_image, "Num Rhombus: " + rhombusList.size(),  new Point(50, 150), Constants.FontFace, 3, Constants.ColorGreen, 2);

			return rgba_gray_image;
		}

		return null;
	}




	public static Mat renderTimeConsumptionMetrics(Mat image) {
		
		RubikFace.drawFlatFaceRepresentation(image, RubikCube.active, 50, 50, 50);

		Core.putText(image, "Greyscale: " + (greyscaleProcessTimeStamp - startTimeStamp) + "mS", new Point(50, 300), Constants.FontFace, 2, Constants.ColorWhite, 2);
		Core.putText(image, "Box Blur: " + (boxBlurProcessTimeStamp - greyscaleProcessTimeStamp) + "mS", new Point(50, 350), Constants.FontFace, 2, Constants.ColorWhite, 2);
		Core.putText(image, "Canny Edge: " + (cannyEdgeDetectionProcessTimeStamp - boxBlurProcessTimeStamp) + "mS", new Point(50, 400), Constants.FontFace, 2, Constants.ColorWhite, 2);
		Core.putText(image, "Dialation: " + (dialationProcessTimeStamp - cannyEdgeDetectionProcessTimeStamp) + "mS", new Point(50, 450), Constants.FontFace, 2, Constants.ColorWhite, 2);
		Core.putText(image, "Contour Gen: " + (contourGenerationTimeStamp - dialationProcessTimeStamp) + "mS", new Point(50, 500), Constants.FontFace, 2, Constants.ColorWhite, 2);
		Core.putText(image, "Polygon Detect: " + (polygonDetectionTimeStamp - contourGenerationTimeStamp) + "mS", new Point(50, 550), Constants.FontFace, 2, Constants.ColorWhite, 2);
		Core.putText(image, "Rhombus Recog: " + (rhombusTileRecognitionTimeStamp - polygonDetectionTimeStamp) + "mS", new Point(50, 600), Constants.FontFace, 2, Constants.ColorWhite, 2);
		Core.putText(image, "Face Recog: " + (rubikFaceRecognitionTimeStamp - rhombusTileRecognitionTimeStamp) + "mS", new Point(50, 650), Constants.FontFace, 2, Constants.ColorWhite, 2);
		Core.putText(image, "Total Process: " + (rubikFaceRecognitionTimeStamp - startTimeStamp) + "mS", new Point(50, 700), Constants.FontFace, 2, Constants.ColorWhite, 2);

		return image;
	}



	/**
	 * @param gray_image
	 * @return
	 */
	@SuppressWarnings("unused")
	private static Mat renderGrayScaleHistogram(Mat gray_image) {

		// Split ?
		Vector<Mat> bgr_planes = new Vector<Mat>();
		Core.split(gray_image, bgr_planes);

		MatOfInt histSize = new MatOfInt(256);
		final MatOfFloat histRange = new MatOfFloat(0f, 256f);

		// Calculate histogram
		Mat rawHistogram = new  Mat();
		Imgproc.calcHist(bgr_planes, new MatOfInt(0),new Mat(), rawHistogram, histSize, histRange, false);

		return renderHistogram(
				rawHistogram, 
				"Gray-Scale Histogram", 
				"Horizontal 0 to 255 image lumananace", 
				"Vertical 0 to ?? elements in bucket");
	}



	/**
	 * Return a Matrix that renders the provided histogram.
	 * Histogram should/could come from Imgproc.calcHist()
	 * Histogram arg is one coloumn.
	 * 
	 * @param rawHistogramData
	 * @param title1 TODO
	 * @param title2 TODO
	 * @param title3 TODO
	 * @return
	 */
	private static Mat renderHistogram(Mat rawHistogramData, String title1, String title2, String title3) {


		//        Log.i(Constants.TAG, "Raw Histogram " + rawHistogramData);

		int numBuckets = rawHistogramData.rows();

		int hist_w = 1280; //1920; // Display Width
		int hist_h = 720; //1080; // Display Height
		int bin_w = (int)Math.round((double) (hist_w / numBuckets));  // About 9 pixels per bin: num of bins is 256.

		// Result Image
		Mat histImage = new Mat(hist_h, hist_w, CvType.CV_8UC1);

		Core.normalize(rawHistogramData, rawHistogramData, 3, histImage.rows(), Core.NORM_MINMAX);

		for (int i = 1; i < numBuckets; i++) {         

			final int x1 = bin_w * (i - 1);
			final int y1 = hist_h - (int)Math.round(rawHistogramData.get(i-1,0)[0]);
			final int x2 = bin_w * (i);
			final int y2 = hist_h - Math.round(Math.round(rawHistogramData.get(i, 0)[0]));

			Core.line(
					histImage, 
					new Point(x1,y1), 
					new Point(x2, y2),
					new  Scalar(255, 0, 0), 
					2, 
					8, 
					0);

		}

		Core.putText(histImage, title1,  new Point(50, 150), Constants.FontFace, 3, Constants.ColorRed, 2);
		Core.putText(histImage, title2,  new Point(50, 200), Constants.FontFace, 3, Constants.ColorRed, 2);
		Core.putText(histImage, title3,  new Point(50, 250), Constants.FontFace, 3, Constants.ColorRed, 2);

		//        Log.i(Constants.TAG, "Generated Histogram: " + histImage);
		return histImage;
	}




	public static void noteCompletionTime() {
		rubikFaceRecognitionTimeStamp = System.currentTimeMillis();
	}




	public static int getTotalComputionTime() {
		return (int) (rubikFaceRecognitionTimeStamp - startTimeStamp);
	}
}
