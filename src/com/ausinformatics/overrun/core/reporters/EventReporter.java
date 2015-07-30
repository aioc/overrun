package com.ausinformatics.overrun.core.reporters;

import java.util.ArrayList;
import java.util.List;

import com.ausinformatics.overrun.core.Unit;
import com.ausinformatics.overrun.visualisation.MoneyGainEvent;
import com.ausinformatics.overrun.visualisation.MoneySpendEvent;
import com.ausinformatics.overrun.visualisation.UnitCreatedEvent;
import com.ausinformatics.overrun.visualisation.UnitUpdatedEvent;
import com.ausinformatics.phais.common.events.EventReceiver;
import com.ausinformatics.phais.common.events.VisualGameEvent;
import com.ausinformatics.phais.common.events.events.EndTurnEvent;
import com.ausinformatics.phais.utils.Pair;
import com.ausinformatics.phais.utils.Position;

// Idea of this class is to only hold stuff for one turn, as it will then disappear.
public class EventReporter implements Reporter {

	private EventReceiver er;
	private List<VisualGameEvent> pendingEvents;
	private List<Position> minedSquares;
	private int curMoneyGain;
	private int curMoneySpend;
	private int playerId;
	private UnitUpdateHandler updatesHandler;

	public EventReporter(EventReceiver er) {
		this.er = er;
		updatesHandler = new UnitUpdateHandler();
		reset();
	}

	@Override
	public void sendError(int id, String error) {
		// Don't do anything.
	}

	@Override
	public void squareUpdated(Position p, int oldVal, int newVal) {
		if (newVal == oldVal - 1) {
			minedSquares.add(p);
		}
	}

	@Override
	public void personMoneyChange(int id, int amount) {
		if (amount > 0) {
			curMoneyGain += amount;
		} else {
			curMoneySpend += -amount;
		}
		playerId = id;
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
			pendingEvents.add(new UnitUpdatedEvent(up.first.ownerId, up.first.myId, up.second.strength, up.first.strength,
					up.first.p, getDirMoved(up.first.p, up.second.p)));
		}
		if (curMoneyGain > 0) {
			pendingEvents.add(new MoneyGainEvent(playerId, curMoneyGain, minedSquares));
		}
		if (curMoneySpend > 0) {
			pendingEvents.add(new MoneySpendEvent(playerId, curMoneySpend));
		}
		pendingEvents.add(new EndTurnEvent());
		er.giveEvents(pendingEvents);
		reset();
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
		curMoneyGain = curMoneySpend = 0;
		updatesHandler.clear();
	}

}
