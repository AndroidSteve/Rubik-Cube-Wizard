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
 *   An Arrow in 3D space made up of 180 separate triangles, two triangles in 
 *   each degree and each flat segment every one degree is drawn.  The arrow
 *   is drawn in a quarter true in the X-Y plane in quadrant I at a radius of 1.0 with the head
 *   pointed at the X axis.  The width of the arrow is in the Z axis (+/- 0.3) with the 
 *   head having maximum Z axis dimensions of +/- 0.6.
 *   
 *   Optionally, an arrow can be constructed that spans two quadrants (I and II) 
 *   thus conveying a full 180 degrees instead of 90 degrees.
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

import org.opencv.core.Scalar;

import android.opengl.GLES20;

/**
 * An arrow in three-dimensional space for use as a drawn object in OpenGL ES 2.0.
 */
public class GLArrow2 {

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

    public enum Amount { QUARTER_TURN, HALF_TURN };

    private final int mProgram;
    private int mPositionHandle;
    private int mColorHandle;
    private int mMVPMatrixHandle;
    
    
    // Buffer for vertex-array
    private FloatBuffer vertexBuffer;

    // number of coordinates per vertex in this array
    private static final int COORDS_PER_VERTEX = 3;
    
    // number of bytes in a float
    private static final int BYTES_PER_FLOAT = 4;

    // number of total bytes in vertex stride: 12 in this case.
    private static final int VERTEX_STRIDE = COORDS_PER_VERTEX * BYTES_PER_FLOAT;

    // number of vertices in the arrow arch
    private static final int VERTICES_PER_ARCH = 90 + 1;

    
    
    /**
     * Sets up the drawing object data for use in an OpenGL ES context.
     */
    public GLArrow2(Amount amount) {
        
        double angleScale = (amount == Amount.QUARTER_TURN) ? 1.0 : 3.0;
        
        float[] vertices = new float[VERTICES_PER_ARCH * 6];
        
        for(int i=0; i<VERTICES_PER_ARCH; i++)  {
            
            // Angle will range from 0 to 90, or 0 to 180.
            double angleRads = i * angleScale * Math.PI / 180.0;
            float x = (float) Math.cos(angleRads);
            float y = (float) Math.sin(angleRads);
            
            vertices[i*6 + 0] = x;
            vertices[i*6 + 1] = y;
            vertices[i*6 + 2] = -1.0f * calculateWidth(angleRads);

            vertices[i*6 + 3] = x;
            vertices[i*6 + 4] = y;
            vertices[i*6 + 5] = +1.0f * calculateWidth(angleRads);
        }
        
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
     * Calculate Width
     * 
     * The width of the arrow is dependent upon the angle with the X axis.  For the 
     * first 20 degrees, the arrow width increases to 0.6 to form the head, there after
     * the arrow width is constant at a width of 0.3.
     * 
     * @param angleRads
     * @return
     */
    private float calculateWidth(double angleRads) {
        
        double angleDegrees = angleRads * 180.0 / Math.PI;
        
        // Arrow Body - A constant width of 0.3.
        if(angleDegrees > 20.0)
            return 0.3f;
        
        // Arrow Head - Ranges from a width of 0.0 to 0.6.
        else
            return (float) (angleDegrees / 20.0 * 0.6);
    }

    
    
    /**
     * Encapsulates the OpenGL ES instructions for drawing this shape.
     *
     * @param mvpMatrix - The Model View Project matrix in which to draw this shape.
     * @param color - Color to apply to arrow
     */
    public void draw(float[] mvpMatrix, Scalar color) {
        
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        
        // Add program to OpenGL environment
        GLES20.glUseProgram(mProgram);

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

        // Enable a handle to the cube vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        
        // get handle to fragment shader's vColor member
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        GLUtil.checkGlError("glGetUniformLocation");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        GLUtil.checkGlError("glUniformMatrix4fv");        
        

        
        
        // Prepare the cube coordinate data
        GLES20.glVertexAttribPointer(
                mPositionHandle, 
                COORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                VERTEX_STRIDE,
                vertexBuffer);

        // Draw Back Side a bit darker
        // Translate to GL Color and make a bit darker.
        float [] glBackSideColor = {
                (float)color.val[0] / (256.0f + 128.0f),
                (float)color.val[1] / (256.0f + 128.0f),
                (float)color.val[2] / (256.0f + 128.0f),
                1.0f
        };
        GLES20.glUniform4fv(mColorHandle, 1, glBackSideColor, 0);

        GLES20.glCullFace(GLES20.GL_BACK);

        // Draw Triangles
        GLES20.glDrawArrays(
                GLES20.GL_TRIANGLE_STRIP, 
                0, 
                VERTICES_PER_ARCH * 2);  // Number of triangles to be drawn

        
                
        // Prepare the cube coordinate data
        GLES20.glVertexAttribPointer(
                mPositionHandle, 
                COORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                VERTEX_STRIDE,
                vertexBuffer);

        // Draw Front Side a bit darker
        // Translate to GL Color and make a bit darker.
        float [] glFrontSideColor = {
                (float)color.val[0] / 256.0f,
                (float)color.val[1] / 256.0f,
                (float)color.val[2] / 256.0f,
                1.0f
        };
        GLES20.glUniform4fv(mColorHandle, 1, glFrontSideColor, 0);

        GLES20.glCullFace(GLES20.GL_FRONT);

        // Draw Triangles
        GLES20.glDrawArrays(
                GLES20.GL_TRIANGLE_STRIP, 
                0, 
                VERTICES_PER_ARCH * 2);  // Number of triangles to be drawn



        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);        
        
        GLES20.glDisable(GLES20.GL_CULL_FACE);
    }
}