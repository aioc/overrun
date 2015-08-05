package com.ausinformatics.overrun.core;

import java.util.Collections;
import java.util.List;

import com.ausinformatics.client.GameClient.ProtocolRequest;
import com.ausinformatics.phais.common.events.EventReceiver;
import com.ausinformatics.phais.server.interfaces.GameBuilder;
import com.ausinformatics.phais.server.interfaces.GameInstance;
import com.ausinformatics.phais.server.interfaces.PersistentPlayer;

public class GameFactory implements GameBuilder {

	private TerrainMapFactory mapFactory;
	public int boardSize = 40;

	public GameFactory(TerrainMapFactory mapFactory) {
		this.mapFactory = mapFactory;
	}

	@Override
	public GameInstance createGameInstance(List<PersistentPlayer> players, EventReceiver eventReceiver) {
		Collections.shuffle(players);
		for (int i = 0; i < players.size(); i++) {
			PersistentPlayer p = players.get(i);
			p.getConnection().sendMessage(ProtocolRequest.newBuilder()
					.setCommand(ProtocolRequest.Command.NEWGAME)
					.setNewgame(ProtocolRequest.NewgameParameters.newBuilder()
							.setNumPlayers(players.size())
							.setBoardSize(boardSize)
							.setPlayerId(i))
					.build());
		}

		TerrainMap map = mapFactory.fromDefaultParams(players.size(), boardSize);
		return new GameRunner(eventReceiver, players, boardSize, map);
	}
}
