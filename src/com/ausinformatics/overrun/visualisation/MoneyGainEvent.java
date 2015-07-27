package com.ausinformatics.overrun.visualisation;

import java.util.List;

import com.ausinformatics.phais.core.visualisation.VisualGameEvent;
import com.ausinformatics.phais.utils.Position;

public class MoneyGainEvent extends VisualGameEvent {
    public int playerId;
    public int moneyGain;
    public List<Position> minedBlocks;

    public MoneyGainEvent(int playerId, int moneyGain, List<Position> minedBlocks) {
        this.playerId = playerId;
        this.moneyGain = moneyGain;
        this.minedBlocks = minedBlocks;
    }
}