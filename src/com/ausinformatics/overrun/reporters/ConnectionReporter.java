package com.ausinformatics.overrun.reporters;

import com.ausinformatics.overrun.Unit;
import com.ausinformatics.phais.core.server.ClientConnection;
import com.ausinformatics.phais.utils.Position;

public class ConnectionReporter implements Reporter {

	public ConnectionReporter(int numPlayers) {
	}

	
	public void sendForPlayer(int id, ClientConnection connection) {
		
	}
	
	@Override
	public void sendError(int id, String error) {
		
	}
	
	@Override
	public void squareUpdated(Position p, int newVal) {
		// TODO Auto-generated method stub

	}

	@Override
	public void personMoneyChange(int id, int amount) {
		// TODO Auto-generated method stub

	}

	@Override
	public void unitCreated(Unit u) {
		// TODO Auto-generated method stub

	}

	@Override
	public void unitUpdated(Unit oldU, Unit newU) {
		// TODO Auto-generated method stub

	}
	
	@Override
	public void endTurn() {
		
	}

}
