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



/**
 * 
 * 
 * @author stevep
 *
 */
public class GLCube {

	  // Buffer for vertex-array
	private FloatBuffer frontVertexBuffer;
	private FloatBuffer leftVertexBuffer;
	private FloatBuffer topVertexBuffer;
	private FloatBuffer frontOutlineVertexBuffer;
	private FloatBuffer leftOutlineVertexBuffer;
	private FloatBuffer topOutlineVertexBuffer;
	
	private float[] frontVertices = { 
			+1.0f,  +1.0f,  +1.0f,
			+1.0f,  -1.0f,  +1.0f,
			-1.0f,  +1.0f,  +1.0f,
			-1.0f,  -1.0f,  +1.0f };
	
	private float[] frontOutlineVertices = {
				+1.0f,  +1.0f,  +1.0f,
				+1.0f,  -1.0f,  +1.0f,
				-1.0f,  -1.0f,  +1.0f,
				-1.0f,  +1.0f,  +1.0f };

	private float[] leftVertices = { 		
			-1.0f,  -1.0f,  +1.0f,
			-1.0f,  +1.0f,  +1.0f,
			-1.0f,  -1.0f,  -1.0f,
			-1.0f,  +1.0f,  -1.0f,};
	
	private float[] leftOutlineVertices = { 		
			-1.0f,  -1.0f,  +1.0f,
			-1.0f,  +1.0f,  +1.0f,
			-1.0f,  +1.0f,  -1.0f,
			-1.0f,  -1.0f,  -1.0f,};
	
	private float[] topVertices = { 		
			+1.0f,  +1.0f,  +1.0f,
			-1.0f,  +1.0f,  +1.0f,
			+1.0f,  +1.0f,  -1.0f,
			-1.0f,  +1.0f,  -1.0f,};
	
	private float[] topOutlineVertices = { 		
			+1.0f,  +1.0f,  +1.0f,
			-1.0f,  +1.0f,  +1.0f,
			-1.0f,  +1.0f,  -1.0f,
			+1.0f,  +1.0f,  -1.0f,};
	


	// Constructor - Setup the vertex buffer
	public GLCube() {
		
		// Setup vertex array buffer. Vertices in float. A float has 4 bytes
		ByteBuffer frontByteBuffer = ByteBuffer.allocateDirect(frontVertices.length * 4);
		frontByteBuffer.order(ByteOrder.nativeOrder()); 
		frontVertexBuffer = frontByteBuffer.asFloatBuffer(); // Convert from byte to float
		frontVertexBuffer.put(frontVertices);                // Copy data into buffer
		frontVertexBuffer.position(0);                       // Rewind

		// Setup vertex array buffer. Vertices in float. A float has 4 bytes
		ByteBuffer leftByteBuffer = ByteBuffer.allocateDirect(leftVertices.length * 4);
		leftByteBuffer.order(ByteOrder.nativeOrder());
		leftVertexBuffer = leftByteBuffer.asFloatBuffer();
		leftVertexBuffer.put(leftVertices);
		leftVertexBuffer.position(0);

		// Setup vertex array buffer. Vertices in float. A float has 4 bytes
		ByteBuffer topByteBuffer = ByteBuffer.allocateDirect(topVertices.length * 4);
		topByteBuffer.order(ByteOrder.nativeOrder());
		topVertexBuffer = topByteBuffer.asFloatBuffer();
		topVertexBuffer.put(topVertices);
		topVertexBuffer.position(0);
		
		
		
		// Setup vertex array buffer. Vertices in float. A float has 4 bytes
		ByteBuffer frontOutlineByteBuffer = ByteBuffer.allocateDirect(frontOutlineVertices.length * 4);
		frontOutlineByteBuffer.order(ByteOrder.nativeOrder()); 
		frontOutlineVertexBuffer = frontOutlineByteBuffer.asFloatBuffer(); // Convert from byte to float
		frontOutlineVertexBuffer.put(frontOutlineVertices);         // Copy data into buffer
		frontOutlineVertexBuffer.position(0);           // Rewind

		// Setup vertex array buffer. Vertices in float. A float has 4 bytes
		ByteBuffer leftOutlineByteBuffer = ByteBuffer.allocateDirect(leftOutlineVertices.length * 4);
		leftOutlineByteBuffer.order(ByteOrder.nativeOrder());
		leftOutlineVertexBuffer = leftOutlineByteBuffer.asFloatBuffer();
		leftOutlineVertexBuffer.put(leftOutlineVertices);
		leftOutlineVertexBuffer.position(0);

		// Setup vertex array buffer. Vertices in float. A float has 4 bytes
		ByteBuffer topOutlineByteBuffer = ByteBuffer.allocateDirect(topOutlineVertices.length * 4);
		topOutlineByteBuffer.order(ByteOrder.nativeOrder());
		topOutlineVertexBuffer = topOutlineByteBuffer.asFloatBuffer();
		topOutlineVertexBuffer.put(topOutlineVertices);
		topOutlineVertexBuffer.position(0);
	}

	// Render the shape
	public void draw(GL10 gl, boolean active) {
		
		float intensity = active ? 1.0f : 0.5f;

		// Enable vertex-array and define its buffer
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		
		gl.glColor4f(intensity, 0.0f, 0.0f, 1.0f);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, frontVertexBuffer);
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, frontVertices.length / 3);

		gl.glColor4f(0.0f, intensity, 0.0f, 1.0f);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, leftVertexBuffer);
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, leftVertices.length / 3);

		gl.glColor4f(intensity, intensity, intensity, 1.0f);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, topVertexBuffer);
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, topVertices.length / 3);
		
		

		gl.glLineWidth(10.0f);
		gl.glColor4f(intensity, intensity, intensity, 1.0f);
		
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, frontOutlineVertexBuffer);
		gl.glDrawArrays(GL10.GL_LINE_LOOP, 0, frontOutlineVertices.length / 3);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, topOutlineVertexBuffer);
		gl.glDrawArrays(GL10.GL_LINE_LOOP, 0, topOutlineVertices.length / 3);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, leftOutlineVertexBuffer);
		gl.glDrawArrays(GL10.GL_LINE_LOOP, 0, leftOutlineVertices.length / 3);
	

		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
	}
}
