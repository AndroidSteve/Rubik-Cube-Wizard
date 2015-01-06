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
import org.ar.rubik.Constants.FaceRecogniztionStateEnum;
import org.ar.rubik.RubikFace.FaceRecognitionStatusEnum;
import org.kociemba.twophase.Search;
import org.kociemba.twophase.Tools;

import android.util.Log;



/**
 * @author android.steve@testlens.com
 *
 */
public class AppStateMachine {

	private StateModel stateModel;

	// 12 tables need to be generated.  When count is 12, tables are valid.  Used by prune table loader.  
	public int pruneTableLoaderCount = 0;

	// Allows for more pleasing user interface
	private int gotItCount = 0;

	// Candidate Rubik Face to be possible adopted by Stable Face Recognizer state machine.
	private RubikFace candidateRubikFace = null;

	// Consecutive counts use by Stable Face Recognizer state machine.
	private int consecutiveCandiateRubikFaceCount = 0;
	
	// Use to determine a New Stable Face.
	private RubikFace lastNewStableRubikFace = null;
	
	// After all six faces have been seen, allow one more rotation to return cube to original orientation.
	private boolean allowOneMoreRotation = false;

    // Set when we want to reset state, but do it synchronously in the frame thread.
    private boolean scheduleReset = false;

	// Set when we want to recall a state from file, but do it synchronously in the frame thread.
	private boolean scheduleRecall = false;


	/**
	 * Application State Machine Constructor
	 * 
	 * @param stateModel
	 */
	public AppStateMachine(StateModel stateModel) {
		this.stateModel = stateModel;
	}


	/**
	 * On Face Event
	 * 
	 * This function is called any time a Rubik Face is recognized, even if it may be 
	 * inaccurate.  Further filtering is perform in this function.  The purpose
	 * of this state machine is to detect a reliable stable face, and to make 
	 * the event calls of onFace and offFace into the app state machine.
	 * 
	 * @param rubikFace 
	 * 
	 */
	public void onFaceEvent(RubikFace rubikFace) {

		// Threshold for the number of times a face must be seen in order to declare it stable.
		final int consecutiveCandidateCountThreashold = 3;	

		Log.d(Constants.TAG_CNTRL, "onFaceEvent() AppState=" + stateModel.appState + " FaceState=" + stateModel.faceRecogniztionState + " Candidate=" + (candidateRubikFace == null ? 0 : candidateRubikFace.myHashCode) + " NewFace=" + (rubikFace == null ? 0 :rubikFace.myHashCode) );   	 

		// Reset Application State.  All past is forgotten.
		if(scheduleReset == true) {
			gotItCount = 0;
			scheduleReset = false;
			candidateRubikFace = null;
			consecutiveCandiateRubikFaceCount = 0;
			lastNewStableRubikFace = null;
			allowOneMoreRotation = false;
			stateModel.reset();
		}
		
		// Reset Application State, and then recall app state from file.
		if(scheduleRecall == true) {
			gotItCount = 0;
			scheduleRecall = false;
			candidateRubikFace = null;
			consecutiveCandiateRubikFaceCount = 0;
			lastNewStableRubikFace = null;
			allowOneMoreRotation = false;
			stateModel.reset();
			stateModel.recallState();
			stateModel.appState = AppStateEnum.COMPLETE;  // Assumes state stored in file is complete.
		}


		// Sometimes, we want state to change simply on frame events.  This has
		// the exact same event model as onFaceEvent().
		onFrameEvent();

		switch(stateModel.faceRecogniztionState) {

		case UNKNOWN:
			if(rubikFace.faceRecognitionStatus == FaceRecognitionStatusEnum.SOLVED) {
				stateModel.faceRecogniztionState = FaceRecogniztionStateEnum.PENDING;
				candidateRubikFace = rubikFace;
				consecutiveCandiateRubikFaceCount = 0;
			}
			else
				; // stay in unknown state.
				break;


		case PENDING:
			if(rubikFace.faceRecognitionStatus == FaceRecognitionStatusEnum.SOLVED) {

				if(rubikFace.myHashCode == candidateRubikFace.myHashCode) {

					if(consecutiveCandiateRubikFaceCount > consecutiveCandidateCountThreashold) {
					    
					    if(lastNewStableRubikFace == null || rubikFace.myHashCode != lastNewStableRubikFace.myHashCode) {
					        lastNewStableRubikFace = rubikFace;
					        stateModel.faceRecogniztionState = FaceRecogniztionStateEnum.NEW_STABLE;
					        onNewStableFaceEvent(rubikFace);
                            onStableFaceEvent(candidateRubikFace);
					    }
					    
					    else {
	                      stateModel.faceRecogniztionState = FaceRecogniztionStateEnum.STABLE;
	                      onStableFaceEvent(candidateRubikFace);	        
					    }
					}
					else 
						consecutiveCandiateRubikFaceCount++;
				}
				//        			else if(false)
					//        				;// =+= add partial match here
				else
					stateModel.faceRecogniztionState = FaceRecogniztionStateEnum.UNKNOWN;
			}
			else
				stateModel.faceRecogniztionState = FaceRecogniztionStateEnum.UNKNOWN;	
			break;


		case STABLE:
			if(rubikFace.faceRecognitionStatus == FaceRecognitionStatusEnum.SOLVED) {

				if(rubikFace.myHashCode == candidateRubikFace.myHashCode) 
					; // Just stay in this state
				//        			else if(false)
				//        				; // =+= add partial match here
				else {
					stateModel.faceRecogniztionState = FaceRecogniztionStateEnum.PARTIAL;
					consecutiveCandiateRubikFaceCount = 0;
				}
			}
			else {
				stateModel.faceRecogniztionState = FaceRecogniztionStateEnum.PARTIAL;
				consecutiveCandiateRubikFaceCount = 0;
			}
			break;


		case PARTIAL:
			if(rubikFace.faceRecognitionStatus == FaceRecognitionStatusEnum.SOLVED) {

				if(rubikFace.myHashCode == candidateRubikFace.myHashCode) {
				    
				    if(lastNewStableRubikFace != null && rubikFace.myHashCode == lastNewStableRubikFace.myHashCode) {
				        stateModel.faceRecogniztionState = FaceRecogniztionStateEnum.NEW_STABLE;
				    }
				    else
				        stateModel.faceRecogniztionState = FaceRecogniztionStateEnum.STABLE;
				}
				//        			else if(false)
				//        				; // =+= add partial match here
				else {
					if(consecutiveCandiateRubikFaceCount > consecutiveCandidateCountThreashold) {
						stateModel.faceRecogniztionState = FaceRecogniztionStateEnum.UNKNOWN;
				        offNewStableFaceEvent();
                        offStableFaceEvent();
					}
					else 
						consecutiveCandiateRubikFaceCount++; // stay in partial state
				}
			}
			else {
				if(consecutiveCandiateRubikFaceCount > consecutiveCandidateCountThreashold) {
					stateModel.faceRecogniztionState = FaceRecogniztionStateEnum.UNKNOWN;
			        offNewStableFaceEvent();
                    offStableFaceEvent();
				}
				else 
					consecutiveCandiateRubikFaceCount++; // stay in partial state
			}
			break;
			
			
        case NEW_STABLE:
            if(rubikFace.faceRecognitionStatus == FaceRecognitionStatusEnum.SOLVED) {

                if(rubikFace.myHashCode == candidateRubikFace.myHashCode) 
                    ; // Just stay in this state
                //                  else if(false)
                //                      ; // =+= add partial match here
                else {
                    stateModel.faceRecogniztionState = FaceRecogniztionStateEnum.PARTIAL;
                    consecutiveCandiateRubikFaceCount = 0;
                }
            }
            else {
                stateModel.faceRecogniztionState = FaceRecogniztionStateEnum.PARTIAL;
                consecutiveCandiateRubikFaceCount = 0;
            }            
            break;
		}
	}



	/**
	 * On Stable Rubik Face Recognized
	 * 
	 * This function is called ever frame when a valid and stable Rubik Face is recognized.
	 * 
	 * @param myHashCode 
	 * 
	 */
	private void onStableFaceEvent(RubikFace rubikFace) {

		Log.i(Constants.TAG_CNTRL, "+onStableRubikFaceRecognized: last=" + (lastNewStableRubikFace == null ? 0 : lastNewStableRubikFace.myHashCode) + " new=" + rubikFace.myHashCode);

		switch (stateModel.appState) {

		case WAITING_MOVE:
			stateModel.appState = AppStateEnum.DO_MOVE;
			stateModel.solutionResultIndex++;
			if(stateModel.solutionResultIndex == stateModel.solutionResultsArray.length)
				stateModel.appState = AppStateEnum.DONE;
			break;

		default:
			break;
		}
	}
	
	
	/**
     * Off Stable Rubik Face Recognized
     * 
     * This function is called ever frame when there is no longer a stable face.
     * 
     * @param myHashCode 
     * 
     */
	public void offStableFaceEvent() {

		Log.i(Constants.TAG_CNTRL, "-offStableRubikFaceRecognized: previous=" + lastNewStableRubikFace.myHashCode);
		
		switch (stateModel.appState) {

		case DO_MOVE:		
			stateModel.appState = AppStateEnum.WAITING_MOVE;
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
	private void onNewStableFaceEvent(RubikFace candidateRubikFace) {

		Log.i(Constants.TAG_CNTRL, "+onNewStableRubikFaceRecognized  Previous State =" + stateModel.appState);


		switch(stateModel.appState) {

		case START:
			stateModel.adopt(candidateRubikFace);
			stateModel.appState = AppStateEnum.GOT_IT;
            gotItCount = 0;
			break;

		case SEARCHING:
			stateModel.adopt(candidateRubikFace);

			// Have not yet seen all six sides.
			if(stateModel.isThereAfullSetOfFaces() == false) {
				stateModel.appState = AppStateEnum.GOT_IT;
				allowOneMoreRotation = true;
	            gotItCount = 0;
			}

			// Do one more turn so cube returns to original orientation.
			else if(allowOneMoreRotation == true) {
				stateModel.appState = AppStateEnum.GOT_IT;
				allowOneMoreRotation = false;
	            gotItCount = 0;
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
	
	
	/**
     * Off New Stable Rubik Face Recognized
     * 
     * This is called when the new stable face is gone.
     * 
     * @param rubikFaceHashCode
     */
	private void offNewStableFaceEvent() {

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
	 * This function is call every time function onFaceEvent() is called, and thus has
	 * the identical event model.
	 * 
	 * It appears handy to have some controller state changes advanced on the periodic frame rate.
	 * Unfortunately, the rate that is function is called is dependent upon the bulk of opencv
	 * processing which can vary with the background.
	 */
	private void onFrameEvent() {

		switch(stateModel.appState) {

		case WAIT_TABLES:
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
				stateModel.appState = AppStateEnum.WAIT_TABLES;
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
			stateModel.solutionResults = Search.solution(cubeString2, 25, 5, false);
			Log.i(Constants.TAG_CNTRL, "Solution Results: " + stateModel.solutionResults);
			if (stateModel.solutionResults.contains("Error")) {
				char solutionCode = stateModel.solutionResults.charAt(stateModel.solutionResults.length() - 1);
				stateModel.verificationResults = solutionCode - '0';
				Log.i(Constants.TAG_CNTRL, "Solution Error: " + Util.getTwoPhaseErrorString(solutionCode) );
				stateModel.appState = AppStateEnum.ERROR;
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
	 * synchronously in the frame thread to eliminate concurrency problems.
	 */
	public void reset() {
		scheduleReset = true;
	}


	/**
	 * Request that the state is recalled from file.  This is performed
	 * synchronously in the frame thread to eliminate concurrency problems.
	 */
    public void recallState() {
    	scheduleRecall = true;
    }
}
