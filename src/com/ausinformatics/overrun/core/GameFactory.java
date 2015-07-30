package com.ausinformatics.overrun.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ausinformatics.overrun.visualisation.InitialGameEvent;
import com.ausinformatics.phais.common.events.EventReceiver;
import com.ausinformatics.phais.common.events.VisualGameEvent;
import com.ausinformatics.phais.server.interfaces.GameBuilder;
import com.ausinformatics.phais.server.interfaces.GameInstance;
import com.ausinformatics.phais.server.interfaces.PersistentPlayer;
import com.ausinformatics.phais.server.server.DisconnectedException;

public class GameFactory implements GameBuilder {

	private TerrainMapFactory mapFactory;
	public int boardSize = 40;

	public GameFactory(TerrainMapFactory mapFactory) {
		this.mapFactory = mapFactory;
	}
	
    @Override
    public GameInstance createGameInstance(List<PersistentPlayer> players, EventReceiver eventReceiver) {
        int boardSize = this.boardSize;
        Collections.shuffle(players);
        for (int i = 0; i < players.size(); i++) {
            PersistentPlayer p = players.get(i);
            String toSend = "NEWGAME " + players.size() + " " + boardSize + " " + i;
            p.getConnection().sendInfo(toSend);
            try {
                String inputString = p.getConnection().getStrInput();
                String[] tokens = inputString.split("\\s");
                if (tokens.length != 1) {
                    p.getConnection().disconnect();
                    continue;
                } else if (!tokens[0].equals("READY")) {
                    p.getConnection().disconnect();
                    continue;
                }
            } catch (DisconnectedException de) {
                p.getConnection().disconnect();
            }
        }
        TerrainMap map = mapFactory.fromDefaultParams(players.size(), boardSize);
        
        // Ok, find out about players and create the event.
        InitialGameEvent ev = new InitialGameEvent();
        ev.boardSize = boardSize;
        ev.numPlayers = players.size();
        ev.playerNames = new ArrayList<>();
        ev.playerColours = new ArrayList<>();
        for (PersistentPlayer p : players) {
            ev.playerNames.add(p.getName());
            ev.playerColours.add(((Player) p).getColour());
        }
        ev.map = new int[boardSize][boardSize];
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                ev.map[i][j] = map.getTerrain(j, i);
            }
        }
        List<VisualGameEvent> firstEventList = new ArrayList<>();
        firstEventList.add(ev);
        eventReceiver.giveEvents(firstEventList);
        GameRunner gr = new GameRunner(eventReceiver, players, boardSize, map);
        return gr;
    }

}
