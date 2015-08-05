package com.ausinformatics.overrun.core.reporters;

import com.ausinformatics.overrun.core.Unit;
import com.ausinformatics.phais.server.server.ClientConnection;
import com.ausinformatics.phais.utils.Position;

public class MultiplePlayerReporter implements Reporter {

	private SinglePlayerReporter[] reporters;

	public MultiplePlayerReporter(int numPlayers) {
		this.reporters = new SinglePlayerReporter[numPlayers];
		for (int i = 0; i < numPlayers; i++) {
			reporters[i] = new SinglePlayerReporter(numPlayers);
		}
	}

	public void sendForPlayer(int id, ClientConnection connection) {
		reporters[id].sendStateUpdateAndClear(connection);
	}

	@Override
	public void sendError(int id, String error) {
		reporters[id].sendError(id, error);
	}

	@Override
	public void squareUpdated(Position p, int oldVal, int newVal) {
		for (SinglePlayerReporter reporter : reporters) {
			reporter.squareUpdated(p, oldVal, newVal);
		}
	}

	@Override
	public void personMoneyChange(int id, int amount) {
		for (SinglePlayerReporter reporter : reporters) {
			reporter.personMoneyChange(id, amount);
		}
	}

	@Override
	public void unitCreated(Unit u) {
		for (SinglePlayerReporter reporter : reporters) {
			reporter.unitCreated(u);
		}
	}

	@Override
	public void unitUpdated(Unit oldU, Unit newU) {
		for (SinglePlayerReporter reporter : reporters) {
			reporter.unitUpdated(oldU, newU);
		}
	}

	@Override
	public void endTurn() {
		// Do nothing.
	}

}
