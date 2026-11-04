package com.ruizurraca.logrobus.architecture

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
            listOf("Más", "Favourite", "Seleccionado", "Hoja", "Abrir", "Mover", "Subir", "Hecho", "Deshacer", "Guardado", "Línea"),
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
}
