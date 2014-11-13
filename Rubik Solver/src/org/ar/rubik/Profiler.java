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
	
	public enum Event { START, GREYSCALE, GAUSSIAN, EDGE, DILATION, CONTOUR, POLYGON, RHOMBUS, FACE, CONTROLLER, TOTAL };
	
	private Map<Event,Long> eventSet = new HashMap<Event,Long>(32);
	
	public void markTime(Event event) {
		long time = System.currentTimeMillis();
		eventSet.put(event, time);
	}
	
	/**
	 * Render Time Consumptions Annotation 
	 * 
	 * @param image
	 * @param stateModel2
	 * @return
	 */
	public Mat renderTimeConsumptionMetrics(Mat image, StateModel2 stateModel2) {
		
		RubikFace.drawFlatFaceRepresentation(image, RubikCube.active, 50, 50, 50);
		int index = 0;
				
		renderAndIndex(Event.GREYSCALE,  Event.START,     image, index++);
		renderAndIndex(Event.GAUSSIAN,   Event.GREYSCALE, image, index++);
		renderAndIndex(Event.EDGE,       Event.GAUSSIAN,  image, index++);
		renderAndIndex(Event.DILATION,   Event.EDGE,      image, index++);
		renderAndIndex(Event.CONTOUR,    Event.DILATION,  image, index++);
		renderAndIndex(Event.POLYGON,    Event.CONTOUR,   image, index++);
		renderAndIndex(Event.RHOMBUS,    Event.POLYGON,   image, index++);
		renderAndIndex(Event.FACE,       Event.RHOMBUS,   image, index++);
		renderAndIndex(Event.CONTROLLER, Event.FACE,      image, index++);
		renderAndIndex(Event.TOTAL,      Event.START,     image, index++);

		return image;
	}

	
	/**
	 * Render one line of time consumption
	 * 
	 * @param startEvent TODO
	 * @param image TODO
	 * @param index TODO
	 * @param greyscale
	 */
    private void renderAndIndex(Event endEvent, Event startEvent, Mat image, int index) {
    	long endTimeStamp = eventSet.get(endEvent);
    	long startTimeStamp = eventSet.get(startEvent);
    	String string = String.format("%s %d mS", endEvent.toString(), endTimeStamp - startTimeStamp);
    	Core.putText(image, string, new Point(50, 300 + 50 * index), Constants.FontFace, 2, Constants.ColorWhite, 2);
    }

}
