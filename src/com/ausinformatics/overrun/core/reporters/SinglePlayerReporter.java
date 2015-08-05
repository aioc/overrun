package com.ausinformatics.overrun.core.reporters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.ausinformatics.client.GameClient.ProtocolRequest;
import com.ausinformatics.client.GameClient.ProtocolRequest.UpdateStateParameters;
import com.ausinformatics.overrun.core.Unit;
import com.ausinformatics.phais.server.server.ClientConnection;
import com.ausinformatics.phais.utils.Pair;
import com.ausinformatics.phais.utils.Position;

/**
 * Reports game state changes to an individual player.
 * 
 * Must be created afresh for each player, every new game.
 */
public class SinglePlayerReporter implements Reporter {

	private Map<Pair<Integer, Integer>, Integer> squareUpdates;
	private List<String> errors;
	private UnitUpdateHandler updateHandler;
	private int[] money;

	public SinglePlayerReporter(int numPlayers) {
		squareUpdates = new HashMap<Pair<Integer, Integer>, Integer>();
		errors = new ArrayList<String>();
		updateHandler = new UnitUpdateHandler();
		money = new int[numPlayers];
	}

	private void clear() {
		errors.clear();
		squareUpdates.clear();
		updateHandler.clear();
		// Do not clear money, it's not a delta.
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
		money[id] += amount;
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

	public void sendStateUpdateAndClear(ClientConnection connection) {
		// First, send all the error messages.
		for (String s : errors) {
			connection.sendMessage(ProtocolRequest.newBuilder()
					.setCommand(ProtocolRequest.Command.ERROR)
					.setErrorDetail(s)
					.build());
		}

		// Then send the updated state.
		UpdateStateParameters.Builder builder = UpdateStateParameters.newBuilder();

		for (int m : money) {
			builder.addMoneyState(m);
		}

		for (Entry<Pair<Integer, Integer>, Integer> sq : squareUpdates.entrySet()) {
			builder.addCellChange(UpdateStateParameters.CellUpdate.newBuilder()
					.setX(sq.getKey().second)
					.setY(sq.getKey().first)
					.setValue(sq.getValue()));
		}

		for (Pair<Unit, Unit> up : updateHandler.getAllUpdates()) {
			Unit u = up.second;
			builder.addUnitChange(UpdateStateParameters.UnitUpdate.newBuilder()
					.setX(u.p.c)
					.setY(u.p.r)
					.setUnitId(u.myId)
					.setPlayerId(u.ownerId)
					.setLevel(u.strength));
		}

		connection.sendMessage(ProtocolRequest.newBuilder()
				.setCommand(ProtocolRequest.Command.UPDATE_STATE)
				.setUpdateState(builder)
				.build());

		this.clear();
	}
}
