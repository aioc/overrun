//
//  This example creates whatever it can make and then moves them around randomly.
//

#include <stdio.h>
#include <stdlib.h>
#include <time.h>

#include "overrun.h"



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
player players[MAX_PLAYERS];

void clientInit(int playerCount, int boardSize, int playerID) {
	for (int i = 0; i < MAX_PLAYERS; i++) {
		players[i].pid = -1;
	}
	myID = playerID;
}

void clientDoTurn() {
	// for each possible player
	for (int i = 0; i < MAX_PLAYERS; i++) {
		// if it is ours
		if (players[i].pid == myID) {
			// try to move it randomly
			// might want to add check to see if the given unit is dead or not
			move(i, rand() % 4);
		}
	}
	if (resources != 0) {
		build(resources);
	}
	numPlayers = 0;
	for (int i = 0; i < MAX_PLAYERS; i++) {
		players[i].pid = -1;
	}
}

void clientTerrainInfo(int x, int y, int type) {
	if (type > 0){
		blah[x][y] = 1;
	} else {
		blah[x][y] = 0;
	}
}

void clientJuiceInfo(int pid, int minerals) {
	if (pid == myID) {
		resources = minerals;
	}
}

void clientStudentLocation(int pid, int id, int x, int y, int level) {
	// If this is about our own player
	if (pid == myID) {
		players[id].pid = pid;
		players[id].x = x;
		players[id].y = y;
		players[id].level = level;
	}
}

