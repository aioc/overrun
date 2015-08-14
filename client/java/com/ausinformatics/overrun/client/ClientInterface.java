package com.ausinformatics.overrun.client;

public interface ClientInterface {
    public void clientRegister();
    public void clientInit(int playerCount, int boardSize, int playerId);
    public void clientMoneyInfo(int pid, int moneyCount);
    public void clientTerrainInfo(int x, int y, int type);
    public void clientDroneLocation(int pid, int id, int x, int y, int numCans);
    public void clientDoTurn();
}
