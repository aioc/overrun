#include <stdio.h>
#include <stdlib.h>
#include <time.h>

#include "overrun.h"

#define TRUE                     1
#define FALSE                    0

#define WORKER_LEVEL             2
#define HARRAS_LEVEL             50
#define BASE_KILLER              200
#define WORKER_PER_PATCH         0.07
#define SOLDIER_PER_UNIT         1.0
#define HARRASER_PER_WORKER      0.5

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

int amoMineralPatches;

int playersMinerals[MAX_PLAYERS];
int terrain[MAX_SIZE][MAX_SIZE];

struct unitOnSquare whatOnSquare[MAX_SIZE][MAX_SIZE];
struct unitOnSquare preOnSquare[MAX_SIZE][MAX_SIZE];

int amoStill;
struct unit stillUnits[MAX_SIZE * MAX_SIZE];


// Number of units in the game. We reset this after every turn
int numUnits;
struct unit allUnits[MAX_SIZE * MAX_SIZE];
int harrasses[MAX_SIZE * MAX_SIZE];


int amoMine;
int amoOthers;
int amoSplit;
int amoToSplit;


int amoWorkers;
int amoSoldiers;
int amoHarras;

int amoEnWork;
int amoEnSold;


int upTo;
int seen[MAX_SIZE][MAX_SIZE];

int preMove[MAX_SIZE][MAX_SIZE];

int dx[] = {0, 1, 0, -1};
int dy[] = {-1, 0, 1, 0};

int startQ, endQ;
struct point queue[MAX_SIZE * MAX_SIZE];


int findDir (int sX, int sY, int tX, int tY, int levelAl);

struct point findClosestMineral (int sX, int sY);

struct point findClosestBelow (int sX, int sY, int le);

struct point findClosestWorker (int sX, int sY);

int shouldMoveWorker (int sX, int sY);


int moveWorker (int num);
int moveSoldier (int num);
int moveHarass (int num);


void reset (void);






// AI logic will mostly go here.
void clientDoTurn() {
   int i, j;
   int nextAmoStill = 0;
   int amoMining = 0;
   for (i = 0; i < amoMine; i++) {
      if (allUnits[i].level <= WORKER_LEVEL) {
         move (allUnits[i].unitID, moveWorker (i));
      } else if (harrasses[allUnits[i].unitID]) {
         amoHarras++;
         move (allUnits[i].unitID, moveHarass (i));
      } else {
         move (allUnits[i].unitID, moveSoldier (i)); 
      }
   }
   int workerDif = (((int) (((double) (amoMineralPatches)) * WORKER_PER_PATCH)) - amoWorkers + amoSoldiers / 5);
   int soldierDif = (((int) (((double) (amoEnSold / (amountOfPlayers - 1))) * SOLDIER_PER_UNIT)) - amoSoldiers);
   int harrasDif = (((int) (((double) (amoEnWork / (amountOfPlayers - 1))) * HARRASER_PER_WORKER)) - amoHarras);

   printf ("%d %d %d\n", workerDif, soldierDif, harrasDif);
   if (workerDif > soldierDif && workerDif > harrasDif) {
      if (playersMinerals[myID] < WORKER_LEVEL) {
         build (playersMinerals[myID]);
      } else {
         build (getCost (WORKER_LEVEL));
      }
   } else if (soldierDif > harrasDif || playersMinerals[myID] < getCost (HARRAS_LEVEL)) {
      build (playersMinerals[myID] - 1);
   } else {
      build (getCost (HARRAS_LEVEL));
   }
   reset ();
}

int moveWorker (int num) {
   int away = shouldMoveWorker (allUnits[num].x, allUnits[num].y);
   if (away != -1 && amoSplit < amoToSplit) {
      amoSplit++;
      amoToSplit--;
      return away;
   } else  if (terrain[allUnits[num].y][allUnits[num].x] > 0) {
      return EXTRACT;
   } else {
      struct point p = findClosestMineral (allUnits[num].x, allUnits[num].y);
      if (p.x == -1) {
         return moveSoldier (num);
      } else {
         // We found somewhere to go
         return findDir (allUnits[num].x, allUnits[num].y, p.x, p.y, 0);
      }
   }
}

int moveSoldier (int num) {
   // Time to attack!
   struct point p = findClosestBelow (allUnits[num].x, allUnits[num].y, 9001);
   if (p.x != -1) {
      return findDir (allUnits[num].x, allUnits[num].y, p.x, p.y, 9001);
   } else {
      // Suicide!
      p = findClosestBelow (allUnits[num].x, allUnits[num].y, 9001);
      if (p.x == -1) {
         // Make random move
         return rand () % 4;
      } else {
         return findDir (allUnits[num].x, allUnits[num].y, p.x, p.y, 9001);
      }
   }
}

int moveHarass (int num) {
   struct point p = findClosestBelow (allUnits[num].x, allUnits[num].y, WORKER_LEVEL + 2);
   if (p.x == -1) {
      return moveSoldier (num);
   } else {
      return findDir (allUnits[num].x, allUnits[num].y, p.x, p.y, WORKER_LEVEL + 2);
   }
}


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
      int s = rand () % 4;
      for (i = 0; i < 4; i++) {
         int modI = (i + s) % 4;
         struct point newP;
         newP.x = cur.x + dx[modI];
         newP.y = cur.y + dy[modI];
         if (newP.x >= 0 && newP.y >= 0 && newP.x < size && newP.y < size) {
            if (terrain[newP.y][newP.x] != WALL && seen[newP.y][newP.x] < upTo) {
               if (((whatOnSquare[newP.y][newP.x].ownerID != myID || whatOnSquare[newP.y][newP.x].level == -1) && whatOnSquare[newP.y][newP.x].level <= levelAl) || (newP.y == sY && newP.x == sX)) {
                  seen[newP.y][newP.x] = upTo;
                  queue[endQ] = newP;
                  endQ++;
                  preMove[newP.y][newP.x] = modI;
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
      int s = rand () % 4;
      for (i = 0; i < 4; i++) {
         int modI = (i + s) % 4;
         struct point newP;
         newP.x = cur.x + dx[modI];
         newP.y = cur.y + dy[modI];
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
      int s = rand () % 4;
      for (i = 0; i < 4; i++) {
         int modI = (i + s) % 4;
         struct point newP;
         newP.x = cur.x + dx[modI];
         newP.y = cur.y + dy[modI];
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


struct point findClosestWorker (int sX, int sY) {

}

int shouldMoveWorker (int sX, int sY) {
   int i;
   int newY, newX; 
   int notFound = TRUE;
   int way;
   for (i = 0; i < 4 && notFound; i++) {
      newX = sX + dx[i];
      newY = sY + dy[i];
      if (newX >= 0 && newY >= 0 && newX < size && newY < size) {
         if (terrain[newY][newX] < 0 && terrain[newY][newX] != WALL) {
            notFound = FALSE;
            way = i;
         }
      }
   }
   if (notFound || terrain[sY][sX] <= 0) {
      return -1;
   }
   int countAr = 0;
   int x = newX;
   int y = newY;
   for (i = 0; i < 4; i++) {
      newX = x + dx[i];
      newY = y + dy[i];
      if (newX >= 0 && newY >= 0 && newX < size && newY < size) {
         if (whatOnSquare[newY][newX].level > 0) {
            countAr++;
         }
      } 
   }
   if (countAr < 4) {
      return -1;
   } else {
      for (i = 1; i < 4; i++) {
         int toMove = (way + i) % 4;
         newX = sX + dx[toMove];
         newY = sY + dy[toMove];
         if (terrain[newY][newX] >= 0 && whatOnSquare[newY][newX].level <= 0) {
            return toMove;
         }
      }
      return -1;
   }
}

void reset (void) {
   int i, j;
   for (i = 0; i < size; i++) {
      for (j = 0; j < size; j++) {
         preOnSquare[i][j] = whatOnSquare[i][j];
         whatOnSquare[i][j].level = -1;
      }
   }
   amoStill = 0;
   amoMine = 0;
   amoOthers = 0;
   amoWorkers = 0;
   amoSoldiers = 0;
   amoHarras = 0;
   amoSplit = 0;
   amoEnSold = 0;
   amoEnWork = 0;
   amoMineralPatches = 0;
}


void clientRegister() {
   // We need to call setName ()
   setName ("Glados");

   // We also want to seed the random generator
   srand (time (NULL));
}

// All of these function simply get data into our AI
void clientInit(int playerCount, int boardSize, int playerID) {
   amountOfPlayers = playerCount;
   size = boardSize;
   reset ();
   myID = playerID;
   amoToSplit = 10;
}

void clientJuiceInfo(int pid, int juiceCount) {
   playersMinerals[pid] = juiceCount;
}

void clientTerrainInfo(int x, int y, int type) {
   terrain[y][x] = type;
   if (type > 0) {
      amoMineralPatches++;
   }
}

void clientStudentLocation(int pid, int id, int x, int y, int level) {
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
      if (level <= WORKER_LEVEL) {
         amoWorkers++;
      } else if (level == HARRAS_LEVEL) {
         if (!harrasses[id]) {
            harrasses[id] = TRUE;
         }
      } else {
         amoSoldiers++;
      }
   } else {
      if (level <= WORKER_LEVEL) {
         amoEnWork++;
      } else {
         amoEnSold++;
      }
      amoOthers++;
   }
}


