# ADR 0001 — Motor de eventos discretos determinista

- **Estado:** Aceptada
- **Fecha:** 2026-06

## Contexto

ChaosLab simula sistemas distribuidos para *enseñar* resiliencia. El valor pedagógico depende
de que una corrida sea **reproducible**: un estudiante (o un test) debe poder repetir un escenario
y obtener exactamente el mismo resultado para razonar sobre él. La Fiabilidad es el atributo de
calidad #1 (ISO/IEC 25010) para este producto.

Las alternativas para modelar la concurrencia y el paso del tiempo eran:

1. **Hilos reales + `Thread.sleep`**, dejando que el planificador del SO ordene los eventos.
2. **Simulación de eventos discretos (DES):** un reloj virtual que salta de evento a evento sobre
   una cola de prioridad ordenada por timestamp simulado.

## Decisión

Se implementa un **motor de eventos discretos propio** con reloj virtual. Toda la aleatoriedad
proviene de un único PRNG **sembrado con el `seed` del YAML**; no se usan hilos reales ni
`Thread.sleep`. Ante empates de timestamp, la cola desempata por orden de inserción.

## Consecuencias

- ✅ **Determinismo:** misma topología + mismo seed ⇒ mismo resultado (invariante testeada).
- ✅ **Rapidez:** una corrida de 60 s simulados termina en milisegundos; los tests del dominio no
  levantan Spring ni esperan tiempo real.
- ✅ **Testeabilidad:** el comportamiento es una función pura de las entradas.
- ⚠️ No se modela el no-determinismo real de un sistema distribuido; es una simplificación
  pedagógica deliberada (el objetivo es enseñar patrones, no reproducir el caos real).
- ⚠️ El "ritmo" de reproducción en vivo debe resolverse fuera del motor (ver [ADR 0002](0002-timeline-replay-vs-websocket.md)).
