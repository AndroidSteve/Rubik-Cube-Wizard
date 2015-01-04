Augmented Reality Android Rubik Cube Solver

By:    Android Steve
Email: android.steve@testlens.com

Notice:
  This is currently work-in-progress.  A final stable version will be
  completed around March 2015 and also presented at the Wearable 
  Tech Conference: see http://www.wearablestechcon.com/classes#AndroidAugmentedRealityRubikCubeSolver.
  
  If you are interested in working or forking this code, I recommend that 
  you wait until then.  I will be much more willing to answer questions
  at that time.
  
  A design document is available here:  ~/doc/Design.docx


To Do:

  o  Refactor and clean up state machines.
  o  Migrate from JME to android.opengl 
  o  Introduce functional coloring to Pilot cube.
  o  Determine actual tile colors after all 54 tiles have been observed for better robustness.
  o  Merge the two separate OpenGL renders into one.
  o  Time and CPU measurements.  Possible use of DDMS
  o  Make direction arrows occult with respect to physical cube.
  o  Improve frame rate, possible turn on OpenCL flag to OpenCV technology.
  o  Review near-far definitions in CameraCalibration.getOpenGLProjectionMatrix(). Is this responsible for Overlay Cube errors? 
  o  Make gesture recognition process with respect to time instead of frames to be CPU independent.
  o  Introduce real Pilot Cube that demonstrates requested user activity.
  o  Sweep through and make screen size independent.
  o  Custom OpenCL functions?
  o  Switch to Android Studio
  
Open Issues:
  o  What if hardware does not provide intrinsic camera calibration parameters? 
  
  