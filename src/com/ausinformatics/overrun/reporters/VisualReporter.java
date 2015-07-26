package com.ausinformatics.overrun.reporters;

import java.util.ArrayList;
import java.util.List;

import com.ausinformatics.overrun.Unit;
import com.ausinformatics.overrun.visualisation.MoneyDeltaEvent;
import com.ausinformatics.overrun.visualisation.UnitCreatedEvent;
import com.ausinformatics.overrun.visualisation.VisualGameState;
import com.ausinformatics.phais.core.visualisation.EndTurnEvent;
import com.ausinformatics.phais.core.visualisation.EventBasedFrameVisualiser;
import com.ausinformatics.phais.core.visualisation.VisualGameEvent;
import com.ausinformatics.phais.utils.Position;

// Idea of this class is to only hold stuff for one turn, as it will then disappear.
public class VisualReporter implements Reporter {

	private EventBasedFrameVisualiser<VisualGameState> vis;
	private List<VisualGameEvent> pendingEvents;
	private List<Position> minedSquares;
	private int curMoneyChange;
	private int playerId;

	public VisualReporter(EventBasedFrameVisualiser<VisualGameState> vis) {
		this.vis = vis;
		reset();
	}

	@Override
	public void sendError(int id, String error) {
		// Don't do anything.
	}

	@Override
	public void squareUpdated(Position p, int newVal) {
		minedSquares.add(p);
	}

	@Override
	public void personMoneyChange(int id, int amount) {
		playerId = id;
		curMoneyChange += amount;
	}

	@Override
	public void unitCreated(Unit u) {
		pendingEvents.add(new UnitCreatedEvent(u.ownerId, u.myId, u.p, u.strength));
	}

	@Override
	public void unitUpdated(Unit oldU, Unit newU) {
		
	}

	@Override
	public void endTurn() {
		if (curMoneyChange != 0) {
			pendingEvents.add(new MoneyDeltaEvent(playerId, curMoneyChange, minedSquares));
		}
		pendingEvents.add(new EndTurnEvent());
		vis.giveEvents(pendingEvents);
	}

	private void reset() {
		pendingEvents = new ArrayList<VisualGameEvent>();
		minedSquares = new ArrayList<Position>();
		curMoneyChange = 0;
		
	}
	
}
