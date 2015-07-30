package com.ausinformatics.overrun.visualisation;

import com.ausinformatics.phais.spectator.interfaces.FrameVisualisationHandler;
import com.ausinformatics.phais.spectator.interfaces.FrameVisualiserFactory;

public class VisualiserFactory implements FrameVisualiserFactory<VisualGameState> {

    @Override
    public FrameVisualisationHandler<VisualGameState> createHandler() {
        return new FrameVisualiser();
    }

}
