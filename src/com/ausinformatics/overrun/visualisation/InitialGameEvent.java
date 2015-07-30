package com.ausinformatics.overrun.visualisation;

import java.util.List;

import com.ausinformatics.phais.common.events.VisualGameEvent;

public class InitialGameEvent extends VisualGameEvent {
    
    public int boardSize;
    public int numPlayers;
    public List<String> playerNames;
    public List<Integer> playerColours;
    public int[][] map;
}
