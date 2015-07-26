/*
 *  This example creates whatever it can make and moves around randomly, with two exceptions.
 *  The first is that if any unit is on a resource patch, it will get them. Also, if it is next
 *  to an enemey unit it will attack them.
 */

#include <stdio.h>
#include <stdlib.h>
#include <time.h>

#include "overrun.h"

struct unit {
   int ownerID;
   int unitID;
   int x;
   int y;
   int level;
};

struct unitOnSquare {
   int ownerID;
   int unitID;
   int level; // A level 0 unit means one does not exist on that square.
};

int myID;

int amountOfPlayers;
int size;

int playersMinerals[MAX_PLAYERS];
int terrain[MAX_SIZE][MAX_SIZE];

struct unitOnSquare whatOnSquare[MAX_SIZE][MAX_SIZE];



// Number of units in the game. We reset this after every turn
int numUnits;
struct unit allUnits[MAX_SIZE * MAX_SIZE];


int dx[] = {0, 1, 0, -1};
int dy[] = {-1, 0, 1, 0};

// AI logic will mostly go here.
void clientDoTurn() {
   int i, j;
   for (i = 0; i < numUnits; i++) {
      if (allUnits[i].ownerID == myID) {
         // We will always try and move one of ours. First, check to see if it is on a mineral patch
         if (terrain[allUnits[i].y][allUnits[i].x] > 0) { // A value > 0 means a mineral patch
            // We want to mine
            printf ("Mine all the things, %d %d, %d %d\n", allUnits[i].y, allUnits[i].x, allUnits[i].unitID, allUnits[i].level);
            move (allUnits[i].unitID, EXTRACT);
         } else {
            // Next, we check to see whether a unit which isn't ours is around us.
            int direction = -1; // -1 means we haven't see another unit
            for (j = 0; j < 4 && direction == -1; j++) {
               int newX = allUnits[i].x + dx[j];
               int newY = allUnits[i].y + dy[j];
               if (newX >= 0 && newY >= 0 && newX < size && newY < size) {
                  if (whatOnSquare[newY][newX].level > 0) {
                     if (whatOnSquare[newY][newX].ownerID != myID) {
                        direction = j;
                     }
                  }
               }
            }
            if (direction == -1) { // We haven't found a unit, so randomly move
               move (allUnits[i].unitID, rand () % 4);
            } else {
               // We have found a unit, so move in that direction
               move (allUnits[i].unitID, direction);
            }
         }
      }
   }
   // Finally, build a unit with all the minerals we have, assuming we have stuff
   if (playersMinerals[myID] > 0) {
      build (playersMinerals[myID]);
   }

   for (i = 0; i < size; i++) {
      for (j = 0; j < size; j++) {
         if (whatOnSquare[i][j].level > 0) {
            if (whatOnSquare[i][j].level > 26) {
               whatOnSquare[i][j].level = 26;
            }
            if (whatOnSquare[i][j].ownerID == myID) {
               printf ("%c", 'A' + whatOnSquare[i][j].level - 1);
            } else {
               printf ("%c", 'a' + whatOnSquare[i][j].level - 1);
            }
         } else {
            if (terrain[i][j] > 0) {
               printf ("%d", (terrain[i][j] + 9) / 10);
            } else if (terrain[i][j] == WALL) {
               printf ("#");
            } else if (terrain[i][j] < 0) {
               printf ("%d", -terrain[i][j]);
            } else {
               printf (" ");
            }
         }
      }
      printf ("\n");
   }

   // We now reset the data we have stored to get the new data next round.
   for (i = 0; i < size; i++) {
      for (j = 0; j < size; j++) {
         whatOnSquare[i][j].level = 0; // We erase eveything set on squares
      }
   }
   numUnits = 0; // This will erase all the units we stored in allUnits.
}



void clientRegister() {
   // We need to call setName ()
   setName ("Dumbo2000");

   // We also want to seed the random generator
   srand (time (NULL));
}

// All of these function simply get data into our AI
void clientInit(int playerCount, int boardSize, int playerID) {
   amountOfPlayers = playerCount;
   size = boardSize;
   myID = playerID;
   numUnits = 0;
}

void clientJuiceInfo(int pid, int juiceCount) {
   playersMinerals[pid] = juiceCount;
}

void clientTerrainInfo(int x, int y, int type) {
   terrain[y][x] = type;
}

void clientStudentLocation(int pid, int id, int x, int y, int level) {
   allUnits[numUnits].ownerID = pid;
   allUnits[numUnits].unitID = id;
   allUnits[numUnits].x = x;
   allUnits[numUnits].y = y;
   allUnits[numUnits].level = level;
   whatOnSquare[y][x].ownerID = pid;
   whatOnSquare[y][x].unitID = id;
   whatOnSquare[y][x].level = level;
   numUnits++;
}

