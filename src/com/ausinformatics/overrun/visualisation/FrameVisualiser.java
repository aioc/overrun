package com.ausinformatics.overrun.visualisation;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import com.ausinformatics.phais.core.visualisation.EndGameEvent;
import com.ausinformatics.phais.core.visualisation.EndTurnEvent;
import com.ausinformatics.phais.core.visualisation.FrameVisualisationHandler;
import com.ausinformatics.phais.core.visualisation.VisualGameEvent;

public class FrameVisualiser implements FrameVisualisationHandler<VisualGameState>{
    private boolean render;
    private static final int LARGE_BORDER = 10;
    private static final int SMALL_BORDER = 3;
    private final int CREATED_FRAMES = 1;
    private final int MOVED_FRAMES = 5;
    private final int FOUGHT_FRAMES = 1;
    private final int KILLED_FRAMES = 1;
    private final int MONEY_DELTA_FRAMES = 1;
    private Box boardBox;
    private Box statsBox;
    private Box[][] boardBoxes;
    private Box titleBox;
    private Box[] playerNameBoxes;
    private Box[] statBoxes;
    private Box winnerScreen;
    private Font rootFont;

    private int pulseCounter = 0;

    @Override
    public void generateBackground(VisualGameState state, int sWidth, int sHeight, Graphics2D g) {
        // Draw the grid, home bases, tag boxes + static information in them.
        render = false;
        BoxFactory f = new BoxFactory(sWidth, sHeight);
        int width = sWidth - 2 * LARGE_BORDER;
        int height = sHeight - 2 * LARGE_BORDER;
        // Divide into two parts: The board, and the stats.
        // Board takes at most 2/3rds of the width.
        int boardWidth = 2 * (width - LARGE_BORDER) / 3;
        int boardSize = Math.min(height, boardWidth);
        int boardN = state.boardSize + 2;
        if (boardSize < 5 * boardN) {
            return;
        }
        // Round down to the nearest multiple of state.boardSize;
        boardSize -= (boardSize % boardN);
        boardBox = f.fromDimensions(LARGE_BORDER, LARGE_BORDER, boardSize, boardSize);
        // Take up a lot of space with the stats.
        statsBox = f.fromPoints(boardBox.right + LARGE_BORDER, LARGE_BORDER, LARGE_BORDER, LARGE_BORDER);
        // Adjust these based of whether it is valid or not.
        if (statsBox.width < 70 || statsBox.height < state.numPlayers * 10 + 20 || statsBox.height < 100) {
            return;
        }
        // We *should* have enough space, so lets define all the rest of the
        // boxes.
        boardBoxes = new Box[boardN][boardN];
        int squareSize = boardBox.height / boardN;
        // We either have a 1px border, or a 2px border.
        int border = squareSize > 10 ? 2 : 1;
        for (int i = 0; i < boardN; i++) {
            for (int j = 0; j < boardN; j++) {
                boardBoxes[i][j] = f.fromDimensions(boardBox.left + j * squareSize, boardBox.top + i * squareSize,
                        squareSize - border, squareSize - border);
            }
        }
        titleBox = f.fromMixedHeight(statsBox.left + SMALL_BORDER, statsBox.top + SMALL_BORDER, statsBox.right
                - SMALL_BORDER, statsBox.height / 10);
        playerNameBoxes = new Box[state.numPlayers];
        statBoxes = new Box[state.numPlayers];
        int playerHeight = (statsBox.height - titleBox.height) / state.numPlayers;
        if (playerHeight < 10) {
            return;
        }
        for (int i = 0; i < state.numPlayers; i++) {
            Box b = f.fromMixedHeight(statsBox.left + SMALL_BORDER, titleBox.bottom + LARGE_BORDER + i * playerHeight,
                    statsBox.right - SMALL_BORDER, playerHeight - LARGE_BORDER);
            playerNameBoxes[i] = f.fromDimensions(b.left, b.top, b.width, b.height / 4);
            statBoxes[i] = f.fromMixedWidth(b.left, playerNameBoxes[i].bottom + LARGE_BORDER, b.width, b.bottom);
        }
        winnerScreen = f.fromDimensions(sWidth / 4, sHeight / 4, sWidth / 2, sHeight / 2);
        // Everything is defined. We start rendering.
        render = true;
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, sWidth, sHeight);
        for (int i = 0; i < boardN; i++) {
            for (int j = 0; j < boardN; j++) {
                if (i == 0 || j == 0 || i == boardN - 1 || j == boardN - 1) {
                    g.setColor(Color.BLACK);
                } else {
                    g.setColor(Color.WHITE);
                }
                boardBoxes[i][j].fill(g);
            }
        }
        g.setColor(Color.LIGHT_GRAY);
        titleBox.fill(g);
        for (int i = 0; i < state.numPlayers; i++) {
            playerNameBoxes[i].fill(g);
            statBoxes[i].fill(g);
        }
        try {
            rootFont = Font.createFont(Font.TRUETYPE_FONT, FrameVisualiser.class.getResourceAsStream("emulogic.ttf"));
        } catch (FontFormatException | IOException e) {
            rootFont = g.getFont();
        }
    }

    @Override
    public void generateState(VisualGameState state, int sWidth, int sHeight, Graphics2D globalGraphics) {
        // Ignore the g here. We're hijacking; keep our own sheet.
        if (!render)
            return;
        stateImage = new BufferedImage(sWidth,
                                       sHeight,
                                       BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g = (Graphics2D) stateImage.getGraphics();
        g.dispose();
    }

    @Override
    public void animateEvents(VisualGameState state, List<VisualGameEvent> events, int sWidth, int sHeight, Graphics2D g) {
        pulseCounter++;
        Image pulseImage = new BufferedImage(sWidth,
                                             sHeight,
                                             BufferedImage.TYPE_4BYTE_ABGR);
        g.drawImage(pulseImage, 0 /* x */, 0 /* y */, null /* observer */);
        g.drawImage(stateImage, 0 /* x */, 0 /* y */, null /* observer */);
        stateImage.flush();
        stateImage = null;
        /* Now draw the animation events directly onto the Graphics object */
    }

    @Override
    public void eventCreated(VisualGameEvent e) {
        if (e instanceof UnitCreatedEvent) {
            e.totalFrames = CREATED_FRAMES;
        } else if (e instanceof UnitMovedEvent) {
            e.totalFrames = MOVED_FRAMES;
        } else if (e instanceof UnitFoughtEvent) {
            e.totalFrames = FOUGHT_FRAMES;
        } else if (e instanceof UnitKilledEvent) {
            e.totalFrames = KILLED_FRAMES;
        } else if (e instanceof MoneyDeltaEvent) {
            e.totalFrames = MONEY_DELTA_FRAMES;
        } else if (e instanceof EndTurnEvent) {
            e.totalFrames = Math.max(CREATED_FRAMES,
                                     Math.max(MOVED_FRAMES,
                                              Math.max(KILLED_FRAMES,
                                                       MONEY_DELTA_FRAMES)));
        } else if (e instanceof EndGameEvent) {
            e.totalFrames = 60;
        }
    }

    @Override
    public void eventEnded(VisualGameEvent e, VisualGameState state) {
        // TODO Auto-generated method stub

    }

    private Font getLargestFittingFont(Font f, Box b, Graphics2D g, String s, int largestSize) {
        int minSize = 1;
        int maxSize = largestSize;
        while (minSize < maxSize) {
            int midSize = (minSize + maxSize) / 2;
            f = f.deriveFont(Font.PLAIN, midSize);
            FontMetrics fm = g.getFontMetrics(f);
            Rectangle2D fR = fm.getStringBounds(s, g);
            if (fR.getWidth() < b.width - 20 && fR.getHeight() < b.height) {
                minSize = midSize + 1;
            } else {
                maxSize = midSize - 1;
            }
        }
        return f.deriveFont(minSize);
    }

}
