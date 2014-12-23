/**
 * Augmented Reality Rubik Cube Solver
 * 
 * Author: Steven P. Punte (aka Android Steve)
 * Date:   Nov 1st 2014
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
package org.ar.rubik;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import android.util.Log;

public class TileLayoutAlgorithm {
	
	private static List<Rhombus> rhombusList = null;
	
	private static double alphaAngle;
	private static double betaAngle;
	

	/**
	 * Initial Layout Algorithm
	 * 
	 * Attempt a two-dimensional sort into, of course, a 3x3 array.
	 * 
	 * Algorithm:
	 *   o  X and Y axis values are converted to "alpha" and "beta" position
	 *      in, roughly, units of pixels.
	 *      
	 *   o  Along alpha and beta axis, but not simultaneously, sort Rohmbi
	 *      w.r.t., alpha or beta, into three sets: Low, Mid, and High.
	 *      
	 *      Some conditions on sort:
	 *      o All sets must have at least 1 Rhombus.
	 *      o Minimize: Sum {   Sum  { (R_i - R_j)^2 } } where i and j are in the same set.
	 *      
	 *   o  Populate Rhombus Face Array according to sorted sets.   
	 *      
	 * @param faceRhombusArray 
	 * 
	 * 
	 * @param rubikFace
	 * @return 
	 */
	public static boolean doInitialLayout(
			List<Rhombus> _rhombusList, 
			Rhombus[][] _rhombusFaceArray,
			double _alphaAngle,
			double _betaAngle) {

		rhombusList = _rhombusList;
		alphaAngle = _alphaAngle;
		betaAngle = _betaAngle;
		
		// Sort Rhombi into three sets along alpha axis.
		List<Collection<Rhombus>> alphaListOfSets = createOptimizedListOfRhombiSets(
				new Comparator<Rhombus>() {
					@Override
					public int compare(Rhombus rhombus0, Rhombus rhombus1) {
						return (getAlpha(rhombus0) - getAlpha(rhombus1));
					} } );
			
		// Sort Rhombi into three sets along beta axis.
		List<Collection<Rhombus>> betaListOfSets = createOptimizedListOfRhombiSets(
				new Comparator<Rhombus>() {
					@Override
					public int compare(Rhombus rhombus0, Rhombus rhombus1) {
						return (getBeta(rhombus0) - getBeta(rhombus1));
					} } );
		

		// Fill Rhombus Face Array
		// Loop over N and M indicies.
		for(int n=0; n<3; n++) {
			for(int m=0; m<3; m++) {
				
			    // Get candidate Rhombi that have the M and N indices. 
				Collection<Rhombus> alphaSet = alphaListOfSets.get(n);
				Collection<Rhombus> betaSet = betaListOfSets.get(m);
				
				// Find Rhmobi that have both the desired M and N indices.
				List<Rhombus> commonElements = findCommonElements(alphaSet, betaSet);
				
				if(commonElements.size() == 0)
					_rhombusFaceArray[n][m] = null; // No Rhombus for this tile
				
				else if(commonElements.size() == 1) 
					_rhombusFaceArray[n][m] = commonElements.get(0);  // Desired result
				
				else {
					// Problem, more than one Rhombus seem candidate for this tile location.
					// Just use first
					_rhombusFaceArray[n][m] = commonElements.get(0);
					Log.w(Constants.TAG, "Excess Rhombi Candidate(s) ");
					// =+= Possibly put in extra set ??
				}
			}
		}
		
		
//		Log.d(Constants.TAG, "Alpha Low  Set: " + rhombiSetToString(alphaListOfSets.get(0)));
//		Log.d(Constants.TAG, "Alpha Mid  Set: " + rhombiSetToString(alphaListOfSets.get(1)));
//		Log.d(Constants.TAG, "Alpha High Set: " + rhombiSetToString(alphaListOfSets.get(2)));
//		Log.d(Constants.TAG, "Beta  Low  Set: " + rhombiSetToString(betaListOfSets.get(0)));
//		Log.d(Constants.TAG, "Beta  Mid  Set: " + rhombiSetToString(betaListOfSets.get(1)));
//		Log.d(Constants.TAG, "Beta  High Set: " + rhombiSetToString(betaListOfSets.get(2)));
		
		
		// Diagnostic Print
		Log.i(Constants.TAG, String.format( " m:n|--------------0--------------|---------------1-------------|-------------2---------------|") );
		Log.i(Constants.TAG, String.format( " 0  |%s|%s|%s|", rhombusToString(_rhombusFaceArray[0][0]), rhombusToString(_rhombusFaceArray[1][0]), rhombusToString(_rhombusFaceArray[2][0])));
		Log.i(Constants.TAG, String.format( " 1  |%s|%s|%s|", rhombusToString(_rhombusFaceArray[0][1]), rhombusToString(_rhombusFaceArray[1][1]), rhombusToString(_rhombusFaceArray[2][1]))); 
		Log.i(Constants.TAG, String.format( " 2  |%s|%s|%s|", rhombusToString(_rhombusFaceArray[0][2]), rhombusToString(_rhombusFaceArray[1][2]), rhombusToString(_rhombusFaceArray[2][2]))); 
		Log.i(Constants.TAG, String.format( "    |-----------------------------|-----------------------------|-----------------------------|") );
		
		// Check that there is at least on Rhombus in each row and column.
		if(_rhombusFaceArray[0][0] == null && _rhombusFaceArray[0][1] == null && _rhombusFaceArray[0][2] == null) return false;
		if(_rhombusFaceArray[1][0] == null && _rhombusFaceArray[1][1] == null && _rhombusFaceArray[1][2] == null) return false;
		if(_rhombusFaceArray[2][0] == null && _rhombusFaceArray[2][1] == null && _rhombusFaceArray[2][2] == null) return false;
		if(_rhombusFaceArray[0][0] == null && _rhombusFaceArray[1][0] == null && _rhombusFaceArray[2][0] == null) return false;
		if(_rhombusFaceArray[0][1] == null && _rhombusFaceArray[1][1] == null && _rhombusFaceArray[2][1] == null) return false;
		if(_rhombusFaceArray[0][2] == null && _rhombusFaceArray[1][2] == null && _rhombusFaceArray[2][2] == null) return false;
		
		return true;
		
	}

	

	/**
	 * Create Optimized List Of Rhombi Sets with respect to provided comparator.
	 * 
	 * Creates three set of Rhombi: Low, Medium, and High.  
	 * 
	 * @param comparator
	 * @return
	 */
	private static List<Collection<Rhombus>> createOptimizedListOfRhombiSets(Comparator<Rhombus> comparator) {

		double best_error = Double.POSITIVE_INFINITY;
		int best_p = 0;
		int best_q = 0;
		
		int n = rhombusList.size();
		
		// First just perform a linear sort: smallest to largest.
		ArrayList<Rhombus> sortedRhombusList = new ArrayList<Rhombus>(rhombusList);
		Collections.sort(
				sortedRhombusList, 
				comparator);
		
//		for(Rhombus rhombus : sortedRhombusList)
//			Log.d(Constants.TAG, String.format("Sorted Rhombi List: x=%4.0f y=%4.0f alpha=%d beta=%d", rhombus.center.x, rhombus.center.y, getAlpha(rhombus), getBeta(rhombus)));
			
		
		// Next search overall all partition possibilities, and find that with the least error w.r.t. provided comparator.
		for(int p=1; p < n-1; p++)  {
			for(int q=p+1; q<n; q++)  {
				double error = calculateErrorAccordingToPartition_P_Q(
						sortedRhombusList,
						comparator,
						p,
						q);
				
				if(error < best_error) {
					best_error = error;
					best_p = p;
					best_q = q;
				}
			}
		}
		
//		Log.d(Constants.TAG, String.format("createOptimizedListOfRhombiSets: Selected p=%d and q=%d N=%d", best_p, best_q, n));
		
		LinkedList<Collection<Rhombus>> result = new LinkedList<Collection<Rhombus>>();
		result.add( sortedRhombusList.subList(0, best_p));
		result.add( sortedRhombusList.subList(best_p, best_q));
		result.add( sortedRhombusList.subList(best_q, n));
		return result;
	}
	
	
	
	/**
	 * Calculate Error According To Partition P and Q and provided comparator
	 * 
	 * Low Set  are elements 0 to P-1
	 * Mid Set  are elements P to Q-1
	 * High Set are elements Q to N-1
	 * 
	 * Sum of error of each set created by partition P and Q
	 * 
	 * @param sortedRhombusList
	 * @param comparator 
	 * @param p
	 * @param q
	 * @return sum square of error
	 */
	private static int calculateErrorAccordingToPartition_P_Q(
			ArrayList<Rhombus> sortedRhombusList, 
			Comparator<Rhombus> comparator, 
			int p, 
			int q) {
	
		int n = sortedRhombusList.size();
		
		int sum =
				calculateSumSquaredErrorOfSet(sortedRhombusList.subList(0, p), comparator) +
				calculateSumSquaredErrorOfSet(sortedRhombusList.subList(p, q), comparator) +
				calculateSumSquaredErrorOfSet(sortedRhombusList.subList(q, n), comparator);

//		Log.d(Constants.TAG, String.format("calculateErrorAccordingToPartition_P_Q: sum=%d for p=%d and q=%d", sum, p, q));
		
		return sum;
	}
	
	
	
	/**
	 * Calculate Sum Squared Error Of Set with respect to provided comparator.
	 * 
	 * @param subList
	 * @param comparator
	 * @return
	 */
	private static int calculateSumSquaredErrorOfSet(List<Rhombus> subList, Comparator<Rhombus> comparator) {
		
		int n = subList.size();
		int sumSquared = 0;
		
		for(int i=0; i<n-1; i++) {
			for( int j=i+1; j<n; j++) {
				int cmp = comparator.compare(subList.get(i), subList.get(j));
				sumSquared += cmp * cmp;
			}
		}

//		Log.d(Constants.TAG, String.format("calculateSumSquaredErrorOfSet: sum=%d for size=%d", sumSquared, n));

		return sumSquared;
	}

	

	/**
	 * Find Common Elements and return in a new List.
	 * 
	 * 
	 * @param alphaSet
	 * @param betaSet
	 * @return
	 */
	private static List<Rhombus> findCommonElements(Collection<Rhombus> alphaSet, Collection<Rhombus> betaSet) {
		List<Rhombus> result = new LinkedList<Rhombus>(alphaSet);
		result.retainAll(betaSet);
		return result;
	}	

	
	
	private static int getAlpha(Rhombus rhombus) {
		return (int)(rhombus.center.x * Math.cos(alphaAngle) + rhombus.center.y * Math.sin(alphaAngle));
	}


	private static int getBeta(Rhombus rhombus) {
		return (int)(rhombus.center.x * Math.cos(betaAngle)  + rhombus.center.y * Math.sin(betaAngle));
	}

	@SuppressWarnings("unused")
	private static String rhombiSetToString(Collection<Rhombus> collection) {		
		StringBuffer buffer = new StringBuffer();
		
		for(Rhombus rhombus : collection) 
			buffer.append(rhombusToString(rhombus));
		
		return buffer.toString();
	}

	private static String rhombusToString(Rhombus rhombus) {
		if(rhombus == null)
			return "----------null---------------";
		else
			return String.format("{x=%4.0f y=%4.0f a=%4d b=%4d}", rhombus.center.x, rhombus.center.y, getAlpha(rhombus), getBeta(rhombus));
	}	
	
}
