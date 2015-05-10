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

import org.ar.rubik.Constants.AppStateEnum;
import org.ar.rubik.Constants.ColorTileEnum;
import org.ar.rubik.CubeReconstructor;
import org.ar.rubik.MenuAndParams;
import org.ar.rubik.Constants.FaceNameEnum;
import org.ar.rubik.StateModel;
import org.ar.rubik.Util;
import org.ar.rubik.gl.GLArrow2.Amount;
import org.ar.rubik.gl.GLCube2.Transparency;
import org.opencv.core.Scalar;

import android.content.Context;
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

	// Android Application Context
    private Context context;
    
    // OpenGL shader program ID
    int programID;
    
	// GL Objects that can be rendered
	private GLArrow2 arrowQuarterTurn;
	private GLArrow2 arrowHalfTurn;
	private GLCube2  overlayGLCube;
    private GLCube2  pilotGLCube;

    // Projection Matrix:  basically defines a Frustum 
    private final float[] mProjectionMatrix = new float[16];



	/**
	 * Constructor with global application stateModel
	 * 
	 * @param stateModel
	 * @param androidActivity 
	 */
	public GLRenderer2(StateModel stateModel, Context context) {
		
		this.stateModel = stateModel;
		this.context = context;
	}


	/**
	 * Call back when the surface is first created or re-created
	 * 
	 *  (non-Javadoc)
	 * @see android.opengl.GLSurfaceView.Renderer#onSurfaceCreated(javax.microedition.khronos.opengles.GL10, javax.microedition.khronos.egl.EGLConfig)
	 */
	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
	    
	    // Obtain vertex and fragment shader source text
        String vertexShaderCode = Util.readTextFileFromAssets(context, "simple_vertex_shader.glsl");
        String fragmentShaderCode = Util.readTextFileFromAssets(context, "simple_fragment_shader.glsl");
        
        // Compile shaders
        int vertexShaderID = GLUtil.compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);   
        int fragmentShaderID = GLUtil.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        
        // Link the shaders together into a final GPU executable.
        programID = GLUtil.linkProgram(vertexShaderID, fragmentShaderID);
        

        // Clear Color 
	    GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

	    // Create the GL pilot cube
	    pilotGLCube = new GLCube2(stateModel);

	    // Create the GL overlay cube
	    overlayGLCube = new GLCube2(stateModel);
	    
	    // Create two arrows: one half turn, one quarter turn.
	    arrowQuarterTurn = new GLArrow2(Amount.QUARTER_TURN);
	    arrowHalfTurn = new GLArrow2(Amount.HALF_TURN);
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
     * Possibly Render:
     *  1) An arrow to rotate the entire cube.
     *  2) An arrow to rotate an edge of the cube.
     *  3) An Overlay Cube (i.e., should be observed as exactly over the physical cube).
     *  4) An Pilot Cube off to the right, at a fixed size and location, but with rotation of the physical cube.
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
        
        // Enable Depth Testing and Occulsion
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

	    // Draw background color
	    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
	    
	    
	    // =+= DO
	    // Get Cube Pose instead of Cube Reconstructor
	    // Calculate Rodrigues here

        
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
        if( (MenuAndParams.cubeOverlayDisplay == true) || (stateModel.appState == AppStateEnum.ROTATE_CUBE) || (stateModel.appState == AppStateEnum.ROTATE_FACE) ) {
            
            System.arraycopy(pvMatrix, 0, mvpMatrix, 0, pvMatrix.length);
            
            // Translate Cube per Pose Estimator
            Matrix.translateM(mvpMatrix, 0, 
                    myCubeReconstructor.x, 
                    myCubeReconstructor.y, 
                    myCubeReconstructor.z);
            
            // Rotation Cube per Pose Estimator
            GLUtil.rotateMatrix(mvpMatrix, myCubeReconstructor.poseRotationMatrix);
            
            // Rotation Cube per additional requests 
            // =+= Don't use: screws up arrows with present code, and not really important.
            //GLUtil.rotateMatrix(mvpMatrix, stateModel.additionalGLCubeRotation);

            // Scale
            // =+= I believe the need for this has something to do with the difference between camera and screen dimensions.
            float scale = (float) MenuAndParams.scaleOffsetParam.value;
            Matrix.scaleM(mvpMatrix, 0, scale, scale, scale);

            // Render, either fully transparent or translucent, the cube in the actual position and orientation from OpenCV Pose Estimator.
            if(MenuAndParams.cubeOverlayDisplay == true)
                overlayGLCube.draw(mvpMatrix, Transparency.TRANSLUCENT, programID);
            else
                overlayGLCube.draw(mvpMatrix, Transparency.TRANSPARENT, programID);
            
            
            // Possibly Render either Entire Cube Rotation arrow or Cube Edge Rotation arrow.
            switch(stateModel.appState) {

            case ROTATE_CUBE:
                renderCubeFullRotationArrow(mvpMatrix, getRotationInDegrees());
                break;

            case ROTATE_FACE:
                renderCubeEdgeRotationArrow(mvpMatrix, getRotationInDegrees());
                break;

            default:
                break;
            }
        }

	    
	    // Render Pilot Cube
        if(MenuAndParams.pilotCubeDisplay == true && stateModel.renderPilotCube == true) {
            
            System.arraycopy(pvMatrix, 0, mvpMatrix, 0, pvMatrix.length);

            // Instead of using pose esitmator coordinates, instead position cube at
            // fix location.  We really just desire to observe rotation.
            Matrix.translateM(mvpMatrix, 0, -6.0f, 0.0f, -10.0f);

            // Rotation Cube per Pose Estimator 
            GLUtil.rotateMatrix(mvpMatrix, myCubeReconstructor.poseRotationMatrix);

            // Rotation Cube per additional requests 
            GLUtil.rotateMatrix(mvpMatrix, stateModel.additionalGLCubeRotation);
            
            pilotGLCube.draw(mvpMatrix, Transparency.OPAQUE, programID);
        }
	}


	/**
	 * Get Rotation In Degrees
	 * 
	 * Calculate a 0 to 90 degrees rotation for a more pleasing
	 * graphical rotation instructions.
	 * 
	 * Note, this is call from the GL thread on every frame.  Logic is 
	 * added so that return value starts at 0 when app state change
	 * is noticed.
	 * 
	 * @return
	 */
	private int getRotationInDegrees() {

		long time = System.currentTimeMillis();

		// Set member data timeReference when graphic arrow turns on.
		if( (stateModel.appState == AppStateEnum.ROTATE_CUBE) || (stateModel.appState == AppStateEnum.ROTATE_FACE) ){
			if(rotationActive == false) {
				timeReference = time;
				rotationActive = true;
			}
		}
		else
			rotationActive = false;


		// Calculate arrow animation rotation.
		int arrowRotationInDegrees = (int) (( (time - timeReference) / 10 ) % 90);
		return arrowRotationInDegrees;
	}
	private boolean rotationActive = false;
	private long timeReference = 0;;
	
	
    
    /**
	 * Render Cube Edge Rotation Arrow
	 * 
	 * Render an Rubik Cube Edge Rotation request/instruction.
	 * This is used after a solution has been computed and to instruct 
	 * the user to rotate one edge at a time.
	 * 
	 * @param mvpMatrix
     * @param arrowRotationInDegrees 
	 */
	private void renderCubeEdgeRotationArrow(final float[] mvpMatrix, int arrowRotationInDegrees) {
		
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
			throw new java.lang.Error("Unknow rotation amount: problem with ascii format of logic solution");
		
		
		Scalar color = null;
		Direction direction = null;
		
		// Rotate and Translate Arrow as required by Rubik Logic Solution algorithm. 
		switch(moveNumonic.charAt(0)) {
		case 'U':
			color = stateModel.getFaceByName(FaceNameEnum.UP).observedTileArray[1][1].cvColor;
			Matrix.translateM(mvpMatrix, 0, 0.0f, +2.0f, 0.0f);
			Matrix.rotateM(mvpMatrix, 0, 90f, 1.0f, 0.0f, 0.0f);  // X rotation
			direction = (rotation == Rotation.CLOCKWISE) ?         Direction.NEGATIVE : Direction.POSITIVE; 
			break;
		case 'D':
			color = stateModel.getFaceByName(FaceNameEnum.DOWN).observedTileArray[1][1].cvColor;		
			Matrix.translateM(mvpMatrix, 0, 0.0f, -2.0f, 0.0f);
			Matrix.rotateM(mvpMatrix, 0, 90f, 1.0f, 0.0f, 0.0f);  // X rotation
			direction = (rotation == Rotation.COUNTER_CLOCKWISE) ? Direction.NEGATIVE : Direction.POSITIVE; 
			break;
		case 'L':
			color = stateModel.getFaceByName(FaceNameEnum.LEFT).observedTileArray[1][1].cvColor;
			Matrix.translateM(mvpMatrix, 0, -2.0f, 0.0f, 0.0f);
			Matrix.rotateM(mvpMatrix, 0, 90f, 0.0f, 1.0f, 0.0f);  // Y rotation
			direction = (rotation == Rotation.CLOCKWISE) ?         Direction.NEGATIVE : Direction.POSITIVE; 
			Matrix.rotateM(mvpMatrix, 0, 30f, 0.0f, 0.0f, 1.0f);  // looks better
			break;
		case 'R':
			color = stateModel.getFaceByName(FaceNameEnum.RIGHT).observedTileArray[1][1].cvColor;
			Matrix.translateM(mvpMatrix, 0, +2.0f, 0.0f, 0.0f);
			Matrix.rotateM(mvpMatrix, 0, 90f, 0.0f, 1.0f, 0.0f);  // Y rotation
			direction = (rotation == Rotation.COUNTER_CLOCKWISE) ? Direction.NEGATIVE : Direction.POSITIVE;
			Matrix.rotateM(mvpMatrix, 0, 30f, 0.0f, 0.0f, 1.0f);  // looks better
			break;
		case 'F':
			color = stateModel.getFaceByName(FaceNameEnum.FRONT).observedTileArray[1][1].cvColor;
			Matrix.translateM(mvpMatrix, 0, 0.0f, 0.0f, +2.0f);
			direction = (rotation == Rotation.COUNTER_CLOCKWISE) ? Direction.NEGATIVE : Direction.POSITIVE; 
			Matrix.rotateM(mvpMatrix, 0, 30f, 0.0f, 0.0f, 1.0f);  // looks better
			break;
		case 'B':
			color = stateModel.getFaceByName(FaceNameEnum.BACK).observedTileArray[1][1].cvColor;
			Matrix.translateM(mvpMatrix, 0, 0.0f, 0.0f, -2.0f);
			direction = (rotation == Rotation.CLOCKWISE) ?         Direction.NEGATIVE : Direction.POSITIVE; 
			Matrix.rotateM(mvpMatrix, 0, 30f, 0.0f, 0.0f, 1.0f);  // looks better
			break;
		}
		
	      
		// Add animated rotation and specify direction of arrow
		if(direction == Direction.NEGATIVE)  {
		    Matrix.rotateM(mvpMatrix, 0, arrowRotationInDegrees, 0.0f, 0.0f, 1.0f);  // 0 -> +90 degrees Z rotation

			Matrix.rotateM(mvpMatrix, 0, -90f,  0.0f, 0.0f, 1.0f);  // Z rotation of -90
			Matrix.rotateM(mvpMatrix, 0, +180f, 0.0f, 1.0f, 0.0f);  // Y rotation of +180
		}

		else
	         Matrix.rotateM(mvpMatrix, 0, -1 * arrowRotationInDegrees, 0.0f, 0.0f, 1.0f);  // 0 -> -90 degrees Z rotation
		
 
		if(amount == Amount.QUARTER_TURN)
			arrowQuarterTurn.draw(mvpMatrix, color, programID);
		else
			arrowHalfTurn.draw(mvpMatrix, color, programID);
	}
	


	/**
	 * Render Cube Full Rotation Arrow
	 * 
	 * Render a Rubik Cube Body Rotation request/instruction.
	 * This is used during the exploration phase to observed all six
	 * sides of the cube before any solution is compute or attempted.
	 * 
	 * @param mvpMatrix
	 * @param arrowRotationInDegrees 
	 */
	private void renderCubeFullRotationArrow(final float[] mvpMatrix, int arrowRotationInDegrees) {
	            
		// Render Front Face to Top Face Arrow Rotation
		if(stateModel.getNumObservedFaces() % 2 == 0) {
            Matrix.translateM(mvpMatrix, 0, 0.0f, +1.5f, +1.5f);
            Matrix.rotateM(mvpMatrix, 0, -90f, 0.0f, 1.0f, 0.0f);  // Y rotation of -90
		}
		
		// Render Right Face to Top Face Arrow Rotation
		else {
            Matrix.translateM(mvpMatrix, 0, +1.5f, +1.5f, 0.0f);
		}	

		// Roate arrow to give impression of movement.  Also, start back at -60 degrees: looks better.
        Matrix.rotateM(mvpMatrix, 0, arrowRotationInDegrees - 60, 0.0f, 0.0f, 1.0f);  // -60 -> +30 degrees Z rotation

		// Reverse direction of arrow.
        Matrix.rotateM(mvpMatrix, 0, -90f,  0.0f, 0.0f, 1.0f);  // Z rotation of -90
        Matrix.rotateM(mvpMatrix, 0, +180f, 0.0f, 1.0f, 0.0f);  // Y rotation of +180

		// Make Arrow Wider than normal by a factor of three.
        Matrix.scaleM(mvpMatrix, 0, 1.0f, 1.0f, 3.0f);
		
		// Render Quarter Turn Arrow
		arrowQuarterTurn.draw(mvpMatrix, ColorTileEnum.WHITE.cvColor, programID);
	}

}
