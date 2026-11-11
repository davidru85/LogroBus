# LogroBus

[English](README.md) · **Español**

LogroBus es una aplicación nativa no oficial de los autobuses urbanos de Logroño, para Android e iOS. Responde rápido a dos preguntas: *¿cuándo llega mi autobús?* y *¿cómo llego?*

> **Estado:** hito M0, «Cimientos», en curso. Ya existen la compilación con Gradle, los 19 módulos compartidos de Kotlin Multiplatform y los esqueletos de las apps de Android e iOS (LB-001), y la CI revisa cada pull request (LB-002); el núcleo compartido está listo (LB-004), con el motor que refresca los datos en directo (LB-009), el cliente que habla con la fuente y los servicios tipados sobre él, comprobados contra las respuestas reales (LB-005, hecho) y la base de datos local con la búsqueda de paradas (LB-007), y las dos apps los ponen en marcha al arrancar, con las convenciones que sigue cada pantalla (LB-010); la sincronización que mantiene las líneas y paradas en el dispositivo está hecha, y espera a los formatos de datos de la fuente (LB-008, hecho), igual que las reglas que limpian esos datos (LB-006, hecho para las líneas y las paradas); el sistema de diseño está listo en las dos plataformas (LB-011); las dos apps se abren en sus tres pestañas (LB-012); y los contratos de telemetría esperan a un proveedor, sin enviar nada sin consentimiento (LB-017). Consulta el [estado del proyecto](#estado-del-proyecto).

## Funcionalidades (MVP)

La app se organiza en tres secciones y siempre se abre en **Líneas**. Funciona en móviles Android y en iPhone (sin iPad en el MVP).

| Sección | Qué hace |
| --- | --- |
| **Líneas** | Tus líneas favoritas en un carrusel arriba, que reordenas o quitas en Editar (hasta 20), y después todas las líneas diurnas y nocturnas (Búho) ordenadas por número, con buscador. El detalle de cada línea muestra el mapa del recorrido, los dos sentidos, las paradas con sus próximas llegadas y los autobuses en directo. |
| **Cerca de mí** | Un mapa con tu ubicación y las paradas cercanas. Toca una parada para ver sus próximos autobuses en un panel inferior. El panel también muestra tus paradas guardadas, con los tiempos en directo, y las paradas más cercanas. |
| **Cómo llegar** | Planificación de viajes de A a B: rutas que combinan líneas, tiempos a pie e indicaciones paso a paso. |

Además, la app incluye:
- Llegadas en tiempo real que indican claramente lo recientes y fiables que son las predicciones.
- Español e inglés: la app sigue el idioma del móvil, y si no lo tiene, va en español. El idioma también se elige en Ajustes, con un tema claro u oscuro, junto con cómo se muestran las llegadas (minutos u hora). Ajustes también dice cuándo se actualizaron por última vez las líneas y las paradas, y borra la caché.

Previsto para la v1.0: modo sin conexión, noticias y avisos de servicio, tarifas, puntos de recarga, un widget para la pantalla de inicio y notificaciones.

## Tecnología

| Área | Tecnología |
| --- | --- |
| Arquitectura | Arquitectura limpia, organizada en módulos por funcionalidad; la capa de presentación (ViewModels e interfaz) se escribe para cada plataforma y los modelos de presentación son compartidos |
| Código compartido | Kotlin Multiplatform: dominio, datos, red (Ktor), persistencia (SQLDelight), ajustes (Multiplatform Settings) y modelos de presentación |
| Android | Kotlin, Jetpack Compose, Material 3 Expressive; Reorderable para reordenar arrastrando (ADR-0019); AppCompat para el idioma y el tema propios de la app (ADR-0020). Mínimo Android 8.0 (API 26) |
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
android/                              App de Android, módulos core (sistema de diseño, mapa, navegación, ayudas para tests) y módulos de funcionalidad
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

Para ejecutar la app de Android, arranca un emulador o conecta un móvil (Android 8.0 o posterior) y ejecuta `./gradlew :android:app:installDebug`. Se abre en la lista de Líneas. Hasta que la app pueda descargar las líneas reales, las builds debug muestran líneas de ejemplo con nombres y colores inventados. Las líneas que marques como favoritas se guardan en el dispositivo y en su copia de seguridad.

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

Ambas apps ya tienen iconos propios de autobús en rojo, adaptados a su plataforma (LB-413, parte 1).

| Hito | Alcance | Estado |
| --- | --- | --- |
| M0 Cimientos | Esqueleto del proyecto, capa de datos, integración continua | En curso |
| M1 Líneas | Sección Líneas | En curso |
| M2 Cerca de mí | Sección Cerca de mí | En curso |
| M3 Cómo llegar | Planificador de viajes | En curso |
| M4 Consolidación del MVP | Gestión de errores, ajustes, accesibilidad, traducciones, diseños de Figma, declaraciones de privacidad | En curso |
| M5 v1.0 | Modo sin conexión, noticias, tarifas, puntos de recarga, widget, notificaciones, publicación | Sin empezar |

Los datos de ejemplo grabados de la fuente están hechos y vigilados (LB-003), así que las historias que esperaban a ellos ya no están bloqueadas: los tests de contrato de LB-005 parte 2, y LB-006 parte 2, LB-008 parte 2 y LB-101 parte 2.

### Cambios recientes

- **Iconos de las apps (LB-413, parte 1):** un autobús blanco propio sobre el rojo municipal; vectores adaptativos e iconos temáticos en Android, y variantes clara, oscura y tintada en iOS. Los recursos se regeneran con `swift scripts/generate-app-icons.swift` en macOS.

- **11-11-2026:**
  - LB-208, parte 2: guardar una parada desde su detalle, y "Mis paradas" en Android.
    - La barra de la parada lleva la misma estrella que una linea favorita: al tocarla pide un alias de hasta 30 caracteres y guarda la parada; al tocarla ya guardada, la quita.
    - "Mis paradas" esta encima de "Cerca de ti" en Cerca de mi, en tu orden, cada fila bajo su alias, y una seccion vacia dice como guardar una.
    - Una parada guardada desde su detalle aparece en "Mis paradas" al instante.
- **11-11-2026:**
  - LB-208, parte 1: las paradas que guardas, con su alias.
    - Una parada guardada con alias lo conserva, recortado a 30 caracteres, y el alias es lo que muestra "Mis paradas"; sin alias se muestra el nombre de la parada.
    - Hasta 20 paradas, igual que las líneas favoritas; la vigésimo primera se rechaza con un mensaje y no se anade nada.
    - Renombrar conserva el sitio, reordenar sigue tu orden, y quitar deja el resto en orden; todo se mantiene entre reinicios.
    - Una parada que el catalogo ya no tiene conserva su fila, para que puedas quitarla en vez de perderla en silencio.
    - El campo para guardar en el detalle de la parada y la hoja de edicion llegan despues.

- **11-11-2026:**
  - LB-203, parte 1: Cerca de mí lista las paradas que hay cerca.
    - "Cerca de ti" muestra hasta cinco paradas a menos de 800 m, la más cercana primero: la distancia en línea recta en cuanto responde el catálogo, y la distancia a pie cuando responde el servicio de distancias.
    - Cada fila dice la parada y lo lejos que está, se lee como una sola locución y abre la pantalla de la parada. Así la lista es el equivalente del mapa, y nada de esto necesita el mapa.
    - Al servicio se le piden solo las 40 candidatas más cercanas, y solo se le vuelve a preguntar cuando el usuario se ha movido más de 50 m.
    - La hoja con sus posiciones y el comportamiento del mapa en peek y medium esperan a LB-202.

- **11-11-2026:**
  - LB-205, parte 2: el detalle de la parada se abre desde el detalle de línea.
    - Cada parada de la lista ordenada de una línea es ahora una puerta: al tocarla se empuja la pantalla propia de la parada, con el mismo contenido y las mismas acciones que mostrará la hoja, y atrás vuelve a la línea.
    - TalkBack lee la fila igual que antes, la parada y su próxima llegada, y añade lo que hace un doble toque: "abrir parada", y "Open stop" en iOS.
    - El pin de parada de Cerca de mí sigue esperando a LB-202 y LB-203.

- **10-11-2026:**
  - LB-205, parte 1: el detalle de una parada, desde el catálogo y la fuente en vivo.
    - Una parada con las líneas que la sirven, cada una con su distintivo y los destinos de los recorridos que pasan por ella, leídos del catálogo en caché para que la cabecera se vea de inmediato.
    - Las llegadas previstas de la parada desde API-06, pedidas exactamente para las líneas que sirve dentro de los 60 minutos que fija la especificación, sondeadas cada 20 s mientras el detalle está visible.
    - Cada llegada dice "Ahora", "4 min" o una hora de reloj; la "~" de una predicción imprecisa, el tachado de una cancelada, y una llegada de hace más de un minuto descartada. Cada fila toma el distintivo y el destino de su propia línea y sentido.
    - "Actualizado hace 50 s" aparece en el color de aviso, y pasados dos minutos las horas pasan a hora de reloj con "Toca para actualizar", como pide la especificación.
    - La hoja de Cerca de mí que lo abra desde un pin de parada espera a LB-203; la pantalla empujada desde el detalle de línea llegó en la parte 2.
- **10-11-2026:**
  - LB-105: cada parada del detalle de una línea muestra su próximo autobús.
    - Bajo el selector de sentido, la fila de cada parada lleva su próxima llegada prevista, "4 min", o "–" cuando la fuente no da ninguna en 30 min. Una llegada que la fuente marca como predicción imprecisa muestra una "~", y una cancelada aparece tachada conservando su hora, así que la fila sigue diciendo cuándo tocaba el autobús.
    - El detalle pregunta a la fuente de ET por la línea cada 20 s mientras está en pantalla y deja de hacerlo al salir; dos pantallas mirando la misma línea comparten una sola petición. La ventana de 30 min es la que fija la especificación para API-05.
    - El formato de hora que elige el usuario se aplica también aquí: elegir horas de reloj reescribe las llegadas ya recibidas en vez de volver a preguntar a la fuente.
    - El código de sentido que envía la fuente es opaco, así que una llegada nunca se coloca en un recorrido adivinándolo: hasta que las paradas de la línea identifiquen el sentido, una llegada se muestra en los dos.
    - Una fila se lee como una sola frase para el lector de pantalla, "Parada 1, El Arco, 4 min", y el guion dice "sin avisos" en vez de callarse.
- **10-11-2026:**
  - LB-104: el detalle de una línea muestra sus dos sentidos y las paradas del seleccionado.
    - Bajo el nombre de la línea, un selector dice "Hacia El Cubo" y "Hacia Campillo", cada uno el último destino de ese sentido, y la lista de debajo sigue al seleccionado en el orden en que el autobús pasa por ellas, numeradas.
    - Cada parada de un recorrido viene del catálogo en caché, en la misma emisión que la línea, así que la lista nunca muestra media ruta.
    - El mapa con las dos formas no está: espera a LB-013, cuyo módulo aún no tiene código y cuyo proveedor de teselas y licencia siguen abiertos (OQ-10). El toque para abrir el detalle de una parada espera a LB-205 por el mismo motivo.
  - LB-102, parte 2: encontrar una línea por una parada que recorre.
    - Escribir el nombre de una parada lista ya todas las líneas cuya ruta pasa por ella, ignorando mayúsculas, acentos y la `ñ` igual que la búsqueda de paradas: "Iglesia de Lardero" encuentra las líneas 1 y B3.
    - Los nombres se leen del catálogo en caché en la misma emisión que las líneas, así que ninguna línea aparece sin ellos, y la búsqueda sigue siendo un filtro puro: una tecla nunca lanza una descarga.
    - La consulta sigue siendo una conjunción de palabras, así que "iglesia lardero" reduce los resultados a las paradas cuyo nombre lleva esa palabra.
  - LB-101, parte 2: la lista de líneas, de extremo a extremo sobre la captura real.
    - Un test coge las 13 líneas y 254 paradas grabadas, las pasa por el cliente tipado, el normalizador y la sincronización hasta una base de datos, las vuelve a leer y construye la lista: diurnas 1, 2, 3, 4, 5, 6, 7, 9, 10, 11 y nocturnas B1, B2, B3, en ese orden, sin línea 8.
    - Comprueba los colores de la propia captura, no los de relleno de las líneas de ejemplo, y que las líneas 4 y B1 comparten color mientras cada distintivo sigue mostrando su código.
    - Con eso se cierra la historia de la lista: todas las build types leen el catálogo real, y solo las previsualizaciones conservan las líneas de ejemplo.
  - LB-008, parte 2: la app ya descarga sus propios datos.
    - La lista de líneas se llena desde la fuente real en el primer uso: los dos endpoints de descubrimiento pasan por el cliente tipado, el normalizador los convierte en el catálogo y toda la descarga aterriza en una sola transacción, así que ninguna pantalla ve medio conjunto. Las builds de depuración ya no muestran líneas de ejemplo.
    - La app refresca el catálogo una vez al arrancar, fuera del hilo principal, y no dice nada si falla. El error que ve el usuario, con Reintentar, es el de la propia pantalla cuando la caché está vacía y no hay red.
    - El refresco diario en segundo plano aún no está: necesita `WorkManager` en Android y una tarea de fondo en iOS, así que va con el trabajo nocturno (LB-412).
  - LB-006, parte 2: las reglas que convierten las respuestas de líneas y paradas de la fuente en el catálogo que guarda la app.
    - `DefaultStaticNormaliser` reutiliza todas las reglas de la parte 1 y añade lo que necesita la forma de los datos: el color y la tinta legible sobre él, el destino de cada sentido y la geometría de la ruta como concatenación de sus tramos con precisión 6.
    - La fuente enumera 257 paradas en sus recorridos pero solo sirve 254, así que las tres que omite se sintetizan como paradas provisionales, con nombre y posición tomados del recorrido, para que ninguna parada de una ruta sea un hueco.
    - `DataQualityReport` cuenta lo que una normalización descartó y reparó, por causa, y no guarda ni un valor de la fuente: una parada sin id, sin nombre o sin posición, una fuera del área de servicio y un id repetido se cuentan, y la sincronización solo falla ante una respuesta inservible.
  - LB-005, parte 2: la visión tipada de la fuente que tiene la app, comprobada contra las respuestas reales.
    - Cada endpoint que lee el MVP tiene ya su DTO y su servicio: líneas y paradas, vehículos en vivo, llegadas estimadas y las rutas y distancias a pie del planificador. Cada petición lleva el tiempo de espera que le corresponde, y la lista de líneas de API-06 sale con sus comas intactas.
    - 16 tests de contrato descodifican las capturas grabadas y fijan lo que promete la especificación: 13 líneas y ninguna línea 8, 254 paradas, 57 actividades de vehículo de las que 40 llevan el `"[object Object]"` de la fuente, 32 llamadas de la línea 1 repartidas entre sus dos sentidos y un plan rechazado que aun así se explica.
    - Un elemento mal formado cuesta solo ese elemento; una ruta con un tramo ilegible se descarta entera, porque un itinerario a medias engaña; y una respuesta sin itinerarios es un éxito, porque así es como la fuente rechaza un plan.
    - Los tests leen las fixtures con un cargador pequeño que funciona en las dos plataformas y no necesita ninguna dependencia nueva.
  - LB-003: los datos de ejemplo grabados, y el grabador que los mantiene fieles.
    - `fixtures/` guarda 19 capturas de la fuente y del geocodificador, 3,5 MB, tomadas el 8 de noviembre de 2026: una petición por endpoint, con al menos un segundo entre ellas, con el `User-Agent` del proyecto, las claves ordenadas y dos espacios de sangrado.
    - `scripts/record-fixtures.sh` es el único sitio que puede llamar a la fuente en directo. Prepara las capturas en un directorio temporal y las mueve a su sitio solo si la ejecución entera salió bien, así que un fallo deja intactas las ya guardadas; `--list` imprime los endpoints y no toca la red, y `--check` vuelve a grabar y compara la forma del JSON, sin escribir nada y con código de salida 1 si algo difiere.
    - Un flujo de trabajo semanal ejecuta `--check` todos los lunes y abre una incidencia por semana ISO cuando un campo aparece, desaparece o cambia de tipo. No está en ninguna puerta: una diferencia nunca bloquea una fusión, y la fusión sigue siendo decisión del propietario.
    - Las capturas conservan los defectos de la fuente tal como llegan: en `vm_all_with_object_lineRef.json`, 40 de las 57 actividades de autobús llevan un `lineRef` que vale `"[object Object]"` (DD-06), así que los DTO se escriben contra lo que devuelve la fuente, no contra lo que espera la especificación.
    - Ningún test unitario ni de integración puede ya llegar a la fuente: un resolutor del JDK en `:shared:core:testing`, presente en el classpath de tests de todo módulo que use la convención de librería KMP o la de Android (salvo los dos módulos de soporte de tests, que no hacen peticiones), resuelve solo loopback y falla cualquier otro nombre de host con `NETWORK GUARD`. Los tests de Kotlin/Native siguen con `MockEngine`, y los jars del framework de Robolectric se resuelven con Gradle, así que la JVM de tests nunca los descarga.
    - Lo cubren tests: los casos de grabación ejecutan el verdadero `scripts/record-fixtures.sh` contra un servidor de prueba local en 127.0.0.1, los casos de formas llaman directamente a `scripts/ci/fixture-shape.sh --diff` sin servidor, tres casos no necesitan ni servidor ni red, y 5 comprueban la guarda, 2 de ellos desde los tests de un módulo corriente, donde solo las convenciones podían haberla puesto.
    - Los términos de reutilización de los datos (OQ-11) siguen abiertos. Las capturas ya están en el repositorio, y fusionarlas es decisión del propietario.
- **06-11-2026:**
  - LB-303: elige cuándo empieza tu viaje, ahora o en una fecha y hora.
    - Bajo los dos campos, «Salir ahora» abre un menú: «Salir ahora», o «Salir a las…», que abre un selector de fecha y luego una hora que escribes en Android, y el selector de fecha y hora del propio iPhone. El viaje muestra entonces cuándo sale: «Salir a las 08:19», «Salir mañana a las 08:19» o «Salir el 15 ene a las 08:19».
    - Las horas son las de Logroño, sea cual sea la zona horaria del móvil, y se tienen en cuenta los días de cambio de hora. No se pueden elegir días pasados.
    - Una hora elegida se mantiene hasta que la cambias, también al volver a la pestaña.
    - Las rutas se planificarán para esa hora cuando lleguen (LB-306).
  - LB-304: un toque intercambia dónde empieza y dónde acaba tu viaje.
    - El botón junto a los dos campos los intercambia, «Mi ubicación» incluida, que entonces puede ser donde acaba el viaje. Un segundo toque los devuelve a su sitio.
    - Volver a la pestaña mantiene el intercambio. «Mi ubicación» rellena un origen vacío solo si el viaje no acaba ya ahí, y sale de cualquiera de los campos si la app ya no puede usar tu ubicación.
    - Los lectores de pantalla dicen «Intercambiar origen y destino», después de los dos campos.
    - Cuando lleguen las rutas (LB-306), un intercambio borrará las anteriores, como cualquier otro cambio de origen o destino.
  - CI: cuando Xcode abandona una auditoría de accesibilidad en una máquina de CI ocupada, la auditoría espera ahora unos segundos antes de volver a ejecutarse. Repetida en el acto, se había rendido tres veces seguidas.
  - LB-301, parte 1: la pestaña Cómo llegar muestra dónde empieza y dónde acaba tu viaje.
    - Si la app puede usar tu ubicación, el viaje sale de «Mi ubicación»; si no, los dos campos están vacíos. El destino muestra «¿A dónde vas?».
    - Abrir la pestaña no pide nada ni empieza a leer tu ubicación. Si activas o desactivas la ubicación en Cerca de mí o en los ajustes del móvil, el origen cambia la próxima vez que se muestra la pestaña.
    - Los lectores de pantalla dicen qué campo es y luego lo que muestra: «Origen, Mi ubicación».
    - Elegir una parada, un lugar guardado, una dirección o un punto del mapa llega con la parte 2, a medida que lleguen las paradas, la búsqueda de lugares y el mapa. Hasta entonces, tocar los campos no hace nada.
- **05-11-2026:**
  - LB-201, parte 1: la pestaña Cerca de mí pide tu ubicación, y solo cuando tocas.
    - Hasta que lleguen el mapa y las paradas cercanas, la pestaña muestra «Cerca de ti» con una fila, «Activa la ubicación para ver paradas cercanas». Abrir la pestaña no pide nada; la fila muestra el aviso del propio móvil.
    - La app pide la ubicación solo mientras la usas, nunca en segundo plano, y funciona con la ubicación aproximada. Si has dicho que no y el móvil ya no vuelve a preguntar, la fila abre los ajustes de la app, donde puedes activarla.
    - La app lee tu ubicación solo mientras la pestaña Cerca de mí está en pantalla, y deja de hacerlo al cambiar de pestaña o salir de la app. Todavía no la muestra ni la envía: la usarán las paradas cercanas.
    - CI: la pasada en modo oscuro de la auditoría de accesibilidad del iPhone se hace ahora de verdad en modo oscuro. En CI la app se quedaba en claro, así que esa pasada revisaba el modo claro por segunda vez. Una auditoría que Xcode abandona en una máquina de CI cargada vuelve a ejecutarse.
  - Compilación: los mínimos de seguridad de las herramientas y los tests de la compilación cubren ahora también las utilidades de los tests de interfaz de Android, cuyo Robolectric volvía a traer un Guava antiguo con un aviso de seguridad conocido. En la app no cambia nada.
  - LB-408, parte 1: ahora un test comprueba que el reloj de la app sigue al del servicio de autobuses, aunque el del móvil vaya desfasado unos minutos, y que una llegada contada con él muestra los minutos correctos. Cuando lleguen las listas de llegadas con LB-205, sus tiempos se contarán con ese reloj.
  - CI: la preparación de Gradle y el job del grafo de dependencias usan `gradle/actions` 6.4.1, que corrige un aviso de seguridad (GHSA-chpw-2mqm-95cc) en un modo que este proyecto no usa.
  - LB-407, parte 1: tus favoritas y tus ajustes van en la copia de seguridad del móvil, así que un móvil nuevo los recupera.
    - Android: la copia automática en tu cuenta de Google y la transferencia a un móvil nuevo guardan la base de datos de la app (las líneas favoritas, y más adelante las paradas y los lugares guardados) y sus ajustes, y nada más. Una caché nunca puede llenar el espacio que Android da a la copia de cada app.
    - iPhone: las copias de iCloud y del ordenador ya guardaban ambos; ahora un test se asegura de que sigan donde llegan las copias.
    - Marcar como sin servicio una parada guardada que sale de la red llega con las paradas guardadas.
- **04-11-2026:**
  - LB-406, parte 1: comprobaciones de accesibilidad en todas las pantallas.
    - Android: cada test de capturas comprueba también su pantalla tal como la recorre TalkBack. Cada control tiene etiqueta y una zona táctil de al menos 48 dp, y ningún texto queda cortado, tampoco con la letra al 200 %.
    - iPhone: un test recorre todas las pantallas con el tamaño de texto normal y con el máximo, y otra vez en modo oscuro, y pasa en cada una la auditoría de accesibilidad de Xcode: zonas táctiles, texto recortado, tamaño del texto, contraste y descripciones.
    - La auditoría encontró en iPhone textos grises y azules demasiado tenues, los colores del propio sistema. Las cabeceras de sección, las notas y los valores usan ahora el gris de la app, y los botones su azul, que alcanzan el contraste de 4,5:1 de WCAG AA.
    - La parte 2 llega con las pantallas que la necesitan: filas de llegadas habladas, avisos discretos de las actualizaciones en directo, una lista para cada mapa y cristal legible.
    - CI: el job de tests de la app de iPhone puede durar ahora hasta 35 minutos, porque las auditorías añaden 6 o 7.
  - LB-405: localización.
    - La app sigue los idiomas del móvil: el primero que habla, español o inglés. En un móvil en otro idioma va en español, y aun así puedes elegir cualquiera de los dos en Ajustes.
    - Todo lo que se ve queda entonces en ese idioma, con las palabras del propio móvil y los formatos de fecha, no solo los textos de la app: en Android, mediante el idioma de la pantalla; en iPhone, mediante el idioma en que SwiftUI busca los textos de la app.
    - Una comprobación falla con cualquier texto para el usuario escrito en el código en lugar de en las traducciones, con una clave de traducción del iPhone que falte y con un texto que no tenga su versión en español y en inglés.
- **03-11-2026:**
  - LB-403: Ajustes, desde Más.
    - Apariencia: el idioma de la app y su tema (el del sistema, claro u oscuro). En Android eliges el idioma en la app y cambia al momento; en iPhone la fila abre la página de la app en Ajustes del iPhone, donde iOS guarda el idioma de cada app (ADR-0020).
    - Llegadas: los tiempos de llegada en minutos o en hora. Las pantallas de llegadas, que vienen después, lo seguirán.
    - Datos: cuándo se actualizaron por última vez las líneas y las paradas, y "Borrar caché", que vacía las cachés de la app y conserva tus favoritas y las líneas y paradas.
    - El tema y el formato de las llegadas se guardan en el dispositivo (Multiplatform Settings, ADR-0018).
  - LB-110: editar las líneas favoritas.
    - "Editar", junto a "Tus líneas favoritas", abre una hoja con todas tus líneas favoritas en tu orden. Arrastra una línea por su asa para moverla, o quítala; el carrusel lo refleja, y la estrella de una línea quitada se apaga. El orden se guarda en el dispositivo.
    - Una línea favorita que ya no circula no tiene tarjeta, pero la hoja la muestra, para poder quitarla.
    - Puedes guardar hasta 20 líneas favoritas. Una estrella pulsada a partir de ahí no guarda nada y explica por qué.
    - Los lectores de pantalla mueven una línea con "Subir" y "Bajar", sin arrastrar.
    - En Android, el arrastre viene de la biblioteca Reorderable (ADR-0019); en iOS, del modo edición de la lista del sistema.
  - LB-102, parte 1: un buscador sobre la lista de Líneas.
    - La lista se filtra según escribes, por el código de una línea ("B1"), su nombre o hacia dónde va, sin importar tildes ni mayúsculas: "yague" encuentra "Yagüe - Varea". Cada palabra tiene que ser el principio de una palabra de la línea, así que "hosp lard" encuentra la línea 1.
    - Mientras hay una búsqueda, el carrusel de favoritas se oculta y los resultados mantienen los grupos diurno y nocturno; al borrar la búsqueda, el carrusel vuelve. Una búsqueda sin resultados lo dice y ofrece "Borrar búsqueda".
    - Buscar por el nombre de una parada llegará cuando se descarguen las paradas (LB-008 parte 2).
    - Las dos apps filtran la lista en el sitio. En Android el campo es un componente nuevo del sistema de diseño, porque la barra de búsqueda de Material solo abre una búsqueda a pantalla completa.
- **02-11-2026:**
  - CI: cuando un repositorio no sirve una dependencia que tiene (el "demasiadas peticiones" de Maven Central o el "no encontrado" del Gradle Plugin Portal), Gradle vuelve a ejecutarse, hasta tres veces y con un minuto de espera. Pasó en un job distinto en casi cada ejecución del 2 de noviembre. Un test que falla sigue fallando en el acto.
  - LB-103: líneas favoritas.
    - Cada línea de la lista tiene una estrella. Al pulsarla, la línea se añade a "Tus líneas favoritas", al final de una fila de tarjetas en lo alto de la lista, y al pulsarla otra vez se quita. Sin favoritas, la fila explica cómo añadir una.
    - Una tarjeta muestra el distintivo de la línea, su nombre y los dos extremos del recorrido. Al pulsar una tarjeta o una línea se abre la página de la línea, que de momento muestra la línea y la misma estrella; su mapa, sus sentidos y sus paradas llegan después (LB-104).
    - Las favoritas se guardan en el dispositivo, en el orden en que se añadieron, y se conservan al reiniciar. Los lectores de pantalla dicen "Añadir línea 3 a favoritas" o "Quitar línea 3 de favoritas".
  - CI: cuando Maven Central rechaza descargas porque llegan demasiadas a la vez (HTTP 429), Gradle sigue reintentando durante cerca de un minuto en vez de 3 segundos. Pasó dos veces el 2 de noviembre, cuando todos los jobs de CI descargaron a la vez las nuevas dependencias.
  - LB-101, parte 1: las dos apps se abren en la lista de Líneas.
    - Primero van las líneas diurnas y después las nocturnas (Búho), cada grupo ordenado por su número: la 9 antes que la 10. Cada línea muestra su distintivo de color, su nombre y una etiqueta "Sin servicio" cuando no circula.
    - Los lectores de pantalla dicen "Línea 4", nunca un color, porque algunas líneas lo comparten. Con textos grandes los nombres pasan a otra línea y los códigos nunca se cortan.
    - Las líneas se descargan la primera vez que hacen falta. Sin conexión, la pantalla lo dice y ofrece Reintentar.
    - Esa descarga aún no existe (espera a las condiciones de reutilización de los datos, OQ-11), así que las builds debug muestran líneas de ejemplo y las de release, el error.
    - Volver a pulsar la pestaña Líneas sube la lista al principio.
    - CI: los tests de cada parte del código de interfaz de iOS (el sistema de diseño, Líneas, Ajustes) se ejecutan ahora en un job propio, y el job de la app de iOS solo ejecuta los tests de la app.
  - Flujo de trabajo: una nueva sesión de un agente de IA, por ejemplo tras un reinicio, retoma el trabajo desde la entrada más reciente de `docs/project_log.md`. Esa entrada indica la pull request abierta, la siguiente tarea y las decisiones pendientes del propietario. `AGENTS.md` lo indica, y una sesión que termina con una pull request abierta escribe antes esa entrada.
    - Cada nueva sesión vuelve además a vigilar las pull requests abiertas antes que nada: `scripts/agent/session-start.sh` las lista al empezar la sesión.
  - CI: el check de SwiftLint vuelve a ejecutarse en Ubuntu 24.04. Las máquinas Linux más nuevas de GitHub no tienen una biblioteca que SwiftLint necesita, y ahora su instalación falla con un error claro cuando ocurre.
  - Seguridad: las 8 alertas abiertas de Dependabot estaban todas en bibliotecas que usan las herramientas de compilación y de test de Android y de Kotlin, ninguna en la app: Guava, OpenTelemetry, Commons Lang, HttpClient, jose4j, JDOM y el plugin de Gradle de Kotlin. La compilación usa ya las versiones corregidas de todas salvo la última, que es el Kotlin que lleva el propio Gradle y espera a una versión de Gradle que lo corrija.
- **31-10-2026:**
  - LB-006, parte 1: las reglas que limpian los datos de la fuente antes de que los use ninguna pantalla.
    - Las paradas y líneas se reconocen en cualquiera de los formatos de identificador de la fuente, y un valor vacío cuenta como ausente.
    - Las direcciones se leen de las palabras de la fuente, y los códigos numéricos de ET se deducen de las paradas por las que pasan sus autobuses.
    - Se corrigen las horas de llegada a la siguiente parada, que la fuente marca como UTC pero da en hora de Madrid, también las noches de cambio de hora.
    - Los colores de línea se leen en los dos formatos de la fuente, los nombres se recortan y se corrigen sus erratas desde la configuración remota, y cada línea obtiene su código, un nombre legible y si es nocturna.
    - Se reconocen las posiciones muy alejadas de Logroño, para poder descartarlas.
    - Aún no está activo: empezará a funcionar cuando la app sepa leer los datos de la fuente (LB-005), con el resto de LB-006.
- **30-10-2026:**
  - Flujo de trabajo: el agente de IA corrige ahora él mismo los checks de CI que fallan en su pull request, sin esperar al propietario, para que cada pull request llegue a revisión en verde. Corrige el fallo en la rama, o relanza una vez un fallo de la máquina de CI, y nunca debilita un check. La regla está en `AGENTS.md`, y `scripts/agent/watch-pr.sh` avisa de los checks fallidos además de las fusiones y los comentarios.
    - Los tests de iOS se reintentan una vez cuando el simulador de CI desaparece antes de ejecutarlos, un fallo de la máquina de CI visto tres veces.
  - LB-008, parte 1: el motor que mantiene al día las líneas y paradas guardadas en el dispositivo.
    - La primera pantalla que las necesita las descarga, y se actualizan cuando tienen más de un día. La app no incluye una copia propia (ADR-0008), así que sin conexión esa primera pantalla mostrará un error con «Reintentar».
    - Una descarga que parece rota, con menos de 5 líneas o 100 paradas, o una que falla a medias, no cambia nada: se conservan las líneas y paradas anteriores. Una descarga idéntica no se vuelve a escribir, salvo que la app haya cambiado su forma de leerla.
    - Cada actualización cuenta cuántas veces seguidas falta una parada guardada, para poder marcar más adelante una parada que ya no tiene servicio.
    - Aún no está activo: empezará a funcionar cuando la app sepa leer las líneas y paradas de la fuente (LB-005, LB-006).
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

Lee [AGENTS.md](AGENTS.md) antes de empezar. El código y la documentación se escriben en inglés técnico (consulta la política de idiomas en AGENTS.md). Trabaja en una rama y abre una pull request con una de las plantillas; nadie sube cambios directamente a `main`. La CI ejecuta las comprobaciones de políticas, de Kotlin y de iOS en cada pull request, y deben pasar antes de fusionarla. Un agente de IA corrige las comprobaciones que fallan en las pull requests que abre antes de que lleguen a revisión. Ejecuta antes `scripts/checks/run-all.sh` y pega su resumen en la pull request: hace las mismas comprobaciones en local, además de las que aún no tienen job en la CI. Cada pull request debe actualizar este README y su versión en inglés, [README.md](README.md), antes de fusionarse.

## Datos y aviso legal

LogroBus es un proyecto independiente, sin relación con el Ayuntamiento de Logroño ni con el operador de los autobuses.

- **Datos:** Ayuntamiento de Logroño ([transporteurbano.logrono.es](https://transporteurbano.logrono.es)).
- **Predicciones en tiempo real:** las proporciona el operador y pueden no ser exactas.
- **Datos del mapa:** © colaboradores de OpenStreetMap.

Las condiciones de reutilización de los datos están pendientes de confirmar con el Ayuntamiento.

## Licencia

El código fuente se puede leer públicamente, pero no se concede ninguna licencia: todos los derechos reservados. No puedes copiarlo, modificarlo ni redistribuirlo sin permiso del autor.
