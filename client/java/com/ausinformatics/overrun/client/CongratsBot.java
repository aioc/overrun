package com.ausinformatics.overrun.client;

import java.util.Random;

/*
 *  This example creates whatever it can make and moves around randomly, with
 *  two exceptions.  The first is that if any unit is on a resource patch, it
 *  will acquire the resources. Also, if it is next to an enemy unit it will
 *  attack them.
 */
class CongratsBot implements ClientLibrary.ClientInterface {
    public class unit {
        int ownerId;
        int unitId;
        int x;
        int y;
        int level;

        public unit() {
            ownerId = -1;
            unitId = -1;
            x = -1;
            y = -1;
            level = 0;
        }
    }

    public class unitOnSquare {
        int ownerId;
        int unitId;
        int level; // A level 0 unit means one does not exist on that square

        public unitOnSquare() {
            ownerId = -1;
            unitId = -1;
            level = 0;
        }
    }

    int myId;
    int amountOfPlayers;
    int size;
    int[] playersMinerals;
    int[][] terrain;

    unitOnSquare[][] whatOnSquare;

    // Number of units in the game. We reset this after every turn
    int numUnits = 0;
    unit allUnits[];

    final int[] dx = {0, 1, 0, -1};
    final int[] dy = {-1, 0, 1, 0};

    ClientLibrary library = null;
    Random rand;


    @Override
    public void clientRegister() {
        // Grab a copy of the current ClientLibrary instance.
        if (library == null)
            library = ClientLibrary.getInstance();

        // Go ahead and set our name and colour.
        library.setName("CongratsBot");
        library.setColour(255, 20, 147);

        rand = new Random();
    }

    @Override
    public void clientInit(int playerCount, int boardSize, int playerId) {
        amountOfPlayers = playerCount;
        size = boardSize;
        myId = playerId;
        numUnits = 0;

        // playerIds are 1-indexed
        playersMinerals = new int[amountOfPlayers + 1];
        terrain = new int[size][size];
        whatOnSquare = new unitOnSquare[size][size];
        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++)
                whatOnSquare[i][j] = new unitOnSquare();

        // There can never be more units than there are squares on the
        // board....
        // I would describe this as the 'elegant' solution.
        allUnits = new unit[Constants.MAX_SIZE * Constants.MAX_SIZE];
        for (int i = 0; i < Constants.MAX_SIZE * Constants.MAX_SIZE; i++)
            allUnits[i] = new unit();
    }

    @Override
    public void clientMoneyInfo(int pid, int moneyCount) {
        playersMinerals[pid] = moneyCount;
    }

    @Override
    public void clientTerrainInfo(int x, int y, int type) {
        terrain[y][x] = type;
    }

    @Override
    public void clientDroneLocation(int pid, int id, int x, int y, int numCans) {
        allUnits[numUnits].ownerId = pid;
        allUnits[numUnits].unitId = id;
        allUnits[numUnits].x = x;
        allUnits[numUnits].y = y;
        allUnits[numUnits].level = numCans;

        whatOnSquare[y][x].ownerId = pid;
        whatOnSquare[y][x].unitId = id;
        whatOnSquare[y][x].level = numCans;

        numUnits++;
    }

    // AI logic will mostly go here.
    @Override
    public void clientDoTurn() {
        for (int i = 0; i < numUnits; i++) {
            if (allUnits[i].ownerId == myId) {
                // We will always try and move one of ours. First, check to see
                // if it is on a mineral patch

                // A value > 0 means a mineral patch
                if (terrain[allUnits[i].y][allUnits[i].x] > 0) {
                    // We want to mine resources
                    System.out.printf("Mine all the things, %d %d, %d %d\n",
                            allUnits[i].y, allUnits[i].x, allUnits[i].unitId,
                            allUnits[i].level);
                    library.move(allUnits[i].unitId, Constants.EXTRACT);
                } else {
                    // Otherwise, we check to see whether a unit which isn't
                    // ours is around us.
                    int direction = -1;
                    for (int j = 0; j < 4 && direction == -1; j++) {
                        int newX = allUnits[i].x + dx[j];
                        int newY = allUnits[i].y + dy[j];
                        if (newX >= 0 && newY >= 0 && newX < size && newY < size) {
                            if (whatOnSquare[newY][newX].level > 0) {
                                if (whatOnSquare[newY][newX].ownerId != myId) {
                                    direction = j;
                                }
                            }
                        }
                    }
                    if (direction == -1) { // Didn't find a unit, so randomly move
                        library.move(allUnits[i].unitId, rand.nextInt(4));
                    } else { // Hunt the unit down
                        library.move(allUnits[i].unitId, direction);
                    }
                }
            }
        }
        // Finally, build a unit with all the minerals we have, assuming we
        // have stuff. This doesn't consider what we've mined this turn because
        // it's bad.
        if (playersMinerals[myId] > 0) {
            library.build(playersMinerals[myId]);
        }

        // Echo to the terminal what we think the current game state is.
        // We clamp things to alphabetical ranges as a quick indicator of the
        // unit's strength.
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (whatOnSquare[i][j].level > 0) {
                    if (whatOnSquare[i][j].level > 26) {
                        whatOnSquare[i][j].level = 26;
                    }
                    if (whatOnSquare[i][j].ownerId == myId) {
                        System.out.printf("%c", 'A' + whatOnSquare[i][j].level - 1);
                    } else {
                        System.out.printf("%c", 'a' + whatOnSquare[i][j].level - 1);
                    }
                } else {
                    if (terrain[i][j] > 0) {
                        System.out.printf("%d", (terrain[i][j] + 9) / 10);
                    } else if (terrain[i][j] == Constants.WALL) {
                        System.out.printf("#");
                    } else if (terrain[i][j] < 0) {
                        System.out.printf("%d", -terrain[i][j]);
                    } else {
                        System.out.printf(" ");
                    }
                }
            }
            System.out.printf("\n");
        }
        

        // Reset the data we stored since we'll be given a fresh copy next
        // round
        numUnits = 0;
        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++)
                whatOnSquare[i][j] = new unitOnSquare();
    }
}
