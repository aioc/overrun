package com.ausinformatics.overrun.core;

import java.io.PrintStream;

import com.ausinformatics.phais.common.commander.Command;
import com.ausinformatics.phais.server.Director;

public class AllGameParamsCommand implements Command {

	private TerrainMapFactory f;

	public AllGameParamsCommand(TerrainMapFactory f) {
		this.f = f;
	}

	@Override
	public void execute(Director reportTo, PrintStream out, String[] args) {
		boolean badArgs = false;

		int baseOffset = 6;
		int amoMin = 20;
		int amoPerMin = 100;
		int sharpness = 4;
		double wallAttract = 1.0;
		double baseAttract = -50.0;
		double rockCutOff = 0.58;
		double minAttract = 4.0;
		double otherWeight = 100.0;
		if (args.length != 9) {
			badArgs = true;
		} else {
			try {
				baseOffset = Integer.parseInt(args[0]);
				amoMin = Integer.parseInt(args[1]);
				amoPerMin = Integer.parseInt(args[2]);
				sharpness = Integer.parseInt(args[3]);
				wallAttract = Double.parseDouble(args[4]);
				baseAttract = Double.parseDouble(args[5]);
				rockCutOff = Double.parseDouble(args[6]);
				minAttract = Double.parseDouble(args[7]);
				otherWeight = Double.parseDouble(args[8]);
			} catch (NumberFormatException e) {
				badArgs = true;
			}
		}

		if (badArgs) {
			out.println(
					"Usage: PARAMS baseOffset amoMin amoPerMin sharpness wallAttract baseAttract rockCutOff minAttract otherWeight");
		} else {
			f.baseOffset = baseOffset;
			f.amoMin = amoMin;
			f.amoPerMin = amoPerMin;
			f.sharpness = sharpness;
			f.wallAttract = wallAttract;
			f.baseAttract = baseAttract;
			f.rockCutOff = rockCutOff;
			f.minAttract = minAttract;
			f.otherWeight = otherWeight;
		}
	}

	@Override
	public String shortHelpString() {
		return "Change the params of the games.\nIn order of baseOffset amoMin amoPerMin sharpness wallAttract baseAttract rockCutOff minAttract otherWeight";
	}

	@Override
	public String detailedHelpString() {
		return null;
	}
}
