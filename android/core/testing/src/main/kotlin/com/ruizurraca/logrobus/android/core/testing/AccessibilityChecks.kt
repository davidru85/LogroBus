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
 * Nodes hidden from accessibility services are skipped.
 */
fun SemanticsNodeInteractionsProvider.assertAccessible() {
    val controls = onAllNodes(isRoot()).fetchSemanticsNodes().flatMap { it.withDescendants() }
    val texts = onAllNodes(isRoot(), useUnmergedTree = true).fetchSemanticsNodes().flatMap { it.withDescendants() }
    val problems =
        controls.filter { it.isControl }.flatMap { it.controlProblems() } + texts.flatMap { it.textProblems() }
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
    return layouts
        .filter { layout -> layout.hasVisualOverflow || (0 until layout.lineCount).any(layout::isLineEllipsized) }
        .map { layout -> "\"${layout.layoutInput.text}\" is cut short" }
}
