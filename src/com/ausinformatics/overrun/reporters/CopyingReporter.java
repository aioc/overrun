package com.ausinformatics.overrun.reporters;

import com.ausinformatics.overrun.Unit;
import com.ausinformatics.phais.utils.Position;

public class CopyingReporter implements Reporter {

	private Reporter r1, r2;
	
	public CopyingReporter(Reporter r1, Reporter r2) {
		this.r1 = r1;
		this.r2 = r2;
	}

	@Override
	public void sendError(int id, String error) {
		r1.sendError(id, error);
		r2.sendError(id, error);
	}
	
	@Override
	public void squareUpdated(Position p, int newVal) {
		r1.squareUpdated(p, newVal);
		r2.squareUpdated(p, newVal);
	}

	@Override
	public void personMoneyChange(int id, int amount) {
		r1.personMoneyChange(id, amount);
		r2.personMoneyChange(id, amount);
	}

	@Override
	public void unitCreated(Unit u) {
		r1.unitCreated(u);
		r2.unitCreated(u);
	}

	@Override
	public void unitUpdated(Unit oldU, Unit newU) {
		r1.unitUpdated(oldU, newU);
		r2.unitUpdated(oldU, newU);
	}
	
	@Override
	public void endTurn() {
		r1.endTurn();
		r2.endTurn();
	}

}
