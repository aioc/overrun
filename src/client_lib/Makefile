CXX = clang++
CXXFLAGS = -Wall -Werror -O2 -std=c++11

all: client

client: client_library.o overrun_example.o
	$(CXX) $^ -o $@

.PHONY: all
