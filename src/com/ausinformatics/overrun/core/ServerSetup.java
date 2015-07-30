package com.ausinformatics.overrun.core;

import com.ausinformatics.phais.common.Config;
import com.ausinformatics.phais.common.events.events.CommonEventManager;
import com.ausinformatics.phais.server.Director;

public class ServerSetup {

    public void start(Config config) {
        config.maxParallelGames = 10;
        TerrainMapFactory mapFactory = new TerrainMapFactory();
        GameFactory f = new GameFactory(mapFactory);
        config.gameCommands.put("PARAMS", new GameParamsCommand(f));
        config.gameCommands.put("ALLPARAMS", new AllGameParamsCommand(mapFactory));
        new Director(new PlayerFactory(), f, new CommonEventManager()).run(config);
    }
    
}
