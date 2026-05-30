# spaCEinvaders

Implementacion distribuida del clasico Space Invaders para el curso Paradigmas de Programacion / Lenguajes, Compiladores e Interpretes (Instituto Tecnologico de Costa Rica). Entrega: 11 de junio de 2026.

## Componentes

- **server-java/** — Servidor Java (orientado a objetos). Mantiene toda la logica del juego, expone TCP en puerto 5555 con protocolo JSON.
- **client-c/** — Cliente jugador en C (imperativo) con raylib. Renderiza estado y envia inputs.
- **spectator-c/** — Cliente espectador (variante del cliente o binario separado).
- **pico-controller/** — Firmware del Raspberry Pi Pico que actua como controlador fisico (2 botones por UART).

## Documentacion

- **docs/protocolo.md** — Protocolo de mensajes JSON entre servidor y clientes.
- **docs/diagramas/** — Diagramas UML, arquitectura, estados y secuencia.
- **bitacora/** — Registro diario de actividades del equipo.

## Build

### Servidor Java (requiere JDK 21)
```bash
cd server-java
./gradlew build
./gradlew run                    # arranca el servidor en puerto 5555
./gradlew runPrueba              # CLI de prueba del motor sin red
```

El JAR queda en `server-java/build/libs/spaceinvaders-server-1.0.0.jar`.

### Cliente C (pendiente)
```bash
cd client-c
make
./client
```

### Firmware Pico (pendiente)
```bash
cd pico-controller
mkdir build && cd build
cmake .. && make
# Flashear controller.uf2 en modo BOOTSEL.
```

## Arquitectura

Cliente jugador (J1, J2) y espectador (E1, E2) se conectan por TCP al servidor. El Pico fisico se conecta al cliente C por UART. Ver `docs/diagramas/` para detalle.

## Equipo

Tres integrantes. Distribucion de responsabilidades documentada en la bitacora.

## Estado actual

- [x] Fase 0: protocolo, estructura, build.
- [x] Fase 1: nucleo del servidor sin red (entidades, lista enlazada propia, motor, factory, observer).
- [ ] Fase 2: integracion red completa (GameLoop + ConsolaAdmin).
- [ ] Fase 3-5: cliente C, multijugador, Pico.
- [ ] Fase 6-7: pulido, documentacion final.
