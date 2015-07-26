package com.ausinformatics.overrun;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;

import javafx.geometry.Pos;

import com.ausinformatics.overrun.visualisation.VisualGameState;
import com.ausinformatics.phais.core.visualisation.EventBasedFrameVisualiser;
import com.ausinformatics.phais.utils.Position;

public class GameState {

	private int numPlayers;
	private int boardSize;
	private TerrainMap map;
	private MoveReporter reporter;
	private EventBasedFrameVisualiser<VisualGameState> vis;

	private Unit[][] unitsOnBoard;
	private int[] money;
	private int[] curUnitId;
	private ArrayList<ArrayList<Unit>> allUnits;

	public GameState(int numPlayers, int boardSize, TerrainMap map, MoveReporter reporter) {
		this.numPlayers = numPlayers;
		this.boardSize = boardSize;
		this.map = map;
		this.reporter = reporter;
		unitsOnBoard = new Unit[boardSize][boardSize];
		money = new int[numPlayers];
		curUnitId = new int[numPlayers];
		allUnits = new ArrayList<ArrayList<Unit>>();
		for (int i = 0; i < numPlayers; i++) {
			allUnits.add(new ArrayList<Unit>());
			createUnit(i, 1);
		}
	}

	public void setUpForVisualisation(EventBasedFrameVisualiser<VisualGameState> vis) {
		this.vis = vis;
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
				// TODO: Decide if we should kick them.
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
				// TODO: Give them an error.
			}
			if (map.getTerrain(newP.c, newP.r) == TerrainMap.WALL) {
				// TODO: Give them an error.
				newP = unit.p;
				u = UnitMove.DEFAULT_MOVE;
			}
			// The interesting bit... fights!
			if (unitsOnBoard[newP.r][newP.c] != null) {
				Unit otherU = unitsOnBoard[newP.r][newP.c];
				if (otherU.ownerId == unit.ownerId) {
					newP = unit.p;
					u = UnitMove.DEFAULT_MOVE;
				} else {
					// Fight!
					int lowestStr = Math.min(unit.stength, otherU.stength);
					// TODO: Tell people that the strength changed.
					unit.stength -= lowestStr;
					otherU.stength -= lowestStr;
					if (otherU.stength <= 0) {
						unitsOnBoard[newP.r][newP.c] = null;
					}
				}
			}
			if (unit.stength > 0) {
				unitsOnBoard[newP.r][newP.c] = unit;
			}
			unit.p = newP;
			// TODO: Tell people that people moved.
			if (u.move == UnitMove.EXTRACT) {
				if (map.getTerrain(newP.c, newP.r) > 0) {
					money[id]++;
					int res = map.getTerrain(newP.c, newP.r) - 1;
					map.setTerrain(newP.c, newP.r, res);
					// TODO: Update people that the square changed.
				} else {
					// TODO: Error them for extracting empty square.
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
						int lowestStr = Math.min(stre, otherU.stength);
						// TODO: Tell people that the strength changed.
						stre -= lowestStr;
						otherU.stength -= lowestStr;
						if (otherU.stength <= 0) {
							unitsOnBoard[po.y][po.x] = null;
						}
					} else {
						// TODO: Error them out
						succeeded = false;
					}
				}
				if (succeeded) {
					if (stre > 0) {
						createUnit(id, stre);
					}
					money[id] -= move.buildCost;
					// TODO: Update people about money changing.
				}
			} else {
				// TODO: Error them for spending too much.
			}
		}
		// TODO: Send end of turn...
	}

	public int getScore(int id) {
		return 0;
	}

	public void endGame() {
		// TODO: Report that the game is over to the visualiser.
	}
	
	private int getStr(int cost) {
		return cost;
	}

	private void killUnit(Unit u) {
		u.stength = 0;
		unitsOnBoard[u.p.r][u.p.c] = null;
		// TODO: Tell people these died.
	}

	private void createUnit(int playerId, int strength) {
		Point po = map.getBase(playerId);
		Position p = new Position(po.y, po.x);
		Unit u = new Unit(strength, curUnitId[playerId]++, playerId, p);
		unitsOnBoard[p.r][p.c] = u;
		allUnits.get(playerId).add(u);
		// TODO: Tell people these were created.
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
