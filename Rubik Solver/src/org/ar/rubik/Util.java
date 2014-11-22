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
 *   Miscellaneous utilities that can exist as simple static functions here in 
 *   this file, and are relatively uninteresting.
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

import java.io.File;

import org.ar.rubik.Constants.ConstantTile;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.highgui.Highgui;

import android.os.Environment;
import android.util.Log;

public class Util {



	public static String dumpRGB(double[] color) {
		return String.format("r=%3.0f g=%3.0f b=%3.0f        ", color[0], color[1], color[2]);
	}
	
	public static String dumpRGB(double[] color, double colorError) {
		return String.format("r=%3.0f g=%3.0f b=%3.0f e=%5.0f", color[0], color[1], color[2], colorError);
	}

	public static String dumpRGB(ConstantTile logicalTile) {
		double color[] = logicalTile.color.val;
		return String.format("r=%3.0f g=%3.0f b=%3.0f     t=%c", color[0], color[1], color[2], logicalTile.character);
	}
	public static String dumpYUV(double[] color) {
		color = getYUVfromRGB(color);
		return String.format("y=%3.0f u=%3.0f v=%3.0f        ", color[0], color[1], color[2]);
	}
	public static String dumpLoc(Rhombus rhombus) {
		if(rhombus == null)
			return "           ";
		else
			return String.format(" %4.0f,%4.0f ", rhombus.center.x, rhombus.center.y);
	}
	public static String dumpPoint(Point point) {
		if(point == null)
			return "           ";
		else
			return String.format(" %4.0f,%4.0f ", point.x, point.y);
	}


	
	/**
	 * Get YUV from RGB
	 * 
	 * @param rgb
	 * @return
	 */
	public static double[] getYUVfromRGB(double [] rgb) {
		
		if(rgb == null)  {
			Log.e(Constants.TAG, "RGB is NULL!");
			return new double[]{0, 0, 0 , 0}; 
		}
		double [] yuv = new double [4];
		yuv[0] =  0.229 * rgb[0]  +   0.587 * rgb[1]  +  0.114 * rgb[2];
		yuv[1] = -0.147 * rgb[0]  +  -0.289 * rgb[1]  +  0.436 * rgb[2];
		yuv[2] =  0.615 * rgb[0]  +  -0.515 * rgb[1]  + -0.100 * rgb[2];
		return yuv;
	}


	/**
	 * Get Two Phase Error String
	 * 
	 * Arg should be a character between 0 and 8 inclusive.
	 * 
	 * @param errorCode
	 * @return
	 */
	public static String getTwoPhaseErrorString(char errorCode) {
		String stringErrorMessage;
		switch (errorCode) {
		case '0':
			stringErrorMessage = "Cube is verified and correct!";
			break;
		case '1':
			stringErrorMessage = "There are not exactly nine facelets of each color!";
			break;
		case '2':
			stringErrorMessage = "Not all 12 edges exist exactly once!";
			break;
		case '3':
			stringErrorMessage = "Flip error: One edge has to be flipped!";
			break;
		case '4':
			stringErrorMessage = "Not all 8 corners exist exactly once!";
			break;
		case '5':
			stringErrorMessage = "Twist error: One corner has to be twisted!";
			break;
		case '6':
			stringErrorMessage = "Parity error: Two corners or two edges have to be exchanged!";
			break;
		case '7':
			stringErrorMessage = "No solution exists for the given maximum move number!";
			break;
		case '8':
			stringErrorMessage = "Timeout, no solution found within given maximum time!";
			break;
		default:
			stringErrorMessage = "Unknown error code returned: ";
			break;
		}
		return stringErrorMessage;
	}


	/**
	 * Save Image to a File
	 * 
	 * @param image
	 */
	public static void saveImage (Mat image) {

		File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		String filename = "cube.png";
		File file = new File(path, filename);

		Boolean bool = null;
		filename = file.toString();
		bool = Highgui.imwrite(filename, image);

		if (bool == true)
			Log.i(Constants.TAG_CNTRL, "SUCCESS writing image to external storage:" + filename);
		else
			Log.e(Constants.TAG_CNTRL, "Fail writing image to external storage");
	}


	/**
	 * Recall Image from a File
	 * 
	 * @return
	 */
	public static Mat recallImage() {

		File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		String filename = "cube.png";
		File file = new File(path, filename);

		filename = file.toString();

		Mat image = Highgui.imread(filename);
		return image;
	}


	
}
