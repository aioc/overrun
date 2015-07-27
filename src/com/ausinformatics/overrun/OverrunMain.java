package com.ausinformatics.overrun;

import com.ausinformatics.phais.core.Config;
import com.ausinformatics.phais.core.Director;

public class OverrunMain {
	public static void main(String[] args) {
		Config config = new Config();
		config.parseArgs(args);
		config.maxParallelGames = 1;
		TerrainMapFactory mapFactory = new TerrainMapFactory();
		GameFactory f = new GameFactory(mapFactory);
		config.gameCommands.put("PARAMS", new GameParamsCommand(f));
		config.gameCommands.put("ALLPARAMS", new AllGameParamsCommand(mapFactory));
		new Director(new PlayerFactory(), f).run(config);
	}
}
