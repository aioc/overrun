CXX = g++
CXXFLAGS = -Wall -Werror -O2 -std=c++11 -I.

CLIENT_BINS := $(patsubst %.cc,%.bin,$(wildcard players/*.cc))

PBLIB := /usr/local/lib/libprotobuf.a
PROTOS := $(wildcard proto/*.proto)
PBOBJS := $(PROTOS:.proto=.pb.o)
PBGENS := $(PROTOS:.proto=.pb.cc)


# Single client. Default build rule.
client: client_library.a players/overrun_example.o
	$(CXX) $^ -o $@

# All clients.
all: $(CLIENT_BINS)

# Client library for distribution.
client_library.a: client_library.o $(PBLIB) $(PBOBJS)
	rm -rf /tmp/legit/ && mkdir -p /tmp/legit/ && cp $(PBOBJS) client_library.o /tmp/legit/ && cd /tmp/legit/ && ar -x $(PBLIB)
	ar -cr $@ /tmp/legit/*.o
client_library.o: client_library.cc
	$(CXX) -c $(CXXFLAGS) $^ -o $@
client_library.cc: $(PBGENS)


# Each individual client binary
%.bin: CXXFLAGS=-O2 -std=c++11 -I.  # No -Wall -Werror
%.bin: %.o client_library.a
	$(CXX) $^ -o $@

#
# Protobufs.
#

proto/%.pb.cc: proto/%.proto
	protoc --cpp_out=. $<

proto/%.pb.o: proto/%.pb.cc


#
# Housekeeping.
#

clean:
	rm -f *.a *.o client players/*.o players/*.bin proto/*.pb.*

.PHONY: all clean
.PRECIOUS: $(PBGENS)
