package com.ausinformatics.overrun;

import com.ausinformatics.phais.core.interfaces.PersistentPlayer;
import com.ausinformatics.phais.core.interfaces.PlayerBuilder;
import com.ausinformatics.phais.core.server.ClientConnection;

public class PlayerFactory implements PlayerBuilder {

	@Override
	public PersistentPlayer createPlayer(int ID, ClientConnection client) {
		Player ret = new Player(ID, client);
		ret.generateNewName();
		return ret;
	}

}
