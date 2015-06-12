Augmented Reality Android Rubik Cube Wizard

By:    Android Steve
Email: android.steve@cl-sw.com

Notice:
  This is currently work-in-progress.  A final stable version will be
  completed around June 2015.
  
  If you are interested in working or forking this code, I recommend that 
  you wait until then.  I will be much more willing to answer questions
  at that time.
  
  A design document is available here:  ~/doc/Design.docx


To Do:

  o  Calculate true Kalman Filter Gain feedback matrix
  o  Change scan rotation so that pilot cube is more interesting
  o  Setup device configuration support
  o  
  o  Perform Edge detection on all three colors, drop grey scale step
  o  Time and CPU measurements.  Possible use of DDMS
  o  Create image recognition test suite.
  o  Improve frame rate, possible turn on OpenCL flag to OpenCV technology.
  o  Refactor and clean up state machines.
  o  Make gesture recognition process with respect to time instead of frames to be CPU independent.
  o  Introduce real Pilot Cube that demonstrates requested user activity.
  o  Edge detection on 3 colors instead of just greyscale?
  o  Improve Camera Calibration:
     - Evaluate openCV checkboard calibration.
     - Determine how to handle screen size != camera image size issue.
     - Goal is accurate rendering without any need for corrections.
  o  Custom OpenCL functions?
  o  Switch to Android Studio
  
Open Issues:
  o  What if hardware does not provide intrinsic camera calibration parameters? 
  
Bugs
  o    