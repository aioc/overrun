package com.ausinformatics.overrun;

import com.ausinformatics.overrun.visualisation.VisualGameState;
import com.ausinformatics.phais.core.visualisation.EventBasedFrameVisualiser;

public class GameState {
	
	private int numPlayers;
	private int boardSize;
	private TerrainMap map;
	private MoveReporter reporter;
	private EventBasedFrameVisualiser<VisualGameState> vis;
	
	public GameState(int numPlayers, int boardSize, TerrainMap map, MoveReporter reporter) {
		this.numPlayers = numPlayers;
		this.boardSize = boardSize;
		this.map = map;
		this.reporter = reporter;
	}
	
	public void setUpForVisualisation(EventBasedFrameVisualiser<VisualGameState> vis) {
		this.vis = vis;
	}
	
	// This should be able to be called on already dead players.
	public void killPlayer(int id) {
		
	}

	public boolean isPlayerDead(int id) {
		return false;
	}
	
	public void makeMove(int id, PlayerMove move) {
		
	}
	
	public int getScore(int id) {
		return -1;
	}
	
	public void endGame() {
		
	}
	
	
}
