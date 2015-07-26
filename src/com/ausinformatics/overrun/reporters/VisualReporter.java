package com.ausinformatics.overrun.reporters;

import java.util.ArrayList;
import java.util.List;

import com.ausinformatics.overrun.Unit;
import com.ausinformatics.overrun.visualisation.MoneyDeltaEvent;
import com.ausinformatics.overrun.visualisation.UnitCreatedEvent;
import com.ausinformatics.overrun.visualisation.UnitUpdatedEvent;
import com.ausinformatics.overrun.visualisation.VisualGameState;
import com.ausinformatics.phais.core.visualisation.EndTurnEvent;
import com.ausinformatics.phais.core.visualisation.EventBasedFrameVisualiser;
import com.ausinformatics.phais.core.visualisation.VisualGameEvent;
import com.ausinformatics.phais.utils.Pair;
import com.ausinformatics.phais.utils.Position;

// Idea of this class is to only hold stuff for one turn, as it will then disappear.
public class VisualReporter implements Reporter {

	private EventBasedFrameVisualiser<VisualGameState> vis;
	private List<VisualGameEvent> pendingEvents;
	private List<Position> minedSquares;
	private int curMoneyChange;
	private int playerId;
	private UnitUpdateHandler updatesHandler;

	public VisualReporter(EventBasedFrameVisualiser<VisualGameState> vis) {
		this.vis = vis;
		updatesHandler = new UnitUpdateHandler();
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
		updatesHandler.sendUpdate(oldU, newU);
	}

	@Override
	public void endTurn() {
		List<Pair<Unit, Unit>> updates = updatesHandler.getAllUpdates();
		for (Pair<Unit, Unit> up : updates) {
			pendingEvents.add(new UnitUpdatedEvent(up.first.ownerId, up.first.myId, up.second.strength,
					up.first.strength, up.first.p, getDirMoved(up.first.p, up.second.p)));
		}
		if (curMoneyChange != 0) {
			pendingEvents.add(new MoneyDeltaEvent(playerId, curMoneyChange, minedSquares));
		}
		pendingEvents.add(new EndTurnEvent());
		vis.giveEvents(pendingEvents);
	}

	private int getDirMoved(Position oldP, Position newP) {
		for (int i = 0; i < 4; i++) {
			if (oldP.move(i).equals(newP)) {
				return i;
			}
		}
		return 4;
	}

	private void reset() {
		pendingEvents = new ArrayList<VisualGameEvent>();
		minedSquares = new ArrayList<Position>();
		curMoneyChange = 0;
		updatesHandler.clear();
	}

}
