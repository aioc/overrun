package com.ausinformatics.overrun;

import java.io.PrintStream;

import com.ausinformatics.phais.core.Director;
import com.ausinformatics.phais.core.commander.commands.Command;
import com.ausinformatics.phais.utils.GCRunner;

public class GCTimeoutCommand implements Command {

    private GCRunner gc;

    public GCTimeoutCommand(GCRunner gc) {
        this.gc = gc;
    }

    @Override
    public void execute(Director reportTo, PrintStream out, String[] args) {
        boolean badArgs = false;
        int timeout = 0;
        if (args.length != 1) {
            badArgs = true;
        } else {
            try {
                timeout = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                badArgs = true;
            }
        }

        if (badArgs) {
            out.println("Usage: PARAMS timeout");
        } else {
            gc.timeout = timeout;
        }
    }

    @Override
    public String shortHelpString() {
        return "Change the timeout of the gc runner.\nIn order of timeout";
    }

    @Override
    public String detailedHelpString() {
        return null;
    }

}