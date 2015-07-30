package com.ausinformatics.overrun.core;

public class TerrainMapFactory {

	public int baseOffset = 6;
	public int amoMin = 20;
	public int amoPerMin = 100;
	public int sharpness = 4;
	public double wallAttract = 1.0;
	public double baseAttract = -50.0;
	public double rockCutOff = 0.58;
	public double minAttract = 4.0;
	public double otherWeight = 100.0;

	public TerrainMap fromDefaultParams(int numPlayers, int boardSize) {
		return new TerrainMap(0, numPlayers, boardSize, baseOffset, amoMin, amoPerMin, sharpness, wallAttract,
				baseAttract, rockCutOff, minAttract, otherWeight);
	}

}
