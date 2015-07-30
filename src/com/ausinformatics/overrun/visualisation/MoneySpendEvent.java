package com.ausinformatics.overrun.visualisation;

import com.ausinformatics.phais.common.events.VisualGameEvent;

public class MoneySpendEvent extends VisualGameEvent {

    /**
     * 
     */
    private static final long serialVersionUID = -7524409205496600373L;
    public int playerId;
    public int moneySpend;

    public MoneySpendEvent(int playerId, int moneySpend) {
        this.playerId = playerId;
        this.moneySpend = moneySpend;
    }
}
