# ADR 0003 — Monolito modular con Clean Architecture

- **Estado:** Aceptada
- **Fecha:** 2026-06

## Contexto

ChaosLab *simula* sistemas distribuidos. Existía la tentación de construirlo *como* un sistema
distribuido (varios servicios, colas reales), pero eso introduciría toda la complejidad operativa
que el proyecto pretende **enseñar**, no **sufrir**. La regla práctica del proyecto: cada incremento
de complejidad estructural debe pagar un atributo de calidad concreto que la alternativa simple no
podía dar — y aquí no lo paga.

## Decisión

Una sola aplicación **Spring Boot** estructurada como **monolito modular** con **Clean Architecture**.
La dependencia siempre apunta hacia adentro:

```
Infrastructure  →  Application  →  Domain
```

- **Domain** (`com.chaoslab.domain`): motor, topología, fallos, resiliencia, métricas. **Cero**
  dependencias de Spring, web o YAML.
- **Application**: casos de uso que orquestan el dominio.
- **Infrastructure**: adaptadores (YAML, CLI, web). Único lugar que conoce a Spring.

## Consecuencias

- ✅ El dominio se testea con JUnit puro, rápido, sin levantar Spring.
- ✅ Los adaptadores son intercambiables (CLI y dashboard comparten el mismo dominio; se agregó la
  web sin tocar el motor).
- ✅ Baja complejidad operativa: un solo artefacto desplegable.
- ⚠️ Exige disciplina para no filtrar dependencias hacia adentro (regla dura en `CLAUDE.md`; se
  puede vigilar con ArchUnit/Checkstyle a futuro).
- ↩️ Si algún día se necesita escalado o despliegue independiente por partes, se reevaluará hacia
  servicios — pero solo cuando un atributo de calidad concreto lo justifique.
