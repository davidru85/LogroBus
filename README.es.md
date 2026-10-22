# LogroBus

[English](README.md) · **Español**

LogroBus es una aplicación nativa no oficial de los autobuses urbanos de Logroño, para Android e iOS. Responde rápido a dos preguntas: *¿cuándo llega mi autobús?* y *¿cómo llego?*

> **Estado:** fase de planificación y documentación. Todavía no hay código de la app; el siguiente hito es M0, «Cimientos». Consulta el [estado del proyecto](#estado-del-proyecto).

## Funcionalidades (MVP)

La app se organiza en tres secciones y siempre se abre en **Líneas**.

| Sección | Qué hace |
| --- | --- |
| **Líneas** | Tus líneas favoritas en un carrusel arriba, y después todas las líneas diurnas y nocturnas (Búho) ordenadas por número, con buscador. El detalle de cada línea muestra el mapa del recorrido, los dos sentidos, las paradas con sus próximas llegadas y los autobuses en directo. |
| **Cerca de mí** | Un mapa con tu ubicación y las paradas cercanas. Toca una parada para ver sus próximos autobuses en un panel inferior. El panel también muestra tus paradas guardadas, con los tiempos en directo, y las paradas más cercanas. |
| **Cómo llegar** | Planificación de viajes de A a B: rutas que combinan líneas, tiempos a pie e indicaciones paso a paso. |

Además, la app incluye:
- Llegadas en tiempo real que indican claramente lo recientes y fiables que son las predicciones.
- La red se puede consultar sin conexión.
- Español e inglés.

Previsto para la v1.0: noticias y avisos de servicio, tarifas, puntos de recarga, un widget para la pantalla de inicio y notificaciones.

## Tecnología

| Área | Tecnología |
| --- | --- |
| Núcleo compartido | Kotlin Multiplatform: dominio, datos, red (Ktor), persistencia (SQLDelight) y ViewModels compartidos |
| Android | Kotlin, Jetpack Compose, Material 3 Expressive. Mínimo Android 8.0 (API 26) |
| iOS | Swift, SwiftUI, Liquid Glass en iOS 26 o superior. Mínimo iOS 18 |
| Mapas | MapLibre Native en ambas plataformas |

## Estructura del repositorio

```
AGENTS.md, CLAUDE.md                  Instrucciones para agentes de IA y colaboradores
README.md, README.es.md               Este archivo (inglés, español)
docs/                                 Arquitectura, contrato de la API, guía de diseño, backlog, decisiones
logrono-bus-app-functional-spec.md    Especificación funcional v0.1
```

Los módulos `shared/`, `androidApp/` e `iosApp/` llegarán en el hito M0. Su estructura prevista se describe en [docs/architecture.md](docs/architecture.md).

## Primeros pasos

Todavía no hay código que compilar. Cuando se cree el proyecto (historia LB-001), esta sección explicará cómo compilar y ejecutar las dos apps. Necesitarás:

- JDK 21.
- La última versión estable de Android Studio con el plugin de Kotlin Multiplatform.
- Xcode 26 o posterior, en macOS, para la app de iOS.

## Documentación

La documentación técnica está en inglés.

| Documento | Contenido |
| --- | --- |
| [AGENTS.md](AGENTS.md) | Normas de trabajo para agentes de IA y colaboradores |
| [Especificación funcional](logrono-bus-app-functional-spec.md) | Alcance, fuentes de datos, requisitos |
| [docs/architecture.md](docs/architecture.md) | Módulos, capas, flujo de datos, integración con cada plataforma |
| [docs/api_spec.md](docs/api_spec.md) | Contrato de la API de autobuses y del geocodificador |
| [docs/design_guidelines.md](docs/design_guidelines.md) | Las tres secciones, Material 3 Expressive y Liquid Glass |
| [docs/backlog.md](docs/backlog.md) | Hitos e historias |
| [docs/adr/](docs/adr/README.md) | Registros de decisiones de arquitectura |

## Estado del proyecto

| Hito | Alcance | Estado |
| --- | --- | --- |
| M0 Cimientos | Esqueleto del proyecto, capa de datos, integración continua | Sin empezar |
| M1 Líneas | Sección Líneas | Sin empezar |
| M2 Cerca de mí | Sección Cerca de mí | Sin empezar |
| M3 Cómo llegar | Planificador de viajes | Sin empezar |
| M4 Consolidación del MVP | Modo sin conexión, ajustes, accesibilidad, traducciones | Sin empezar |
| M5 v1.0 | Noticias, tarifas, puntos de recarga, widget, notificaciones, publicación | Sin empezar |

### Cambios recientes

- **22-10-2026:**
  - Creada la documentación del proyecto.
  - Decidido: iOS 18 como mínimo, identificador `com.ruizurraca.logrobus`, Líneas como pestaña inicial y paradas guardadas en Cerca de mí.
  - Creado el repositorio privado en GitHub.

## Cómo contribuir

Lee [AGENTS.md](AGENTS.md) antes de empezar. Cada pull request debe actualizar este README y su versión en inglés, [README.md](README.md), antes de fusionarse.

## Datos y aviso legal

LogroBus es un proyecto independiente, sin relación con el Ayuntamiento de Logroño ni con el operador de los autobuses.

- **Datos:** Ayuntamiento de Logroño ([transporteurbano.logrono.es](https://transporteurbano.logrono.es)).
- **Predicciones en tiempo real:** las proporciona el operador y pueden no ser exactas.
- **Datos del mapa:** © colaboradores de OpenStreetMap.

Las condiciones de reutilización de los datos están pendientes de confirmar con el Ayuntamiento.

## Licencia

Todavía no se ha elegido licencia, así que todos los derechos están reservados.
