package com.ausinformatics.overrun.visualisation;

import com.ausinformatics.phais.core.visualisation.VisualGameEvent;

public class EndGameEvent extends VisualGameEvent {
    public String winnerName;

    public EndGameEvent(String winnerName) {
        this.winnerName = winnerName;
    }
}
