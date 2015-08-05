package com.ausinformatics.overrun.core;

import java.util.ArrayList;
import java.util.List;

import com.ausinformatics.client.GameClient;
import com.ausinformatics.client.GameClient.GameMove;
import com.ausinformatics.phais.server.server.ClientConnection;
import com.ausinformatics.phais.server.server.DisconnectedException;

public class PlayerMove {

    public List<UnitMove> unitMoves;
    public int buildCost;

    public PlayerMove() {
        buildCost = 0;
        unitMoves = new ArrayList<UnitMove>();
    }

    public void addUnitMove(UnitMove move) {
        unitMoves.add(move);
    }

    public static PlayerMove readPlayerMove(ClientConnection c) throws DisconnectedException {
        GameMove.Builder proto = GameMove.newBuilder();
        c.recvMessage(proto);

        PlayerMove move = new PlayerMove();
        move.buildCost = proto.getBuild();
        for (GameClient.UnitMove um : proto.getMoveList()) {
            move.addUnitMove(new UnitMove(um.getUnitId(), um.getAction().getNumber()));
        }

        return move;
    }
}
