package com.ausinformatics.overrun.visualisation;

import com.ausinformatics.phais.core.visualisation.VisualGameEvent;
import com.ausinformatics.phais.utils.Position;

public class UnitCreatedEvent extends VisualGameEvent {
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
