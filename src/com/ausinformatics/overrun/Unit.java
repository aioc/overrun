package com.ausinformatics.overrun;

import com.ausinformatics.phais.utils.Position;

public class Unit {
	public int stength;
	public final int myId;
	public final int ownerId;
	public Position p;
	
	public Unit(int strength, int myId, int ownerId, Position p) {
		this.stength = strength;
		this.myId = myId;
		this.ownerId = ownerId;
		this.p = p;
	}
	
	public void changePosition(int dir) {
		p = p.move(dir);
	}
	
	public boolean isAlive() {
		return stength > 0;
	}
}
