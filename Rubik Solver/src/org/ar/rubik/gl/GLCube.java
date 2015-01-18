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
import javax.microedition.khronos.opengles.GL10;

/**
 * Class CubeGL
 * 
 * Coordinates of cube are described by an floating point array of (6x4x3) 72 elements.
 * 
 * =+= TODO: How to evolve this such that:
 *   0)  How to get colors from global Constants
 *   1)  Color can be assigned per face name.
 *   2)  Only the center tile of a face is colored.
 *   3)  Orientation is set able (i.e., xxxxxxxxxx)
 *   4)  Or should we consider Rubik Cube Animator
 * 
 * @author android.steve@testlens.com
 *
 */
public class GLCube {

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
     *  Constructor - Set up the buffers
     */
    public GLCube() {

        // Setup vertex-array buffer. Vertices in float. A float has 4 bytes
        // This reserves memory that GPU has direct access to (correct?).
        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder()); // Use native byte order
        vertexBuffer = vbb.asFloatBuffer(); // Convert from byte to float
        vertexBuffer.put(vertices);         // Copy data into buffer
        vertexBuffer.position(0);           // Rewind
    }


    /**
     * Draw the shape
     *  
     * @param gl
     * @param isTransparent
     */
    public void draw(GL10 gl, boolean isTransparent) {

        gl.glFrontFace(GL10.GL_CCW);    // Front face in counter-clockwise orientation
        gl.glEnable(GL10.GL_CULL_FACE); // Enable cull face
        gl.glCullFace(GL10.GL_BACK);    // Cull the back face (don't display)

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);

        // Render all the faces
        for (int face = 0; face < numFaces; face++) {
            // Set the color for each of the faces
            gl.glColor4f(colors[face][0], colors[face][1], colors[face][2], (isTransparent ? 0.2f : 1.0f) );
            // Draw the primitive from the vertex-array directly
            gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, face*4, 4);
        }

        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glDisable(GL10.GL_CULL_FACE);
    }
}


