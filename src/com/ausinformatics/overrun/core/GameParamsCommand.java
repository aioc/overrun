package com.ausinformatics.overrun.core;

import java.io.PrintStream;

import com.ausinformatics.phais.common.commander.Command;

public class GameParamsCommand implements Command {

    private GameFactory f;

    public GameParamsCommand(GameFactory f) {
        this.f = f;
    }

    @Override
    public void execute(PrintStream out, String[] args) {
        boolean badArgs = false;
        int boardSize = 0;
        if (args.length != 1) {
            badArgs = true;
        } else {
            try {
                boardSize = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                badArgs = true;
            }
        }

        if (badArgs) {
            out.println("Usage: PARAMS boardSize");
        } else {
            f.boardSize = boardSize;
        }
    }

    @Override
    public String shortHelpString() {
        return "Change the params of the games.\nIn order of boardSize";
    }

    @Override
    public String detailedHelpString() {
        return null;
    }

}