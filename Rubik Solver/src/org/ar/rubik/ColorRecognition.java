/**
 * Augmented Reality Rubik Cube Solver
 * 
 * Author: Steven P. Punte (aka Android Steve : android.steve@cl-sw.com)
 * Date:   Nov 1st 2014
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
import org.opencv.core.Scalar;

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
        
        public void faceTileColorRecognition(RubikFace rubicFace) {
            
        }
        
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
