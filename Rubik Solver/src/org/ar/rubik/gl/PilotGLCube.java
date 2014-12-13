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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.opengles.GL10;

public class PilotGLCube {

    private FloatBuffer vertexBuffer;  // Buffer for vertex-array
    private int numFaces = 6;

    private float[][] colors = {  // Colors of the 6 faces
            {1.0f, 0.0f, 0.0f, 1.0f},  // 0. Front is Red
            {1.0f, 0.5f, 0.0f, 1.0f},  // 1. Back is Orange
            {0.0f, 1.0f, 0.0f, 1.0f},  // 2. Left is Green
            {0.0f, 0.0f, 1.0f, 1.0f},  // 3. Right is Blue
            {1.0f, 1.0f, 1.0f, 1.0f},  // 4. Up is White
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

    // Constructor - Set up the buffers
    public PilotGLCube() {
        // Setup vertex-array buffer. Vertices in float. An float has 4 bytes
        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder()); // Use native byte order
        vertexBuffer = vbb.asFloatBuffer(); // Convert from byte to float
        vertexBuffer.put(vertices);         // Copy data into buffer
        vertexBuffer.position(0);           // Rewind
    }

    // Draw the shape
    public void draw(GL10 gl, boolean b) {
        
        gl.glFrontFace(GL10.GL_CCW);    // Front face in counter-clockwise orientation
        gl.glEnable(GL10.GL_CULL_FACE); // Enable cull face
        gl.glCullFace(GL10.GL_BACK);    // Cull the back face (don't display)

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);

        // Render all the faces
        for (int face = 0; face < numFaces; face++) {
            // Set the color for each of the faces
            gl.glColor4f(colors[face][0], colors[face][1], colors[face][2], colors[face][3]);
            // Draw the primitive from the vertex-array directly
            gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, face*4, 4);
        }
        
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glDisable(GL10.GL_CULL_FACE);
    }
}

//import java.nio.ByteBuffer;
//import java.nio.ByteOrder;
//import java.nio.FloatBuffer;
//
//import javax.microedition.khronos.opengles.GL10;
//
//
//
///**
// * 
// * 
// * @author stevep
// *
// */
//public class PilotGLCube {
//
//	// Buffer for vertex-array
//	private FloatBuffer frontVertexBuffer;
//	private FloatBuffer backVertexBuffer;
//	private FloatBuffer leftVertexBuffer;
//	private FloatBuffer rightVertexBuffer;
//	private FloatBuffer upVertexBuffer;
//	private FloatBuffer downVertexBuffer;
//
//	private FloatBuffer frontOutlineVertexBuffer;
//	private FloatBuffer backOutlineVertexBuffer;
//	private FloatBuffer leftOutlineVertexBuffer;
//	private FloatBuffer rightOutlineVertexBuffer;
//	private FloatBuffer upOutlineVertexBuffer;
//	private FloatBuffer downOutlineVertexBuffer;
//
//	private float[] frontVertices = { 
//			+1.0f,  +1.0f,  +1.0f,
//			+1.0f,  -1.0f,  +1.0f,
//			-1.0f,  +1.0f,  +1.0f,
//			-1.0f,  -1.0f,  +1.0f };
//
//	private float[] frontOutlineVertices = {
//			+1.0f,  +1.0f,  +1.0f,
//			+1.0f,  -1.0f,  +1.0f,
//			-1.0f,  -1.0f,  +1.0f,
//			-1.0f,  +1.0f,  +1.0f };
//
//	private float[] backVertices = { 
//			+1.0f,  +1.0f,  -1.0f,
//			+1.0f,  -1.0f,  -1.0f,
//			-1.0f,  +1.0f,  -1.0f,
//			-1.0f,  -1.0f,  -1.0f };
//
//	private float[] backOutlineVertices = {
//			+1.0f,  +1.0f,  -1.0f,
//			+1.0f,  -1.0f,  -1.0f,
//			-1.0f,  -1.0f,  -1.0f,
//			-1.0f,  +1.0f,  -1.0f };
//
////	private float[] leftVertices = { 		
////			-1.0f,  -1.0f,  +1.0f,
////			-1.0f,  +1.0f,  +1.0f,
////			-1.0f,  -1.0f,  -1.0f,
////			-1.0f,  +1.0f,  -1.0f,};
//	private float[] leftVertices = { 		
//			-1.0f,  +1.0f,  +1.0f,
//			-1.0f,  +1.0f,  -1.0f,
//			-1.0f,  -1.0f,  +1.0f,
//			-1.0f,  -1.0f,  -1.0f,};
//	
//	private float[] leftOutlineVertices = { 		
//			-1.0f,  -1.0f,  +1.0f,
//			-1.0f,  +1.0f,  +1.0f,
//			-1.0f,  +1.0f,  -1.0f,
//			-1.0f,  -1.0f,  -1.0f,};
//
//	private float[] rightVertices = { 		
//			+1.0f,  +1.0f,  +1.0f,
//			+1.0f,  +1.0f,  -1.0f,
//			+1.0f,  -1.0f,  +1.0f,
//			+1.0f,  -1.0f,  -1.0f,};
//	
//	private float[] rightOutlineVertices = { 		
//			+1.0f,  -1.0f,  +1.0f,
//			+1.0f,  +1.0f,  +1.0f,
//			+1.0f,  +1.0f,  -1.0f,
//			+1.0f,  -1.0f,  -1.0f,};
//
//	private float[] upVertices = { 		
//			+1.0f,  +1.0f,  +1.0f,
//			-1.0f,  +1.0f,  +1.0f,
//			+1.0f,  +1.0f,  -1.0f,
//			-1.0f,  +1.0f,  -1.0f,};
//
//	private float[] upOutlineVertices = { 		
//			+1.0f,  +1.0f,  +1.0f,
//			-1.0f,  +1.0f,  +1.0f,
//			-1.0f,  +1.0f,  -1.0f,
//			+1.0f,  +1.0f,  -1.0f,};
//
//	private float[] downVertices = { 		
//			+1.0f,  -1.0f,  +1.0f,
//			-1.0f,  -1.0f,  +1.0f,
//			+1.0f,  -1.0f,  -1.0f,
//			-1.0f,  -1.0f,  -1.0f,};
//
//	private float[] downOutlineVertices = { 		
//			+1.0f,  -1.0f,  +1.0f,
//			-1.0f,  -1.0f,  +1.0f,
//			-1.0f,  -1.0f,  -1.0f,
//			+1.0f,  -1.0f,  -1.0f,};
//
//
//
//	// Constructor - Setup the vertex buffer
//	public PilotGLCube() {
//
//		// Setup vertex array buffer. Vertices in float. A float has 4 bytes
//		ByteBuffer frontByteBuffer = ByteBuffer.allocateDirect(frontVertices.length * 4);
//		frontByteBuffer.order(ByteOrder.nativeOrder()); 
//		frontVertexBuffer = frontByteBuffer.asFloatBuffer(); // Convert from byte to float
//		frontVertexBuffer.put(frontVertices);                // Copy data into buffer
//		frontVertexBuffer.position(0);                       // Rewind
//
//		// Setup vertex array buffer. Vertices in float. A float has 4 bytes
//		ByteBuffer backByteBuffer = ByteBuffer.allocateDirect(backVertices.length * 4);
//		backByteBuffer.order(ByteOrder.nativeOrder()); 
//		backVertexBuffer = backByteBuffer.asFloatBuffer(); // Convert from byte to float
//		backVertexBuffer.put(backVertices);                // Copy data into buffer
//		backVertexBuffer.position(0);                       // Rewind
//
//		
//		// Setup vertex array buffer. Vertices in float. A float has 4 bytes
//		ByteBuffer leftByteBuffer = ByteBuffer.allocateDirect(leftVertices.length * 4);
//		leftByteBuffer.order(ByteOrder.nativeOrder());
//		leftVertexBuffer = leftByteBuffer.asFloatBuffer();
//		leftVertexBuffer.put(leftVertices);
//		leftVertexBuffer.position(0);
//
//		// Setup vertex array buffer. Vertices in float. A float has 4 bytes
//		ByteBuffer rightByteBuffer = ByteBuffer.allocateDirect(rightVertices.length * 4);
//		rightByteBuffer.order(ByteOrder.nativeOrder());
//		rightVertexBuffer = rightByteBuffer.asFloatBuffer();
//		rightVertexBuffer.put(rightVertices);
//		rightVertexBuffer.position(0);
//
//		
//		// Setup vertex array buffer. Vertices in float. A float has 4 bytes
//		ByteBuffer topByteBuffer = ByteBuffer.allocateDirect(upVertices.length * 4);
//		topByteBuffer.order(ByteOrder.nativeOrder());
//		upVertexBuffer = topByteBuffer.asFloatBuffer();
//		upVertexBuffer.put(upVertices);
//		upVertexBuffer.position(0);
//
//		// Setup vertex array buffer. Vertices in float. A float has 4 bytes
//		ByteBuffer downByteBuffer = ByteBuffer.allocateDirect(downVertices.length * 4);
//		downByteBuffer.order(ByteOrder.nativeOrder());
//		downVertexBuffer = downByteBuffer.asFloatBuffer();
//		downVertexBuffer.put(downVertices);
//		downVertexBuffer.position(0);
//
//
//
//		// Setup vertex array buffer. Vertices in float. A float has 4 bytes
//		ByteBuffer frontOutlineByteBuffer = ByteBuffer.allocateDirect(frontOutlineVertices.length * 4);
//		frontOutlineByteBuffer.order(ByteOrder.nativeOrder()); 
//		frontOutlineVertexBuffer = frontOutlineByteBuffer.asFloatBuffer(); // Convert from byte to float
//		frontOutlineVertexBuffer.put(frontOutlineVertices);         // Copy data into buffer
//		frontOutlineVertexBuffer.position(0);           // Rewind
//
//		// Setup vertex array buffer. Vertices in float. A float has 4 bytes
//		ByteBuffer backOutlineByteBuffer = ByteBuffer.allocateDirect(backOutlineVertices.length * 4);
//		backOutlineByteBuffer.order(ByteOrder.nativeOrder()); 
//		backOutlineVertexBuffer = backOutlineByteBuffer.asFloatBuffer(); // Convert from byte to float
//		backOutlineVertexBuffer.put(backOutlineVertices);         // Copy data into buffer
//		backOutlineVertexBuffer.position(0);           // Rewind
//
//		// Setup vertex array buffer. Vertices in float. A float has 4 bytes
//		ByteBuffer leftOutlineByteBuffer = ByteBuffer.allocateDirect(leftOutlineVertices.length * 4);
//		leftOutlineByteBuffer.order(ByteOrder.nativeOrder());
//		leftOutlineVertexBuffer = leftOutlineByteBuffer.asFloatBuffer();
//		leftOutlineVertexBuffer.put(leftOutlineVertices);
//		leftOutlineVertexBuffer.position(0);
//
//		// Setup vertex array buffer. Vertices in float. A float has 4 bytes
//		ByteBuffer rightOutlineByteBuffer = ByteBuffer.allocateDirect(rightOutlineVertices.length * 4);
//		rightOutlineByteBuffer.order(ByteOrder.nativeOrder()); 
//		rightOutlineVertexBuffer = rightOutlineByteBuffer.asFloatBuffer(); // Convert from byte to float
//		rightOutlineVertexBuffer.put(rightOutlineVertices);         // Copy data into buffer
//		rightOutlineVertexBuffer.position(0);           // Rewind
//
//		// Setup vertex array buffer. Vertices in float. A float has 4 bytes
//		ByteBuffer upOutlineByteBuffer = ByteBuffer.allocateDirect(upOutlineVertices.length * 4);
//		upOutlineByteBuffer.order(ByteOrder.nativeOrder());
//		upOutlineVertexBuffer = upOutlineByteBuffer.asFloatBuffer();
//		upOutlineVertexBuffer.put(upOutlineVertices);
//		upOutlineVertexBuffer.position(0);
//
//		// Setup vertex array buffer. Vertices in float. A float has 4 bytes
//		ByteBuffer downOutlineByteBuffer = ByteBuffer.allocateDirect(downOutlineVertices.length * 4);
//		downOutlineByteBuffer.order(ByteOrder.nativeOrder()); 
//		downOutlineVertexBuffer = downOutlineByteBuffer.asFloatBuffer(); // Convert from byte to float
//		downOutlineVertexBuffer.put(downOutlineVertices);         // Copy data into buffer
//		downOutlineVertexBuffer.position(0);           // Rewind
//
//	}
//
//	
//	// Render the shape
//	public void draw(GL10 gl, boolean active) {
//
//		float intensity = active ? 1.0f : 0.5f;
//		float alpha = 1.0f;
//		
//		// =+= Don't understand why inversion of Z axis seems necessary for all mode
//		// =+= location to seem correct.
//		gl.glScalef(2, 2, 2);
//
//		// Enable vertex-array and define its buffer
//		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
//
//		gl.glColor4f(intensity, 0.0f, 0.0f, alpha);  // Front is red
//		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, frontVertexBuffer);
//		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, frontVertices.length / 3);
//
//		gl.glColor4f(intensity * 0.9f, intensity * 0.5f, intensity * 0.4f, alpha); // back is orange
//		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, backVertexBuffer);
//		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, backVertices.length / 3);
//
//		gl.glColor4f(0.0f, intensity, 0.0f, alpha);  // Left is green
//		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, leftVertexBuffer);
//		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, leftVertices.length / 3);
//
//		gl.glColor4f(0.0f, 0.0f, intensity, alpha);  // right is blue
//		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, rightVertexBuffer);
//		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, rightVertices.length / 3);
//
//		gl.glColor4f(intensity, intensity, intensity, alpha); // up is white
//		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, upVertexBuffer);
//		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, upVertices.length / 3);
//
//		gl.glColor4f(intensity, intensity, 0.0f, alpha); // down is yellow
//		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, downVertexBuffer);
//		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, downVertices.length / 3);
//		
//
//		gl.glColor4f(intensity, 0.0f, 0.0f, alpha);  // Front is red
//		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, frontVertexBuffer);
//		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, frontVertices.length / 3);
//		
//		
//
//		// Thick white lines
//		gl.glLineWidth(10.0f);
//		gl.glColor4f(intensity, intensity, intensity, 1.0f);
//
//		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, frontOutlineVertexBuffer);
//		gl.glDrawArrays(GL10.GL_LINE_LOOP, 0, frontOutlineVertices.length / 3);
//		
//		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, backOutlineVertexBuffer);
//		gl.glDrawArrays(GL10.GL_LINE_LOOP, 0, backOutlineVertices.length / 3);
//		
//		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, upOutlineVertexBuffer);
//		gl.glDrawArrays(GL10.GL_LINE_LOOP, 0, upOutlineVertices.length / 3);
//		
//		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, downOutlineVertexBuffer);
//		gl.glDrawArrays(GL10.GL_LINE_LOOP, 0, downOutlineVertices.length / 3);
//		
//		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, leftOutlineVertexBuffer);
//		gl.glDrawArrays(GL10.GL_LINE_LOOP, 0, leftOutlineVertices.length / 3);
//
//		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, rightOutlineVertexBuffer);
//		gl.glDrawArrays(GL10.GL_LINE_LOOP, 0, rightOutlineVertices.length / 3);
//
//
//		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
//	}
//}
