package org.kociemba.twophase;

import static org.kociemba.twophase.CoordCube.FRtoBR_Move;
import static org.kociemba.twophase.CoordCube.MergeURtoULandUBtoDF;
import static org.kociemba.twophase.CoordCube.N_FLIP;
import static org.kociemba.twophase.CoordCube.N_FRtoBR;
import static org.kociemba.twophase.CoordCube.N_MOVE;
import static org.kociemba.twophase.CoordCube.N_PARITY;
import static org.kociemba.twophase.CoordCube.N_SLICE1;
import static org.kociemba.twophase.CoordCube.N_SLICE2;
import static org.kociemba.twophase.CoordCube.N_TWIST;
import static org.kociemba.twophase.CoordCube.N_UBtoDF;
import static org.kociemba.twophase.CoordCube.N_URFtoDLF;
import static org.kociemba.twophase.CoordCube.N_URtoDF;
import static org.kociemba.twophase.CoordCube.N_URtoUL;
import static org.kociemba.twophase.CoordCube.Slice_Flip_Prun;
import static org.kociemba.twophase.CoordCube.Slice_Twist_Prun;
import static org.kociemba.twophase.CoordCube.Slice_URFtoDLF_Parity_Prun;
import static org.kociemba.twophase.CoordCube.Slice_URtoDF_Parity_Prun;
import static org.kociemba.twophase.CoordCube.UBtoDF_Move;
import static org.kociemba.twophase.CoordCube.URFtoDLF_Move;
import static org.kociemba.twophase.CoordCube.URtoDF_Move;
import static org.kociemba.twophase.CoordCube.URtoUL_Move;
import static org.kociemba.twophase.CoordCube.flipMove;
import static org.kociemba.twophase.CoordCube.getPruning;
import static org.kociemba.twophase.CoordCube.parityMove;
import static org.kociemba.twophase.CoordCube.setPruning;
import static org.kociemba.twophase.CoordCube.twistMove;

/**
 * <p>This class provides (very basic) control over the loading of the pruning tables in {@link CoordCube} for Herbert Kociemba's 
 * <i>Twophase Solver</i>.
 * 
 * <p>This class will mostly be used like this:
 * <pre><code>
 * PruneTableLoader tableLoader = new PruneTableLoader();
 * while (!tableLoader.loadingFinished())
 *       loadNext();
 * </pre></code>
 * 
 * <p><i>Note</i>: For this class to have any effect, you must have replaced {@link CoordCube} with the custom 
 * implementation beforehand.
 * 
 * @author Herbert Kociemba <i>(generation of the pruning tables)</i>
 * @author Elias Frantar <i>(implementation of this class)</i>
 * @version 2014-8-16
 */
public class PruneTableLoader {
	private static final int TABLES = 12; // there are 12 different pruning tables to load
	
	private int tablesLoaded; // the number of already loaded tables
	
	/**
	 * Constructor<br>
	 * Number of tables loaded is set to 0.
	 */
	public PruneTableLoader() {
		tablesLoaded = 0;
	}
	
	/**
	 * Loads the next pruning table if and only if it is <i>null</i>.<br>
	 * Equivalent to <code>loadNext(false);</code>
	 */
	public void loadNext() { loadNext(false); }
	
	/**
	 * Loads the next pruning table.
	 * @param force if true override it even if it already exists; if false only load when it is <i>null</i>
	 */
	public void loadNext(boolean force) {
		switch(tablesLoaded++) {
			case 0:  loadTwistMoves(force);		 		 break;
			case 1:  loadFlipMoves(force);  		 	 break;
			case 2:  loadFRtoBRMoves(force);		 	 break;
			case 3:  loadURFtoDLFMoves(force);	 		 break;
			case 4:  loadURtoDFMoves(force);		 	 break;
			case 5:  loadURtoULMoves(force);		 	 break;
			case 6:  loadUBtoDFMoves(force);		 	 break;
			case 7:  mergeURtoULandUBtoDF(force); 		 break;
			case 8:  loadSliceURFtoDLFParityPrun(force); break;
			case 9:  loadSliceURtoDFParityPrun(force);	 break;
			case 10: loadSliceTwistPrune(force);		 break;
			case 11: loadSliceFlipPrune(force);			 break;
			default: break;
		}
	}
	
	/**
	 * Determines if all pruning tables have already been loaded by using this class.
	 * @return true if all tables have already been loaded; false otherwise
	 */
	public boolean loadingFinished() {
		return tablesLoaded >= TABLES;
	}
	
	/*
	 * Methods for loading each individual pruning table.
	 * @param force if true override the table even it already exists; if false only load it when it is <i>null</i>
	 * 
	 * Code and commments have been directly copied from {@author Herbert Kociemba}'s original CoordCube-class.
	 */
	
	// ******************************************Phase 1 move tables*****************************************************

	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// Move table for the twists of the corners
	// twist < 2187 in phase 2.
	// twist = 0 in phase 2.
	private void loadTwistMoves(boolean force) {
		/* only load if not already loaded */
		if (!force && twistMove != null)
			return;
		twistMove = new short[N_TWIST][N_MOVE];
		
		CubieCube a = new CubieCube();
		for (short i = 0; i < N_TWIST; i++) {
			a.setTwist(i);
			for (int j = 0; j < 6; j++) {
				for (int k = 0; k < 3; k++) {
					a.cornerMultiply(CubieCube.moveCube[j]);
					twistMove[i][3 * j + k] = a.getTwist();
				}
				a.cornerMultiply(CubieCube.moveCube[j]); // 4. faceturn restores a
			}
		}
	}
	
	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// Move table for the flips of the edges
	// flip < 2048 in phase 1
	// flip = 0 in phase 2.
	private void loadFlipMoves(boolean force) {
		if (!force && flipMove != null)
			return;
		flipMove = new short[N_FLIP][N_MOVE];
		
		CubieCube a = new CubieCube();
		for (short i = 0; i < N_FLIP; i++) {
			a.setFlip(i);
			for (int j = 0; j < 6; j++) {
				for (int k = 0; k < 3; k++) {
					a.edgeMultiply(CubieCube.moveCube[j]);
					flipMove[i][3 * j + k] = a.getFlip();
				}
				a.edgeMultiply(CubieCube.moveCube[j]); // a
			}
		}
	}
	
	// ***********************************Phase 1 and 2 movetable********************************************************

	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// Move table for the four UD-slice edges FR, FL, Bl and BR
	// FRtoBRMove < 11880 in phase 1
	// FRtoBRMove < 24 in phase 2
	// FRtoBRMove = 0 for solved cube
	private void loadFRtoBRMoves(boolean force) {
		if (!force && FRtoBR_Move != null)
			return;
		FRtoBR_Move = new short[N_FRtoBR][N_MOVE];
		
		CubieCube a = new CubieCube();
		for (short i = 0; i < N_FRtoBR; i++) {
			a.setFRtoBR(i);
			for (int j = 0; j < 6; j++) {
				for (int k = 0; k < 3; k++) {
					a.edgeMultiply(CubieCube.moveCube[j]);
					FRtoBR_Move[i][3 * j + k] = a.getFRtoBR();
				}
				a.edgeMultiply(CubieCube.moveCube[j]);
			}
		}
	}
	
	// *******************************************Phase 1 and 2 movetable************************************************

	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// Move table for permutation of six corners. The positions of the DBL and DRB corners are determined by the parity.
	// URFtoDLF < 20160 in phase 1
	// URFtoDLF < 20160 in phase 2
	// URFtoDLF = 0 for solved cube.
	private void loadURFtoDLFMoves(boolean force) {
		if (!force && URFtoDLF_Move != null)
			return;
		URFtoDLF_Move = new short[N_URFtoDLF][N_MOVE];
		
		CubieCube a = new CubieCube();
		for (short i = 0; i < N_URFtoDLF; i++) {
			a.setURFtoDLF(i);
			for (int j = 0; j < 6; j++) {
				for (int k = 0; k < 3; k++) {
					a.cornerMultiply(CubieCube.moveCube[j]);
					URFtoDLF_Move[i][3 * j + k] = a.getURFtoDLF();
				}
				a.cornerMultiply(CubieCube.moveCube[j]);
			}
		}
	}
	
	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// Move table for the permutation of six U-face and D-face edges in phase2. The positions of the DL and DB edges are
	// determined by the parity.
	// URtoDF < 665280 in phase 1
	// URtoDF < 20160 in phase 2
	// URtoDF = 0 for solved cube.
	private void loadURtoDFMoves(boolean force) {
		if (!force && URtoDF_Move != null)
			return;
		URtoDF_Move = new short[N_URtoDF][N_MOVE];
		
		CubieCube a = new CubieCube();
		for (short i = 0; i < N_URtoDF; i++) {
			a.setURtoDF(i);
			for (int j = 0; j < 6; j++) {
				for (int k = 0; k < 3; k++) {
					a.edgeMultiply(CubieCube.moveCube[j]);
					URtoDF_Move[i][3 * j + k] = (short) a.getURtoDF(); // Table values are only valid for phase 2 moves! For phase 1 moves, casting to short is not possible.
				}
				a.edgeMultiply(CubieCube.moveCube[j]);
			}
		}
	}
	
	// **************************helper move tables to compute URtoDF for the beginning of phase2************************

	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// Move table for the three edges UR,UF and UL in phase1.	
	private void loadURtoULMoves(boolean force) {
		if (!force && URtoUL_Move != null)
			return;
		URtoUL_Move = new short[N_URtoUL][N_MOVE];
		
		CubieCube a = new CubieCube();
		for (short i = 0; i < N_URtoUL; i++) {
			a.setURtoUL(i);
			for (int j = 0; j < 6; j++) {
				for (int k = 0; k < 3; k++) {
					a.edgeMultiply(CubieCube.moveCube[j]);
					URtoUL_Move[i][3 * j + k] = a.getURtoUL();
				}
				a.edgeMultiply(CubieCube.moveCube[j]);
			}
		}
	}
	
	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// Move table for the three edges UB,DR and DF in phase1.
	private void loadUBtoDFMoves(boolean force) {
		if (!force && UBtoDF_Move != null)
			return;
		UBtoDF_Move = new short[N_UBtoDF][N_MOVE];
	
		CubieCube a = new CubieCube();
		for (short i = 0; i < N_UBtoDF; i++) {
			a.setUBtoDF(i);
			for (int j = 0; j < 6; j++) {
				for (int k = 0; k < 3; k++) {
					a.edgeMultiply(CubieCube.moveCube[j]);
					UBtoDF_Move[i][3 * j + k] = a.getUBtoDF();
				}
				a.edgeMultiply(CubieCube.moveCube[j]);
			}
		}
	}
	
	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// Table to merge the coordinates of the UR,UF,UL and UB,DR,DF edges at the beginning of phase2
	private void mergeURtoULandUBtoDF(boolean force) {
		if (!force && MergeURtoULandUBtoDF != null)
			return;
		MergeURtoULandUBtoDF = new short[336][336];
		
		/* for i, j < 336 the six edges UR,UF,UL,UB,DR,DF are not in the UD-slice and the index is < 20160 */
		for (short uRtoUL = 0; uRtoUL < 336; uRtoUL++) {
			for (short uBtoDF = 0; uBtoDF < 336; uBtoDF++) {
				MergeURtoULandUBtoDF[uRtoUL][uBtoDF] = (short) CubieCube.getURtoDF(uRtoUL, uBtoDF);
			}
		}
	}
	
	// ****************************************Pruning tables for the search*********************************************

	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// Pruning table for the permutation of the corners and the UD-slice edges in phase2.
	// The pruning table entries give a lower estimation for the number of moves to reach the solved cube.
	private void loadSliceURFtoDLFParityPrun(boolean force) {
		if (!force && Slice_URFtoDLF_Parity_Prun != null)
			return;
		Slice_URFtoDLF_Parity_Prun = new byte[N_SLICE2 * N_URFtoDLF * N_PARITY / 2];

		for (int i = 0; i < N_SLICE2 * N_URFtoDLF * N_PARITY / 2; i++)
			Slice_URFtoDLF_Parity_Prun[i] = -1;
		
		int depth = 0;
		setPruning(Slice_URFtoDLF_Parity_Prun, 0, (byte) 0);
		int done = 1;
		while (done != N_SLICE2 * N_URFtoDLF * N_PARITY) {
			for (int i = 0; i < N_SLICE2 * N_URFtoDLF * N_PARITY; i++) {
				int parity = i % 2;
				int URFtoDLF = (i / 2) / N_SLICE2;
				int slice = (i / 2) % N_SLICE2;
				if (getPruning(Slice_URFtoDLF_Parity_Prun, i) == depth) {
					for (int j = 0; j < 18; j++) {
						switch (j) {
						case 3:
						case 5:
						case 6:
						case 8:
						case 12:
						case 14:
						case 15:
						case 17:
							continue;
						default:
							int newSlice = FRtoBR_Move[slice][j];
							int newURFtoDLF = URFtoDLF_Move[URFtoDLF][j];
							int newParity = parityMove[parity][j];
							if (getPruning(Slice_URFtoDLF_Parity_Prun, (N_SLICE2 * newURFtoDLF + newSlice) * 2 + newParity) == 0x0f) {
								setPruning(Slice_URFtoDLF_Parity_Prun, (N_SLICE2 * newURFtoDLF + newSlice) * 2 + newParity,
										(byte) (depth + 1));
								done++;
							}
						}
					}
				}
			}
			depth++;
		}
	}
	
	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// Pruning table for the permutation of the edges in phase2.
	// The pruning table entries give a lower estimation for the number of moves to reach the solved cube.
	private void loadSliceURtoDFParityPrun(boolean force) {
		if (!force && Slice_URtoDF_Parity_Prun != null)
			return;
		Slice_URtoDF_Parity_Prun = new byte[N_SLICE2 * N_URtoDF * N_PARITY / 2];
		
		for (int i = 0; i < N_SLICE2 * N_URtoDF * N_PARITY / 2; i++)
			Slice_URtoDF_Parity_Prun[i] = -1;
		
		int depth = 0;
		setPruning(Slice_URtoDF_Parity_Prun, 0, (byte) 0);
		int done = 1;
		while (done != N_SLICE2 * N_URtoDF * N_PARITY) {
			for (int i = 0; i < N_SLICE2 * N_URtoDF * N_PARITY; i++) {
				int parity = i % 2;
				int URtoDF = (i / 2) / N_SLICE2;
				int slice = (i / 2) % N_SLICE2;
				if (getPruning(Slice_URtoDF_Parity_Prun, i) == depth) {
					for (int j = 0; j < 18; j++) {
						switch (j) {
						case 3:
						case 5:
						case 6:
						case 8:
						case 12:
						case 14:
						case 15:
						case 17:
							continue;
						default:
							int newSlice = FRtoBR_Move[slice][j];
							int newURtoDF = URtoDF_Move[URtoDF][j];
							int newParity = parityMove[parity][j];
							if (getPruning(Slice_URtoDF_Parity_Prun, (N_SLICE2 * newURtoDF + newSlice) * 2 + newParity) == 0x0f) {
								setPruning(Slice_URtoDF_Parity_Prun, (N_SLICE2 * newURtoDF + newSlice) * 2 + newParity,
										(byte) (depth + 1));
								done++;
							}
						}
					}
				}
			}
			depth++;
		}
	}
	
	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// Pruning table for the twist of the corners and the position (not permutation) of the UD-slice edges in phase1
	// The pruning table entries give a lower estimation for the number of moves to reach the H-subgroup.
	private void loadSliceTwistPrune(boolean force) {
		if (!force && Slice_Twist_Prun != null)
			return;
		Slice_Twist_Prun = new byte[N_SLICE1 * N_TWIST / 2 + 1];
		
		for (int i = 0; i < N_SLICE1 * N_TWIST / 2 + 1; i++)
			Slice_Twist_Prun[i] = -1;
		
		int depth = 0;
		setPruning(Slice_Twist_Prun, 0, (byte) 0);
		int done = 1;
		while (done != N_SLICE1 * N_TWIST) {
			for (int i = 0; i < N_SLICE1 * N_TWIST; i++) {
				int twist = i / N_SLICE1, slice = i % N_SLICE1;
				if (getPruning(Slice_Twist_Prun, i) == depth) {
					for (int j = 0; j < 18; j++) {
						int newSlice = FRtoBR_Move[slice * 24][j] / 24;
						int newTwist = twistMove[twist][j];
						if (getPruning(Slice_Twist_Prun, N_SLICE1 * newTwist + newSlice) == 0x0f) {
							setPruning(Slice_Twist_Prun, N_SLICE1 * newTwist + newSlice, (byte) (depth + 1));
							done++;
						}
					}
				}
			}
			depth++;
		}
	}
	
	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// Pruning table for the flip of the edges and the position (not permutation) of the UD-slice edges in phase1
	// The pruning table entries give a lower estimation for the number of moves to reach the H-subgroup.
	private void loadSliceFlipPrune(boolean force) {
		if (!force && Slice_Flip_Prun != null)
			return;
		Slice_Flip_Prun = new byte[N_SLICE1 * N_FLIP / 2];
		
		for (int i = 0; i < N_SLICE1 * N_FLIP / 2; i++)
			Slice_Flip_Prun[i] = -1;
		
		int depth = 0;
		setPruning(Slice_Flip_Prun, 0, (byte) 0);
		int done = 1;
		while (done != N_SLICE1 * N_FLIP) {
			for (int i = 0; i < N_SLICE1 * N_FLIP; i++) {
				int flip = i / N_SLICE1, slice = i % N_SLICE1;
				if (getPruning(Slice_Flip_Prun, i) == depth) {
					for (int j = 0; j < 18; j++) {
						int newSlice = FRtoBR_Move[slice * 24][j] / 24;
						int newFlip = flipMove[flip][j];
						if (getPruning(Slice_Flip_Prun, N_SLICE1 * newFlip + newSlice) == 0x0f) {
							setPruning(Slice_Flip_Prun, N_SLICE1 * newFlip + newSlice, (byte) (depth + 1));
							done++;
						}
					}
				}
			}
			depth++;
		}
	}
	

	
	
//	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//	// Save prune tables to a file.
//	public void saveTablesToFile() {
//		
//		Object [] tables = new Object[12];
//		tables[0] = twistMove;
//		tables[1] = flipMove;
//		tables[2] = FRtoBR_Move;
//		tables[3] = URFtoDLF_Move;
//		tables[4] = URtoDF_Move;
//		tables[5] = URtoUL_Move;
//		tables[6] = UBtoDF_Move;
//		tables[7] = MergeURtoULandUBtoDF;
//		tables[8] = Slice_URFtoDLF_Parity_Prun;
//		tables[9] = Slice_URtoDF_Parity_Prun;
//		tables[10] = Slice_Twist_Prun;
//		tables[11] = Slice_Flip_Prun;
//		
//		try {
//			File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
//			String filename = "cube.tbls";
//			File file = new File(path, filename);
//			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file));
//			out.writeObject(tables);
//			out.flush();
//			out.close();
//			Log.i(Constants.TAG, "SUCCESS writing prune tables to external storage:" + filename);
//		}
//		catch (Exception e) {
//			System.out.print(e);
//			Log.e(Constants.TAG, "Fail writing prune tables to external storage: " + e);
//		}
//	}
//	
//	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//	// Recall prune tables from a file
//	public boolean recallTablesFromFile() {
//		
//		try {
//			File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
//			String filename = "cube.tbls";
//			File file = new File(path, filename);
//	        ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
//	        Object [] tables = (RubikFace[])in.readObject();
//	        in.close();
//	        
//			twistMove = tables[0];
//			flipMove = tables[1];
//			FRtoBR_Move = tables[2];
//			URFtoDLF_Move = tables[3];
//			URtoDF_Move = tables[4];
//			URtoUL_Move = tables[5];
//			UBtoDF_Move = tables[6];
//			MergeURtoULandUBtoDF = tables[7];
//			Slice_URFtoDLF_Parity_Prun = tables[8];
//			Slice_URtoDF_Parity_Prun = tables[9];
//			Slice_Twist_Prun = tables[10];
//			Slice_Flip_Prun = tables[11];
//	        
//	        
//			Log.i(Constants.TAG, "SUCCESS reading cube state to external storage:" + filename);
//		}
//		catch (Exception e) {
//			System.out.print(e);
//			Log.e(Constants.TAG, "Fail reading cube to external storage: " + e);
//		}
//	}
}
