package com.ausinformatics.overrun;

public class UnitMove {

	public static final int dx[] = {0, 1, 0, -1, 0, 0};
	public static final int dy[] = {-1, 0, 1, 0, 0, 0};
	public static final int EXTRACT = 4;
	public static final int NO_MOVE = 5;
	
	public final int id;
	public final int move;
	
	public UnitMove(int id, int move) {
		this.id = id;
		this.move = move;
	}
	
}
