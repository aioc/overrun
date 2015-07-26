package com.ausinformatics.overrun;

import com.ausinformatics.phais.core.Config;
import com.ausinformatics.phais.core.Director;

public class OverrunMain {
	public static void main(String[] args) {
		Config config = new Config();
		config.parseArgs(args);
		GameFactory f = new GameFactory();
		//config.gameCommands.put("PARAMS", new GameParamsCommand(f));
		new Director(new PlayerFactory(), f).run(config);
	}
}
