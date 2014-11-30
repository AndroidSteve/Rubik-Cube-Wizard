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
package org.ar.rubik.gl;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.ar.rubik.Constants;
import org.ar.rubik.Constants.AppStateEnum;
import org.ar.rubik.StateModel;
import org.ar.rubik.gl.GLArrow.Amount;
import org.opencv.core.Scalar;

import android.opengl.GLSurfaceView;
import android.opengl.GLU;


/**
 *  OpenGL Custom renderer used with GLSurfaceView 
 */
public class UserInstructionsGLRenderer implements GLSurfaceView.Renderer {

	public enum FaceType { UP, DOWN, LEFT, RIGHT, FRONT, BACK, FRONT_TOP, LEFT_TOP };
	
	public enum Rotation { CLOCKWISE, COUNTER_CLOCKWISE, ONE_HUNDRED_EIGHTY };
	
	public enum Size { NARROW, WIDE };
	
	// Basically clockwise, or counter clockwise as to original rendering of arrow on XY plane.
	public enum Direction { POSITIVE, NEGATIVE };

	private StateModel stateModel;
	
	// Control Flags
	private boolean renderCubeOverlay   = true;
//	private boolean renderArrow  = false;
	
	// GL Object that can be rendered
	private GLArrow arrowQuarterTurn;
	private GLArrow arrowHalfTurn;
	private OverlayGLCube overlayGLCube;
	
	// Arrow Rendering Data
	private Direction direction  = Direction.POSITIVE;
//	private Size size            = Size.NARROW;
	private Amount amount        = Amount.HALF_TURN;
	private Scalar color         = Constants.ColorGrey;
	private FaceType faceType    = null;

	private Rotation rotation;



	/**
	 * Constructor with global application stateModel
	 * @param stateModel
	 */
	public UserInstructionsGLRenderer(StateModel stateModel) {
		
		this.stateModel = stateModel;
		
		overlayGLCube = new OverlayGLCube();
		
		arrowQuarterTurn = new GLArrow(Amount.QUARTER_TURN);
		arrowHalfTurn = new GLArrow(Amount.HALF_TURN);
	}


	/**
	 * Call back when the surface is first created or re-created
	 *  (non-Javadoc)
	 * @see android.opengl.GLSurfaceView.Renderer#onSurfaceCreated(javax.microedition.khronos.opengles.GL10, javax.microedition.khronos.egl.EGLConfig)
	 */
	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {

		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);  // Set color's clear-value to black and transparent.
		gl.glClearDepthf(1.0f);            // Set depth's clear-value to farthest
		gl.glEnable(GL10.GL_DEPTH_TEST);   // Enables depth-buffer for hidden surface removal
		gl.glDepthFunc(GL10.GL_LEQUAL);    // The type of depth testing to do
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);  // nice perspective view
		gl.glShadeModel(GL10.GL_SMOOTH);   // Enable smooth shading of color
		gl.glDisable(GL10.GL_DITHER);      // Disable dithering for better performance
	}


	/**
	 * Call back after onSurfaceCreated() or whenever the window's size changes
	 *  (non-Javadoc)
	 * @see android.opengl.GLSurfaceView.Renderer#onSurfaceChanged(javax.microedition.khronos.opengles.GL10, int, int)
	 */
	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {

		if (height == 0) height = 1;   // To prevent divide by zero
		float aspect = (float)width / height;

		// Set the viewport (display area) to cover the entire window
		gl.glViewport(0, 0, width, height);

		// Setup perspective projection, with aspect ratio matches viewport
		gl.glMatrixMode(GL10.GL_PROJECTION); // Select projection matrix
		gl.glLoadIdentity();                 // Reset projection matrix
		
		// Use perspective projection
		GLU.gluPerspective(gl, 45, aspect, 0.1f, 100.f);

		gl.glMatrixMode(GL10.GL_MODELVIEW);  // Select model-view matrix =+=
		gl.glLoadIdentity();                 // Reset
	}
	
	
	
	/**
	 * @param gl
	 */
	public void onDrawFrame(GL10 gl) {
		
		// Clear color and depth buffers using clear-value set earlier
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		
		if(stateModel.appState != AppStateEnum.ROTATE && stateModel.appState != AppStateEnum.DO_MOVE)
			return;

		if(stateModel.cubeReconstructor == null)
			return;
		
		// Perform general scene translation.
		// This is based on the reconstructed 3D cube position and orientation.
		float scale = stateModel.cubeReconstructor.scale;
		float x = stateModel.cubeReconstructor.x;
		float y = stateModel.cubeReconstructor.y;
		float cubeXrotation = stateModel.cubeReconstructor.cubeXrotation;
		float cubeYrotation = stateModel.cubeReconstructor.cubeYrotation;
		
		gl.glLoadIdentity();                   // Reset model-view matrix 
		
		// Perspective Translate
		// =+= really, we should just put scale in z-translation param.
		gl.glTranslatef(x, y, -10.0f);
		gl.glScalef(scale, scale, scale);

		// Cube Rotation
		gl.glRotatef(cubeXrotation, 1.0f, 0.0f, 0.0f);  // X rotation of ~35
		gl.glRotatef(cubeYrotation, 0.0f, 1.0f, 0.0f);  // Y rotation of ~45
		
		// If desire, render what we think is the cube location and orientation.
		if(renderCubeOverlay == true)
			overlayGLCube.draw(gl);
		
		
		// Render either Entire Cube Rotation arrow or Cube Edge Rotation arrow.
		switch(stateModel.appState) {
		
		case ROTATE:
			renderCubeFullRotationArrow(gl);
			break;

		case DO_MOVE:
			renderCubeEdgeRotationArrow(gl);
			break;
			
		default:
			break;
		}
	}


	/**
	 * Render Cube Edge Rotation Arrow
	 * 
	 * Render an Rubik Cube Edge Rotation request/instruction.
	 * This is used after a solution has been computed and to instruct 
	 * the user to rotate one edge at a time.
	 * 
	 * @param gl
	 */
	private void renderCubeEdgeRotationArrow(GL10 gl) {
		
		String moveNumonic = stateModel.solutionResultsArray[stateModel.solutionResultIndex];

		if(moveNumonic.length() == 1)  {
			rotation = Rotation.CLOCKWISE;
			this.amount = Amount.QUARTER_TURN;
		}
		else if(moveNumonic.charAt(1) == '2') {
			rotation = Rotation.ONE_HUNDRED_EIGHTY;
			this.amount = Amount.HALF_TURN;
		}
		else if(moveNumonic.charAt(1) == '\'') {
			rotation = Rotation.COUNTER_CLOCKWISE;
			this.amount = Amount.QUARTER_TURN;
		}
		else
			throw new java.lang.Error("Unknow rotation amount");
		
		// Obtain details of arrow to be rendered.
		// TO DO:
		// - Get color from cube state
		// - Condense this switch with next switch: eliminate face type enum
		switch(moveNumonic.charAt(0)) {
		case 'U': 
			faceType = FaceType.UP;
			color = stateModel.upRubikFace.observedTileArray[1][1].color;
			break;
		case 'D': 
			faceType = FaceType.DOWN;
			color = stateModel.downRubikFace.observedTileArray[1][1].color;
			break;
		case 'L': 
			faceType = FaceType.LEFT;
			color = stateModel.leftRubikFace.observedTileArray[1][1].color;
			break;
		case 'R': 
			faceType = FaceType.RIGHT;
			color = stateModel.rightRubikFace.observedTileArray[1][1].color;
			break;
		case 'F': 
			faceType = FaceType.FRONT;
			color = stateModel.frontRubikFace.observedTileArray[1][1].color;
			break;
		case 'B':
			faceType = FaceType.BACK;
			color = stateModel.backRubikFace.observedTileArray[1][1].color;
			break;
		}


		// Specify location and orientation relative to cube of arrow
		switch(faceType) {
		
		case FRONT:
			gl.glTranslatef(0.0f, 0.0f, +2.0f);
			direction = rotation == Rotation.COUNTER_CLOCKWISE ? Direction.NEGATIVE : Direction.POSITIVE; 
			gl.glRotatef(30f, 0.0f, 0.0f, 1.0f);  // looks better
			break;
		case BACK:
			gl.glTranslatef(0.0f, 0.0f, -2.0f);
			direction = rotation == Rotation.CLOCKWISE ?         Direction.NEGATIVE : Direction.POSITIVE; 
			gl.glRotatef(30f, 0.0f, 0.0f, 1.0f);  // looks better
			break;

		case UP:
			gl.glTranslatef(0.0f, +2.0f, 0.0f);
			gl.glRotatef(90f, 1.0f, 0.0f, 0.0f);  // X rotation
			direction = rotation == Rotation.CLOCKWISE ?         Direction.NEGATIVE : Direction.POSITIVE; 
			break;
		case DOWN:			
			gl.glTranslatef(0.0f, -2.0f, 0.0f);
			gl.glRotatef(90f, 1.0f, 0.0f, 0.0f);  // X rotation
			direction = rotation == Rotation.COUNTER_CLOCKWISE ? Direction.NEGATIVE : Direction.POSITIVE; 
			break;
			
		case LEFT:
			gl.glTranslatef(-2.0f, 0.0f, 0.0f);
			gl.glRotatef(90f, 0.0f, 1.0f, 0.0f);  // Y rotation
			direction = rotation == Rotation.CLOCKWISE ?         Direction.NEGATIVE : Direction.POSITIVE; 
			gl.glRotatef(30f, 0.0f, 0.0f, 1.0f);  // looks better
			break;
		case RIGHT:
			gl.glTranslatef(+2.0f, 0.0f, 0.0f);
			gl.glRotatef(90f, 0.0f, 1.0f, 0.0f);  // Y rotation
			direction = rotation == Rotation.COUNTER_CLOCKWISE ? Direction.NEGATIVE : Direction.POSITIVE;
			gl.glRotatef(30f, 0.0f, 0.0f, 1.0f);  // looks better
			break;
		}
		
		// Specify direction of arrow
		if(direction == Direction.NEGATIVE)  {
			gl.glRotatef(-90f,  0.0f, 0.0f, 1.0f);  // Z rotation of -90
			gl.glRotatef(+180f, 0.0f, 1.0f, 0.0f);  // Y rotation of +180
		}
		
//		// Specify width of arrow
//		if(size == Size.WIDE)  {
//			gl.glScalef(1.0f, 1.0f, 3.0f);
//		}
		
		if(amount == Amount.QUARTER_TURN)
			arrowQuarterTurn.draw(gl, color);
		else
			arrowHalfTurn.draw(gl, color);
	}


	/**
	 * Render Cube Full Rotation Arrow
	 * 
	 * Render a Rubik Cube Body Rotation request/instruction.
	 * This is used during the exploration phase to observed all six
	 * sides of the cube before any solution is compute or attempted.
	 * 
	 * @param gl
	 */
	private void renderCubeFullRotationArrow(GL10 gl) {		
		
		// Render Front Face to Top Face Arrow Rotation
		if(stateModel.getNumObservedFaces() % 2 != 0) {
			gl.glTranslatef(0.0f, +1.5f, +1.5f);
			gl.glRotatef(-90f, 0.0f, 1.0f, 0.0f);  // Y rotation of -90
			gl.glRotatef(30f, 0.0f, 0.0f, 1.0f);  // looks better		
		}
		
		// Render Left Face to Top Face Arrow Rotation
		else {
			gl.glTranslatef(-1.5f, +1.5f, 0.0f);
			gl.glRotatef(180f, 0.0f, 1.0f, 0.0f);  // Y rotation of 180
			gl.glRotatef(30f, 0.0f, 0.0f, 1.0f);  // looks better
		}
		
		
		// Reverse direction of arrow.
		gl.glRotatef(-90f,  0.0f, 0.0f, 1.0f);  // Z rotation of -90
		gl.glRotatef(+180f, 0.0f, 1.0f, 0.0f);  // Y rotation of +180

		// Make Arrow Wide
		gl.glScalef(1.0f, 1.0f, 3.0f);
		
		// Render Quarter Turn Arrow
		arrowQuarterTurn.draw(gl, Constants.ColorWhite);
	}


//	/**
//	 * Call back to draw the current frame.
//	 * 
//	 *  (non-Javadoc)
//	 * @see android.opengl.GLSurfaceView.Renderer#onDrawFrame(javax.microedition.khronos.opengles.GL10)
//	 */
////	@Override
//	public void onDrawFramexxx(GL10 gl) {
//		
//		// Clear color and depth buffers using clear-value set earlier
//		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
//		
//		if(stateModel.cubeReconstructor == null)
//			return;
//		
//		float scale = stateModel.cubeReconstructor.scale;
//		float x = stateModel.cubeReconstructor.x;
//		float y = stateModel.cubeReconstructor.y;
//		float cubeXrotation = stateModel.cubeReconstructor.cubeXrotation;
//		float cubeYrotation = stateModel.cubeReconstructor.cubeYrotation;
//		
//////		x = 0;
//////		y = 0;
////		faceType = FaceType.RIGHT;
////		rotation = Rotation.CLOCKWISE;
////		amount = Amount.QUARTER_TURN;
////		size = Size.NARROW;
////		color = Constants.ColorRed;
////		renderArrow = true;
////		renderCube = true;
//////		scale = 2.5f;
//////		cubeYrotation = 0f;
//////		cubeXrotation =0f;
//		
//
//		
//		if(renderCubeOverlay == false && renderArrow == false)
//			return;
//		
//		gl.glLoadIdentity();                   // Reset model-view matrix 
//		
//		// Perspective Translate
//		gl.glTranslatef(x, y, -10.0f);
//		gl.glScalef(scale, scale, scale);
//
//		
//		// Cube Rotation
//		gl.glRotatef(cubeXrotation, 1.0f, 0.0f, 0.0f);  // X rotation of ~35
//		gl.glRotatef(cubeYrotation, 0.0f, 1.0f, 0.0f);  // Y rotation of ~45
//		
//		if(renderCubeOverlay == true)
//			overlayGLCube.draw(gl);
//		
//		
//		if(renderArrow == false)
//			return;
//		
//		// Specify location and orienation relative to cube of arrow
//		switch(faceType) {
//		
//		case FRONT:
//			gl.glTranslatef(0.0f, 0.0f, +2.0f);
//			direction = rotation == Rotation.COUNTER_CLOCKWISE ? Direction.NEGATIVE : Direction.POSITIVE; 
//			gl.glRotatef(30f, 0.0f, 0.0f, 1.0f);  // looks better
//			break;
//		case BACK:
//			gl.glTranslatef(0.0f, 0.0f, -2.0f);
//			direction = rotation == Rotation.CLOCKWISE ?         Direction.NEGATIVE : Direction.POSITIVE; 
//			gl.glRotatef(30f, 0.0f, 0.0f, 1.0f);  // looks better
//			break;
//
//		case UP:
//			gl.glTranslatef(0.0f, +2.0f, 0.0f);
//			gl.glRotatef(90f, 1.0f, 0.0f, 0.0f);  // X rotation
//			direction = rotation == Rotation.CLOCKWISE ?         Direction.NEGATIVE : Direction.POSITIVE; 
//			break;
//		case DOWN:			
//			gl.glTranslatef(0.0f, -2.0f, 0.0f);
//			gl.glRotatef(90f, 1.0f, 0.0f, 0.0f);  // X rotation
//			direction = rotation == Rotation.COUNTER_CLOCKWISE ? Direction.NEGATIVE : Direction.POSITIVE; 
//			break;
//			
//		case LEFT:
//			gl.glTranslatef(-2.0f, 0.0f, 0.0f);
//			gl.glRotatef(90f, 0.0f, 1.0f, 0.0f);  // Y rotation
//			direction = rotation == Rotation.CLOCKWISE ?         Direction.NEGATIVE : Direction.POSITIVE; 
//			gl.glRotatef(30f, 0.0f, 0.0f, 1.0f);  // looks better
//			break;
//		case RIGHT:
//			gl.glTranslatef(+2.0f, 0.0f, 0.0f);
//			gl.glRotatef(90f, 0.0f, 1.0f, 0.0f);  // Y rotation
//			direction = rotation == Rotation.COUNTER_CLOCKWISE ? Direction.NEGATIVE : Direction.POSITIVE;
//			gl.glRotatef(30f, 0.0f, 0.0f, 1.0f);  // looks better
//			break;
//			
//		case FRONT_TOP:
//			gl.glTranslatef(0.0f, +1.5f, +1.5f);
//			gl.glRotatef(-90f, 0.0f, 1.0f, 0.0f);  // Y rotation of -90
//			direction = Direction.NEGATIVE;
//			gl.glRotatef(30f, 0.0f, 0.0f, 1.0f);  // looks better
//			break;
//		case LEFT_TOP:
//			gl.glTranslatef(-1.5f, +1.5f, 0.0f);
//			gl.glRotatef(180f, 0.0f, 1.0f, 0.0f);  // Y rotation of 180
//			direction = Direction.NEGATIVE;
//			gl.glRotatef(30f, 0.0f, 0.0f, 1.0f);  // looks better
//			break;
//		}
//		
//		// Specify direction of arrow
//		if(direction == Direction.NEGATIVE)  {
//			gl.glRotatef(-90f,  0.0f, 0.0f, 1.0f);  // Z rotation of -90
//			gl.glRotatef(+180f, 0.0f, 1.0f, 0.0f);  // Y rotation of +180
//		}
//		
//		// Specify width of arrow
//		if(size == Size.WIDE)  {
//			gl.glScalef(1.0f, 1.0f, 3.0f);
//		}
//		
//		if(amount == Amount.QUARTER_TURN)
//			arrowQuarterTurn.draw(gl, color);
//		else
//			arrowHalfTurn.draw(gl, color);
//	}


	public void setRenderArrow(boolean state) {
//		renderArrow = state;
	}
	
	public void setRenderCube(boolean state) {
//		renderCubeOverlay = state;
	}
	


	/**
	 * Show Cube Edge Rotation Arrow
	 * 
	 * @param origin
	 * @param angle
	 * @param direction
	 * @param range
	 */
	public void showCubeEdgeRotationArrow(Rotation rotation, FaceType faceType, Scalar color) {
//		renderArrow = true;
//		this.rotation = rotation;
//		this.size = Size.NARROW;
//		this.faceType = faceType;
//		this.color = color;
//		
//		switch(rotation) {
//		case CLOCKWISE:
//			this.amount = Amount.QUARTER_TURN;
//			break;
//		case COUNTER_CLOCKWISE:
//			this.amount = Amount.QUARTER_TURN;
//			break;
//		case ONE_HUNDRED_EIGHTY:
//			this.amount = Amount.HALF_TURN;
//			break;
//		}
	}

	
	/**
	 * Show Full CUbe Rotation Arrow
	 * 
	 * 
	 * @param origin
	 * @param angle
	 */
	public void showFullCubeRotateArrow(FaceType faceType) {
//		renderArrow = true;
//		this.faceType = faceType;
//		this.size = Size.WIDE;
//		this.amount = Amount.QUARTER_TURN;
//		this.color = Constants.ColorWhite;
	}
}
