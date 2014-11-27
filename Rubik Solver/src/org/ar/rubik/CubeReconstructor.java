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
 *   Cube location and orientation in GL space coordinates are reconstructed from Face information.
 *   
 *   This class implements the OpenCV Frame Listener. 
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
package org.ar.rubik;

import org.ar.rubik.RubikFace.FaceRecognitionStatusEnum;



/**
 * 
 * 
 * @author android.steve@testlens.com
 *
 */
public class CubeReconstructor {
	
	public float scale;
	public float x;
	public float y;
	public float cubeYrotation;  // degrees
	public float cubeXrotation;  // degrees

	/**
	 * =+= this is temporary code
	 * 
	 * This function actually calculates, currently rather crudely, a 2D to 3D translation.
	 * That is, information from the Rubik Face object is used to deduce the 
	 * true location in OpenGL space of the cube and it's orientation.  
	 * 
	 * @param rubikFace
	 */
    public void reconstruct(RubikFace rubikFace) {
    	
		final float opecnCL2opencvRatio = 100.0f;
		final float xOffset = 650.0f;
		final float yOffset = 200.0f;
		
		if(rubikFace == null)
			return;
		
		if(rubikFace.faceRecognitionStatus != FaceRecognitionStatusEnum.SOLVED)
			return;
		
		LeastMeansSquare lmsResult = rubikFace.lmsResult;
		
		if(lmsResult == null)
			return;
		
				
		// This is very crude.
		this.scale = (float) Math.sqrt(Math.abs(rubikFace.alphaLatticLength * rubikFace.betaLatticLength)) / 70.0f;
		
		// =+= not necessarily correct, really should use X, Y rotations
		this.x = (float) ((lmsResult.origin.x - xOffset) / opecnCL2opencvRatio);
		this.y = (float) (-1 * (lmsResult.origin.y - yOffset) / opecnCL2opencvRatio);
		
		float alpha = 90.0f - (float) (rubikFace.alphaAngle * 180.0 / Math.PI);
		float beta = (float) (rubikFace.betaAngle * 180.0 / Math.PI) - 90.0f;
		
		
		// Very crude estimations of orientation.  These equations and number found empirically.
		// =+= We require a solution of two non-linear equations and two unknowns to correctly calculate
		// =+= X and Y 3D rotation values from 2D alpha and beta values.  Probably use of Newton successive
		// =+= approximation will produce good results.
		this.cubeYrotation = 45.0f + (alpha - beta) / 2.0f;
		this.cubeXrotation =  90.0f + ( (alpha - 45.0f) + (beta - 45.0f) )/ -0.5f;
    }

}
