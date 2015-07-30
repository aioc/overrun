package com.ausinformatics.overrun.core.reporters;

import com.ausinformatics.overrun.core.Unit;
import com.ausinformatics.phais.server.server.ClientConnection;
import com.ausinformatics.phais.utils.Position;

public class ConnectionReporter implements Reporter {

	private int numPlayers;
	private PlayerConnectionReporter[] reporters;
	private int[] money;

	public ConnectionReporter(int numPlayers) {
		this.numPlayers = numPlayers;
		reporters = new PlayerConnectionReporter[numPlayers];
		money = new int[numPlayers];
		for (int i = 0; i < numPlayers; i++) {
			reporters[i] = new PlayerConnectionReporter();
		}
	}

	public void sendForPlayer(int id, ClientConnection connection) {
		String moneyString = "MINERALS";
		for (int i = 0; i < numPlayers; i++) {
			moneyString += " " + money[i];
		}
		connection.sendInfo(moneyString);
		reporters[id].sendToPlayer(connection);
	}

	@Override
	public void sendError(int id, String error) {
		reporters[id].sendError(id, error);
	}

	@Override
	public void squareUpdated(Position p, int oldVal, int newVal) {
		for (int i = 0; i < numPlayers; i++) {
			reporters[i].squareUpdated(p, oldVal, newVal);
		}
	}

	@Override
	public void personMoneyChange(int id, int amount) {
		money[id] += amount;
	}

	@Override
	public void unitCreated(Unit u) {
		for (int i = 0; i < numPlayers; i++) {
			reporters[i].unitCreated(u);
		}
	}

	@Override
	public void unitUpdated(Unit oldU, Unit newU) {
		for (int i = 0; i < numPlayers; i++) {
			reporters[i].unitUpdated(oldU, newU);
		}
	}

	@Override
	public void endTurn() {
		// Do nothing.
	}

}
