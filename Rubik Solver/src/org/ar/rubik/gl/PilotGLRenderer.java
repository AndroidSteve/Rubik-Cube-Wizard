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
import org.ar.rubik.Lms;
import org.ar.rubik.DeprecatedRubikFace;
import org.ar.rubik.DeprecatedRubikFace.FaceRecognitionStatusEnum;
import org.ar.rubik.gl.GLArrow.Amount;
import org.opencv.core.Scalar;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;


/**
 *  OpenGL Custom renderer used with GLSurfaceView 
 */
public class PilotGLRenderer implements GLSurfaceView.Renderer {

	public enum FaceType { UP, DOWN, LEFT, RIGHT, FRONT, BACK, FRONT_TOP, LEFT_TOP };
	
	public enum Rotation { CLOCKWISE, COUNTER_CLOCKWISE, ONE_HUNDRED_EIGHTY };
	
	public enum Size { NARROW, WIDE };
	
	// Basically clockwise, or counter clockwise as to original rendering of arrow on XY plane.
	public enum Direction { POSITIVE, NEGATIVE };

	@SuppressWarnings("unused")
	private Context context;   // Application's context
	
	// Control Flags
	private boolean renderCube   = false;
	private boolean renderArrow  = false;
	
	// GL Object that can be rendered
	private GLArrow arrowQuarterTurn;
	private GLArrow arrowHalfTurn;
	private OverlayGLCube overlayGLCube;
	
	// Arrow Rendering Data
	private Direction direction  = Direction.POSITIVE;
	private Size size            = Size.NARROW;
	private Amount amount        = Amount.HALF_TURN;
	private Scalar color         = Constants.ColorGrey;
	private FaceType faceType    = null;

	// Cube (and really scene) rendering information
	private float scale;
	private float x;
	private float y;
	private float cubeXrotation = +45f;
	private float cubeYrotation = +55f;

	private Rotation rotation;



	/**
	 * Constructor with global application context
	 * @param context
	 */
	public PilotGLRenderer(Context context) {
		this.context = context;
		
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
	 * Call back to draw the current frame.
	 * 
	 *  (non-Javadoc)
	 * @see android.opengl.GLSurfaceView.Renderer#onDrawFrame(javax.microedition.khronos.opengles.GL10)
	 */
	@Override
	public void onDrawFrame(GL10 gl) {
		
//		x = 0;
//		y = 0;
//		faceType = FaceType.RIGHT;
//		rotation = Rotation.CLOCKWISE;
//		amount = Amount.QUARTER_TURN;
//		size = Size.NARROW;
//		color = Constants.ColorRed;
//		renderArrow = true;
//		scale = 2.5f;
//		cubeYrotation = 0f;
//		cubeXrotation =0f;
		
		
		// Clear color and depth buffers using clear-value set earlier
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		
		if(renderCube == false && renderArrow == false)
			return;
		
		gl.glLoadIdentity();                   // Reset model-view matrix 
		
		// Perspective Translate
		gl.glTranslatef(x, y, -10.0f);
		gl.glScalef(scale, scale, scale);

		
		// Cube Rotation
		gl.glRotatef(cubeXrotation, 1.0f, 0.0f, 0.0f);  // X rotation of ~35
		gl.glRotatef(cubeYrotation, 0.0f, 1.0f, 0.0f);  // Y rotation of ~45
		
		if(renderCube == true)
			overlayGLCube.draw(gl);
		
		
		if(renderArrow == false)
			return;
		
		// Specify location and orienation relative to cube of arrow
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
			
		case FRONT_TOP:
			gl.glTranslatef(0.0f, +1.5f, +1.5f);
			gl.glRotatef(-90f, 0.0f, 1.0f, 0.0f);  // Y rotation of -90
			direction = Direction.NEGATIVE;
			gl.glRotatef(30f, 0.0f, 0.0f, 1.0f);  // looks better
			break;
		case LEFT_TOP:
			gl.glTranslatef(-1.5f, +1.5f, 0.0f);
			gl.glRotatef(180f, 0.0f, 1.0f, 0.0f);  // Y rotation of 180
			direction = Direction.NEGATIVE;
			gl.glRotatef(30f, 0.0f, 0.0f, 1.0f);  // looks better
			break;
		}
		
		// Specify direction of arrow
		if(direction == Direction.NEGATIVE)  {
			gl.glRotatef(-90f,  0.0f, 0.0f, 1.0f);  // Z rotation of -90
			gl.glRotatef(+180f, 0.0f, 1.0f, 0.0f);  // Y rotation of +180
		}
		
		// Specify width of arrow
		if(size == Size.WIDE)  {
			gl.glScalef(1.0f, 1.0f, 3.0f);
		}
		
		if(amount == Amount.QUARTER_TURN)
			arrowQuarterTurn.draw(gl, color);
		else
			arrowHalfTurn.draw(gl, color);
	}


	public void setRenderArrow(boolean state) {
		renderArrow = state;
	}
	
	public void setRenderCube(boolean state) {
		renderCube = state;
	}
	
	/**
	 * Set Cube Orientation
	 * 
	 * This function actually calculates, currently rather crudely, a 2D to 3D translation.
	 * That is, information from the Rubik Face object is used to deduce the 
	 * true location in OpenGL space of the cube and it's orientation.  
	 * 
	 * 
	 * @param rubikFace
	 */
	public void setCubeOrienation(DeprecatedRubikFace rubikFace) {
		
		final float opecnCL2opencvRatio = 100.0f;
		final float xOffset = 650.0f;
		final float yOffset = 200.0f;
		
		if(rubikFace == null)
			return;
		
		if(rubikFace.faceRecognitionStatus != FaceRecognitionStatusEnum.SOLVED)
			return;
		
		Lms lmsResult = rubikFace.lmsResult;
		
		if(lmsResult == null)
			return;
		
				
		// This is very crude.
		this.scale = (float) Math.sqrt(Math.abs(rubikFace.alphaLatticLength * rubikFace.betaLatticLength)) / 70.0f;
		
		// =+= not necessarily correct, really should use X, Y rotations
		x = (float) ((lmsResult.origin.x - xOffset) / opecnCL2opencvRatio);
		y = (float) (-1 * (lmsResult.origin.y - yOffset) / opecnCL2opencvRatio);
		
		float alpha = 90.0f - (float) (rubikFace.alphaAngle * 180.0 / Math.PI);
		float beta = (float) (rubikFace.betaAngle * 180.0 / Math.PI) - 90.0f;
		
		
		// Very crude estimations of orientation.  These equations and number found empirically.
		// =+= We require a solution of two non-linear equations and two unknowns to correctly calculate
		// =+= X and Y 3D rotation values from 2D alpha and beta values.  Probably use of Newton successive
		// =+= approximation will produce good results.
		cubeYrotation = 45.0f + (alpha - beta) / 2.0f;
		cubeXrotation =  90.0f + ( (alpha - 45.0f) + (beta - 45.0f) )/ -0.5f;
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
		renderArrow = true;
		this.rotation = rotation;
		this.size = Size.NARROW;
		this.faceType = faceType;
		this.color = color;
		
		switch(rotation) {
		case CLOCKWISE:
			this.amount = Amount.QUARTER_TURN;
			break;
		case COUNTER_CLOCKWISE:
			this.amount = Amount.QUARTER_TURN;
			break;
		case ONE_HUNDRED_EIGHTY:
			this.amount = Amount.HALF_TURN;
			break;
		}
	}

	
	/**
	 * Show Full CUbe Rotation Arrow
	 * 
	 * 
	 * @param origin
	 * @param angle
	 */
	public void showFullCubeRotateArrow(FaceType faceType) {
		renderArrow = true;
		this.faceType = faceType;
		this.size = Size.WIDE;
		this.amount = Amount.QUARTER_TURN;
		this.color = Constants.ColorWhite;
	}
}
