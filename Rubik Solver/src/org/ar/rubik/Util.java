package org.ar.rubik;

import org.ar.rubik.Constants.LogicalTile;

import android.util.Log;

public class Util {



	public static String dumpRGB(double[] color) {
		return String.format("r=%3.0f g=%3.0f b=%3.0f        ", color[0], color[1], color[2]);
	}
	
	public static String dumpRGB(double[] color, double colorError) {
		return String.format("r=%3.0f g=%3.0f b=%3.0f e=%5.0f", color[0], color[1], color[2], colorError);
	}

	public static String dumpRGB(LogicalTile logicalTile) {
		double color[] = logicalTile.color.val;
		return String.format("r=%3.0f g=%3.0f b=%3.0f     t=%c", color[0], color[1], color[2], logicalTile.character);
	}


	public static String dumpYUV(double[] color) {
		color = getYUVfromRGB(color);
		return String.format("y=%3.0f u=%3.0f v=%3.0f        ", color[0], color[1], color[2]);
	}

	
	/**
	 * Get YUV from RGB
	 * 
	 * @param rgb
	 * @return
	 */
	public static double[] getYUVfromRGB(double [] rgb) {
		
		if(rgb == null)  {
			Log.e(Constants.TAG, "RGB is NULL!");
			return new double[]{0, 0, 0 , 0}; 
		}
		double [] yuv = new double [4];
		yuv[0] =  0.229 * rgb[0]  +   0.587 * rgb[1]  +  0.114 * rgb[2];
		yuv[1] = -0.147 * rgb[0]  +  -0.289 * rgb[1]  +  0.436 * rgb[2];
		yuv[2] =  0.615 * rgb[0]  +  -0.515 * rgb[1]  + -0.100 * rgb[2];
		return yuv;
	}






	
}
