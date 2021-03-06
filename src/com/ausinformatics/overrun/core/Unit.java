package com.ausinformatics.overrun.core;

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
	
	public Unit clone() {
		return new Unit(strength, myId, ownerId, p.clone());
	}
	
	public String toString() {
		return (ownerId + 1) + " " + myId + " " + p.r + " " + p.c + " " + strength;
	}
}
