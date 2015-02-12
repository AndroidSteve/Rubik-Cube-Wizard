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
 *   Renders a six sided cube in Object Coordinates centered at the origin with
 *   edge length of 2.0 units.
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

import static android.opengl.GLES20.GL_LINK_STATUS;
import static android.opengl.GLES20.glDeleteProgram;
import static android.opengl.GLES20.glGetProgramInfoLog;
import static android.opengl.GLES20.glGetProgramiv;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import org.ar.rubik.Constants;
import org.ar.rubik.R;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

/**
 * A three-dimensional cube use as a drawn object in OpenGL ES 2.0.
 */
public class GLCube2 {

    // OpenGL shader program ID
    private final int programID;
    
    // Buffer for vertex-array
    private FloatBuffer vertexBuffer; 
    
    // number of cube faces: of course it is 6!
    private static final int numFaces = 6;

    // number of coordinates per vertex in this array
    private static final int COORDS_PER_VERTEX = 3;
    
    // number of bytes in a float
    private static final int BYTES_PER_FLOAT = 4;

    // number of total bytes in vertex stride: 12 in this case.
    private static final int vertexStride = COORDS_PER_VERTEX * BYTES_PER_FLOAT;

    private float[][] colors = {  // Colors of the 6 faces
            {1.0f, 0.0f, 0.0f, 1.0f},  // 0. Front is Red
            {1.0f, 0.5f, 0.0f, 1.0f},  // 1. Back is Orange
            {0.0f, 1.0f, 0.0f, 1.0f},  // 2. Left is Green
            {0.0f, 0.0f, 1.0f, 1.0f},  // 3. Right is Blue
            {0.8f, 0.8f, 0.8f, 1.0f},  // 4. Up is White
            {1.0f, 1.0f, 0.0f, 1.0f}   // 5. Down is Yellow
    };
    
    private float[] vertices = {  // Vertices of the 6 faces
            // FRONT
           -1.0f, -1.0f,  1.0f,  // 0. left-bottom-front
            1.0f, -1.0f,  1.0f,  // 1. right-bottom-front
           -1.0f,  1.0f,  1.0f,  // 2. left-top-front
            1.0f,  1.0f,  1.0f,  // 3. right-top-front
            // BACK
            1.0f, -1.0f, -1.0f,  // 6. right-bottom-back
           -1.0f, -1.0f, -1.0f,  // 4. left-bottom-back
            1.0f,  1.0f, -1.0f,  // 7. right-top-back
           -1.0f,  1.0f, -1.0f,  // 5. left-top-back
            // LEFT
           -1.0f, -1.0f, -1.0f,  // 4. left-bottom-back
           -1.0f, -1.0f,  1.0f,  // 0. left-bottom-front 
           -1.0f,  1.0f, -1.0f,  // 5. left-top-back
           -1.0f,  1.0f,  1.0f,  // 2. left-top-front
            // RIGHT
            1.0f, -1.0f,  1.0f,  // 1. right-bottom-front
            1.0f, -1.0f, -1.0f,  // 6. right-bottom-back
            1.0f,  1.0f,  1.0f,  // 3. right-top-front
            1.0f,  1.0f, -1.0f,  // 7. right-top-back
            // UP
           -1.0f,  1.0f,  1.0f,  // 2. left-top-front
            1.0f,  1.0f,  1.0f,  // 3. right-top-front
           -1.0f,  1.0f, -1.0f,  // 5. left-top-back
            1.0f,  1.0f, -1.0f,  // 7. right-top-back
            // DOWN
           -1.0f, -1.0f, -1.0f,  // 4. left-bottom-back
            1.0f, -1.0f, -1.0f,  // 6. right-bottom-back
           -1.0f, -1.0f,  1.0f,  // 0. left-bottom-front
            1.0f, -1.0f,  1.0f   // 1. right-bottom-front
    };

    
    

    /**
     * Sets up the drawing object data for use in an OpenGL ES context.
     * @param context 
     */
    public GLCube2(Context context) {
        
        // Setup vertex-array buffer. Vertices in float. A float has 4 bytes
        // This reserves memory that GPU has direct access to (correct?).
        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder()); // Use native byte order
        vertexBuffer = vbb.asFloatBuffer(); // Convert from byte to float
        vertexBuffer.put(vertices);         // Copy data into buffer
        vertexBuffer.position(0);           // Rewind
        
        
        // Obtain vertex and fragment shader source text
        String vertexShaderCode = GLUtil.readTextFileFromResource(context, R.raw.simple_vertex_shader);
        String fragmentShaderCode = GLUtil.readTextFileFromResource(context, R.raw.simple_fragment_shader);

        
        // prepare shaders and OpenGL program
        int vertexShader = GLUtil.compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);   
        int fragmentShader = GLUtil.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        programID = GLES20.glCreateProgram();             // create empty OpenGL Program
        GLES20.glAttachShader(programID, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(programID, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(programID);                  // create OpenGL program executables
        
        // Get the link status.
        final int[] linkStatus = new int[1];
        glGetProgramiv(programID, GL_LINK_STATUS, linkStatus, 0);

        if (Constants.LOGGER) {
            // Print the program info log to the Android log output.
            Log.v(Constants.TAG_SHADER, "Results of linking program:\n" + glGetProgramInfoLog(programID));
        }

        // Verify the link status.
        if (linkStatus[0] == 0) {
            // If it failed, delete the program object.
            glDeleteProgram(programID);

            if (Constants.LOGGER) {
                Log.e(Constants.TAG_SHADER, "Linking of program failed.");
            }
        }
    }

    
    
    /**
     * Encapsulates the OpenGL ES instructions for drawing this shape.
     *
     * @param mvpMatrix - The Model View Project matrix in which to draw
     * this shape.
     */
    public void draw(float[] mvpMatrix, boolean isTransparent) {
        
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);   // =+= ?? Why ??
        
        // Add program to OpenGL environment
        GLES20.glUseProgram(programID);

        // get handle to vertex shader's vPosition member
        int vertexArrayID = GLES20.glGetAttribLocation(programID, "vPosition");

        // Enable a handle to the cube vertices
        GLES20.glEnableVertexAttribArray(vertexArrayID);

        // Prepare the cube coordinate data
        GLES20.glVertexAttribPointer(
                vertexArrayID, 
                COORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                vertexStride,
                vertexBuffer);
        
        // get handle to fragment shader's vColor member
        int colorID = GLES20.glGetUniformLocation(programID, "vColor");

        // get handle to shape's transformation matrix
        int mvpMatrixID = GLES20.glGetUniformLocation(programID, "uMVPMatrix");
        GLUtil.checkGlError("glGetUniformLocation");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mvpMatrixID, 1, false, mvpMatrix, 0);
        GLUtil.checkGlError("glUniformMatrix4fv");
        
        // Render all the faces
        for (int face = 0; face < numFaces; face++) {
 
            // Specify color and transparency
            colors[face][3] = (isTransparent ? 0.2f : 1.0f);
            GLES20.glUniform4fv(colorID, 1, colors[face], 0);

            // Draw Triangles
            GLES20.glDrawArrays(
                    GLES20.GL_TRIANGLE_STRIP, 
                    face*BYTES_PER_FLOAT, 
                    BYTES_PER_FLOAT);
        }

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(vertexArrayID);
        
        GLES20.glDisable(GLES20.GL_CULL_FACE);
    }
}