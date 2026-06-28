# CLAUDE.md — ChaosLab

Reglas duras para trabajar en este repo. Estándar completo en
[docs/DIRECTRICES_INGENIERIA.md](docs/DIRECTRICES_INGENIERIA.md) (Directrices de Ingeniería v1).

## Regla transversal

- Todos los cambios se hacen **directamente en los archivos del proyecto** (nunca en copias).
- **Verificar tras cada cambio**: `mvn verify` debe quedar verde antes de dar algo por hecho.
- **Prioridad ante conflicto:** Seguridad > QA/Correctitud > Arquitectura > UX/UI > estilo.

## Arquitectura (no negociable)

- Clean Architecture: la dependencia **siempre apunta hacia adentro**
  (Infrastructure → Application → Domain).
- **El paquete `com.chaoslab.domain` NO importa NADA de `org.springframework`, web ni SnakeYAML.**
  Si aparece un import de Spring en `domain/`, es deuda técnica activa: hay que revertirlo.
- Cada componente es una frontera con contrato (interfaz explícita), no "una clase suelta".
- No introducir complejidad estructural (microservicios, event-driven externo) "porque sí":
  cada incremento debe pagar un atributo de calidad concreto.

## Determinismo (atributo de Fiabilidad, #1)

- **Nunca** `Math.random()` suelto ni hilos reales ni `Thread.sleep`. Un único `Random` sembrado
  con el `seed` del YAML, inyectado en el motor. Tiempo virtual por eventos discretos.
- Invariante de test: **mismo seed → mismo resultado**.

## Git

- Ramas: `main` (producción, nunca commit directo) ← `develop` (integración) ← `feature/*`,
  `fix/*`, `hotfix/*`. Todo entra por PR hacia `develop` tras CI verde.
- Conventional Commits: `tipo(scope): descripción en imperativo`
  (`feat`/`fix`/`refactor`/`test`/`docs`/`chore`/`ci`/`perf`).
- `git push --force-with-lease`, nunca `--force`.

## Build y gates (ver pom.xml)

- `mvn verify` corre: Checkstyle (validate) → compile → tests → SpotBugs → gate JaCoCo (dominio ≥70%).
- JDK 21 obligatorio. Con JDK por defecto < 21 se usa el perfil `jdk-toolchain` (Maven Toolchains).
- Seguridad/SCA: `mvn -Psecurity verify` (OWASP Dependency-Check, falla si CVSS ≥ 7).

## Seguridad

- El YAML de topología es **entrada externa no confiable**: validar esquema, rangos y referencias,
  y aplicar **límites duros** (máx. componentes, duración, requests/seg) antes de tocar el motor.
  Nunca interpretar el YAML como código (SnakeYAML sin deserialización de tipos arbitrarios).
- Secretos fuera del código. Validación en servidor por defecto.

## Alcance MVP (no sobre-modelar)

4 componentes (Service, Queue, Database, LoadBalancer) · 3 fallos (Latency, Crash, NetworkPartition)
· 3 patrones de resiliencia (Retry, Timeout, CircuitBreaker). Lo demás es backlog explícito.
