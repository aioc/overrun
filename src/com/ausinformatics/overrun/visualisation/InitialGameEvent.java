package com.ausinformatics.overrun.visualisation;

import java.util.List;

import com.ausinformatics.phais.common.events.VisualGameEvent;

public class InitialGameEvent extends VisualGameEvent {
    
    /**
     * 
     */
    private static final long serialVersionUID = 3240090713538275616L;
    public int boardSize;
    public int numPlayers;
    public List<String> playerNames;
    public List<Integer> playerColours;
    public int[][] map;
}
