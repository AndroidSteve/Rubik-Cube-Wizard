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
 *   Renders user instruction graphics; in particular wide white arrows to rotate entire
 *   cube or narrower colored arrows to rotate cube edges on a GL Surface.
 *   
 *   Also renders the "Pilot Cube" which appears on the right hand side in
 *   normal mode.  It tracks rotation of the cube, but not translation.
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
import org.ar.rubik.CubeReconstructor;
import org.ar.rubik.MenuAndParams;
import org.ar.rubik.Constants.FaceNameEnum;
import org.ar.rubik.StateModel;
import org.ar.rubik.gl.GLArrow.Amount;
import org.opencv.core.Scalar;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;


/**
 *  OpenGL Custom renderer used with GLSurfaceView 
 */
public class GLRenderer2 implements GLSurfaceView.Renderer {
	
	// Requested Rotation Type
	public enum Rotation { CLOCKWISE, COUNTER_CLOCKWISE, ONE_HUNDRED_EIGHTY };
	
	// Specify direction of arrow.
	public enum Direction { POSITIVE, NEGATIVE };

	// Main State Model
	private StateModel stateModel;
	
	// GL Objects that can be rendered
	private GLArrow arrowQuarterTurn;
	private GLArrow arrowHalfTurn;
	private GLCube2  overlayGLCube;
    private GLCube2  pilotGLCube;


    // Projection Matrix:  basically defines a Frustum 
    private final float[] mProjectionMatrix = new float[16];


	/**
	 * Constructor with global application stateModel
	 * 
	 * @param stateModel
	 */
	public GLRenderer2(StateModel stateModel) {
		
		this.stateModel = stateModel;
	}


	/**
	 * Call back when the surface is first created or re-created
	 * 
	 *  (non-Javadoc)
	 * @see android.opengl.GLSurfaceView.Renderer#onSurfaceCreated(javax.microedition.khronos.opengles.GL10, javax.microedition.khronos.egl.EGLConfig)
	 */
	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {

	    GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

	    // Create the GL pilot cube
	    pilotGLCube = new GLCube2();

	    // Create the GL overlay cube
	    overlayGLCube = new GLCube2();
//      
//      // Create two arrows: one half turn, one quarter turn.
//      arrowQuarterTurn = new GLArrow(Amount.QUARTER_TURN);
//      arrowHalfTurn = new GLArrow(Amount.HALF_TURN);
	    
	    GLES20.glEnable(GLES20.GL_CULL_FACE);
	}


	/**
	 * Call back after onSurfaceCreated() or whenever the window's size changes
	 *  (non-Javadoc)
	 *  
	 * @see android.opengl.GLSurfaceView.Renderer#onSurfaceChanged(javax.microedition.khronos.opengles.GL10, int, int)
	 */
	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
	    
	       if (height == 0) height = 1;   // To prevent divide by zero

	        // Adjust the viewport based on geometry changes
	        // such as screen rotations
	        GLES20.glViewport(0, 0, width, height);
	        
	        // Calculate screen aspect ratio.  Should be the same as cameras
	        float ratio = (float) width / height;

	        // this projection matrix is applied to object coordinates
	        // in the onDrawFrame() method
	        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 2, 100);
	}



	/**
	 * On Draw Frame
	 * 
	 *  (non-Javadoc)
	 * @see android.opengl.GLSurfaceView.Renderer#onDrawFrame(javax.microedition.khronos.opengles.GL10)
	 */
	@Override
	public void onDrawFrame(GL10 unused) {

	    // View Matrix
        final float[] viewMatrix = new float[16];
        
        // Projection View Matrix
        final float[] pvMatrix   = new float[16];
        
        // Model View Projection Matrix
	    final float[] mvpMatrix  = new float[16];
	    
	    // Temporary Matrix to facilitate in-place multiplies
	    final float[] tmpMatrix  = new float[16];

	    // Draw background color
	    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
	    
	    // Make copy reference to Cube Reconstructor.
        // This is to avoid asynchronous OpenGL and OpenCV problems. 
        CubeReconstructor myCubeReconstructor = stateModel.cubeReconstructor;

        // Check and if null don't render.
        if(myCubeReconstructor == null)
            return;

	    // Set the camera position (View matrix)
        Matrix.setLookAtM(viewMatrix, 0,
                0,    0,    0,    // Camera Location
                0f,   0f,  -1f,   // Camera points down Z axis.
                0f, 1.0f, 0.0f);  // Specifies rotation of camera: in this case, standard upwards orientation.

	    // Calculate the projection and view transformation
	    Matrix.multiplyMM(pvMatrix, 0, mProjectionMatrix, 0, viewMatrix, 0);
	    
	    
	    
	    // Render User Instruction Arrows and possibly Overlay Cube
        if( (MenuAndParams.cubeOverlayDisplay == true)  ) {
            
            // Translate Cube per Pose Estimator
            float [] tranMatrix = new float[16];
            Matrix.setIdentityM(tranMatrix, 0);
            Matrix.translateM(tranMatrix, 0, 
                  myCubeReconstructor.x, 
                  myCubeReconstructor.y, 
                  myCubeReconstructor.z);
            Matrix.multiplyMM(mvpMatrix, 0, pvMatrix, 0, tranMatrix, 0);

            // Rotation Cube per Pose Estimator 
            Matrix.multiplyMM(tmpMatrix, 0, mvpMatrix, 0, myCubeReconstructor.rotationMatrix, 0);
            System.arraycopy(tmpMatrix, 0, mvpMatrix, 0, tmpMatrix.length);
            
            // Rotation Cube per additional requests 
//            Matrix.multiplyMM(mvpMatrix, 0, mvpMatrix, 0, stateModel.additionalGLCubeRotation, 0);

            // Scale
            // =+= I believe the need for this has something to do with the difference between camera and screen dimensions.
            float scale = (float) MenuAndParams.scaleOffsetParam.value;
            float [] scaleMatrix = new float[16];
            Matrix.setIdentityM(scaleMatrix, 0);
            Matrix.scaleM(scaleMatrix, 0, scale, scale, scale);
            Matrix.multiplyMM(tmpMatrix, 0, mvpMatrix, 0, scaleMatrix, 0);
            System.arraycopy(tmpMatrix, 0, mvpMatrix, 0, tmpMatrix.length);

            // If desire, render what we think is the cube location and orientation.
            if(MenuAndParams.cubeOverlayDisplay == true)
                overlayGLCube.draw(mvpMatrix, true);
        }

	    
	    // Render Pilot Cube
        if(MenuAndParams.pilotCubeDisplay == true && stateModel.renderPilotCube == true) {

            // Instead of using pose esitmator coordinates, instead position cube at
            // fix location.  We really just desire to observe rotation.
            float [] tranMatrix = new float[16];
            Matrix.setIdentityM(tranMatrix, 0);
            Matrix.translateM(tranMatrix, 0, -4.0f, 0.0f, -10.0f);
            Matrix.multiplyMM(mvpMatrix, 0, pvMatrix, 0, tranMatrix, 0);

            // Rotation Cube per Pose Estimator 
            Matrix.multiplyMM(tmpMatrix, 0, mvpMatrix, 0, myCubeReconstructor.rotationMatrix, 0);
            System.arraycopy(tmpMatrix, 0, mvpMatrix, 0, tmpMatrix.length);

            // Rotation Cube per additional requests 
//            Matrix.multiplyMM(mvpMatrix, 0, mvpMatrix, 0, stateModel.additionalGLCubeRotation, 0);
            
            pilotGLCube.draw(mvpMatrix, false);
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

		Rotation rotation;
		Amount amount;
		
		if(moveNumonic.length() == 1)  {
			rotation = Rotation.CLOCKWISE;
			amount = Amount.QUARTER_TURN;
		}
		else if(moveNumonic.charAt(1) == '2') {
			rotation = Rotation.ONE_HUNDRED_EIGHTY;
			amount = Amount.HALF_TURN;
		}
		else if(moveNumonic.charAt(1) == '\'') {
			rotation = Rotation.COUNTER_CLOCKWISE;
			amount = Amount.QUARTER_TURN;
		}
		else
			throw new java.lang.Error("Unknow rotation amount");
		
		
		Scalar color = null;
		Direction direction = null;
		
		// Rotate and Translate Arrow as required by Rubik Logic Solution algorithm. 
		switch(moveNumonic.charAt(0)) {
		case 'U':
			color = stateModel.getFaceByName(FaceNameEnum.UP).observedTileArray[1][1].color;
			gl.glTranslatef(0.0f, +2.0f, 0.0f);
			gl.glRotatef(90f, 1.0f, 0.0f, 0.0f);  // X rotation
			direction = (rotation == Rotation.CLOCKWISE) ?         Direction.NEGATIVE : Direction.POSITIVE; 
			break;
		case 'D':
			color = stateModel.getFaceByName(FaceNameEnum.DOWN).observedTileArray[1][1].color;		
			gl.glTranslatef(0.0f, -2.0f, 0.0f);
			gl.glRotatef(90f, 1.0f, 0.0f, 0.0f);  // X rotation
			direction = (rotation == Rotation.COUNTER_CLOCKWISE) ? Direction.NEGATIVE : Direction.POSITIVE; 
			break;
		case 'L':
			color = stateModel.getFaceByName(FaceNameEnum.LEFT).observedTileArray[1][1].color;
			gl.glTranslatef(-2.0f, 0.0f, 0.0f);
			gl.glRotatef(90f, 0.0f, 1.0f, 0.0f);  // Y rotation
			direction = (rotation == Rotation.CLOCKWISE) ?         Direction.NEGATIVE : Direction.POSITIVE; 
			gl.glRotatef(30f, 0.0f, 0.0f, 1.0f);  // looks better
			break;
		case 'R':
			color = stateModel.getFaceByName(FaceNameEnum.RIGHT).observedTileArray[1][1].color;
			gl.glTranslatef(+2.0f, 0.0f, 0.0f);
			gl.glRotatef(90f, 0.0f, 1.0f, 0.0f);  // Y rotation
			direction = (rotation == Rotation.COUNTER_CLOCKWISE) ? Direction.NEGATIVE : Direction.POSITIVE;
			gl.glRotatef(30f, 0.0f, 0.0f, 1.0f);  // looks better
			break;
		case 'F':
			color = stateModel.getFaceByName(FaceNameEnum.FRONT).observedTileArray[1][1].color;
			gl.glTranslatef(0.0f, 0.0f, +2.0f);
			direction = (rotation == Rotation.COUNTER_CLOCKWISE) ? Direction.NEGATIVE : Direction.POSITIVE; 
			gl.glRotatef(30f, 0.0f, 0.0f, 1.0f);  // looks better
			break;
		case 'B':
			color = stateModel.getFaceByName(FaceNameEnum.BACK).observedTileArray[1][1].color;
			gl.glTranslatef(0.0f, 0.0f, -2.0f);
			direction = (rotation == Rotation.CLOCKWISE) ?         Direction.NEGATIVE : Direction.POSITIVE; 
			gl.glRotatef(30f, 0.0f, 0.0f, 1.0f);  // looks better
			break;
		}

		// Specify direction of arrow
		if(direction == Direction.NEGATIVE)  {
			gl.glRotatef(-90f,  0.0f, 0.0f, 1.0f);  // Z rotation of -90
			gl.glRotatef(+180f, 0.0f, 1.0f, 0.0f);  // Y rotation of +180
		}
		
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
		if(stateModel.getNumObservedFaces() % 2 == 0) {
			gl.glTranslatef(0.0f, +1.5f, +1.5f);
			gl.glRotatef(-90f, 0.0f, 1.0f, 0.0f);  // Y rotation of -90
		}
		
		// Render Right Face to Top Face Arrow Rotation
		else {
			gl.glTranslatef(+1.5f, +1.5f, 0.0f);
		}
		
		
		// Reverse direction of arrow.
		gl.glRotatef(-90f,  0.0f, 0.0f, 1.0f);  // Z rotation of -90
		gl.glRotatef(+180f, 0.0f, 1.0f, 0.0f);  // Y rotation of +180

		// Make Arrow Wider than normal by a factor of three.
		gl.glScalef(1.0f, 1.0f, 3.0f);
		
		// Render Quarter Turn Arrow
		arrowQuarterTurn.draw(gl, Constants.ColorWhite);
	}

}
