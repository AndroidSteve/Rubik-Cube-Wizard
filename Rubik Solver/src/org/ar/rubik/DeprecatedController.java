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
 *   Each frame of video is first passed to this Object.
 *   It's responsibility is to orchestrate the user
 *   through the various moves to a Rubik solution.
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

import org.ar.rubik.Constants.AnnotationModeEnum;
import org.ar.rubik.Constants.ImageProcessModeEnum;
import org.ar.rubik.Constants.ImageSourceModeEnum;
import org.ar.rubik.DeprecatedRubikFace.FaceRecognitionStatusEnum;
import org.ar.rubik.gl.PilotCubeGLRenderer;
import org.ar.rubik.gl.UserInstructionsGLRenderer;
import org.ar.rubik.gl.UserInstructionsGLRenderer.FaceType;
import org.ar.rubik.gl.UserInstructionsGLRenderer.Rotation;
import org.kociemba.twophase.PruneTableLoader;
import org.kociemba.twophase.Search;
import org.kociemba.twophase.Tools;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import android.os.AsyncTask;
import android.util.Log;


public class DeprecatedController {
	
	public enum ControllerStateEnum { 
		START,     // Ready
		GOT_IT,    // A Cube Face has been recognized and captured.
		ROTATE,    // Request user to rotate Cube.
		SEARCHING, // Attempting to lock onto new Cube Face.
		COMPLETE,  // All six faces have been captured, and we seem to have valid color.
		BAD_COLORS,// All six faces have been captured, but we do not have properly nine tiles of each color.
		VERIFIED,  // Two Phase solution has verified that the cube tile/colors/positions are a valid cube.
		WAITING,   // Waiting for TwoPhase Prune Tree generation to complete.
		INCORRECT, // Two Phase solution has analyzed the cube and found it to be invalid.
		SOLVED,    // Two Phase solution has analyzed the cube and found a solution.
		DO_MOVE,   // Inform user to perform a face rotation
		WAITING_FOR_MOVE_COMPLETE, // Wait for face rotation to complete
		DONE       // Cube should be completely physically solved.
		};
	
	private ControllerStateEnum controllerState = ControllerStateEnum.START;
	
	// 12 tables need to be generated.  When count is 12, tables are valid.
	private int pruneTableLoaderCount = 0;
	
	// Allows for more pleasing user interface
	private int gotItCount = 0;

	// Specifies where image comes from
    public ImageSourceModeEnum imageSourceMode;

	// Specifies what to do with image
    public ImageProcessModeEnum imageProcessMode;
    
    // Specified what annotation to add
    public AnnotationModeEnum annotationMode;

    // Result when Two Phase algorithm is ask to evaluate if cube in valid.  If valid, code is zero.
	private int verificationResults;

	// String notation on how to solve cube.
	private String solutionResults;
	
	// Above, but broken into individual moves.
	private String [] solutionResultsArray;
	
	// Index to above array as to which move we are on.
	private int solutionResultIndex;

	// Each of this is a layer on the screen
	private UserInstructionsGLRenderer userInstructionsGLRenderer;
	private PilotCubeGLRenderer annotationGlRenderer;

	// Most recent face (not necessarily that which decision are made on).
	private DeprecatedRubikFace rubikFace;

	// Perform reset on Frame Thread, otherwise if performed asynchronously, there are too many potential null pointer problems.
	private boolean pendendReset = false;
	
	private boolean hackFastMode = false;
	private boolean hackSkipImageProc = false;

	private DeprecatedRubikFace hackRubikFace;
	


	
    /**
     * Controller Constructor
     * @param userInstructionsGLRenderer 
     * @param annotationGlRenderer 
     * 
     */
    public DeprecatedController(UserInstructionsGLRenderer userInstructionsGLRenderer, PilotCubeGLRenderer annotationGlRenderer) {
    	
    	this.userInstructionsGLRenderer = userInstructionsGLRenderer;
    	this.annotationGlRenderer = annotationGlRenderer;
    	
    	/*
    	 * Launch thread to asynchronous, and probably in a different CPU, calculate
    	 * Two Phase Prune Tables.  These tables require 150 Mbytes of RAM and take
    	 * about 15 seconds to compute.  They are required by the Two Phase algorithm
    	 * to compute a solution for a valid Rubik Cube.
    	 */
    	new LoadPruningTablesTask().execute();
	}



	/**
     * On Camera Frame
     * 
     * @param inputFrame
	 * @param annotationMode2 
	 * @param imageProcessMode2 
	 * @param imageSourceMode2 
     * @return
     */
    public Mat onCameraFrame(
    		CvCameraViewFrame inputFrame, 
    		ImageSourceModeEnum _imageSourceMode, 
    		ImageProcessModeEnum _imageProcessMode, 
    		AnnotationModeEnum _annotationMode) {
    	
    	// Convert to RGBA  =+= However, I think this is actually BGRA in opencv vernacular.
    	Mat image = inputFrame.rgba(); // CV_8UC4
    	
    	// Update Modes
		this.imageSourceMode  = _imageSourceMode;
		this.imageProcessMode = _imageProcessMode;
		this.annotationMode   = _annotationMode; 	
    	
		// Reset all member data to initial state
		if(pendendReset == true) {
			pendendReset = false;
			controllerState = ControllerStateEnum.START;
		    faceRecogniztionState = FaceRecogniztionState.UNKNOWN;
		    rubikFace = null;
		    candidateRubikFace = null;
		    lastStableRubikFace = null;
		    consecutiveCandiateRubikFaceCount = 0;
		    allowOneMoreRotation = false;
			DeprecatedRubikCube.reset();
		}
		
		switch( imageSourceMode) {

		case NORMAL:
			break;

		case SAVE_NEXT:
			Util.saveImage(image);
			imageSourceMode = ImageSourceModeEnum.NORMAL;
			break;

		case PLAYBACK:
			image = Util.recallImage();

		default:
			break;
		}
		
		
		Mat resultImage;
  
		// Skip any image processing and thus allow controller state machine to proceeded more quickly
		if(hackSkipImageProc == true && hackFastMode == true) {
			rubikFace = hackRubikFace;
			resultImage = image;
		}
		else {

			// Instantiate a Rubik Face Object
			rubikFace = new DeprecatedRubikFace(imageProcessMode);

			// Process Image and Obtain a Rubik Face object if possible		
			resultImage = rubikFace.findSolutionFromImage(image);

			hackRubikFace = rubikFace;
		}
		
		// Process Rubik Face results in Controller State Machine
		processRubikFaceSolution(rubikFace);

		// Add Annotation on right side.
		addAnnoation(resultImage);
		
		// Draw Face Overlay
		rubikFace.drawRubikCubeOverlay(image, faceRecogniztionState == FaceRecogniztionState.STABLE);
			
		// Render User Instruction along Top
		renderUserInstructions(image, rubikFace);
		
		return resultImage;
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
    private DeprecatedRubikFace candidateRubikFace = null;
    private int consecutiveCandiateRubikFaceCount = 0;
	private final int consecutiveCandidateCountThreashold = 1;

    private void processRubikFaceSolution(DeprecatedRubikFace rubikFace) {
    	
    	DeprecatedRubikCube.active = rubikFace;
    	
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
//    			else if(false)
//    				;// =+= add partial match here
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
//    			else if(false)
//    				; // =+= add partial match here
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
//    			else if(false)
//    				; // =+= add partial match here
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
    private DeprecatedRubikFace lastStableRubikFace = null;
	private boolean allowOneMoreRotation = false;
    private void onStableRubikFaceRecognition(DeprecatedRubikFace rubikFace) {

   		Log.i(Constants.TAG_CNTRL, "+onStableRubikFaceRecognized: last=" + (lastStableRubikFace == null ? 0 : lastStableRubikFace.hashCode) + " new=" + rubikFace.hashCode);
    	if(lastStableRubikFace == null || rubikFace.hashCode != lastStableRubikFace.hashCode) {
    		lastStableRubikFace = rubikFace;
    		onNewStableRubikFaceRecognized(rubikFace);
    	}
    	

    	switch (controllerState) {
    	
    	case WAITING_FOR_MOVE_COMPLETE:
    		controllerState = ControllerStateEnum.DO_MOVE;
    		solutionResultIndex++;
    		if(solutionResultIndex == solutionResultsArray.length)
    			controllerState = ControllerStateEnum.DONE;
		break;

    	default:
    		break;
    	}
    }
    public void offStableRubikFaceRecognition() {
    	
    	Log.i(Constants.TAG_CNTRL, "-offStableRubikFaceRecognized: previous=" + lastStableRubikFace.hashCode);
    	offNewStableRubikFaceRecognition();
    	
    	switch (controllerState) {

    	case DO_MOVE:		
    		controllerState = ControllerStateEnum.WAITING_FOR_MOVE_COMPLETE;
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
    private void onNewStableRubikFaceRecognized(DeprecatedRubikFace rubikFace) {
    	
    	Log.i(Constants.TAG_CNTRL, "+onNewStableRubikFaceRecognized  Previous State =" + controllerState);


    	switch(controllerState) {

    	case START:
    		DeprecatedRubikCube.adopt(rubikFace);
    		controllerState = ControllerStateEnum.GOT_IT;
    		hackFastMode = true;
    		break;
    		
    	case SEARCHING:
    		DeprecatedRubikCube.adopt(rubikFace);

    		// Have not yet seen all six sides.
    		if(DeprecatedRubikCube.isThereAfullSetOfFaces() == false) {
    			controllerState = ControllerStateEnum.GOT_IT;
    			hackFastMode = true;
    			allowOneMoreRotation = true;
    		}
    		
    		// Do one more turn so cube returns to original orientation.
    		else if(allowOneMoreRotation == true) {
    			controllerState = ControllerStateEnum.GOT_IT;
    			hackFastMode = true;
    			allowOneMoreRotation = false;
    		}
    		
    		// Begin processing of cube: first check that there are exactly 9 tiles of each color.
    		else
    			if(DeprecatedRubikCube.isTileColorsValid() == true)
    				controllerState = ControllerStateEnum.COMPLETE;
    			else
    				controllerState = ControllerStateEnum.BAD_COLORS;
    		break;
    		
		default:
			break;
    	}
    }
   private void offNewStableRubikFaceRecognition() {
    	
    	Log.i(Constants.TAG_CNTRL, "-offNewStableRubikFaceRecognition  Previous State =" + controllerState);
    	
       	switch(controllerState) {

    	case ROTATE:
    		controllerState = ControllerStateEnum.SEARCHING;
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

	   switch(controllerState) {

	   case WAITING:
		   hackFastMode = false;
		   if(pruneTableLoaderCount == 12) {
			   controllerState = ControllerStateEnum.VERIFIED;
			   hackFastMode = true;
		   }
		   break;


	   case GOT_IT:
		   hackFastMode = false;
		   if(gotItCount < 3)
			   gotItCount++;
		   else {
			   controllerState = ControllerStateEnum.ROTATE;
			   gotItCount = 0;
		   }
		   break;

		   
	   case COMPLETE:
		   String cubeString = DeprecatedRubikCube.getStringRepresentationOfCube();

		   // Returns 0 if cube is solvable.
		   verificationResults = Tools.verify(cubeString);

		   if(verificationResults == 0) {
			   controllerState = ControllerStateEnum.WAITING;
			   hackFastMode = true;
		   }
		   else
			   controllerState = ControllerStateEnum.INCORRECT;

		   String stringErrorMessage = Util.getTwoPhaseErrorString((char)(verificationResults * -1 + '0'));

		   Log.i(Constants.TAG_CNTRL, "Cube String Rep: " + cubeString);
		   Log.i(Constants.TAG_CNTRL, "Verification Results: (" + verificationResults + ") " + stringErrorMessage);
		   break;

		   
	   case VERIFIED:
		   hackFastMode = false;
		   String cubeString2 = DeprecatedRubikCube.getStringRepresentationOfCube();

		   // Returns 0 if solution computed
		   solutionResults = Search.solution(cubeString2, 25, 2, false);
		   Log.i(Constants.TAG_CNTRL, "Solution Results: " + solutionResults);
		   if (solutionResults.contains("Error")) {
			   char solutionCode = solutionResults.charAt(solutionResults.length() - 1);
			   verificationResults = solutionCode - '0';
			   Log.i(Constants.TAG_CNTRL, "Solution Error: " + Util.getTwoPhaseErrorString(solutionCode) );
			   controllerState = ControllerStateEnum.INCORRECT;
		   }
		   else {
			   controllerState = ControllerStateEnum.SOLVED;
			   hackFastMode = true;
		   }
		   break;

		   
	   case SOLVED:
		   hackFastMode = false;
		   solutionResultsArray = solutionResults.split(" ");
		   Log.i(Constants.TAG_CNTRL, "Solution Results Array: " + solutionResultsArray);
		   solutionResultIndex = 0;
		   controllerState = ControllerStateEnum.DO_MOVE;
		   break;
		   
		   
	   default:
		   break;
	   }
   }



	/**
	 * Render User Instructions
	 * 
	 * @param image
	 * @param rubikFace 
	 */
	private void renderUserInstructions(Mat image, DeprecatedRubikFace rubikFace) {

		// Create black area for text
		if(MenuAndParams.userTextDisplay == true)
			Core.rectangle(image, new Point(0, 0), new Point(1270, 60), Constants.ColorBlack, -1);

		userInstructionsGLRenderer.setCubeOrienation(rubikFace);

		switch(controllerState) {

		case START:
			if(MenuAndParams.userTextDisplay == true)
				Core.putText(image, "Show Me The Rubik Cube", new Point(0, 60), Constants.FontFace, 5, Constants.ColorWhite, 5);
			userInstructionsGLRenderer.setRenderArrow(false);
			break;

		case GOT_IT:
			if(MenuAndParams.userTextDisplay == true)
				Core.putText(image, "OK, Got It", new Point(0, 60), Constants.FontFace, 5, Constants.ColorWhite, 5);
			userInstructionsGLRenderer.setRenderArrow(false);
			userInstructionsGLRenderer.setRenderCube(MenuAndParams.cubeOverlayDisplay);
			break;

		case ROTATE:
			if(MenuAndParams.userTextDisplay == true)
				Core.putText(image, "Please Rotate: " + DeprecatedRubikCube.getNumValidFaces(), new Point(0, 60), Constants.FontFace, 5, Constants.ColorWhite, 5);
			if(  DeprecatedRubikCube.getNumValidFaces() % 2 == 0)
				userInstructionsGLRenderer.showFullCubeRotateArrow(FaceType.LEFT_TOP);
			else
				userInstructionsGLRenderer.showFullCubeRotateArrow(FaceType.FRONT_TOP);
			break;

		case SEARCHING:
			if(MenuAndParams.userTextDisplay == true)
				Core.putText(image, "Searching for Another Face", new Point(0, 60), Constants.FontFace, 5, Constants.ColorWhite, 5);
			userInstructionsGLRenderer.setRenderArrow(false);
			break;

		case COMPLETE:
			if(MenuAndParams.userTextDisplay == true)
				Core.putText(image, "Cube is Complete and has Good Colors", new Point(0, 60), Constants.FontFace, 4, Constants.ColorWhite, 4);
			break;

		case WAITING:
			if(MenuAndParams.userTextDisplay == true)
				Core.putText(image, "Waiting - Preload Next: " + pruneTableLoaderCount, new Point(0, 60), Constants.FontFace, 5, Constants.ColorWhite, 5);
			break;

		case BAD_COLORS:
//			if(MenuAndParams.userTextDisplay == true)
				Core.putText(image, "Cube is Complete but has Bad Colors", new Point(0, 60), Constants.FontFace, 4, Constants.ColorWhite, 4);
			break;

		case VERIFIED:
			if(MenuAndParams.userTextDisplay == true)
				Core.putText(image, "Cube is Complete and Verified", new Point(0, 60), Constants.FontFace, 4, Constants.ColorWhite, 4);
			break;

		case INCORRECT:
//			if(MenuAndParams.userTextDisplay == true)
				Core.putText(image, "Cube is Complete but Incorrect: " + verificationResults, new Point(0, 60), Constants.FontFace, 4, Constants.ColorWhite, 4);
			break;

		case SOLVED:
			if(MenuAndParams.userTextDisplay == true) {
				Core.putText(image, "SOLUTION: ", new Point(0, 60), Constants.FontFace, 4, Constants.ColorWhite, 4);
				Core.rectangle(image, new Point(0, 60), new Point(1270, 120), Constants.ColorBlack, -1);
				Core.putText(image, "" + solutionResults, new Point(0, 120), Constants.FontFace, 2, Constants.ColorWhite, 2);
			}
			break;

		case DO_MOVE:
			String moveNumonic = solutionResultsArray[solutionResultIndex];
			Log.d(Constants.TAG, "Move:" + moveNumonic + ":");
			StringBuffer moveDescription = new StringBuffer("Rotate ");
			switch(moveNumonic.charAt(0)) {
			case 'U': moveDescription.append("Top Face"); break;
			case 'D': moveDescription.append("Down Face"); break;
			case 'L': moveDescription.append("Left Face"); break;
			case 'R': moveDescription.append("Right Face"); break;
			case 'F': moveDescription.append("Front Face"); break;
			case 'B': moveDescription.append("Back Face"); break;
			}
			if(moveNumonic.length() == 1)
				moveDescription.append(" Clockwise");
			else if(moveNumonic.charAt(1) == '2')
				moveDescription.append(" 180 Degrees");
			else if(moveNumonic.charAt(1) == '\'')
				moveDescription.append(" Counter Clockwise");
			else
				moveDescription.append("?");

			if(MenuAndParams.userTextDisplay == true)
				Core.putText(image, moveDescription.toString(), new Point(0, 60), Constants.FontFace, 4, Constants.ColorWhite, 4);


			// Args to be passed to renderer.
			Rotation rotation = null;
			FaceType faceType = null;
			Scalar color = null;
			
			if(moveNumonic.length() == 1) 
				rotation = Rotation.CLOCKWISE;
			else if(moveNumonic.charAt(1) == '2') 
				rotation = Rotation.ONE_HUNDRED_EIGHTY;
			else if(moveNumonic.charAt(1) == '\'') 
				rotation = Rotation.COUNTER_CLOCKWISE;
			else
				throw new java.lang.Error("Unknow rotation amount");

			// Obtain details of arrow to be rendered.
			switch(moveNumonic.charAt(0)) {
			case 'U': 
				faceType = FaceType.UP;
				color = Constants.RubikWhite;
				break;
			case 'D': 
				faceType = FaceType.DOWN;  
				color = Constants.RubikYellow;
				break;
			case 'L': 
				faceType = FaceType.LEFT;
				color = Constants.RubikGreen;
				break;
			case 'R': 
				faceType = FaceType.RIGHT; 
				color = Constants.RubikBlue;
				break;
			case 'F': 
				faceType = FaceType.FRONT;
				color = Constants.RubikRed;
				break;
			case 'B':
				faceType = FaceType.BACK;
				color = Constants.RubikOrange;
				break;
			}
			userInstructionsGLRenderer.setRenderCube(true && MenuAndParams.cubeOverlayDisplay);
			userInstructionsGLRenderer.showCubeEdgeRotationArrow(
					rotation,
					faceType, 
					color);
			break;

		case WAITING_FOR_MOVE_COMPLETE:
			userInstructionsGLRenderer.setRenderArrow(false);
			if(MenuAndParams.userTextDisplay == true)
				Core.putText(image, "Waiting for move to be completed", new Point(0, 60), Constants.FontFace, 4, Constants.ColorWhite, 4);
			break;

		case DONE:
			userInstructionsGLRenderer.setRenderArrow(false);
			break;

		default:
			if(MenuAndParams.userTextDisplay == true)
				Core.putText(image, "Oops", new Point(0, 60), Constants.FontFace, 5, Constants.ColorWhite, 5);
			break;
		}
		
		// User indicator that tables have been computed.
		Core.line(image, new Point(0, 0), new Point(1270, 0), pruneTableLoaderCount < 12 ? Constants.ColorRed : Constants.ColorGreen, 4);
	}



	/**
	 * 
	 */
	public void reset() {
		pendendReset = true;
	}



	/**
	 * Add Annotation
	 * 
	 * @param image
	 * @return
	 */
	private Mat addAnnoation(Mat image) {
		
		switch(annotationMode) {
		
		case LAYOUT:
			annotationGlRenderer.setRenderState(false);
	    	Core.rectangle(image, new Point(0, 0), new Point(450, 720), Constants.ColorBlack, -1);
			DeprecatedRubikCube.renderFlatLayoutRepresentation(image);
			break;
			
		case RHOMBUS:
			annotationGlRenderer.setRenderState(false);
	    	Core.rectangle(image, new Point(0, 0), new Point(450, 720), Constants.ColorBlack, -1);
	    	Rhombus.deprecatedRenderRhombusRecognitionMetrics(image, DeprecatedProcessImage.polygonList);
			break;

		case FACE_METRICS:
			annotationGlRenderer.setRenderState(false);
			if(DeprecatedRubikCube.active != null) {
		    	Core.rectangle(image, new Point(0, 0), new Point(450, 720), Constants.ColorBlack, -1);
				DeprecatedRubikCube.active.renderFaceRecognitionMetrics(image);
			}
			break;
			
		case CUBE_METRICS:
			annotationGlRenderer.setRenderState(false);
		    Core.rectangle(image, new Point(0, 0), new Point(450, 720), Constants.ColorBlack, -1);
			DeprecatedRubikCube.renderCubeMetrics(image);
			break;

		case TIME:
			annotationGlRenderer.setRenderState(false);
	    	Core.rectangle(image, new Point(0, 0), new Point(450, 720), Constants.ColorBlack, -1);
	    	DeprecatedProcessImage.renderTimeConsumptionMetrics(image);
			break;
			
		case COLOR:
			annotationGlRenderer.setRenderState(false);
			if(DeprecatedRubikCube.active != null) {
		    	Core.rectangle(image, new Point(0, 0), new Point(570, 720), Constants.ColorBlack, -1);
				DeprecatedRubikCube.active.renderColorMetrics(image);
			}
			break;
			
		case NORMAL:
			Core.rectangle(image, new Point(0, 0), new Point(350, 720), Constants.ColorBlack, -1);
			annotationGlRenderer.setRenderState(true);
			annotationGlRenderer.setCubeOrienation(DeprecatedRubikCube.active);
			break;
		}
		
		return image;
	}



	/**
	 * @author stevep
	 *
	 */
	private class LoadPruningTablesTask extends AsyncTask<Void, Void, Void> {
		
	    private PruneTableLoader tableLoader = new PruneTableLoader();

	    @Override
	    protected Void doInBackground(Void... params) {
	    	
	        /* load all tables if they are not already in RAM */
	        while (!tableLoader.loadingFinished()) { // while tables are left to load
	            tableLoader.loadNext(); // load next pruning table
	            pruneTableLoaderCount++;
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
	 * Save the cube state to file system.
	 */
	public void saveCube() {
		DeprecatedRubikCube.saveCube();
	}



	/**
	 * Recall the cube state from file system and set controller state.
	 */
	public void recallCube() {
		
		DeprecatedRubikCube.recallCube();
		
		if(DeprecatedRubikCube.isThereAfullSetOfFaces() == true)
			if(DeprecatedRubikCube.isTileColorsValid() == true)
				controllerState = ControllerStateEnum.COMPLETE;
			else
				controllerState = ControllerStateEnum.BAD_COLORS;
		else
			controllerState = ControllerStateEnum.SEARCHING;
	}
	
	


}
