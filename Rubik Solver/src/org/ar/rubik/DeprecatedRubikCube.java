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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.ar.rubik.Constants.ConstantTile;
import org.ar.rubik.Constants.ConstantTileColorEnum;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import android.os.Environment;
import android.util.Log;

public class DeprecatedRubikCube {
	
	public static DeprecatedRubikFace active;
	
	// An array of Rubik Face Objects.  Order is per Logical Tile Color Enum Ordinal. 
	// Faces are arranged by color of center tile.  
	public static DeprecatedRubikFace [] rubikFaceArray = new DeprecatedRubikFace[6];
	
	// Count how many tile colors entire cube has as a first check.
	private static int [] numColorTilesArray = new int[6];

	
	// We define this relationship: White on Top and Red on Front.
	public static DeprecatedRubikFace getUpFace()    {return rubikFaceArray[ConstantTileColorEnum.WHITE.ordinal()];}
	public static DeprecatedRubikFace getDownFace()  {return rubikFaceArray[ConstantTileColorEnum.YELLOW.ordinal()];}
	public static DeprecatedRubikFace getRightFace() {return rubikFaceArray[ConstantTileColorEnum.BLUE.ordinal()];}
	public static DeprecatedRubikFace getLeftFace()  {return rubikFaceArray[ConstantTileColorEnum.GREEN.ordinal()];}
	public static DeprecatedRubikFace getFrontFace() {return rubikFaceArray[ConstantTileColorEnum.RED.ordinal()];}
	public static DeprecatedRubikFace getBackFace()  {return rubikFaceArray[ConstantTileColorEnum.ORANGE.ordinal()];}
		
	
	
	/**
	 * Render Flat Layout Representation
	 * 
	 * Render each side, if available.
	 * This will render the picture as show in file Facelet.java
	 * 
	 * @param image
	 */
	public static void renderFlatLayoutRepresentation(Mat image) {
		
		final int tSize = 35;  // Tile Size in pixels
		
		
		// Faces are orientated as per Face Recognition (and N, M axis)
		DeprecatedRubikFace.drawFlatFaceRepresentation(image, getUpFace(),     3 * tSize, 0 * tSize + 70, tSize);
		DeprecatedRubikFace.drawFlatFaceRepresentation(image, getLeftFace(),   0 * tSize, 3 * tSize + 70, tSize);
		DeprecatedRubikFace.drawFlatFaceRepresentation(image, getFrontFace(),  3 * tSize, 3 * tSize + 70, tSize);
		DeprecatedRubikFace.drawFlatFaceRepresentation(image, getRightFace(),  6 * tSize, 3 * tSize + 70, tSize);
		DeprecatedRubikFace.drawFlatFaceRepresentation(image, getBackFace(),   9 * tSize, 3 * tSize + 70, tSize);
		DeprecatedRubikFace.drawFlatFaceRepresentation(image, getDownFace(),   3 * tSize, 6 * tSize + 70, tSize);

		// Faces are orientated as per a paper cut-out that can be folded into a cube.
		DeprecatedRubikFace.drawLogicalFlatFaceRepresentation(image, getUpFace(),    getVirtualLogicalTileArray(  getUpFace()),     3 * tSize, 0 * tSize + 400, tSize);
		DeprecatedRubikFace.drawLogicalFlatFaceRepresentation(image, getLeftFace(),  getVirtualLogicalTileArray(  getLeftFace()),   0 * tSize, 3 * tSize + 400, tSize);
		DeprecatedRubikFace.drawLogicalFlatFaceRepresentation(image, getFrontFace(), getVirtualLogicalTileArray(  getFrontFace()),  3 * tSize, 3 * tSize + 400, tSize);
		DeprecatedRubikFace.drawLogicalFlatFaceRepresentation(image, getRightFace(), getVirtualLogicalTileArray(  getRightFace()),  6 * tSize, 3 * tSize + 400, tSize);
		DeprecatedRubikFace.drawLogicalFlatFaceRepresentation(image, getBackFace(),  getVirtualLogicalTileArray(  getBackFace()),   9 * tSize, 3 * tSize + 400, tSize);
		DeprecatedRubikFace.drawLogicalFlatFaceRepresentation(image, getDownFace(),  getVirtualLogicalTileArray(  getDownFace()),   3 * tSize, 6 * tSize + 400, tSize);
	}
	
	
	/**
	 * Corrects Face Orientation.
	 * 
	 * These rotations are simply obtained empirically until the cube layout is correct.
	 * Calibration is: white on top, red to the left, gree to the right.
	 * 
	 * @param rubikFace
	 * @return
	 */
	private static ConstantTile[][] getVirtualLogicalTileArray( DeprecatedRubikFace rubikFace) {

		if(rubikFace == null)
			return null;

		if(rubikFace.logicalTileArray[1][1] == null)
			return null;

		switch(rubikFace.logicalTileArray[1][1].constantTileColor) {

		case RED:
			return getVirtualTileArrayRotatedClockwise(rubikFace.logicalTileArray);
		case BLUE:
			return getVirtualTileArrayRotated180(rubikFace.logicalTileArray);
		case GREEN:
			return getVirtualTileArrayRotatedClockwise(rubikFace.logicalTileArray);
		case ORANGE:
			return getVirtualTileArrayRotated180(rubikFace.logicalTileArray);
		case WHITE:
			return getVirtualTileArrayRotatedClockwise(rubikFace.logicalTileArray);
		case YELLOW:
			return rubikFace.logicalTileArray;
		}

		return null; // =+= ??
	}


	

	public static ConstantTile[][] getVirtualTileArrayRotatedClockwise(ConstantTile[][] arg) {	
		//         n -------------->
		//   m     0-0    1-0    2-0
		//   |     0-1    1-1    2-1
		//   v     0-2    1-2    2-2
		ConstantTile [][] result = new ConstantTile[3][3];
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
	public static ConstantTile [][] getVirtualTileArrayRotatedCounterClockwise(ConstantTile[][] arg) {
		return getVirtualTileArrayRotatedClockwise( getVirtualTileArrayRotatedClockwise( getVirtualTileArrayRotatedClockwise( arg)));
	}
	public static ConstantTile [][] getVirtualTileArrayRotated180(ConstantTile[][] arg) {
		return getVirtualTileArrayRotatedClockwise( getVirtualTileArrayRotatedClockwise( arg));
	}


	/**
	 * Adopt Face
	 * 
	 * @param rubikFace
	 */
	public static void adopt(DeprecatedRubikFace rubikFace) {
		// This is becoming archaic
		active = rubikFace;
		
		ConstantTileColorEnum tileColor = rubikFace.logicalTileArray[1][1].constantTileColor;
		int ordinal = tileColor.ordinal();
		rubikFaceArray[ordinal] = rubikFace;
	}
	
	
	/**
	 * Returns true if all six faces have been obtained.
	 * 
	 * @return
	 */
	public static boolean isThereAfullSetOfFaces() {
		for(ConstantTileColorEnum constantTileColor : Constants.ConstantTileColorEnum.values()) {
			if( rubikFaceArray[constantTileColor.ordinal()] == null)
				return false;
		}
		return true;
	}
	
	
	/**
	 * @return
	 */
	public static int getNumValidFaces() {
		int faceCount = 0;
		for(ConstantTileColorEnum colorEnum : ConstantTileColorEnum.values()) {
			if( rubikFaceArray[colorEnum.ordinal()] != null)
				faceCount++;
		}
		return faceCount;
	}
	
	
	/**
	 * Returns true if there are exactly nine of each tile cover over entire cube.
	 * 
	 * @return
	 */
	public static boolean isTileColorsValid() {
		
		// Reset tile color count
		for(ConstantTileColorEnum constantTileColor : Constants.ConstantTileColorEnum.values()) {
			numColorTilesArray[constantTileColor.ordinal()] = 0;
		}

		// Count up
		for(ConstantTileColorEnum constantTileColor : Constants.ConstantTileColorEnum.values()) {
			DeprecatedRubikFace rubikFace = rubikFaceArray[constantTileColor.ordinal()];
			for(int n=0; n<3; n++) {
				for(int m=0; m<3; m++) {
					numColorTilesArray[ rubikFace.logicalTileArray[n][m].constantTileColor.ordinal() ]++;
				}
			}
		}
		
		// Check that we have nine of each tile color over entire cube.
		for(ConstantTileColorEnum constantTileColor : Constants.ConstantTileColorEnum.values()) {
			if( numColorTilesArray[constantTileColor.ordinal()] != 9)
				return false;
		}
		return true;
	}
		
		

	/**
	 * Get String Representation of Cube
	 * 
	 * This should only be called if cube if colors are valid.
	 * 
	 * @return
	 */
	public static String getStringRepresentationOfCube() {
		StringBuffer sb = new StringBuffer();
		sb.append(getStringRepresentationOfFace( getUpFace() ) );
		sb.append(getStringRepresentationOfFace( getRightFace() ) );
		sb.append(getStringRepresentationOfFace( getFrontFace() ) );
		sb.append(getStringRepresentationOfFace( getDownFace() ) );
		sb.append(getStringRepresentationOfFace( getLeftFace() ) );
		sb.append(getStringRepresentationOfFace( getBackFace() ) );
		return sb.toString();
	}



	/**
	 * Get String Representing a particular Face.
	 * 
	 * @param rubikFace
	 * @return
	 */
	private static StringBuffer getStringRepresentationOfFace(DeprecatedRubikFace rubikFace) {
		StringBuffer sb = new StringBuffer();
		ConstantTile[][] virtualLogicalTileArray = getVirtualLogicalTileArray(rubikFace);
		for(int m=0; m<3; m++)
			for(int n=0; n<3; n++)
				sb.append(getCharacterRepresentingColor(virtualLogicalTileArray[n][m].constantTileColor));
		return sb;
	}
	
	
	/**
	 * Get Character Representing Color
	 * 
	 * =+= Note, this is in according to definition mapping at top of file, and that in file Facelet.java
	 * 
	 * @param constantTileColor
	 * @return
	 */
	private static char getCharacterRepresentingColor(ConstantTileColorEnum constantTileColor) {
		
		switch(constantTileColor) {
		case RED:     return 'F';
		case ORANGE:  return 'B';
		case YELLOW:  return 'D';
		case GREEN:   return 'L';
		case BLUE:    return 'R';
		case WHITE:   return 'U';
		}
		return 0;  // =+= odd error message wihtout this.
	}
	
	
	/**
	 * Reset
	 * 
	 * Forget all previous face information.
	 */
	public static void reset() {
		for(ConstantTileColorEnum constantTileColor : Constants.ConstantTileColorEnum.values()) {
			rubikFaceArray[constantTileColor.ordinal()] = null;
		}
	}
	
	
	
	/**
	 * Render Cube Diagnostic Metrics
	 * 
	 * @param image
	 */
	public static void renderCubeMetrics(Mat image) {
		for(ConstantTileColorEnum constantTileColor : Constants.ConstantTileColorEnum.values()) {
			int ordinal = constantTileColor.ordinal();
			Core.putText(image, String.format("Num %s = %2d", constantTileColor, numColorTilesArray[ordinal]), new Point(50, 200 + 50*ordinal), Constants.FontFace, 2, Constants.ColorWhite, 2);
		}
	}
	
	
	/**
	 * Save cube state to file.
	 */
	public static void saveCube() {
		
		try {
			File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
			String filename = "cube.ser";
			File file = new File(path, filename);
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file));
			out.writeObject(rubikFaceArray);
			out.flush();
			out.close();
			Log.i(Constants.TAG, "SUCCESS writing cube state to external storage:" + filename);
		}
		catch (Exception e) {
			System.out.print(e);
			Log.e(Constants.TAG, "Fail writing cube state to external storage: " + e);
		}
	}
	
	
	/**
	 * Recall cube state from file.
	 */
	public static void recallCube() {
		try {
			File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
			String filename = "cube.ser";
			File file = new File(path, filename);
	        ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
	        rubikFaceArray = (DeprecatedRubikFace[])in.readObject();
	        in.close();
			Log.i(Constants.TAG, "SUCCESS reading cube state to external storage:" + filename);
		}
		catch (Exception e) {
			System.out.print(e);
			Log.e(Constants.TAG, "Fail reading cube to external storage: " + e);
		}
	}


}
