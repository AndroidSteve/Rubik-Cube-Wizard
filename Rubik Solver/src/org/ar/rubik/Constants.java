/**
 * Augmented Reality Rubik Cube Wizard
 * 
 * Author: Steven P. Punte (aka Android Steve : android.steve@cl-sw.com)
 * Date:   April 25th 2015
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
package org.ar.rubik;

import org.opencv.core.Core;
import org.opencv.core.Scalar;


public class Constants {
    
    // Turn on and off all Log Cat logging (=+= not yet completed)
    public final static boolean LOGGER = true;

    // General Log Cat Tag
	public static final String TAG = "RubikWizard";

	// Log Cat Tag for State Machine related diagnostics
	public static final String TAG_STATE = "RubikState";
	
	// Log Cat Tag for Color Recognition diagnostics
    public static final String TAG_COLOR = "RubikColor";
	
	// Log Cat Tag for OpenGL related diagnostics.
    public static final String TAG_OPENGL = "RubikOpenGL";
	
	// Log Cat Tag for Camera Calibration related diagnostics.
    public static final String TAG_CAL = "RubikCal";
    
    // Log Cat for Kalman Filter related diagnostics
	public static final String TAG_KALMAN = "RubikKalman";

    
	public enum AppStateEnum { 
		START,         // Ready
		GOT_IT,        // A Cube Face has been recognized and captured.
		ROTATE_CUBE,   // Request user to rotate Cube.
		SEARCHING,     // Attempting to lock onto new Cube Face.
		COMPLETE,      // All six faces have been captured, and we seem to have valid color.
		BAD_COLORS,    // All six faces have been captured, but we do not have properly nine tiles of each color.
		VERIFIED,      // Two Phase solution has verified that the cube tile/colors/positions are a valid cube.
		WAIT_TABLES,   // Waiting for TwoPhase Prune Tree generation to complete.
		INCORRECT,     // Two Phase solution could not produce a solution; see error code.
		ERROR,         // Two Phase solution has analyzed the cube and found it to be invalid.
		SOLVED,        // Two Phase solution has analyzed the cube and found a solution.
		ROTATE_FACE,   // Inform user to perform a face rotation
		WAITING_MOVE,  // Wait for face rotation to complete
		DONE           // Cube should be completely physically solved.
	};

	public enum GestureRecogniztionStateEnum { 
		UNKNOWN, // No face recognition
		PENDING, // A particular face seems to becoming stable.
		STABLE,  // A particular face is stable.
		NEW_STABLE, // A new face is stable.
		PARTIAL  // A particular face seems to becoming unstable.
	};

	// Specifies where image comes from
	public enum ImageSourceModeEnum { NORMAL, SAVE_NEXT, PLAYBACK}

	// Specifies what to do with image
	public enum ImageProcessModeEnum { DIRECT, MONOCHROMATIC, GREYSCALE, GAUSSIAN, CANNY, DILATION, CONTOUR, POLYGON, RHOMBUS, FACE_DETECT, NORMAL}

	// Specifies what annotation to add
	public enum AnnotationModeEnum { LAYOUT, RHOMBUS, FACE_METRICS, TIME, COLOR_FACE, COLOR_CUBE, CUBE_METRICS, NORMAL }

	// Conventional Rubik Face nomenclature
	public enum FaceNameEnum { UP, DOWN, LEFT, RIGHT, FRONT, BACK};
	
	
	/**
	 * Color Tile Enum
	 * 
	 * This one class serves as both a collection of colors and the values used by various activities
	 * throughout the application, and as a Tile type (more specifically enumeration) that 
	 * the RubikFace class can reference to for each of the nine tiles on each face.
	 * 
	 * Each enumerated color value possess three values: 
	 * - openCV of type Scalar
	 * - openGL of type float[4]
	 * - symbol of type char 
	 * 
	 * @author android.steve@cl-sw.com
	 *
	 */
	public enum ColorTileEnum {
	    //                     Target Measurement Colors                   Graphics (both CV and GL)  
	    RED   ( true, 'R', new Scalar(220.0,   20.0,  30.0), new float [] {1.0f, 0.0f, 0.0f, 1.0f}),	    
	    ORANGE( true, 'O', new Scalar(240.0,   80.0,   0.0), new float [] {0.9f, 0.4f, 0.0f, 1.0f}),
	    YELLOW( true, 'Y', new Scalar(230.0,  230.0,  20.0), new float [] {0.9f, 0.9f, 0.2f, 1.0f}),
	    GREEN ( true, 'G', new Scalar(0.0,    140.0,  60.0), new float [] {0.0f, 1.0f, 0.0f, 1.0f}),
	    BLUE  ( true, 'B', new Scalar(0.0,     60.0, 220.0), new float [] {0.2f, 0.2f, 1.0f, 1.0f}),
        WHITE ( true, 'W', new Scalar(225.0,  225.0, 225.0), new float [] {1.0f, 1.0f, 1.0f, 1.0f}),
        
        BLACK (false, 'K', new Scalar(  0.0,    0.0,   0.0) ),    
        GREY  (false, 'E', new Scalar( 50.0,   50.0,  50.0) );
        
	    
	    // A Rubik Color
	    public final boolean isRubikColor;
	    
	    // Measuring and Decision Testing in OpenCV
	    public final Scalar rubikColor;
	    
	    // Rendering in OpenCV
        public final Scalar cvColor;
        
        // Rendering in OpenGL
        public final float [] glColor;
        
        // Single letter character
        public final char symbol;
        
        /**
         * Color Tile Enum Constructor
         * 
         * Accept an Rubik Color and derive OpenCV and OpenGL colors from this.
         * 
         * @param isRubik
         * @param symbol
         * @param rubikColor
         */
        private ColorTileEnum(boolean isRubik, char symbol, Scalar rubikColor) {
            this.isRubikColor = isRubik;
            this.cvColor = rubikColor;
            this.rubikColor = rubikColor;
            this.glColor =  new float [] {(float)rubikColor.val[0] / 255f, (float)rubikColor.val[1] / 255f, (float)rubikColor.val[2] / 255f, 1.0f};  
            this.symbol = symbol;
        }
        
        
        /**
         * Color Tile Enum Constructor
         * 
         * Accept an Rubik Color and an OpenGL color.  Derive OpenCV color from OpenGL color.
         * 
         * @param isRubik
         * @param symbol
         * @param rubikColor
         */
        private ColorTileEnum(boolean isRubik, char symbol, Scalar rubikColor, float[] renderColor) {
            this.isRubikColor = isRubik;
            this.cvColor = new Scalar(renderColor[0] * 255, renderColor[1] * 255, renderColor[2] * 255);
            this.rubikColor = rubikColor;
            this.glColor =  renderColor;
            this.symbol = symbol;
        }

	}

	// Any OpenCV font
	public final static int FontFace = Core.FONT_HERSHEY_PLAIN;

}
