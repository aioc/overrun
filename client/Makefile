CXX = g++
CXXFLAGS = -Wall -Werror -O2 -std=c++11 -I.

CLIENT_BINS = $(patsubst %.cc,%.bin,$(wildcard players/*.cc))


# Single client. Default build rule.
client: client_library.o players/overrun_example.o
	$(CXX) $^ -o $@


# All clients.
all: CXXFLAGS=-O2 -std=c++11 -I.  # No -Wall -Werror
all: $(CLIENT_BINS)


%.bin: %.o client_library.o
	$(CXX) $^ -o $@

clean:
	rm -f *.o client players/*.o players/*.bin

.PHONY: all clean
