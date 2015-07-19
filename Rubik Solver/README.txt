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


  o  Improve frame rate, try out OpenCV 3.0 technology
  o  Improve arrow rotation motion: possibly a growing arrow instead. 
  o  Change scan rotation so that pilot cube is more interesting
  o  Perform Edge detection on all three colors, drop grey scale step
  o  Add user error recovery:
     - top face as expected, but rotated
     - different face being shown
     - incorrect edge rotated
  o  Time and CPU measurements.  Possible use of DDMS
  o  Create image recognition test suite.
  
  o  Setup device configuration support
  o  Refactor and clean up state machines.
  o  Make gesture recognition process with respect to time instead of frames to be CPU independent.
  o  Introduce real Pilot Cube that demonstrates requested user activity.
  o  Edge detection on 3 colors instead of just greyscale?s.
  o  Custom OpenCL functions?
  o  Switch to Android Studio

  o  Calculate true Kalman Filter Gain feedback matrix
  
Open Issues:
  o  What if hardware does not provide intrinsic camera calibration parameters? 
  
Bugs
  o    