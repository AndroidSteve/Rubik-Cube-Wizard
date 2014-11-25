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
 *   This is the primary Android Activity class for this application.  Intentionally,
 *   as little is done as possible in this class.  Work is partitioned and 
 *   handled in other classes.
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


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.ar.rubik.gl.AnnotationGLRenderer;
import org.ar.rubik.gl.PilotGLRenderer;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.FrameLayout;


public class AndroidActivity extends Activity implements CvCameraViewListener2 {

	// Camera Object
    private CameraBridgeViewBase mOpenCvCameraView;
    
    // Surface for rending Pilot (i.e. Direction Arrows)
	private GLSurfaceView pilotGLSurfaceView;
	
	// Surface for rendering graphic annotations
	private GLSurfaceView annotationGLSurfaceView;
	
	// Top Level Controller of this application
    public DeprecatedController controller;  // =+= some menu actions need this.
    
    // Primary Image Processor
    public ImageRecognizer imageRecognizer;
    
    // Primary Application Controller
    public AppStateMachine appStateMachine;
    
    // Primary Application State
    public StateModel stateModel;
    
    // Once an exception or error is encountered, display message from thence forth.
	Mat errorImage = null;
    
    // Loads Open CL program
    static {
        System.loadLibrary("step");
    }
    
    
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(Constants.TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };



    /**
     * Constructor
     * 
     * Instantiate primary components of application.
     */
    public AndroidActivity() {
    	
        Log.i(Constants.TAG, "Instantiated new " + this.getClass());
        
        // Construct and associate Primary Components (i.e., "Objects") for this application.
        stateModel = new StateModel();
        appStateMachine = new AppStateMachine(stateModel);
        imageRecognizer = new ImageRecognizer(appStateMachine, stateModel);
        
    	/*
    	 * Launch thread to asynchronous, and probably in a different CPU, calculate
    	 * Two Phase Prune Tables.  These tables require 150 Mbytes of RAM and take
    	 * about 15 seconds to compute.  They are required by the Two Phase algorithm
    	 * to compute a solution for a valid Rubik Cube.
    	 * 
    	 * =+= Normally, AsyncTask should be instantiated only on the UI thread.  
    	 * =+= Which thread are we on?
    	 */
    	new Util.LoadPruningTablesTask().execute(appStateMachine);
    }

    
    /** 
     * Called when the activity is first created.
     * 
     * (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @SuppressWarnings("unused")
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
        Log.i(Constants.TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.surface_view);
        FrameLayout layout = (FrameLayout) findViewById(R.id.activity_frame_layout);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.activity_surface_view); 
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        if(Constants.refactor == true) {
        	mOpenCvCameraView.setCvCameraViewListener(imageRecognizer);
        }

        else {
        	// Setup and Add Pilot GL Surface View and Pilot GL Renderer
        	pilotGLSurfaceView = new GLSurfaceView(this);
        	pilotGLSurfaceView.getHolder().setFormat(PixelFormat.TRANSPARENT);
        	pilotGLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        	pilotGLSurfaceView.setZOrderOnTop(true);
        	pilotGLSurfaceView.setLayoutParams(
        			new FrameLayout.LayoutParams(
        					FrameLayout.LayoutParams.MATCH_PARENT,
        					FrameLayout.LayoutParams.MATCH_PARENT));
        	layout.addView(pilotGLSurfaceView);
        	PilotGLRenderer pilotGlRenderer = new PilotGLRenderer(this);
        	pilotGLSurfaceView.setRenderer(pilotGlRenderer);


        	// Setup and add Annotation GL Surface View and Annotation GL Renderer
        	annotationGLSurfaceView = new GLSurfaceView(this);
        	annotationGLSurfaceView.getHolder().setFormat(PixelFormat.TRANSPARENT);
        	annotationGLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        	annotationGLSurfaceView.setZOrderOnTop(true);
        	annotationGLSurfaceView.setLayoutParams(
        			new FrameLayout.LayoutParams(
        					FrameLayout.LayoutParams.MATCH_PARENT,
        					FrameLayout.LayoutParams.MATCH_PARENT));
        	layout.addView(annotationGLSurfaceView);
        	AnnotationGLRenderer annotationGlRenderer = new AnnotationGLRenderer(this);
        	annotationGLSurfaceView.setRenderer(annotationGlRenderer);


        	// Instantiate Controller Objet
        	controller = new DeprecatedController(pilotGlRenderer, annotationGlRenderer);
        }

        // =+= Currently obsolete, but is used to support OpenCL
        MonoChromatic.initOpenCL(getOpenCLProgram());

    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        if(pilotGLSurfaceView != null)
        	pilotGLSurfaceView.onPause();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if(pilotGLSurfaceView != null)
        	pilotGLSurfaceView.onResume();
        if(annotationGLSurfaceView != null)
        	annotationGLSurfaceView.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
//        if(glSurfaceView != null)
//        	glSurfaceView.destroyDrawingCache();
        MonoChromatic.shutdownOpenCL();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(Constants.TAG, "called onCreateOptionsMenu");
        
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    
    /**
     *  (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	return MenuAndParams.onOptionsItemSelected(item, this);
    }


    
	/**
	 *  (non-Javadoc)
	 * @see org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2#onCameraViewStarted(int, int)
	 */
	public void onCameraViewStarted(int width, int height) {
    }

	
    /**
     *  (non-Javadoc)
     * @see org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2#onCameraViewStopped()
     */
    public void onCameraViewStopped() {
    }

    
    /**
     * On Camera Frame
     * 
     * This is the main event point for the entire application.
     * 
     *  (non-Javadoc)
     * @see org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2#onCameraFrame(org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame)
     */
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
    	
    	Mat rgba = inputFrame.rgba();
		Size imageSize = rgba.size();
   	
    	if(errorImage != null)
    		return errorImage;
    	  	
    	Mat resultImage = null;
    	// =+= problem: can't make toast in frame thread.
        try {
	        resultImage = controller.onCameraFrame(
	        		inputFrame, 
	        		MenuAndParams.imageSourceMode, 
	        		MenuAndParams.imageProcessMode, 
	        		MenuAndParams.annotationMode);
	        
        } catch (CvException e) {
        	e.printStackTrace();        	
			errorImage = new Mat(imageSize, CvType.CV_8UC4);
			Core.putText(errorImage, e.getMessage(), new Point(50, 50), Constants.FontFace, 2, Constants.ColorWhite, 2);
        } catch (Exception e) {
        	e.printStackTrace();        	
			errorImage = new Mat(imageSize, CvType.CV_8UC4);
			Core.putText(errorImage, e.getMessage(), new Point(50, 50), Constants.FontFace, 2, Constants.ColorWhite, 2);
        }
    	
    	return resultImage;
    }
    
    
    /**
     * 
     * @return
     */
    private String getOpenCLProgram ()
    {
        /* OpenCL program text is stored in a separate file in
         * assets directory. Here you need to load it as a single
         * string.
         *
         * In fact, the program may be directly built into
         * native source code where OpenCL API is used,
         * it is useful for short kernels (few lines) because it doesn't
         * involve loading code and you don't need to pass it from Java to
         * native side.
         */

        try
        {
            StringBuilder buffer = new StringBuilder();
            InputStream stream = getAssets().open("step.cl");
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String s;

            while((s = reader.readLine()) != null)
            {
                buffer.append(s);
                buffer.append("\n");
            }

            reader.close();
            return buffer.toString();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return "";
        
    }
}
