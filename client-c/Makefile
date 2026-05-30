# Makefile del cliente C de spaCEinvaders.
#
# Genera dos binarios en build/:
#   - client          : cliente jugador/espectador con raylib (necesita X11/GL)
#   - test_protocol   : test del protocolo JSON (NO necesita raylib)
#
# El cliente se linkea estaticamente contra libraylib.a para que el binario
# corra sin depender de LD_LIBRARY_PATH.

CC      := gcc
CFLAGS  := -Wall -Wextra -O2 -std=c11 -D_GNU_SOURCE -Iinclude -Ivendor/cjson -I$(HOME)/.local/include
LDLIBS  := $(HOME)/.local/lib/libraylib.a -L$(HOME)/.local/lib -lGL -lX11 -lm -lpthread -ldl -lrt

SRC_COMUN  := src/network.c src/protocol.c src/pico.c vendor/cjson/cJSON.c
SRC_CLIENT := src/render.c src/input.c src/main.c $(SRC_COMUN)
SRC_TEST   := test/test_protocol.c $(SRC_COMUN)

BUILD := build

all: $(BUILD)/client $(BUILD)/test_protocol

$(BUILD):
	mkdir -p $(BUILD)

$(BUILD)/client: $(SRC_CLIENT) | $(BUILD)
	$(CC) $(CFLAGS) $(SRC_CLIENT) -o $@ $(LDLIBS)

$(BUILD)/test_protocol: $(SRC_TEST) | $(BUILD)
	$(CC) $(CFLAGS) $(SRC_TEST) -o $@

run: $(BUILD)/client
	$(BUILD)/client

test: $(BUILD)/test_protocol
	$(BUILD)/test_protocol

clean:
	rm -rf $(BUILD)

.PHONY: all run test clean
