#include <stdio.h>
#include <stdlib.h>
#include <time.h>

#include "valley.h"

#define TRUE         1
#define FALSE        0


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

struct point {
   int x, y;
};

int amountOfPlayers;
int size;

int myID;

int playersMinerals[MAX_PLAYERS];
int terrain[MAX_SIZE][MAX_SIZE];

struct unitOnSquare whatOnSquare[MAX_SIZE][MAX_SIZE];
struct unitOnSquare preOnSquare[MAX_SIZE][MAX_SIZE];

int amoStill;
struct unit stillUnits[MAX_SIZE * MAX_SIZE];


// Number of units in the game. We reset this after every turn
int numUnits;
struct unit allUnits[MAX_SIZE * MAX_SIZE];

int amoMine;
int amoOthers;


int upTo;
int seen[MAX_SIZE][MAX_SIZE];

int preMove[MAX_SIZE][MAX_SIZE];

int dx[] = {0, 1, 0, -1};
int dy[] = {-1, 0, 1, 0};

int startQ, endQ;
struct point queue[MAX_SIZE * MAX_SIZE];

int findDir (int sX, int sY, int tX, int tY, int levelAl) {
   startQ = 0;
   endQ = 1;
   queue[0].y = tY;
   queue[0].x = tX;
   upTo++;
   int i;
   while (startQ < endQ && !(queue[startQ].x == sX && queue[startQ].y == sY)) {
      struct point cur = queue[startQ];
      startQ++;
      for (i = 0; i < 4; i++) {
         struct point newP;
         newP.x = cur.x + dx[i];
         newP.y = cur.y + dy[i];
         if (newP.x >= 0 && newP.y >= 0 && newP.x < size && newP.y < size) {
            if (terrain[newP.y][newP.x] != WALL && seen[newP.y][newP.x] < upTo) {
               if (((whatOnSquare[newP.y][newP.x].ownerID != myID || whatOnSquare[newP.y][newP.x].level == -1) && whatOnSquare[newP.y][newP.x].level <= levelAl) || (newP.y == sY && newP.x == sX)) {
                  seen[newP.y][newP.x] = upTo;
                  queue[endQ] = newP;
                  endQ++;
                  preMove[newP.y][newP.x] = i;
               }
            }
         }
      }
   }
   if (startQ == endQ) {
      return -1;
   } else {
      return (preMove[sY][sX] + 2) % 4;
   }
}

struct point findClosestMineral (int sX, int sY) {
   startQ = 0;
   endQ = 1;
   queue[0].y = sY;
   queue[0].x = sX;
   upTo++;
   int i;
   while (startQ < endQ && terrain[queue[startQ].y][queue[startQ].x] <= 0) {
      struct point cur = queue[startQ];
      startQ++;
      for (i = 0; i < 4; i++) {
         struct point newP;
         newP.x = cur.x + dx[i];
         newP.y = cur.y + dy[i];
         if (newP.x >= 0 && newP.y >= 0 && newP.x < size && newP.y < size) {
            if (terrain[newP.y][newP.x] != WALL && seen[newP.y][newP.x] < upTo) {
               if (whatOnSquare[newP.y][newP.x].level <= 0) {
                  seen[newP.y][newP.x] = upTo;
                  queue[endQ] = newP;
                  endQ++;
               }
            }
         }
      }
   }
   struct point ret;
   if (startQ == endQ) {
      ret.x = -1;
      ret.y = -1;
      return ret;
   } else {
      ret.x = queue[startQ].x;
      ret.y = queue[startQ].y;
      return ret;
   }
}

struct point findClosestBelow (int sX, int sY, int le) {
   startQ = 0;
   endQ = 1;
   queue[0].y = sY;
   queue[0].x = sX;
   upTo++;
   int i;
   while (startQ < endQ && (whatOnSquare[queue[startQ].y][queue[startQ].x].level <= 0 || startQ == 0)) {
      struct point cur = queue[startQ];
      startQ++;
      for (i = 0; i < 4; i++) {
         struct point newP;
         newP.x = cur.x + dx[i];
         newP.y = cur.y + dy[i];
         if (newP.x >= 0 && newP.y >= 0 && newP.x < size && newP.y < size) {
            if (terrain[newP.y][newP.x] != WALL && seen[newP.y][newP.x] < upTo) {
               if (whatOnSquare[newP.y][newP.x].level <= le && (whatOnSquare[newP.y][newP.x].ownerID != myID || whatOnSquare[newP.y][newP.x].level == -1)) {
                  seen[newP.y][newP.x] = upTo;
                  queue[endQ] = newP;
                  endQ++;
               }
            }
         }
      }
   }
   struct point ret;
   if (startQ == endQ) {
      ret.x = -1;
      ret.y = -1;
      return ret;
   } else {
      ret.x = queue[startQ].x;
      ret.y = queue[startQ].y;
      return ret;
   }

}

// AI logic will mostly go here.
void clientDoTurn() {
   int i, j;
   int nextAmoStill = 0;
   int amoMining = 0;
   for (i = 0; i < amoMine; i++) {
      if (allUnits[i].ownerID == myID) {
         numUnits++;
         int assigned = FALSE;
         struct point p;
         // Drone!
         if (terrain[allUnits[i].y][allUnits[i].x] > 0 && allUnits[i].level <= 2) {
            move (allUnits[i].unitID, EXTRACT);
            amoMining++;
         } else {
            if (allUnits[i].level <= 2) {
               p = findClosestMineral (allUnits[i].x, allUnits[i].y);
               if (p.x != -1) {
                  // We found somewhere to go
                  move (allUnits[i].unitID, findDir (allUnits[i].x, allUnits[i].y, p.x, p.y, 0));
                  assigned = TRUE;
               }
            }
            if (!assigned) {
               // Time to attack!
               // Choose someone sitting still :)
               int d = -1;
               while (nextAmoStill < amoStill && d == -1) {
                  d = findDir (allUnits[i].x, allUnits[i].y, stillUnits[nextAmoStill].x, stillUnits[nextAmoStill].y, stillUnits[nextAmoStill].level);
                  nextAmoStill++;
               }
               if (d != -1) {
                  move (allUnits[i].unitID, d);
               } else {
                  p = findClosestBelow (allUnits[i].x, allUnits[i].y, allUnits[i].level);
                  if (p.x != -1) {
                     move (allUnits[i].unitID, findDir (allUnits[i].x, allUnits[i].y, p.x, p.y, allUnits[i].level));
                  } else {
                     // Suicide!
                     p = findClosestBelow (allUnits[i].x, allUnits[i].y, 9001);
                     if (p.x == -1) {
                        // Make random move
                        move (allUnits[i].unitID, rand () % 4);
                     } else {
                        move (allUnits[i].unitID, findDir (allUnits[i].x, allUnits[i].y, p.x, p.y, 9001));
                     }
                  }
               }
            }
         }
      }
   }
   if ((amoMining < 5 || amoMine > amoOthers - 5) && (playersMinerals[myID] < 20)) {
      build (getCost (2));
   } else {
      build (playersMinerals[myID]);
   }

   for (i = 0; i < size; i++) {
      for (j = 0; j < size; j++) {
         preOnSquare[i][j] = whatOnSquare[i][j];
         whatOnSquare[i][j].level = -1;
      }
   }
   amoStill = 0;
   amoMine = 0;
   amoOthers = 0;
}


void clientRegister() {
   // We need to call setName ()
   setName ("hal");

   // We also want to seed the random generator
   srand (time (NULL));
}

// All of these function simply get data into our AI
void clientInit(int playerCount, int boardSize, int playerID) {
   amountOfPlayers = playerCount;
   size = boardSize;
   int i, j;
   for (i = 0; i < size; i++) {
      for (j = 0; j < size; j++) {
         seen[i][j] = 0;
         whatOnSquare[i][j].level = -1;
      }
   }
   myID = playerID;
   upTo = 2;
   amoMine = 0;
   amoOthers = 0;
}

void clientMoneyInfo(int pid, int juiceCount) {
   playersMinerals[pid] = juiceCount;
}

void clientTerrainInfo(int x, int y, int type) {
   terrain[y][x] = type;
}

void clientDroneLocation(int pid, int id, int x, int y, int level) {
      if (preOnSquare[y][x].unitID == id && preOnSquare[y][x].ownerID == pid && preOnSquare[y][x].level == level && pid != myID) {
         stillUnits[amoStill].x = x;
         stillUnits[amoStill].y = y;
         amoStill++;
      }
      whatOnSquare[y][x].ownerID = pid;
      whatOnSquare[y][x].unitID = id;
      whatOnSquare[y][x].level = level;

      if (pid == myID) {
         allUnits[amoMine].ownerID = pid;
         allUnits[amoMine].unitID = id;
         allUnits[amoMine].x = x;
         allUnits[amoMine].y = y;
         allUnits[amoMine].level = level;
         amoMine++;
      } else {
         amoOthers++;
      }
}



