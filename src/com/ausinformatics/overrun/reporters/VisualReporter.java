package com.ausinformatics.overrun.reporters;

import java.util.ArrayList;
import java.util.List;

import com.ausinformatics.overrun.Unit;
import com.ausinformatics.overrun.visualisation.VisualGameState;
import com.ausinformatics.phais.core.visualisation.EndTurnEvent;
import com.ausinformatics.phais.core.visualisation.EventBasedFrameVisualiser;
import com.ausinformatics.phais.core.visualisation.VisualGameEvent;
import com.ausinformatics.phais.utils.Position;

public class VisualReporter implements Reporter {
	
	private EventBasedFrameVisualiser<VisualGameState> vis;
	private List<VisualGameEvent> pendingEvents;
	private List<Position> minedSquares;

	public VisualReporter (EventBasedFrameVisualiser<VisualGameState> vis) {
		pendingEvents = new ArrayList<VisualGameEvent>();
		minedSquares = new ArrayList<Position>();
		this.vis = vis;
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
	public void unitUpdated(Unit u) {
		// TODO Auto-generated method stub

	}

	@Override
	public void endTurn() {
		pendingEvents.add(new EndTurnEvent());
		vis.giveEvents(pendingEvents);
		pendingEvents = new ArrayList<VisualGameEvent>();
		minedSquares = new ArrayList<Position>();
	}
	
}
