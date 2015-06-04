/**
 * Augmented Reality Rubik Cube Wizard
 * 
 * Author: Steven P. Punte (aka Android Steve : android.steve@cl-sw.com)
 * Date:   April 25th 2015
 * 
 * Project Description:
 *   Android application developed on a commercial Smart Phone which, when run on a pair 
 *   of Smart Glasses, guides a user through the process of solving a Rubik Cube.
 *   
 * File Description:
 *   Assignment of measured tile colors from camera pixels to ColorTile enumeration is
 *   performed here in this file/class.  There are actually two separate algorithms: "Cube Color
 *   Recognition" and "Face Color Recognition."
 *   
 *     Cube Color Recognition
 *     All six sides of the cube have now been observed.  There MUST be exactly 
 *     nine tiles assigned to each color.  A recursive algorithm is used
 *     to achieve a minimum total color error costs (i.e., distance in pixels
 *     between measured color values (RGB) and expected color values) of all 
 *     54 tiles.
 *     
 *     Face Color Recognition
 *     In this case, no restriction of tile assignments applies.  The algorithm
 *     used here:
 *     - Assigns measured tile to closest expected color.
 *     - Assume that some selection of Orange vs. Red was incorrect above.
 *       Algorithm adjust for LUMONISITY using the Blue, Green, Yellow and
 *       White tiles (assuming the face has some, and that they are correctly
 *       identified) and re-assigns tiles based on the adjusted closest expected color.
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

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.ar.rubik.Constants.ColorTileEnum;
import org.ar.rubik.Constants.FaceNameEnum;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;

import android.util.Log;

/**
 * Class Color Recognition
 * 
 * @author android.steve@cl-sw.com
 *
 */
public class ColorRecognition {


    /**
     * Private Class Tile Location
     * 
     * This is used to refer to a location of a tile on the cube
     * 
     * @author android.steve@cl-sw.com
     *
     */
    private static class TileLocation {
        final Constants.FaceNameEnum faceNameEnum;
        final int n;
        final int m;
        public TileLocation(Constants.FaceNameEnum faceNameEnum, int n, int m) {
            this.faceNameEnum = faceNameEnum;
            this.n = n;
            this.m = m;
        }
    }



    /**
     * Inner class Cube
     * 
     *     All six sides of the cube have now been observed.  There MUST be exactly 
     *     nine tiles assigned to each color.  A recursive algorithm is used
     *     to achieve a minimum total color error costs (i.e., distance in pixels
     *     between measured color values (RGB) and expected color values) of all 
     *     54 tiles.
     * 
     * @author android.steve@cl-sw.com
     *
     */
    public static class Cube {

        private StateModel stateModel;

        
        // Map of ColorTimeEnum to a group of tiles.  Group of tiles is actually a Map of color error to tile location. 
        // This mapping must be synchronized with the assignments in StateModel.
        // It would be possible/natural to place this data structure in State Model since it is synchronized with RubikFace[name].observedTileArray,
        // which contains the tile to color mapping state.
        private static Map <ColorTileEnum, TreeMap <Double, TileLocation>> observedColorGroupMap;
        private static Map <ColorTileEnum, TreeMap <Double, TileLocation>> bestColorGroupMap;


        // Best Color Assignment State.
        // When assignment is completed, this data structure is copied to StateModel.face[name].colorTile[][] of the main state.
        // It is important to have two copies so that one can represent the starting point of a recursive search, and the other (below)
        // can represent the best assignment mapping achieved during the current (i.e., for a specific tile) recursive search.  
        private static Map <FaceNameEnum, ColorTileEnum[][]> bestAssignmentState;


        // Best Costs.
        private static double bestAssignmentCost;

        

        /**
         * Subclass Cube Constructor
         * 
         * @param stateModel
         */
        public Cube(StateModel stateModel) {
            this.stateModel = stateModel;
        }


        
        /**
         * Cube Tile Color Recognition
         * 
         *     All six sides of the cube have now been observed.  There MUST be exactly 
         *     nine tiles assigned to each color.  A recursive algorithm is used
         *     to achieve a minimum total color error costs (i.e., distance in pixels
         *     between measured color values (RGB) and expected color values) of all 
         *     54 tiles.  Also, the cost calculation algorithm rules out any two faces 
         *     having the same center tile color.
         * 
         */
        public void cubeTileColorRecognition() {
        	
        	Log.i(Constants.TAG_COLOR, "Entering cube tile color recognition.");
        	printDiagnosticsColorTileAssignments();

            // Clear out all tile color mapping in state model.
            for(Constants.FaceNameEnum faceNameEnum : Constants.FaceNameEnum.values())
                for(int n=0; n<3; n++) 
                    for(int m=0; m<3; m++)
                        stateModel.nameRubikFaceMap.get(faceNameEnum).observedTileArray[n][m] = null;

            // Populate Color Group Map with necessary objects: i.e., one tree object per color.
            observedColorGroupMap = new TreeMap<ColorTileEnum, TreeMap <Double, TileLocation>>();
            for(ColorTileEnum colorTile : ColorTileEnum.values())
                if(colorTile.isRubikColor == true)
                    observedColorGroupMap.put(colorTile, new TreeMap<Double,TileLocation>());

            // Populate Best Color Group Map with necessary objects: i.e., tree object per color.
            bestColorGroupMap = new TreeMap<ColorTileEnum, TreeMap <Double, TileLocation>>();
            for(ColorTileEnum colorTile : ColorTileEnum.values())
                if(colorTile.isRubikColor == true)
                    bestColorGroupMap.put(colorTile, new TreeMap<Double,TileLocation>());
            
            // Loop over all 54 tile location and assign a ColorTileEnum to this location.
            for(Constants.FaceNameEnum faceNameEnum : Constants.FaceNameEnum.values()) {
                for(int n=0; n<3; n++) {
                    for(int m=0; m<3; m++) {
                    	
                    	/* Initialize Best Variables */

                        // Copy State Model to private "best assignment state" as starting point for recursive search.
                        bestAssignmentState = new TreeMap<Constants.FaceNameEnum, Constants.ColorTileEnum[][]>();  // Fresh new Map
                        for(Constants.FaceNameEnum faceNameEnum2 : Constants.FaceNameEnum.values()) {
                            ColorTileEnum[][] tileArrayClone = new ColorTileEnum[3][3];
                            for(int n2=0; n2<3; n2++) 
                                for(int m2=0; m2<3; m2++)
                                    tileArrayClone[n2][m2] = stateModel.nameRubikFaceMap.get(faceNameEnum2).observedTileArray[n2][m2];
                            bestAssignmentState.put(faceNameEnum2, tileArrayClone);
                        }
                        
                        // Copy Local State Color Group to "best color group"
                        for(ColorTileEnum colorTile2 : ColorTileEnum.values())
                            if(colorTile2.isRubikColor == true) {
								TreeMap<Double, TileLocation> colorGroupClone = new TreeMap<Double,TileLocation>(observedColorGroupMap.get(colorTile2));
								bestColorGroupMap.put(colorTile2, colorGroupClone);
							}

                        bestAssignmentCost = Double.MAX_VALUE;
                        
                        

                        /* Insert tile into State Model */
                        
                        // Evaluate (possibly recursive) tile for insertion
                        // The least-cost solution shall be copied to the "best" variables.
                        TileLocation tileLocation = new TileLocation(faceNameEnum, n, m);
                        evaluateTileAssignmentForLowestOverallCostPossiblyReursively(tileLocation, new HashSet<ColorTileEnum>(9));

                        
                        // Copy "best assignment state" to State Model
                        for(Constants.FaceNameEnum faceNameEnum3 : Constants.FaceNameEnum.values())
                            stateModel.nameRubikFaceMap.get(faceNameEnum3).observedTileArray = bestAssignmentState.get(faceNameEnum3);  // no need for clone.
                        
                        // Copy "best color group" to Local State Model
                        for(ColorTileEnum colorTile2 : ColorTileEnum.values())
                            if(colorTile2.isRubikColor == true)
                            	observedColorGroupMap.put(colorTile2, new TreeMap<Double,TileLocation>(bestColorGroupMap.get(colorTile2)));
                        
                        printDiagnosticsColorTileAssignments();
                    }
                }
            }
        }

        

        /**
         * Evaluate Tile Assignment for Lowest Overall Cost Possibly Recursive
         * 
         * Attempts to assign tile in State Model to every tile color, and copies
         * lowest cost total assignment to member data "bestAssignmentState".  If
         * a tile is assigned to a color group, and now that group has more than
         * nine tiles assigned to it, then remove the tile with the highest cost,
         * and assign it to some other group recursively.
         * 
         * @param tileLocation
         * @param measuredColor
         * @param blackList
         */
        private void evaluateTileAssignmentForLowestOverallCostPossiblyReursively(TileLocation tileLocation, Set <ColorTileEnum> blackList) {

//           	Log.d(Constants.TAG_COLOR, "Assign tile with blacklist = " + blackList.size() );
        	       	
        	// Loop and evaluate cost of assigning all colors to this location.  Keep lowest cost in "best" variables.
            for(ColorTileEnum colorTile : ColorTileEnum.values()) {

                if(colorTile.isRubikColor == false)
                    continue;

                // No evaluation for this color tile: group is full.
                if(blackList.contains(colorTile))
                    continue;


                // Assign a color to State Model at specified location
                assignTileToColor(tileLocation, colorTile);

                // If mapping is still valid (i.e., not more than 9 tiles are assigned to any color),
                // then do not attempt any further re-arrangement of tile mapping.
                TreeMap<Double, TileLocation> colorGroup = observedColorGroupMap.get(colorTile);
                if(colorGroup.size() <=9 )  {

                	// This is sum of selected color errors of tiles that are assigned.  No-assigned are not considered.
                    double cost = calculateTotalColorErrorCostOfAssignment();

                    // If lower costs of anything so far found, then adopt.
                    if(cost < bestAssignmentCost ) {

                    	// Copy current cost to best assignment cost
                        bestAssignmentCost = cost;

                        // Copy State Model to Best Assignment State
                        bestAssignmentState = new TreeMap<Constants.FaceNameEnum, Constants.ColorTileEnum[][]>();  // Fresh new Map
                        for(Constants.FaceNameEnum faceNameEnum : Constants.FaceNameEnum.values()) {
                            ColorTileEnum[][] tileArrayClone = new ColorTileEnum[3][3];
                            for(int n=0; n<3; n++) 
                                for(int m=0; m<3; m++)
                                    tileArrayClone[n][m] = stateModel.nameRubikFaceMap.get(faceNameEnum).observedTileArray[n][m];
                            bestAssignmentState.put(faceNameEnum, tileArrayClone);
                        }
                        
                        // Copy Local State Color Group to "best color group"
                        for(ColorTileEnum colorTile2 : ColorTileEnum.values())
                            if(colorTile2.isRubikColor == true) {
								TreeMap<Double, TileLocation> colorGroupClone = new TreeMap<Double,TileLocation>(observedColorGroupMap.get(colorTile2));
								bestColorGroupMap.put(colorTile2, colorGroupClone);
							}
                    }
                }

                // Else, current color group is invalid: too many tiles.  Take out highest cost
                // tile, and try moving elsewhere.
                else {
                	
//                	Log.d(Constants.TAG_COLOR, "Color Group " + colorTile + " has too many elements.");

                    // Highest cost assignment is at end of list for TreeMap
                    TileLocation moveTileLoc = colorGroup.lastEntry().getValue();

                    // Remove from State Model
                    unassignTileToColor(moveTileLoc, colorTile);
//                    Log.d(Constants.TAG_COLOR, "Unassign tile at location [" + moveTileLoc.faceNameEnum + "][" + moveTileLoc.n + "][" + moveTileLoc.m + "] from color group " + colorTile + " with error cost " + moveTileColorError);

                    // Add to blacklist
                    blackList.add(colorTile);

                    // Recursively assign tile 2 somewhere else.
                    evaluateTileAssignmentForLowestOverallCostPossiblyReursively(moveTileLoc, blackList);
                    
                    // Remove from blacklist
                    blackList.remove(colorTile);

                    // Replace Back to State Model
                    assignTileToColor(moveTileLoc, colorTile);
                }

                // Remove from State Model
                unassignTileToColor(tileLocation, colorTile);

            } // End of loop over colors
        }

        
        
        /**
         * Unassign Color Tile Enum from State Model Tile Location: i.e., observed tile array
         * 
         * @param tileLocation
         * @param colorlTile
         */
        private void unassignTileToColor(TileLocation tileLocation, ColorTileEnum colorlTile) {
            
            RubikFace rubikFace = stateModel.nameRubikFaceMap.get(tileLocation.faceNameEnum);
			rubikFace.observedTileArray[tileLocation.n][tileLocation.m] = null;
            
            TreeMap<Double, TileLocation> colorGroup = observedColorGroupMap.get(colorlTile);
            Double keyOfItemToBeRemoved = null;
            for(Entry<Double, TileLocation> entry : colorGroup.entrySet()) {
                if(entry.getValue() == tileLocation)  // =+= Is tile location same object?  Seems like this is true.
                	keyOfItemToBeRemoved = entry.getKey();
            }
            colorGroup.remove(keyOfItemToBeRemoved);
        }

        

        /**
         * Assign Color Tile Enum to State Model Tile Location: i.e., observed tile array
         * 
         * @param tileLocation
         * @param colorTile
         */
        private void assignTileToColor(TileLocation tileLocation, ColorTileEnum colorTile) {

            RubikFace rubikFace = stateModel.nameRubikFaceMap.get(tileLocation.faceNameEnum);
            rubikFace.observedTileArray[tileLocation.n][tileLocation.m] = colorTile;
            
            TreeMap<Double, TileLocation> colorGroup = observedColorGroupMap.get(colorTile);
			colorGroup.put(
                    calculateColorErrorCost(new Scalar(rubikFace.measuredColorArray[tileLocation.n][tileLocation.m]), colorTile.cvColor),
                    tileLocation);
        }



        /**
         * Calculate and return the Color Error Assignment costs of the provided assignment map.
         * 
         * Return is sum of color error vectors added in a simple scalar magnitude manner.  Note,
         * a cost of Double.MAX_VALUE is returned if center tile of any two faces have duplicate color.
         * =+= possibly should be sum square.
         * 
         * @return
         */
        private double calculateTotalColorErrorCostOfAssignment() {

            double cost = 0.0;

            // Loop over all 54 tile location and assign an ColorTileEnum to this location.
            for(Constants.FaceNameEnum faceNameEnum : Constants.FaceNameEnum.values())
                for(int n=0; n<3; n++) 
                    for(int m=0; m<3; m++) {
                        RubikFace rubikFace = this.stateModel.nameRubikFaceMap.get(faceNameEnum);
                        if(rubikFace.observedTileArray[n][m] != null)
                            cost += calculateColorErrorCost(new Scalar(rubikFace.measuredColorArray[n][m]), rubikFace.observedTileArray[n][m].cvColor);
                    }

            
            // Test if all six center tiles have different colors.  Return infinite cost if not true.
            // Test if any two sides have the same center tile color.  Ignore sides that are not yet assigned.
        	Set<ColorTileEnum> centerTileColorSet = new HashSet<ColorTileEnum>(16);
            for(Constants.FaceNameEnum faceNameEnum : Constants.FaceNameEnum.values()) {
            	RubikFace rubikFace = this.stateModel.nameRubikFaceMap.get(faceNameEnum);
				ColorTileEnum centerColorTile = rubikFace.observedTileArray[1][1];
				
				if(centerColorTile == null)
					continue;
				
            	if(centerTileColorSet.contains(centerColorTile))
            		return Double.MAX_VALUE;
            	else
            		centerTileColorSet.add(centerColorTile);
            }
            
            return cost;
        }
        



		/**
		 * 
		 */
        @SuppressWarnings("unused")
		private void printDiagnosticsColorTileAssignments() {
        	
        	if(true) return;

        	// Print State Model Observed Tile Array
        	for(Constants.FaceNameEnum faceNameEnum2 : Constants.FaceNameEnum.values()) {

        		StringBuilder str = new StringBuilder();
        		for(int n2=0; n2<3; n2++)
        			for(int m2=0; m2<3; m2++)
        				str.append( "|" + stateModel.nameRubikFaceMap.get(faceNameEnum2).observedTileArray[n2][m2]);

        		Log.d(Constants.TAG_COLOR, "State Tile at [" + faceNameEnum2 + "] " + str + "|");
        	}

        	
        	// Print State Model ObservedColorGroupMap
        	if(observedColorGroupMap != null)
        		for(ColorTileEnum colorTile : observedColorGroupMap.keySet()) {

        			TreeMap<Double, TileLocation> tileColorMap = observedColorGroupMap.get(colorTile);
        			StringBuilder str = new StringBuilder();
        			for( Entry<Double, TileLocation> entry : tileColorMap.entrySet()) {
        				str.append("|" + String.format("%5.1f", entry.getKey()));
        			}
        			Log.d(Constants.TAG_COLOR, "Color Group " + colorTile + " " + tileColorMap.size() + " " + str + "|");
        		}
        	
        	Log.d(Constants.TAG_COLOR, "Total Currnet Cost = " + calculateTotalColorErrorCostOfAssignment());
        }

        

        /**
         * Calculate the magnitude of the color error vector between 
         * the two provided color values.
         * 
         * =+= probably should make RubicFace.measuredColor a Scalar
         * 
         * @param color1
         * @param color2
         * @return
         */
        private static double calculateColorErrorCost(Scalar color1, Scalar color2) {

            // Calculate distance
            double distance =
                    (color1.val[0] - color2.val[0]) * (color1.val[0] - color2.val[0]) +
                    (color1.val[1] - color2.val[1]) * (color1.val[1] - color2.val[1]) +
                    (color1.val[2] - color2.val[2]) * (color1.val[2] - color2.val[2]);

            return Math.sqrt(distance);
        }   
    }
    
    



    /**
     * Inner class Face
     * 
     *     In this case, no restriction of tile assignments applies.  The algorithm
     *     used here:
     *     - Assigns measured tile to closest expected color.
     *     - Assume that some selection of Orange vs. Red was incorrect above.
     *       Algorithm adjust for LUMONISITY using the Blue, Green, Yellow and
     *       White tiles (assuming the face has some, and that they are correctly
     *       identified) and re-assigns tiles based on the adjusted closest expected color.
     *
     */
    public static class Face {
    	
    	private RubikFace rubikFace;
        
    	// Sum of Color Error before Luminous correction
    	public double colorErrorBeforeCorrection;

    	// Sum of Color Error after Luminous correction
    	public double colorErrorAfterCorrection;
    	
    	// Luminous Offset: Added to luminous of tiles for better accuracy
    	public double luminousOffset = 0.0;
    	
      
    	public Face(RubikFace rubikFace) {
			this.rubikFace = rubikFace;
		}


		/**
    	 * Find Closest Tile Color
    	 * 
    	 * Two Pass algorithm:
    	 * 1) Find closest fit using just U and V axis.
    	 * 2) Calculate luminous correction value assuming above choices are correct (exclude Red and Orange)
    	 * 3) Find closed fit again using Y, U and V axis where Y is corrected.
    	 * @param image 
    	 * 
    	 * @return
    	 */
    	public void faceTileColorRecognition(Mat image) {
    		
    		double [][] colorError = new double[3][3];
    		
    		// Obtain actual measured tile color from image.
    		for(int n=0; n<3; n++) {
    			for(int m=0; m<3; m++) {

    			    Point tileCenter = rubikFace.getTileCenterInPixels(n, m);
    			    Size size = image.size();
    			    double width = size.width;
    			    double height = size.height;
    			    
    			    // Check location of tile on screen: can be too close to screen edge.
    			    if( tileCenter.x < 10 || tileCenter.x > width - 10 || tileCenter.y < 10 || tileCenter.y > height - 10) {
    			        Log.w(Constants.TAG_COLOR, String.format("Tile at [%1d,%1d] has coordinates x=%5.1f y=%5.1f too close to edge to assign color.", n, m, tileCenter.x, tileCenter.y));
    			        rubikFace.measuredColorArray[n][m] = new double[4];  // This will default to back.  
    			    }

    			    // Obtain measured color from average over 20 by 20 pixel squar.
    			    else {

    			        try {
    			            Mat mat = image.submat((int)(tileCenter.y - 10), (int)(tileCenter.y + 10), (int)(tileCenter.x - 10), (int)(tileCenter.x + 10));
    			            rubikFace.measuredColorArray[n][m] = Core.mean(mat).val;
    			        }

    			        // Probably LMS calculations produced bogus tile location.
    			        catch(CvException cvException) {
    			            Log.e(Constants.TAG_COLOR, "ERROR findClosestLogicalTiles(): x=" + tileCenter.x + " y=" + tileCenter.y + " img=" + image + " :" + cvException);
    			            rubikFace.measuredColorArray[n][m] = new double[4];				
    			        }
    			    }
    			}
    		}

    		
    		// First Pass: Find closest logical color using only UV axis.
    		for(int n=0; n<3; n++) {
    			for(int m=0; m<3; m++) {

    				double [] measuredColor = rubikFace.measuredColorArray[n][m];
    				double [] measuredColorYUV   = Util.getYUVfromRGB(measuredColor);

    				double smallestError = Double.MAX_VALUE;
    				ColorTileEnum bestCandidate = null;
    				
    				for(ColorTileEnum candidateColorTile : Constants.ColorTileEnum.values()) {

    				    if(candidateColorTile.isRubikColor == true) {

    				        double[] candidateColorYUV = Util.getYUVfromRGB(candidateColorTile.rubikColor.val);

    				        // Only examine U and V axis, and not luminous.
    				        double error =
    				                (candidateColorYUV[1] - measuredColorYUV[1]) * (candidateColorYUV[1] - measuredColorYUV[1]) +
    				                (candidateColorYUV[2] - measuredColorYUV[2]) * (candidateColorYUV[2] - measuredColorYUV[2]);

    				        colorError[n][m] = Math.sqrt(error);

    				        if(error < smallestError) {
    				            bestCandidate = candidateColorTile;
    				            smallestError = error;
    				        }
    				    }
    				}

//    				Log.d(Constants.TAG_COLOR, String.format( "Tile[%d][%d] has R=%3.0f, G=%3.0f B=%3.0f %c err=%4.0f", n, m, measuredColor[0], measuredColor[1], measuredColor[2], bestCandidate.character, smallestError));

    				// Assign best candidate to this tile location.
    				rubikFace.observedTileArray[n][m] = bestCandidate;
    			}
    		}
    		
    		// Calculate and record LMS error (including luminous).
    		for(int n=0; n<3; n++) {
    			for(int m=0; m<3; m++) {
    				double[] selectedColor = rubikFace.observedTileArray[n][m].rubikColor.val;
    				double[] measuredColor = rubikFace.measuredColorArray[n][m];
    				colorErrorBeforeCorrection += calculateColorError(selectedColor, measuredColor, true, 0.0);
    			}
    		}
    		
    		// Diagnostics:  For each tile location print: measure RGB, measure YUV, logical RGB, logical YUV
    		Log.d(Constants.TAG_COLOR, "Table: Measure RGB, Measure YUV, Logical RGB, Logical YUV");
    		Log.d(Constants.TAG_COLOR, String.format( " m:n|----------0--------------|-----------1-------------|---------2---------------|") );
    		Log.d(Constants.TAG_COLOR, String.format( " 0  |%s|%s|%s|", Util.dumpRGB(rubikFace.measuredColorArray[0][0], colorError[0][0]), Util.dumpRGB(rubikFace.measuredColorArray[1][0], colorError[1][0]), Util.dumpRGB(rubikFace.measuredColorArray[2][0], colorError[2][0]) )); 
    		Log.d(Constants.TAG_COLOR, String.format( " 0  |%s|%s|%s|", Util.dumpYUV(rubikFace.measuredColorArray[0][0]), Util.dumpYUV(rubikFace.measuredColorArray[1][0]), Util.dumpYUV(rubikFace.measuredColorArray[2][0]) )); 
    		Log.d(Constants.TAG_COLOR, String.format( " 0  |%s|%s|%s|", Util.dumpRGB(rubikFace.observedTileArray[0][0]), Util.dumpRGB(rubikFace.observedTileArray[1][0]), Util.dumpRGB(rubikFace.observedTileArray[2][0]) )); 
    		Log.d(Constants.TAG_COLOR, String.format( " 0  |%s|%s|%s|", Util.dumpYUV(rubikFace.observedTileArray[0][0].rubikColor.val), Util.dumpYUV(rubikFace.observedTileArray[1][0].rubikColor.val), Util.dumpYUV(rubikFace.observedTileArray[2][0].rubikColor.val) )); 
    		Log.d(Constants.TAG_COLOR, String.format( "    |-------------------------|-------------------------|-------------------------|") );
    		Log.d(Constants.TAG_COLOR, String.format( " 1  |%s|%s|%s|", Util.dumpRGB(rubikFace.measuredColorArray[0][1], colorError[0][1]), Util.dumpRGB(rubikFace.measuredColorArray[1][1], colorError[1][1]), Util.dumpRGB(rubikFace.measuredColorArray[2][1], colorError[2][1]) )); 
    		Log.d(Constants.TAG_COLOR, String.format( " 1  |%s|%s|%s|", Util.dumpYUV(rubikFace.measuredColorArray[0][1]), Util.dumpYUV(rubikFace.measuredColorArray[1][1]), Util.dumpYUV(rubikFace.measuredColorArray[2][1]) )); 
    		Log.d(Constants.TAG_COLOR, String.format( " 1  |%s|%s|%s|", Util.dumpRGB(rubikFace.observedTileArray[0][1]), Util.dumpRGB(rubikFace.observedTileArray[1][1]), Util.dumpRGB(rubikFace.observedTileArray[2][1]) )); 
    		Log.d(Constants.TAG_COLOR, String.format( " 1  |%s|%s|%s|", Util.dumpYUV(rubikFace.observedTileArray[0][1].rubikColor.val), Util.dumpYUV(rubikFace.observedTileArray[1][1].rubikColor.val), Util.dumpYUV(rubikFace.observedTileArray[2][1].rubikColor.val) )); 
    		Log.d(Constants.TAG_COLOR, String.format( "    |-------------------------|-------------------------|-------------------------|") );
    		Log.d(Constants.TAG_COLOR, String.format( " 2  |%s|%s|%s|", Util.dumpRGB(rubikFace.measuredColorArray[0][2], colorError[0][2]), Util.dumpRGB(rubikFace.measuredColorArray[1][2], colorError[1][2]), Util.dumpRGB(rubikFace.measuredColorArray[2][2], colorError[2][2]) ));
    		Log.d(Constants.TAG_COLOR, String.format( " 2  |%s|%s|%s|", Util.dumpYUV(rubikFace.measuredColorArray[0][2]), Util.dumpYUV(rubikFace.measuredColorArray[1][2]), Util.dumpYUV(rubikFace.measuredColorArray[2][2]) ));
    		Log.d(Constants.TAG_COLOR, String.format( " 2  |%s|%s|%s|", Util.dumpRGB(rubikFace.observedTileArray[0][2]), Util.dumpRGB(rubikFace.observedTileArray[1][2]), Util.dumpRGB(rubikFace.observedTileArray[2][2]) ));
    		Log.d(Constants.TAG_COLOR, String.format( " 2  |%s|%s|%s|", Util.dumpYUV(rubikFace.observedTileArray[0][2].rubikColor.val), Util.dumpYUV(rubikFace.observedTileArray[1][2].rubikColor.val), Util.dumpYUV(rubikFace.observedTileArray[2][2].rubikColor.val) ));
    		Log.d(Constants.TAG_COLOR, String.format( "    |-------------------------|-------------------------|-------------------------|") );
    		Log.d(Constants.TAG_COLOR, "Total Color Error Before Correction: " + colorErrorBeforeCorrection);
    		
    		
    		// Now compare Actual Luminous against expected luminous, and calculate an offset.
    		// However, do not use Orange and Red because they are most likely to be miss-identified.
    		// =+= TODO: Also, diminish weight on colors that are repeated.
    		luminousOffset = 0.0;
    		int count = 0;
    		for(int n=0; n<3; n++) {
    			for(int m=0; m<3; m++) {
    			    ColorTileEnum colorTile = rubikFace.observedTileArray[n][m];
    				if(colorTile == ColorTileEnum.RED || colorTile == ColorTileEnum.ORANGE)
    					continue;
    				double measuredLuminousity = Util.getYUVfromRGB(rubikFace.measuredColorArray[n][m])[0];
    				double expectedLuminousity = Util.getYUVfromRGB(colorTile.rubikColor.val)[0];
    				luminousOffset += (expectedLuminousity - measuredLuminousity);
    				count++;
    			}
    		}
    		luminousOffset = (count == 0) ? 0.0 : luminousOffset / count;
    		Log.d(Constants.TAG_COLOR, "Luminousity Offset: " + luminousOffset);
    		
    		
    		// Second Pass: Find closest logical color using YUV but add luminousity offset to measured values.
    		for(int n=0; n<3; n++) {
    			for(int m=0; m<3; m++) {

    				double [] measuredColor = rubikFace.measuredColorArray[n][m];
    				double [] measuredColorYUV   = Util.getYUVfromRGB(measuredColor);

    				double smallestError = Double.MAX_VALUE;
    				ColorTileEnum bestCandidate = null;
    				
    				for(ColorTileEnum candidateColorTile : ColorTileEnum.values() ) {
    				    
    				    if(candidateColorTile.isRubikColor == true) {

    				        double[] candidateColorYUV = Util.getYUVfromRGB(candidateColorTile.rubikColor.val);

    				        // Calculate Error based on U, V, and Y, but adjust with luminous offset.
    				        double error =
    				                (candidateColorYUV[0] - (measuredColorYUV[0] + luminousOffset)) * (candidateColorYUV[0] - (measuredColorYUV[0] + luminousOffset)) +
    				                (candidateColorYUV[1] -  measuredColorYUV[1]) * (candidateColorYUV[1] - measuredColorYUV[1]) +
    				                (candidateColorYUV[2] -  measuredColorYUV[2]) * (candidateColorYUV[2] - measuredColorYUV[2]);

    				        colorError[n][m] = Math.sqrt(error);

    				        if(error < smallestError) {
    				            bestCandidate = candidateColorTile;
    				            smallestError = error;
    				        }
    				    }
    				}

//    				Log.d(Constants.TAG_COLOR, String.format( "Tile[%d][%d] has R=%3.0f, G=%3.0f B=%3.0f %c err=%4.0f", n, m, measuredColor[0], measuredColor[1], measuredColor[2], bestCandidate.character, smallestError));

    				// Check and possibly re-assign this tile location with a different color.
    				if(bestCandidate != rubikFace.observedTileArray[n][m]) {
    					Log.i(Constants.TAG_COLOR, String.format("Reclassiffying tile [%d][%d] from %c to %c", n, m, rubikFace.observedTileArray[n][m].symbol, bestCandidate.symbol));
    					rubikFace.observedTileArray[n][m] = bestCandidate;
    				}
    			}
    		}
    		
    		// Calculate and record LMS error (includeing LMS).
    		for(int n=0; n<3; n++) {
    			for(int m=0; m<3; m++) {
    				double[] selectedColor = rubikFace.observedTileArray[n][m].rubikColor.val;
    				double[] measuredColor = rubikFace.measuredColorArray[n][m];
    				colorErrorAfterCorrection += calculateColorError(selectedColor, measuredColor, true, luminousOffset);
    			}
    		}		
    		
    		// Diagnostics: 
    		Log.d(Constants.TAG_COLOR, "Table: Measure RGB, Measure YUV, Logical RGB, Logical YUV");
    		Log.d(Constants.TAG_COLOR, String.format( " m:n|----------0--------------|-----------1-------------|---------2---------------|") );
    		Log.d(Constants.TAG_COLOR, String.format( " 0  |%s|%s|%s|", Util.dumpRGB(rubikFace.measuredColorArray[0][0], colorError[0][0]), Util.dumpRGB(rubikFace.measuredColorArray[1][0], colorError[1][0]), Util.dumpRGB(rubikFace.measuredColorArray[2][0], colorError[2][0]) )); 
    		Log.d(Constants.TAG_COLOR, String.format( " 0  |%s|%s|%s|", Util.dumpYUV(rubikFace.measuredColorArray[0][0]), Util.dumpYUV(rubikFace.measuredColorArray[1][0]), Util.dumpYUV(rubikFace.measuredColorArray[2][0]) )); 
    		Log.d(Constants.TAG_COLOR, String.format( " 0  |%s|%s|%s|", Util.dumpRGB(rubikFace.observedTileArray[0][0]), Util.dumpRGB(rubikFace.observedTileArray[1][0]), Util.dumpRGB(rubikFace.observedTileArray[2][0]) )); 
    		Log.d(Constants.TAG_COLOR, String.format( " 0  |%s|%s|%s|", Util.dumpYUV(rubikFace.observedTileArray[0][0].rubikColor.val), Util.dumpYUV(rubikFace.observedTileArray[1][0].rubikColor.val), Util.dumpYUV(rubikFace.observedTileArray[2][0].rubikColor.val) )); 
    		Log.d(Constants.TAG_COLOR, String.format( "    |-------------------------|-------------------------|-------------------------|") );
    		Log.d(Constants.TAG_COLOR, String.format( " 1  |%s|%s|%s|", Util.dumpRGB(rubikFace.measuredColorArray[0][1], colorError[0][1]), Util.dumpRGB(rubikFace.measuredColorArray[1][1], colorError[1][1]), Util.dumpRGB(rubikFace.measuredColorArray[2][1], colorError[2][1]) )); 
    		Log.d(Constants.TAG_COLOR, String.format( " 1  |%s|%s|%s|", Util.dumpYUV(rubikFace.measuredColorArray[0][1]), Util.dumpYUV(rubikFace.measuredColorArray[1][1]), Util.dumpYUV(rubikFace.measuredColorArray[2][1]) )); 
    		Log.d(Constants.TAG_COLOR, String.format( " 1  |%s|%s|%s|", Util.dumpRGB(rubikFace.observedTileArray[0][1]), Util.dumpRGB(rubikFace.observedTileArray[1][1]), Util.dumpRGB(rubikFace.observedTileArray[2][1]) )); 
    		Log.d(Constants.TAG_COLOR, String.format( " 1  |%s|%s|%s|", Util.dumpYUV(rubikFace.observedTileArray[0][1].rubikColor.val), Util.dumpYUV(rubikFace.observedTileArray[1][1].rubikColor.val), Util.dumpYUV(rubikFace.observedTileArray[2][1].rubikColor.val) )); 
    		Log.d(Constants.TAG_COLOR, String.format( "    |-------------------------|-------------------------|-------------------------|") );
    		Log.d(Constants.TAG_COLOR, String.format( " 2  |%s|%s|%s|", Util.dumpRGB(rubikFace.measuredColorArray[0][2], colorError[0][2]), Util.dumpRGB(rubikFace.measuredColorArray[1][2], colorError[1][2]), Util.dumpRGB(rubikFace.measuredColorArray[2][2], colorError[2][2]) ));
    		Log.d(Constants.TAG_COLOR, String.format( " 2  |%s|%s|%s|", Util.dumpYUV(rubikFace.measuredColorArray[0][2]), Util.dumpYUV(rubikFace.measuredColorArray[1][2]), Util.dumpYUV(rubikFace.measuredColorArray[2][2]) ));
    		Log.d(Constants.TAG_COLOR, String.format( " 2  |%s|%s|%s|", Util.dumpRGB(rubikFace.observedTileArray[0][2]), Util.dumpRGB(rubikFace.observedTileArray[1][2]), Util.dumpRGB(rubikFace.observedTileArray[2][2]) ));
    		Log.d(Constants.TAG_COLOR, String.format( " 2  |%s|%s|%s|", Util.dumpYUV(rubikFace.observedTileArray[0][2].rubikColor.val), Util.dumpYUV(rubikFace.observedTileArray[1][2].rubikColor.val), Util.dumpYUV(rubikFace.observedTileArray[2][2].rubikColor.val) ));
    		Log.d(Constants.TAG_COLOR, String.format( "    |-------------------------|-------------------------|-------------------------|") );

    		Log.d(Constants.TAG_COLOR, "Color Error After Correction: " + colorErrorAfterCorrection);
    	}
    	
    	
    	/**
    	 * Calculate Color Error
    	 * 
    	 * Return distance between two colors.
    	 * 
    	 * @param slected
    	 * @param measured
    	 * @param useLuminous
    	 * @param _luminousOffset 
    	 * @return
    	 */
    	private static double calculateColorError(double[] slected, double[] measured, boolean useLuminous, double _luminousOffset) {
    		double error =
    				(slected[0] - (measured[0] + _luminousOffset)) * (slected[0] - (measured[0] + _luminousOffset) ) +
    				(slected[1] - measured[1]) * (slected[1] - measured[1]) +
    				(slected[2] - measured[2]) * (slected[2] - measured[2]);
    		return Math.sqrt(error);
    	}
        
    }

}
