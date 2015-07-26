package com.ausinformatics.overrun.visualisation;

public class EndGameEvent extends com.ausinformatics.phais.core.visualisation.EndGameEvent {
    public String winnerName;

    public EndGameEvent(String winnerName) {
        super();
        this.winnerName = winnerName;
    }
}
