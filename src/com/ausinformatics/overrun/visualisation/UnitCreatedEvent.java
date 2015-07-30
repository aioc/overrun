package com.ausinformatics.overrun.visualisation;

import com.ausinformatics.phais.common.events.VisualGameEvent;
import com.ausinformatics.phais.utils.Position;

public class UnitCreatedEvent extends VisualGameEvent {
    /**
     * 
     */
    private static final long serialVersionUID = 7736362948423172349L;
    public int player;
    public int unitId;
    public Position p;
    public int strength;

    public UnitCreatedEvent(int player, int unitId, Position p, int strength) {
        this.player = player;
        this.unitId = unitId;
        this.p = p;
        this.strength = strength;
    }
}
