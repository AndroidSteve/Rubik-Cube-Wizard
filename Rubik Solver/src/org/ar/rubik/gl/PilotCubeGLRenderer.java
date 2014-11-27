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
 *   This class renders the "Pilot Cube" which appears on the right hand side in
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
import org.ar.rubik.StateModel;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;

/**
 * @author stevep
 *
 */
public class PilotCubeGLRenderer implements GLSurfaceView.Renderer {

    private StateModel stateModel;
	private PilotGLCube pilotGLCube;
	
	// True if we are actively tracking the cube (i.e. solve or partially solved)
	private boolean active = false;
	

	/**
	 * @param mainActivity
	 */
    public PilotCubeGLRenderer(StateModel stateModel) {
		this.stateModel = stateModel;
		
		pilotGLCube = new PilotGLCube();
    }

    
	/**
	 *  (non-Javadoc)
	 * @see android.opengl.GLSurfaceView.Renderer#onDrawFrame(javax.microedition.khronos.opengles.GL10)
	 */
    @Override
    public void onDrawFrame(GL10 gl) {
    	
//    	Log.e(Constants.TAG, "GL Thread ID = " + Thread.currentThread().getId());
    	
		// Clear color and depth buffers using clear-value set earlier
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		
		if(stateModel.renderPilotCube == false)
			return;
		
		if(stateModel.cubeReconstructor == null)
			return;
		
		float cubeXrotation = stateModel.cubeReconstructor.cubeXrotation;
		float cubeYrotation = stateModel.cubeReconstructor.cubeYrotation;
		
		gl.glLoadIdentity();                   // Reset model-view matrix 
		
		// Perspective Translate
		gl.glTranslatef(-6.0f, 0.0f, -10.0f);
		
		// Cube Rotation
		gl.glRotatef(cubeXrotation, 1.0f, 0.0f, 0.0f);  // X rotation of +45
		gl.glRotatef(cubeYrotation + 25.0f, 0.0f, 1.0f, 0.0f);  // Y rotation of +45
		
		pilotGLCube.draw(gl, active);
    }

    
	/**
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


    
	/** =+= delete this
	 * @param renderState
	 */
    public void setRenderState(boolean renderState) {
//    	this.renderState = renderState;
    }

}
