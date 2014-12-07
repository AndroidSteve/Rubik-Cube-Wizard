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
 *   Contains state of Rubik Cube plus a variety of other key application state parameters.
 *   This class represents the "Model" in the MVC design paradigm.
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
import java.util.HashMap;

import org.ar.rubik.Constants.ConstantTile;
import org.ar.rubik.Constants.ConstantTileColorEnum;
import org.ar.rubik.Constants.FaceNameEnum;
import org.ar.rubik.Constants.AppStateEnum;
import org.ar.rubik.Constants.FaceRecogniztionStateEnum;

import android.os.Environment;
import android.util.Log;

/**
 * Rubik Cube and application State
 * 
 * @author android.steve@testlens.com
 */
public class StateModel {

	// Rubik Face of latest processed frame: may or may not be any of the six state objects.
	public RubikFace activeRubikFace;
	
	/*
	 * This is "Rubik Cube State" or "Rubik Cube Model" in model-veiw-controller vernacular.
	 * Array of above rubik face objects index by FaceNameEnum
	 */
	public HashMap<FaceNameEnum, RubikFace> nameRubikFaceMap = new HashMap<Constants.FaceNameEnum, RubikFace>(6);
	
	// Array of above rubik face objects index by TileColorEnum.
	public HashMap<ConstantTileColorEnum, RubikFace> colorRubikFaceMap = new HashMap<Constants.ConstantTileColorEnum, RubikFace>(6);

	// Application State; see AppStateEnum.
	public AppStateEnum appState = AppStateEnum.START;
	
	// Stable Face Recognizer State
    public FaceRecogniztionStateEnum faceRecogniztionState = FaceRecogniztionStateEnum.UNKNOWN;

    // Result when Two Phase algorithm is ask to evaluate if cube in valid.  If valid, code is zero.
	public int verificationResults;
	
	// String notation on how to solve cube.
	public String solutionResults;
	
	// Above, but broken into individual moves.
	public String [] solutionResultsArray;
	
	// Index to above array as to which move we are on.
	public int solutionResultIndex;
	
	// We assume that faces will be explored in a particular sequence.
	private int adoptFaceCount = 0;

	// True if we are to render GL Pilot Cube
	public boolean renderPilotCube = true;

	// Cube Location and Orientation deduced from Face.
	public CubeReconstructor cubeReconstructor;

	// Intrinsic Camera Calibration Parameters from hardware.
	public CameraParameters cameraParameters;
	
	
	/**
	 * Default State Model Constructor
	 */
	public StateModel() {
		reset();
    }
	
	
	
	/**
	 * Adopt Face
	 * 
	 * Adopt faces in a particular sequence dictated by the user directed instruction on
	 * how to rotate the code during the exploration phase.  Also tile name is 
	 * specified at this time, and "transformedTileArray" is created which is a 
	 * rotated version of the observed tile array so that the face orientations
	 * match the convention of a cut-out rubik cube layout.
	 * 
	 * @param rubikFace
	 */
    public void adopt(RubikFace rubikFace) {
	    
    	
    	switch(adoptFaceCount) {
    	
    	case 0:
    		rubikFace.faceNameEnum = FaceNameEnum.UP;
    		rubikFace.transformedTileArray =  Util.getTileArrayRotatedClockwise(rubikFace.observedTileArray);
    		break;
    	case 1:
    		rubikFace.faceNameEnum = FaceNameEnum.FRONT;
    		rubikFace.transformedTileArray = Util.getTileArrayRotatedClockwise(rubikFace.observedTileArray);
    		break;
    	case 2:
    		rubikFace.faceNameEnum = FaceNameEnum.LEFT;
    		rubikFace.transformedTileArray = Util.getTileArrayRotatedClockwise(rubikFace.observedTileArray);
    		break;
    	case 3:
    		rubikFace.faceNameEnum = FaceNameEnum.DOWN;
    		rubikFace.transformedTileArray = rubikFace.observedTileArray.clone();
    		break;
    	case 4:
    		rubikFace.faceNameEnum = FaceNameEnum.BACK;
    		rubikFace.transformedTileArray = Util.getTileArrayRotated180(rubikFace.observedTileArray);
    		break;
    	case 5:
    		rubikFace.faceNameEnum = FaceNameEnum.RIGHT;
    		rubikFace.transformedTileArray = Util.getTileArrayRotated180(rubikFace.observedTileArray);
    		break;
    		
    		default:
    			// =+= log error ?
    	}
    	
    	if(adoptFaceCount < 6) {
    		colorRubikFaceMap.put(rubikFace.observedTileArray[1][1].constantTileColor, rubikFace); // =+= can be inaccurate!
    		nameRubikFaceMap.put(rubikFace.faceNameEnum, rubikFace);
    	}
    	
    	adoptFaceCount++;
    }
    
    
    /**
     * Get Rubik Face by Name
     * 
     * @param faceNameEnum
     * @return
     */
    public RubikFace getFaceByName(FaceNameEnum faceNameEnum) {
    	return nameRubikFaceMap.get(faceNameEnum);
    }
    
    
    /**
     * Return the number of valid and adopted faces.  Maximum is of course six.
     * 
     * @return
     */
    public int getNumObservedFaces() {    	
    	return nameRubikFaceMap.size();
    }


	/**
	 * Return true if all six faces have been observed and adopted.
	 * 
	 * @return
	 */
    public boolean isThereAfullSetOfFaces() {
    	if(getNumObservedFaces() >= 6)
    		return true;
    	else
    		return false;
    }


	/**
	 * Returns true if there are exactly nine of each tile color over entire cube.
	 * 
	 * @return
	 */
    public boolean isTileColorsValid() {
    	
    	// Count how many tile colors entire cube has as a first check.
    	int [] numColorTilesArray = new int[] {0, 0, 0, 0, 0, 0};
		for(RubikFace rubikFace : nameRubikFaceMap.values() ) {
			for(int n=0; n<3; n++) {
				for(int m=0; m<3; m++) {
					numColorTilesArray[ rubikFace.observedTileArray[n][m].constantTileColor.ordinal() ]++;
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
	 * The "String Representation" is a per the two-phase rubik cube
	 * logic solving algorithm requires.
	 * 
	 * This should only be called if cube if colors are valid.
	 * 
	 * @return
	 */
	public String getStringRepresentationOfCube() {
		StringBuffer sb = new StringBuffer();
		sb.append(getStringRepresentationOfFace( getFaceByName(FaceNameEnum.UP)));
		sb.append(getStringRepresentationOfFace( getFaceByName(FaceNameEnum.RIGHT)));
		sb.append(getStringRepresentationOfFace( getFaceByName(FaceNameEnum.FRONT)));
		sb.append(getStringRepresentationOfFace( getFaceByName(FaceNameEnum.DOWN)));
		sb.append(getStringRepresentationOfFace( getFaceByName(FaceNameEnum.LEFT)));
		sb.append(getStringRepresentationOfFace( getFaceByName(FaceNameEnum.BACK)));
		return sb.toString();
	}



	/**
	 * Get String Representing a particular Face.
	 * 
	 * @param rubikFace
	 * @return
	 */
	private StringBuffer getStringRepresentationOfFace(RubikFace rubikFace) {
		StringBuffer sb = new StringBuffer();
		ConstantTile[][] virtualLogicalTileArray = rubikFace.transformedTileArray;
		for(int m=0; m<3; m++)
			for(int n=0; n<3; n++)
				sb.append(getCharacterRepresentingColor(virtualLogicalTileArray[n][m].constantTileColor));
		return sb;
	}
	
	
	/**
	 * Get Character Representing Color
	 * 
	 * Return single character representing Face Name (i.e., Up, Down, etc...) of face 
	 * who's center tile is of the passed in arg.
	 * 
	 * 
	 * @param colorEnum
	 * @return
	 */
	private char getCharacterRepresentingColor(ConstantTileColorEnum colorEnum) {

		switch(colorRubikFaceMap.get(colorEnum).faceNameEnum) {
		case FRONT: return 'F';
		case BACK:  return 'B';
		case DOWN:  return 'D';
		case LEFT:  return 'L';
		case RIGHT: return 'R';
		case UP:    return 'U';
		default:    return 0;   // Odd error message without this, but cannot get here by definition.  Hmm.
		}
	}


	/**
	 * Save cube state to file.
	 */
	public void saveState() {
		
		RubikFace [] rubikFaceArray = new RubikFace[] { 
				getFaceByName(FaceNameEnum.UP),
				getFaceByName(FaceNameEnum.RIGHT),
				getFaceByName(FaceNameEnum.FRONT),
				getFaceByName(FaceNameEnum.DOWN),
				getFaceByName(FaceNameEnum.LEFT),
				getFaceByName(FaceNameEnum.BACK)};
		
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
	 * Recall cube state (i.e., the six faces) from file.
	 */
	public void recallState() {
		
		RubikFace [] rubikFaceArray = new RubikFace[6];
		
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
		
		nameRubikFaceMap.put(FaceNameEnum.UP, rubikFaceArray[0]);
		nameRubikFaceMap.put(FaceNameEnum.RIGHT, rubikFaceArray[1]);
		nameRubikFaceMap.put(FaceNameEnum.FRONT, rubikFaceArray[2]);
		nameRubikFaceMap.put(FaceNameEnum.DOWN, rubikFaceArray[3]);
		nameRubikFaceMap.put(FaceNameEnum.LEFT, rubikFaceArray[4]);
		nameRubikFaceMap.put(FaceNameEnum.BACK, rubikFaceArray[5]);
		
		
		// Rebuild Color and Name Rubik Maps.
		for(RubikFace rubikFace : rubikFaceArray)
    		colorRubikFaceMap.put(rubikFace.observedTileArray[1][1].constantTileColor, rubikFace); // =+= can be inaccurate!
	}


	/**
	 * Reset
	 * 
	 * Reset state to the initial values.
	 */
	public void reset() {

		// Rubik Face of latest processed frame: may or may not be any of the six state objects.
		activeRubikFace = null;

		// Array of above rubik face objects index by TileColorEnum.
		colorRubikFaceMap = new HashMap<Constants.ConstantTileColorEnum, RubikFace>(6);

		// Array of above rubik face objects index by FaceNameEnum
		nameRubikFaceMap = new HashMap<Constants.FaceNameEnum, RubikFace>(6);

		// Application State = null; see AppStateEnum.
		appState = AppStateEnum.START;
		
		// Stable Face Recognizer State
	    faceRecogniztionState = FaceRecogniztionStateEnum.UNKNOWN;

		// Result when Two Phase algorithm is ask to evaluate if cube in valid.  If valid, code is zero.
		verificationResults = 0;

		// String notation on how to solve cube.
		solutionResults = null;

		// Above, but broken into individual moves.
		solutionResultsArray = null;

		// Index to above array as to which move we are on.
		solutionResultIndex = 0;

		// We assume that faces will be explored in a particular sequence.
		adoptFaceCount = 0;
		
		// True if we are to render GL Pilot Cube
		renderPilotCube = true;

		// Cube Location and Orientation deduced from Face.
		cubeReconstructor = null;
	}
}
