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
 *   This class interprets the recognized Rubik Faces and determines how primary
 *   application state should change.
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


import org.ar.rubik.Constants.AppStateEnum;
import org.ar.rubik.RubikFace.FaceRecognitionStatusEnum;
//import org.ar.rubik.gl.PilotGLRenderer.FaceType;
//import org.ar.rubik.gl.PilotGLRenderer.Rotation;
import org.kociemba.twophase.Search;
import org.kociemba.twophase.Tools;

import android.util.Log;



/**
 * @author android.steve@testlens.com
 *
 */
public class StateMachine {
	
//	public enum ControllerStateEnum { 
//		START,     // Ready
//		GOT_IT,    // A Cube Face has been recognized and captured.
//		ROTATE,    // Request user to rotate Cube.
//		SEARCHING, // Attempting to lock onto new Cube Face.
//		COMPLETE,  // All six faces have been captured, and we seem to have valid color.
//		BAD_COLORS,// All six faces have been captured, but we do not have properly nine tiles of each color.
//		VERIFIED,  // Two Phase solution has verified that the cube tile/colors/positions are a valid cube.
//		WAITING,   // Waiting for TwoPhase Prune Tree generation to complete.
//		INCORRECT, // Two Phase solution has analyzed the cube and found it to be invalid.
//		SOLVED,    // Two Phase solution has analyzed the cube and found a solution.
//		DO_MOVE,   // Inform user to perform a face rotation
//		WAITING_FOR_MOVE_COMPLETE, // Wait for face rotation to complete
//		DONE       // Cube should be completely physically solved.
//		};
	
//	private ControllerStateEnum controllerState = ControllerStateEnum.START;
	
	private StateModel stateModel;
	

	
	// 12 tables need to be generated.  When count is 12, tables are valid.
	// =+= used by prune table loader.  
	public int pruneTableLoaderCount = 0;
	
	// Allows for more pleasing user interface
	private int gotItCount = 0;

	

	/**
	 * @param stateModel
	 */
    public StateMachine(StateModel stateModel) {
    	this.stateModel = stateModel;
    }

    
    /**
     * On Rubik Face Recognized
     * 
     * This function is called any time a Rubik Face is recognized, even if it may be 
     * inaccurate.  Further filtering is perform in this function.
     * @param rubikFace 
     * 
     */
    private enum FaceRecogniztionState { 
    	UNKNOWN, // No face recognition
    	PENDING, // A particular face seems to becoming stable.
    	STABLE,  // A particular face is stable.
    	PARTIAL  // A particular face seems to becoming unstable.
    	};
    private FaceRecogniztionState faceRecogniztionState = FaceRecogniztionState.UNKNOWN;
    private RubikFace candidateRubikFace = null;
    private int consecutiveCandiateRubikFaceCount = 0;
	private final int consecutiveCandidateCountThreashold = 1;
	
	/**
	 * Process Rubik Face
	 * 
	 * Assuming that a Rubik Face is recognized, then update the Application State Machine.
	 * 
	 * @param rubikFace2
	 */
    public void processFace(RubikFace rubikFace) {
        	
        	// Sometimes, we want state to change simply on frame events.
           	onFrameStateChanges();
        	
           	Log.d(Constants.TAG_CNTRL, "processRubikFaceSolution() state=" + faceRecogniztionState + " candidate=" + (candidateRubikFace == null ? 0 : candidateRubikFace.hashCode) + " newFace=" + (rubikFace == null ? 0 :rubikFace.hashCode) );   	 

        	switch(faceRecogniztionState) {

        	case UNKNOWN:
        		if(rubikFace.faceRecognitionStatus == FaceRecognitionStatusEnum.SOLVED) {
        			faceRecogniztionState = FaceRecogniztionState.PENDING;
        			candidateRubikFace = rubikFace;
        			consecutiveCandiateRubikFaceCount = 0;
        		}
        		else
        			; // stay in unknown state.
        		break;


        	case PENDING:
        		if(rubikFace.faceRecognitionStatus == FaceRecognitionStatusEnum.SOLVED) {

        			if(rubikFace.hashCode == candidateRubikFace.hashCode) {

        				if(consecutiveCandiateRubikFaceCount > consecutiveCandidateCountThreashold) {
        					faceRecogniztionState = FaceRecogniztionState.STABLE;
        					onStableRubikFaceRecognition(candidateRubikFace);
        				}
        				else 
        					consecutiveCandiateRubikFaceCount++;
        			}
//        			else if(false)
//        				;// =+= add partial match here
        			else
        				faceRecogniztionState = FaceRecogniztionState.UNKNOWN;
        		}
        		else
        			faceRecogniztionState = FaceRecogniztionState.UNKNOWN;	
        		break;


        	case STABLE:
        		if(rubikFace.faceRecognitionStatus == FaceRecognitionStatusEnum.SOLVED) {

        			if(rubikFace.hashCode == candidateRubikFace.hashCode) 
        				; // Just stay in this state
//        			else if(false)
//        				; // =+= add partial match here
        			else {
        				faceRecogniztionState = FaceRecogniztionState.PARTIAL;
        				consecutiveCandiateRubikFaceCount = 0;
        			}
        		}
        		else {
        			faceRecogniztionState = FaceRecogniztionState.PARTIAL;
        			consecutiveCandiateRubikFaceCount = 0;
        		}
        		break;


        	case PARTIAL:
        		if(rubikFace.faceRecognitionStatus == FaceRecognitionStatusEnum.SOLVED) {
        			
        			if(rubikFace.hashCode == candidateRubikFace.hashCode)
        				faceRecogniztionState = FaceRecogniztionState.STABLE;
//        			else if(false)
//        				; // =+= add partial match here
        			else {
            			if(consecutiveCandiateRubikFaceCount > consecutiveCandidateCountThreashold) {
            				faceRecogniztionState = FaceRecogniztionState.UNKNOWN;
            				offStableRubikFaceRecognition();
            			}
            			else 
            				consecutiveCandiateRubikFaceCount++; // stay in partial state
        			}
        		}
        		else {
        			if(consecutiveCandiateRubikFaceCount > consecutiveCandidateCountThreashold) {
        				faceRecogniztionState = FaceRecogniztionState.UNKNOWN;
        				offStableRubikFaceRecognition();
        			}
        			else 
        				consecutiveCandiateRubikFaceCount++; // stay in partial state
        		}
        		break;

        	}
        }

        

    	/**
         * On Stable Rubik Face Recognized
         * 
         * This function is called ever frame when a valid and stable Rubik Face is recognized.
         * 
         * @param hashCode 
         * 
         */
        private RubikFace lastStableRubikFace = null;
    	private boolean allowOneMoreRotation = false;
        private void onStableRubikFaceRecognition(RubikFace candidateRubikFace2) {

       		Log.i(Constants.TAG_CNTRL, "+onStableRubikFaceRecognized: last=" + (lastStableRubikFace == null ? 0 : lastStableRubikFace.hashCode) + " new=" + candidateRubikFace2.hashCode);
        	if(lastStableRubikFace == null || candidateRubikFace2.hashCode != lastStableRubikFace.hashCode) {
        		lastStableRubikFace = candidateRubikFace2;
        		onNewStableRubikFaceRecognized(candidateRubikFace2);
        	}
        	

        	switch (stateModel.appState) {
        	
        	case WAITING_FOR_MOVE_COMPLETE:
        		stateModel.appState = AppStateEnum.DO_MOVE;
        		stateModel.solutionResultIndex++;
        		if(stateModel.solutionResultIndex == stateModel.solutionResultsArray.length)
        			stateModel.appState = AppStateEnum.DONE;
    		break;

        	default:
        		break;
        	}
        }
        public void offStableRubikFaceRecognition() {
        	
        	Log.i(Constants.TAG_CNTRL, "-offStableRubikFaceRecognized: previous=" + lastStableRubikFace.hashCode);
        	offNewStableRubikFaceRecognition();
        	
        	switch (stateModel.appState) {

        	case DO_MOVE:		
        		stateModel.appState = AppStateEnum.WAITING_FOR_MOVE_COMPLETE;
        		break;

        	default:
        		break;
        	}
        }
        

        
        /**
         * On New Stable Rubik Face Recognized
         * 
         * This function is called when a new and different stable Rubik Face is recognized.
         * In other words, this should be a different face than the last stable face, 
         * however, it will be called on frame rate while new stable Rubik Face is 
         * recognized in image.
         * 
         * @param rubikFaceHashCode
         */
        private void onNewStableRubikFaceRecognized(RubikFace candidateRubikFace2) {
        	
        	Log.i(Constants.TAG_CNTRL, "+onNewStableRubikFaceRecognized  Previous State =" + stateModel.appState);


        	switch(stateModel.appState) {

        	case START:
        		stateModel.adopt(candidateRubikFace2);
        		stateModel.appState = AppStateEnum.GOT_IT;
        		break;
        		
        	case SEARCHING:
        		stateModel.adopt(candidateRubikFace2);

        		// Have not yet seen all six sides.
        		if(stateModel.isThereAfullSetOfFaces() == false) {
        			stateModel.appState = AppStateEnum.GOT_IT;
        			allowOneMoreRotation = true;
        		}
        		
        		// Do one more turn so cube returns to original orientation.
        		else if(allowOneMoreRotation == true) {
        			stateModel.appState = AppStateEnum.GOT_IT;
        			allowOneMoreRotation = false;
        		}
        		
        		// Begin processing of cube: first check that there are exactly 9 tiles of each color.
        		else {
        			Util.reevauateSelectTileColors(stateModel);
        			if(stateModel.isTileColorsValid() == true)
        				stateModel.appState = AppStateEnum.COMPLETE;
        			else
        				stateModel.appState = AppStateEnum.BAD_COLORS;
        		}
        		break;
        		
    		default:
    			break;
        	}
        }
       private void offNewStableRubikFaceRecognition() {
        	
        	Log.i(Constants.TAG_CNTRL, "-offNewStableRubikFaceRecognition  Previous State =" + stateModel.appState);
        	
           	switch(stateModel.appState) {

        	case ROTATE:
        		stateModel.appState = AppStateEnum.SEARCHING;
        		break;
        		
    		default:
    			break;
        	}
        }   


       /**
        * On Frame State Changes
        * 
        * It appears handy to have some controller state changes advanced on the periodic frame rate.
        * Unfortunately, the rate that is function is called is dependent upon the bulk of opencv
        * processing which can vary with the background.
        */
       private void onFrameStateChanges() {

    	   switch(stateModel.appState) {

    	   case WAITING:
    		   if(pruneTableLoaderCount == 12) {
    			   stateModel.appState = AppStateEnum.VERIFIED;
    		   }
    		   break;


    	   case GOT_IT:
    		   if(gotItCount < 3)
    			   gotItCount++;
    		   else {
    			   stateModel.appState = AppStateEnum.ROTATE;
    			   gotItCount = 0;
    		   }
    		   break;

    		   
    	   case COMPLETE:
    		   String cubeString = stateModel.getStringRepresentationOfCube();

    		   // Returns 0 if cube is solvable.
    		   stateModel.verificationResults = Tools.verify(cubeString);

    		   if(stateModel.verificationResults == 0) {
    			   stateModel.appState = AppStateEnum.WAITING;
    		   }
    		   else
    			   stateModel.appState = AppStateEnum.INCORRECT;

    		   String stringErrorMessage = Util.getTwoPhaseErrorString((char)(stateModel.verificationResults * -1 + '0'));

    		   Log.i(Constants.TAG_CNTRL, "Cube String Rep: " + cubeString);
    		   Log.i(Constants.TAG_CNTRL, "Verification Results: (" + stateModel.verificationResults + ") " + stringErrorMessage);
    		   break;

    		   
    	   case VERIFIED:
    		   String cubeString2 = stateModel.getStringRepresentationOfCube();

    		   // Returns 0 if solution computed
    		   stateModel.solutionResults = Search.solution(cubeString2, 25, 2, false);
    		   Log.i(Constants.TAG_CNTRL, "Solution Results: " + stateModel.solutionResults);
    		   if (stateModel.solutionResults.contains("Error")) {
    			   char solutionCode = stateModel.solutionResults.charAt(stateModel.solutionResults.length() - 1);
    			   stateModel.verificationResults = solutionCode - '0';
    			   Log.i(Constants.TAG_CNTRL, "Solution Error: " + Util.getTwoPhaseErrorString(solutionCode) );
    			   stateModel.appState = AppStateEnum.INCORRECT;
    		   }
    		   else {
    			   stateModel.appState = AppStateEnum.SOLVED;
    		   }
    		   break;

    		   
    	   case SOLVED:
    		   stateModel.solutionResultsArray = stateModel.solutionResults.split(" ");
    		   Log.i(Constants.TAG_CNTRL, "Solution Results Array: " + stateModel.solutionResultsArray);
    		   stateModel.solutionResultIndex = 0;
    		   stateModel.appState = AppStateEnum.DO_MOVE;
    		   break;
    		   
    		   
    	   default:
    		   break;
    	   }
       }

       
//   	/**
//   	 * Render User Instructions
//   	 * 
//   	 * @param image
//   	 * @param rubikFace 
//   	 */
//   	public void renderUserInstructions(Mat image, RubikFace rubikFace) {
//
//   		// Create black area for text
//   		if(RubikMenuAndParameters.userTextDisplay == true)
//   			Core.rectangle(image, new Point(0, 0), new Point(1270, 60), Constants.ColorBlack, -1);
//
////   		pilotGLRenderer.setCubeOrienation(rubikFace);
//
//   		switch(controllerState) {
//
//   		case START:
//   			if(RubikMenuAndParameters.userTextDisplay == true)
//   				Core.putText(image, "Show Me The Rubik Cube", new Point(0, 60), Constants.FontFace, 5, Constants.ColorWhite, 5);
////   			pilotGLRenderer.setRenderArrow(false);
//   			break;
//
//   		case GOT_IT:
//   			if(RubikMenuAndParameters.userTextDisplay == true)
//   				Core.putText(image, "OK, Got It", new Point(0, 60), Constants.FontFace, 5, Constants.ColorWhite, 5);
////   			pilotGLRenderer.setRenderArrow(false);
////   			pilotGLRenderer.setRenderCube(RubikMenuAndParameters.cubeOverlayDisplay);
//   			break;
//
//   		case ROTATE:
//   			if(RubikMenuAndParameters.userTextDisplay == true)
//   				Core.putText(image, "Please Rotate: " + stateModel.getNumObservedFaces(), new Point(0, 60), Constants.FontFace, 5, Constants.ColorWhite, 5);
////   			if(  stateModel.getNumValidFaces() % 2 == 0)
////   				pilotGLRenderer.showFullCubeRotateArrow(FaceType.LEFT_TOP);
////   			else
////   				pilotGLRenderer.showFullCubeRotateArrow(FaceType.FRONT_TOP);
//   			break;
//
//   		case SEARCHING:
//   			if(RubikMenuAndParameters.userTextDisplay == true)
//   				Core.putText(image, "Searching for Another Face", new Point(0, 60), Constants.FontFace, 5, Constants.ColorWhite, 5);
////   			pilotGLRenderer.setRenderArrow(false);
//   			break;
//
//   		case COMPLETE:
//   			if(RubikMenuAndParameters.userTextDisplay == true)
//   				Core.putText(image, "Cube is Complete and has Good Colors", new Point(0, 60), Constants.FontFace, 4, Constants.ColorWhite, 4);
//   			break;
//
//   		case WAITING:
//   			if(RubikMenuAndParameters.userTextDisplay == true)
//   				Core.putText(image, "Waiting - Preload Next: " + pruneTableLoaderCount, new Point(0, 60), Constants.FontFace, 5, Constants.ColorWhite, 5);
//   			break;
//
//   		case BAD_COLORS:
////   			if(RubikMenuAndParameters.userTextDisplay == true)
//   				Core.putText(image, "Cube is Complete but has Bad Colors", new Point(0, 60), Constants.FontFace, 4, Constants.ColorWhite, 4);
//   			break;
//
//   		case VERIFIED:
//   			if(RubikMenuAndParameters.userTextDisplay == true)
//   				Core.putText(image, "Cube is Complete and Verified", new Point(0, 60), Constants.FontFace, 4, Constants.ColorWhite, 4);
//   			break;
//
//   		case INCORRECT:
////   			if(RubikMenuAndParameters.userTextDisplay == true)
//   				Core.putText(image, "Cube is Complete but Incorrect: " + stateModel.verificationResults, new Point(0, 60), Constants.FontFace, 4, Constants.ColorWhite, 4);
//   			break;
//
//   		case SOLVED:
//   			if(RubikMenuAndParameters.userTextDisplay == true) {
//   				Core.putText(image, "SOLUTION: ", new Point(0, 60), Constants.FontFace, 4, Constants.ColorWhite, 4);
//   				Core.rectangle(image, new Point(0, 60), new Point(1270, 120), Constants.ColorBlack, -1);
//   				Core.putText(image, "" + stateModel.solutionResults, new Point(0, 120), Constants.FontFace, 2, Constants.ColorWhite, 2);
//   			}
//   			break;
//
//   		case DO_MOVE:
//   			String moveNumonic = stateModel.solutionResultsArray[stateModel.solutionResultIndex];
//   			Log.d(Constants.TAG, "Move:" + moveNumonic + ":");
//   			StringBuffer moveDescription = new StringBuffer("Rotate ");
//   			switch(moveNumonic.charAt(0)) {
//   			case 'U': moveDescription.append("Top Face"); break;
//   			case 'D': moveDescription.append("Down Face"); break;
//   			case 'L': moveDescription.append("Left Face"); break;
//   			case 'R': moveDescription.append("Right Face"); break;
//   			case 'F': moveDescription.append("Front Face"); break;
//   			case 'B': moveDescription.append("Back Face"); break;
//   			}
//   			if(moveNumonic.length() == 1)
//   				moveDescription.append(" Clockwise");
//   			else if(moveNumonic.charAt(1) == '2')
//   				moveDescription.append(" 180 Degrees");
//   			else if(moveNumonic.charAt(1) == '\'')
//   				moveDescription.append(" Counter Clockwise");
//   			else
//   				moveDescription.append("?");
//
//   			if(RubikMenuAndParameters.userTextDisplay == true)
//   				Core.putText(image, moveDescription.toString(), new Point(0, 60), Constants.FontFace, 4, Constants.ColorWhite, 4);
//
////
////   			// Args to be passed to renderer.
////   			Rotation rotation = null;
////   			FaceType faceType = null;
////   			Scalar color = null;
////   			
////   			if(moveNumonic.length() == 1) 
////   				rotation = Rotation.CLOCKWISE;
////   			else if(moveNumonic.charAt(1) == '2') 
////   				rotation = Rotation.ONE_HUNDRED_EIGHTY;
////   			else if(moveNumonic.charAt(1) == '\'') 
////   				rotation = Rotation.COUNTER_CLOCKWISE;
////   			else
////   				throw new java.lang.Error("Unknow rotation amount");
////
////   			// Obtain details of arrow to be rendered.
////   			switch(moveNumonic.charAt(0)) {
////   			case 'U': 
////   				faceType = FaceType.UP;
////   				color = Constants.RubikWhite;
////   				break;
////   			case 'D': 
////   				faceType = FaceType.DOWN;  
////   				color = Constants.RubikYellow;
////   				break;
////   			case 'L': 
////   				faceType = FaceType.LEFT;
////   				color = Constants.RubikGreen;
////   				break;
////   			case 'R': 
////   				faceType = FaceType.RIGHT; 
////   				color = Constants.RubikBlue;
////   				break;
////   			case 'F': 
////   				faceType = FaceType.FRONT;
////   				color = Constants.RubikRed;
////   				break;
////   			case 'B':
////   				faceType = FaceType.BACK;
////   				color = Constants.RubikOrange;
////   				break;
////   			}
////   			pilotGLRenderer.setRenderCube(true && RubikMenuAndParameters.cubeOverlayDisplay);
////   			pilotGLRenderer.showCubeEdgeRotationArrow(
////   					rotation,
////   					faceType, 
////   					color);
//   			break;
//
//   		case WAITING_FOR_MOVE_COMPLETE:
////   			pilotGLRenderer.setRenderArrow(false);
//   			if(RubikMenuAndParameters.userTextDisplay == true)
//   				Core.putText(image, "Waiting for move to be completed", new Point(0, 60), Constants.FontFace, 4, Constants.ColorWhite, 4);
//   			break;
//
//   		case DONE:
////   			pilotGLRenderer.setRenderArrow(false);
//   			break;
//
//   		default:
//   			if(RubikMenuAndParameters.userTextDisplay == true)
//   				Core.putText(image, "Oops", new Point(0, 60), Constants.FontFace, 5, Constants.ColorWhite, 5);
//   			break;
//   		}
//   		
//   		// User indicator that tables have been computed.
//   		Core.line(image, new Point(0, 0), new Point(1270, 0), pruneTableLoaderCount < 12 ? Constants.ColorRed : Constants.ColorGreen, 4);
//   	}
}
