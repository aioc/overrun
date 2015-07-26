package com.ausinformatics.overrun.visualisation;

import com.ausinformatics.phais.core.visualisation.VisualGameEvent;
import com.ausinformatics.phais.utils.Position;

public class UnitKilledEvent extends VisualGameEvent {
    public int player;
    public int unitId;
    public Position p;

    public UnitKilledEvent(int player, int unitId, Position p) {
        this.player = player;
        this.unitId = unitId;
        this.p = p;
    }
}
