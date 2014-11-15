/**
 * 
 */
package org.ar.rubik;

import java.util.List;

import org.ar.rubik.Constants.LogicalTile;
import org.ar.rubik.RubikFace2.FaceRecognitionStatusEnum;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;

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
//			annotationGlRenderer.setRenderState(false);
//			if(RubikCube.active != null) {
//		    	Core.rectangle(image, new Point(0, 0), new Point(450, 720), Constants.ColorBlack, -1);
//				RubikCube.active.renderFaceRecognitionMetrics(image);
//			}
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
			
//		case COLOR:
////			annotationGlRenderer.setRenderState(false);
//			if(RubikCube.active != null) {
//		    	Core.rectangle(image, new Point(0, 0), new Point(570, 720), Constants.ColorBlack, -1);
//				RubikCube.active.renderColorMetrics(image);
//			}
//			break;
			
		case NORMAL:
			Core.rectangle(image, new Point(0, 0), new Point(350, 720), Constants.ColorBlack, -1);
//			annotationGlRenderer.setRenderState(true);
//			annotationGlRenderer.setCubeOrienation(RubikCube.active);
			break;
		}
		
		return image;
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

}
