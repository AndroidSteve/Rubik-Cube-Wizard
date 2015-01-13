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
 *   The purpose of this class is to record timestamps and also provide for 
 *   diagnostic annotation.
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

import java.util.HashMap;
import java.util.Map;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;

/**
 * @author android.steve@testlens.com
 *
 */
public class Profiler {
	
	public enum Event { START, GREYSCALE, GAUSSIAN, EDGE, DILATION, CONTOUR, POLYGON, RHOMBUS, FACE, POSE, CONTROLLER, TOTAL};
	
	// Store time stamps of various events.
	private Map<Event,Long> eventSet = new HashMap<Event,Long>(32);
	
	// Store minimum event times so far observed.
	private static Map<Event,Long> minEventSet = new HashMap<Event, Long>(32);
	
	// Store time stamp for Frames Per Second
	private static long framesPerSecondTimeStamp = 0;
	
	private boolean scheduleReset = false;
	
	public void markTime(Event event) {
		long time = System.currentTimeMillis();
		eventSet.put(event, time);
		if(minEventSet.containsKey(event) == false)
			minEventSet.put(event, Long.MAX_VALUE);
	}
	
	public void reset() {
		scheduleReset = true;
	}
	
	/**
	 * Render Time Consumptions Annotation 
	 * 
	 * @param image
	 * @param stateModel
	 * @return
	 */
	public Mat drawTimeConsumptionMetrics(Mat image, StateModel stateModel) {

    	Core.rectangle(image, new Point(0, 0), new Point(500, 720), Constants.ColorBlack, -1);
		int index = 0;
		
        long newTimeStamp = System.currentTimeMillis();
		if(framesPerSecondTimeStamp > 0)  {
		    long frameTime = newTimeStamp - framesPerSecondTimeStamp;
		    double framesPerSecond = 1000.0 / frameTime;
		    String string = String.format("Frames Per Second: %4.1f", framesPerSecond);
		    Core.putText(image, string, new Point(50, 100), Constants.FontFace, 2, Constants.ColorWhite, 2);
		}
        framesPerSecondTimeStamp = newTimeStamp;


		Core.putText(image, "Event    Time  Min", new Point(50, 150), Constants.FontFace, 2, Constants.ColorWhite, 2);
				
		renderAndIndex(Event.GREYSCALE,  Event.START,     image, index++);
		renderAndIndex(Event.GAUSSIAN,   Event.GREYSCALE, image, index++);
		renderAndIndex(Event.EDGE,       Event.GAUSSIAN,  image, index++);
		renderAndIndex(Event.DILATION,   Event.EDGE,      image, index++);
		renderAndIndex(Event.CONTOUR,    Event.DILATION,  image, index++);
		renderAndIndex(Event.POLYGON,    Event.CONTOUR,   image, index++);
		renderAndIndex(Event.RHOMBUS,    Event.POLYGON,   image, index++);
        renderAndIndex(Event.FACE,       Event.RHOMBUS,   image, index++);
        renderAndIndex(Event.POSE,       Event.FACE,      image, index++);
		renderAndIndex(Event.CONTROLLER, Event.POSE,      image, index++);
		renderAndIndex(Event.TOTAL,      Event.START,     image, index++);
		
		if(scheduleReset == true) {
			minEventSet = new HashMap<Event, Long>(32);
			scheduleReset = false;
		}

		return image;
	}

	
	/**
	 * Render one line of time consumption
	 * 
     * @param endEvent
     * @param startEvent
     * @param image
     * @param index
     */
    private void renderAndIndex(Event endEvent, Event startEvent, Mat image, int index) {
    	
    	// No measurement yet for this event type.
    	if(eventSet.containsKey(endEvent) == false) {
    		Core.putText(image, endEvent.toString() + ": NA", new Point(50, 200 + 50 * index), Constants.FontFace, 2, Constants.ColorWhite, 2);
    	}
    	
    	// If total, perform special processing.  Specifically, add up and report all minimums found in 
    	// has set instead of measuring and recording a minimum total.  Thus, this number should converge
    	// more quickly to the desired value.
    	else if(endEvent == Event.TOTAL) {
 
    		long endTimeStamp = eventSet.get(endEvent);
    		long startTimeStamp = eventSet.get(startEvent);
    		long elapsedTime = endTimeStamp - startTimeStamp;
    		
    		// Sum up all minimum times: this converges faster than recording the minimum total time and should be the same.
    		long minValue = 0;
    		for(long minEventTime : minEventSet.values())
    			minValue += minEventTime;

			String string = String.format("%10s: %3dmS %3dmS", endEvent.toString(), elapsedTime, minValue);
    		Core.putText(image, string, new Point(50, 200 + 50 * index), Constants.FontFace, 2, Constants.ColorWhite, 2);
    	}
    	
    	// Render time and minimum tile for this event type.
    	else {
    		long endTimeStamp = eventSet.get(endEvent);
    		long startTimeStamp = eventSet.get(startEvent);
    		long elapsedTime = endTimeStamp - startTimeStamp;
    		
    		long minValue = minEventSet.get(endEvent);
    		if(elapsedTime < minValue) {
    			minValue = elapsedTime;
    			minEventSet.put(endEvent, minValue);
    		}

			String string = String.format("%10s: %3dmS %3dmS", endEvent.toString(), elapsedTime, minValue);
    		Core.putText(image, string, new Point(50, 200 + 50 * index), Constants.FontFace, 2, Constants.ColorWhite, 2);
    	}
    }

}
