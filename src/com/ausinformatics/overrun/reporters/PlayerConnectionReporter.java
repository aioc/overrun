package com.ausinformatics.overrun.reporters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.ausinformatics.overrun.Unit;
import com.ausinformatics.phais.core.server.ClientConnection;
import com.ausinformatics.phais.utils.Pair;
import com.ausinformatics.phais.utils.Position;

public class PlayerConnectionReporter implements Reporter {

	private Map<Pair<Integer, Integer>, Integer> squareUpdates;
	private List<String> errors;
	private UnitUpdateHandler updateHandler;

	public PlayerConnectionReporter() {
		squareUpdates = new HashMap<Pair<Integer, Integer>, Integer>();
		errors = new ArrayList<String>();
		updateHandler = new UnitUpdateHandler();
	}

	@Override
	public void sendError(int id, String error) {
		errors.add(error);
	}

	@Override
	public void squareUpdated(Position p, int oldVal, int newVal) {
		squareUpdates.put(new Pair<Integer, Integer>(p.r, p.c), newVal);
	}

	@Override
	public void personMoneyChange(int id, int amount) {
		// Do nothing.
	}

	@Override
	public void unitCreated(Unit u) {
		Unit blankU = new Unit(0, u.myId, u.ownerId, new Position(-1, -1));
		updateHandler.sendUpdate(blankU, u);
	}

	@Override
	public void unitUpdated(Unit oldU, Unit newU) {
		updateHandler.sendUpdate(oldU, newU);
	}

	@Override
	public void endTurn() {
		// Do nothing.
	}

	public void sendToPlayer(ClientConnection c) {
		for (String s : errors) {
			c.sendInfo("ERROR " + s);
		}
		String cellString = "CELL " + squareUpdates.size();
		for (Entry<Pair<Integer, Integer>, Integer> sq : squareUpdates.entrySet()) {
			cellString += " " + sq.getKey().first + " " + sq.getKey().second + " " + sq.getValue();
		}
		c.sendInfo(cellString);
		List<Pair<Unit, Unit>> updates = updateHandler.getAllUpdates();
		String unitString = "LOCATION " + updates.size();
		for (Pair<Unit, Unit> up : updates) {
			Unit u = up.second;
			unitString += " " + u;
		}
		c.sendInfo(unitString);
		errors.clear();
		squareUpdates.clear();
		updateHandler.clear();
	}

}
