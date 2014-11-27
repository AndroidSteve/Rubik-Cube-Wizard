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
 *   application state should change.  There are two state machines contained in this
 *   class: the Stable Face Recognizer State Machine, and the Application State Machine.
 *   See Design.docx for more details.
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
import org.ar.rubik.Constants.FaceRecogniztionState;
import org.ar.rubik.RubikFace.FaceRecognitionStatusEnum;
//import org.ar.rubik.gl.UserInstructionsGLRenderer.FaceType;
//import org.ar.rubik.gl.UserInstructionsGLRenderer.Rotation;
import org.kociemba.twophase.Search;
import org.kociemba.twophase.Tools;

import android.util.Log;



/**
 * @author android.steve@testlens.com
 *
 */
public class AppStateMachine {

	private StateModel stateModel;

	// 12 tables need to be generated.  When count is 12, tables are valid.
	// =+= used by prune table loader.  
	public int pruneTableLoaderCount = 0;

	// Allows for more pleasing user interface
	private int gotItCount = 0;

	// Set when we want to reset state, but do it synchronously in the frame thread.
	private boolean scheduleReset = false;

	// Candidate Rubik Face to be possible adopted by Stable Face Recognizer state machine.
	private RubikFace candidateRubikFace = null;

	// Consecutive counts use by Stable Face Recognizer state machine.
	private int consecutiveCandiateRubikFaceCount = 0;
	
	// Use by "New Stable Face Recognizer" state machine.
	private RubikFace lastStableRubikFace = null;
	
	// After all six faces have been seen, allow one more rotation to return cube to original orientation.
	private boolean allowOneMoreRotation = false;


	/**
	 * @param stateModel
	 */
	public AppStateMachine(StateModel stateModel) {
		this.stateModel = stateModel;
	}


	/**
	 * On Rubik Face Recognized
	 * 
	 * This function is called any time a Rubik Face is recognized, even if it may be 
	 * inaccurate.  Further filtering is perform in this function.  The purpose
	 * of this state machine is to detect a reliable stable face, and to make 
	 * the event calls of onFace and offFace into the app state machine.
	 * 
	 * @param rubikFace 
	 * 
	 */
	public void processFace(RubikFace rubikFace) {

		// Threshold for the number of times a face must be seen in order to declare it stable.
		final int consecutiveCandidateCountThreashold = 4;	

		Log.d(Constants.TAG_CNTRL, "processRubikFaceSolution() state=" + stateModel.faceRecogniztionState + " candidate=" + (candidateRubikFace == null ? 0 : candidateRubikFace.hashCode) + " newFace=" + (rubikFace == null ? 0 :rubikFace.hashCode) );   	 

		// Reset Application State.  All past is forgotten.
		if(scheduleReset == true) {
			scheduleReset = false;
			gotItCount = 0;
			candidateRubikFace = null;
			consecutiveCandiateRubikFaceCount = 0;
			lastStableRubikFace = null;
			allowOneMoreRotation = false;
			stateModel.reset();
		}

		// Sometimes, we want state to change simply on frame events.
		onFrameStateChanges();

		switch(stateModel.faceRecogniztionState) {

		case UNKNOWN:
			if(rubikFace.faceRecognitionStatus == FaceRecognitionStatusEnum.SOLVED) {
				stateModel.faceRecogniztionState = FaceRecogniztionState.PENDING;
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
						stateModel.faceRecogniztionState = FaceRecogniztionState.STABLE;
						onStableRubikFaceRecognition(candidateRubikFace);
					}
					else 
						consecutiveCandiateRubikFaceCount++;
				}
				//        			else if(false)
					//        				;// =+= add partial match here
				else
					stateModel.faceRecogniztionState = FaceRecogniztionState.UNKNOWN;
			}
			else
				stateModel.faceRecogniztionState = FaceRecogniztionState.UNKNOWN;	
			break;


		case STABLE:
			if(rubikFace.faceRecognitionStatus == FaceRecognitionStatusEnum.SOLVED) {

				if(rubikFace.hashCode == candidateRubikFace.hashCode) 
					; // Just stay in this state
				//        			else if(false)
				//        				; // =+= add partial match here
				else {
					stateModel.faceRecogniztionState = FaceRecogniztionState.PARTIAL;
					consecutiveCandiateRubikFaceCount = 0;
				}
			}
			else {
				stateModel.faceRecogniztionState = FaceRecogniztionState.PARTIAL;
				consecutiveCandiateRubikFaceCount = 0;
			}
			break;


		case PARTIAL:
			if(rubikFace.faceRecognitionStatus == FaceRecognitionStatusEnum.SOLVED) {

				if(rubikFace.hashCode == candidateRubikFace.hashCode)
					stateModel.faceRecogniztionState = FaceRecogniztionState.STABLE;
				//        			else if(false)
				//        				; // =+= add partial match here
				else {
					if(consecutiveCandiateRubikFaceCount > consecutiveCandidateCountThreashold) {
						stateModel.faceRecogniztionState = FaceRecogniztionState.UNKNOWN;
						offStableRubikFaceRecognition();
					}
					else 
						consecutiveCandiateRubikFaceCount++; // stay in partial state
				}
			}
			else {
				if(consecutiveCandiateRubikFaceCount > consecutiveCandidateCountThreashold) {
					stateModel.faceRecogniztionState = FaceRecogniztionState.UNKNOWN;
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
	private void onStableRubikFaceRecognition(RubikFace rubikFace) {

		Log.i(Constants.TAG_CNTRL, "+onStableRubikFaceRecognized: last=" + (lastStableRubikFace == null ? 0 : lastStableRubikFace.hashCode) + " new=" + rubikFace.hashCode);
		if(lastStableRubikFace == null || rubikFace.hashCode != lastStableRubikFace.hashCode) {
			lastStableRubikFace = rubikFace;
			onNewStableRubikFaceRecognized(rubikFace);
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



	/**
	 * Request that the state is reset to initial values.  This is performed
	 * synchronously in the frame thread to eliminate problems.
	 */
	public void reset() {
		scheduleReset = true;
	}
}
