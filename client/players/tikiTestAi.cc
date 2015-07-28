#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <queue>
#include <algorithm>
using namespace std;
#include "valley.h"

// general idea: the object of the game is to mine the shit out of everything
// and outlive the other player (in this AI's mind).
// always spend whatever you have.

#define CHANCE_DENOM 12
#define CHANCE_NUMER 1
#define sqr(x) ((x)*(x))

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
	int level; // INV: >= 1
};

int nPlayers;
int size;
int baseX, baseY;

int playersMinerals[MAX_PLAYERS];
int terrain[MAX_SIZE][MAX_SIZE];

struct unitOnSquare whatOnSquare[MAX_SIZE][MAX_SIZE];

// Number of units in the game. We reset this after every turn
int numUnits;
int MY_ID;
struct unit allUnits[MAX_SIZE * MAX_SIZE];

int dx[] = {0, 1, 0, -1};
int dy[] = {-1, 0, 1, 0};

int distFromBase[MAX_SIZE][MAX_SIZE];
int distFromBaseBT[MAX_SIZE][MAX_SIZE];

int friendlyAt[MAX_SIZE][MAX_SIZE];
int enemyAt[MAX_SIZE][MAX_SIZE];

int nMineralPatchesLeft, nMyUnits, nEnemyUnits;

void resetData();
int mineralFindingAction(int sy, int sx, int maxd);
void dfsFrom(int result[MAX_SIZE][MAX_SIZE], int bt[MAX_SIZE][MAX_SIZE], int sy, int sx);
void calculateDistFromBase();

// AI logic will mostly go here.
void clientDoTurn() {
	calculateDistFromBase();
	int threatLevel = 0;
	int i, j;
	nMineralPatchesLeft = 0;
	nMyUnits = 0;
	nEnemyUnits = 0;
	bool movedYet[numUnits];
	for (i = 0; i < size; i++) {
	for (j = 0; j < size; j++) {
		friendlyAt[i][j] = 0;
		if (terrain[i][j] > 0) nMineralPatchesLeft ++;
	}
	}
	for (i = 0; i < numUnits; i++) {
		movedYet[i] = 0;
		if (allUnits[i].ownerID == MY_ID) {
			friendlyAt[allUnits[i].y][allUnits[i].x] = 1;
			nMyUnits++;
		} else {
			enemyAt[allUnits[i].y][allUnits[i].x] = 1;
			nEnemyUnits++;
			threatLevel +=
					(15*size*size*sqr(allUnits[i].level+1))/
					sqr(distFromBase[allUnits[i].y][allUnits[i].x]+size);
		}
	}
	const int N_DISTS = 6;
	for (int k = 0; k < N_DISTS; k++) {
		int dists[N_DISTS] = {5,8,15,20,30,size*size};
	for (i = 0; i < numUnits; i++) {
		if (!movedYet[i] && allUnits[i].ownerID == MY_ID) {
			friendlyAt[allUnits[i].y][allUnits[i].x] = 0;
			int wantedMove = mineralFindingAction(allUnits[i].y,allUnits[i].x,dists[k]);
			friendlyAt[allUnits[i].y][allUnits[i].x] = 1;
			if (wantedMove == -1) {
				if (k == N_DISTS-1) {
					move(allUnits[i].unitID,rand()%4);
					printf("!!\n");
				}
			} else {
				move (allUnits[i].unitID,wantedMove);
				printf("%d?\n",k);
				movedYet[i] = 1;
			}
		}
	}
	}
	int minCostToBuild = max(getCost(1),getCost(threatLevel/4)-12);
	int maxCostToBuild = getCost(threatLevel)+2;
	printf("%3d %3d %4d\n",minCostToBuild,maxCostToBuild,threatLevel);
	int buildAmt = min(maxCostToBuild,playersMinerals[MY_ID]*3/4);
	if (minCostToBuild <= buildAmt) {
		build(buildAmt);
	} else {
	}
	printf("%3d\n",nMyUnits-nEnemyUnits);

	resetData();
}


void resetData() {
	// We now reset the data we have stored to get the new data next round.
	for (int i = 0; i < size; i++) {
		for (int j = 0; j < size; j++) {
			whatOnSquare[i][j].level = 0; // We erase eveything set on squares
		}
	}
	numUnits = 0;
	// This will erase all the units we stored in allUnits, ready for next turn
}

void clientRegister() {
	setName ("SHODAN");
}

// All of these function simply get data into our AI
void clientInit(int playerCount, int boardSize, int pid) {
	nPlayers = playerCount;
	size = boardSize;
	numUnits = 0;
	MY_ID = pid;
}

void clientMoneyInfo(int pid, int juiceCount) {
	playersMinerals[pid] = juiceCount;
}

void clientTerrainInfo(int x, int y, int type) {
	terrain[y][x] = type;
	if (type == -MY_ID) {
		baseX = x;
		baseY = y;
	}
}

void clientDroneLocation(int pid, int id, int x, int y, int level) {
	if (level > 0) { // Anything that is level 0 or below is dead, so we don't worry about it.
		whatOnSquare[y][x].ownerID = pid;
		whatOnSquare[y][x].unitID = id;
		whatOnSquare[y][x].level = level;
		allUnits[numUnits].ownerID = pid;
		allUnits[numUnits].unitID = id;
		allUnits[numUnits].x = x;
		allUnits[numUnits].y = y;
		numUnits++;
	}
}

void dfsFrom(int result[MAX_SIZE][MAX_SIZE], int bt[MAX_SIZE][MAX_SIZE], int sy, int sx) {
	bool seen[MAX_SIZE][MAX_SIZE];
	for (int i = 0; i < size; i++) {
		for (int j = 0; j < size; j++) {
			seen[i][j] = 0;
			result[i][j] = -1;
			if (bt != NULL) bt[i][j] = -1;
		}
	}
	queue<pair<int,int> > q;
	q.push(make_pair(sy,sx));
	seen[sy][sx] = 1;
	result[sy][sx] = 0;
	while (!q.empty()) {
		int y = q.front().first;
		int x = q.front().second;
		int d = result[y][x];
		q.pop();
		for (int k = 0; k < 4; k++) {
			int nx = x + dx[k];
			int ny = y + dy[k];
			if (nx < 0 || nx >= size || ny < 0 || ny >= size) {
				continue;
			}
			if (seen[ny][nx]) continue;
			if (terrain[ny][nx] == WALL) continue;
			seen[ny][nx] = 1;
			q.push(make_pair(ny,nx));
			result[ny][nx] = d+1;
			// bt contains the *last* move taken to get there
			if (bt != NULL) {
				if (x==sx && y==sy) {
					bt[ny][nx] = k;
				} else {
					bt[ny][nx] = bt[y][x];
				}
			}
		}
	}
}

int mineralFindingAction(int sy, int sx, int maxd) {
	bool seen[MAX_SIZE][MAX_SIZE];
	int bt[MAX_SIZE][MAX_SIZE];
	int d[MAX_SIZE][MAX_SIZE];
	for (int i = 0; i < size; i++) {
		for (int j = 0; j < size; j++) {
			seen[i][j] = 0;
			d[i][j] = 0;
			if (bt != NULL) bt[i][j] = -1;
		}
	}
	queue<pair<int,int> > q;
	q.push(make_pair(sy,sx));
	seen[sy][sx] = 1;
	bt[sy][sx] = EXTRACT;
	d[sy][sx] = 0;
	while (!q.empty()) {
		int y = q.front().first;
		int x = q.front().second;
		q.pop();
		if ((terrain[y][x] > 0 || (nMineralPatchesLeft < nMyUnits && enemyAt[y][x])) && !friendlyAt[y][x]) {
			if (rand() % CHANCE_DENOM >= CHANCE_NUMER) {
				if (nMineralPatchesLeft > nMyUnits) friendlyAt[y][x] = 1;//stake as mine
				return bt[y][x];
			}
		}
		if (d[y][x] >= maxd) continue;
		for (int k = 0; k < 4; k++) {
			int nx = x + dx[k];
			int ny = y + dy[k];
			if (nx < 0 || nx >= size || ny < 0 || ny >= size) {
				continue;
			}
			if (seen[ny][nx]) continue;
			if (terrain[ny][nx] == WALL || friendlyAt[ny][nx]) continue;
			seen[ny][nx] = 1;
			d[ny][nx] = d[y][x] + 1;
			q.push(make_pair(ny,nx));
			// bt contains the *last* move taken to get there
			if (bt != NULL) {
				if (x==sx && y==sy) {
					bt[ny][nx] = k;
				} else {
					bt[ny][nx] = bt[y][x];
				}
			}
		}
	}
	return -1;
}

void calculateDistFromBase() {
	dfsFrom(distFromBase,distFromBaseBT,baseY,baseX);
}
