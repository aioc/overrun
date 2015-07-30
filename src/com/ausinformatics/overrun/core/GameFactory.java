package com.ausinformatics.overrun.core;

import java.util.Collections;
import java.util.List;

import com.ausinformatics.phais.common.events.EventReceiver;
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
        GameRunner gr = new GameRunner(eventReceiver, players, boardSize, map);
        return gr;
    }

}
