# LogroBus

[English](README.md) · **Español**

LogroBus es una aplicación nativa no oficial de los autobuses urbanos de Logroño, para Android e iOS. Responde rápido a dos preguntas: *¿cuándo llega mi autobús?* y *¿cómo llego?*

> **Estado:** hito M0, «Cimientos», en curso. Ya existen la compilación con Gradle, los 19 módulos compartidos de Kotlin Multiplatform y los esqueletos de las apps de Android e iOS (LB-001), y la CI revisa cada pull request (LB-002); el núcleo compartido está listo (LB-004), con el motor que refresca los datos en directo (LB-009) el cliente que habla con la fuente (LB-005, en curso) la base de datos local con la búsqueda de paradas (LB-007), y las dos apps los ponen en marcha al arrancar, con las convenciones que sigue cada pantalla (LB-010); el sistema de diseño está listo en las dos plataformas (LB-011); las dos apps se abren en sus tres pestañas (LB-012); y los contratos de telemetría esperan a un proveedor, sin enviar nada sin consentimiento (LB-017). Consulta el [estado del proyecto](#estado-del-proyecto).

## Funcionalidades (MVP)

La app se organiza en tres secciones y siempre se abre en **Líneas**. Funciona en móviles Android y en iPhone (sin iPad en el MVP).

| Sección | Qué hace |
| --- | --- |
| **Líneas** | Tus líneas favoritas en un carrusel arriba, y después todas las líneas diurnas y nocturnas (Búho) ordenadas por número, con buscador. El detalle de cada línea muestra el mapa del recorrido, los dos sentidos, las paradas con sus próximas llegadas y los autobuses en directo. |
| **Cerca de mí** | Un mapa con tu ubicación y las paradas cercanas. Toca una parada para ver sus próximos autobuses en un panel inferior. El panel también muestra tus paradas guardadas, con los tiempos en directo, y las paradas más cercanas. |
| **Cómo llegar** | Planificación de viajes de A a B: rutas que combinan líneas, tiempos a pie e indicaciones paso a paso. |

Además, la app incluye:
- Llegadas en tiempo real que indican claramente lo recientes y fiables que son las predicciones.
- Español e inglés.

Previsto para la v1.0: modo sin conexión, noticias y avisos de servicio, tarifas, puntos de recarga, un widget para la pantalla de inicio y notificaciones.

## Tecnología

| Área | Tecnología |
| --- | --- |
| Arquitectura | Arquitectura limpia, organizada en módulos por funcionalidad; la capa de presentación (ViewModels e interfaz) se escribe para cada plataforma y los modelos de presentación son compartidos |
| Código compartido | Kotlin Multiplatform: dominio, datos, red (Ktor), persistencia (SQLDelight) y modelos de presentación |
| Android | Kotlin, Jetpack Compose, Material 3 Expressive. Mínimo Android 8.0 (API 26) |
| iOS | Swift, SwiftUI, Liquid Glass en iOS 26 o superior. Mínimo iOS 18, solo iPhone |
| Mapas | MapLibre Native en ambas plataformas |
| Ubicación | Fused Location Provider de los servicios de Google Play en Android, Core Location en iOS; solo en primer plano |

## Estructura del repositorio

```
AGENTS.md                             Normas para agentes de IA y colaboradores (referencia única)
CLAUDE.md, GEMINI.md                  Punteros a AGENTS.md para agentes de IA concretos
README.md, README.es.md               Este archivo (inglés, español)
docs/                                 Requisitos, arquitectura, contrato de la API, diseño, guías, backlog, decisiones
docs/playbooks/                       Procedimientos paso a paso para personas y agentes de IA
scripts/                              Arranque, comprobaciones de calidad, utilidades de CI y hooks de los agentes
.githooks/                            Hooks de git (los instala scripts/bootstrap.sh)
.claude/, .github/                    Configuración de Claude Code; plantillas de pull request y workflows de CI
logrono-bus-app-functional-spec.md    Especificación funcional v0.1
shared/                               Módulos de Kotlin Multiplatform: core, transit, favourites, funcionalidades, umbrella
android/                              App de Android, módulos core (sistema de diseño, mapa, navegación) y módulos de funcionalidad
iosApp/                               App de iOS (especificación de XcodeGen) y el paquete Swift LogroBusUI
build-logic/, gradle/                 Plugins de convención de Gradle; el catálogo de versiones
tools/architecture-tests/             Reglas automáticas de módulos y paquetes
config/                               Configuración de herramientas (detekt)
version.properties                    La versión de la app, común a Android e iOS
```

La estructura completa se describe en [docs/architecture.md](docs/architecture.md).

## Primeros pasos

Después de clonar el repositorio, ejecuta una vez `scripts/bootstrap.sh`: instala los hooks de git y avisa de las herramientas que falten. `scripts/checks/run-all.sh` ejecuta todas las comprobaciones de calidad.

Los módulos compartidos ya compilan: `./gradlew ciUnitTest` ejecuta los tests unitarios y las reglas de arquitectura. Si `ANDROID_HOME` no está definido, crea un archivo `local.properties` con `sdk.dir=<ruta a tu Android SDK>`; Git lo ignora.

Para ejecutar la app de Android, arranca un emulador o conecta un móvil (Android 8.0 o posterior) y ejecuta `./gradlew :android:app:installDebug`. De momento muestra una pantalla provisional de la pestaña Líneas.

Para ejecutar la app de iOS en macOS, ejecuta `scripts/ios/generate-project.sh` (también lo ejecuta `scripts/bootstrap.sh`, y hay que repetirlo tras cambiar código Kotlin). El script compila el framework compartido y genera `iosApp/iosApp.xcodeproj`; abre ese proyecto en Xcode y ejecuta el esquema `iosApp` en un simulador de iPhone.

Necesitarás:

- JDK 21.
- La última versión estable de Android Studio con el plugin de Kotlin Multiplatform.
- Xcode 26 o posterior y XcodeGen (`brew install xcodegen`), en macOS, para la app de iOS.

## Documentación

La documentación técnica está en inglés.

| Documento | Contenido |
| --- | --- |
| [AGENTS.md](AGENTS.md) | Normas de trabajo para agentes de IA y colaboradores |
| [Especificación funcional](logrono-bus-app-functional-spec.md) | Alcance, fuentes de datos, requisitos |
| [docs/requirements.md](docs/requirements.md) | Alcance del MVP y requisitos añadidos después de la especificación |
| [docs/engineering_workflow.md](docs/engineering_workflow.md) | Ramas, commits, pull requests y revisión |
| [docs/testing_strategy.md](docs/testing_strategy.md) | Pruebas y TDD |
| [docs/ci_cd.md](docs/ci_cd.md) | Integración y entrega continuas |
| [docs/architecture.md](docs/architecture.md) | Módulos, capas, flujo de datos, integración con cada plataforma |
| [docs/api_spec.md](docs/api_spec.md) | Contrato de la API de autobuses y del geocodificador |
| [docs/design_guidelines.md](docs/design_guidelines.md) | Las tres secciones, Material 3 Expressive y Liquid Glass |
| [docs/backlog.md](docs/backlog.md) | Hitos e historias |
| [docs/adr/](docs/adr/README.md) | Registros de decisiones de arquitectura |

## Estado del proyecto

| Hito | Alcance | Estado |
| --- | --- | --- |
| M0 Cimientos | Esqueleto del proyecto, capa de datos, integración continua | En curso |
| M1 Líneas | Sección Líneas | Sin empezar |
| M2 Cerca de mí | Sección Cerca de mí | Sin empezar |
| M3 Cómo llegar | Planificador de viajes | Sin empezar |
| M4 Consolidación del MVP | Gestión de errores, ajustes, accesibilidad, traducciones, diseños de Figma, declaraciones de privacidad | Sin empezar |
| M5 v1.0 | Modo sin conexión, noticias, tarifas, puntos de recarga, widget, notificaciones, publicación | Sin empezar |

### Cambios recientes

- **30-10-2026:**
  - Completada LB-017. Las dos apps tienen sus contratos de telemetría, listos para el proveedor de analítica e informes de fallos que falta elegir.
    - Los eventos de analítica solo llevan identificadores, recuentos, valores de sí o no y categorías fijas, nunca lo que escribes ni dónde estás. Una comprobación de la compilación rechaza cualquier otra cosa.
    - No se envía nada sin tu consentimiento, que se pide por separado para la analítica y para los informes de fallos. Hasta que llegue la pantalla de consentimiento, no se envía nada en absoluto.
    - Los informes de fallos conservan el tipo de cada error y dónde ocurrió, nunca su mensaje, que podría citar una búsqueda o una posición.
    - Las compilaciones de publicación aún no tienen telemetría. Las de depuración la mostrarán en la consola de desarrollo cuando puedas dar tu consentimiento; hasta entonces, la comprueban los tests. Solo la propia app puede añadir la biblioteca de un proveedor, en Android y en iOS, y cada app entrega su propia telemetría al código compartido al arrancar.
  - Todos los commits tienen ahora una sola identidad, `David Ruiz <davidru85@gmail.com>`, y ninguna línea de coautoría, como decidió el propietario. La regla está en `AGENTS.md`, los hooks de git y las comprobaciones locales la hacen cumplir, y el script de configuración la fija en cada clon. El propietario reescribió la historia existente para ajustarla.
  - Completada LB-012. Las dos apps se abren en sus tres pestañas: Líneas, Cerca de mí y Cómo llegar.
    - Cada pestaña recuerda dónde estabas. Abre «Sobre los datos» desde Líneas, cambia a Cerca de mí y vuelve, y sigue ahí. Al volver a tocar la pestaña, vuelve a su inicio.
    - Un botón de ajustes en cada pestaña abre Más, con Ajustes (vacío hasta que lleguen sus opciones) y «Sobre los datos», que ya se puede abrir.
    - En Android, las pestañas pasan de la barra inferior a un riel lateral en tabletas, plegables y móviles en horizontal, y Atrás desde Cerca de mí o Cómo llegar vuelve a Líneas. Los nombres de las pestañas se siguen leyendo con los tamaños de texto más grandes, y los lectores de pantalla pueden saltar entre los títulos de pantalla.
    - Las capturas en modo claro, oscuro y con texto muy grande protegen las pestañas y las pantallas nuevas en las dos plataformas.
    - Las pantallas de las pestañas son provisionales hasta que llegue cada sección.
- **29-10-2026:**
  - Completada LB-011. La parte 2 da a la app de iPhone el mismo sistema de diseño.
    - Los mismos colores provisionales, insignias de línea y tiempos de llegada que en Android, con las mismas palabras, que VoiceOver lee en español o inglés.
    - Los botones flotantes del mapa usan Liquid Glass en iOS 26, y un material translúcido en versiones anteriores.
    - Las capturas en modo claro, oscuro y con texto muy grande protegen cada pieza, también el cristal.
  - El arnés de agentes deja que Claude Code abra pull requests sin pedir permiso cada vez, como decidió el propietario. El merge sigue siendo cosa del propietario.
- **28-10-2026:**
  - LB-011, parte 1: la app de Android tiene su sistema de diseño.
    - Un aspecto neutro provisional en modo claro y oscuro, hasta que lleguen los diseños definitivos. Todos los textos se leen con facilidad.
    - Las insignias de línea muestran siempre el código, en negro o blanco, el que mejor se lea sobre el color de la línea. Los lectores de pantalla dicen «Línea 3», nunca el color.
    - Los tiempos de llegada muestran «Ahora», «4 min» o la hora, con «~» en las predicciones inciertas, los retrasos y adelantos, y los autobuses programados y cancelados. Cada uno se lee en voz alta en español o inglés.
    - Las capturas en modo claro, oscuro y con texto muy grande protegen el aspecto de cada pieza en cada pull request.
    - La parte de iOS llega en la parte 2.
  - Completada LB-010. La parte 2 fija el patrón que sigue cada pantalla en las dos plataformas: la pantalla obtiene su lógica del código compartido, las previsualizaciones y los tests usan datos guionizados, al salir de una pantalla se detienen sus actualizaciones, y un fallo muestra un motivo breve con «Reintentar».
    - La primera pantalla que lo sigue es el comienzo de «Acerca de los datos»: cuándo se actualizaron por última vez las líneas y las paradas, con la hora de Madrid, en español e inglés. Se abrirá desde el menú Más cuando lleguen las pestañas (LB-012).
    - El arnés de agentes deja que Claude Code suba ramas sin pedir permiso cada vez, como decidió el propietario. Siguen bloqueados el push a `main` y el force push.
  - LB-010, parte 1: las dos apps ponen en marcha el núcleo compartido al arrancar: un reloj corregido por el servidor, el cliente de la fuente de datos, el motor de datos en directo y la base de datos local, cada uno creado una sola vez. Las builds de release solo registran errores. iOS ve los tipos compartidos como tipos nativos de Swift (SKIE). Las convenciones de pantalla llegan en la parte 2, con la primera pantalla.
- **27-10-2026:**
  - Completada LB-007. La app tiene su base de datos local, que guarda las líneas, las paradas y los recorridos, tus favoritos y lugares guardados, y tus búsquedas recientes de rutas.
    - La búsqueda de paradas ignora las tildes, la ñ y las mayúsculas, así que «logrono» encuentra «Logroño».
    - Encaja el principio de cada palabra, en cualquier orden, y un número de parada como «38» encuentra esa parada la primera.
    - Responde en unos milisegundos sobre las 254 paradas.
  - LB-005, parte 2, primera mitad: el cliente envía los parámetros de consulta tal como los espera la fuente, y las dos apps rechazan las conexiones HTTP sin cifrar. Los formatos de datos de cada endpoint esperan a los datos de ejemplo grabados (LB-003).
  - LB-005, parte 1: el cliente compartido que habla con la fuente de datos de los autobuses.
    - Identifica la app y pide los datos comprimidos.
    - Desiste a los 10 s con los datos en directo y a los 30 s con el catálogo de líneas y paradas.
    - Lee la hora del servidor en cada respuesta, para que los tiempos de llegada sigan siendo correctos aunque el reloj del móvil vaya mal, incluso detrás de una caché.
    - Una respuesta rota, o un elemento erróneo en una lista, solo hace fallar ese dato; los demás elementos se siguen mostrando.
    - No guarda cookies ni respuestas en caché, y sus registros de errores nunca contienen ubicaciones ni respuestas.
    - Los formatos de datos de cada endpoint llegan en la parte 2.
  - Completada LB-009. Un único motor compartido refresca ahora los datos en directo, como las llegadas y la posición de los autobuses, para las pantallas que los muestran:
    - Las pantallas que muestran los mismos datos comparten una sola petición.
    - Espera el intervalo configurado, nunca menos de 10 s, con un segundo al azar arriba o abajo, para que los usuarios no consulten la fuente todos a la vez.
    - Se detiene en segundo plano y refresca al volver.
    - Tras un error conserva los últimos datos y reintenta a los 2, 4, 8 y 16 s, y después cada 30 s.
    - Volver a entrar en una pantalla nunca hace que consulte más deprisa.
- **26-10-2026:**
  - Completada LB-004. El núcleo compartido ya puede registrar mensajes, saber si la app está en primer plano y leer la ubicación del móvil. La ubicación viene de los servicios de Google Play en Android y de Core Location en iOS. Solo se actualiza mientras una pantalla la está escuchando, y las coordenadas nunca se imprimen, ni siquiera en los registros de errores. La petición de permiso llega con la pantalla Cerca de mí (LB-201).
  - El propietario aceptó los siete ADR de LB-014, así que cada biblioteca puede entrar en la compilación con la primera historia que la necesite.
  - LB-014: siete registros de decisiones de arquitectura (ADR-0012 a ADR-0018) para las bibliotecas que eligió la especificación: red, base de datos, inyección de dependencias, fechas y horas, imágenes, mapas y ajustes.
  - LB-004, parte 1: el núcleo compartido tiene sus primeras piezas:
    - un reloj que corrige la hora del móvil con la del servidor, para que los tiempos de llegada sean correctos aunque el reloj del móvil vaya mal;
    - los valores de configuración por defecto con los que funciona la app;
    - utilidades de distancias y de formas de mapa.
- **25-10-2026:**
  - Completada LB-002: la CI está terminada. Tres pull requests desechables confirmaron que un test roto, una violación de estilo y una caída de la cobertura de tests bloquean cada una la fusión.
  - CI: el job de iOS arranca el simulador del iPhone mientras aún se compila la app, en lugar de después, así que termina antes.
  - Flujo de trabajo: el propietario ya no anuncia las fusiones en la conversación. El agente de IA vigila su pull request y empieza la siguiente tarea cuando se fusiona.
- **24-10-2026:**
  - La CI revisa las dependencias en busca de vulnerabilidades de seguridad conocidas: no se puede fusionar una pull request que añada una biblioteca con una vulnerabilidad crítica conocida. Las primeras revisiones encontraron tres, en bibliotecas que usan las herramientas de compilación y de test de Android (Netty, Bouncy Castle y FreeMarker), no la app. La compilación ya usa las versiones corregidas.
  - Aceptado el ADR-0011, que eligió las herramientas de calidad del código: ktlint, detekt y Kover.
  - Herramientas de calidad del código: cada cambio en Kotlin se revisa en estilo (ktlint), en reglas del proyecto como «nada de llamadas directas al reloj» (detekt) y en cobertura de tests: al menos el 80 % del código compartido de dominio y de datos (Kover). La CI ejecuta las tres.
  - La CI también comprueba iOS en las máquinas macOS de GitHub: SwiftLint, los tests del código compartido en el simulador de iOS y la compilación y los tests de la app.
  - CI: los jobs de tests de Kotlin y Android tienen nombres legibles aunque se salten. Se saltan a propósito cuando una pull request no toca código.
- **23-10-2026:**
  - Aceptado el ADR-0010: cómo se genera el proyecto de iOS y cómo enlaza el código compartido.
  - LB-001, parte 3 de 3: el esqueleto de la app de iOS. Con esto se completa LB-001.
    - La app (`com.ruizurraca.logrobus`, solo iPhone, iOS 18 o posterior) se abre en una pantalla provisional de la pestaña Líneas, en español y en inglés.
    - Su pestaña inicial sale del código compartido, igual que en Android.
    - El proyecto de Xcode se genera a partir de una especificación y no se guarda en el repositorio.
    - El paquete Swift LogroBusUI contiene las pantallas de iOS, todavía vacías, y usa el framework compartido.
  - LB-001, parte 2 de 3: el esqueleto de la app de Android.
    - La app (`com.ruizurraca.logrobus`, Android 8.0 o posterior) se abre en una pantalla provisional de la pestaña Líneas, en español y en inglés.
    - Su pestaña inicial sale del código compartido, el primer valor que compartirán las dos apps.
    - Ya existen los módulos core y de funcionalidad de Android, todavía vacíos.
    - La CI también ejecuta los tests unitarios de Android, la compilación y lint.
  - Empezada LB-002, parte 1: la CI también ejecuta las comprobaciones de Kotlin en cada pull request que toque ficheros de Kotlin o de Gradle:
    - los tests de build-logic, las reglas de arquitectura y los tests unitarios de cada uno de los 19 módulos compartidos, cada uno en su propio job;
    - un único job, «Kotlin gate», los resume y es obligatorio para fusionar.
  - El repositorio ya es público (LB-415):
    - La CI ejecuta las comprobaciones de políticas en cada pull request, y `main` está protegida en GitHub.
    - Activados el escaneo de secretos y la protección de push de GitHub.
    - Decidida la licencia: no se concede ninguna, así que todos los derechos están reservados.
- **22-10-2026:**
  - Empezada LB-001, parte 1 de 3:
    - Compilación con Gradle: Kotlin 2.4.20, Android Gradle Plugin 9.3.3, Gradle 9.7.1.
    - Los 19 módulos compartidos.
    - Comprobación automática de las reglas de dependencias entre módulos y de la arquitectura.
    - Una revisión independiente encontró 16 problemas; todos están resueltos, con reglas más estrictas y tests para las propias reglas.
  - CI: `actions/checkout` actualizado de la v5 a la v7 (Dependabot, PR #2).
  - Creada la documentación del proyecto.
  - Decidido: iOS 18 como mínimo, identificador `com.ruizurraca.logrobus`, Líneas como pestaña inicial y paradas guardadas en Cerca de mí.
  - Creado el repositorio privado en GitHub.
  - Política de idiomas: el proyecto se escribe en inglés técnico, y las conversaciones con los agentes de IA, en español de España.
  - Definido antes de crear el proyecto:
    - Requisitos y criterios de aceptación del MVP.
    - Guías de ingeniería: TDD, pull requests, CI/CD, publicación, seguridad y estilo de código.
    - Un harness común para todos los agentes de IA: normas, procedimientos, comprobaciones de calidad y hooks de git.
  - Decidido:
    - El modo sin conexión pasa a la v1.0 y el MVP no tendrá iPad.
    - TDD con un commit por fase (rojo, verde, refactorización).
    - Arquitectura limpia con módulos por funcionalidad.
    - Contratos de telemetría, con el proveedor aún por decidir.
    - Diseños de Figma y declaraciones de privacidad al final del MVP.
    - El repositorio será público, con `main` protegida, antes de completar el MVP.

## Cómo contribuir

Lee [AGENTS.md](AGENTS.md) antes de empezar. El código y la documentación se escriben en inglés técnico (consulta la política de idiomas en AGENTS.md). Trabaja en una rama y abre una pull request con una de las plantillas; nadie sube cambios directamente a `main`. La CI ejecuta las comprobaciones de políticas, de Kotlin y de iOS en cada pull request, y deben pasar antes de fusionarla. Ejecuta antes `scripts/checks/run-all.sh` y pega su resumen en la pull request: hace las mismas comprobaciones en local, además de las que aún no tienen job en la CI. Cada pull request debe actualizar este README y su versión en inglés, [README.md](README.md), antes de fusionarse.

## Datos y aviso legal

LogroBus es un proyecto independiente, sin relación con el Ayuntamiento de Logroño ni con el operador de los autobuses.

- **Datos:** Ayuntamiento de Logroño ([transporteurbano.logrono.es](https://transporteurbano.logrono.es)).
- **Predicciones en tiempo real:** las proporciona el operador y pueden no ser exactas.
- **Datos del mapa:** © colaboradores de OpenStreetMap.

Las condiciones de reutilización de los datos están pendientes de confirmar con el Ayuntamiento.

## Licencia

El código fuente se puede leer públicamente, pero no se concede ninguna licencia: todos los derechos reservados. No puedes copiarlo, modificarlo ni redistribuirlo sin permiso del autor.
