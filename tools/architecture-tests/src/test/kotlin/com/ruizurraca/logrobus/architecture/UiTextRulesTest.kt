package com.ruizurraca.logrobus.architecture

import com.ruizurraca.logrobus.architecture.UiTextRules.hardcodedKotlinUiText
import com.ruizurraca.logrobus.architecture.UiTextRules.hardcodedSwiftUiText
import com.ruizurraca.logrobus.architecture.UiTextRules.swiftCatalogueKeys
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
                "lines.towards",
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
    fun lb405_everyPluralFormIsTranslated_andUntranslatableEntriesNeedOnlySpanish() {
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
                "language.english" : { "shouldTranslate" : false }
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
