package com.ausinformatics.overrun.core;

import java.util.Random;

import com.ausinformatics.client.GameClient.ProtocolNameResponse;
import com.ausinformatics.client.GameClient.ProtocolRequest;
import com.ausinformatics.phais.server.interfaces.PersistentPlayer;
import com.ausinformatics.phais.server.server.ClientConnection;
import com.ausinformatics.phais.server.server.DisconnectedException;
import com.ausinformatics.phais.server.server.ProtobufClientConnection;

public class Player implements PersistentPlayer {

	private ProtobufClientConnection connection;
	private int player_id;

	private String name;
	private int colour;

	public Player(int player_id, ClientConnection connection) {
		this.player_id = player_id;
		this.connection = (ProtobufClientConnection) connection;
	}

	@Override
	public int getID() {
		return player_id;
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
			connection.sendMessage(ProtocolRequest.newBuilder().setCommand(ProtocolRequest.Command.NAME).build());
			try {
				ProtocolNameResponse.Builder response_builder = ProtocolNameResponse.newBuilder();
				connection.recvMessage(response_builder);
				ProtocolNameResponse response = response_builder.build();

				if (response.getName().length() > 16) {
					connection.sendFatal("name too long");
					return;
				}

				// TODO handle colours being too similar to others
				int multiplier = 1 << 8;
				int r = response.getR();
				int g = response.getG();
				int b = response.getB();
				if (r < 0 || g < 0 || b < 0 || r >= 256 || g >= 256 || b >= 256 || r + g + b > 650) {
					connection.sendFatal("your colour choices are questionable");
					return;
				}

				name = response.getName();
				colour = r;
				colour *= multiplier;
				colour += g;
				colour *= multiplier;
				colour += b;
			} catch (DisconnectedException e) {
				name = "DisconnectedPlayer " + new Random().nextInt(1000);
				return;
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
