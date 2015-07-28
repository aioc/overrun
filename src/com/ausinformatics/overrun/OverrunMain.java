package com.ausinformatics.overrun;

import com.ausinformatics.phais.core.Config;
import com.ausinformatics.phais.core.Director;
import com.ausinformatics.phais.utils.GCRunner;

public class OverrunMain {
	public static void main(String[] args) {
		Config config = new Config();
		config.parseArgs(args);
		config.maxParallelGames = 1;
		//config.visualise = false;
		TerrainMapFactory mapFactory = new TerrainMapFactory();
		GameFactory f = new GameFactory(mapFactory);
		GCRunner gc = new GCRunner();
		config.gameCommands.put("PARAMS", new GameParamsCommand(f));
		config.gameCommands.put("ALLPARAMS", new AllGameParamsCommand(mapFactory));
	    config.gameCommands.put("GCTIMEOUT", new GCTimeoutCommand(gc));
	    new Thread(gc).start();
		new Director(new PlayerFactory(), f).run(config);
	}
}
