package org.ar.rubik;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.ar.rubik.Constants.AnnotationModeEnum;
import org.ar.rubik.Constants.ImageProcessModeEnum;
import org.ar.rubik.Constants.ImageSourceModeEnum;
import org.ar.rubik.gl.AnnotationGLRenderer;
import org.ar.rubik.gl.PilotGLRenderer;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;


public class MainActivity extends Activity implements CvCameraViewListener2 {

	// Camera Object
    private CameraBridgeViewBase mOpenCvCameraView;
    
    // Surface for rending Pilot (i.e. Direction Arrows)
	private GLSurfaceView pilotGLSurfaceView;
	
	// Surface for rendering graphic annotations
	private GLSurfaceView annotationGLSurfaceView;
	
	// Top Level Controller of this application
    private Controller controller;


    // Monochromatic Image Process Parameters =+= obsolete
    public static double monochromaticSizeParam    = 8.0;
    public static double monochromaticEpsilonParam = 10.0;
    
    // Gaussian Blur Parameters 
    public static double boxBlurKernelSizeParam   = 10.0;
    
    // Canny Edge Detection Parameters
    public static double cannyLowerThresholdParam = 50.0;
    public static double cannyUpperThresholdParam = 100.0;
    
    // Dilation Kernel Size
    public static double dilationKernelSize       = 10.0;
    
    // Ploygone Detection Parameters
    public static double polygonEpsilonParam      = 30.0;
    
    public static double manualLuminousOffset     = 0.0;

	// Toggles User Text Interface
	public static boolean userTextDisplay = true;
	
	// Toggle Cube Overlay Display
	public static boolean cubeOverlayDisplay = false;
        
    // Specifies where image comes from
    public ImageSourceModeEnum imageSourceMode = ImageSourceModeEnum.NORMAL;

	// Specifies what to do with image
    public ImageProcessModeEnum imageProcessMode = ImageProcessModeEnum.FACE_DETECT;
    
    // Specified what annotation to add
    public AnnotationModeEnum annotationMode = AnnotationModeEnum.NORMAL;
    

    
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
     */
    public MainActivity() {
        Log.i(Constants.TAG, "Instantiated new " + this.getClass());
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
        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.surface_view);
        FrameLayout layout = (FrameLayout) findViewById(R.id.activity_frame_layout);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.activity_surface_view); 
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        
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
        controller = new Controller(pilotGlRenderer, annotationGlRenderer);
        
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Log.i(Constants.TAG, "called onOptionsItemSelected; selected item: " + item);


    	switch (item.getItemId()) {  

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
    		imageProcessMode = ImageProcessModeEnum.BOXBLUR; 
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
    		
    	case R.id.faceDetectionMenuItem:
    		imageProcessMode = ImageProcessModeEnum.FACE_DETECT; 
    		return true;
    		
       	case R.id.luminousOffsetMenuItem:
    		seekerDialog(
    				"Luminous Offset",
    				-50.0,
    				+50.0,
    				item.getItemId());
    		break;
    		
       	case R.id.boxBlurKernelSizeMenuItem:
    		seekerDialog(
    				"Box Blur Kernel Size",
    				3.0,
    				20.0,
    				item.getItemId());
    		break;
    		
       	case R.id.cannyLowerThresholdMenuItem:
    		seekerDialog(
    				"Canny Lower Threshold",
    				20.0,
    				100.0,
    				item.getItemId());
    		break;
    		
       	case R.id.cannyUpperThresholdMenuItem:
    		seekerDialog(
    				"Canny Upper Threshold",
    				50.0,
    				200.0,
    				item.getItemId());
    		break;
    		
       	case R.id.dilationKernelMenuItem:
    		seekerDialog(
    				"Dialation Kernel Size",
    				5.0,
    				20.0,
    				item.getItemId());
    		break;
    		
       	case R.id.polygonEpsilonMenuItem:
    		seekerDialog(
    				"Polygone Recognition Epsilon Accuracy",
    				10.0,
    				100.0,
    				item.getItemId());
    		break;
    		
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
       		break;
       		
       	case R.id.colorAnnotationMenuItem:
       		annotationMode = AnnotationModeEnum.COLOR;
       		break;
       		
       	case R.id.saveCubeMenuItem:
       		controller.saveCube();
       		break;
       		
       	case R.id.recallCubeMenuItem:
       		controller.recallCube();
       		break;
       		
       	case R.id.resetImageMenuItem:
       		controller.reset();
       		break;
    		
        case R.id.exitImageMenuItem:
    		finish();
    		System.exit(0);
    		break;
    		
        case R.id.toggleUserTextMenuItem:
        	userTextDisplay ^= true;
        	break;

        case R.id.toggleCubeOverlayMenuItem:
        	cubeOverlayDisplay ^= true;
        	break;
    	}
    		
        return true;
    }

    
    /**
     * Pop-up like slider bar to adjust various parameters.
     * 
     * @param name
     * @param min
     * @param max
     * @param paramID
     */
    private void seekerDialog(String name, final double min, final double max, final int paramID) {
    	
		// get prompts.xml view
		LayoutInflater li = LayoutInflater.from(this);
		View promptsView = li.inflate(R.layout.prompts, null);

		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

		// set prompts.xml to alertdialog builder
		alertDialogBuilder.setView(promptsView);

		// create alert dialog
		AlertDialog alertDialog = alertDialogBuilder.create();

		// show it
		alertDialog.show();
		
		double value = 0;
		switch(paramID) {
		case R.id.luminousOffsetMenuItem:
			value = manualLuminousOffset;
			break;
		case R.id.boxBlurKernelSizeMenuItem:
			value = boxBlurKernelSizeParam;
			break;
		case R.id.cannyLowerThresholdMenuItem:
			value = cannyLowerThresholdParam;
			break;
		case R.id.cannyUpperThresholdMenuItem:
			value = cannyUpperThresholdParam;
			break;
		case R.id.dilationKernelMenuItem:
			value = dilationKernelSize;
			break;
		case R.id.polygonEpsilonMenuItem:
			value = polygonEpsilonParam;
			break;
		}
	    
	    TextView paramTitleTextView = (TextView)(promptsView.findViewById(R.id.param_title_text_view));
	    paramTitleTextView.setText(name);
	    
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
				
				double newParamValue = (max - min) * ((double)progress) / 100.0 + min;
			    paramValueTextView.setText(String.format("%5.1f", newParamValue));
			    
				switch(paramID) {
				case R.id.luminousOffsetMenuItem:
					manualLuminousOffset = newParamValue;
					break;
				case R.id.boxBlurImageProcessMenuItem:
					boxBlurKernelSizeParam = newParamValue;
					break;
				case R.id.cannyLowerThresholdMenuItem:
					cannyLowerThresholdParam = newParamValue;
					break;
				case R.id.cannyUpperThresholdMenuItem:
					cannyUpperThresholdParam = newParamValue;
					break;
				case R.id.dilationKernelMenuItem:
					dilationKernelSize = newParamValue;
					break;
				case R.id.polygonEpsilonMenuItem:
					polygonEpsilonParam = newParamValue;
					break;
				}
			}
		});
	    seekBar.setProgress( (int) (100.0 * (value - min) / (max - min)) );

	}

    
	public void onCameraViewStarted(int width, int height) {
    }

	
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
    	  	
    	Mat resultImage = controller.onCameraFrame(inputFrame, imageSourceMode, imageProcessMode, annotationMode);
    	
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
