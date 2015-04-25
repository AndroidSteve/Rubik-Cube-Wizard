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

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.graphics.Bitmap;
import android.util.Log;


/**
 * 
 * 
 * @author android.steve@cl-sw.com
 *
 */
public class MonoChromatic {
	
	/**
	 * 
	 * 
	 * @param original_image
	 * @return
	 */
	public static Mat monochromaticMedianImageFilter(Mat original_image) {
		
		return monochromaticMedianImageFilterExp(original_image);
		

//		return monochromaticMedianImageFilterOpenCL(original_image);
		
//		return monochromaticMedianImageFilterUtilizingOpenCv3(original_image);
//		return monochromaticMedianImageFilterUtilizingOpenCv2(original_image);
//		return monochromaticMedianImageFilterUtilizingOpenCv(original_image);
//		return monochromaticMedianImageFilterBruteForceInJava(original_image);
	}	
	
	
	
    private static Mat monochromaticMedianImageFilterExp(Mat image) {

        Mat gray_image = new Mat();
        Imgproc.cvtColor( image, gray_image, Imgproc.COLOR_BGR2GRAY);
        
				Mat gaussian_image = new Mat(); 
			    Imgproc.GaussianBlur(
			    		gray_image, 
			    		gaussian_image, 
			    		new Size(MenuAndParams.gaussianBlurKernelSizeParam.value, MenuAndParams.gaussianBlurKernelSizeParam.value), 0, 0);
//			    		MainActivity.gaussianSigmaBlurParam, 
//			    		MainActivity.gaussianSigmaBlurParam);
        
        return gaussian_image;
    	
//		List<Mat> channels = new LinkedList<Mat>();
//		Core.split(yuv_image, channels);
//		Mat yMat = channels.get(0);
//		Mat uMat = channels.get(1);
//		Mat vMat = channels.get(2);
//        
//		return vMat;
	}



	/**
     * Computer Mono Chromatic Filter using GPU and OpenCL
     * @param image
     * @return
     */
    private static Mat monochromaticMedianImageFilterOpenCL(Mat image) {
    	
    	Log.i(Constants.TAG, "Mono Arg Image: " + image);  // 720*1280 CV_8UC3
    	
//    	Imgproc.resize(image, image, new Size(image.size().height/2, image.size().width/2));
    	
    	Size size = image.size();
    	
    	// Create OpenCL Output Bit Map
        Bitmap outputBitmap = Bitmap.createBitmap(
        		(int)size.width,
        		(int)size.height,
        		Bitmap.Config.ARGB_8888);
        
        // Create OpenCL Input Bit Map
        Bitmap inputBitmap = Bitmap.createBitmap(
        		(int)size.width,
        		(int)size.height,
        		Bitmap.Config.ARGB_8888);

        // Convert to Hue, Luminance, and Saturation
//        long startHLS = System.currentTimeMillis();
        Mat hls_image = new Mat();
        Imgproc.cvtColor( image, hls_image, Imgproc.COLOR_BGR2YUV);
//        Log.i(Constants.TAG, "Mono HLS Image: " + hls_image);  // 720*1280 CV_8UC3
//        long endHLS = System.currentTimeMillis();
//        Log.i(Constants.TAG, "HLS Conversion Time: " + (endHLS - startHLS) + "mS"); 

        // Convert image Mat to Bit Map.
        Utils.matToBitmap(hls_image, inputBitmap);
        
        // Call C++ code.
    	nativeStepOpenCL(
    			(int) 7,
    			(int) 5,
    			0,
    			0,
    			true,
    			inputBitmap,
    			outputBitmap);
    	
        Mat result = new Mat();   	
    	Utils.bitmapToMat(outputBitmap, result);

//        long startChannel = System.currentTimeMillis();
		List<Mat> channels = new LinkedList<Mat>();
		Core.split(result, channels);
		Mat channel0 = channels.get(0);
//        long endChannel = System.currentTimeMillis();
//        Log.i(Constants.TAG, "Channel Conversion Time: " + (endChannel - startChannel) + "mS"); 
		return channel0;
    	
//    	return result;
	}
    
    public static native void initOpenCL (String openCLProgramText);

	public static native void shutdownOpenCL();
	
    private static native void nativeStepOpenCL (
            int filterSizeParam,
            int epsilonParam,
            int dummy3,
            int dummy4,
            boolean dummy_bool,
            Bitmap inputBitmap,
            Bitmap outputBitmap
        );


    
    
    
	/**
	 * Create submatrix using bytearray, then Mat.minmax().
	 * This solution consumes about 10 seconds per frame.
	 * 
	 * @param original_image
	 * @return
	 */
	private static Mat monochromaticMedianImageFilterUtilizingOpenCv3(Mat original_image) {
		final Size imageSize = original_image.size();
		
		Mat monochromatic_image = new Mat(imageSize, CvType.CV_8UC1);
		Mat hsv_image = new Mat(imageSize, CvType.CV_8UC3);

		Imgproc.cvtColor( original_image, hsv_image, Imgproc.COLOR_RGB2HLS);
		//		Log.i(Constants.TAG, "HSV Image: " + hsv_image); // CV_8UC3

		// Try RGB below
		//		hsv_image = result;


		// Get hue channel into simple byte array for speed efficiency.
		final int numColumns = (int) original_image.size().width;
		final int numRows = (int) original_image.size().height;
		final int span = (int) 7;
		final int accuracy = (int) 5;		
		List<Mat> channels = new LinkedList<Mat>();
		Core.split(hsv_image, channels);
		Mat hueMat = channels.get(0);
		Mat lumMat = channels.get(1);
		Mat satMat = channels.get(2);
		final int bufferSize = numColumns*numRows;
		byte [] hueByteArray = new byte[bufferSize];
		byte [] lumByteArray = new byte[bufferSize];
		byte [] satByteArray = new byte[bufferSize];
		hueMat.get(0,0,hueByteArray); // get all the pixels
		lumMat.get(0,0,lumByteArray); // get all the pixels
		satMat.get(0,0,satByteArray); // get all the pixels

		// Output byte array for speed efficiency
		byte [] monochromaticByteArray = new byte[bufferSize];
		
		Mat subimageMat = new Mat(span, span, CvType.CV_8UC1);
		byte [] subimageByteArray = new byte[span*span];


		for(int row=0; row<numRows; row++) {


			byte result_pixel = 0;

			for( int col=0; col<numColumns; col++) {

				if(col < span || (col >= numColumns - span) )
					result_pixel = 0;  // Just put in black
				
				else if(row < span || (row >= numRows - span) )
					result_pixel = 0;  // Just put in black

				else {
					
					// Copy a row (or column)
					for(int i=0; i<span; i++) {
						
						// copy span bytes from (row + i) * numCol + col
						int srcPos = (row + i) * numColumns + col;
						int dstPos = i * span;
						System.arraycopy(hueByteArray, srcPos, subimageByteArray, dstPos, span);
					}
						
					subimageMat.put(0, 0, subimageByteArray);
					Core.MinMaxLocResult minMaxResult = Core.minMaxLoc(subimageMat);
					
					
					if( (( minMaxResult.maxVal - minMaxResult.maxVal) < accuracy) ) //&& (lum_max - lum_min < accuracy) && (sat_max - sat_min < accuracy) )
						result_pixel = (byte) 128;
					else
						result_pixel= (byte) 0;


					//					Log.i(Constants.TAG, String.format("Lum %d %d", lum_min, lum_max));

				}  // End of else

				if( (col >= span/2)  && (row >= span/2) )
					monochromaticByteArray[ (row - span/2) * numColumns + (col - span/2)] = result_pixel;

//				int test = (int)(satByteArray[row * numColumns + col]) & 0xFF;
//				monochromaticByteArray[row * numColumns + (col - span/2)] = (byte) test;
			
			}  // End of column sweep
			
			
		} // End of row sweep
		Log.i(Constants.TAG, "Completed MonoChromatic CV");
		monochromatic_image.put(0, 0, monochromaticByteArray);
		return monochromatic_image;
	}




	/**
	 * Use mask operation and then min max.
	 * This solution consumes about 20 minutes per frame!
	 * 
	 * @param original_image
	 * @return
	 */
	@SuppressWarnings("unused")
	private static Mat monochromaticMedianImageFilterUtilizingOpenCv2(Mat original_image) {
		
		final Size imageSize = original_image.size();
		final int numColumns = (int) original_image.size().width;
		final int numRows = (int) original_image.size().height;
		final int bufferSize = numColumns*numRows;
		final int span = (int) 7;
		final int accuracy = (int) 5;
		
		
		Mat hsv_image = new Mat(imageSize, CvType.CV_8UC3);
		Imgproc.cvtColor( original_image, hsv_image, Imgproc.COLOR_RGB2HLS);
		List<Mat> channels = new LinkedList<Mat>();
		Core.split(hsv_image, channels);
		Mat hueMat = channels.get(0);
		Mat lumMat = channels.get(1);
		Mat satMat = channels.get(2);
		
		// Output byte array for speed efficiency
		Mat monochromatic_image = new Mat(imageSize, CvType.CV_8UC1);
		byte [] monochromaticByteArray = new byte[bufferSize];
		
		Mat mask = Mat.zeros(numRows, numColumns, CvType.CV_8UC1);

		Log.i(Constants.TAG, "Begin MonoChromatic CV");
		for(int row=0; row<numRows; row++) {

			byte result_pixel = 0;

			for( int col=0; col<numColumns; col++) {

				if(col < span || (col >= numColumns - span) )
					result_pixel = 0;  // Just put in black
				
				else if(row < span || (row >= numRows - span) )
					result_pixel = 0;  // Just put in black

				else {	

//					Log.i(Constants.TAG, "Creating Mask at " + row +"," + col);
					Core.rectangle(
							mask, 
							new Point(row, col), 
							new Point(row + span, col + span),
							new Scalar(1, 1, 1) );
					
					
//					Core.MinMaxLocResult minMaxResult = Core.minMaxLoc(hueMat, mask);
					Mat subset = new Mat();
					hueMat.copyTo(subset, mask);
					Core.MinMaxLocResult minMaxResult = Core.minMaxLoc(subset);
					
					
					if( (( minMaxResult.maxVal - minMaxResult.maxVal) < accuracy) ) //&& (lum_max - lum_min < accuracy) && (sat_max - sat_min < accuracy) )
						result_pixel = (byte) 128;
					else
						result_pixel= (byte) 0;
//					Log.i(Constants.TAG, "Completed Mask at " + row +"," + col);
					
					Core.rectangle(
							mask, 
							new Point(row, col), 
							new Point(row + span, col + span),
							new Scalar(0, 0, 0) );
				}


				
				if( (col >= span/2)  && (row >= span/2) )
					monochromaticByteArray[ (row - span/2) * numColumns + (col - span/2)] = result_pixel;
			}
			

			Log.i(Constants.TAG, "Completed Row: " + row);
		}
		
		monochromatic_image.put(0, 0, monochromaticByteArray);
		Log.i(Constants.TAG, "Completed MonoChromatic CV");
//		System.exit(0);
		return monochromatic_image;
	}


	/**
	 * Use OpenCV minMax.
	 * 
	 * However, this is enormously slow, taking 10 minutes per frame!  Why?
	 * I think because it is effective O(O^4) in computation.
	 * 
	 * @param original_image
	 * @return
	 */
	@SuppressWarnings("unused")
	private static Mat monochromaticMedianImageFilterUtilizingOpenCv(Mat original_image) {
		
		final Size imageSize = original_image.size();
		final int numColumns = (int) original_image.size().width;
		final int numRows = (int) original_image.size().height;
		final int bufferSize = numColumns*numRows;
		final int span = (int) 7;
		final int accuracy = (int) 5;
		
		
		Mat hsv_image = new Mat(imageSize, CvType.CV_8UC3);
		Imgproc.cvtColor( original_image, hsv_image, Imgproc.COLOR_RGB2HLS);
		List<Mat> channels = new LinkedList<Mat>();
		Core.split(hsv_image, channels);
		Mat hueMat = channels.get(0);
		Mat lumMat = channels.get(1);
		Mat satMat = channels.get(2);
		
		// Output byte array for speed efficiency
		Mat monochromatic_image = new Mat(imageSize, CvType.CV_8UC1);
		byte [] monochromaticByteArray = new byte[bufferSize];
		

		Mat mask = Mat.zeros(numRows, numColumns, CvType.CV_8UC1);

		Log.i(Constants.TAG, "Begin MonoChromatic CV");
		for(int row=0; row<numRows; row++) {

			byte result_pixel = 0;

			for( int col=0; col<numColumns; col++) {

				if(col < span || (col >= numColumns - span) )
					result_pixel = 0;  // Just put in black
				
				else if(row < span || (row >= numRows - span) )
					result_pixel = 0;  // Just put in black

				else {	

//					Log.i(Constants.TAG, "Creating Mask at " + row +"," + col);
					Core.rectangle(
							mask, 
							new Point(row, col), 
							new Point(row + span, col + span),
							new Scalar(1, 1, 1) );
					
					
					Core.MinMaxLocResult minMaxResult = Core.minMaxLoc(hueMat, mask);
					
					if( (( minMaxResult.maxVal - minMaxResult.maxVal) < accuracy) ) //&& (lum_max - lum_min < accuracy) && (sat_max - sat_min < accuracy) )
						result_pixel = (byte) 128;
					else
						result_pixel= (byte) 0;
//					Log.i(Constants.TAG, "Completed Mask at " + row +"," + col);
					
					Core.rectangle(
							mask, 
							new Point(row, col), 
							new Point(row + span, col + span),
							new Scalar(0, 0, 0) );
				}


				
				if( (col >= span/2)  && (row >= span/2) )
					monochromaticByteArray[ (row - span/2) * numColumns + (col - span/2)] = result_pixel;
			}
			

			Log.i(Constants.TAG, "Completed Row: " + row);
			
		}
		
		monochromatic_image.put(0, 0, monochromaticByteArray);
		Log.i(Constants.TAG, "Completed MonoChromatic CV");
//		System.exit(0);
		return monochromatic_image;
	}

	/**
	 * Simple algorithm in Java.  Java byte arrays of the original image
	 * are obtain and operated on to then produce a resulting Java byte
	 * array.
	 * 
	 * 
	 * @param original_image
	 * @return
	 */
	private static Mat monochromaticMedianImageFilterBruteForceInJava(Mat original_image) {
		
		final Size imageSize = original_image.size();
		
		Mat monochromatic_image = new Mat(imageSize, CvType.CV_8UC1);
		Mat hsv_image = new Mat(imageSize, CvType.CV_8UC3);

		Imgproc.cvtColor( original_image, hsv_image, Imgproc.COLOR_RGB2HLS);
		//		Log.i(Constants.TAG, "HSV Image: " + hsv_image); // CV_8UC3

		// Try RGB below
		//		hsv_image = result;


		// Get hue channel into simple byte array for speed efficiency.
		final int numColumns = (int) original_image.size().width;
		final int numRows = (int) original_image.size().height;
		List<Mat> channels = new LinkedList<Mat>();
		Core.split(hsv_image, channels);
		Mat hueMat = channels.get(0);
		Mat lumMat = channels.get(1);
		Mat satMat = channels.get(2);
		final int bufferSize = numColumns*numRows;
		byte [] hueByteArray = new byte[bufferSize];
		byte [] lumByteArray = new byte[bufferSize];
		byte [] satByteArray = new byte[bufferSize];
		hueMat.get(0,0,hueByteArray); // get all the pixels
		lumMat.get(0,0,lumByteArray); // get all the pixels
		satMat.get(0,0,satByteArray); // get all the pixels

		// Output byte array for speed efficiency
		byte [] monochromaticByteArray = new byte[bufferSize];


		for(int row=0; row<numRows; row++) {

			final int span = (int) 7;
			final int accuracy = (int) 5;

			byte result_pixel = 0;

			for( int col=0; col<numColumns; col++) {

				if(col < span )
					result_pixel = 0;  // Just put in black
				
				else if(row < span)
					result_pixel = 0;  // Just put in black

				else {			

					int hue_min = 255;
					int hue_max = 0;
					int lum_min = 255;
					int lum_max = 0;
//					int sat_min = 255;
//					int sat_max = 0;

					for(int i=0; i<span; i++) {
						
						for(int j=0; j<span; j++) {

						int hue = (int)hueByteArray[(row - j) * numColumns + (col - i) ] & 0xFF;
						if(hue > hue_max)
							hue_max = hue;
						if(hue < hue_min)
							hue_min = hue;

						int lum = (int)lumByteArray[(row - j) * numColumns + (col - i) ] & 0xFF;
						if(lum > lum_max)
							lum_max = lum;
						if(lum < lum_min)
							lum_min = lum;
						
// =+= Saturation does not look correct when veiw as gray scale image.  Not sure what is going on.
//						int sat = (int)satByteArray[row * numColumns + (col - i) ] & 0xFF;
//						if(sat > sat_max)
//							sat_max = sat;
//						if(sat < sat_min)
//							sat_min = sat;
						
						} // End of row min/max sweep
					}  // End of column min/max sweep

					if( (hue_max - hue_min < accuracy) ) //&& (lum_max - lum_min < accuracy) && (sat_max - sat_min < accuracy) )
						result_pixel = (byte) 128;
					else
						result_pixel= (byte) 0;

					// Eliminate all black areas from consideration even if they are very flat.
					// For some reason, keying off minimum lumosity works best.	
					if( lum_min < 30 )
						result_pixel = 0;

					//					Log.i(Constants.TAG, String.format("Lum %d %d", lum_min, lum_max));

				}  // End of else

				if( (col >= span/2)  && (row >= span/2) )
					monochromaticByteArray[ (row - span/2) * numColumns + (col - span/2)] = result_pixel;

//				int test = (int)(satByteArray[row * numColumns + col]) & 0xFF;
//				monochromaticByteArray[row * numColumns + (col - span/2)] = (byte) test;
			
			}  // End of column sweep
			
			
		} // End of row sweep

		monochromatic_image.put(0, 0, monochromaticByteArray);
		return monochromatic_image;
	}

}
