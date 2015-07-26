package com.ausinformatics.overrun.visualisation;

import com.ausinformatics.phais.core.visualisation.VisualGameEvent;
import com.ausinformatics.phais.utils.Position;

public class UnitUpdatedEvent extends VisualGameEvent {
    public int player;
    public int unitId;
    public int currStrength;
    public int prevStrength;
    public Position start;
    public int dir;

    public UnitUpdatedEvent(int player, int unitId, int currStrength, int prevStrength, Position start, int dir) {
        this.player = player;
        this.unitId = unitId;
        this.currStrength = currStrength;
        this.prevStrength = prevStrength;
        this.start = start;
        this.dir = dir;
    }
}
