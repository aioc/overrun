package com.ausinformatics.overrun.visualisation;

public class WinnerEvent extends com.ausinformatics.phais.common.events.events.EndGameEvent {
    /**
     * 
     */
    private static final long serialVersionUID = 713636307920086599L;
    public String winnerName;

    public WinnerEvent(String winnerName) {
        this.winnerName = winnerName;
    }
}
