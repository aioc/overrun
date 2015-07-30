package com.ausinformatics.overrun.visualisation;

import com.ausinformatics.overrun.core.GCTimeoutCommand;
import com.ausinformatics.phais.common.Config;
import com.ausinformatics.phais.common.events.events.CommonEventManager;
import com.ausinformatics.phais.spectator.VisualiserDirector;
import com.ausinformatics.phais.utils.GCRunner;

public class VisualSetup {

    public void start(Config config) {
        GCRunner gc = new GCRunner();
        config.gameCommands.put("GCTIMEOUT", new GCTimeoutCommand(gc));
        gc.start();
        VisualiserDirector<VisualGameState> d = new VisualiserDirector<>(new CommonEventManager(), new VisualiserFactory());
        d.runForever(config.address, config.port, config.gid);
        gc.timeout = -1;
    }

}
