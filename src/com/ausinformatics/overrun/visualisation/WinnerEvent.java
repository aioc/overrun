package com.ausinformatics.overrun.visualisation;

public class WinnerEvent extends com.ausinformatics.phais.common.events.events.EndGameEvent {
    public String winnerName;

    public WinnerEvent(String winnerName) {
        super();
        this.winnerName = winnerName;
    }
}
