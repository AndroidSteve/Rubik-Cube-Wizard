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
import java.util.HashSet;

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
	 * Returns true if there are exactly nine of each tile color over entire cube,
	 * and if no two center tiles have the same color.
	 * 
	 * @return
	 */
    public static boolean isTileColorsValid(StateModel stateModel) {
    	
    	// Count how many tile colors entire cube has as a first check.
    	int [] numColorTilesArray = new int[] {0, 0, 0, 0, 0, 0};
		for(RubikFace rubikFace : stateModel.nameRubikFaceMap.values() ) {
			for(int n=0; n<3; n++) {
				for(int m=0; m<3; m++) {
					numColorTilesArray[ rubikFace.observedTileArray[n][m].ordinal() ]++;    // constantTileColor.ordinal() ]++;
				}
			}	
		}
		
		// Check that we have nine of each tile color over entire cube.
		for(ColorTileEnum colorTile : ColorTileEnum.values()) {
		    if(colorTile.isRubikColor == true) {
		        if( numColorTilesArray[colorTile.ordinal()] != 9) {
		        	Log.i(Constants.TAG_COLOR, "REJECT: There are " + numColorTilesArray[colorTile.ordinal()] + " tiles of color " + colorTile + ", and there should be exactly 9");
		            return false;
		        }
		    }
		}
		
		// Check that there are exactly six elements in the above set.
		HashSet<ColorTileEnum> centerTileSet = new HashSet<ColorTileEnum>(16);
		for(RubikFace rubikFace : stateModel.nameRubikFaceMap.values() ) {
			ColorTileEnum colorTile = rubikFace.observedTileArray[1][1];
			if(centerTileSet.contains(colorTile)) {
				Log.i(Constants.TAG_COLOR, "REJECT: There are two center tiles that have been assigned the same color of:" + colorTile);			
				return false;
			}
			else
				centerTileSet.add(colorTile);
		}
		
		return true;
    }
	
//    /**
//     * =+=
//     * 
//     * This suggest we need:
//     *  Tile Class
//     *  o Referenced from:  RubikFace.tiles[n][m]
//     *  o Member Data:
//     *      - Rohmbi      rhombi
//     *      - ColorEnum   originalDesignation
//     *      - ColorEnum   transposedDesignation
//     *      - Scalar      measuredColor
//     *      - float       distanceToMutableDesignation
//     *
//     * @param stateModel 
//     */	
//	private static class TileLocation {
//	    RubikFace rubikFace;
//	    int n;
//	    int m;
//        public TileLocation(RubikFace rubikFace, int n, int m) {
//            this.rubikFace = rubikFace;
//            this.n = n;
//            this.m = m;
//        }
//	}
//
//    
//	/**
//	 * Re-Evaluate Tile Colors
//	 * 
//	 * Re-examine tile colors across entire cube.  Adjust selection so that there are nine tiles
//	 * of each color.  This provides much more robustness with respect to lighting conditions.
//	 *
//	 * @param stateModel 
//	 */
//    public static void reevauateSelectTileColors(StateModel stateModel) {
//
//	    
//	    // =+= this algorithm needs to run before transformedArray[][] is created.
//	    
//	    /*
//	     * Modified Mean Shift Algorithm
//	     * 
//	     * Make copy of constant rubik colors into mutable array
//	     * 
//	     * 
//	     * Assign each tile to closest mutable rubik color.
//	     * 
//	     * WHILE (there are NOT nine tiles of each color type)
//	     * 
//	     *   IF (too many iterations)
//	     *     ERROR
//	     *    
//         *   FOREACH mutable rubik color
//         *   
//         *     Find closest five tiles
//         *     
//         *     Calculate new color value base on above selection
//         *     
//         *   END FOREACH
//	     * 
//	     * END WHILE
//	     */
//        
//        stateModel.mutableTileColors.clear();
//        for(ColorTileEnum colorTile : ColorTileEnum.values())
//            if(colorTile.isRubikColor == true) {
//                stateModel.mutableTileColors.put(colorTile, colorTile.rubikColor);
//                Log.e(Constants.TAG_COLOR, "Color Tile = " + colorTile + ", Initial Color =" + Arrays.toString(stateModel.mutableTileColors.get(colorTile).val) );
//            }
//
//	    // Assign each tile to closest mutable rubik color.
//	    for(RubikFace rubikFace : stateModel.nameRubikFaceMap.values())
//	        assignColorToTiles(rubikFace, stateModel.mutableTileColors);
//
//	    // Loop until a valid solution (i.e., mapping of measured tile colors to logical Color Tiles).
//	    int itterationCount = 0;
//	    while(stateModel.isTileColorsValid() == false) {
//	        
//	        if(itterationCount++ > 3) {
//	            Log.e(Constants.TAG_COLOR, "Error: could not converge on correct tile color designations.");
//	            return;
//	        }
//
//	        // Loop over available tile colors.
//	        for (Map.Entry<ColorTileEnum, Scalar> colorTileEntry : stateModel.mutableTileColors.entrySet()) {
//           
//	            //
//	            ColorTileEnum colorTile = colorTileEntry.getKey();
//	            Scalar mutableColor = colorTileEntry.getValue();
//                
//                if(colorTile.isRubikColor == false)
//                    continue;
//                
//                // Map of color error distance (smallest to largest) mapped to location on cube for the current colorTileEnum
//                TreeMap<Double, TileLocation> colorErrorDistanceMap = new TreeMap<Double, TileLocation>();
//                
//	            // Count up total number of this color tile across all 54 locations.
//	            for(RubikFace rubikFace : stateModel.nameRubikFaceMap.values()) {
//
//	                for(int n=0; n<3; n++) {
//	                    for(int m=0; m<3; m++) {
//
//	                        // This location has color tile of current interest.
//	                        if(rubikFace.observedTileArray[n][m] == colorTile) {
//	                       
//	                            double[] measuredColor = rubikFace.measuredColorArray[n][m];
//	                            
//	                            // Calculate distance
//	                            double distance =
//	                                    (measuredColor[0] - mutableColor.val[0]) * (measuredColor[0] - mutableColor.val[0]) +
//	                                    (measuredColor[1] - mutableColor.val[1]) * (measuredColor[1] - mutableColor.val[1]) +
//	                                    (measuredColor[2] - mutableColor.val[2]) * (measuredColor[2] - mutableColor.val[2]);
//	                            
//	                            TileLocation tileLocation = new Util.TileLocation(rubikFace, n, m);
//                                colorErrorDistanceMap.put(distance, tileLocation);
//	                        }
//	                    }
//	                }
//	            }
//
//	            // Obtain averaged measured color of closest 5 tiles to colorTile.
//	            int tileCount = 0;
//	            double [] meanMeasuredColor = {0, 0, 0, 0};
//
//	            for( Entry<Double, TileLocation> distanceEntry : colorErrorDistanceMap.entrySet() ) {
//	                TileLocation tileLocation = distanceEntry.getValue();
//	                double distance = distanceEntry.getKey();
//
//	                Log.e(Constants.TAG_COLOR, "Color Tile = " + colorTile + ", Count = " + tileCount + ", Distance = " + distance);
//	                
//	                double[] measuredColor = tileLocation.rubikFace.measuredColorArray[tileLocation.n][tileLocation.m];
//
//	                // Accumulate RGBs into array.
//	                for(int i=0; i<3; i++)
//	                    meanMeasuredColor[i] += measuredColor[i];
//
//	                tileCount++;
//	                if(tileCount >= 5)
//	                    break;
//	            }
//	            
//	            // Safety check in case there were no tiles assigned to this color.
//	            if(tileCount == 0)
//	                continue;
//	            
//	            // Calculate average RGB color values
//                for(int i=0; i<3; i++)
//                    meanMeasuredColor[i] /= tileCount;	            
//
//	            // Update mutable color with new mean value.
//	            colorTileEntry.setValue(new Scalar(meanMeasuredColor));
//	            
//	            Log.e(Constants.TAG_COLOR, "Color Tile = " + colorTile + ", Count = " + tileCount + ", New Mean Color =" + Arrays.toString(stateModel.mutableTileColors.get(colorTile).val) );
//
//	        }  // End loop over color tiles
//
//	    }  // End while loop
//	    
//	    // Assign each tile to closest mutable rubik color.
//        for(RubikFace rubikFace : stateModel.nameRubikFaceMap.values())
//            assignColorToTiles(rubikFace, stateModel.mutableTileColors);
//	    
//	}  // End function
//
//
//	
//	private static void assignColorToTiles(RubikFace rubikFace, HashMap<ColorTileEnum, Scalar> mutableTileColors) {
//	    
//	    for(int n=0; n<3; n++) {
//	        for(int m=0; m<3; m++) {
//
//	            double [] measuredColor = rubikFace.measuredColorArray[n][m];
//
//	            double smallestError = Double.MAX_VALUE;
//	            ColorTileEnum bestCandidate = null;
//
//	            // Loop over available tile colors
//	            for (Map.Entry<ColorTileEnum, Scalar> entry : mutableTileColors.entrySet()) {
//	                ColorTileEnum candidateColorTile = entry.getKey();
//	                Scalar mutableColor = entry.getValue();
//
//	                double error =
//	                        (measuredColor[0] - mutableColor.val[0]) * (measuredColor[0] - mutableColor.val[0]) +
//	                        (measuredColor[1] - mutableColor.val[1]) * (measuredColor[1] - mutableColor.val[1]) +
//	                        (measuredColor[2] - mutableColor.val[2]) * (measuredColor[2] - mutableColor.val[2]);
//
//	                if(error < smallestError) {
//	                    bestCandidate = candidateColorTile;
//	                    smallestError = error;
//	                }
//	            }
//
//	            Log.d(Constants.TAG_COLOR, String.format( "Face %s Tile[%d][%d] has R=%3.0f, G=%3.0f B=%3.0f %c err=%4.0f", rubikFace.faceNameEnum, n, m, measuredColor[0], measuredColor[1], measuredColor[2], bestCandidate.symbol, smallestError));
//
//	            // Assign best candidate to this tile location.
//	            if(rubikFace.observedTileArray[n][m] != bestCandidate) {
//	                Log.w(Constants.TAG_COLOR, String.format( "Changing Tile Designation for Face %s at [%d][%d] from %s to %s",rubikFace.faceNameEnum, n, m, rubikFace.observedTileArray[n][m], bestCandidate));
//	                rubikFace.observedTileArray[n][m] = bestCandidate;
//	            }
//	        }
//	    }
//	}


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
			Log.i(Constants.TAG_STATE, "SUCCESS writing image to external storage:" + filename);
		else
			Log.e(Constants.TAG_STATE, "Fail writing image to external storage");
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
	 * @author android.steve@cl-sw.com
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
	            Log.i(Constants.TAG_STATE, "Created a prune table.");
	        }
	        Log.i(Constants.TAG_STATE, "Completed all prune table.");
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
