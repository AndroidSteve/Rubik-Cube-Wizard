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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.ar.rubik.Constants.ColorTileEnum;
import org.kociemba.twophase.PruneTableLoader;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.highgui.Highgui;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

public class Util {



	public static String dumpRGB(ColorTileEnum colorTile) {
		double[] val = colorTile.cvColor.val;
        return String.format("r=%3.0f g=%3.0f b=%3.0f        ", val[0], val[1], val[2]);
	}
	
	public static String dumpRGB(double[] color, double colorError) {
		return String.format("r=%3.0f g=%3.0f b=%3.0f e=%5.0f", color[0], color[1], color[2], colorError);
	}

//	public static String dumpRGB(ConstantTile logicalTile) {
//		double color[] = logicalTile.colorOpenCV.val;
//		return String.format("r=%3.0f g=%3.0f b=%3.0f     t=%c", color[0], color[1], color[2], logicalTile.symbol);
//	}
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
	 * Re-Evaluate Tile Colors
	 * 
	 * Re-examine tile colors across entire cube.  Adjust selection so that there are nine tiles
	 * of each color.  This provides much more robustness with respect to lighting conditions.
	 * 
	 * @param stateModel 
	 */
	public static void reevauateSelectTileColors(StateModel stateModel) {

		/*
		 * Populate a 3D space with 54 measure tile colors.
		 *  	
		 * Set initial color points as per those values found in constant color
		 *  
		 * WHILE
		 * 
		 *   Assign points to color enum per nearest neighbor algorithm.
		 * 
		 *   Calculate total error
		 *   
		 *   IF( < threshold) break
		 *   
		 *   IF too many iterations break
		 *   
		 *   Calculate new color points as average of closest 5 points per region
		 *   
		 */    	

	}


	/**
	 * Create a new array instance object, populate it with tiles rotated clockwise
	 * with respect to the pass in arg, and then return the new object.
	 * 
	 * @param arg
	 * @return
	 */
	public static ColorTileEnum[][] getTileArrayRotatedClockwise(ColorTileEnum[][] arg) {	
		//         n -------------->
		//   m     0-0    1-0    2-0
		//   |     0-1    1-1    2-1
		//   v     0-2    1-2    2-2
		ColorTileEnum [][] result = new ColorTileEnum[3][3];
		result[1][1] = arg[1][1];
		result[2][0] = arg[0][0];
		result[2][1] = arg[1][0];
		result[2][2] = arg[2][0];
		result[1][2] = arg[2][1];
		result[0][2] = arg[2][2];
		result[0][1] = arg[1][2];
		result[0][0] = arg[0][2];
		result[1][0] = arg[0][1];
		
		return result;
	}
	public static ColorTileEnum [][] getTileArrayRotatedCounterClockwise(ColorTileEnum[][] arg) {
		return getTileArrayRotatedClockwise( getTileArrayRotatedClockwise( getTileArrayRotatedClockwise( arg)));
	}
	public static ColorTileEnum [][] getTileArrayRotated180(ColorTileEnum[][] arg) {
		return getTileArrayRotatedClockwise( getTileArrayRotatedClockwise( arg));
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


	/**
	 * Load Rubik Logic Algorithm Pruning Tables is a separate thread.
	 * 
	 * @author stevep
	 *
	 */
	public static class LoadPruningTablesTask extends AsyncTask<AppStateMachine, Void, Void> {
		
	    private PruneTableLoader tableLoader = new PruneTableLoader();
	    private AppStateMachine appStateMachine;

	    @Override
	    protected Void doInBackground(AppStateMachine... params) {
	    	
	    	appStateMachine = params[0];
	    	
	        /* load all tables if they are not already in RAM */
	        while (!tableLoader.loadingFinished()) { // while tables are left to load
	            tableLoader.loadNext(); // load next pruning table
	            appStateMachine.pruneTableLoaderCount++;
	            Log.i(Constants.TAG_CNTRL, "Created a prune table.");
	        }
	        Log.i(Constants.TAG_CNTRL, "Completed all prune table.");
	        return null;
	    }

	    @Override
	    protected void onProgressUpdate(Void... values) {
	    }
	}
	



    /**
     * Read Text File from Assets
     * 
     * @param context
     * @param fileName
     * @return
     */
    public static String readTextFileFromAssets(Context context, String fileName) {

        try
        {
            InputStream stream = context.getAssets().open(fileName);
            InputStreamReader inputStreamReader = new InputStreamReader(stream);
            BufferedReader reader = new BufferedReader(inputStreamReader);

            StringBuilder buffer = new StringBuilder();
            String str;

            while((str = reader.readLine()) != null)
            {
                buffer.append(str);
                buffer.append("\n");
            }

            reader.close();
            return buffer.toString();
        }
        catch (IOException e) {
            throw new RuntimeException("Could not open asset: " + fileName, e);

        }

    }
	
}
