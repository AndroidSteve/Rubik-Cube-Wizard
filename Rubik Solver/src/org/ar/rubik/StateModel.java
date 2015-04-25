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
import org.ar.rubik.Constants.ColorTileEnum;
import org.ar.rubik.Constants.FaceNameEnum;
import org.ar.rubik.Constants.AppStateEnum;
import org.ar.rubik.Constants.GestureRecogniztionStateEnum;
import org.opencv.core.Scalar;

import android.opengl.Matrix;
import android.os.Environment;
import android.util.Log;

/**
 * Rubik Cube and application State
 * 
 * @author android.steve@cl-sw.com
 */
public class StateModel {

	// Rubik Face of latest processed frame: may or may not be any of the six state objects.
	public RubikFace activeRubikFace;
	
	/*
	 * This is "Rubik Cube State" or "Rubik Cube Model" in model-view-controller vernacular.
	 * Map of above rubik face objects index by FaceNameEnum
	 */
	public HashMap<FaceNameEnum, RubikFace> nameRubikFaceMap = new HashMap<Constants.FaceNameEnum, RubikFace>(6);
	
	/*
	 * This is a hash map of OpenCV colors that are initialized to those specified by field
	 * rubikColor of ColorTileEnum.   Function reevauateSelectTileColors() adjusts these 
	 * colors according to a Mean-Shift algorithm to correct for lumonosity.
	 */
	public HashMap<ColorTileEnum, Scalar> mutableTileColors = new HashMap<ColorTileEnum, Scalar>(6);
	
	// Application State; see AppStateEnum.
	public AppStateEnum appState = AppStateEnum.START;
	
	// Stable Face Recognizer State
    public GestureRecogniztionStateEnum gestureRecogniztionState = GestureRecogniztionStateEnum.UNKNOWN;

    // Result when Two Phase algorithm is ask to evaluate if cube in valid.  If valid, code is zero.
	public int verificationResults;
	
	// String notation on how to solve cube.
	public String solutionResults;
	
	// Above, but broken into individual moves.
	public String [] solutionResultsArray;
	
	// Index to above array as to which move we are on.
	public int solutionResultIndex;
	
	// We assume that faces will be explored in a particular sequence.
	public int adoptFaceCount = 0;
	
	// Additional Cube Rotation: initially set to Identity Rotation Matrix
	public float[] additionalGLCubeRotation = new float[16];

	// True if it is OK to render GL Pilot Cube
	public boolean renderPilotCube = true;

	// Cube Location and Orientation deduced from Face.
	public CubeReconstructor cubeReconstructor;

	// Intrinsic Camera Calibration Parameters from hardware.
	public CameraCalibration cameraParameters;
	
	
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
	 * =+= This logic duplicated in AppStateMachine
	 * 
	 * @param rubikFace
	 */
    public void adopt(RubikFace rubikFace) {
	    
    	
    	switch(adoptFaceCount) {
    	
    	case 0:
    		rubikFace.faceNameEnum = FaceNameEnum.UP;
    		rubikFace.transformedTileArray =  rubikFace.observedTileArray.clone();
    		break;
    	case 1:
    		rubikFace.faceNameEnum = FaceNameEnum.RIGHT;
    		rubikFace.transformedTileArray = Util.getTileArrayRotatedClockwise(rubikFace.observedTileArray);
    		break;
    	case 2:
    		rubikFace.faceNameEnum = FaceNameEnum.FRONT;
    		rubikFace.transformedTileArray = Util.getTileArrayRotatedClockwise(rubikFace.observedTileArray);
    		break;
    	case 3:
    		rubikFace.faceNameEnum = FaceNameEnum.DOWN;
    		rubikFace.transformedTileArray = Util.getTileArrayRotatedClockwise(rubikFace.observedTileArray);
    		break;
    	case 4:
    		rubikFace.faceNameEnum = FaceNameEnum.LEFT;
    		rubikFace.transformedTileArray = Util.getTileArrayRotated180(rubikFace.observedTileArray);
    		break;
    	case 5:
    		rubikFace.faceNameEnum = FaceNameEnum.BACK;
    		rubikFace.transformedTileArray = Util.getTileArrayRotated180(rubikFace.observedTileArray);
    		break;
    		
    		default:
    			// =+= log error ?
    	}
    	
    	if(adoptFaceCount < 6) {
    	    
    	    // Record Face by Name: i.e., UP, DOWN, LEFT, ...
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
	    
	    // Create a map of tile color to face name. The center tile of each face is used for this 
	    // definition.  This information is used by Rubik Cube Logic Solution.
	    HashMap<ColorTileEnum, FaceNameEnum> colorTileToNameMap = new HashMap<ColorTileEnum, FaceNameEnum>(6);
        colorTileToNameMap.put(getFaceByName(FaceNameEnum.UP).transformedTileArray[1][1],    FaceNameEnum.UP);
        colorTileToNameMap.put(getFaceByName(FaceNameEnum.DOWN).transformedTileArray[1][1],  FaceNameEnum.DOWN);
        colorTileToNameMap.put(getFaceByName(FaceNameEnum.LEFT).transformedTileArray[1][1],  FaceNameEnum.LEFT);
        colorTileToNameMap.put(getFaceByName(FaceNameEnum.RIGHT).transformedTileArray[1][1], FaceNameEnum.RIGHT);
        colorTileToNameMap.put(getFaceByName(FaceNameEnum.FRONT).transformedTileArray[1][1], FaceNameEnum.FRONT);
        colorTileToNameMap.put(getFaceByName(FaceNameEnum.BACK).transformedTileArray[1][1],  FaceNameEnum.BACK);
	    
	    
		StringBuffer sb = new StringBuffer();
		sb.append(getStringRepresentationOfFace( colorTileToNameMap, getFaceByName(FaceNameEnum.UP)));
		sb.append(getStringRepresentationOfFace( colorTileToNameMap, getFaceByName(FaceNameEnum.RIGHT)));
		sb.append(getStringRepresentationOfFace( colorTileToNameMap, getFaceByName(FaceNameEnum.FRONT)));
		sb.append(getStringRepresentationOfFace( colorTileToNameMap, getFaceByName(FaceNameEnum.DOWN)));
		sb.append(getStringRepresentationOfFace( colorTileToNameMap, getFaceByName(FaceNameEnum.LEFT)));
		sb.append(getStringRepresentationOfFace( colorTileToNameMap, getFaceByName(FaceNameEnum.BACK)));
		return sb.toString();
	}



	/**
	 * Get String Representing a particular Face.
	 * @param colorTileToNameMap 
	 * 
	 * @param rubikFace
	 * @return
	 */
	private StringBuffer getStringRepresentationOfFace(HashMap<ColorTileEnum, FaceNameEnum> colorTileToNameMap, RubikFace rubikFace) {
	    
		StringBuffer sb = new StringBuffer();
		ColorTileEnum[][] virtualLogicalTileArray = rubikFace.transformedTileArray;
		for(int m=0; m<3; m++)
			for(int n=0; n<3; n++)
				sb.append(getCharacterRepresentingColor(colorTileToNameMap, virtualLogicalTileArray[n][m]));
		return sb;
	}
	
	
	/**
	 * Get Character Representing Color
	 * 
	 * Return single character representing Face Name (i.e., Up, Down, etc...) of face 
	 * who's center tile is of the passed in arg.
	 * @param colorTileToNameMap 
	 * 
	 * 
	 * @param colorEnum
	 * @return
	 */
	private char getCharacterRepresentingColor(HashMap<ColorTileEnum, FaceNameEnum> colorTileToNameMap, ColorTileEnum colorEnum) {
		
//		Log.e(Constants.TAG_COLOR, "colorEnum=" + colorEnum + " colorTileToNameMap=" + colorTileToNameMap);

		switch(colorTileToNameMap.get(colorEnum) ) {
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
	}


	/**
	 * Reset
	 * 
	 * Reset state to the initial values.
	 */
	public void reset() {

		// Rubik Face of latest processed frame: may or may not be any of the six state objects.
		activeRubikFace = null;
		
		// Array of above rubik face objects index by FaceNameEnum
		nameRubikFaceMap = new HashMap<Constants.FaceNameEnum, RubikFace>(6);
		
		// Array of tile colors index by ColorTileEnum.
		mutableTileColors.clear();
		for(ColorTileEnum colorTile : ColorTileEnum.values())
		    if(colorTile.isRubikColor == true)
		        mutableTileColors.put(colorTile, colorTile.rubikColor);

		// Application State = null; see AppStateEnum.
		appState = AppStateEnum.START;
		
		// Stable Face Recognizer State
	    gestureRecogniztionState = GestureRecogniztionStateEnum.UNKNOWN;

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

        // Set additional GL cube rotation to Identity Rotation Matrix
        Matrix.setIdentityM(additionalGLCubeRotation, 0);
	}
}
