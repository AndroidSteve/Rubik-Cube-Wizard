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
 *   This file global configuration and parameters used throughout the Rubik Cube Wizard
 *   program, and also the menu processing system to support the associated UI.
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

import org.ar.rubik.Constants.AnnotationModeEnum;
import org.ar.rubik.Constants.ImageProcessModeEnum;
import org.ar.rubik.Constants.ImageSourceModeEnum;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;




/**
 * @author android.steve@cl-sw.com
 *
 */
public class MenuAndParams {

	/**
	 * Application Mode Control
	 * 
	 * These mode controls are typically used for development, debugging, and analysis.
	 */
	
	// Toggles User Text Interface
	public static boolean userTextDisplay = true;
	
    // Toggle Cube Overlay
    public static boolean cubeOverlayDisplay = false;

    // Toggle Face Overlay
    public static boolean faceOverlayDisplay = true;

    // Toggle Face Overlay
    public static boolean pilotCubeDisplay = true;
        
    // Specifies where image comes from
    public static ImageSourceModeEnum imageSourceMode = ImageSourceModeEnum.NORMAL;

	// Specifies what to do with image
    public static ImageProcessModeEnum imageProcessMode = ImageProcessModeEnum.NORMAL;
    
    // Specified what annotation to add
    public static AnnotationModeEnum annotationMode = AnnotationModeEnum.NORMAL;
	
	
	
	/**
	 * User Adjustable Parameters 
	 * 
	 * These typically have been found empirically with respect to what works best.
	 */
    
    // Manual offset to Pose Estimator (i.e., opencv solvepnp()) results.
    public static RubikMenuParam xRotationOffsetParam        = new RubikMenuParam("X Rotation Offset",         -20.0,  +20.0,   +0.0);
    public static RubikMenuParam yRotationOffsetParam        = new RubikMenuParam("Y Rotation Offset",         -20.0,  +20.0,   +0.0);
    public static RubikMenuParam zRotationOffsetParam        = new RubikMenuParam("Z Rotation Offset",         -20.0,  +20.0,   +0.0);
    public static RubikMenuParam xTranslationOffsetParam     = new RubikMenuParam("X Translation Offset",      -2.0,    +2.0,   +0.2);
    public static RubikMenuParam yTranslationOffsetParam     = new RubikMenuParam("Y Translation Offset",      -2.0,    +2.0,   -0.1);
    public static RubikMenuParam zTranslationOffsetParam     = new RubikMenuParam("Z Translation Offset",      -2.0,    +2.0,   +0.0);
    public static RubikMenuParam scaleOffsetParam            = new RubikMenuParam("Scale Offset",              +0.5,    +2.0,   +1.4);

    
	// Gaussian Blur Kernal Size
	public static RubikMenuParam gaussianBlurKernelSizeParam = new RubikMenuParam("Gaussian Blur Kernel Size",  +3.0,  +20.0,   +7.0);
		
	// Canny Edge Detector Upper Threshold Parameter
	public static RubikMenuParam cannyUpperThresholdParam    = new RubikMenuParam("Canny Upper Threshold",     +50.0, +200.0, +100.0);
	
	// Canny Edge Detector Lower Threshold Parameter
	public static RubikMenuParam cannyLowerThresholdParam    = new RubikMenuParam("Canny Lower Threshold",     +20.0, +100.0,  +50.0);
	
	// Dilation Kernel Size Parameter
	public static RubikMenuParam dilationKernelSizeParam     = new RubikMenuParam("Dilation Kernel Size",       +2.0,  +20.0,  +10.0);
	
	// Minimum contour area size to be considered a tile candidate.
	public static RubikMenuParam minimumContourAreaParam     = new RubikMenuParam("Minimum Contour Area Size", +10.0, +500.0, +100.0);
	
	// Polygon Epsilon Parameter
	public static RubikMenuParam polygonEpsilonParam         = new RubikMenuParam("Polygon Epsilon Threshold", +10.0, +100.0,  +30.0);
	
	// Minimum contour area size to be considered a tile candidate.
	public static RubikMenuParam minimumRhombusAreaParam     = new RubikMenuParam("Minimum Parallelogram Area Size", +0.0, +2000.0, +1000.0);
	
	// Minimum contour area size to be considered a tile candidate.
	public static RubikMenuParam maximumRhombusAreaParam     = new RubikMenuParam("Maximum Parallelogram Area Size", +0.0, +20000.0, +10000.0);
	
	// Parallelogram Angle Outlier Threshold
	public static RubikMenuParam angleOutlierThresholdPaaram = new RubikMenuParam("Angle Outlier Threshold",         +1.0,     +20.0, +10.0);
	
	// Face Least Means Square Threshold
	public static RubikMenuParam faceLmsThresholdParam       = new RubikMenuParam("Face LMS Threshold",              +5.0,    +150.0, +35.0);
	

	/**
	 * Rubik Menu Parameters
	 * 
	 * This class is to support a user adjustable parameter.
	 * 
	 * @author android.steve@cl-sw.com
	 *
	 */
	public static class RubikMenuParam {

		/**
		 * @param label
		 * @param min
		 * @param max
		 * @param value
		 */
        public RubikMenuParam(String label, double min, double max, double value) {
	        this.label = label;
	        this.min   = min;
	        this.max   = max;
	        this.value = value;
        }
        
		public  double value;	
		private double min;
		private double max;
		private String label;
	}
	
	
	/**
	 * On Options Item Selected
	 * 
	 * This receives and processes every menu action
	 * 
	 * @param item
	 * @param ma
	 * @return
	 */
	public static boolean onOptionsItemSelected(MenuItem item, AndroidActivity ma) {

		Log.i(Constants.TAG, "called onOptionsItemSelected; selected item: " + item);


		switch (item.getItemId()) {  

		// Image Source Mode
		case R.id.saveImageMenuItem:
			imageSourceMode = ImageSourceModeEnum.SAVE_NEXT;
			return true;

		case R.id.useSavedImageMenuItem:
			imageSourceMode = ImageSourceModeEnum.PLAYBACK;
			return true;

		case R.id.directImageProcessMenuItem:
			imageProcessMode = ImageProcessModeEnum.DIRECT;
			return true;

		case R.id.greyscaleImageProcessMenuItem:
			imageProcessMode = ImageProcessModeEnum.GREYSCALE; 
			return true;

		case R.id.boxBlurImageProcessMenuItem:
			imageProcessMode = ImageProcessModeEnum.GAUSSIAN; 
			return true;

		case R.id.cannyImageProcessMenuItem:
			imageProcessMode = ImageProcessModeEnum.CANNY;
			return true;

		case R.id.dialateImageProcessMenuItem:
			imageProcessMode = ImageProcessModeEnum.DILATION;
			return true;

		case R.id.contourImageProcessMenuItem:
			imageProcessMode = ImageProcessModeEnum.CONTOUR; 
			return true;

		case R.id.ploygoneProcessMenuItem:
			imageProcessMode = ImageProcessModeEnum.POLYGON; 
			return true;

		case R.id.rhombusProcessMenuItem:
			imageProcessMode = ImageProcessModeEnum.RHOMBUS; 
			return true;

		case R.id.faceRecognitionMenuItem:
			imageProcessMode = ImageProcessModeEnum.FACE_DETECT; 
			return true;

		case R.id.normalProcessMenuItem:
			imageProcessMode = ImageProcessModeEnum.NORMAL; 
			return true;


			// Annotation Mode
		case R.id.normalAnnotationMenuItem:
			annotationMode = AnnotationModeEnum.NORMAL;
			break;

		case R.id.layoutAnnotationMenuItem:
			annotationMode = AnnotationModeEnum.LAYOUT;
			break;

		case R.id.rhombusAnnotationMenuItem:
			annotationMode = AnnotationModeEnum.RHOMBUS;
			break;

		case R.id.faceMetricsAnnotationMenuItem:
			annotationMode = AnnotationModeEnum.FACE_METRICS;
			break;

		case R.id.cubeMetricsAnnotationMenuItem:
			annotationMode = AnnotationModeEnum.CUBE_METRICS;
			break;

		case R.id.timeAnnotationMenuItem:
			annotationMode = AnnotationModeEnum.TIME;
			if(ma.stateModel.activeRubikFace != null)
				ma.stateModel.activeRubikFace.profiler.reset();
			break;

        case R.id.faceColorAnnotationMenuItem:
            annotationMode = AnnotationModeEnum.COLOR_FACE;
            break;

        case R.id.cubeColorAnnotationMenuItem:
            annotationMode = AnnotationModeEnum.COLOR_CUBE;
            break;


			// Adjustable Parameters
		case R.id.xRotationOffsetMenuItem:
		    seekerDialogNew(xRotationOffsetParam, ma);
		    break;

		case R.id.yRotationOffsetMenuItem:
		    seekerDialogNew(yRotationOffsetParam, ma);
		    break;

		case R.id.zRotationOffsetMenuItem:
		    seekerDialogNew(zRotationOffsetParam, ma);
		    break;

		case R.id.xTranslationOffsetMenuItem:
		    seekerDialogNew(xTranslationOffsetParam, ma);
		    break;

		case R.id.yTranslationOffsetMenuItem:
		    seekerDialogNew(yTranslationOffsetParam, ma);
		    break;

        case R.id.zTranslationOffsetMenuItem:
            seekerDialogNew(zTranslationOffsetParam, ma);
            break;
            
        case R.id.scaleOffsetMenuItem:
            seekerDialogNew(scaleOffsetParam, ma);
            break;

		case R.id.boxBlurKernelSizeMenuItem:
			seekerDialogNew(gaussianBlurKernelSizeParam, ma);
			break;

		case R.id.cannyLowerThresholdMenuItem:
			seekerDialogNew(cannyLowerThresholdParam, ma);
			break;

		case R.id.cannyUpperThresholdMenuItem:
			seekerDialogNew(cannyUpperThresholdParam, ma);
			break;

		case R.id.dilationKernelMenuItem:
			seekerDialogNew(dilationKernelSizeParam, ma);
			break;
			
		case R.id.minimumContourAreaSizelMenuItem:
			seekerDialogNew(minimumContourAreaParam, ma);
			break;

		case R.id.polygonEpsilonMenuItem:
			seekerDialogNew(polygonEpsilonParam, ma);
			break;
			
		case R.id.minimumRhombusAreaSizelMenuItem:
			seekerDialogNew(minimumRhombusAreaParam, ma);
			break;
			
		case R.id.maximumRhombusAreaSizelMenuItem:
			seekerDialogNew(maximumRhombusAreaParam, ma);
			break;
			
		case R.id.angleOutlierThresholdMenuItem:
			seekerDialogNew(angleOutlierThresholdPaaram, ma);
			break;
			
		case R.id.faceLmsThresholdMenuItem:
			seekerDialogNew(faceLmsThresholdParam, ma);
			break;


			// Miscellaneous
		case R.id.saveCubeMenuItem:
			ma.stateModel.saveState();
			break;

		case R.id.recallCubeMenuItem:
			ma.appStateMachine.recallState();
			break;

		case R.id.resetImageMenuItem:
			ma.appStateMachine.reset();
			break;

		case R.id.exitImageMenuItem:
			ma.finish();
			System.exit(0);
			break;

		case R.id.toggleUserTextMenuItem:
			userTextDisplay ^= true;
			break;

        case R.id.toggleFaceOverlayMenuItem:
            faceOverlayDisplay ^= true;
            break;

        case R.id.toggleCubeOverlayMenuItem:
            cubeOverlayDisplay ^= true;
            break;

        case R.id.togglePilotCubeMenuItem:
            pilotCubeDisplay ^= true;
            break;
		}

		return true;
	}
	
	
	
	/**
	 * Pop-up like slider bar to adjust various parameters.
	 * 
	 * @param rubikMenuParam
	 * @param context
	 */
	private static void seekerDialogNew(final RubikMenuParam rubikMenuParam, Context context) {
		
		// get prompts.xml view
		LayoutInflater li = LayoutInflater.from(context);
		View promptsView = li.inflate(R.layout.prompts, null);

		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

		// set prompts.xml to alertdialog builder
		alertDialogBuilder.setView(promptsView);

		// create alert dialog
		AlertDialog alertDialog = alertDialogBuilder.create();

		// show it
		alertDialog.show();
		
		double value = rubikMenuParam.value;
		
		TextView paramTitleTextView = (TextView)(promptsView.findViewById(R.id.param_title_text_view));
		paramTitleTextView.setText(rubikMenuParam.label);

		final TextView paramValueTextView = (TextView) promptsView.findViewById(R.id.param_value_text_view);
		paramValueTextView.setText(String.format("%5.1f", value));

		SeekBar seekBar = (SeekBar) promptsView.findViewById(R.id.parameter_seekbar);
		seekBar.setMax(100);
		
		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

				double newParamValue = (rubikMenuParam.max - rubikMenuParam.min) * ((double)progress) / 100.0 + rubikMenuParam.min;
				paramValueTextView.setText(String.format("%5.1f", newParamValue));
				
				rubikMenuParam.value = newParamValue;
			}
		});
		seekBar.setProgress( (int) (100.0 * (value - rubikMenuParam.min) / (rubikMenuParam.max - rubikMenuParam.min)) );
	}
}
