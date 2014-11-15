/**
 * 
 */
package org.ar.rubik;

import java.util.List;

import org.ar.rubik.Constants.LogicalTile;
import org.ar.rubik.Constants.LogicalTileColorEnum;
import org.ar.rubik.RubikFace2.FaceRecognitionStatusEnum;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

/**
 * @author android.steve@testlens.com
 *
 */
public class Annotation2 {

	private StateModel2 stateModel2;
	
	/**
	 * @param stateModel2
	 */
    public Annotation2(StateModel2 stateModel2) {
	    this.stateModel2 = stateModel2;
    }
    
	/**
	 * Add Annotation
	 * 
	 * @param image
	 * @return
	 */
	public Mat renderAnnotation(Mat image) {
		
		renderFaceOverlayAnnotation(image, false);
		
		switch(RubikMenuAndParameters.annotationMode) {
		
//		case LAYOUT:
////			annotationGlRenderer.setRenderState(false);
//	    	Core.rectangle(image, new Point(0, 0), new Point(450, 720), Constants.ColorBlack, -1);
//			RubikCube.renderFlatLayoutRepresentation(image);
//			break;
			
		case RHOMBUS:
	    	renderRhombusRecognitionMetrics(image, stateModel2.activeRubikFace.rhombusList);
			break;

		case FACE_METRICS:
			renderRubikFaceMetrics(image, stateModel2.activeRubikFace);
			break;
			
//		case CUBE_METRICS:
////			annotationGlRenderer.setRenderState(false);
//		    Core.rectangle(image, new Point(0, 0), new Point(450, 720), Constants.ColorBlack, -1);
//			RubikCube.renderCubeMetrics(image);
//			break;

		case TIME:
			stateModel2.activeRubikFace.profiler.renderTimeConsumptionMetrics(image, stateModel2);
			break;
			
		case COLOR:
			renderFaceColorMetrics(image, stateModel2.activeRubikFace);
			break;
			
		case NORMAL:
			Core.rectangle(image, new Point(0, 0), new Point(350, 720), Constants.ColorBlack, -1);
//			annotationGlRenderer.setRenderState(true);
//			annotationGlRenderer.setCubeOrienation(RubikCube.active);
			break;
		}
		
		return image;
	}

   

	/**
	 * @param image
	 */
    private void renderFaceOverlayAnnotation(Mat img, boolean accepted) {
    	
    	RubikFace2 face = stateModel2.activeRubikFace;
    	
		Scalar color = Constants.ColorBlack;
		switch(face.faceRecognitionStatus) {
		case UNKNOWN:
		case INSUFFICIENT:
		case INVALID_MATH:
			color = Constants.ColorRed;
			break;
		case BAD_METRICS:
		case INCOMPLETE:
		case INADEQUATE:
		case BLOCKED:
		case UNSTABLE:
			color = Constants.ColorOrange;
			break;
		case SOLVED:
			color = accepted ? Constants.ColorGreen : Constants.ColorYellow;
			break;
		}
		
		// Adjust drawing grid to start at edge of cube and not center of a tile.
		double x = face.lmsResult.origin.x - (face.alphaLatticLength * Math.cos(face.alphaAngle) + face.betaLatticLength * Math.cos(face.betaAngle) ) / 2;
		double y = face.lmsResult.origin.y - (face.alphaLatticLength * Math.sin(face.alphaAngle) + face.betaLatticLength * Math.sin(face.betaAngle) ) / 2;

		for(int n=0; n<4; n++) {
			Core.line(
					img,
					new Point(
							x + n * face.alphaLatticLength * Math.cos(face.alphaAngle),
							y + n * face.alphaLatticLength * Math.sin(face.alphaAngle) ), 
					new Point(
							x + (face.betaLatticLength * 3 * Math.cos(face.betaAngle)) + (n * face.alphaLatticLength * Math.cos(face.alphaAngle) ),
							y + (face.betaLatticLength * 3 * Math.sin(face.betaAngle)) + (n * face.alphaLatticLength * Math.sin(face.alphaAngle) ) ), 
					color, 
					3);
		}
		
		for(int m=0; m<4; m++) {
			Core.line(
					img,
					new Point(
							x + m * face.betaLatticLength * Math.cos(face.betaAngle),
							y + m * face.betaLatticLength * Math.sin(face.betaAngle) ), 
					new Point(
							x + (face.alphaLatticLength * 3 * Math.cos(face.alphaAngle)) + (m * face.betaLatticLength * Math.cos(face.betaAngle) ),
							y + (face.alphaLatticLength * 3 * Math.sin(face.alphaAngle)) + (m * face.betaLatticLength * Math.sin(face.betaAngle) ) ), 
					color, 
					3);
		}
		
//		// Draw a circule at the Rhombus reported center of each tile.
//		for(int n=0; n<3; n++) {
//			for(int m=0; m<3; m++) {
//				Rhombus rhombus = faceRhombusArray[n][m];
//				if(rhombus != null)
//					Core.circle(img, rhombus.center, 5, Constants.ColorBlue, 3);
//			}
//		}
//		
//		// Draw the error vector from center of tile to actual location of Rhombus.
//		for(int n=0; n<3; n++) {
//			for(int m=0; m<3; m++) {
//				Rhombus rhombus = faceRhombusArray[n][m];
//				if(rhombus != null) {
//					
//					Point tileCenter = getTileCenterInPixels(n, m);				
//					Core.line(img, tileCenter, rhombus.center, Constants.ColorRed, 3);
//					Core.circle(img, tileCenter, 5, Constants.ColorBlue, 1);
//				}
//			}
//		}
		
		// Draw reported Logical Tile Color Characters in center of each tile.
		if(face.faceRecognitionStatus == FaceRecognitionStatusEnum.SOLVED)
			for(int n=0; n<3; n++) {
				for(int m=0; m<3; m++) {

					// Draw tile character in UV plane
					Point tileCenterInPixels = face.getTileCenterInPixels(n, m);
					tileCenterInPixels.x -= 10.0;
					tileCenterInPixels.y += 10.0;
					String text = Character.toString(face.logicalTileArray[n][m].character);
					Core.putText(img, text, tileCenterInPixels, Constants.FontFace, 3, Constants.ColorBlack, 3);
				}
			}
		
		// Also draw recognized Rhombi for clarity.
		if(face.faceRecognitionStatus != FaceRecognitionStatusEnum.SOLVED)
			for(Rhombus rhombus : face.rhombusList)
				rhombus.draw(img, Constants.ColorGreen);
	}

	/**
	 * Draw Flat Face Representation
	 * 
	 * This depicts the faces as recognized, but without any rotation.
	 * That is, rendering orientates faces with respect to N and M axis.
	 * 
	 * @param image
	 * @param rubikFace
	 * @param x
	 * @param y
	 * @param tileSize Size of tiles in pixels
	 */
	private static void drawFlatFaceRepresentation(Mat image, RubikFace2 rubikFace, double x, double y, int tileSize) {
		
		if(rubikFace == null) {
			Core.rectangle(image, new Point( x, y), new Point( x + 3*tileSize, y + 3*tileSize), Constants.ColorGrey, -1);
		}

		else if(rubikFace.faceRecognitionStatus != FaceRecognitionStatusEnum.SOLVED) {
			Core.rectangle(image, new Point( x, y), new Point( x + 3*tileSize, y + 3*tileSize), Constants.ColorGrey, -1);
		}
		else

			for(int n=0; n<3; n++) {
				for(int m=0; m<3; m++) {
					LogicalTile logicalTile = rubikFace.logicalTileArray[n][m];
					if(logicalTile != null)
						Core.rectangle(image, new Point( x + tileSize * n, y + tileSize * m), new Point( x + tileSize * (n + 1), y + tileSize * (m + 1)), logicalTile.color, -1);//Core.CV_FILLED);
				}
			}
	}
	
	

	/**
	 * Render Rhombus Recognition Metrics
	 * 
	 * @param image
	 * @param rhombusList
	 */
    private void renderRhombusRecognitionMetrics(Mat image, List<Rhombus> rhombusList) {
    	
//		RubikFace.drawFlatFaceRepresentation(image, RubikCube.active, 50, 50, 50);

    	Core.rectangle(image, new Point(0, 0), new Point(450, 720), Constants.ColorBlack, -1);
    	
		int totalNumber = 0;
		int totalNumberValid = 0;
		
		int totalNumberUnknow = 0;
		int totalNumberNot4Points = 0;
		int totalNumberNotConvex = 0;
		int totalNumberBadArea = 0;
		int totalNumberClockwise = 0;
		int totalNumberOutlier = 0;

		// Loop over Rhombus list and total status types.
		for(Rhombus rhombus : rhombusList)  {

			switch(rhombus.status) {
			case NOT_PROCESSED:
				totalNumberUnknow++;
				break;
			case NOT_4_POINTS:
				totalNumberNot4Points++;
				break;
			case NOT_CONVEX:
				totalNumberNotConvex++;
				break;
			case AREA:
				totalNumberBadArea++;
				break;
			case CLOCKWISE:
				totalNumberClockwise++;
				break;
			case OUTLIER:
				totalNumberOutlier++;
				break;
			case VALID:
				totalNumberValid++;
				break;
			default:
				break;
			}
			totalNumber++;
		}
		
		Core.putText(image, "Num Unknown: " + totalNumberUnknow,          new Point(50, 300), Constants.FontFace, 2, Constants.ColorWhite, 2);
		Core.putText(image, "Num Not 4 Points: " + totalNumberNot4Points, new Point(50, 350), Constants.FontFace, 2, Constants.ColorWhite, 2);
		Core.putText(image, "Num Not Convex: " + totalNumberNotConvex,    new Point(50, 400), Constants.FontFace, 2, Constants.ColorWhite, 2);
		Core.putText(image, "Num Bad Area: " + totalNumberBadArea,        new Point(50, 450), Constants.FontFace, 2, Constants.ColorWhite, 2);
		Core.putText(image, "Num Clockwise: " + totalNumberClockwise,     new Point(50, 500), Constants.FontFace, 2, Constants.ColorWhite, 2);
		Core.putText(image, "Num Outlier: " + totalNumberOutlier,         new Point(50, 550), Constants.FontFace, 2, Constants.ColorWhite, 2);
		Core.putText(image, "Num Valid: " + totalNumberValid,             new Point(50, 600), Constants.FontFace, 2, Constants.ColorWhite, 2);
		Core.putText(image, "Total Num: " + totalNumber,                  new Point(50, 650), Constants.FontFace, 2, Constants.ColorWhite, 2);
    }


	/**
	 * Diagnostic Text Rendering of Rubik Face Metrics
	 * 
	 * @param image
	 * @param activeRubikFace
	 */
    private void renderRubikFaceMetrics(Mat image, RubikFace2 activeRubikFace) {

    	Core.rectangle(image, new Point(0, 0), new Point(450, 720), Constants.ColorBlack, -1);
    	
    	if(activeRubikFace == null)
    		return;
		
    	RubikFace2 face = activeRubikFace;
		drawFlatFaceRepresentation(image, face, 50, 50, 50);

		Core.putText(image, "Status = " + face.faceRecognitionStatus,                              new Point(50, 300), Constants.FontFace, 2, Constants.ColorWhite, 2);
		Core.putText(image, String.format("AlphaA = %4.1f", face.alphaAngle * 180.0 / Math.PI),    new Point(50, 350), Constants.FontFace, 2, Constants.ColorWhite, 2);
		Core.putText(image, String.format("BetaA  = %4.1f", face.betaAngle  * 180.0 / Math.PI),    new Point(50, 400), Constants.FontFace, 2, Constants.ColorWhite, 2);
		Core.putText(image, String.format("AlphaL = %4.0f", face.alphaLatticLength),               new Point(50, 450), Constants.FontFace, 2, Constants.ColorWhite, 2);
		Core.putText(image, String.format("Beta L = %4.0f", face.betaLatticLength),                new Point(50, 500), Constants.FontFace, 2, Constants.ColorWhite, 2);
		Core.putText(image, String.format("Gamma  = %4.2f", face.gammaRatio),                      new Point(50, 550), Constants.FontFace, 2, Constants.ColorWhite, 2);
		Core.putText(image, String.format("Sigma  = %5.0f", face.lmsResult.sigma),                 new Point(50, 600), Constants.FontFace, 2, Constants.ColorWhite, 2);
		Core.putText(image, String.format("Moves  = %d",    face.numRhombusMoves),                 new Point(50, 650), Constants.FontFace, 2, Constants.ColorWhite, 2);
		Core.putText(image, String.format("#Rohmbi= %d",    face.rhombusList.size()),              new Point(50, 700), Constants.FontFace, 2, Constants.ColorWhite, 2);

    }
    
    
    
	/**
	 * @param image
	 * @param face
	 */
    private void renderFaceColorMetrics(Mat image, RubikFace2 face) {
    	
    	Core.rectangle(image, new Point(0, 0), new Point(570, 720), Constants.ColorBlack, -1);
    	
		if(face.faceRecognitionStatus != FaceRecognitionStatusEnum.SOLVED)
			return;

		// Draw simple grid
		Core.rectangle(image, new Point(-256 + 256, -256 + 400), new Point(256 + 256, 256 + 400), Constants.ColorWhite);
		Core.line(image, new Point(0 + 256, -256 + 400), new Point(0 + 256, 256 + 400), Constants.ColorWhite);		
		Core.line(image, new Point(-256 + 256, 0 + 400), new Point(256 + 256, 0 + 400), Constants.ColorWhite);
		Core.putText(image, String.format("Luminosity Offset = %4.0f", face.luminousOffset), new Point(0, -256 + 400 - 60), Constants.FontFace, 2, Constants.ColorWhite, 2);
		Core.putText(image, String.format("Color Error Before Corr = %4.0f", face.colorErrorBeforeCorrection), new Point(0, -256 + 400 - 30), Constants.FontFace, 2, Constants.ColorWhite, 2);
		Core.putText(image, String.format("Color Error After Corr = %4.0f", face.colorErrorAfterCorrection), new Point(0, -256 + 400), Constants.FontFace, 2, Constants.ColorWhite, 2);

		for(int n=0; n<3; n++) {
			for(int m=0; m<3; m++) {

				double [] measuredTileColor = face.measuredColorArray[n][m];
//				Log.e(Constants.TAG, "RGB: " + logicalTileArray[n][m].character + "=" + actualTileColor[0] + "," + actualTileColor[1] + "," + actualTileColor[2] + " x=" + x + " y=" + y );
				double[] measuredTileColorYUV   = Util.getYUVfromRGB(measuredTileColor);

//				if(measuredTileColor == null)
//					return;

//				Log.e(Constants.TAG, "Lum: " + logicalTileArray[n][m].character + "=" + acutalTileYUV[0]);

				
				double luminousScaled     = measuredTileColorYUV[0] * 2 - 256;
				double uChromananceScaled = measuredTileColorYUV[1] * 2;
				double vChromananceScaled = measuredTileColorYUV[2] * 2;

				String text = Character.toString(face.logicalTileArray[n][m].character);
				
				// Draw tile character in UV plane
				Core.putText(image, text, new Point(uChromananceScaled + 256, vChromananceScaled + 400), Constants.FontFace, 3, face.logicalTileArray[n][m].color, 3);
				
				// Draw tile characters on right side for Y axis
				Core.putText(image, text, new Point(512 - 40, luminousScaled + 400 + face.luminousOffset  + RubikMenuAndParameters.luminousOffsetParam.value), Constants.FontFace, 3, face.logicalTileArray[n][m].color, 3);
				Core.putText(image, text, new Point(512 + 20, luminousScaled + 400), Constants.FontFace, 3, face.logicalTileArray[n][m].color, 3);
//				Log.e(Constants.TAG, "Lum: " + logicalTileArray[n][m].character + "=" + luminousScaled);
			}
		}

		Scalar rubikRed    = Constants.logicalTileColorArray[LogicalTileColorEnum.RED.ordinal()].color;
		Scalar rubikOrange = Constants.logicalTileColorArray[LogicalTileColorEnum.ORANGE.ordinal()].color;
		Scalar rubikYellow = Constants.logicalTileColorArray[LogicalTileColorEnum.YELLOW.ordinal()].color;
		Scalar rubikGreen  = Constants.logicalTileColorArray[LogicalTileColorEnum.GREEN.ordinal()].color;
		Scalar rubikBlue   = Constants.logicalTileColorArray[LogicalTileColorEnum.BLUE.ordinal()].color;
		Scalar rubikWhite  = Constants.logicalTileColorArray[LogicalTileColorEnum.WHITE.ordinal()].color;

		
		// Render Color Calibration in UV plane as dots
		Core.circle(image, new Point(2*Util.getYUVfromRGB(rubikRed.val)[1] +    256, 2*Util.getYUVfromRGB(rubikRed.val)[2] + 400), 10, rubikRed, -1);
		Core.circle(image, new Point(2*Util.getYUVfromRGB(rubikOrange.val)[1] + 256, 2*Util.getYUVfromRGB(rubikOrange.val)[2] + 400), 10, rubikOrange, -1);
		Core.circle(image, new Point(2*Util.getYUVfromRGB(rubikYellow.val)[1] + 256, 2*Util.getYUVfromRGB(rubikYellow.val)[2] + 400), 10, rubikYellow, -1);
		Core.circle(image, new Point(2*Util.getYUVfromRGB(rubikGreen.val)[1] +  256, 2*Util.getYUVfromRGB(rubikGreen.val)[2] + 400), 10, rubikGreen, -1);
		Core.circle(image, new Point(2*Util.getYUVfromRGB(rubikBlue.val)[1] +   256, 2*Util.getYUVfromRGB(rubikBlue.val)[2] + 400), 10, rubikBlue, -1);
		Core.circle(image, new Point(2*Util.getYUVfromRGB(rubikWhite.val)[1] +  256, 2*Util.getYUVfromRGB(rubikWhite.val)[2] + 400), 10, rubikWhite, -1);

		// Render Color Calibration on right side Y axis as dots
		Core.line(image, new Point(502, -256 + 2*Util.getYUVfromRGB(rubikRed.val)[0] + 400),    new Point(522, -256 + 2*Util.getYUVfromRGB(rubikRed.val)[0] + 400), rubikRed, 3);
		Core.line(image, new Point(502, -256 + 2*Util.getYUVfromRGB(rubikOrange.val)[0] + 400), new Point(522, -256 + 2*Util.getYUVfromRGB(rubikOrange.val)[0] + 400), rubikOrange, 3);
		Core.line(image, new Point(502, -256 + 2*Util.getYUVfromRGB(rubikGreen.val)[0] + 400),  new Point(522, -256 + 2*Util.getYUVfromRGB(rubikGreen.val)[0] + 400), rubikGreen, 3);
		Core.line(image, new Point(502, -256 + 2*Util.getYUVfromRGB(rubikYellow.val)[0] + 400), new Point(522, -256 + 2*Util.getYUVfromRGB(rubikYellow.val)[0] + 400), rubikYellow, 3);
		Core.line(image, new Point(502, -256 + 2*Util.getYUVfromRGB(rubikBlue.val)[0] + 400),   new Point(522, -256 + 2*Util.getYUVfromRGB(rubikBlue.val)[0] + 400), rubikBlue, 3);
		Core.line(image, new Point(502, -256 + 2*Util.getYUVfromRGB(rubikWhite.val)[0] + 400),  new Point(522, -256 + 2*Util.getYUVfromRGB(rubikWhite.val)[0] + 400), rubikWhite, 3); 
    }

}
