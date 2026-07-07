# ChaosLab 🔬

> Simulador educativo de sistemas distribuidos y *chaos engineering*. Definís tu arquitectura
> en YAML, se levanta una simulación **en memoria** (sin infraestructura real) e inyectás fallos
> —latencia, caídas, particiones de red— para ver cómo responde el sistema y cómo lo defienden
> los patrones de resiliencia. Todo **determinista**: mismo `seed` → mismo resultado.

La versión de aprendizaje de Chaos Monkey: barata, visible y reproducible. Y un paso más allá —
declarás tus SLOs y ChaosLab **busca solo** el fallo que los rompe.

![Dashboard de ChaosLab: order-api colapsa al 83% de éxito ante un CrashFault; resilient-order-api, con CircuitBreaker, mantiene el 100%](docs/chaoslab-demo.gif)

> El dashboard corriendo dos escenarios con el **mismo** `CrashFault` en una réplica: primero
> `order-api` (sin defensas) cae al **83%** de éxito y su latencia p95 se dispara; luego
> `resilient-order-api` (con **CircuitBreaker** en el balanceador) **degrada en vez de colapsar** y
> mantiene el **100%**. Mismo `seed` → mismo resultado.

## Qué hace

- **Simula** una topología de microservicios (colas, bases de datos, balanceadores) con un motor de
  **eventos discretos** propio y reloj virtual — miles de requests en milisegundos, sin red ni contenedores.
- **Inyecta caos**: latencia, caídas y particiones de red, programadas en el YAML o desde la CLI.
- **Defiende** con patrones de resiliencia: Retry, Timeout y CircuitBreaker.
- **Verifica hipótesis de estado estable** (SLOs): cada corrida da un veredicto **PASA/FALLA**.
- **Mide resiliencia**: disponibilidad, MTTR, curva de degradación y tiempo de detección.
- **Busca el fallo que te rompe** (mini-DST): explora automáticamente los escenarios y reporta el
  **mínimo** que refuta tu hipótesis.
- **Visualiza** todo en un dashboard web (grafo por salud, replay de la línea de tiempo, gráficas).

## Probalo en 1 minuto

Requiere **JDK 21** (para compilar *y* ejecutar; con un JDK por defecto < 21 el build usa Maven
Toolchains, pero el jar corre sobre Java 21).

```bash
mvn -DskipTests package                                            # compila el jar
java -jar target/chaoslab-0.1.0-SNAPSHOT.jar                       # dashboard: http://localhost:8080
java -jar target/chaoslab-0.1.0-SNAPSHOT.jar run examples/order-api.yaml   # una corrida por CLI
```

En el dashboard: elegí una topología (o **pegá/subí tu propio YAML**), sumá un fallo opcional
(botón *Tumbar api-1* o un spec como `crash:api-1:20:15`) y dale **Correr**. Compará `order-api`
con `resilient-order-api` para ver el CircuitBreaker en acción.

## El "momento ajá": caos vs. resiliencia

Misma topología y mismo `CrashFault` en una réplica; lo único que cambia es un CircuitBreaker
en el balanceador:

```bash
java -jar target/chaoslab-0.1.0-SNAPSHOT.jar run examples/order-api.yaml            # sin breaker
java -jar target/chaoslab-0.1.0-SNAPSHOT.jar run examples/resilient-order-api.yaml  # con breaker
```

| Réplica caída | Sin CircuitBreaker | Con CircuitBreaker |
|---|---|---|
| Tasa de éxito | ~83 % | **100 %** |

Con el breaker, el balanceador deja de enrutar a la réplica caída y el sistema **degrada en vez
de colapsar**.

## Hipótesis de estado estable (el oráculo)

El principio formal de Chaos Engineering: declarás qué es "operación normal" con invariantes
medibles (SLOs) y el experimento intenta **refutarlos**. Se declaran en el YAML y la corrida
devuelve un veredicto **PASA/FALLA**:

```yaml
steady_state:
  - { metric: success_rate,   comparison: ">=", threshold: 0.99 }
  - { metric: p95_latency_ms, comparison: "<=", threshold: 400 }
```

- **Métricas:** `success_rate`, `p50/p95/p99/max_latency_ms`, `completed/failed/generated_requests`.
- **Comparadores:** `>=`, `<=`, `>`, `<`, `==` (o `gte`, `lte`, `gt`, `lt`, `eq`).
- En el **dashboard** aparece un badge verde/rojo con el desglose por invariante.
- En la **CLI**, si la hipótesis se refuta el proceso sale con **código 1** (listo para un gate de
  CI: la resiliencia se vuelve un test que rompe el build si regresa).

Las dos topologías del "momento ajá" traen la misma hipótesis (`success_rate >= 0.99`):
`order-api` la **refuta** (~0.83) y `resilient-order-api` la **cumple** (~1.0).

## Métricas de resiliencia

Cada corrida reporta métricas estandarizadas, derivadas de la línea de tiempo, que convierten el
"momento ajá" en números comparables:

| Métrica | Qué mide |
|---|---|
| **Disponibilidad** | fracción del tiempo sin ningún componente caído |
| **MTTR** | tiempo medio de recuperación de los episodios de deterioro |
| **Éxito en el peor segundo** | profundidad de la degradación de cara al usuario |
| **Detección** | cuándo se detectó el fallo (apertura del CircuitBreaker) |

El contraste queda explícito: ante el mismo crash, ambas topologías pierden disponibilidad (~75 %),
pero con breaker el **éxito del peor segundo es 100 %** (vs. ~49 % sin él) y el fallo se **detecta en 1 s**.

## Búsqueda de caos (mini-DST): "encontrá el escenario que te rompe"

En vez de reproducir un escenario que vos definís, ChaosLab **busca solo** el conjunto de fallos que
refuta tu hipótesis, y lo **minimiza** al más chico que aún la rompe — la idea de la *Deterministic
Simulation Testing* (FoundationDB / Antithesis / TigerBeetle), a escala educativa:

```bash
java -jar target/chaoslab-0.1.0-SNAPSHOT.jar search examples/resilient-order-api.yaml
```

```
veredicto: hipótesis REFUTADA — escenario mínimo hallado
  semilla: 0
  fallos (1): crash orders-queue
  éxito en el peor segundo: 0.0%
```

El diseño resiliente cubre las réplicas `api` con un CircuitBreaker, pero la búsqueda descubre que
la **cola y la base de datos son puntos únicos de fallo** (SPOF): un solo crash de `orders-queue`
tira el SLO. Como todo es determinista, el contraejemplo es **reproducible**. Sale con **código 1**
si halla un contraejemplo (gate de resiliencia para CI). Opciones: `--seeds N`, `--budget N`.

## Uso

### CLI

```bash
# Correr una simulación (los fallos definidos en el YAML se inyectan solos)
java -jar target/chaoslab-0.1.0-SNAPSHOT.jar run examples/order-api.yaml

# Inyectar fallos por CLI (repetible): crash:<target>:<atSeg>[:<durSeg>],
# latency:<target>:<atSeg>:<durSeg>:<extraMs>, partition:<a,b>:<c>:<atSeg>:<durSeg>
java -jar target/chaoslab-0.1.0-SNAPSHOT.jar run examples/order-api.yaml --fault crash:api-1:10:20

# Buscar el escenario mínimo que refuta la hipótesis del YAML
java -jar target/chaoslab-0.1.0-SNAPSHOT.jar search examples/resilient-order-api.yaml
```

### Dashboard

Sin argumentos, arranca en `http://localhost:8080`. Podés correr una topología de ejemplo **o
cargar la tuya**: desplegá *"…o usá tu propio YAML"*, pegá o subí tu archivo, y corré.

### Docker

```bash
docker compose up --build   # dashboard en http://localhost:8080
```

Imagen multi-etapa (compila con Maven + JDK 21, corre sobre un JRE 21); no necesitás Java 21 local.

## Topologías de ejemplo

En [`examples/`](examples) (para la CLI) y en el catálogo del dashboard:

| Topología | Qué demuestra |
|---|---|
| `order-api` | sin defensas: una réplica caída arrastra el éxito al ~83 % |
| `resilient-order-api` | el CircuitBreaker mantiene el 100 % ante el mismo fallo |
| `slow-database` | saturación (Ley de Little): la DB no da abasto → fallos por capacidad |
| `network-partition` | *split-brain*: la partición aísla un grupo del resto |

## Arquitectura

Monolito modular con **Clean Architecture**. La dependencia **siempre apunta hacia adentro**
(verificado por **ArchUnit** en CI):

```
Infrastructure  →  Application  →  Domain
   (adaptadores)   (casos de uso)   (motor puro: cero Spring/web/YAML)
```

```
src/main/java/com/chaoslab/
├── domain/            ← núcleo, cero dependencias externas
│   ├── engine/        ← Clock, EventQueue, SimulationEngine (eventos discretos), eventos sellados
│   ├── topology/      ← Component, Service, Queue, Database, LoadBalancer, TopologyGraph
│   ├── fault/         ← Fault (sellado): LatencyFault, CrashFault, NetworkPartition
│   ├── resilience/    ← CircuitBreaker, ResiliencePolicy (Retry / Timeout)
│   ├── workload/      ← generador de carga (Poisson, sembrado)
│   ├── metrics/       ← MetricsCollector, SimulationReport, ResilienceMetrics, percentiles, timeline
│   ├── hypothesis/    ← hipótesis de estado estable (SLOs) y su veredicto PASA/FALLA
│   └── search/        ← búsqueda de caos determinista (mini-DST) + minimización
├── application/       ← casos de uso: RunSimulationUseCase, ChaosSearchUseCase
└── infrastructure/    ← adaptadores: yaml (loader), cli (Picocli), web (dashboard), config
```

**Determinismo (atributo de calidad #1):** un único `Random` sembrado con el `seed`, tiempo virtual
por eventos discretos, sin hilos reales ni `Thread.sleep`. Invariante de test: *mismo seed → mismo
resultado*. Es lo que hace posible la búsqueda de caos reproducible.

**Dashboard sin WebSocket:** como la simulación es determinista y se computa completa, el motor
produce una **línea de tiempo** de snapshots y el cliente la **reproduce** (replay). Empujar por
WebSocket solo enviaría datos ya calculados. Chart.js va **empaquetado localmente** (sin CDN).

### Decisiones de arquitectura (ADRs)

- [0001 — Motor de eventos discretos determinista](docs/adr/0001-motor-eventos-discretos-determinista.md)
- [0002 — Dashboard por replay de línea de tiempo (no WebSocket)](docs/adr/0002-timeline-replay-vs-websocket.md)
- [0003 — Monolito modular con Clean Architecture](docs/adr/0003-monolito-modular-clean-architecture.md)

## Stack

- **Java 21 LTS** + **Spring Boot 3.5** (solo en las capas externas; el dominio es puro)
- Motor de **eventos discretos** propio · **SnakeYAML** (`SafeConstructor`) · **Picocli** (CLI)
- Frontend HTML/CSS/JS vanilla + **Chart.js** (vendorizado) · grafo SVG + replay en el cliente
- UI con el lenguaje de **IBM Carbon Design System** (tema Gray 100): tokens, IBM Plex, foco accesible
- **JUnit 5 + AssertJ** · **Checkstyle** · **SpotBugs** · **JaCoCo** · **ArchUnit** · **OWASP Dependency-Check**
- **GitHub Actions** (CI) · **Docker** (entorno reproducible)

## Build y calidad

```bash
mvn verify              # compila + tests + Checkstyle + SpotBugs + ArchUnit + gate JaCoCo
mvn test                # solo tests
mvn -Psecurity verify   # + OWASP Dependency-Check (SCA)
```

Tras `mvn verify`, el reporte de cobertura queda en `target/site/jacoco/index.html`.

| Gate | Herramienta | Umbral |
|---|---|---|
| Estilo | Checkstyle | 0 violaciones (warning+) |
| Bugs estáticos | SpotBugs | 0 (effort Max, threshold Medium) |
| Arquitectura | ArchUnit | dominio puro + dependencia hacia adentro |
| Cobertura del dominio | JaCoCo | ≥ 70 % líneas en `com.chaoslab.domain` |
| Vulnerabilidades de dependencias | OWASP Dependency-Check | falla si CVSS ≥ 7 (perfil `security`) |

El análisis de dependencias (OWASP) corre en un workflow nocturno propio
([.github/workflows/security.yml](.github/workflows/security.yml)) porque la descarga de la base
NVD es lenta; los PRs y pushes corren solo lint + tests para feedback rápido.

**Seguridad:** el YAML es **entrada externa no confiable** — se valida esquema, rangos y
referencias, con límites duros (máx. componentes, duración, requests/seg, invariantes) y sin
deserialización de tipos arbitrarios (`SafeConstructor`). Nunca se interpreta como código.

## Requisitos

- JDK 21 (Temurin recomendado). Si tu JDK por defecto es < 21, el build usa **Maven Toolchains**
  para compilar con JDK 21 (ver `~/.m2/toolchains.xml`).
- Maven 3.9+
- `.mvn/jvm.config` fuerza `-Djava.net.preferIPv4Stack=true` para evitar fallos de resolución de
  Maven Central en entornos con IPv6 roto; es inocuo donde IPv6 funciona.

## Estado

**MVP (v1) completo:**

- ✅ **Fase 0 — Fundación:** Clean Architecture, gates de calidad y CI.
- ✅ **Fase 1 — Motor:** eventos discretos deterministas, 4 componentes, YAML + CLI.
- ✅ **Fase 2 — Caos y resiliencia:** 3 fallos (latencia, caída, partición) y 3 patrones
  (Retry, Timeout, CircuitBreaker).
- ✅ **Fase 3 — Dashboard:** grafo por salud, replay de la línea de tiempo, gráficas, subir tu YAML.
- ✅ **Fase 4 — Calidad de producción:** ADRs, Docker, ArchUnit, topologías de demo.

**v2 — oráculo + búsqueda (frontera DST / Chaos Engineering 2.0):**

- ✅ Hipótesis de estado estable (SLOs) con veredicto PASA/FALLA.
- ✅ Métricas de resiliencia estandarizadas (disponibilidad, MTTR, degradación, detección).
- ✅ Búsqueda de caos determinista (mini-DST) con minimización de escenarios.

**Backlog:** gate de resiliencia en CI, más patrones (Bulkhead, Rate-Limiter, Retry con jitter),
más fallos (clock skew, degradación parcial), deploy público, mutation testing (PIT).

## Licencia

[MIT](LICENSE) · Sebastian Ramirez · ITM Medellín · 2026
