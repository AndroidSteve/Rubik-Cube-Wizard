package org.kociemba.twophase;

import java.util.Random;

public class Tools {

	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// Check if the cube string s represents a solvable cube.
	// 0: Cube is solvable
	// -1: There is not exactly one facelet of each colour
	// -2: Not all 12 edges exist exactly once
	// -3: Flip error: One edge has to be flipped
	// -4: Not all corners exist exactly once
	// -5: Twist error: One corner has to be twisted
	// -6: Parity error: Two corners or two edges have to be exchanged
	// 
	/**
	 * Check if the cube definition string s represents a solvable cube.
	 * 
	 * @param s is the cube definition string , see {@link Facelet}
	 * @return 0: Cube is solvable<br>
	 *         -1: There is not exactly one facelet of each colour<br>
	 *         -2: Not all 12 edges exist exactly once<br>
	 *         -3: Flip error: One edge has to be flipped<br>
	 *         -4: Not all 8 corners exist exactly once<br>
	 *         -5: Twist error: One corner has to be twisted<br>
	 *         -6: Parity error: Two corners or two edges have to be exchanged
	 */
	public static int verify(String s) {
		int[] count = new int[6];
		try {
			for (int i = 0; i < 54; i++)
				count[Color.valueOf(s.substring(i, i + 1)).ordinal()]++;
		} catch (Exception e) {
			return -1;
		}

		for (int i = 0; i < 6; i++)
			if (count[i] != 9)
				return -1;

		FaceCube fc = new FaceCube(s);
		CubieCube cc = fc.toCubieCube();

		return cc.verify();
	}

	/**
	 * Generates a random cube.
	 * @return A random cube in the string representation. Each cube of the cube space has the same probability.
	 */
	public static String randomCube() {
		CubieCube cc = new CubieCube();
		Random gen = new Random();
		cc.setFlip((short) gen.nextInt(CoordCube.N_FLIP));
		cc.setTwist((short) gen.nextInt(CoordCube.N_TWIST));
		do {
			cc.setURFtoDLB(gen.nextInt(CoordCube.N_URFtoDLB));
			cc.setURtoBR(gen.nextInt(CoordCube.N_URtoBR));
		} while ((cc.edgeParity() ^ cc.cornerParity()) != 0);
		FaceCube fc = cc.toFaceCube();
		return fc.to_String();
	}
}
