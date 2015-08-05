package com.ausinformatics.overrun.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ausinformatics.client.GameClient.ProtocolRequest;
import com.ausinformatics.overrun.core.reporters.MultiplePlayerReporter;
import com.ausinformatics.overrun.visualisation.InitialGameEvent;
import com.ausinformatics.overrun.visualisation.WinnerEvent;
import com.ausinformatics.phais.common.events.EventReceiver;
import com.ausinformatics.phais.common.events.VisualGameEvent;
import com.ausinformatics.phais.common.events.events.EndGameEvent;
import com.ausinformatics.phais.server.interfaces.GameInstance;
import com.ausinformatics.phais.server.interfaces.PersistentPlayer;
import com.ausinformatics.phais.server.server.ClientConnection;
import com.ausinformatics.phais.server.server.DisconnectedException;

public class GameRunner implements GameInstance {

	private GameState state;
	private List<PersistentPlayer> players;
	private Map<PersistentPlayer, Integer> results;
	private EventReceiver er;
	private MultiplePlayerReporter reporter;
	private TerrainMap map;

	private int[] finalRanks;

	public GameRunner(EventReceiver er, List<PersistentPlayer> players, int boardSize, TerrainMap map) {
		this.players = players;
		this.er = er;
		this.map = map;
		results = new HashMap<PersistentPlayer, Integer>();
		finalRanks = new int[players.size()];
		reporter = new MultiplePlayerReporter(players.size());
		state = new GameState(players.size(), boardSize, map, reporter, er);
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
		InitialGameEvent ev = new InitialGameEvent();
		ev.boardSize = map.getSize();
		ev.numPlayers = players.size();
		ev.playerNames = new ArrayList<>();
		ev.playerColours = new ArrayList<>();
		for (PersistentPlayer p : players) {
			ev.playerNames.add(p.getName());
			ev.playerColours.add(((Player) p).getColour());
		}
		ev.map = new int[ev.boardSize][ev.boardSize];
		for (int i = 0; i < ev.boardSize; i++) {
			for (int j = 0; j < ev.boardSize; j++) {
				ev.map[i][j] = map.getTerrain(j, i);
			}
		}
		List<VisualGameEvent> firstEventList = new ArrayList<>();
		firstEventList.add(ev);
		er.giveEvents(firstEventList);

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
				c.sendMessage(ProtocolRequest.newBuilder().setCommand(ProtocolRequest.Command.COMPUTE_MOVE).build());

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
			players.get(i).getConnection().sendMessage(ProtocolRequest.newBuilder()
					.setCommand(ProtocolRequest.Command.GAMEOVER)
					.setGameover(ProtocolRequest.GameoverParameters.newBuilder().setDetail("" + finalRanks[i]))
					.build());
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

		er.giveEvents(finalEvents);
	}

	@Override
	public Map<PersistentPlayer, Integer> getResults() {
		return results;
	}

	public int getReward(int pos) {
		return 1 + (players.size() - pos - 1) * 10000;
	}
}
