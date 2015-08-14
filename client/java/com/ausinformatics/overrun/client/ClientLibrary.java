package com.ausinformatics.overrun.client;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Inet4Address;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.reflections.Reflections;

import com.ausinformatics.phais.utils.Pair;

/** This class is the manager of all things client.
 *
 * It is the main() entry point of the client, as well as providing all network
 * communication. It also provides a {@link ClientInterface} that your class
 * must implement if it intends to play the game
 */
public class ClientLibrary {

    private static ClientInterface client;
    private static ClientLibrary mInstance;

    private static Socket socket;
    private static InputStreamReader socketReader;
    /** Set ClientLibrary.echo_mode = true if you need to debug network
     * traffic. Likely won't be needed by a client, but sometimes it's worth
     * knowing what the server sends vs what you believe about what you're
     * receiving
     */
    public static boolean echo_mode = false;
    private final String VERSION = "0.1";

    private static Player player;
    private static State state;

    private final Map<String, command_func_t> commands;

    private static class Unit {
        /* "Typedef" */
        public class Id extends Pair<Integer, Integer> {
            Id(Integer a, Integer b) {
                super(a, b);
            }
        }

        public Id id;
        public int level;
        public int x;
        public int y;

        public Unit(int p, int u) {
            id = new Id(p, u);
            level = -1;
            x = -1;
            y = -1;
        }
    }
    
    private static class State {
        public int[][] map;
        public int[] money;
        public Map<Unit.Id, Unit> units;

        public enum STATE {
            ILLEGAL,
            USER_TURN;
        }

        public STATE fsm;
        public List<String> buffer_moves;
        public List<String> buffer_builds;

        public void Reset(int num_players, int boardSize) {
            map = new int[boardSize][boardSize];
            for (int i = 0; i < boardSize; i++) {
                for (int j = 0; j < boardSize; j++)
                    map[i][j] = 0;
            }

            money = new int[num_players];
            for (int i = 0; i < num_players; i++)
                money[i] = 0;
            units = new HashMap<Unit.Id, Unit>();

            fsm = STATE.ILLEGAL;
            buffer_moves = new ArrayList<String>();
            buffer_builds = new ArrayList<String>();
        }
    }

    private static class Player {
        public String name = "noname";
        public int r = 255;
        public int g = 0;
        public int b = 255;
    };

    private static class Net {
        public static boolean sendline(final String rawdata) {
            if (echo_mode) {
                System.err.printf("\u001B[1;35m> \"");
                System.err.printf("%s", rawdata);
                System.err.printf("\"\u001B[0m\n");
            }
            try {
                DataOutputStream os = new DataOutputStream(socket.getOutputStream());
                String line = rawdata + "\n";
                os.writeBytes(line);
                return os.size() == line.length();
            } catch (IOException e) {
                System.err.println("Catastrophic error when trying to send data");
                e.printStackTrace();
                return false;
            }
        }

        public static boolean recvline(StringBuilder data) {
            data.delete(0, data.length());
            char c = (char) -1;
            boolean success = false;
            try {
                while ((c = (char)socketReader.read()) != (char) -1) {
                    if (c == '\n') {
                        success = true;
                        break;
                    }
                    data.append(c);
                }
            } catch (IOException e) {
                System.err.println("Couldn't recvline!");
                e.printStackTrace();
            }

            if (echo_mode) {
                System.err.printf("\u001B[1;32m< \"");
                System.err.printf("%s", data);
                System.err.printf("\"\u001B[0m\n");
            }

            return success;
        }
    }

    private static class Util {
        public static void err(final String s) {
            System.err.println(s);
        }
    }

    private static class Handlers {
        public static boolean error(final String args) {
            Util.err("Server sent an error: " + args);
            return true;
        }

        public static String sanitize_name(final String name) {
            char[] res = new char[17];
            res[0] = '\0';
            int i;
            for (i = 0; i < name.length() && i < 16; i++) {
                if (name.charAt(i) == ' ') {
                    res[i] = '_';
                } else {
                    res[i] = name.charAt(i);
                }
                res[i + 1] = '\0';
            }
            return new String(res).substring(0, i);
        }

        public static boolean name(final String args) {
            StringBuilder sb = new StringBuilder();
            sb.append("NAME");
            sb.append(" ");
            sb.append(Handlers.sanitize_name(player.name));
            sb.append(" ");
            sb.append(player.r);
            sb.append(" ");
            sb.append(player.g);
            sb.append(" ");
            sb.append(player.b);
            return Net.sendline(sb.toString());
        }

        public static boolean new_game(final String args) {
            int num_players;
            int boardsize;
            int player_id;
            Scanner inp = new Scanner(args);

            try {
                num_players = inp.nextInt();
                boardsize = inp.nextInt();
                player_id = inp.nextInt();
            } catch (Exception e) {
                Util.err("New game builder failed: " + e.getMessage());
                inp.close();
                return false;
            }

            if (num_players >= ClientInterface.Constants.MAX_PLAYERS ||
                    boardsize >= ClientInterface.Constants.MAX_SIZE || 
                    player_id > ClientInterface.Constants.MAX_PLAYER_ID ||
                    player_id == 0) {
                Util.err("New game had bad parameters");
                inp.close();
                return false;
            }

            state.Reset(num_players, boardsize);
            client.clientInit(num_players, boardsize, player_id);

            inp.close();
            return Net.sendline("READY");
        }
        
        public static boolean gameover(final String args) {
            System.out.println("Game over: " + args);
            return true;
        }

        public static boolean update_cell(final String args) {
            Scanner s = new Scanner(args);
            int n = 0;
            try {
                n = s.nextInt();
            } catch (Exception e) {
                Util.err("Update cell args were beyond broken: " + args);
                s.close();
                return false;
            }
            for (int i = 0; i < n; i++) {
                int x, y;
                int value;
                try {
                    y = s.nextInt();
                    x = s.nextInt();
                    value = s.nextInt();
                } catch (Exception e) {
                    Util.err("Update cell builder failed: " + e.getMessage());
                    s.close();
                    return false;
                }

                if (x >= state.map.length || y >= state.map.length) {
                    Util.err("Update cell had bad params");
                    s.close();
                    return false;
                }

                state.map[y][x] = value;
            }

            s.close();
            return true;
        }

        public static boolean update_money(final String args) {
            Scanner s = new Scanner(args);
            for (int i = 0; i < state.money.length; i++) {
                try {
                    state.money[i] = s.nextInt();
                } catch (Exception e) {
                    Util.err("Update money builder failed: " + e.getMessage());
                    s.close();
                    return false;
                }
                if (state.money[i] < 0) {
                    Util.err("Update money had bad params");
                    s.close();
                    return false;
                }
            }

            s.close();
            return true;
        }

        public static boolean update_unit(final String args) {
            Scanner s = new Scanner(args);

            int n;
            try {
                n = s.nextInt();
            } catch (Exception e) {
                Util.err("Update unit args were beyond broken: " + args);
                s.close();
                return false;
            }

            for (int i = 0; i < n; i++) {
                int player_id, unit_id, x, y;
                int level;
                try {
                    player_id = s.nextInt();
                    unit_id = s.nextInt();
                    y = s.nextInt();
                    x = s.nextInt();
                    level = s.nextInt();
                } catch (Exception e) {
                    Util.err("Update unit builder failed: " + e.getMessage());
                    s.close();
                    return false;
                }

                if (x >= state.map.length || y >= state.map.length) {
                    Util.err("update unit had bad params");
                    s.close();
                    return false;
                }

                final ClientLibrary.Unit unit = new ClientLibrary.Unit(player_id, unit_id);
                if (level == 0) {
                    state.units.remove(unit.id);
                } else {
                    if (!state.units.containsKey(unit.id)) {
                        state.units.put(unit.id, unit);
                    }
                    state.units.get(unit.id).id = unit.id;
                    state.units.get(unit.id).x = x;
                    state.units.get(unit.id).y = y;
                    state.units.get(unit.id).level = level;
                }
            }

            s.close();
            return true;
        }

        public static boolean user_turn(final String args) {
            for (int i = 0; i < state.money.length; i++) 
                client.clientMoneyInfo(i + 1, state.money[i]);
            for (int i = 0; i < state.map.length; i++)
                for (int j = 0; j < state.map.length; j++)
                    client.clientTerrainInfo(j, i, state.map[i][j]);
            for (final Map.Entry<Unit.Id, Unit> item : state.units.entrySet()) {
                Unit unit = item.getValue();
                client.clientDroneLocation(unit.id.first, unit.id.second, unit.x, unit.y, unit.level);
            }

            state.fsm = State.STATE.USER_TURN;
            client.clientDoTurn();
            state.fsm = State.STATE.ILLEGAL;

            StringBuilder sb = new StringBuilder();
            sb.append("MOVE");
            sb.append(" ");
            if (state.buffer_builds.size() == 0) {
                sb.append(0);
            } else {
                sb.append(state.buffer_builds.get(state.buffer_builds.size() - 1));
            }

            sb.append(" ");
            sb.append(state.buffer_moves.size());
            for (final String move : state.buffer_moves)  {
                sb.append(" ");
                sb.append(move);
            }

            state.buffer_moves.clear();
            state.buffer_builds.clear();

            return Net.sendline(sb.toString());
        }
    }

    interface command_func_t {
        public boolean f(final String s);
    }

    protected ClientLibrary() {
        commands = new HashMap<String, command_func_t>(8);
        commands.put("ERROR", new command_func_t() {
            @Override public boolean f(final String s) {
                return Handlers.error(s);
            }
        });
        commands.put("NAME", new command_func_t() {
            @Override public boolean f(final String s) {
                return Handlers.name(s);
            }
        });
        commands.put("NEWGAME", new command_func_t() {
            @Override public boolean f(final String s) {
                return Handlers.new_game(s);
            }
        });
        commands.put("GAMEOVER", new command_func_t() {
            @Override public boolean f(final String s) {
                return Handlers.gameover(s);
            }
        });
        commands.put("CELL", new command_func_t() {
            @Override public boolean f(final String s) {
                return Handlers.update_cell(s);
            }
        });
        commands.put("MINERALS", new command_func_t() {
            @Override public boolean f(final String s) {
                return Handlers.update_money(s);
            }
        });
        commands.put("LOCATION", new command_func_t() {
            @Override public boolean f(final String s) {
                return Handlers.update_unit(s);
            }
        });
        commands.put("YOURMOVE", new command_func_t() {
            @Override public boolean f(final String s) {
                return Handlers.user_turn(s);
            }
        });
    }

    /************************************************
     * These are the only functions that a user should care about
     */
    /** Returns the singleton instance of the ClientLibrary. This will be
     * necessary if you intend to call functions like {@link #build(int cost)}
     * or similar
     */
    public static ClientLibrary getInstance() {
        if (mInstance == null) {
            mInstance = new ClientLibrary();
        }
        return mInstance;
    }

    /** The client interface. You <b><i>must</i></b> implement this class for
     * your bot to actually function (since the ClientLibrary determines
     * through reflection the name of your class).
     */
    public interface ClientInterface {

        /** A set of helpful constants */
        public class Constants {
            /** The max number of players that will ever be in a game. */
            public final static int MAX_PLAYERS = 90;
            /** The maximum side-length of the board. */
            public final static int MAX_SIZE = 100;
            /** The maximum player ID that will ever be assigned to a player */
            public final static int MAX_PLAYER_ID = 100000;

            /** This constant indicates a wall on the map */
            public final static int WALL = -99;
            /** This constant indicates a completely blank cell; no minerals,
             * bases or walls
             */
            public final static int BLANK_CELL = 0;

            /** The move constant to move north */
            public final static int NORTH = 0;
            /** The move constant to move east */
            public final static int EAST = 1;
            /** The move constant to move south */
            public final static int SOUTH = 2;
            /** The move constant to move west */
            public final static int WEST = 3;
            /** The move constant to extract money from a square */
            public final static int EXTRACT = 4;
        }

        /** This function is called when your client connects to the server.
         *
         * <p>
         * You need to provide a name using {@link #setName(String)}, and
         * optionally choose a colour using {@link #setColour(int, int, int)}
         * </p>
         *
         * <p>
         * clientRegister() must return within 1 second.
         * </p>
         */
        public void clientRegister();

        /** This is called when a game is about to begin.
         * <p>
         * You are not required to make any calls in this function.
         * </p>
         *
         * @param playerCount The number of players in this game
         * @param boardSize The length of the side of the board (The board will
         * be square.)
         * @param playerId Your player ID. Your base has a type of -playerId
         * (see {@link #clientTerrainInfo(int, int, int)})
         *
         * <p>
         * This function must return within 1 second
         * </p>
         */
        public void clientInit(int playerCount, int boardSize, int playerId);

        /** This function is called once per player currently alive, before
         * your turn begins.
         *
         * <p>
         * You are not required to make any calls in this function
         * </p>
         *
         * @param pid The player being reported
         * @param moneyCount How much money the player currently has banked
         *
         * <p>
         * This function must return within 1 second
         * </p>
         */
        public void clientMoneyInfo(int pid, int moneyCount);

        /** This function is called once per square on the map, before you
         * take your turn.
         * <p>
         * You are not required to make any calls in this function.
         * </p>
         *
         * @param x The x coordinate of the square being reported
         * @param y The y coordinate of the square being reported
         * @param type The type of this terrain. <br/><table>
                <tr><td><b>Value</b></td><td><b>Type</b></td></tr>
                <tr><td>0</td><td>Blank square</td></tr>
                <tr><td>{@link ClientInterface.Constants#WALL}</td><td>A wall</td></tr>
                <tr><td><i>n</i>, <i>n</i> &lt; 0</td>
                    <td>The home base of player -n. If -n is your ID, this is
                        your base</td>
                </tr>
                <tr><td><i>x</i>, <i>x</i> &gt; 0</td>
                    <td>A square with <i>x</i> resources</td>
                </tr></table>
         *
         * <p>
         * This function will not ever be called with any information about the
         * units on the map. This function must return within 1 second.
         * </p>
         */
        public void clientTerrainInfo(int x, int y, int type);

        /** This function is called once per unit on the map, before your
         * turn.
         *
         * <p>
         * You are not required to call anything from this function.
         * </p>
         *
         * @param pid The ID of the player who owns this unit
         * @param id The ID of this unit. The pair (pId, id) is guaranteed to
         * be unique across the game, however (id) itself is not.
         * @param x The x value of the square this unit sits on
         * @param y The y value of the square this unit sits on
         * @param numCans The number of cans this drone has in its stock.
         *
         * <p>
         * This function must return within 1 second
         * </p>
         */
        public void clientDroneLocation(int pid, int id, int x, int y, int numCans);

        /** This function is called when it is your turn to play.
         *
         * <p>
         * You are not required to call anything from this function, but it is
         * strongly suggested that you call some combination of
         * {@link #move(int, int)}, {@link #build(int)}, {@link #getCost(int)},
         * and {@link #getCans(int)} from within this function.
         * </p>
         *
         * <p>
         * This function must return within <b>2 seconds</b>
         * </p>
         */
        public void clientDoTurn();
    }

    /** This will ask the server to set your name. Names are sanitised to
     * remove spaces and be at most 16 characters long.
     *
     * @param name Your name
     */
    public void setName(final String name) {
        player.name = name;
    }

    /** This will ask the server to set the colour of your AI. For a given game
     * you are not guaranteed to be exactly this colour (if two colours are
     * considered 'too close'); in this case your colour will be used as a
     * starting hint for the final game colour.
     *
     * @param r The r value of your colour, between 0 and 255.
     * @param g The g value of your colour, between 0 and 255.
     * @param b The b value of your colour, between 0 and 255.
     */
    public void setColour(final int r, final int g, final int b) {
        player.r = r;
        player.g = g;
        player.b = b;
    }

    /** This function asks the server to move one of your units. It may only be
     * called from {@link ClientInterface#clientDoTurn()}. If you ask the server
     * to move the same unit multiple times per turn, only the first request
     * will be respected; the remainder will be ignored
     *
     * @param uid The Id of the unit you wish to move.
     * @param move The command you wish to issue (see the constants defined in
     * {@link Constants})
     */
    public void move(int uid, int move) {
        if (state.fsm != State.STATE.USER_TURN) {
            Util.err("Called move() but preconditions are not met.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(uid);
        sb.append(" ");
        sb.append(move);
        state.buffer_moves.add(sb.toString());
    }

    /** This function asks the server to build a unit. It may only be called
     * from within {@link ClientInterface#clientDoTurn()}. You may only build
     * one unit per turn; the <i>last</i> such request will be honoured and the
     * remainder will be ignored.
     *
     * @param cost The amount of money you wish to spend on this unit. See
     * {@link #getCost(int)} and {@link #getCans(int)}
     */
    public void build(int cost) {
        if (state.fsm != State.STATE.USER_TURN) {
            Util.err("Called build() but preconditions are not met");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(cost);
        state.buffer_builds.add(sb.toString());
    }

    /** This function returns the cost to build a unit. It may be called at any
     * time.
     *
     * @param cans The number of cans that the unit has
     * @return The cost to build a unit with the given number of cans
     */
    public int getCost(int cans) {
        return cans;
    }

    /** This function returns the number of cans a unit will start with, given a cost.
     * It may be called at any time.
     *
     * @param value The value you wish to spend on this unit
     * @return The number of cans this unit will start with, given the value
     * you wish to spend on it.
     */
    public int getCans(int value) {
        return value;
    }

    /*************************************************/

    private void mainLoop() {
        player = new Player();
        state = new State();
        Net.sendline("CLIENT");
        client.clientRegister();

        while (true) {
            StringBuilder sb = new StringBuilder();
            if (!Net.recvline(sb))
                break;
            
            String line = sb.toString();
            String command = line, args = "";
            final int space_idx = line.indexOf(' ');
            if (space_idx != -1) {
                command = line.substring(0, space_idx);
                args = line.substring(space_idx + 1, line.length());
            }

            if (commands.containsKey(command)) {
                if (!commands.get(command).f(args)) {
                    System.err.println("Error: failed to process command " + command);
                    break;
                }
            } else {
                System.err.printf("Error: server sent unknown command \"%s\"\n"
                        + "(as part of \"%s\"\n", command, line);
            }
        }
    }

    private void connectServer(String server, int port) {
        try {
            socket = new Socket(Inet4Address.getByName(server), port);
        } catch (UnknownHostException e) {
            System.err.println("Error: Unknown host \"" + server + "\". Did you use the right address?");
            System.err.println("Error: This is unrecoverable");
            System.exit(-1);
        } catch (ConnectException e) {
            System.err.println("Error: Could not connect to the server. Is the server running?");
            return;
        } catch (Exception e) {
            System.err.println("Error: could not create socket. This is unrecoverable\n");
            e.printStackTrace();
            return;
        }

        try {
            socket.setTcpNoDelay(true /* on */);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        try {
            socketReader = new InputStreamReader(socket.getInputStream());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private boolean isSocketGood() {
        return socket != null;
    }

    private void disconnectServer() {
        try {
            socketReader.close();
            socket.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void printVersion() {
        System.err.printf("\n"
                + "*******************************\n"
                + "*          Welcome to         *\n"
                + "*  PHAIS client library %-6s*\n"
                + "*******************************\n", VERSION);
    }

    private void printHelp(String name) {
        System.err.printf("Usage: %s [-hvse] server-ip [port]\n"
                + "Options:\n"
                + "\th: Print out this help\n"
                + "\tv: Print out the version message\n"
                + "\te: Echo all network traffic\n", name);
    }

    /** The entry point of the client. You should not attempt to write your own
     * main function!
     */
    public static void main(String[] args) {
        ClientLibrary library = ClientLibrary.getInstance();

        library.printVersion();
        String server = null;
        int port = 12317;
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-")) {
                if (args[i].contains("h")) {
                    String myName = Thread.currentThread().getStackTrace()[2].getClassName();
                    library.printHelp(myName);
                }
                if (args[i].contains("v")) {
                    library.printVersion();
                }
                if (args[i].contains("e")) {
                    ClientLibrary.echo_mode = true;
                }
            } else if (server == null) {
                server = args[i];
            } else {
                try {
                    port = Integer.parseInt(args[i]);
                } catch (Exception e) {
                    System.err.println("Must provide a valid integer for the port\n");
                    return;
                }
            }
        }

        if (server == null) {
            System.err.println("Must provide a server address\n");
            return;
        }

        /* Find the class that implements ClientInterface */
        Reflections reflections = new Reflections("com.ausinformatics.overrun.client");
        Set<Class<? extends ClientLibrary.ClientInterface>> classes = reflections.getSubTypesOf(ClientLibrary.ClientInterface.class);
        ClientLibrary.ClientInterface client = null;
        for (Class<? extends ClientLibrary.ClientInterface> cls : classes) {
            try {
                client = (ClientLibrary.ClientInterface) cls.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                System.err.println("Couldn't find a class that implements ClientInterface; this is unrecoverable");
                return;
            }
        }

        ClientLibrary.client = client;

        System.err.println("Configured!");
        System.err.printf("Trying to connect to %s, port %d\n", server, port);
        while (true) {
            while (true) {
                System.err.println("Attempting to connect...");
                library.connectServer(server, port);
                if (library.isSocketGood())
                    break;
                System.err.println("    failed to connect. Will retry...");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
            if (library.isSocketGood()) {
                System.err.println("    connected!");
                library.mainLoop();
                System.err.println("Was disconnected\n");
                library.disconnectServer();
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
