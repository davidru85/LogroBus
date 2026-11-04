package com.ruizurraca.logrobus.architecture

import com.ruizurraca.logrobus.architecture.UiTextRules.catalogueKeys
import com.ruizurraca.logrobus.architecture.UiTextRules.hardcodedKotlinUiText
import com.ruizurraca.logrobus.architecture.UiTextRules.hardcodedSwiftUiText
import com.ruizurraca.logrobus.architecture.UiTextRules.swiftCatalogueKeys
import com.ruizurraca.logrobus.architecture.UiTextRules.unbundledSwiftKeys
import com.ruizurraca.logrobus.architecture.UiTextRules.untranslatedCatalogueEntries
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** LB-405 (NFR-I18N-01): the scanners that find user-facing text written into UI code instead of the resources. */
class UiTextRulesTest {
    @Test
    fun lb405_kotlinTextWithALiteralIsHardcoded() {
        val source =
            """
            Text("Hola")
            Text(text = "Lines")
            Text(
                "Más",
                style = MaterialTheme.typography.titleSmall,
            )
            """.trimIndent()

        assertEquals(
            listOf(UiTextFinding(1, "Hola"), UiTextFinding(2, "Lines"), UiTextFinding(4, "Más")),
            hardcodedKotlinUiText(source),
        )
    }

    @Test
    fun lb405_kotlinSpokenLabelsAndMessagesWithALiteralAreHardcoded() {
        val source =
            """
            Modifier.semantics { contentDescription = "Más" }
            Icon(Icons.Default.Star, contentDescription = "Favourite")
            Modifier.semantics { stateDescription = "Seleccionado"; paneTitle = "Hoja" }
            Modifier.clickable(onClickLabel = "Abrir", onLongClickLabel = "Mover") {}
            CustomAccessibilityAction("Subir") { true }
            snackbars.showSnackbar("Hecho", actionLabel = "Deshacer")
            Toast.makeText(context, "Guardado", Toast.LENGTH_SHORT)
            AnnotatedString("Línea")
            """.trimIndent()

        assertEquals(
            listOf(
                "Más",
                "Favourite",
                "Seleccionado",
                "Hoja",
                "Abrir",
                "Mover",
                "Subir",
                "Hecho",
                "Deshacer",
                "Guardado",
                "Línea",
            ),
            hardcodedKotlinUiText(source).map { it.text },
        )
    }

    @Test
    fun lb405_kotlinResourcesTemplatesSymbolsAndNonUiLiteralsPass() {
        val source =
            """
            Text(stringResource(R.string.lines_title))
            Text("${'$'}{line.code}")
            Text("→${'$'}NO_BREAK_SPACE${'$'}headsign")
            Text(" · ")
            Icon(Icons.Default.Star, contentDescription = null)
            rememberInfiniteTransition(label = "now")
            Modifier.testTag("search")
            String.format(Locale.ROOT, "%02d:%02d", hour, minute)
            private const val SEARCH = "search"
            """.trimIndent()

        assertEquals(emptyList<UiTextFinding>(), hardcodedKotlinUiText(source))
    }

    @Test
    fun lb405_kotlinCommentsStringsAndCharactersDoNotFoolTheScanner() {
        val source =
            """
            // Text("Hola")
            /* contentDescription = "Más" /* nested */ Text("Adiós") */
            val code = "Text(\"Hola\")"
            val quote = '"'
            val raw = ""${'"'}Text("Hola")""${'"'}
            Text(stringResource(R.string.ok))
            Text("Fin")
            """.trimIndent()

        assertEquals(listOf(UiTextFinding(7, "Fin")), hardcodedKotlinUiText(source))
    }

    @Test
    fun lb405_kotlinPreviewsAreExempt() {
        val source =
            """
            @Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
            @Composable
            private fun ButtonPreview() {
                Text("Hola")
            }

            @PreviewLightDark
            @Composable
            internal fun Gallery() {
                Text("Galería")
            }

            @Composable
            private fun SampleRowPreview(name: String) {
                Text("Muestra")
            }

            @Composable
            fun Real() {
                Text("Adiós")
            }
            """.trimIndent()

        assertEquals(listOf(UiTextFinding(20, "Adiós")), hardcodedKotlinUiText(source))
    }

    @Test
    fun lb405_aPreviewAnnotationOnAnAnnotationClassExemptsNothingElse() {
        val source =
            """
            @Preview(name = "Light")
            @Preview(name = "Dark")
            annotation class ThemePreviews

            @Composable
            fun Real() {
                Text("Adiós")
            }
            """.trimIndent()

        assertEquals(listOf(UiTextFinding(7, "Adiós")), hardcodedKotlinUiText(source))
    }

    @Test
    fun lb405_kotlinTextAnywhereInATextArgumentIsHardcoded() {
        val source =
            """
            Text(if (late) "Tarde" else stringResource(R.string.on_time))
            Text(stop.name ?: "Parada sin nombre")
            Text(count.toString() + " paradas")
            Text("${'$'}{if (open) "Abierta" else "Cerrada"}")
            """.trimIndent()

        assertEquals(
            listOf("Tarde", "Parada sin nombre", " paradas", "Abierta", "Cerrada"),
            hardcodedKotlinUiText(source).map { it.text },
        )
    }

    @Test
    fun lb405_kotlinPositionalDescriptionsLabelsAndMessagesAreHardcoded() {
        val source =
            """
            Icon(Icons.Default.Close, "Cerrar")
            Image(painterResource(R.drawable.map), "Mapa")
            CustomAccessibilityAction(action = { true }, label = "Subir")
            Modifier.semantics { onClick(label = "Abrir línea") { true } }
            snackbars.showSnackbar(actionLabel = "Deshacer", message = "Hecho")
            Toast.makeText(requireContext(), "Guardado", Toast.LENGTH_SHORT)
            Text(buildAnnotatedString { append("Hacia ") })
            builder.setContentTitle("Próximo bus")
            """.trimIndent()

        assertEquals(
            listOf("Cerrar", "Mapa", "Subir", "Abrir línea", "Deshacer", "Hecho", "Guardado", "Hacia ", "Próximo bus"),
            hardcodedKotlinUiText(source).map { it.text },
        )
    }

    @Test
    fun lb405_kotlinFormatPatternsStateAndAnimationLabelsPass() {
        val source =
            """
            Text("%02d:%02d".format(Locale.ROOT, hour, minute))
            state.copy(text = "Hola")
            rememberInfiniteTransition(label = "now")
            Text(text = stringResource(R.string.ok))
            """.trimIndent()

        assertEquals(emptyList<UiTextFinding>(), hardcodedKotlinUiText(source))
    }

    @Test
    fun lb405_aKotlinPreviewWithAnExpressionBodyExemptsOnlyItself() {
        val source =
            """
            @Preview
            @Composable
            private fun RowPreview() = RowSample()

            @Composable
            fun Real() {
                Text("Adiós")
            }
            """.trimIndent()

        assertEquals(listOf(UiTextFinding(7, "Adiós")), hardcodedKotlinUiText(source))
    }

    @Test
    fun lb405_onlyAPrivateKotlinFunctionIsAPreviewByItsName() {
        val source =
            """
            @Composable
            fun RoutePreview(route: RouteUi) {
                Text("Ruta recomendada")
            }
            """.trimIndent()

        assertEquals(listOf(UiTextFinding(3, "Ruta recomendada")), hardcodedKotlinUiText(source))
    }

    @Test
    fun lb405_aKotlinStringEndsAtItsLine_soAStrayQuoteHidesNothingAfterIt() {
        val source =
            """
            val broken = "no closing quote
            Text("Hola")
            """.trimIndent()

        assertEquals(listOf(UiTextFinding(2, "Hola")), hardcodedKotlinUiText(source))
    }

    @Test
    fun lb405_swiftTextThatIsNoCatalogueKeyIsHardcoded() {
        val source =
            """
            Text("Hola")
            Button("Cancel") {}
            .navigationTitle("Lines")
            .accessibilityLabel("Más")
            Label("Info", systemImage: "info.circle")
            Section("Datos") {}
            .alert("Caché borrada", isPresented: ${'$'}shown) {}
            .searchable(text: ${'$'}query, prompt: "Buscar")
            String(localized: "Hecho")
            """.trimIndent()

        assertEquals(
            listOf("Hola", "Cancel", "Lines", "Más", "Info", "Datos", "Caché borrada", "Buscar", "Hecho"),
            hardcodedSwiftUiText(source).map { it.text },
        )
    }

    @Test
    fun lb405_swiftVerbatimTextWithALetterIsHardcoded() {
        val source =
            """
            Text(verbatim: "Hola")
            Text(verbatim: "lines.title")
            Text(verbatim: "")
            Text(verbatim: " · ")
            """.trimIndent()

        assertEquals(listOf(UiTextFinding(1, "Hola"), UiTextFinding(2, "lines.title")), hardcodedSwiftUiText(source))
    }

    @Test
    fun lb405_swiftCatalogueKeysDataAndSymbolsPass() {
        val source =
            """
            Text("lines.title", bundle: .module)
            Text("lines.towards \(headsign)")
            Text("lines.favourites.position \(index + 1) \(count)")
            Text(line.name)
            Text("\(code), \(name)")
            Image(systemName: "star.fill")
            String(localized: "eta.now", bundle: bundle)
            .accessibilityIdentifier("settings.theme")
            """.trimIndent()

        assertEquals(emptyList<UiTextFinding>(), hardcodedSwiftUiText(source))
    }

    @Test
    fun lb405_swiftCommentsAndStringsDoNotFoolTheScanner() {
        val source =
            """
            // Text("Hola")
            /* Text("Hola") /* nested */ Text("Adiós") */
            let code = "Text(\"Hola\")"
            let multiline = ""${'"'}
                Text("Hola")
                ""${'"'}
            let raw = #"Text("Hola")"#
            Text("Fin")
            """.trimIndent()

        assertEquals(listOf(UiTextFinding(8, "Fin")), hardcodedSwiftUiText(source))
    }

    @Test
    fun lb405_swiftPreviewsAreExempt() {
        val source =
            """
            #Preview("Dark") {
                Text("Hola")
            }

            private struct GlassPreview: View {
                var body: some View { Text(verbatim: "Buscar rutas") }
            }

            struct Row_Previews: PreviewProvider {
                static var previews: some View { Text("Muestra") }
            }

            struct Row: View {
                var body: some View { Text("Adiós") }
            }
            """.trimIndent()

        assertEquals(listOf(UiTextFinding(14, "Adiós")), hardcodedSwiftUiText(source))
    }

    @Test
    fun lb405_swiftLocalizedMembersAndValuesHoldKeysNotWords() {
        val source =
            """
            var title: LocalizedStringKey {
                switch self {
                case .day: "lines.group.day"
                case .night: "Líneas nocturnas"
                }
            }
            let subtitle: LocalizedStringKey = "Próximas llegadas"
            static var title: LocalizedStringResource = "lines.title"
            func row(label: LocalizedStringKey = "Fila") {}
            var hint: LocalizedStringResource { LocalizedStringResource("lines.hint", comment: "Read after the list") }
            """.trimIndent()

        assertEquals(
            listOf("Líneas nocturnas", "Próximas llegadas", "Fila"),
            hardcodedSwiftUiText(source).map { it.text },
        )
    }

    @Test
    fun lb405_swiftTextAnywhereInATextArgumentIsHardcoded() {
        val source =
            """
            Text(isOn ? "lines.on" : "Apagado")
            Text(stop.name ?? "Parada sin nombre")
            Text("lines.towards \(flag ? "Sí" : "No")")
            """.trimIndent()

        assertEquals(
            listOf("Apagado", "Parada sin nombre", "Sí", "No"),
            hardcodedSwiftUiText(source).map { it.text },
        )
    }

    @Test
    fun lb405_swiftAnnouncementsMapsPickersWidgetsAndUiKitShowOrSpeakTheirText() {
        val source =
            """
            AccessibilityNotification.Announcement("Actualizado").post()
            UIAccessibility.post(notification: .announcement, argument: "Actualizado ahora")
            Marker("Parada", coordinate: coordinate)
            Annotation("Bus", coordinate: coordinate) { BusPin() }
            DatePicker("Salida", selection: ${'$'}date)
            .accessibilityInputLabels(["Más", "Opciones"])
            .accessibilityCustomContent("Andén", "Dos")
            .navigationSubtitle("Centro")
            ShareLink("Compartir", item: url)
            ControlGroup("Mapa") {}
            AttributedString(localized: "Hola")
            NSLocalizedString("Adiós", comment: "")
            label.accessibilityLabel = "Botón"
            .configurationDisplayName("Llegadas")
            IntentDescription("Muestra las llegadas")
            @Parameter(title: "Parada") var stop: StopEntity
            DisplayRepresentation(title: "Línea")
            """.trimIndent()

        assertEquals(
            listOf(
                "Actualizado",
                "Actualizado ahora",
                "Parada",
                "Bus",
                "Salida",
                "Más",
                "Opciones",
                "Andén",
                "Dos",
                "Centro",
                "Compartir",
                "Mapa",
                "Hola",
                "Adiós",
                "Botón",
                "Llegadas",
                "Muestra las llegadas",
                "Parada",
                "Línea",
            ),
            hardcodedSwiftUiText(source).map { it.text },
        )
    }

    @Test
    fun lb405_onlyAPrivateSwiftTypeIsAPreviewByItsName() {
        val source =
            """
            struct RoutePreview: View {
                var body: some View { Text("Ruta recomendada") }
            }
            """.trimIndent()

        assertEquals(listOf(UiTextFinding(2, "Ruta recomendada")), hardcodedSwiftUiText(source))
    }

    @Test
    fun lb405_aPackageTargetTakesItsKeysFromItsOwnBundle() {
        // SwiftUI looks a key up in the app's bundle unless the call names another, which a package target must do.
        val source =
            """
            Text("lines.title", bundle: .module)
            Text("lines.title")
            Button("settings.clear_cache") {}
            String(localized: "eta.now", bundle: bundle)
            String(localized: "eta.now")
            .navigationTitle(Text("settings.title", bundle: .module))
            .navigationTitle("settings.title")
            var title: LocalizedStringKey { "lines.group.day" }
            var label: LocalizedStringResource { "tab.lines" }
            .searchable(text: ${'$'}query, prompt: "lines.search")
            """.trimIndent()

        assertEquals(
            listOf(
                UiTextFinding(2, "lines.title"),
                UiTextFinding(3, "settings.clear_cache"),
                UiTextFinding(5, "eta.now"),
                UiTextFinding(7, "settings.title"),
                UiTextFinding(9, "tab.lines"),
                UiTextFinding(10, "lines.search"),
            ),
            unbundledSwiftKeys(source),
        )
    }

    @Test
    fun lb405_keysKeepTheirArguments_soAKeyWithoutThemIsAnotherKey() {
        val catalogue =
            """
            { "sourceLanguage" : "es", "version" : "1.0", "strings" : {
              "line.title %@" : { }, "line.title.generic" : { }, "eta.clock_spoken %lld %@" : { },
              "lines.favourites.position %lld %lld" : { } } }
            """.trimIndent()
        val source =
            """
            Text("line.title \(code)", bundle: .module)
            Text("line.title", bundle: .module)
            String(localized: "eta.clock_spoken \(hour) \(time)", bundle: bundle)
            """.trimIndent()

        assertEquals(
            setOf(
                "line.title \uFFFC",
                "line.title.generic",
                "eta.clock_spoken \uFFFC \uFFFC",
                "lines.favourites.position \uFFFC \uFFFC",
            ),
            catalogueKeys(catalogue),
        )
        assertEquals(
            setOf("line.title \uFFFC", "line.title", "eta.clock_spoken \uFFFC \uFFFC"),
            swiftCatalogueKeys(source),
        )
    }

    @Test
    fun lb405_theKeysSwiftCodeUsesAreItsTextKeysAndWhatItsLocalizedMembersReturn() {
        val source =
            """
            Text("lines.title", bundle: .module)
            Text("lines.towards \(headsign)")
            Button("settings.clear_cache") {}
            String(localized: "eta.now", bundle: bundle)
            Text(verbatim: "lines.verbatim")
            Image(systemName: "star.fill")
            .accessibilityIdentifier("settings.theme")
            var label: LocalizedStringKey {
                switch self {
                case .light: "settings.theme.light"
                case .dark: "settings.theme.dark"
                }
            }
            func title(for kind: Kind) -> LocalizedStringResource {
                kind == .day ? "lines.group.day" : "lines.group.night"
            }
            var icon: String { "info.circle" }
            """.trimIndent()

        assertEquals(
            setOf(
                "lines.title",
                "lines.towards \uFFFC",
                "settings.clear_cache",
                "eta.now",
                "settings.theme.light",
                "settings.theme.dark",
                "lines.group.day",
                "lines.group.night",
            ),
            swiftCatalogueKeys(source),
        )
    }

    @Test
    fun lb405_aCatalogueEntryNeedsATranslatedSpanishAndEnglishValue() {
        val catalogue =
            """
            {
              "sourceLanguage" : "es",
              "strings" : {
                "done" : { "localizations" : {
                  "en" : { "stringUnit" : { "state" : "translated", "value" : "Done" } },
                  "es" : { "stringUnit" : { "state" : "translated", "value" : "Hecho" } } } },
                "no_english" : { "localizations" : {
                  "es" : { "stringUnit" : { "state" : "translated", "value" : "Solo español" } } } },
                "new_english" : { "localizations" : {
                  "en" : { "stringUnit" : { "state" : "new", "value" : "Draft" } },
                  "es" : { "stringUnit" : { "state" : "translated", "value" : "Borrador" } } } },
                "empty_spanish" : { "localizations" : {
                  "en" : { "stringUnit" : { "state" : "translated", "value" : "Empty" } },
                  "es" : { "stringUnit" : { "state" : "translated", "value" : "" } } } },
                "nothing" : { }
              },
              "version" : "1.0"
            }
            """.trimIndent()

        assertEquals(
            listOf("empty_spanish (es)", "new_english (en)", "no_english (en)", "nothing (en)", "nothing (es)"),
            untranslatedCatalogueEntries(catalogue),
        )
    }

    @Test
    fun lb405_everyPluralFormIsTranslated_andUntranslatableEntriesNeedOnlyASpanishValue() {
        val catalogue =
            """
            {
              "sourceLanguage" : "es",
              "strings" : {
                "minutes %lld" : { "localizations" : {
                  "en" : { "variations" : { "plural" : {
                    "one" : { "stringUnit" : { "state" : "translated", "value" : "%lld minute" } },
                    "other" : { "stringUnit" : { "state" : "needs_review", "value" : "%lld minutes" } } } } },
                  "es" : { "variations" : { "plural" : {
                    "one" : { "stringUnit" : { "state" : "translated", "value" : "%lld minuto" } },
                    "other" : { "stringUnit" : { "state" : "translated", "value" : "%lld minutos" } } } } } } },
                "language.spanish" : { "shouldTranslate" : false, "localizations" : {
                  "es" : { "stringUnit" : { "state" : "translated", "value" : "Español" } } } },
                "language.english" : { "shouldTranslate" : false },
                "%@, %@" : { "shouldTranslate" : false, "localizations" : {
                  "es" : { "stringUnit" : { "state" : "new", "value" : "%1${'$'}@, %2${'$'}@" } } } }
              },
              "version" : "1.0"
            }
            """.trimIndent()

        assertEquals(
            listOf("language.english (es)", "minutes %lld (en)"),
            untranslatedCatalogueEntries(catalogue),
        )
    }
}
