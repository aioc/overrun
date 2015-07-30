package com.ausinformatics.overrun.visualisation;

import com.ausinformatics.phais.common.events.VisualGameEvent;
import com.ausinformatics.phais.utils.Position;

public class UnitUpdatedEvent extends VisualGameEvent {
    /**
     * 
     */
    private static final long serialVersionUID = -4329478114665454390L;
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
