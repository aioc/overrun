package com.ausinformatics.overrun;

import java.util.HashMap;
import java.util.Map;

import com.ausinformatics.phais.core.server.ClientConnection;
import com.ausinformatics.phais.core.server.DisconnectedException;

public class PlayerMove {

	private Map<Integer, UnitMove> unitMoves;
	private int buildLevel;

	public PlayerMove() {
		buildLevel = 0;
		unitMoves = new HashMap<Integer, UnitMove>();
	}

	public void addUnitMove(UnitMove move) {
		unitMoves.put(move.id, move);
	}

	public void setBuildLevel(int buildLevel) {
		this.buildLevel = buildLevel;
	}

	public UnitMove getMoveForUnitId(int id) {
		if (unitMoves.containsKey(id)) {
			return unitMoves.get(id);
		} else {
			return new UnitMove(id, UnitMove.NO_MOVE);
		}
	}

	public int getBuildLevel() {
		return buildLevel;
	}

	public static PlayerMove readPlayerMove(ClientConnection c) throws BadProtocolException, DisconnectedException {
		String inputString;
		inputString = c.getStrInput();
		String[] tokens = inputString.split("\\s");
		PlayerMove finalMove = new PlayerMove();
		if (tokens.length < 3) {
			throw new BadProtocolException("Getting action: Not enough arguments (got " + inputString + ")");
		} else if (tokens.length % 2 != 1) {
			throw new BadProtocolException("Getting action: Need pairs of moves (got " + inputString + ")");
		} else if (!tokens[0].equals("MOVE")) {
			throw new BadProtocolException("Getting action: Invalid identifier (got " + inputString + ")");
		}
		finalMove.setBuildLevel(readInt(tokens[1], inputString));
		int totalMoves = readInt(tokens[2], inputString);
		if (totalMoves * 2 != tokens.length - 3) {
			throw new BadProtocolException("Getting action: Not enough moves for what is specified (got " + inputString
					+ ")");
		}
		for (int i = 0; i < totalMoves; i++) {
			int id = readInt(tokens[3 + i * 2], inputString);
			int moveId = readInt(tokens[4 + i * 2], inputString);
			if (moveId < 0 || moveId > UnitMove.NO_MOVE) {
				throw new BadProtocolException("Getting action: Moveid is invalid (got " + inputString + ")");
			}
			finalMove.addUnitMove(new UnitMove(id, moveId));
		}
		return finalMove;
	}

	private static int readInt(String token, String input) throws BadProtocolException {
		int res;
		try {
			res = Integer.parseInt(token);
		} catch (NumberFormatException e) {
			throw new BadProtocolException("Getting action: Bad numbers (got " + input + ")");
		}
		return res;
	}
}
