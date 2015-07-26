package com.ausinformatics.overrun.visualisation;

import com.ausinformatics.phais.core.visualisation.VisualGameEvent;

public class MoneyDeltaEvent extends VisualGameEvent {
    public int playerId;
    public int moneyDelta;

    public MoneyDeltaEvent(int playerId, int moneyDelta) {
        this.playerId = playerId;
        this.moneyDelta = moneyDelta;
    }
}
