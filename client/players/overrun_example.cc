//
//  This example creates whatever it can make and then moves them around randomly.
//

#include <stdio.h>
#include <stdlib.h>
#include <time.h>

#include "valley.h"



void clientRegister(void) {
  setName("Dumbo");
}

struct player {
	int pid;
	int x, y;
	int level;
};

int resources;

int blah[100][100];

int myID;

int numPlayers;
player players[MAX_PLAYER_ID];

void clientInit(int playerCount, int boardSize, int playerID) {
	for (int i = 0; i < MAX_PLAYER_ID; i++) {
		players[i].pid = -1;
	}
	myID = playerID;
}

void clientDoTurn() {
	// for each possible player
	for (int i = 0; i < MAX_PLAYER_ID; i++) {
		// if it is ours
		if (players[i].pid == myID) {
			// try to move it randomly
			// might want to add check to see if the given unit is dead or not
			move(i, rand() % 5);
		}
	}
	if (resources != 0) {
		build(resources);
	}
	numPlayers = 0;
	for (int i = 0; i < MAX_PLAYER_ID; i++) {
		players[i].pid = -1;
	}
}

void clientTerrainInfo(int x, int y, int type) {
	//printf ("Told about info: %d %d %d\n", x, y, type);
	if (type > 0){
		blah[x][y] = 1;
	} else {
		blah[x][y] = 0;
	}
}

void clientMoneyInfo(int pid, int minerals) {
	//printf ("Told about juice: %d %d\n", pid, minerals);
	if (pid == myID) {
		resources = minerals;
	}
}

void clientDroneLocation(int pid, int id, int x, int y, int level) {
	//printf ("Told about unit: %d %d %d %d %d\n", pid, id, y, x, level);
	// If this is about our own player
	if (pid == myID) {
		players[id].pid = pid;
		players[id].x = x;
		players[id].y = y;
		players[id].level = level;
	}
}

