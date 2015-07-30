package com.ausinformatics.overrun.core;

import java.awt.Point;
import java.util.Random;

/**
 * Handles the generation of maps
 */
public class TerrainMap {

	private int[][] currentMap;
	private Point[] bases;

	// Magic number for walls
	public static final int WALL = -99;
	// A blank square
	private final int BLANK = 0;
	// I don't even know
	private final int NO_WALLS = 0;
	private final int SIDE_WALLS = 1;
	private final int ALL_WALLS = 2;
	// dy/dx arrays; must have the same number of elements as each other
	private final int[] dx = { -1, 0, 1, 0 };
	private final int[] dy = { 0, -1, 0, 1 };

	/**
	 * The requisite information that genTerrain() needs to function properly
	 */
	private long seed;
	private int numPlayers;
	private int boardsize;
	private int baseOffset;
	private int sharpness;
	private int amoMin;
	private int amoPerMin;
	private double wallAttract;
	private double baseAttract;
	private double rockCutOff;
	private double minAttract;
	private double otherWeight;

	public TerrainMap(long seed, int numPlayers, int boardsize, int baseOffset, int amoMin, int amoPerMin,
			int sharpness, double wallAttract, double baseAttract, double rockCutOff, double minAttract,
			double otherWeight) {

		this.seed = seed;
		this.numPlayers = numPlayers;
		this.boardsize = boardsize;
		this.baseOffset = baseOffset;
		this.sharpness = sharpness;
		this.amoMin = amoMin;
		this.amoPerMin = amoPerMin;
		this.wallAttract = wallAttract;
		this.baseAttract = baseAttract;
		this.rockCutOff = rockCutOff;
		this.minAttract = minAttract;
		this.otherWeight = otherWeight;

		bases = new Point[numPlayers];
		genMap();
	}

	/**
	 * returns the x co-ordinate of a point given in polar form
	 * 
	 * @param angle
	 * @param len
	 * @return
	 */
	private double getX(double angle, double len) {
		return Math.cos(angle) * len;
	}

	/**
	 * returns the y co-ordinate of a point given in polar form
	 * 
	 * @param angle
	 * @param len
	 * @return
	 */
	private double getY(double angle, double len) {
		return Math.sin(angle) * len;
	}

	private Point findPlace(double angle, int size, int baseOffset) {
		double min = 0.0;
		double max = size;
		double midX = size / 2.0;
		double midY = size / 2.0;
		while (min + 0.0001 < max) {
			double mid = (min + max) / 2.0;
			int newX = (int) (midX + getX(angle, mid));
			int newY = (int) (midY + getY(angle, mid));
			if (newX < baseOffset || newY < baseOffset || newX >= size - baseOffset || newY >= size - baseOffset) {
				max = mid;
			} else {
				min = mid;
			}
		}
		Point ret = new Point();
		ret.x = (int) (midX + getX(angle, min));
		ret.y = (int) (midY + getY(angle, min));
		return ret;
	}

	private void genTerrain() {
		double incAngle = 2.0 * Math.PI / (double) (numPlayers);
		double currAngle = Math.PI / 4.0;
		currentMap = new int[boardsize][boardsize];
		for (int i = 0; i < numPlayers; i++) {
			Point p = findPlace(currAngle, boardsize, baseOffset);
			bases[i] = new Point(p);
			currentMap[p.y][p.x] = -(i + 1);
			currAngle += incAngle;
		}
		// Then we generate a bit of the map, depending on what we need to do
		int walls, height, width;

		if (numPlayers % 4 == 0) {
			walls = NO_WALLS;
			height = (boardsize + 1) / 2;
			width = (boardsize + 1) / 2;
		} else if (numPlayers % 2 == 0) {
			walls = SIDE_WALLS;
			height = (boardsize + 1) / 2;
			width = boardsize;
		} else {
			walls = ALL_WALLS;
			height = boardsize;
			width = boardsize;
		}
		// First, assign a random number to every square representing the chance
		// for a obstacle to be there.
		Random rand = new Random(seed);
		double[][] chanceObs = new double[height][width];

		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				if (currentMap[i][j] >= 0) {
					if (i == 0 || j == 0 || (j == width - 1 && walls != NO_WALLS)
							|| (i == height - 1 && walls == ALL_WALLS)) {
						chanceObs[i][j] = wallAttract;
					} else {
						chanceObs[i][j] = rand.nextDouble();
					}
				} else {
					chanceObs[i][j] = baseAttract;
				}
			}
		}

		double[][] tempObs = new double[height][width];
		double total;
		double numAround;
		for (int n = 0; n < sharpness; n++) {
			for (int i = 0; i < height; i++) {
				for (int j = 0; j < width; j++) {
					numAround = 1.0;
					total = chanceObs[i][j];
					for (int k = 0; k < dx.length; k++) {
						int newX = j + dx[k];
						int newY = i + dy[k];
						if (newX >= 0 && newY >= 0 && newX < width && newY < height) {
							numAround++;
							total += chanceObs[newY][newX];
						}
					}
					tempObs[i][j] = total / numAround;
				}
			}
			for (int i = 0; i < height; i++) {
				for (int j = 0; j < width; j++) {
					chanceObs[i][j] = tempObs[i][j];
				}
			}
		}

		// Then, place walls down by a discrete cut off.
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				if (currentMap[i][j] >= 0) {
					if (i == 0 || j == 0 || (j == width - 1 && walls != NO_WALLS)
							|| (i == height - 1 && walls == ALL_WALLS)) {
						currentMap[i][j] = WALL;
					} else {
						if (chanceObs[i][j] >= rockCutOff) {
							currentMap[i][j] = WALL;
						} else {
							currentMap[i][j] = BLANK;
						}
					}
				}
			}
		}
		// Now, gen minerals, using the idea of mineral patches (areas with
		// minerals around them)
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				chanceObs[i][j] = rand.nextDouble() / otherWeight;
				if ((currentMap[i][j] < 0 && currentMap[i][j] != WALL)
						|| (walls == ALL_WALLS && i == height / 2 && j == width / 2)
						|| (walls == SIDE_WALLS && i == height - 1 && j == width / 2)
						|| (walls == NO_WALLS && i == height - 1 && j == width - 1)) {
					chanceObs[i][j] = minAttract + rand.nextDouble() - 0.5;
				}
			}
		}
		// Place some random patches elsewhere
		for (int i = 0; i < amoMin; i++) {
			int x = rand.nextInt(width);
			int y = rand.nextInt(height);
			chanceObs[y][x] = minAttract + rand.nextDouble() - 0.5;
		}
		// Now bleed
		for (int n = 0; n < sharpness; n++) {
			for (int i = 0; i < height; i++) {
				for (int j = 0; j < width; j++) {
					numAround = 1.0;
					total = chanceObs[i][j];
					for (int k = 0; k < dx.length; k++) {
						int newX = j + dx[k];
						int newY = i + dy[k];
						if (newX >= 0 && newY >= 0 && newX < width && newY < height) {
							numAround++;
							total += chanceObs[newY][newX];
						}
					}
					tempObs[i][j] = total / numAround;
				}
			}
			for (int i = 0; i < height; i++) {
				for (int j = 0; j < width; j++) {
					chanceObs[i][j] = tempObs[i][j];
				}
			}
		}
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				if (currentMap[i][j] == BLANK) {
					for (int k = 0; k < 5; k++) {
						if (rand.nextDouble() < chanceObs[i][j]) {
							currentMap[i][j] += amoPerMin / 5;
						}
					}
				}
			}
		}
		// Lastly, we copy everything back
		if (numPlayers % 4 == 0) {
			for (int i = 0; i < (boardsize + 1) / 2; i++) {
				for (int j = boardsize / 2; j < boardsize; j++) {
					if (!((currentMap[i][j] < 0 && currentMap[i][j] != WALL) || (currentMap[((boardsize + 1) / 2)
							- (j - (boardsize / 2)) - 1][i] < 0 && currentMap[((boardsize + 1) / 2)
							- (j - (boardsize / 2)) - 1][i] != WALL))) {
						currentMap[i][j] = currentMap[((boardsize + 1) / 2) - (j - (boardsize / 2)) - 1][i];
					}
				}
			}
		}
		if (numPlayers % 2 == 0) {
			for (int i = boardsize / 2; i < boardsize; i++) {
				for (int j = 0; j < boardsize; j++) {
					if (!((currentMap[i][j] < 0 && currentMap[i][j] != WALL) || (currentMap[boardsize - i - 1][boardsize
							- j - 1] < 0 && currentMap[boardsize - i - 1][boardsize - j - 1] != WALL))) {
						currentMap[i][j] = currentMap[boardsize - i - 1][boardsize - j - 1];
					}
				}
			}
		}
	}

	/**
	 * dfs over the map
	 * 
	 * @param x
	 *            the x to start dfs-ing from
	 * @param y
	 *            the y to start dfs-ing from
	 * @param seen
	 *            the seen array to adhere to
	 * @return
	 */
	private int dfs(int x, int y, boolean seen[][]) {
		// TODO: lukeha Rewrite as dfs.
		seen[y][x] = true;
		int count = 0;
		if (currentMap[y][x] < 0 && currentMap[y][x] != WALL) {
			count++;
		}
		for (int i = 0; i < 4; i++) {
			int newX = x + dx[i];
			int newY = y + dy[i];
			if (newX >= 0 && newY >= 0 && newX < boardsize && newY < boardsize) {
				if (!seen[newY][newX]) {
					if (currentMap[newY][newX] != WALL) {
						count += dfs(newX, newY, seen);
					}
				}
			}
		}
		return count;
	}

	/**
	 * gets a map where the bases are _not_ blocked off
	 * 
	 * @return the map that has been generated
	 */
	private void genMap() {
		boolean[][] seen = new boolean[boardsize][boardsize];
		long orS = seed;
		do {
			if (orS == 0) {
				seed = System.currentTimeMillis();
			} else {
				seed = orS;
				orS = 0;
			}
			genTerrain();
			for (int i = 0; i < boardsize; i++) {
				for (int j = 0; j < boardsize; j++) {
					seen[i][j] = false;
				}
			}
		} while (boardsize > baseOffset && dfs(baseOffset, baseOffset, seen) < numPlayers);
	}

	/**
	 * returns the location of a player's base
	 * 
	 * @param pid
	 *            the player id of the player
	 * @return
	 */
	public Point getBase(int pid) {
		return bases[pid];
	}

	/**
	 * returns the terrain at the a cell location
	 */
	public int getTerrain(int x, int y) {
		return currentMap[y][x];
	}

	/**
	 * set the terrain at a cell location
	 * 
	 * @param x
	 *            the x co-ordinate of the cell
	 * @param y
	 *            the y co-ordinate of the cell
	 * @param type
	 *            the type to set the cell to
	 */
	public void setTerrain(int x, int y, int type) {
		currentMap[y][x] = type;
	}

	public int getSize() {
		return boardsize;
	}
}
