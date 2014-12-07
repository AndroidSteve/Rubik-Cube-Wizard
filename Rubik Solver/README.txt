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
  o  Complete conversion to using OpenCV Pose Estimator.
  o  Achieve quality overlay cube.
  o  Clarify definition of Front of Cube: current mixed  
  o  Make direction arrows occult with respect to physical cube.
  o  Rework N,M definition: currently backwards from desired.
  o  Determine actual tile colors after all 54 tiles have been observed for better robustness.
  o  Time and CPU measurements.
  o  Improve frame rate, possible turn on OpenCL flag to OpenCV technology.
  o  Make gesture recognition process with respect to time instead of frames to be CPU independent.
  o  Introduce real Pilot Cube that demonstrates requested user activity.
  o  Sweep through and make screen size independent.
  o  Custom OpenCL functions?
  
Open Issues:
  o  What if hardware does not provide intrinsic camera calibration parameters? 
  
  