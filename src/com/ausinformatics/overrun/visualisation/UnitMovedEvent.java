package com.ausinformatics.overrun.visualisation;

import com.ausinformatics.phais.core.visualisation.VisualGameEvent;
import com.ausinformatics.phais.utils.Position;

public class UnitMovedEvent extends VisualGameEvent {
    public int player;
    public int unitId;
    public Position start;
    public int dir;

    public UnitMovedEvent(int player, int unitId, Position start, int dir) {
        this.player = player;
        this.unitId = unitId;
        this.start = start;
        this.dir = dir;
    }
}
