Rubik Cube Wizard
============

Augmented Reality Android Rubik Cube Wizard

Android application developed on a commercial Smart Phone which, when run on a pair 
of Smart Glasses, guides a user through the process of solving a Rubik Cube.

Design
============
The overall design is a basically a classical "Model View Controller" paradigm, or 
perhaps more logically phrased as "Controller Model View."  The Controller 
portion is the most complex in that Image Recognition is perform in this section.
Image Recognition is achieve through series of steps using the OpenCV library,
and the process begins with Edge Detection and ends with estimation of the Rubik
Cube "Pose" (i.e., position and orientation in 3D space).  The Model portion is 
simply application state information, and the View section is a separate 
thread (i.e., the graphics display thread) executing standard 3D OpenGL graphics
overlaid on the camera image.

Video
============
A vide of the application in action can be veiw at: https://www.youtube.com/watch?v=Kvmb-lyGrdw

Goals
============
The Rubik Cube itself, as a game, is not patented thus effectively open-sourced, and the 
logic solution portion of this application (i.e., the code that produces the solution
sequence: http://kociemba.org/cube.htm) is also open-source.  Thus, it only seems fitting that this 
application also be in the open-source domain.  My hopes are to see it ported to many smart glasses 
devices and thus become sort of a de-facto standard.



See file ~/README.txt for more details.
