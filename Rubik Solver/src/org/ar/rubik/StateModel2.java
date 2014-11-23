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
 *   Represents state of Rubik Cube.
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

import java.util.HashMap;

import org.ar.rubik.Constants.ConstantTile;
import org.ar.rubik.Constants.ConstantTileColorEnum;
import org.ar.rubik.Constants.FaceNameEnum;

/**
 * @author android.steve@testlens.com
 * 
 *
 */
public class StateModel2 {
	
	// Rubik Face of latest processed frame: may or may not be any of the six state objects.
	public RubikFace2 activeRubikFace;
	
	/*
	 * This is "Rubik Cube State" or "Rubik Cube Model" in model-veiw-controller vernacular.
	 */
	public RubikFace2 upRubikFace;
	public RubikFace2 downRubikFace;
	public RubikFace2 leftRubikFace;
	public RubikFace2 rightRubikFace;
	public RubikFace2 frontRubikFace;
	public RubikFace2 backRubikFace;
	
	// Set of above rubik faces for easy access
//	public HashSet<RubikFace2> rubikFaceSet = new HashSet<RubikFace2>();
	
	// Array of above rubik face objects index by TileColorEnum.
	public HashMap<ConstantTileColorEnum, RubikFace2> colorRubikFaceMap = new HashMap<Constants.ConstantTileColorEnum, RubikFace2>(6);
	
	// Array of above rubik face objects index by FaceNameEnum
	public HashMap<FaceNameEnum, RubikFace2> nameRubikFaceMap = new HashMap<Constants.FaceNameEnum, RubikFace2>(6);
	
	
    // Result when Two Phase algorithm is ask to evaluate if cube in valid.  If valid, code is zero.
	public int verificationResults;
	
	// String notation on how to solve cube.
	public String solutionResults;
	
	// Above, but broken into individual moves.
	public String [] solutionResultsArray;
	
	// We assume that faces will be explored in a particular sequence.
	private int adoptFaceCount = 0;
	
	
	
	
	/**
	 * Adopt Face
	 * 
	 * Adopt faces in a particular sequence dictated by the user directed instruction on
	 * how to rotate the code during the exploration phase.  Also tile name is 
	 * specified at this time, and "transformedTileArray" is created which is a 
	 * rotated version of the observed tile array so that the face orientations
	 * match the convention of a cut-out rubik cube layout.
	 * 
	 * @param rubikFace2
	 */
    public void adopt(RubikFace2 rubikFace2) {
	    
    	switch(adoptFaceCount) {
    	
    	case 0:
    		rubikFace2.faceNameEnum = FaceNameEnum.UP;
    		upRubikFace = rubikFace2;
    		rubikFace2.transformedTileArray =  Util.getVirtualTileArrayRotatedClockwise(rubikFace2.observedTileArray);
    		rubikFace2.transformedTileArray = rubikFace2.observedTileArray.clone();
    		break;
    	case 1:
    		rubikFace2.faceNameEnum = FaceNameEnum.RIGHT;
    		rightRubikFace = rubikFace2;
    		rubikFace2.transformedTileArray = Util.getVirtualTileArrayRotatedClockwise(rubikFace2.observedTileArray);
    		break;
    	case 2:
    		rubikFace2.faceNameEnum = FaceNameEnum.FRONT;
    		frontRubikFace = rubikFace2;
    		rubikFace2.transformedTileArray = Util.getVirtualTileArrayRotatedClockwise(rubikFace2.observedTileArray);
    		break;
    	case 3:
    		rubikFace2.faceNameEnum = FaceNameEnum.DOWN;
    		downRubikFace = rubikFace2;
    		rubikFace2.transformedTileArray = Util.getVirtualTileArrayRotatedClockwise(rubikFace2.observedTileArray);
    		break;
    	case 4:
    		rubikFace2.faceNameEnum = FaceNameEnum.LEFT;
    		leftRubikFace = rubikFace2;
    		rubikFace2.transformedTileArray = Util.getVirtualTileArrayRotated180(rubikFace2.observedTileArray);
    		break;
    	case 5:
    		rubikFace2.faceNameEnum = FaceNameEnum.BACK;
    		backRubikFace = rubikFace2;
    		rubikFace2.transformedTileArray = Util.getVirtualTileArrayRotated180(rubikFace2.observedTileArray);
    		break;
    		
    		default:
    			// =+= log error ?
    	}
    	
    	if(adoptFaceCount < 6) {
    		colorRubikFaceMap.put(rubikFace2.observedTileArray[1][1].constantTileColor, rubikFace2); // =+= can be inaccurate!
    		nameRubikFaceMap.put(rubikFace2.faceNameEnum, rubikFace2);
    	}
    	
    	adoptFaceCount++;
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
	 * Returns true if there are exactly nine of each tile cover over entire cube.
	 * 
	 * @return
	 */
    public boolean isTileColorsValid() {
    	
    	// Count how many tile colors entire cube has as a first check.
    	int [] numColorTilesArray = new int[] {0, 0, 0, 0, 0, 0};
		for(RubikFace2 rubikFace2 : nameRubikFaceMap.values() ) {
			for(int n=0; n<3; n++) {
				for(int m=0; m<3; m++) {
					numColorTilesArray[ rubikFace2.observedTileArray[n][m].constantTileColor.ordinal() ]++;
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
		sb.append(getStringRepresentationOfFace( upRubikFace ) );
		sb.append(getStringRepresentationOfFace( rightRubikFace ) );
		sb.append(getStringRepresentationOfFace( frontRubikFace ) );
		sb.append(getStringRepresentationOfFace( downRubikFace ) );
		sb.append(getStringRepresentationOfFace( leftRubikFace ) );
		sb.append(getStringRepresentationOfFace( backRubikFace ) );
		return sb.toString();
	}



	/**
	 * Get String Representing a particular Face.
	 * 
	 * @param rubikFace
	 * @return
	 */
	private StringBuffer getStringRepresentationOfFace(RubikFace2 rubikFace) {
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




}
