package com.ausinformatics.overrun.visualisation;

import com.ausinformatics.phais.core.visualisation.VisualGameEvent;
import com.ausinformatics.phais.utils.Position;

public class UnitFoughtEvent extends VisualGameEvent {
    public int player;
    public int unitId;
    public int strength;
    public Position p;

    public UnitFoughtEvent(int player, int unitId, int strength, Position p) {
        this.player = player;
        this.unitId = unitId;
        this.strength = strength;
        this.p = p;
    }
}
