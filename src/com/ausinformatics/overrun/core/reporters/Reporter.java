package com.ausinformatics.overrun.core.reporters;

import com.ausinformatics.overrun.core.Unit;
import com.ausinformatics.phais.utils.Position;

public interface Reporter {

	public void sendError(int id, String error);

	public void squareUpdated(Position p, int oldVal, int newVal);
	
	public void personMoneyChange(int id, int amount);
	
	public void unitCreated(Unit u);
	
	public void unitUpdated(Unit oldU, Unit newU);
	
	public void endTurn();
	
}
