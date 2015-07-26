package com.ausinformatics.overrun;

import com.ausinformatics.phais.utils.Position;

public class UnitMove {

	public static final int dr[] = {-1, 0, 1, 0, 0, 0};
	public static final int dc[] = {0, 1, 0, -1, 0, 0};
	public static final int EXTRACT = 4;
	public static final int NO_MOVE = 5;
	
	public final int id;
	public final int move;
	
	public static final UnitMove DEFAULT_MOVE = new UnitMove(-1, NO_MOVE);
	
	public UnitMove(int id, int move) {
		this.id = id;
		this.move = move;
	}
	
	public Position applyToPosition(Position p) {
		return new Position(p.r + dr[move], p.c + dc[move]);
	}
	
}
