package com.ausinformatics.overrun.core;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;

import com.ausinformatics.overrun.core.reporters.CopyingReporter;
import com.ausinformatics.overrun.core.reporters.Reporter;
import com.ausinformatics.overrun.core.reporters.EventReporter;
import com.ausinformatics.phais.common.events.EventReceiver;
import com.ausinformatics.phais.utils.Position;

public class GameState {

	private int boardSize;
	private TerrainMap map;
	private Reporter reporter;

	private Unit[][] unitsOnBoard;
	private int[] money;
	private int[] curUnitId;
	private ArrayList<ArrayList<Unit>> allUnits;

	public GameState(int numPlayers, int boardSize, TerrainMap map, Reporter reporter, EventReceiver er) {
		this.boardSize = boardSize;
		this.map = map;
		this.reporter = new CopyingReporter(reporter, new EventReporter(er));
        allUnits = new ArrayList<ArrayList<Unit>>();
        unitsOnBoard = new Unit[boardSize][boardSize];
        money = new int[numPlayers];
        curUnitId = new int[numPlayers];
        
        for (int i = 0; i < numPlayers; i++) {
            allUnits.add(new ArrayList<Unit>());
            createUnit(i, 1);
        }
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                reporter.squareUpdated(new Position(i, j), map.getTerrain(j, i), map.getTerrain(j, i));
            }
        }
		
	}

	// This should be able to be called on already dead players.
	public void killPlayer(int id) {
		for (Unit u : allUnits.get(id)) {
			killUnit(u);
		}
	}

	public boolean isPlayerDead(int id) {
		collectDeadUnits(id);
		return allUnits.get(id).size() == 0;
	}

	public void makeMove(int id, PlayerMove move) {
		// Move first, build unit later.
		// Build a lookup from id to unit.
		HashMap<Integer, Unit> playersUnits = new HashMap<Integer, Unit>();
		for (Unit u : allUnits.get(id)) {
			if (u.isAlive()) {
				playersUnits.put(u.myId, u);
			}
		}
		for (UnitMove u : move.unitMoves) {
			if (!playersUnits.containsKey(u.id)) {
				reporter.sendError(id, "You gave an invalid id (either dead or non-existant): " + u.id);
				continue;
			}
			Unit unit = playersUnits.get(u.id);
			// Remove from board.
			unitsOnBoard[unit.p.r][unit.p.c] = null;
			// Find new position.
			Position newP = u.applyToPosition(unit.p);
			// Check if it is in bounds.
			if (newP.r < 0 || newP.c < 0 || newP.r >= boardSize || newP.c >= boardSize) {
				newP = unit.p;
				u = UnitMove.DEFAULT_MOVE;
				reporter.sendError(id, "You moved a unit out of bounds. id: " + u.id);
			}
			if (map.getTerrain(newP.c, newP.r) == TerrainMap.WALL) {
				newP = unit.p;
				u = UnitMove.DEFAULT_MOVE;
				reporter.sendError(id, "You moved a unit into a wall. id: " + u.id);
			}
			// The interesting bit... fights!
			if (unitsOnBoard[newP.r][newP.c] != null) {
				Unit otherU = unitsOnBoard[newP.r][newP.c];
				if (otherU.ownerId == unit.ownerId) {
					newP = unit.p;
					u = UnitMove.DEFAULT_MOVE;
				} else {
					// Fight!
					int lowestStr = Math.min(unit.strength, otherU.strength);
					Unit copyU = unit.clone();
					unit.strength -= lowestStr;
					reporter.unitUpdated(copyU, unit);
					copyU = otherU.clone();
					otherU.strength -= lowestStr;
					reporter.unitUpdated(copyU, otherU);
					if (otherU.strength <= 0) {
						unitsOnBoard[newP.r][newP.c] = null;
					}
				}
			}
			if (unit.strength > 0) {
				unitsOnBoard[newP.r][newP.c] = unit;
			}
			Unit copyU = unit.clone();
			unit.p = newP;
			reporter.unitUpdated(copyU, unit);
			if (u.move == UnitMove.EXTRACT) {
				if (map.getTerrain(newP.c, newP.r) > 0) {
					money[id]++;
					int res = map.getTerrain(newP.c, newP.r) - 1;
					map.setTerrain(newP.c, newP.r, res);
					reporter.personMoneyChange(id, 1);
					reporter.squareUpdated(newP, res + 1, res);
				} else {
					reporter.sendError(id, "You tried to extract from an empty square. id: " + u.id);
				}
			}
		}
		if (move.buildCost > 0) {
			if (move.buildCost <= money[id]) {
				Point po = map.getBase(id);
				int stre = getStr(move.buildCost);
				boolean succeeded = true;
				if (unitsOnBoard[po.y][po.x] != null) {
					Unit otherU = unitsOnBoard[po.y][po.x];
					if (otherU.ownerId != id) {
						// Fight!
						int lowestStr = Math.min(stre, otherU.strength);
						Unit copyU = otherU.clone();
						otherU.strength -= lowestStr;
						reporter.unitUpdated(copyU, otherU);
						stre -= lowestStr;
						if (otherU.strength <= 0) {
							unitsOnBoard[po.y][po.x] = null;
						}
					} else {
						reporter.sendError(id, "You tried to create a unit when your home square was occupied.");
						succeeded = false;
					}
				}
				if (succeeded) {
					if (stre > 0) {
						createUnit(id, stre);
					}
					money[id] -= move.buildCost;
					reporter.personMoneyChange(id, -move.buildCost);
				}
			} else {
				reporter.sendError(id, "You tried to spend to much.");
			}
		}
		reporter.endTurn();
	}

	public int getScore(int id) {
		int total = 0;
		for (Unit u : allUnits.get(id)) {
			total += u.strength;
		}
		return total;
	}

	public void endGame() {
		reporter.endTurn();
	}

	private int getStr(int cost) {
		return cost;
	}

	private void killUnit(Unit u) {	
		Unit copyU = u.clone();
		u.strength = 0;
		unitsOnBoard[u.p.r][u.p.c] = null;
		reporter.unitUpdated(copyU, u);
	}

	private void createUnit(int playerId, int strength) {
		Point po = map.getBase(playerId);
		Position p = new Position(po.y, po.x);
		Unit u = new Unit(strength, curUnitId[playerId]++, playerId, p);
		unitsOnBoard[p.r][p.c] = u;
		allUnits.get(playerId).add(u);
		reporter.unitCreated(u);
	}

	private void collectDeadUnits(int id) {
		ArrayList<Unit> aliveUnits = new ArrayList<Unit>();
		for (Unit u : allUnits.get(id)) {
			if (u.isAlive()) {
				aliveUnits.add(u);
			}
		}
		allUnits.set(id, aliveUnits);
	}

}
