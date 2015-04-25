/**
 * Augmented Reality Rubik Cube Solver
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
kernel void stepKernel
(
    global const int* inputPixels,
    global int* outputPixels,
    const uint rowPitch,
    const int size, // filterSizeParam,
    const int epsilonParam,
    const int dummy3,
    const int dummy4,
    const int dummy5
)
{
 
    int x = get_global_id(0);
    int y = get_global_id(1);
    int imageHeight = get_global_size(0);

    int inPixel = inputPixels[x + y*rowPitch];  // =+= we only need this one pixel
    int outPixel = 0xffffffff;   // white border
    
#if 0   

      	int yy   = (inPixel >> 0)  & 0xFF;   // Y  lum
       	int uu = (inPixel >> 8)  & 0xFF;     // U  blue-yellow
       	int vv  = (inPixel >> 16) & 0xFF;    // Y  red-green 
    
        outPixel = (yy + uu + vv ) / 4;
#elif 1
  // 720 x 1280
  
  // I think pixel format is alpha_blue_green_red each 8 bits in that order.
  // or ????_saturation_luminance_hue 
  
  // alpha_V_U_Y

    int y_max = 0;
    int y_min = 255;
    int u_max = 0;
    int u_min = 255;
    int v_max = 0;
    int v_min = 255;
  
    outPixel = 0x00000000;
    
      
    for(int i=0; i<size; i++) {
      for(int j=0; j<size; j++) {
      	  
//    for(int i=0; i<16; i+=4) {
//      for(int j=0; j<16; j+=4) {
      	  
        int pixel = inputPixels[(x + i) + (y + j) * rowPitch];
      	int y   = (pixel >> 0)  & 0xFF;   // Y  lum
       	int u = (pixel >> 8)  & 0xFF;     // U  blue-yellow
       	int v  = (pixel >> 16) & 0xFF;    // Y  red-green
//      int alpha = (pixel >> 24) & 0xFF;   // All 1's
      	  
      	if(y < y_min)
      	  y_min = y;
      	if(y > y_max)
         y_max = y;
         
      	if(u < u_min)
      	  u_min = u;
      	if(u > u_max)
         u_max = u;
         
      	if(v < v_min)
      	  v_min = v;
      	if(v > v_max)
         v_max = v;
      
//        if( (y_max - y_min > epsilonParam) || (u_max - u_min > epsilonParam) || (v_max - v_min > epsilonParam) )
//          outPixel = 0x0;  // Black
//        else
//          outPixel = 0x00808080; // Medium Gray

		// Render grey scale as to decision
        if( y_max - y_min > 3 * epsilonParam)
          outPixel = 0xFF;  // white (lumanance)
        else if (u_max - u_min > epsilonParam)
          outPixel = 0x00;  // light grey (blue-yellow)
        else if (v_max - v_min > epsilonParam)
          outPixel = 0x00;  // dark grey (red-greeen)
        else
          outPixel = 0x00; // black




          
        // Also, eliminate any dark areas.
       // if(y_min < 20)
         // outPixel = 0x0;
          
          
          //outPixel = alpha;
      }
    }

    

#elif 0
// Rotate 
    if( (polar < radiusLo) || (polar > radiusHi) )
    {
    	// Outter area of the circle
    	outPixel = inPixel;
    }
    else {
    	
    	int deltaRadius = radiusHi - radiusLo;
    	int delta = polar - radiusLo;
    	float proportion = (float)delta / (float)deltaRadius; 
    	float theta = proportion * 45.0 * 3.14 / 180.0;		

//    	float theta = 45.0 * 3.14 / 180.0;
    	float sinTheta = sin(theta);
    	float cosTheta = cos(theta);
    	
    	int xPrim = (float)xRel * cosTheta - (float)yRel * sinTheta + xTouch;
    	int yPrim = (float)xRel * sinTheta + (float)yRel * cosTheta + yTouch;
    			
    	outPixel = inputPixels[xPrim + yPrim * rowPitch];
    }
#elif 0
// Invert colors 
    if( (polar < radiusLo) || (polar > radiusHi) )
    {
    	// Outter area of the circle
    	outPixel = inPixel;
    }
    
    else {
    	
    	// Extract Colors
    	uint blue   = (inPixel & 0x00ff0000) >> 16;
    	uint green = (inPixel & 0x0000ff00) >> 8;
    	uint red  = (inPixel & 0x000000ff) >> 0;
    	
    	// Normal reconstruction
//    	outPixel = 0xff000000 | (blue << 16) | (green << 8) | red << 0;
    	
    	// Color Inverstion
    	outPixel = 0xff000000 | ((0xFF - blue) << 16) | ((0xFF - green) << 8) | (0xFF - red) << 0;
    }

#else
// Original: make black and white interior 

   float3 channelWeights = (float3)(0.299f, 0.587f, 0.114f);

    if(polar < radiusLo)
    {
        // Inner area of the circle

        // Disassembly channels
        float3 channels = { (inPixel & 0xff), (inPixel & 0xff00) >> 8, (inPixel & 0xff0000) >> 16 };

        // Calculate gray value based on the canonical channel weights
        uint gray = dot(channels, channelWeights);

        // Assembly final color
        outPixel = 0xff000000 | (gray << 16) | (gray << 8) | gray;
    }
    else if(polar > radiusHi)
    {
        // Outter area of the circle
        outPixel = inPixel;
    }
#endif

    outputPixels[x + y*rowPitch] = outPixel;
}
