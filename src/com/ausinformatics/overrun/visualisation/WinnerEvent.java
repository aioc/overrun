package com.ausinformatics.overrun.visualisation;

public class WinnerEvent extends com.ausinformatics.phais.core.visualisation.EndGameEvent {
    public String winnerName;

    public WinnerEvent(String winnerName) {
        super();
        this.winnerName = winnerName;
    }
}
