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

import org.ar.rubik.Lms;
import org.ar.rubik.RubikFace;
import org.ar.rubik.RubikFace.FaceRecognitionStatusEnum;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;

/**
 * @author stevep
 *
 */
public class AnnotationGLRenderer implements GLSurfaceView.Renderer {

	@SuppressWarnings("unused")
    private Context context;
	private GLCube gLCube;
	private float cubeXrotation = 35.0f;
	private float cubeYrotation = 45.0f;
	private boolean renderState = false;
	
	// True if we are actively tracking the cube (i.e. solve or partially solved)
	private boolean active = false;

	/**
	 * @param mainActivity
	 */
    public AnnotationGLRenderer(Context context) {
		this.context = context;
		
		gLCube = new GLCube();
    }

	/**
	 *  (non-Javadoc)
	 * @see android.opengl.GLSurfaceView.Renderer#onDrawFrame(javax.microedition.khronos.opengles.GL10)
	 */
    @Override
    public void onDrawFrame(GL10 gl) {
    	
		// Clear color and depth buffers using clear-value set earlier
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		
		if(renderState == false)
			return;
		
		gl.glLoadIdentity();                   // Reset model-view matrix 
		
		// Perspective Translate
		gl.glTranslatef(-6.0f, 0.0f, -10.0f);
		
		// Cube Rotation
		gl.glRotatef(cubeXrotation, 1.0f, 0.0f, 0.0f);  // X rotation of +45
		gl.glRotatef(cubeYrotation + 25.0f, 0.0f, 1.0f, 0.0f);  // Y rotation of +45
		
		gLCube.draw(gl, active);
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

    
	/**
	 * @param active
	 */
    public void setCubeOrienation(RubikFace rubikFace) {
		
		cubeXrotation = 35.0f;
		cubeYrotation = 45.0f;
		active = false;
		
		if(rubikFace == null)
			return;
		
		if(rubikFace.faceRecognitionStatus != FaceRecognitionStatusEnum.SOLVED)
			return;
		
		Lms lmsResult = rubikFace.lmsResult;
		
		if(lmsResult == null)
			return;
		
	
		active = true;
		
		
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
	 * @param renderState
	 */
    public void setRenderState(boolean renderState) {
    	this.renderState = renderState;
    }

}
