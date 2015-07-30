package com.ausinformatics.overrun.core;

import com.ausinformatics.phais.server.interfaces.PersistentPlayer;
import com.ausinformatics.phais.server.interfaces.PlayerBuilder;
import com.ausinformatics.phais.server.server.ClientConnection;

public class PlayerFactory implements PlayerBuilder {

	@Override
	public PersistentPlayer createPlayer(int ID, ClientConnection client) {
		Player ret = new Player(ID, client);
		ret.generateNewName();
		return ret;
	}

}
