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

import javax.microedition.khronos.opengles.GL10;

import org.opencv.core.Scalar;

/*
 * A square drawn in 2 triangles (using TRIANGLE_STRIP).
 */
public class GLArrow {
		
	public enum Amount { QUARTER_TURN, HALF_TURN };

	// Buffer for vertex-array
	private FloatBuffer bodyVertexBuffer;
	private FloatBuffer bodyOutlineVertexBuffer;
//	private FloatBuffer stripeVertexBuffer;
	
	private float[] bodyVertices = new float[91 * 6];
	private float[] bodyOutlineVertices = new float[91 * 6];  // 0 to 545
//	private float[] stripeVertices = new float[91 * 6];


	/**
	 * Constructor - Setup the vertex buffer
	 * 
	 * @param amount
	 */
	public GLArrow(Amount amount) {
		
		double angleScale = (amount == Amount.QUARTER_TURN) ? 1.0 : 3.0;
		
		for(int i=0; i<91; i++)  {
			
			// Angle wiil range from 0 to 90, or 0 to 180.
			double angleRads = i * angleScale * Math.PI / 180.0;
			float x = (float) Math.cos(angleRads);
			float y = (float) Math.sin(angleRads);
			
			bodyVertices[i*6 + 0] = x;
			bodyVertices[i*6 + 1] = y;
			bodyVertices[i*6 + 2] = -1.0f * calculateWidth(angleRads);

			bodyVertices[i*6 + 3] = x;
			bodyVertices[i*6 + 4] = y;
			bodyVertices[i*6 + 5] = calculateWidth(angleRads);
			
			
//			stripeVertices[i*6 + 0] = 1.1f * x;
//			stripeVertices[i*6 + 1] = 1.1f * y;
//			stripeVertices[i*6 + 2] = -0.5f * calculateWidth(angleRads);
//
//			stripeVertices[i*6 + 3] = 1.1f * x;
//			stripeVertices[i*6 + 4] = 1.1f * y;
//			stripeVertices[i*6 + 5] = +0.5f * calculateWidth(angleRads);			
			
			
			// Fill from 0 to 272
			bodyOutlineVertices[i*3 + 0] = x;
			bodyOutlineVertices[i*3 + 1] = y;
			bodyOutlineVertices[i*3 + 2] = -1.0f * calculateWidth(angleRads);

			// Fill from 545 to 273
			bodyOutlineVertices[(180 - i)*3 + 3] = x;
			bodyOutlineVertices[(180 - i)*3 + 4] = y;
			bodyOutlineVertices[(180 - i)*3 + 5] = calculateWidth(angleRads);
		}
		
		
		// Setup vertex array buffer. Vertices in float. A float has 4 bytes
		ByteBuffer bodyVbb = ByteBuffer.allocateDirect(bodyVertices.length * 4);
		bodyVbb.order(ByteOrder.nativeOrder());     // Use native byte order
		bodyVertexBuffer = bodyVbb.asFloatBuffer(); // Convert from byte to float
		bodyVertexBuffer.put(bodyVertices);         // Copy data into buffer
		bodyVertexBuffer.position(0);               // Rewind

//		// Setup vertex array buffer. Vertices in float. A float has 4 bytes
//		ByteBuffer stripeVbb = ByteBuffer.allocateDirect(stripeVertices.length * 4);
//		stripeVbb.order(ByteOrder.nativeOrder());     // Use native byte order
//		stripeVertexBuffer = stripeVbb.asFloatBuffer(); // Convert from byte to float
//		stripeVertexBuffer.put(stripeVertices);         // Copy data into buffer
//		stripeVertexBuffer.position(0);               // Rewind
		
		ByteBuffer bodyOutlineVbb = ByteBuffer.allocateDirect(bodyOutlineVertices.length * 4);
		bodyOutlineVbb.order(ByteOrder.nativeOrder());
		bodyOutlineVertexBuffer = bodyOutlineVbb.asFloatBuffer();
		bodyOutlineVertexBuffer.put(bodyOutlineVertices);
		bodyOutlineVertexBuffer.position(0);
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
    	
    	// Arrow Body
    	if(angleDegrees > 20.0)
    		return 0.3f;
    	
    	// Arrow Head
    	else
    		return (float) (angleDegrees / 20.0 * 0.6);
    }
    

	// Render the shape
	public void draw(GL10 gl, Scalar color) {
		
		// Enable vertex-array and define its buffer
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);

		// Draw Outline First
		gl.glColor4f(0.0f, 0.0f, 0.0f, 1.0f);
		gl.glLineWidth(10.0f);

		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, bodyOutlineVertexBuffer);
		// Draw the primitives from the vertex-array directly
		gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, bodyOutlineVertices.length / 3);

		
//		// Draw Stripe
//		gl.glColor4f(0.0f, 0.0f, 0.0f, 1.0f);
//		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, stripeVertexBuffer);
//		// Draw the primitives from the vertex-array directly
//		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, stripeVertices.length / 3);
		

		// Draw Body a bit darker
		gl.glEnable(GL10.GL_CULL_FACE);
		gl.glCullFace(GL10.GL_BACK);
		gl.glColor4f((float)color.val[0] / (256.0f + 128.0f), (float)color.val[1] / (256.0f + 128.0f), (float)color.val[2] / (256.0f + 128.0f), 1.0f);

		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, bodyVertexBuffer);
		// Draw the primitives from the vertex-array directly
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, bodyVertices.length / 3);

		gl.glDisable(GL10.GL_BACK);

		
		// Draw Body normal brightness
		gl.glCullFace(GL10.GL_FRONT);
		gl.glColor4f((float)color.val[0] / 256.0f, (float)color.val[1] / 256.0f, (float)color.val[2] / 256.0f, 1.0f);

		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, bodyVertexBuffer);
		// Draw the primitives from the vertex-array directly
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, bodyVertices.length / 3);
		
		gl.glDisable(GL10.GL_FRONT);

		
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
	}
}
