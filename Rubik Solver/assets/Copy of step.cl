// Copyright © 2014 Intel Corporation. All rights reserved.
// This file contains the CL code.
//
// WARRANTY DISCLAIMER
//
// THESE MATERIALS ARE PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL INTEL OR ITS
// CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
// EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
// PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
// OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY OR TORT (INCLUDING
// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THESE
// MATERIALS, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//
// Intel Corporation is the author of the Materials, and requests that all
// problem reports or change requests be submitted to it directly


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
    
#if 1
  // 720 x 1080 I think
  
  // I think pixel format is alpha_blue_green_red each 8 bits in that order.
  // or ????_saturation_luminance_hue

    int hue_max = 0;
    int hue_min = 255;
    int lum_max = 0;
    int lum_min = 255;
    int sat_max = 0;
    int sat_min = 255;
  
    outPixel = 0x00000000;
      
    for(int i=0; i<size; i++) {
      for(int j=0; j<size; j++) {
      	  
        int pixel = inputPixels[(x + i) + (y + j) * rowPitch];
      	int hue   = (pixel >> 0)  & 0xFF;
       	int lum = (pixel >> 8)  & 0xFF;
//       	int sat  = (pixel >> 16) & 0xFF;
      	  
      	if(hue < hue_min)
      	  hue_min = hue;
      	if(hue > hue_max)
         hue_max = hue;
         
      	if(lum < lum_min)
      	  lum_min = lum;
      	if(lum > lum_max)
         lum_max = lum;
         
//      	if(sat < sat_min)
//      	  sat_min = sat;
//      	if(sat > sat_max)
//         sat_max = sat;
      
        if( (hue_max - hue_min > epsilonParam) ) // || (lum_max - lum_min > epsilonParam) || (sat_max - sat_min > epsilonParam) )
          outPixel = 0x0;  // Black
        else
          outPixel = 0x00808080; // Medium Gray
          
        // Also, eliminate any dark areas.
        if(lum_min < 30)
          outPixel = 0x0;
          
      }
    }
    
//outPixel = 0x00FF00FF;
    

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
