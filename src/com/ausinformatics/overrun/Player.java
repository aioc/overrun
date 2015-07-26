package com.ausinformatics.overrun;

import java.util.Random;

import com.ausinformatics.phais.core.interfaces.PersistentPlayer;
import com.ausinformatics.phais.core.server.ClientConnection;
import com.ausinformatics.phais.core.server.DisconnectedException;

public class Player implements PersistentPlayer {

	private int ID;
	private String name;
	private ClientConnection connection;
	private int colour;

	public Player(int ID, ClientConnection connection) {
		this.ID = ID;
		this.connection = connection;
	}

	@Override
	public int getID() {
		return ID;
	}

	@Override
	public String getName() {
		if (name == null) {
			generateNewName();
		}
		return name;
	}

	@Override
	public void generateNewName() {
		if (name == null) {
			connection.sendInfo("NAME");
			try {
				String inputString = connection.getStrInput();
				String[] tokens = inputString.split("\\s");
				if (tokens.length < 5) {
					connection.disconnect();
					return;
				} else if (!tokens[0].equals("NAME")) {
					connection.disconnect();
					return;
				} else {
					if (tokens[1].length() > 16) {
						connection.sendInfo("ERROR Your name is too long");
						connection.disconnect();
						return;
					}
					name = tokens[1];
					// TODO handle colours being too similar to others
					int multiplier = 1 << 8;
					int r = Integer.parseInt(tokens[2]);
					int g = Integer.parseInt(tokens[3]);
					int b = Integer.parseInt(tokens[4]);
					if (r < 0 || g < 0 || b < 0 || r >= 256 || g >= 256 || b >= 256) {
						connection.sendInfo("ERROR Your colour values are invalid");
						connection.disconnect();
						return;
					}
					if (r + g + b > 650) {
						connection.sendInfo("ERROR Your choice of colour is invalid");
						connection.disconnect();
						return;
					}
					colour = r;
					colour *= multiplier;
					colour += g;
					colour *= multiplier;
					colour += b;
				}
			} catch (DisconnectedException e) {
				name = "DisconnectedPlayer " + new Random().nextInt(1000);
				// TODO print out something useful
			}
		} else {
			name += new Random().nextInt(1000);
		}
	}

	@Override
	public ClientConnection getConnection() {
		return connection;
	}

	public int getColour() {
		return colour;
	}
}
