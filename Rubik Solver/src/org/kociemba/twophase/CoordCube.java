package org.kociemba.twophase;

import org.kociemba.twophase.CubieCube;

//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//Representation of the cube on the coordinate level
public class CoordCube {
	static final short N_TWIST = 2187;// 3^7 possible corner orientations
	static final short N_FLIP = 2048;// 2^11 possible edge flips
	static final short N_SLICE1 = 495;// 12 choose 4 possible positions of FR,FL,BL,BR edges
	static final short N_SLICE2 = 24;// 4! permutations of FR,FL,BL,BR edges in phase2
	static final short N_PARITY = 2; // 2 possible corner parities
	static final short N_URFtoDLF = 20160;// 8!/(8-6)! permutation of URF,UFL,ULB,UBR,DFR,DLF corners
	static final short N_FRtoBR = 11880; // 12!/(12-4)! permutation of FR,FL,BL,BR edges
	static final short N_URtoUL = 1320; // 12!/(12-3)! permutation of UR,UF,UL edges
	static final short N_UBtoDF = 1320; // 12!/(12-3)! permutation of UB,DR,DF edges
	static final short N_URtoDF = 20160; // 8!/(8-6)! permutation of UR,UF,UL,UB,DR,DF edges in phase2
	
	static final int N_URFtoDLB = 40320;// 8! permutations of the corners
	static final int N_URtoBR = 479001600;// 8! permutations of the corners
	
	static final short N_MOVE = 18;

	// All coordinates are 0 for a solved cube except for UBtoDF, which is 114
	short twist;
	short flip;
	short parity;
	short FRtoBR;
	short URFtoDLF;
	short URtoUL;
	short UBtoDF;
	int URtoDF;
	
	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// Generate a CoordCube from a CubieCube
	CoordCube(CubieCube c) {
		twist = c.getTwist();
		flip = c.getFlip();
		parity = c.cornerParity();
		FRtoBR = c.getFRtoBR();
		URFtoDLF = c.getURFtoDLF();
		URtoUL = c.getURtoUL();
		UBtoDF = c.getUBtoDF();
		URtoDF = c.getURtoDF();// only needed in phase2
	}

	// A move on the coordinate level
	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	void move(int m) {
		twist = twistMove[twist][m];
		flip = flipMove[flip][m];
		parity = parityMove[parity][m];
		FRtoBR = FRtoBR_Move[FRtoBR][m];
		URFtoDLF = URFtoDLF_Move[URFtoDLF][m];
		URtoUL = URtoUL_Move[URtoUL][m];
		UBtoDF = UBtoDF_Move[UBtoDF][m];
		if (URtoUL < 336 && UBtoDF < 336)// updated only if UR,UF,UL,UB,DR,DF
			// are not in UD-slice
			URtoDF = MergeURtoULandUBtoDF[URtoUL][UBtoDF];
	}
	
	/* all empty pruning tables; must be loaded with {@link PruneTableLoader} first before using the solver */
	static short[][] twistMove;
	static short[][] flipMove;
	static short[][] FRtoBR_Move;
	static short[][] URFtoDLF_Move;
	static short[][] URtoDF_Move;
	static short[][] URtoUL_Move;
	static short[][] UBtoDF_Move;
	static short[][] MergeURtoULandUBtoDF;
	static byte[] Slice_URFtoDLF_Parity_Prun;
	static byte[] Slice_URtoDF_Parity_Prun;
	static byte[] Slice_Twist_Prun;
	static byte[] Slice_Flip_Prun;
	
	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// Parity of the corner permutation. This is the same as the parity for the edge permutation of a valid cube.
	// parity has values 0 and 1
	static short[][] parityMove = { { 1, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1 },
		{ 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0 } };

	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// Set pruning value in table. Two values are stored in one byte.
	static void setPruning(byte[] table, int index, byte value) {
		if ((index & 1) == 0)
			table[index / 2] &= 0xf0 | value;
		else
			table[index / 2] &= 0x0f | (value << 4);
	}

	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// Extract pruning value
	static byte getPruning(byte[] table, int index) {
		if ((index & 1) == 0)
			return (byte) (table[index / 2] & 0x0f);
		else
			return (byte) ((table[index / 2] & 0xf0) >>> 4);
	}
}
