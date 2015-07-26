package com.ausinformatics.overrun.visualisation;

import java.util.List;

import com.ausinformatics.phais.core.visualisation.VisualGameEvent;
import com.ausinformatics.phais.utils.Position;

public class MoneyDeltaEvent extends VisualGameEvent {
    public int playerId;
    public int moneyDelta;
    public List<Position> minedBlocks;

    public MoneyDeltaEvent(int playerId, int moneyDelta, List<Position> minedBlocks) {
        this.playerId = playerId;
        this.moneyDelta = moneyDelta;
        this.minedBlocks = minedBlocks;
    }
}
