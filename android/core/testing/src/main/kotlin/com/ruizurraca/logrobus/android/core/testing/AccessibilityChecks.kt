package com.ruizurraca.logrobus.android.core.testing

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.robolectric.annotation.GraphicsMode
import org.robolectric.config.ConfigurationRegistry
import kotlin.math.roundToInt

/** The smallest touch target, in both directions (NFR-A11Y, Material's 48 dp). */
private val MIN_TOUCH_TARGET = 48.dp

/**
 * LB-406 (NFR-A11Y): fails on what TalkBack users would meet on the screen under test, listing every problem.
 *
 * The Accessibility Test Framework's checks, which Compose UI Test offers, do nothing under Robolectric, so these
 * read the semantics tree that TalkBack reads:
 * - every control (a click, a long press, a slider or a text field) has a label, from its text or its description;
 * - its touch area, which Compose widens around a click, is at least 48 dp in both directions;
 * - no text is cut short, by an ellipsis or by its box (NFR-A11Y-03 at 200 % font).
 *
 * Nodes hidden from accessibility services, and controls outside the screen, are skipped. The test must run with
 * native graphics: Robolectric's legacy graphics do not measure text, so the text checks would judge it wrongly.
 */
fun SemanticsNodeInteractionsProvider.assertAccessible() {
    if (ConfigurationRegistry.get(GraphicsMode.Mode::class.java) != GraphicsMode.Mode.NATIVE) {
        throw AssertionError(
            "assertAccessible needs @GraphicsMode(GraphicsMode.Mode.NATIVE): legacy graphics do not measure text",
        )
    }
    val controls = onAllNodes(isRoot()).fetchSemanticsNodes().flatMap { it.withDescendants() }
    val texts = onAllNodes(isRoot(), useUnmergedTree = true).fetchSemanticsNodes().flatMap { it.withDescendants() }
    val problems =
        controls.filter { it.isControl && it.isOnScreen }.flatMap { it.controlProblems() } +
            texts.flatMap { it.textProblems() }
    if (problems.isNotEmpty()) {
        throw AssertionError("Accessibility problems (LB-406):\n" + problems.joinToString("\n") { "- $it" })
    }
}

private fun SemanticsNode.withDescendants(): List<SemanticsNode> =
    if (config.contains(SemanticsProperties.HideFromAccessibility)) {
        emptyList()
    } else {
        listOf(this) + children.flatMap { it.withDescendants() }
    }

private val SemanticsNode.isControl: Boolean
    get() =
        config.contains(SemanticsActions.OnClick) ||
            config.contains(SemanticsActions.OnLongClick) ||
            config.contains(SemanticsActions.SetProgress) ||
            config.contains(SemanticsActions.SetText)

/** Whether any of the node shows: TalkBack does not see a node outside the screen, whose touch area is empty. */
private val SemanticsNode.isOnScreen: Boolean
    get() = !touchBoundsInRoot.isEmpty

private val SemanticsNode.label: String
    get() =
        listOfNotNull(
            config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" "),
            config.getOrNull(SemanticsProperties.Text)?.joinToString(" "),
            config.getOrNull(SemanticsProperties.EditableText)?.text,
        ).joinToString(" ").trim()

private fun SemanticsNode.controlProblems(): List<String> {
    val name = label.ifEmpty { "A control at ${boundsInRoot.topLeft}" }
    val touch = with(layoutInfo.density) { DpSize(touchBoundsInRoot.width.toDp(), touchBoundsInRoot.height.toDp()) }
    return listOfNotNull(
        "$name has no label".takeIf { label.isEmpty() },
        "$name: its touch target is ${touch.width.text} × ${touch.height.text} dp, under ${MIN_TOUCH_TARGET.text} dp"
            .takeIf { touch.width < MIN_TOUCH_TARGET || touch.height < MIN_TOUCH_TARGET },
    )
}

/** A size in dp to one decimal, without the decimal when it is whole: "24", "47.5". */
private val Dp.text: String
    get() = ((value * 10).roundToInt() / 10f).let { if (it % 1f == 0f) it.roundToInt().toString() else it.toString() }

private fun SemanticsNode.textProblems(): List<String> {
    val layouts = mutableListOf<TextLayoutResult>()
    config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
    return layouts.filter { it.isCutShort }.map { layout -> "\"${layout.layoutInput.text}\" is cut short" }
}

/**
 * Whether the text lost lines or characters: an ellipsis, lines beyond its height, or a line wider than its width.
 * Not [TextLayoutResult.hasVisualOverflow]: the layout that semantics give lays the paragraph out at the widest the
 * text may be, which is wider than a short text, so its width would always seem to overflow.
 */
private val TextLayoutResult.isCutShort: Boolean
    get() {
        val lines = 0 until lineCount
        val widest = lines.maxOfOrNull { getLineRight(it) - getLineLeft(it) } ?: 0f
        return lines.any(::isLineEllipsized) || didOverflowHeight || widest > size.width + ROUNDING
    }

/** Half a pixel: a line's width is a fraction, and the text's width rounds it up. */
private const val ROUNDING = 0.5f
