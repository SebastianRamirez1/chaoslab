# ChaosLab 🔬

> Simulador educativo de sistemas distribuidos y *chaos engineering*. Define tu arquitectura
> en YAML, se levanta una simulación **en memoria** (sin infraestructura real) e inyectas fallos
> —latencia, caídas, particiones de red— para ver en tiempo real cómo responde el sistema.

La versión de aprendizaje de Chaos Monkey: barata, visible y reproducible.

---

## Estado

🚧 **Fase 0 — Fundación.** El proyecto compila, tiene estructura profesional (Clean Architecture)
y CI con gates de calidad. El motor de simulación se construye en las fases siguientes.

## Stack

- **Java 21 LTS** + **Spring Boot 3.3** (solo en las capas externas; el dominio es puro)
- Motor de **eventos discretos** propio · **Picocli** (CLI) · **Spring WebSocket/STOMP** (dashboard)
- **JUnit 5 + AssertJ** · **Checkstyle** · **SpotBugs** · **JaCoCo** · **OWASP Dependency-Check**
- **GitHub Actions** (CI) · **Docker** (entorno reproducible)

## Arquitectura

Monolito modular con Clean Architecture. La dependencia **siempre apunta hacia adentro**:

```
Infrastructure  →  Application  →  Domain
   (adaptadores)   (casos de uso)   (motor puro: cero Spring/web/YAML)
```

```
src/main/java/com/chaoslab/
├── domain/            ← núcleo, cero dependencias externas
│   └── engine/        ← Clock, SimulationEngine, EventQueue (motor de eventos discretos)
├── application/       ← casos de uso (orquestan el dominio)
└── infrastructure/    ← adaptadores: yaml, cli, web
```

## Requisitos

- JDK 21 (Temurin recomendado). Si tu JDK por defecto es < 21, el build usa **Maven Toolchains**
  para compilar con JDK 21 (ver `~/.m2/toolchains.xml`).
- Maven 3.9+

## Comandos

```bash
# Build completo: compila + tests + Checkstyle + SpotBugs + gate de cobertura JaCoCo
mvn verify

# Solo lint de estilo
mvn checkstyle:check

# Solo tests
mvn test

# Escaneo de seguridad (SCA de dependencias, OWASP). Lento: descarga datos NVD.
# Recomendado configurar una API key: -DnvdApiKey=XXXX
mvn -Psecurity verify
```

Tras `mvn verify`, el reporte de cobertura queda en `target/site/jacoco/index.html`.

## Calidad y gates

| Gate | Herramienta | Umbral |
|---|---|---|
| Estilo | Checkstyle | 0 violaciones (warning+) |
| Bugs estáticos | SpotBugs | 0 (effort Max, threshold Medium) |
| Cobertura del dominio | JaCoCo | ≥ 70% líneas en `com.chaoslab.domain` |
| Vulnerabilidades de dependencias | OWASP Dependency-Check | falla si CVSS ≥ 7 (perfil `security`) |

Se siguen las **Directrices de Ingeniería de Software** del autor
([docs/DIRECTRICES_INGENIERIA.md](docs/DIRECTRICES_INGENIERIA.md)) y las reglas duras en
[CLAUDE.md](CLAUDE.md).

## Licencia

[MIT](LICENSE) · Sebastian Ramirez · ITM Medellín · 2026
