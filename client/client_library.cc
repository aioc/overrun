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

#include <google/protobuf/io/coded_stream.h>
#include <google/protobuf/io/zero_copy_stream_impl.h>
#include "proto/game_client.pb.h"
#include "valley.h"

using std::string;
using std::vector;

#define VERSION "0.3"
#define MAX_PLAYER_NAME_LEN 16

namespace {

bool echo_mode;

//////////////////////////////
// Tiny utils
//
namespace util {
void err(const string& s) {
  fprintf(stderr, "%s\n", s.c_str());
}
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

struct {
  vector<vector<int>> map;
  vector<int> money;
  std::map<Unit::Id, Unit> units;

  client::GameMove move;

  void Reset(size_t num_players, size_t boardsize) {
    map = vector<vector<int>>(boardsize, vector<int>(boardsize, 0));
    money = vector<int>(num_players, 0);
    units.clear();

    move.Clear();
  }
} state;

}  // namespace


//////////////////////////////
// Public player methods
//
void setName(const char* name) {
  player.name = string(name).substr(0, MAX_PLAYER_NAME_LEN);
}

void setColour(int r, int g, int b) {
  player.r = r;
  player.g = g;
  player.b = b;
}

void move(int uid, int action) {
  auto* move = state.move.add_move();
  move->set_unit_id(uid);
  move->set_action(static_cast<client::UnitMove_Action>(action));
}

void build(int cost) {
  state.move.set_build(cost);
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

bool error(const client::ProtocolRequest& request) {
  util::err("Server sent an error: " + request.error_detail());
  return true;
}

bool name(const client::ProtocolRequest& request, client::ProtocolNameResponse* response) {
  response->set_name(player.name);
  response->set_r(player.r);
  response->set_g(player.g);
  response->set_b(player.b);
  return true;
}

bool new_game(const client::ProtocolRequest& request, client::ProtocolNewGameResponse* response) {
  const auto& params = request.newgame();

  if (params.num_players() >= MAX_PLAYERS ||
      params.board_size() >= MAX_SIZE ||
      params.player_id() >= MAX_PLAYER_ID) {  // zero-based, adjust here before giving to user
    util::err("new game had bad params: " + params.ShortDebugString());
    return false;
  }

  state.Reset(params.num_players(), params.board_size());
  clientInit(params.num_players(), params.board_size(), params.player_id() + 1);
  return true;
}

bool gameover(const client::ProtocolRequest& request) {
  printf("Game over: %s\n", request.gameover().detail().c_str());
  return true;
}

bool update_state(const client::ProtocolRequest& request) {
  const auto& params = request.update_state();

  // Cells
  for (const auto& info : params.cell_change()) {
    if (info.x() >= state.map.size() || info.y() >= state.map.size()) {
      util::err("cell_change item was bad: " + info.ShortDebugString());
      return false;
    }

    state.map[info.y()][info.x()] = info.value();
  }

  // Units
  for (const auto& info : params.unit_change()) {
    if (info.x() >= state.map.size() ||
        info.y() >= state.map.size() ||
        info.player_id() >= state.money.size()) {
      util::err("unit_change item was bad: " + info.ShortDebugString());
      return false;
    }

    const Unit::Id id(info.player_id(), info.unit_id());
    if (info.level() == 0) {
      // Unit died.
      state.units.erase(id);
    } else {
      // Unit alive.
      auto* unit = &state.units[id];
      unit->id = id;
      unit->x = info.x();
      unit->y = info.y();
      unit->level = info.level();
    }
  }

  // Money
  if (params.money_state_size() != state.money.size()) {
    util::err("unexpected money_state size");
    return false;
  }
  for (size_t i = 0; i < state.money.size(); i++) {
    state.money[i] = params.money_state(i);
  }

  return true;
}

bool compute_move(const client::ProtocolRequest& request, client::GameMove* response) {
  // Notify the client of game state.
  for (size_t i = 0; i < state.money.size(); i++)
    clientMoneyInfo(i + 1, state.money[i]);
  for (size_t i = 0; i < state.map.size(); i++)
    for (size_t k = 0; k < state.map[i].size(); k++)
      clientTerrainInfo(k, i, state.map[i][k]);
  for (const auto it : state.units) {
    const auto& unit = it.second;
    clientDroneLocation(unit.id.first + 1, unit.id.second, unit.x, unit.y, unit.level);
  }

  state.move.Clear();
  clientDoTurn();
  *response = state.move;
  return true;
}

}  // namespace handlers


namespace net {

bool ReadLengthDelimitedMessage(
    google::protobuf::io::ZeroCopyInputStream* zcis,
    google::protobuf::Message* message) {
  uint32_t protosz;
  {
    google::protobuf::io::LimitingInputStream lis(zcis, sizeof(protosz));
    google::protobuf::io::CodedInputStream input(&lis);

    if (!input.ReadLittleEndian32(&protosz))
      return false;
    if (protosz >> 31)
      return false;
  }

  {
    google::protobuf::io::LimitingInputStream lis(zcis, protosz);
    google::protobuf::io::CodedInputStream input(&lis);

    if (!message->ParseFromCodedStream(&input))
      return false;
    if (!input.ConsumedEntireMessage())
      return false;
  }

  if (echo_mode) {
    fprintf(stderr, "\x1b[0;32m> ");
    fprintf(stderr, "%s", message->ShortDebugString().c_str());
    fprintf(stderr, "\x1b[0m\n");
  }

  return true;
}

void SendLengthDelimitedMessage(
    google::protobuf::io::ZeroCopyOutputStream* zcos,
    const google::protobuf::Message& message) {
  {
    google::protobuf::io::CodedOutputStream output(zcos);

    // No error checks.
    const string data(message.SerializeAsString());
    output.WriteLittleEndian32(data.size());
    output.WriteString(data);
  }

  // HACK
  dynamic_cast<google::protobuf::io::FileOutputStream*>(zcos)->Flush();

  if (echo_mode) {
    fprintf(stderr, "\x1b[0;31m< ");
    fprintf(stderr, "%s", message.ShortDebugString().c_str());
    fprintf(stderr, "\x1b[0m\n");
  }
}

}  // namespace net


void mainLoop(google::protobuf::io::ZeroCopyInputStream* input,
    google::protobuf::io::ZeroCopyOutputStream* output) {
  // First, let the user code do its own shit. It will set the player name.
  clientRegister();

  bool keep_going = true;
  while (keep_going) {
    client::ProtocolRequest command;
    if (!net::ReadLengthDelimitedMessage(input, &command))
      break;

#define DO_FAST(enum, handler)  \
    case client::ProtocolRequest::enum:  \
        keep_going = handlers::handler(command); break;
#define DO_RESPONSE(enum, handler, type)  \
    case client::ProtocolRequest::enum: {  \
        type response;  \
        keep_going = handlers::handler(command, &response);  \
        if (keep_going) net::SendLengthDelimitedMessage(output, response);  \
        break;  \
    }

    switch (command.command()) {
      // Protocol commands
      DO_FAST(ERROR, error);
      DO_RESPONSE(NAME, name, client::ProtocolNameResponse);
      DO_RESPONSE(NEWGAME, new_game, client::ProtocolNewGameResponse);
      DO_FAST(GAMEOVER, gameover);

      // Game
      DO_FAST(UPDATE_STATE, update_state);
      DO_RESPONSE(COMPUTE_MOVE, compute_move, client::GameMove);

      case client::ProtocolRequest::UNKNOWN:
      default:
        fprintf(stderr, "Error: server sent unknown command: %s", command.ShortDebugString().c_str());
        assert(!"buddy, this is wrong");
        continue;
    }

#undef DO_FAST
#undef DO_RESPONSE
  }
}

int connectServer(char *server, int port) {
  // Create socket
  int sock;
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

  return sock;
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
    int sock = -1;

    // Attempt to connect here
    while (true) {
      fprintf(stderr, "Attempting to connect...");
      sock = connectServer(server, port);
      if (-1 != sock) {
        break;
      }
      fprintf(stderr, "   failed to connect. Will retry...\n");
      sleep(1);
    }
    fprintf(stderr, "   connected!\n");

    // HACK: send a "client" tag
    char tag = 'C';
    send(sock, &tag, 1, 0);

    {  // Make a scope for the input streams.
      google::protobuf::io::FileInputStream fis(sock);
      google::protobuf::io::FileOutputStream fos(sock);
      mainLoop(&fis, &fos);
    }

    fprintf(stderr, "Was disconnected\n");
    shutdown(sock, SHUT_RDWR);
    close(sock);
    sleep(2);
  }

  return EXIT_SUCCESS;
}
