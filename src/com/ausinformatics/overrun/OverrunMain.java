package com.ausinformatics.overrun;

import com.ausinformatics.overrun.core.ServerSetup;
import com.ausinformatics.overrun.visualisation.VisualSetup;
import com.ausinformatics.phais.common.Config;

public class OverrunMain {
	public static void main(String[] args) {
		Config config = new Config();
		config.parseArgs(args);
		if (config.visualise) {
            new VisualSetup().start(config);
		} else {
		    new ServerSetup().start(config);
		}
	}
}
