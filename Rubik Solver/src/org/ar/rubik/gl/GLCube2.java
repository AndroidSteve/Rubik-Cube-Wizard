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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import android.opengl.GLES20;

/**
 * A two-dimensional square for use as a drawn object in OpenGL ES 2.0.
 */
public class GLCube2 {

    private final String vertexShaderCode =
            // This matrix member variable provides a hook to manipulate
            // the coordinates of the objects that use this vertex shader
            "uniform mat4 uMVPMatrix;" +
            "attribute vec4 vPosition;" +
            "void main() {" +
            // The matrix must be included as a modifier of gl_Position.
            // Note that the uMVPMatrix factor *must be first* in order
            // for the matrix multiplication product to be correct.
            "  gl_Position = uMVPMatrix * vPosition;" +
            "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
            "uniform vec4 vColor;" +
            "void main() {" +
            "  gl_FragColor = vColor;" +
            "}";

//    private final FloatBuffer frontVertexBuffer;
//    private final ShortBuffer frontDrawListBuffer;

    private final int mProgram;
    private int mPositionHandle;
    private int mColorHandle;
    private int mMVPMatrixHandle;
    
    private FloatBuffer vertexBuffer;  // Buffer for vertex-array
    private int numFaces = 6;
    
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
//           -1.0f,  1.0f,  1.0f,  // 0. left-bottom-front
//           -1.0f, -1.0f,  1.0f,  // 1. right-bottom-front
//            1.0f, -1.0f,  1.0f,  // 2. left-top-front
//            1.0f,  1.0f,  1.0f,  // 3. right-top-front
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

    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 3;
    static float frontCoords[] = {
        -1.0f,  1.0f, +1.0f,   // top left
        -1.0f, -1.0f, +1.0f,   // bottom left
         1.0f, -1.0f, +1.0f,   // bottom right
         1.0f,  1.0f, +1.0f }; // top right


//    private final short drawOrder[] = { 0, 1, 2, 0, 2, 3 }; // order to draw vertices

    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    float redColor[] = { 1.0f, 0.0f, 0.0f, 1.0f };

    /**
     * Sets up the drawing object data for use in an OpenGL ES context.
     */
    public GLCube2() {
        
//        // initialize vertex byte buffer for shape coordinates
//        // (# of coordinate values * 4 bytes per float)
//        ByteBuffer frontCoordsByteBuffer = ByteBuffer.allocateDirect(frontCoords.length * 4);
//        frontCoordsByteBuffer.order(ByteOrder.nativeOrder());
//        frontVertexBuffer = frontCoordsByteBuffer.asFloatBuffer();
//        frontVertexBuffer.put(frontCoords);
//        frontVertexBuffer.position(0);
//
//        // initialize byte buffer for the draw list
//        // (# of coordinate values * 2 bytes per short)
//        ByteBuffer frontDrawByteBuffer = ByteBuffer.allocateDirect(drawOrder.length * 2);
//        frontDrawByteBuffer.order(ByteOrder.nativeOrder());
//        frontDrawListBuffer = frontDrawByteBuffer.asShortBuffer();
//        frontDrawListBuffer.put(drawOrder);
//        frontDrawListBuffer.position(0);
        
        // Setup vertex-array buffer. Vertices in float. A float has 4 bytes
        // This reserves memory that GPU has direct access to (correct?).
        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder()); // Use native byte order
        vertexBuffer = vbb.asFloatBuffer(); // Convert from byte to float
        vertexBuffer.put(vertices);         // Copy data into buffer
        vertexBuffer.position(0);           // Rewind
        

        // prepare shaders and OpenGL program
        int vertexShader = GLUtil.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);   
        int fragmentShader = GLUtil.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();             // create empty OpenGL Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mProgram);                  // create OpenGL program executables
    }

    /**
     * Encapsulates the OpenGL ES instructions for drawing this shape.
     *
     * @param mvpMatrix - The Model View Project matrix in which to draw
     * this shape.
     */
    public void draw(float[] mvpMatrix) {
        
        // Add program to OpenGL environment
        GLES20.glUseProgram(mProgram);

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
//        GLES20.glVertexAttribPointer(
//                mPositionHandle, 
//                COORDS_PER_VERTEX,
//                GLES20.GL_FLOAT,
//                false,
//                vertexStride,
//                frontVertexBuffer);
        GLES20.glVertexAttribPointer(
                mPositionHandle, 
                COORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                vertexStride,
                vertexBuffer);
        
        // get handle to fragment shader's vColor member
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

        // Set color for drawing the triangle
        GLES20.glUniform4fv(mColorHandle, 1, redColor, 0);

        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        GLUtil.checkGlError("glGetUniformLocation");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        GLUtil.checkGlError("glUniformMatrix4fv");

        // Draw the square
//        GLES20.glDrawElements(
//                GLES20.GL_TRIANGLES, 
//                drawOrder.length,
//                GLES20.GL_UNSIGNED_SHORT,
//                frontDrawListBuffer);
        
        // Render all the faces
        for (int face = 0; face < numFaces; face++) {
            
            // =+= Front (red) and Up (white)
            if(face == 0 || face == 4) {

                GLES20.glUniform4fv(mColorHandle, 1, colors[face], 0);

                GLES20.glDrawArrays(
                        GLES20.GL_TRIANGLE_STRIP, 
                        face*4, 
                        4);
            }
        }

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }

}