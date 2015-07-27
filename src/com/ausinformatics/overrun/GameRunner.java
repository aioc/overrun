package com.ausinformatics.overrun;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ausinformatics.overrun.reporters.ConnectionReporter;
import com.ausinformatics.overrun.visualisation.VisualGameState;
import com.ausinformatics.overrun.visualisation.WinnerEvent;
import com.ausinformatics.phais.core.interfaces.PersistentPlayer;
import com.ausinformatics.phais.core.server.ClientConnection;
import com.ausinformatics.phais.core.server.DisconnectedException;
import com.ausinformatics.phais.core.visualisation.EndGameEvent;
import com.ausinformatics.phais.core.visualisation.EventBasedFrameVisualiser;
import com.ausinformatics.phais.core.visualisation.GameHandler;
import com.ausinformatics.phais.core.visualisation.VisualGameEvent;

public class GameRunner implements GameHandler {

	private GameState state;
	private List<PersistentPlayer> players;
	private Map<PersistentPlayer, Integer> results;
	private EventBasedFrameVisualiser<VisualGameState> vis;
	private ConnectionReporter reporter;

	private int[] finalRanks;

	public GameRunner(List<PersistentPlayer> players, int boardSize, TerrainMap map) {
		this.players = players;
		results = new HashMap<PersistentPlayer, Integer>();
		finalRanks = new int[players.size()];
		reporter = new ConnectionReporter(players.size());
		state = new GameState(players.size(), boardSize, map, reporter);
	}

	public void setEventVisualiser(EventBasedFrameVisualiser<VisualGameState> vis) {
		this.vis = vis;
		state.setUpForVisualisation(vis);
	}

	private boolean isFinished(int playerIndex) {
		return results.containsKey(players.get(playerIndex));
	}

	private void killPlayers(List<Integer> toKill) {
		for (Integer i : toKill) {
			finalRanks[i] = players.size() - results.size() - toKill.size() + 1;
		}
		for (Integer i : toKill) {
			results.put(players.get(i), getReward(finalRanks[i] - 1));
		}
	}

	private void killPlayer(int toKill) {
		killPlayers(Arrays.asList(toKill));
	}

	@Override
	public void begin() {
		int curTurn = 0;
		while (results.size() < players.size() - 1 && curTurn < 500) {
			curTurn++;
			for (int i = 0; i < players.size() && results.size() < players.size() - 1; i++) {
				PersistentPlayer p = players.get(i);
				ClientConnection c = p.getConnection();
				if (!c.isConnected() && !isFinished(i)) {
					state.killPlayer(i);
					killPlayer(i);
					continue;
				}
				if (isFinished(i)) {
					continue;
				}
				reporter.sendForPlayer(i, c);
				c.sendInfo("YOURMOVE");
				boolean playerDied = false;
				try {
					PlayerMove move = PlayerMove.readPlayerMove(c);
					state.makeMove(i, move);
					for (int j = 0; j < players.size(); j++) {
						if (state.isPlayerDead(j) && !isFinished(j)) {
							state.killPlayer(j);
							killPlayer(j);
						}
					}
				} catch (DisconnectedException ex) {
					playerDied = true;
				} catch (BadProtocolException ex) {
					c.sendInfo("BADPROT Invalid action. " + ex.getExtraInfo());
					playerDied = true;
				}
				if (playerDied) {
					state.killPlayer(i);
					killPlayer(i);
				}
			}
		}
		while (results.size() < players.size()) {
			int minV = -1;
			List<Integer> curMin = null;
			for (int i = 0; i < players.size(); i++) {
				if (!isFinished(i) && (minV == -1 || (state.getScore(i) < state.getScore(minV)))) {
					minV = i;
					curMin = new ArrayList<Integer>();
					curMin.add(i);
				} else if (!isFinished(i) && (minV == -1 || (state.getScore(i) == state.getScore(minV)))) {
					curMin.add(i);
				}
			}
			killPlayers(curMin);
		}
		int amoWinners = 0;
		String name = "";
		for (int i = 0; i < players.size(); i++) {
			players.get(i).getConnection().sendInfo("GAMEOVER " + finalRanks[i]);
			if (finalRanks[i] == 1) {
				amoWinners++;
				name = players.get(i).getName();
			}
		}
		if (amoWinners > 1) {
			name = "It was a draw";
		} else {
			name = "The winner was " + name;
		}
		
		
		state.endGame();
	
		List<VisualGameEvent> finalEvents = new ArrayList<VisualGameEvent>();
		finalEvents.add(new WinnerEvent(name));
		finalEvents.add(new EndGameEvent());
		
		vis.giveEvents(finalEvents);
		int round = 0;
		while (!vis.finishedVisualising() && vis.isVisualising() && round < 500) {
			try {
				round++;
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (vis.isVisualising()) {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	@Override
	public Map<PersistentPlayer, Integer> getResults() {
		return results;
	}

	public int getReward(int pos) {
		return 1 + (players.size() - pos - 1) * 10000;
	}
}
