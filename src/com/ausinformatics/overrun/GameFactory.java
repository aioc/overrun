package com.ausinformatics.overrun;

import java.util.Collections;
import java.util.List;

import com.ausinformatics.overrun.visualisation.FrameVisualiser;
import com.ausinformatics.overrun.visualisation.VisualGameState;
import com.ausinformatics.phais.core.interfaces.GameBuilder;
import com.ausinformatics.phais.core.interfaces.GameInstance;
import com.ausinformatics.phais.core.interfaces.PersistentPlayer;
import com.ausinformatics.phais.core.server.DisconnectedException;
import com.ausinformatics.phais.core.visualisation.EventBasedFrameVisualiser;


public class GameFactory implements GameBuilder {
	
	private TerrainMapFactory mapFactory;
	public int boardSize = 30;
	
	public GameFactory(TerrainMapFactory mapFactory) {
		this.mapFactory = mapFactory;
	}
	
	@Override
	public GameInstance createGameInstance(List<PersistentPlayer> players) {
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
		GameRunner gr = new GameRunner(players, boardSize, mapFactory.fromDefaultParams(players.size(), boardSize));
		FrameVisualiser fv = new FrameVisualiser();
		EventBasedFrameVisualiser<VisualGameState> vis = new EventBasedFrameVisualiser<VisualGameState>(gr, fv,
				new VisualGameState()); // TODO: Fix up this stuff.

		gr.setEventVisualiser(vis);
		return vis;
	}

}
