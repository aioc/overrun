#include <cassert>
#include <cstdarg>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <ctime>
#include <functional>
#include <map>
#include <sstream>
#include <vector>

#include <unistd.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <arpa/inet.h>
#include <netinet/tcp.h>
#include <signal.h>

#include "valley.h"

using std::string;
using std::vector;

#define VERSION "0.3"

namespace {

//////////////////////////////
// Forward declarations
//
namespace net {
bool sendline(const string& rawdata);
bool recvline(string* data);  // strips newline
}  // namespace net
namespace util {
void err(const string& s);
}  // namespace util


//////////////////////////////
// Player-supplied information
//
struct {
  string name = "noname";
  uint8_t r = 255, g = 0, b = 255;
} player;


//////////////////////////////
// Game state
//
struct Unit {
  typedef std::pair<size_t /* player id */, size_t /* unit id */> Id;
  typedef int Level;

  Id id;
  Level level;
  size_t x, y;
};

enum State {
  ILLEGAL = 0,
  USER_TURN = 1,
};

struct {
  vector<vector<int>> map;
  vector<int> money;
  std::map<Unit::Id, Unit> units;

  State fsm;
  vector<string> buffer_moves, buffer_builds;

  void Reset(size_t num_players, size_t boardsize) {
    map = vector<vector<int>>(boardsize, vector<int>(boardsize, 0));
    money = vector<int>(num_players, 0);
    units.clear();

    fsm = State::ILLEGAL;
    buffer_moves.clear();
    buffer_builds.clear();
  }
} state;

}  // namespace



//////////////////////////////
// Public player methods
//
void setName(const char* name) {
  player.name = name;
}

void setColour(int r, int g, int b) {
  player.r = r;
  player.g = g;
  player.b = b;
}

void move(int uid, int move) {
  if (state.fsm != State::USER_TURN) {
    util::err("Called move() but preconditions are not met.");
    return;
  }

  std::ostringstream builder;
  builder << uid << " " << move;
  state.buffer_moves.push_back(builder.str());
}

void build(int cost) {
  if (state.fsm != State::USER_TURN) {
    util::err("Called build() but preconditions are not met.");
    return;
  }

  std::ostringstream builder;
  builder << cost;
  state.buffer_builds.push_back(builder.str());
}

int getCost(int level) {
  return level;
}

int getCans(int cost) {
  return cost;
}


namespace {

//////////////////////////////
// Command handlers
//
namespace handlers {

bool error(const string& args) {
  util::err("Server sent an error: " + args);
  return true;
}

bool name(const string& args) {
  std::ostringstream builder;
  builder << "NAME"
      << " " << player.name
      << " " << (int) player.r
      << " " << (int) player.g
      << " " << (int) player.b;
  return net::sendline(builder.str());
}

bool new_game(const string& args) {
  size_t num_players, boardsize, player_id;
  std::istringstream builder(args);
  builder >> num_players >> boardsize >> player_id;
  if (builder.bad()) {
    util::err("new game builder failed");
    return false;
  }

  if (num_players >= MAX_PLAYERS ||
      boardsize >= MAX_SIZE ||
      player_id >= MAX_PLAYER_ID) {
    util::err("new game had bad params");
    return false;
  }

  state.Reset(num_players, boardsize);
  clientInit(num_players, boardsize, player_id);

  return net::sendline("READY");
}

bool gameover(const string& args) {
  printf("Game over: %s\n", args.c_str());
  return true;
}

bool update_cell(const string& args) {
  std::istringstream builder(args);

  size_t n = 0;
  builder >> n;
  for (size_t i = 0; i < n; i++) {
    size_t x, y;
    int value;
    builder >> y >> x >> value;

    if (builder.bad()) {
      util::err("update cell builder failed");
      return false;
    }

    if (x >= state.map.size() || y >= state.map.size()) {
      util::err("update cell had bad params");
      return false;
    }

    state.map[y][x] = value;
  }

  return true;
}

bool update_money(const string& args) {
  std::istringstream builder(args);

  for (auto& value : state.money) {
    builder >> value;

    if (builder.bad()) {
      util::err("update money builder failed");
      return false;
    }

    if (value < 0) {
      util::err("update money had bad params");
      return false;
    }
  }

  return true;
}

bool update_unit(const string& args) {
  std::istringstream builder(args);

  size_t n = 0;
  builder >> n;
  for (size_t i = 0; i < n; i++) {
    size_t player_id, unit_id, x, y;
    Unit::Level level;
    builder >> player_id >> unit_id >> y >> x >> level;

    if (builder.bad()) {
      util::err("update unit builder failed");
      return false;
    }

    if (x >= state.map.size() || y >= state.map.size()) {
      util::err("update unit had bad params");
      return false;
    }

    const Unit::Id id(player_id, unit_id);
    if (level == 0) {
      // Unit died.
      state.units.erase(id);
    } else {
      // Unit alive.
      auto* unit = &state.units[id];
      unit->id = id;
      unit->x = x;
      unit->y = y;
      unit->level = level;
    }
  }

  return true;
}

bool user_turn(const string& args) {
  // Notify the client of game state.
  for (size_t i = 0; i < state.money.size(); i++)
    clientMoneyInfo(i, state.money[i]);
  for (size_t i = 0; i < state.map.size(); i++)
    for (size_t k = 0; k < state.map[i].size(); k++)
      clientTerrainInfo(k, i, state.map[i][k]);
  for (const auto it : state.units) {
    const auto& unit = it.second;
    clientDroneLocation(unit.id.first, unit.id.second, unit.x, unit.y, unit.level);
  }

  state.fsm = State::USER_TURN;
  clientDoTurn();
  state.fsm = State::ILLEGAL;

  std::ostringstream builder("MOVE", std::ostringstream::ate);

  // Building
  if (state.buffer_builds.size() == 0) {
    builder << " " << 0;
  } else {
    builder << " " << state.buffer_builds.back();
  }

  // Moving
  builder << " " << state.buffer_moves.size();
  for (const auto& move : state.buffer_moves)
    builder << " " << move;

  state.buffer_moves.clear();
  state.buffer_builds.clear();

  return net::sendline(builder.str());
}

}  // namespace handlers


// Returns true on success.
typedef std::function<bool(const string&)> command_func_t;


const std::map<string, command_func_t> commands{
  {"ERROR", handlers::error},

  {"NAME", handlers::name},
  {"NEWGAME", handlers::new_game},
  {"GAMEOVER", handlers::gameover},

  {"CELL", handlers::update_cell},
  {"MINERALS", handlers::update_money},
  {"LOCATION", handlers::update_unit},

  {"YOURMOVE", handlers::user_turn},
};


/**********************************************
 *                                            *
 *         IF THIS IS DONE CORRECTLY,         *
 *        YOU WILL NOT HAVE TO CHANGE         *
 *          STUFF BELOW THIS COMMENT          *
 *                                            *
 **********************************************/

int sock;
bool echo_mode;


namespace net {

bool sendline(const string& rawdata) {
  const string line = rawdata + "\n";
  if (echo_mode) {
    fprintf(stderr, "\x1b[1;35m> \"");
    fprintf(stderr, "%s", rawdata.c_str());
    fprintf(stderr, "\"\x1b[0m\n");
  }

  ssize_t sent = send(sock, line.c_str(), line.size(), 0);
  return sent == line.size();
}

// Receives until a newline and strips it
bool recvline(string* data) {
  data->clear();
  char c;
  bool success = false;
  while (recv(sock, &c, 1, 0) == 1) {
    if (c == '\n') {
      success = true;
      break;
	}
    *data += c;
  }
  if (echo_mode) {
    fprintf(stderr, "\x1b[1;32m< \"");
    fprintf(stderr, "%s", data->c_str());
    fprintf(stderr, "\"\x1b[0m\n");
  }
  return success;
}

}  // namespace net

namespace util {
void err(const string& s) {
  fprintf(stderr, "%s\n", s.c_str());
}
}  // namespace util


void mainLoop(void) {
  net::sendline("CLIENT");
  // First, let the user code do its own shit. It will set the player name.
  clientRegister();

  while (true) {
    string line;
    if (!net::recvline(&line))
      break;

    // Split line on space into (command, args).
    string command = line, args;
    const auto space_idx = line.find(" ");
    if (space_idx != string::npos) {
      command = line.substr(0, space_idx);
      args = line.substr(space_idx + 1, line.length());
    }

    // Invoke the appropriate handler.
    const auto it = commands.find(command);
    if (it != commands.end()) {
      if (!it->second(args)) {
        // Encountered an error -- disconnect.
        fprintf(stderr, "Error: Failed to process command %s\n",
            command.c_str());
        break;
      }
    } else {
      fprintf(stderr, "Error: server sent unknown command \"%s\"\n"
          "(as part of \"%s\"\n", command.c_str(), line.c_str());
      break;
    }
  }
}

void connectServer(char *server, int port) {
  // Create socket
  if ((sock = socket(PF_INET, SOCK_STREAM, IPPROTO_TCP)) < 0) {
    fprintf(stderr, "Error: could not create socket. This is bad\n");
    exit(EXIT_FAILURE);
  }

  int yes = 1;  // not a bool
  if (setsockopt(sock, IPPROTO_TCP, TCP_NODELAY, &yes, sizeof(yes)) < 0) {
    fprintf(stderr, "Error: could not turn nodelay on. This is bad\n");
    exit(EXIT_FAILURE);
  }

#ifndef __APPLE__
  if (setsockopt(sock, IPPROTO_TCP, TCP_QUICKACK, &yes, sizeof(yes)) < 0) {
    fprintf(stderr, "Error: could not turn quickack on. This is bad\n");
    exit(EXIT_FAILURE);
  }
#endif

  // Initialise remote address
  struct sockaddr_in serv_addr;

  memset(&serv_addr, 0, sizeof(serv_addr));
  serv_addr.sin_family = AF_INET;
  serv_addr.sin_port = htons(port);
  if (inet_pton(AF_INET, server, &serv_addr.sin_addr) <= 0) {
    fprintf(stderr, "Error: could not set up server address. This is bad\n");
    exit(EXIT_FAILURE);
  }
  if (connect(sock, (const struct sockaddr *)&serv_addr, sizeof(struct sockaddr_in)) < 0) {
    close(sock);
    sock = -1;
    // We failed to connect :(
  }
}

///////////////////////////////////////////////
//
//  SEGFAULT CATCHING
//

int app_argc;
char **app_argv;

void segfaultHandler(int v) {
  fprintf(stderr, "Segmentation fault");
  fprintf(stderr, " -- restarting client\n\n\n");
  sleep(1);
  execvp(app_argv[0], app_argv);
  exit(-1);
}

void siguserHandler(int v) {
  fprintf(stderr, "Invalid client operation");
  fprintf(stderr, " -- restarting client\n\n\n");
  sleep(1);
  execvp(app_argv[0], app_argv);
  exit(-1);
}

void sigalrmHandler(int v) {
  fprintf(stderr, "TIME LIMIT EXCEEDED");
  fprintf(stderr, " -- restarting client\n\n\n");
  sleep(1);
  execvp(app_argv[0], app_argv);
  exit(-1);
}

void prepareSignalHandler(int signum, void(*handler)(int)) {
  struct sigaction param;
  memset(&param, 0, sizeof(param));
  param.sa_handler = handler;
  sigemptyset(&(param.sa_mask));
  param.sa_flags = SA_NODEFER | SA_RESTART;
  sigaction(signum, &param, NULL);
}


///////////////////////////////////////////////
//
//  PRINTING FUNCTIONS
//

void printVersion(void) {
  fprintf(stderr, "\n"
      "*******************************\n"
      "*          Welcome to         *\n"
      "*  PHAIS client library %-6s*\n"
      "*******************************\n", VERSION);
}

void printHelp(char *name) {
  fprintf(stderr, "Usage: %s [-hvse] server-ip [port]\n"
      "Options:\n"
      "\th: Print out this help\n"
      "\tv: Print out the version message\n"
      "\ts: Disable segfault handling\n"
      "\te: Echo all network traffic\n", name);
}

}  // namespace

int main(int argc, char *argv[]) {
  printVersion();
  // Make sure that crashes will
  char c;
  char *server;
  int port = 12317;
  bool seg_handle = true;
  int res;
  while ((c = getopt (argc, argv, "hvse")) != -1) {
    switch (c) {
      case 'h':
        printHelp(argv[0]);
        break;
      case 'v':
        printVersion();
        break;
      case 's':
        seg_handle = false;
        break;
      case 'e':
        echo_mode = true;
        break;
    }
  }
  if (optind == argc) {
    fprintf(stderr, "Must provide a server address\n");
    return EXIT_FAILURE;
  }
  if (optind <= argc - 2) {
    // Includes port
    res = sscanf(argv[optind + 1], "%d", &port);
    if (res != 1) {
      fprintf(stderr, "Invalid port option: %s\n", argv[optind + 1]);
      return EXIT_FAILURE;
    }
  }
  server = argv[optind];
  fprintf(stderr, "Configured!\n");
  fprintf(stderr, "Trying server of %s, port %d\n", server, port);
  if (!seg_handle) {
    fprintf(stderr, "SEGFAULTs will *not* be handled\n");
  }

  // Prepare handlers
  app_argc = argc;
  app_argv = argv;
  if (seg_handle) {
    prepareSignalHandler(SIGSEGV, &segfaultHandler);
  }
  prepareSignalHandler(SIGUSR1, &siguserHandler);
  prepareSignalHandler(SIGALRM, &sigalrmHandler);

  // Might as well initialise random
  srand(time(0));
  // Always connect
  while (true) {
    // Attempt to connect here
    while (true) {
      fprintf(stderr, "Attempting to connect...");
      connectServer(server, port);
      if (-1 != sock) {
        break;
      }
      fprintf(stderr, "   failed to connect. Will retry...\n");
      sleep(1);
    }
    fprintf(stderr, "   connected!\n");
    mainLoop();
    fprintf(stderr, "Was disconnected\n");
    shutdown(sock, SHUT_RDWR);
    close(sock);
    sleep(2);
  }
  return EXIT_SUCCESS;
}
