# Directrices de Ingeniería de Software — Knowledge Base

> **Qué es esto:** un único documento de referencia que consolida todos los estándares
> que sigo en cualquier proyecto: arquitectura, Git, CI/CD, QA, seguridad y UX/UI frontend.
> Está escrito para ser consumido por Claude (en chat, Claude Code o Cowork) como contexto
> persistente y reutilizable. Cada sección es **operativa y copy-paste-ready**, no teoría suelta.
>
> **Fuentes consolidadas:** 4 briefs propios (Git, QA, Seguridad, UX/UI) + 5 documentos
> académicos de la biblioteca (arquitectura basada en datos, diseño por componentes,
> IA generativa para arquitectura, ecosistema CI/CD, fundamentos de arquitectura de software).
>
> **Última actualización:** 2026-06 · v1

---

## Cómo usar este documento con Claude

Tres formas, de menor a mayor automatización:

1. **Como `CLAUDE.md` del proyecto** (recomendado para Claude Code/Cowork): copia este archivo
   —o un extracto— a la raíz del repo como `CLAUDE.md`. Claude Code lo lee automáticamente al
   inicio de cada sesión. Si el repo es grande, deja en `CLAUDE.md` solo el índice + las reglas
   duras, y mantén este archivo completo en `docs/DIRECTRICES_INGENIERIA.md` referenciado desde ahí.
2. **Como documento adjunto/contexto** en una conversación: súbelo y pídeme "aplica las
   directrices del documento a este código/PR/diseño".
3. **Como RAG vectorial** (solo si lo necesitas de verdad): este documento ya está dividido por
   secciones con anclas. Para un RAG real harían falta embeddings + vector store (Chroma/pgvector),
   chunking por sección y un retriever. Para *estándares que Claude debe seguir*, un archivo de
   contexto estructurado como este es más directo y barato que un RAG —un RAG tiene sentido cuando
   el corpus es grande, cambiante o se consulta por similitud, no para reglas fijas que quieres
   que se apliquen siempre.

> **Regla transversal para Claude:** todos los cambios se hacen **directamente en los archivos
> del proyecto**, nunca en worktrees o copias. Verificar el resultado (compilar/test/navegador)
> tras cada cambio. Ante conflicto entre directrices, prioridad: **Seguridad > QA/Correctitud >
> Arquitectura > UX/UI > estilo**.

### Índice

1. [Principios de Arquitectura](#1-principios-de-arquitectura)
2. [Git Workflow](#2-git-workflow)
3. [CI/CD](#3-cicd)
4. [QA y Testing](#4-qa-y-testing)
5. [Seguridad / DevSecOps](#5-seguridad--devsecops)
6. [UX/UI Frontend](#6-uxui-frontend)
7. [Checklists maestras por fase](#7-checklists-maestras-por-fase)
8. [Referencias](#8-referencias)

---

## 1. Principios de Arquitectura

Base teórica destilada de los documentos de arquitectura de la biblioteca (Richards & Ford,
*Fundamentals of Software Architecture*; Valdes, *Diseño basado en componentes*; Ortega Ovalle,
*Arquitecturas basadas en datos*; Goncalves & Burgos, *IA generativa para arquitectura*).

### 1.1 La ley que gobierna todo

**Todo en arquitectura es un trade-off.** No existe la "mejor" arquitectura, solo la menos mala
para un contexto y unos atributos de calidad concretos. Antes de elegir un estilo, identificar
qué atributos de calidad (ISO/IEC 25010) son *críticos* para ESTE proyecto — no todos pesan igual.

- En logística/distribución → **fiabilidad y rendimiento** primero.
- En app pública → **usabilidad y seguridad** primero.
- En API interna → **mantenibilidad y observabilidad** primero.

Documentar la decisión y su justificación (un ADR breve por decisión importante: contexto,
opciones, decisión, consecuencias).

### 1.2 Principios estructurales no negociables

- **Clean Architecture + SOLID + DDD** como base por defecto (mi stack habitual: Java/Spring,
  TypeScript/Fastify). La lógica de dominio no depende de frameworks, BD ni transporte.
- **Diseño basado en componentes**: cada componente es una unidad independiente y reemplazable,
  con responsabilidad clara, interfaces explícitas y restricciones de interacción. Un componente
  no es "una clase" — es una frontera con contrato.
- **Bajo acoplamiento, alta cohesión**: cada módulo hace una cosa y la hace bien; cambiar un
  módulo no debe romper otro. Es la métrica que decide si el código será mantenible o un problema.
- **Interfaces explícitas, estables y documentadas**: son el mecanismo principal para controlar
  la interacción y limitar el acoplamiento.

### 1.3 Cinco propiedades de los sistemas modernos (basados en datos)

Todo sistema serio debe diseñarse integrando estos cinco componentes, no como añadidos:

| Propiedad | Qué significa en la práctica |
|---|---|
| **Modularidad** | Componentes que evolucionan de forma autónoma; despliegue independiente |
| **Resiliencia** | Tolerancia a fallos; el sistema mantiene continuidad operativa ante caídas |
| **Observabilidad** | El sistema expone su estado interno: logs, métricas, traces |
| **Gobernanza** | Control de integridad, seguridad y coherencia de datos y accesos |
| **Automatización** | Procesos que se ejecutan solos y se auto-optimizan (CI/CD, scaling) |

### 1.4 Selección de estilo arquitectónico

- **Monolítico (modular)**: válido y a menudo correcto para empezar. Baja complejidad operativa.
  No es deuda técnica si está bien modularizado internamente.
- **En capas**: simple, claro; cuidado con que la lógica se filtre entre capas.
- **Orientado a servicios / microservicios**: solo cuando hay necesidad real de despliegue,
  escalado o equipos independientes. Cada servicio: independiente, desplegable y testeable por
  separado. Mide coupling/cohesión entre servicios con análisis estático.
- **Basado en eventos (Kafka/colas)**: para desacoplar servicios y absorber picos de carga;
  introduce complejidad (idempotencia, orden, esquemas) — evaluar antes de adoptar.

> **Regla práctica:** no introducir microservicios, event-driven o multicloud "porque sí".
> Cada incremento de complejidad estructural debe pagarse con un atributo de calidad concreto
> que el monolito no podía dar.

### 1.5 Si el proyecto integra IA/LLM (RAG, agentes, chatbots)

- Pipeline RAG: vector store + retriever + chunking por sección; el objetivo es **reducir
  alucinaciones y dar citabilidad**, no reemplazar el razonamiento.
- Orquestación multi-agente (LangGraph/equivalente): un rol por nodo, un supervisor que enruta,
  un nodo unificador que consolida la respuesta final. Persistir histórico (thread_id) para
  trazabilidad.
- **Ver §5.6 para los riesgos de seguridad específicos de LLM** — las prácticas web no bastan.

---

## 2. Git Workflow

Flujo empresarial probado en `proyecto_itm_bienestar`. Aplica a cualquier repo con CI/CD.

### 2.1 Estructura de ramas

```
main           ← producción, siempre estable, NUNCA commits directos
  └── develop  ← integración, base de todo el trabajo
        ├── feature/nombre-descriptivo   ← nueva funcionalidad
        ├── fix/nombre-descriptivo       ← corrección de bug
        └── hotfix/nombre-descriptivo    ← fix urgente desde main
```

**Regla de oro:** nunca commit directo a `main` ni a `develop`. Todo cambio entra por Pull Request
después de pasar CI.

### 2.2 Branch protection (GitHub Rulesets) para `main`

| Opción | Valor |
|--------|-------|
| Restrict deletions | ✅ |
| Require a pull request before merging | ✅ |
| Required approvals | 1 (en equipos) |
| Block force pushes | ✅ |
| Require status checks to pass | ✅ |
| Required checks | `lint-and-typecheck`, `test` |

### 2.3 Flujo completo

```bash
# Iniciar feature desde develop actualizado
git checkout develop && git pull origin develop
git checkout -b feature/nombre-descriptivo
git add src/modules/mi-modulo/
git commit -m "feat(mi-modulo): descripción concisa del cambio"
git push -u origin feature/nombre-descriptivo

# Abrir PR hacia develop
gh pr create --title "feat(mi-modulo): descripción" --base develop \
  --body "## Qué hace\n- ...\n## Cómo probar\n- ..."

# CI verde → mergear
gh pr merge <número> --squash --delete-branch
```

### 2.4 Rebase para mantener la rama al día

```bash
git fetch origin
git checkout feature/mi-rama
git rebase origin/develop
git push origin feature/mi-rama --force-with-lease
```

- Usar **`--force-with-lease`**, nunca `--force` (falla si alguien más pusheó → seguro).
- Hacer rebase: antes de abrir PR, cuando el PR lleva días abierto, o cuando CI falla por un fix
  ya mergeado en develop.

### 2.5 Conventional Commits

`<tipo>(<scope>): <descripción en imperativo>`

`feat` · `fix` · `refactor` · `test` · `docs` · `chore` · `ci` · `perf`

```
feat(health): implement Health module with contextual tips
fix(auth): add explicit user properties to register response schema
fix(ci): add @vitest/coverage-v8 missing dependency
```

### 2.6 Checklist antes de abrir PR

- [ ] La rama viene de `develop`, no de `main`
- [ ] Commits siguen Conventional Commits
- [ ] No hay `console.log` de debug
- [ ] `npm test`, `npm run typecheck` y `npm run lint` pasan localmente
- [ ] El PR apunta a `develop`
- [ ] Orden de merge al cierre de ciclo: `feature/* → develop` (uno a uno, esperando CI) → `develop → main`

---

## 3. CI/CD

Consolida el `ci.yml` base del workflow de Git + el ecosistema CI/CD de la tesis de Zavala
(control de versiones → análisis de código → artefactos → seguridad → contenedores → monitorización).

### 3.1 Anatomía del pipeline (orden canónico)

```
Commit
  → Lint + Typecheck
  → Build
  → Unit + Integration tests (con coverage)
  → SAST + Secret scanning            (ver §5)
  → SCA de dependencias + SBOM        (ver §5)
  → Deploy a staging
  → DAST sobre staging                (ver §5)
  → Gate de release
```

Cada etapa puede **detener** el pipeline. Política de gate madura: *bloquear despliegue si hay
artefacto sin firmar, o vulnerabilidad con CVSS ≥ 7 no justificada en un VEX*.

### 3.2 GitHub Actions — estructura mínima

```yaml
name: CI
on:
  push: { branches: [main, develop] }
  pull_request: { branches: [main, develop] }
jobs:
  lint-and-typecheck:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: '20', cache: 'npm' }
      - run: npm ci
      - run: npm run typecheck
      - run: npm run lint
  test:
    runs-on: ubuntu-latest
    needs: lint-and-typecheck      # tests solo si lint pasa
    services:
      postgres:
        image: postgres:16-alpine
        env: { POSTGRES_DB: mi_proyecto_test, POSTGRES_USER: postgres, POSTGRES_PASSWORD: postgres }
        ports: ['5432:5432']
        options: --health-cmd pg_isready --health-interval 10s --health-timeout 5s --health-retries 5
      redis:
        image: redis:7-alpine
        ports: ['6379:6379']
        options: --health-cmd "redis-cli ping" --health-interval 10s --health-timeout 5s --health-retries 5
    env:
      NODE_ENV: test
      DATABASE_URL: postgresql://postgres:postgres@localhost:5432/mi_proyecto_test
      REDIS_URL: redis://localhost:6379
      JWT_SECRET: test_secret_at_least_32_characters_long
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: '20', cache: 'npm' }
      - run: npm ci
      - run: npx prisma generate
      - run: npx prisma db push --accept-data-loss
      - run: npm test -- --coverage
```

### 3.3 Componentes del ecosistema (qué herramienta para qué)

| Función | Herramientas de referencia |
|---|---|
| Control de versiones | Git + GitHub/GitLab |
| Servidor CI | GitHub Actions / GitLab Runner |
| Análisis de código | SonarQube (calidad + seguridad estática) |
| Repositorio de artefactos | Nexus / registry de contenedores |
| Análisis de seguridad | SCA + SAST + secret scanning (ver §5) |
| Contenedores y orquestación | Docker + (Kubernetes si la escala lo justifica) |
| Monitorización | Prometheus + Grafana; LangSmith si hay agentes LLM |
| Despliegue | On-premise / nube / híbrido según contexto y presupuesto |

> Práctica habitual mía: Docker + Railway para despliegue; cuidar el coste de los entornos de
> **staging** (suelen ser el principal driver de gasto — apagarlos cuando no se usan).

### 3.4 Infraestructura

- **On-premise**: control total, coste fijo, mayor mantenimiento.
- **Nube**: elasticidad y pago por uso; vigilar coste de entornos no productivos.
- **Híbrida**: datos sensibles on-premise, cómputo elástico en nube.

---

## 4. QA y Testing

Brief de QA propio, respaldado por investigación peer-reviewed. **La calidad se diseña desde el
principio, no se añade al final.** Automatizar testing aumenta la detección de defectos ~50% y
reduce el tiempo de testing ~60% (Neelapu, 2025).

### 4.1 El marco: ISO/IEC 25010

Ocho dimensiones: **Funcionalidad, Fiabilidad, Usabilidad, Eficiencia de rendimiento,
Mantenibilidad, Seguridad, Portabilidad, Compatibilidad.** Al inicio del proyecto, identificar
cuáles son críticas para ESE contexto (ver §1.1).

### 4.2 Métricas que importan

**Proceso (durante desarrollo):**

| Métrica | Umbral / uso |
|---|---|
| Defect Detection Rate | Efectividad del testing |
| Defect Density (def/KLOC) | Zonas problemáticas del código |
| Test Coverage | **Mínimo 70–80% en módulos críticos** |
| Mean Time to Detect (MTTD) | Agilidad del QA |
| Defect Escape Rate | El número que nunca debe subir |

**Producto (sistema vivo):**

| Métrica | Umbral de referencia |
|---|---|
| Response Time | < 2s para acciones normales |
| Uptime / Availability | ≥ 99.5% en producción |
| Error Rate | < 1% en condiciones normales |
| User Satisfaction (SUS) | SUS ≥ 68 = aceptable, ≥ 80 = bueno |

### 4.3 Shift-left y pirámide de automatización

Detectar un bug en desarrollo cuesta una fracción de detectarlo en producción.

```
        /\        E2E (pocos, lentos, costosos)
       /--\
      /    \      Integration
     /------\
    /        \    Unit (muchos, rápidos, baratos)  ← base más ancha
   /__________\
```

Si hay más E2E que unit tests, la pirámide está invertida y el proceso es frágil.

Frameworks por capa: **Vitest/Jest/JUnit/pytest** (unit), **Postman/REST Assured** (API),
**Playwright/Selenium** (E2E web).

### 4.4 Tipos de testing — cuándo

| Tipo | Cuándo |
|---|---|
| Unit | En cada función/módulo nuevo |
| Integration | Al conectar módulos/servicios |
| System | Antes de cada release |
| UAT | Antes de entregar al cliente |
| Regression | Después de cada cambio |
| Performance | En sistemas con carga alta |
| Security | Cualquier sistema con datos de usuarios (ver §5) |

### 4.5 Clasificación de defectos

| Severidad | Acción |
|---|---|
| **Critical** (falla/pérdida de datos) | Bloquea el release. Corrección inmediata |
| **High** (funcionalidad principal rota) | Resolver antes del próximo release |
| **Medium** (funcionalidad secundaria) | Siguiente sprint |
| **Low** (cosmético) | Backlog |

### 4.6 Calidad estructural y tests en paralelo

- **Coupling/cohesión**: medir con análisis estático (SonarQube/CodeClimate). Crítico en
  microservicios.
- **Tests con BD compartida** (Vitest corre archivos en paralelo): cada test file limpia **solo
  sus** tablas en `beforeEach`, siempre `redis.flushdb()`, y crea usuarios con email único
  (`test.${Date.now()}@empresa.com`). Nunca `prisma.user.deleteMany()` global → causa FK
  violations en paralelo.
- **Response schemas (Fastify/`fast-json-stringify`)**: declarar cada propiedad explícitamente;
  `{ type: 'object' }` sin `properties` serializa `{}`.

---

## 5. Seguridad / DevSecOps

Brief propio basado en OWASP Top 10:2025, NIST SSDF (SP 800-218) y OWASP GenAI Security Project.
**La seguridad no es una fase, es una propiedad del diseño** (secure-by-design). Los ataques a la
cadena de suministro **más que se duplicaron en 2025**.

### 5.1 Tres marcos complementarios

- **OWASP Top 10:2025** — "¿qué me va a romper?" (punto de partida obligatorio).
- **NIST SSDF (SP 800-218)** — "¿cómo organizo el proceso?" (PO, PS, PW, RV).
- **OWASP ASVS** — "¿qué nivel de rigor aplico?" (nivel 1 básico, 2 datos sensibles, 3 crítico).
  Decidir el nivel ASVS al inicio según la criticidad de los datos.

### 5.2 OWASP Top 10:2025 — mitigación esencial

| # | Riesgo | Mitigación |
|---|---|---|
| A01 | Broken Access Control (+SSRF) | Denegar por defecto; autorización en el **servidor** en cada petición |
| A02 | Security Misconfiguration | Config endurecida; sin cuentas/servicios por defecto; cabeceras de seguridad |
| A03 | **Software Supply Chain** *(nuevo)* | SBOM + SCA + procedencia firmada |
| A04 | Cryptographic Failures | TLS en tránsito + cifrado en reposo; nada de algoritmos obsoletos |
| A05 | Injection | Consultas parametrizadas; validación y sanitización de toda entrada |
| A06 | Insecure Design | Threat modeling en diseño; patrones seguros desde el inicio |
| A07 | Authentication Failures | MFA; sesiones robustas; protección fuerza bruta / credential stuffing |
| A08 | Data Integrity Failures | Verificar integridad; firmas; pipelines CI/CD protegidos |
| A09 | Logging & Alerting Failures | Logging suficiente + alertas accionables (sin logs = sin detección) |
| A10 | **Mishandling Exceptional Conditions** *(nuevo)* | "fail closed", no "fail open"; no filtrar errores sensibles |

### 5.3 Shift-left: gates en el pipeline

```
Commit → SAST + Secret scanning → Build → SCA (dependencias) → Deploy staging → DAST → Gate
```

| Tipo | Cuándo | Detecta |
|---|---|---|
| **SAST** | Cada commit/build | Vulnerabilidades en tu código |
| **DAST** | Tras deploy a staging | Fallos en ejecución (XSS, config) |
| **SCA** | Cada build | CVEs en dependencias de terceros |
| **Secret scanning** | Cada commit (pre-commit ideal) | API keys, tokens, contraseñas filtradas |
| **Pentesting** | Antes de releases importantes | Lógica de negocio que la automatización no ve |

Herramientas: SonarQube (SAST), OWASP ZAP (DAST), Burp Suite (pentest manual).
**Secrets detectados en commits debe tender a cero** — cada uno es un incidente potencial.

### 5.4 Threat modeling (antes de construir lo sensible)

- **STRIDE**: Spoofing, Tampering, Repudiation, Information disclosure, Denial of service,
  Elevation of privilege — componente por componente.
- **Risk storming**: sesión colaborativa que puntúa riesgos por *impacto × probabilidad*.
- Principios fijados aquí: **defensa en profundidad** (nunca un único control),
  **mínimo privilegio**, **zero trust** (verificar cada petición; credenciales de corta duración + MFA).

### 5.5 Cadena de suministro (mínimos no opcionales)

- **SBOM** (SPDX/CycloneDX, generadores como Syft): inventario de cada componente. Ante un nuevo
  Log4Shell, te dice en minutos si estás expuesto.
- **SCA**: escaneo de dependencias vs CVEs en cada build; priorizar por explotabilidad real (VEX),
  no solo por CVSS.
- **Procedencia firmada** (SLSA nivel 2–3 + Sigstore/cosign): garantiza que el artefacto salió de
  tu pipeline sin manipular.

### 5.6 Seguridad en apps con IA / LLM (OWASP Top 10 for LLM 2025)

Si el proyecto integra LLMs/RAG/agentes, las prácticas web **no bastan**:

| # | Riesgo | En una frase |
|---|---|---|
| LLM01 | Prompt Injection | Entrada que hace al modelo ignorar sus instrucciones (#1) |
| LLM02 | Sensitive Information Disclosure | El modelo filtra datos sensibles |
| LLM03 | Supply Chain | Modelos/datasets/plugins de terceros comprometidos |
| LLM04 | Data & Model Poisoning | Manipulación de datos de entrenamiento/embeddings |
| LLM05 | Improper Output Handling | Confiar en la salida del modelo sin validar |
| LLM06 | Excessive Agency | Dar al modelo más permisos/autonomía de los necesarios |
| LLM07 | System Prompt Leakage | Fuga del prompt de sistema |
| LLM08 | Vector & Embedding Weaknesses | Debilidades en el vector store del RAG |
| LLM09 | Misinformation | Alucinaciones con confianza |
| LLM10 | Unbounded Consumption | DoS / "Denial of Wallet" |

Defensas: validar entrada y **sanitizar salida en cada frontera**, **mínimo privilegio** para
cada capacidad del modelo, supervisión humana en decisiones de alto impacto. En sistemas
agénticos el radio de impacto de un prompt injection se dispara (el modelo ejecuta acciones).

### 5.7 Web — lo no negociable

TLS en todo el tráfico (100%) · validación de entrada en **servidor** · consultas parametrizadas ·
sesiones con expiración/rotación + cookies `HttpOnly`/`Secure` · tokens anti-CSRF ·
cabeceras (`Content-Security-Policy`, `Strict-Transport-Security`, `X-Content-Type-Options`) ·
rate limiting · no concentrar todo el control en un único API Gateway sin defensa detrás.

### 5.8 Severidad (CVSS)

Critical 9.0–10.0 (bloquea release) · High 7.0–8.9 (antes del próximo release) ·
Medium 4.0–6.9 (siguiente sprint) · Low 0.1–3.9 (backlog). Priorizar por **explotabilidad real
en tu contexto**, no solo por el número.

---

## 6. UX/UI Frontend

Brief propio basado en investigación peer-reviewed (Cognitive Load Theory, HCD/UCD, benchmarking
de UX). Aplicar los principios **en este orden de prioridad**:

### 6.1 Los 9 principios (reglas duras)

1. **Carga cognitiva** — Máx. 3–4 párrafos sin separación visual; regla 7±2 por lista; eliminar
   decoración que no comunica significado; consistencia total en radio/color/tipo/spacing; feedback
   de hover y `:focus-visible` en todo elemento interactivo.
2. **Jerarquía visual** — Una sola cosa más importante por sección (visible en <1s); escala
   tipográfica de ≥3 niveles; el **color de acento solo** en elementos interactivos, estados y
   métricas clave (nunca en texto de cuerpo); máx. 2 familias tipográficas.
3. **Navegación predecible** — Estado activo visible (scroll spy + `aria-current="true"`);
   `scroll-padding-top` ≥ altura del header sticky; labels claros; máx. 6 ítems; skip link.
4. **Tipografía y legibilidad** — Ancho de cuerpo 50–75ch; contraste ≥ 4.5:1 (texto normal),
   ≥ 3:1 (grande); `line-height` 1.6–1.75 cuerpo / 1.0–1.15 headings; mínimo **16px** en mobile;
   fuentes con `display=swap`.
5. **CTAs** — Un solo CTA primario por sección; texto = acción específica ("Ver código en GitHub",
   no "Ver más"); feedback en hover 150–200ms; CTA principal visible sin scroll.
6. **Confianza** — Números concretos > afirmaciones abstractas ("4 proyectos · 140+ tests");
   señal de disponibilidad en el hero; enlazar evidencia real (GitHub/LinkedIn/CV/demo); contacto
   sin fricción (`mailto:` funcional).
7. **Responsividad / mobile-first** — Breakpoints 1024/640; touch targets ≥ **44×44px**; grids que
   colapsan a 1 columna (`repeat(auto-fit, minmax(280px, 1fr))`); `overflow-wrap: break-word` en
   textos largos; `width`/`height` explícitos en `<img>` (evita CLS); **sin** `loading="lazy"`
   above-the-fold.
8. **Consistencia del sistema** — Variables CSS para todo lo que se repite; escala de spacing
   (4/8/12/16/24/32/48/64); cards del mismo tipo = mismo estilo base; en tema oscuro no mezclar
   fondos claros (usar tinte de acento ≤ 8% opacidad); easing y duraciones coherentes +
   `prefers-reduced-motion`.
9. **Accesibilidad como UX** — HTML semántico (`<header>`, `<main>`, `<nav>`, `<section id>`);
   `alt` en todas las imágenes (`alt=""` si decorativa); orden lógico del DOM; JSON-LD `Person`
   con `sameAs` (LinkedIn/GitHub); meta tags completos (title, description, Open Graph, Twitter Card).

### 6.2 Concepto clave

Buen UI = **mínima carga cognitiva exógena** (la que causa el diseño, evitable) + **máxima
germana** (esfuerzo útil de comprensión). Toda decisión de diseño debe preguntarse: ¿este elemento
añade carga exógena o germana? La inconsistencia visual es una violación de usabilidad de
severidad alta (3–4), no "estética distinta".

---

## 7. Checklists maestras por fase

Unifica los checklists de QA, Seguridad y UX/UI para usar como gate de fase en cualquier proyecto.

### Planificación
- [ ] Identificar dimensiones ISO 25010 prioritarias (§1.1, §4.1)
- [ ] Definir métricas + umbrales de aceptación (QA y seguridad) y SLAs de remediación
- [ ] Definir nivel ASVS según criticidad de datos (§5.1)
- [ ] Threat modeling (STRIDE / risk storming) de flujos sensibles (§5.4)
- [ ] Definir estrategia de testing y política de gates del pipeline (§3.1, §5.3)
- [ ] Elegir estilo arquitectónico justificado por atributos de calidad (§1.4)

### Desarrollo
- [ ] Unit tests por módulo nuevo (cobertura acordada) (§4.3)
- [ ] Análisis estático en cada commit (lint, SonarQube); revisar coupling/cohesión (§4.6)
- [ ] SAST + secret scanning en cada commit (§5.3)
- [ ] SCA + SBOM en cada build (§5.5)
- [ ] Consultas parametrizadas + validación en servidor por defecto (§5.7)
- [ ] Cifrado en tránsito/reposo; secretos fuera del código
- [ ] Commits Conventional + PR hacia `develop` con checklist (§2.6)

### Antes de cada release
- [ ] Suite completa (unit + integration) + regresión (§4.4)
- [ ] DAST sobre staging; 0 Critical/High sin justificar (VEX) (§5.3, §5.8)
- [ ] Artefactos firmados (SLSA) (§5.5)
- [ ] Performance bajo carga esperada; revisar logs de staging
- [ ] Checklist UX/UI si hay frontend (§6.1)
- [ ] Merge `develop → main` solo con todo verde (§2.6)

### Después del release
- [ ] Monitorear error rate, response time y logs las primeras 24–48h (MTTD bajo)
- [ ] Vigilar autenticaciones fallidas y picos anómalos (§5)
- [ ] Suscribirse a avisos de CVE de dependencias usadas
- [ ] Documentar defectos/incidentes para retroalimentar el proceso y el threat model

---

## 8. Referencias

**Briefs propios consolidados**
- Git Workflow Empresarial (`proyecto_itm_bienestar`)
- Directrices de QA (ISO/IEC 25010 + 8 papers peer-reviewed)
- Directrices de Seguridad (OWASP Top 10:2025, NIST SSDF, OWASP GenAI)
- Directrices UX/UI Frontend (8 papers de Cognitive Load / HCD / UX benchmarking)

**Documentos académicos de la biblioteca**
- Ortega Ovalle, M. T. (2026). *Arquitecturas de Software Basadas en Datos y Automatización Inteligente.* Vitalia, 7(1).
- Valdes Vargas, A. A. *Arquitectura de software y diseño basado en componentes.* UDAVINCI.
- Goncalves & Burgos (2025). *Inteligencia Artificial Generativa para Arquitectura de Software.* Uniandes.
- Zavala Toronjo, G. (2024/2025). *Arquitectura e Implementación de un Ecosistema CI/CD.* EPSEM.
- Richards, M. & Ford, N. (2020). *Fundamentals of Software Architecture.* O'Reilly. *(principios generales; obra con copyright, no reproducida)*

**Estándares externos**
- OWASP Top 10:2025 · OWASP Top 10 for LLM Applications 2025 · NIST SP 800-218 (SSDF) ·
  OWASP ASVS/SAMM · SLSA · CVSS v4.0 · ISO/IEC 25010:2011

---

*Mantener este documento vivo: actualizarlo al adoptar una nueva práctica, herramienta o estándar.*
