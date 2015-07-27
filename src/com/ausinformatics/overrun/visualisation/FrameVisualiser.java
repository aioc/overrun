package com.ausinformatics.overrun.visualisation;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import com.ausinformatics.overrun.TerrainMap;
import com.ausinformatics.overrun.Unit;
import com.ausinformatics.phais.core.visualisation.EndTurnEvent;
import com.ausinformatics.phais.core.visualisation.FrameVisualisationHandler;
import com.ausinformatics.phais.core.visualisation.VisualGameEvent;
import com.ausinformatics.phais.utils.Pair;
import com.ausinformatics.phais.utils.Position;
import com.ausinformatics.phais.utils.VisualisationUtils;
import com.ausinformatics.phais.utils.VisualisationUtils.Box;
import com.ausinformatics.phais.utils.VisualisationUtils.BoxFactory;

public class FrameVisualiser implements FrameVisualisationHandler<VisualGameState> {
		private boolean render;
		private static final int LARGE_BORDER = 10;
		private static final int SMALL_BORDER = 3;
		private final int CREATED_FRAMES = 1;
		private final int UPDATED_FRAMES = 1;
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
			int boardN = state.boardSize;
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
			titleBox = f.fromMixedHeight(statsBox.left + SMALL_BORDER, statsBox.top + SMALL_BORDER,
						statsBox.right - SMALL_BORDER, statsBox.height / 10);
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
			for (int y = 0; y < state.boardSize; y++) {
				for (int x = 0; x < state.boardSize; x++) {
						int lwall = state.tileVals[y][x];
						if (lwall == TerrainMap.WALL) {
							g.setColor(Color.BLACK);
						} else if (lwall < 0) {
							g.setColor(state.colours[-lwall - 1]);
						} else {
							g.setColor(Color.WHITE);
						}
						boardBoxes[y][x].fill(g);
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
				System.err.println("Couldn't find the resource \"emulogic.ttf\", "
							+ "you probably misconfigured something when building the " + "server");
				rootFont = g.getFont();
			}
		}

		@Override
		public void generateState(VisualGameState state, int sWidth, int sHeight, Graphics2D g) {
			if (!render) {
				return;
			}

			pulseCounter++;
			for (int y = 0; y < state.boardSize; y++) {
				for (int x = 0; x < state.boardSize; x++) {
						int mineralPatch = state.tileVals[y][x];
						if (mineralPatch > 0) {
							float colorMul = ((float) mineralPatch) / 50;
							colorMul = Math.min(1, colorMul);
							Color interiorColor = avColor(Color.white,
										Color.getHSBColor((System.currentTimeMillis() % 2000) / 2000f, 0.2f, 0.4f), colorMul);
							g.setColor(interiorColor);
							boardBoxes[y][x].fill(g);
						}
				}
			}

		}

		@Override
		public void animateEvents(VisualGameState state, List<VisualGameEvent> events, int sWidth, int sHeight,
				Graphics2D g) {
			if (!render) {
				return;
			}
			BoxFactory f = new BoxFactory(sWidth, sHeight);
			for (VisualGameEvent ev : events) {
				if (ev instanceof UnitUpdatedEvent) {
						UnitUpdatedEvent e = (UnitUpdatedEvent) ev;
						state.units.remove(new Pair<Integer, Integer>(e.player, e.unitId));
						/* Tween on currentStrength and current position */
						tweenUpdateEvent(state, f, e, g);
				} else if (ev instanceof UnitCreatedEvent) {
						// Just draw the unit
				} else if (ev instanceof MoneyDeltaEvent) {
						// Update the player's money
				}
			}

			for (Entry<Pair<Integer, Integer>, Unit> entry : state.units.entrySet()) {
				Unit u = entry.getValue();
				g.setColor(getUnitFillColour(u, state.colours[u.ownerId]));
				Box box = boardBoxes[u.p.r][u.p.c];
				g.fillOval(box.left, box.top, box.width, box.height);
				g.setColor(getUnitOutlineColour(u, state.colours[u.ownerId]));
				g.drawOval(box.left, box.top, box.width, box.height);
			}
			if (state.winner != null) {
				g.setColor(Color.black);
				winnerScreen.fill(g);
				VisualisationUtils.drawString(g, winnerScreen, rootFont, state.winner, Color.WHITE);
			}
		}

		@Override
		public void eventCreated(VisualGameEvent e) {
			if (e instanceof UnitCreatedEvent) {
				e.totalFrames = CREATED_FRAMES;
			} else if (e instanceof UnitUpdatedEvent) {
				e.totalFrames = UPDATED_FRAMES;
			} else if (e instanceof MoneyDeltaEvent) {
				e.totalFrames = MONEY_DELTA_FRAMES;
			} else if (e instanceof EndTurnEvent) {
				e.totalFrames = Math.max(CREATED_FRAMES, Math.max(MONEY_DELTA_FRAMES, UPDATED_FRAMES));
			} else if (e instanceof EndGameEvent) {
				e.totalFrames = 60;
			}
		}

		@Override
		public void eventEnded(VisualGameEvent e, VisualGameState state) {
			if (e instanceof UnitCreatedEvent) {
				UnitCreatedEvent ev = (UnitCreatedEvent) e;
				state.units.put(new Pair<Integer, Integer>(ev.player, ev.unitId), new Unit(ev.strength, ev.unitId, ev.player, ev.p));
			} else if (e instanceof UnitUpdatedEvent) {
				UnitUpdatedEvent ev = (UnitUpdatedEvent) e;
				if (ev.currStrength > 0) {
					state.units.put(new Pair<Integer, Integer>(ev.player, ev.unitId), new Unit(ev.currStrength, ev.unitId, ev.player, ev.start.move(ev.dir)));
				}
			} else if (e instanceof MoneyDeltaEvent) {
				MoneyDeltaEvent ev = (MoneyDeltaEvent) e;
				state.money[ev.playerId] += ev.moneyDelta;
				for (Position pos : ev.minedBlocks) {
						state.tileVals[pos.r][pos.c]--;
				}
			} else if (e instanceof EndGameEvent) {
				state.winner = ((EndGameEvent) e).winnerName;
			}
		}

		private void tweenUpdateEvent(VisualGameState state, BoxFactory f, UnitUpdatedEvent event, Graphics2D g) {
			Unit startUnit = new Unit(event.prevStrength, -1, -1, null);
			Color startColour = getUnitFillColour(startUnit, state.colours[event.player]);
			Unit endUnit = new Unit(event.currStrength, -1, -1, null);
			Color endColour = getUnitFillColour(endUnit, state.colours[event.player]);
			int amount = event.curFrame;
			g.setColor(avColor(startColour, endColour, amount / event.totalFrames));
			Box tweendItem = VisualisationUtils.tweenMovement(f, boardBoxes, event.start, event.start.move(event.dir),
						((double) amount) / UPDATED_FRAMES, 0);
			g.fillOval(tweendItem.left, tweendItem.top, tweendItem.width, tweendItem.height);
		}

		private float getColourMulForUnit(Unit u) {
			float colourMul = ((float) u.strength) / 100;

			colourMul = Math.min(1, colourMul); // in range [0,1]
			colourMul = (float) (0.3 + 0.7 * colourMul);
			return colourMul;
		}

		private Color getUnitFillColour(Unit u, Color c) {
			return avColor(Color.gray, c, getColourMulForUnit(u));
		}

		private Color getUnitOutlineColour(Unit u, Color c) {
			return avColor(Color.black, c, 0.5f);
		}

		private Color avColor(float r1, float g1, float b1, float r2, float g2, float b2, float colorMul) {
			float redVal = (r1 * (1 - colorMul) + r2 * colorMul);
			float greenVal = (g1 * (1 - colorMul) + g2 * colorMul);
			float blueVal = (b1 * (1 - colorMul) + b2 * colorMul);
			return new Color(redVal, greenVal, blueVal);
		}

		private Color avColor(Color c1, Color c2, float colorMul) {
			return avColor(c1.getRed() / 255f, c1.getGreen() / 255f, c1.getBlue() / 255f, c2.getRed() / 255f,
						c2.getGreen() / 255f, c2.getBlue() / 255f, colorMul);
		}
}
