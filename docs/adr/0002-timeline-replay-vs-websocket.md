# ADR 0002 — Dashboard por replay de línea de tiempo (no WebSocket push)

- **Estado:** Aceptada
- **Fecha:** 2026-06

## Contexto

El dashboard debe mostrar la simulación "en vivo": el grafo cambiando de color y las métricas
animándose. El plan inicial preveía un canal **WebSocket/STOMP** para que el motor empujara
eventos al navegador según ocurren.

Pero el motor es determinista y corre en **tiempo virtual**: una simulación de 60 s se computa
entera en milisegundos (ver [ADR 0001](0001-motor-eventos-discretos-determinista.md)). No hay un
flujo temporal real que empujar: los eventos ya están todos calculados cuando la corrida termina.
Un "streaming en vivo" real exigiría mapear tiempo virtual a tiempo de pared con hilos/temporizadores
en el servidor, lo que **contradice el determinismo** y agrega concurrencia y estado por sesión.

## Decisión

El backend corre la simulación completa y devuelve por **REST** (`POST /api/run`) la **línea de
tiempo** de snapshots junto con la estructura del grafo. El **frontend reproduce** esa línea de
tiempo en el cliente (animación con temporizador de navegador), a la velocidad que elija el usuario.

## Consecuencias

- ✅ Servidor **sin estado** por sesión, sin hilos de temporización; el motor sigue puro.
- ✅ Más simple y robusto: una sola llamada REST; el "ritmo" es cosmético y vive en el cliente.
- ✅ El usuario controla la velocidad y podría pausar/rebobinar (la timeline está completa en el cliente).
- ⚠️ No es "tiempo real" de verdad; para simulaciones interactivas de larga duración (streaming
  continuo) haría falta el enfoque WebSocket. Queda como mejora futura si el caso de uso lo pide.
