package org.ar.rubik;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.ar.rubik.Constants.LogicalTile;
import org.ar.rubik.Constants.LogicalTileColorEnum;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import android.os.Environment;
import android.util.Log;

public class RubikCube {
	
	public static RubikFace active;
	
	// An array of Rubik Face Objects.  Order is per Logical Tile Color Enum Ordinal. 
	// Faces are arranged by color of center tile.  
	public static RubikFace [] rubikFaceArray = new RubikFace[6];
	
	// Count how many tile colors entire cube has as a first check.
	private static int [] numColorTilesArray = new int[6];

	
	// We define this relationship: White on Top and Red on Front.
	public static RubikFace getUpFace()    {return rubikFaceArray[LogicalTileColorEnum.WHITE.ordinal()];}
	public static RubikFace getDownFace()  {return rubikFaceArray[LogicalTileColorEnum.YELLOW.ordinal()];}
	public static RubikFace getRightFace() {return rubikFaceArray[LogicalTileColorEnum.BLUE.ordinal()];}
	public static RubikFace getLeftFace()  {return rubikFaceArray[LogicalTileColorEnum.GREEN.ordinal()];}
	public static RubikFace getFrontFace() {return rubikFaceArray[LogicalTileColorEnum.RED.ordinal()];}
	public static RubikFace getBackFace()  {return rubikFaceArray[LogicalTileColorEnum.ORANGE.ordinal()];}
		
	
	
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
		RubikFace.drawFlatFaceRepresentation(image, getUpFace(),     3 * tSize, 0 * tSize + 70, tSize);
		RubikFace.drawFlatFaceRepresentation(image, getLeftFace(),   0 * tSize, 3 * tSize + 70, tSize);
		RubikFace.drawFlatFaceRepresentation(image, getFrontFace(),  3 * tSize, 3 * tSize + 70, tSize);
		RubikFace.drawFlatFaceRepresentation(image, getRightFace(),  6 * tSize, 3 * tSize + 70, tSize);
		RubikFace.drawFlatFaceRepresentation(image, getBackFace(),   9 * tSize, 3 * tSize + 70, tSize);
		RubikFace.drawFlatFaceRepresentation(image, getDownFace(),   3 * tSize, 6 * tSize + 70, tSize);

		// Faces are orientated as per a paper cut-out that can be folded into a cube.
		RubikFace.drawLogicalFlatFaceRepresentation(image, getUpFace(),    getVirtualLogicalTileArray(  getUpFace()),     3 * tSize, 0 * tSize + 400, tSize);
		RubikFace.drawLogicalFlatFaceRepresentation(image, getLeftFace(),  getVirtualLogicalTileArray(  getLeftFace()),   0 * tSize, 3 * tSize + 400, tSize);
		RubikFace.drawLogicalFlatFaceRepresentation(image, getFrontFace(), getVirtualLogicalTileArray(  getFrontFace()),  3 * tSize, 3 * tSize + 400, tSize);
		RubikFace.drawLogicalFlatFaceRepresentation(image, getRightFace(), getVirtualLogicalTileArray(  getRightFace()),  6 * tSize, 3 * tSize + 400, tSize);
		RubikFace.drawLogicalFlatFaceRepresentation(image, getBackFace(),  getVirtualLogicalTileArray(  getBackFace()),   9 * tSize, 3 * tSize + 400, tSize);
		RubikFace.drawLogicalFlatFaceRepresentation(image, getDownFace(),  getVirtualLogicalTileArray(  getDownFace()),   3 * tSize, 6 * tSize + 400, tSize);
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
	private static LogicalTile[][] getVirtualLogicalTileArray( RubikFace rubikFace) {

		if(rubikFace == null)
			return null;

		if(rubikFace.logicalTileArray[1][1] == null)
			return null;

		switch(rubikFace.logicalTileArray[1][1].logicalTileColor) {

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
			
//		case RED:
//			return rubikFace.logicalTileArray;
//		case BLUE:
//			return getVirtualTileArrayRotatedCounterClockwise(rubikFace.logicalTileArray);
//		case GREEN:
//			return rubikFace.logicalTileArray;
//		case ORANGE:
//			return getVirtualTileArrayRotatedCounterClockwise(rubikFace.logicalTileArray);
//		case WHITE:
//			return getVirtualTileArrayRotatedClockwise(rubikFace.logicalTileArray);
//		case YELLOW:
//			return rubikFace.logicalTileArray;
		}

		return null; // =+= ??
	}


	

	public static LogicalTile[][] getVirtualTileArrayRotatedClockwise(LogicalTile[][] arg) {	
		//         n -------------->
		//   m     0-0    1-0    2-0
		//   |     0-1    1-1    2-1
		//   v     0-2    1-2    2-2
		LogicalTile [][] result = new LogicalTile[3][3];
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
	public static LogicalTile [][] getVirtualTileArrayRotatedCounterClockwise(LogicalTile[][] arg) {
		return getVirtualTileArrayRotatedClockwise( getVirtualTileArrayRotatedClockwise( getVirtualTileArrayRotatedClockwise( arg)));
	}
	public static LogicalTile [][] getVirtualTileArrayRotated180(LogicalTile[][] arg) {
		return getVirtualTileArrayRotatedClockwise( getVirtualTileArrayRotatedClockwise( arg));
	}


	/**
	 * Adopt Face
	 * 
	 * @param rubikFace
	 */
	public static void adopt(RubikFace rubikFace) {
		// This is becoming archaic
		active = rubikFace;
		
		LogicalTileColorEnum tileColor = rubikFace.logicalTileArray[1][1].logicalTileColor;
		int ordinal = tileColor.ordinal();
		rubikFaceArray[ordinal] = rubikFace;
	}
	
	
	/**
	 * Returns true if all six faces have been obtained.
	 * 
	 * @return
	 */
	public static boolean isThereAfullSetOfFaces() {
		for(LogicalTileColorEnum logicalTileColor : Constants.LogicalTileColorEnum.values()) {
			if( rubikFaceArray[logicalTileColor.ordinal()] == null)
				return false;
		}
		return true;
	}
	
	
	/**
	 * @return
	 */
	public static int getNumValidFaces() {
		int faceCount = 0;
		for(LogicalTileColorEnum colorEnum : LogicalTileColorEnum.values()) {
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
		for(LogicalTileColorEnum logicalTileColor : Constants.LogicalTileColorEnum.values()) {
			numColorTilesArray[logicalTileColor.ordinal()] = 0;
		}

		// Count up
		for(LogicalTileColorEnum logicalTileColor : Constants.LogicalTileColorEnum.values()) {
			RubikFace rubikFace = rubikFaceArray[logicalTileColor.ordinal()];
			for(int n=0; n<3; n++) {
				for(int m=0; m<3; m++) {
					numColorTilesArray[ rubikFace.logicalTileArray[n][m].logicalTileColor.ordinal() ]++;
				}
			}
		}
		
		// Check that we have nine of each tile color over entire cube.
		for(LogicalTileColorEnum logicalTileColor : Constants.LogicalTileColorEnum.values()) {
			if( numColorTilesArray[logicalTileColor.ordinal()] != 9)
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
	private static StringBuffer getStringRepresentationOfFace(RubikFace rubikFace) {
		StringBuffer sb = new StringBuffer();
		LogicalTile[][] virtualLogicalTileArray = getVirtualLogicalTileArray(rubikFace);
		for(int m=0; m<3; m++)
			for(int n=0; n<3; n++)
				sb.append(getCharacterRepresentingColor(virtualLogicalTileArray[n][m].logicalTileColor));
		return sb;
	}
	
	
	/**
	 * Get Character Representing Color
	 * 
	 * =+= Note, this is in according to definition mapping at top of file, and that in file Facelet.java
	 * 
	 * @param logicalTileColor
	 * @return
	 */
	private static char getCharacterRepresentingColor(LogicalTileColorEnum logicalTileColor) {
		
		switch(logicalTileColor) {
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
		for(LogicalTileColorEnum logicalTileColor : Constants.LogicalTileColorEnum.values()) {
			rubikFaceArray[logicalTileColor.ordinal()] = null;
		}
	}
	
	
	
	/**
	 * Render Cube Diagnostic Metrics
	 * 
	 * @param image
	 */
	public static void renderCubeMetrics(Mat image) {
		for(LogicalTileColorEnum logicalTileColor : Constants.LogicalTileColorEnum.values()) {
			int ordinal = logicalTileColor.ordinal();
			Core.putText(image, String.format("Num %s = %2d", logicalTileColor, numColorTilesArray[ordinal]), new Point(50, 200 + 50*ordinal), Constants.FontFace, 2, Constants.ColorWhite, 2);
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
	        rubikFaceArray = (RubikFace[])in.readObject();
	        in.close();
			Log.i(Constants.TAG, "SUCCESS reading cube state to external storage:" + filename);
		}
		catch (Exception e) {
			System.out.print(e);
			Log.e(Constants.TAG, "Fail reading cube to external storage: " + e);
		}
	}


}
