package com.ausinformatics.overrun.reporters;

import com.ausinformatics.overrun.Unit;
import com.ausinformatics.phais.utils.Position;

public interface Reporter {

	public void sendError(int id, String error);

	public void squareUpdated(Position p, int newVal);
	
	public void personMoneyChange(int id, int amount);
	
	public void unitCreated(Unit u);
	
	public void unitUpdated(Unit u);
	
	public void endTurn();
	
}
