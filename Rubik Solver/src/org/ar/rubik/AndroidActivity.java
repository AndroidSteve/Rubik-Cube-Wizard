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
 *   handled in other classes.  The key actions here are to:
 *   1)  Obtains the OpenCV JavaCameraView
 *   2)  Create and attach a frame listener: i.e., ImageRecognizer.
 *   3)  Create and attach to GL renderers.
 *   
 *   Note, OpenCL is supported, but not actually in use (at least directly).
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

import org.ar.rubik.gl.GLRenderer2;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

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


public class AndroidActivity extends Activity {

	// Camera Object
    private CameraBridgeViewBase mOpenCvCameraView;

    // Surface for rendering graphics: arrows, pilot cube, overlay cube, etc...
 	private GLSurfaceView gLSurfaceView;
    
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
    
    
    /**
     * Base Loader Callback.
     * 
     * This informs us that OpenCV has successfully loaded.
     */
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
    	 * Normally, AsyncTask should be instantiated only on the UI thread, however,
    	 * this seems to work find.
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
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
        Log.i(Constants.TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        
        // Get Camera Parameters and compute OpenCL Intrinsic Camera Parameters.
        stateModel.cameraParameters = new CameraCalibration();
        
        // Don't allow screen to go dim or dark.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Set up display as per layout.xml
        setContentView(R.layout.surface_view);
        
        // Obtain Frame Layout object.
        FrameLayout frameLayout = (FrameLayout) findViewById(R.id.activity_frame_layout);

        // Obtain JavaCameraView object.
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.activity_surface_view); 
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(imageRecognizer);  // Image Recognizer is attached here.

        
        // Setup and Add GL Surface View and GL Renderer
        gLSurfaceView = new GLSurfaceView(this);
        gLSurfaceView.setEGLContextClientVersion(2);         // Create an OpenGL ES 2.0 context.
        gLSurfaceView.getHolder().setFormat(PixelFormat.TRANSPARENT);
        gLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        gLSurfaceView.setZOrderOnTop(true);
        gLSurfaceView.setLayoutParams(
        		new FrameLayout.LayoutParams(
        				FrameLayout.LayoutParams.MATCH_PARENT,
        				FrameLayout.LayoutParams.MATCH_PARENT));
        frameLayout.addView(gLSurfaceView);
        GLRenderer2 gLRenderer = new GLRenderer2(stateModel, this);
        gLSurfaceView.setRenderer(gLRenderer);

        // =+= Currently not in use, but is used to support OpenCL
        MonoChromatic.initOpenCL(getOpenCLProgram());
    }

    
    /* (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        if(gLSurfaceView != null)
        	gLSurfaceView.onPause();
    }

    
    /* (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
    public void onResume()
    {
        super.onResume();
        if(gLSurfaceView != null)
        	gLSurfaceView.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    
    /* (non-Javadoc)
     * @see android.app.Activity#onDestroy()
     */
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
//        if(glSurfaceView != null)
//        	glSurfaceView.destroyDrawingCache();
        MonoChromatic.shutdownOpenCL();
    }

    
    /* (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
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
     * Get OpenCL Program
     * 
     * File name is assumed to be "step.cl"
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
