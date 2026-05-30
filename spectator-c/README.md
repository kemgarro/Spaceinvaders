# spectator-c — Cliente espectador (C + raylib)

Cliente espectador que se conecta al servidor y observa la partida sin enviar input.
Hay un espectador asociado a cada jugador (ver diagrama de arquitectura en `docs/diagramas/`).

## Decision de diseno
Se considera implementarlo como variante del binario de `client-c/` mediante un flag
(`./client --spectator`) en vez de un proyecto separado. Esta carpeta queda como
placeholder por si en revision se decide separar; documentar la decision definitiva
en la bitacora.

## Estado
Pendiente (Fase 4).
