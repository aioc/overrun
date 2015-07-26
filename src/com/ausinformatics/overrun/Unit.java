package com.ausinformatics.overrun;

import com.ausinformatics.phais.utils.Position;

public class Unit {
	public int strength;
	public final int myId;
	public final int ownerId;
	public Position p;
	
	public Unit(int strength, int myId, int ownerId, Position p) {
		this.strength = strength;
		this.myId = myId;
		this.ownerId = ownerId;
		this.p = p;
	}
	
	public boolean isAlive() {
		return strength > 0;
	}
}
