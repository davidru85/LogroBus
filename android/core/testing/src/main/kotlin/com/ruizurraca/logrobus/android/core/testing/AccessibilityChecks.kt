package com.ruizurraca.logrobus.android.core.testing

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import org.robolectric.annotation.GraphicsMode
import org.robolectric.config.ConfigurationRegistry
import kotlin.math.roundToInt
import android.graphics.Rect as ScreenRect

/** The smallest touch target, in both directions (NFR-A11Y, Material's 48 dp). */
private val MIN_TOUCH_TARGET = 48.dp

/** Half a pixel: layout measures in fractions, and bounds round them. */
private const val ROUNDING = 0.5f

/**
 * LB-406 (NFR-A11Y): fails on what TalkBack users would meet on the screen under test, listing every problem.
 *
 * The Accessibility Test Framework's checks, which Compose UI Test offers, do nothing under Robolectric, so these
 * read the semantics tree and the node info that TalkBack reads:
 * - every control (a click, a long press, a slider, a text field, custom actions, expand, collapse or dismiss) has a
 *   label, from its text or its description, wherever it is: TalkBack scrolls to it;
 * - every control that takes a touch, once on screen, gets a touch area of at least 48 dp in both directions, as
 *   TalkBack and Accessibility Scanner get it: Compose widens a click's area, then cuts it at the window's edge and
 *   where a later control takes the space. A control a scrolling list cuts is measured once scrolled to;
 * - no text is cut short, by an ellipsis, by its box or by a container that does not scroll (NFR-A11Y-03 at 200 %),
 *   whether or not TalkBack reads it.
 *
 * A node hidden from accessibility services is skipped as a control, and so are its children when it merges them.
 * The test must run under Robolectric with native graphics: legacy graphics do not measure text, so the text checks
 * would judge it wrongly.
 *
 * [knownIssues] names, word for word, problems inside a library component that the screen cannot change, each with
 * its reason where the test lists it. They are left out; one that no longer occurs fails, so that its exemption goes.
 */
fun SemanticsNodeInteractionsProvider.assertAccessible(knownIssues: Set<String> = emptySet()) {
    val graphics = runCatching { ConfigurationRegistry.get(GraphicsMode.Mode::class.java) }.getOrNull()
    if (graphics != GraphicsMode.Mode.NATIVE) {
        throw AssertionError(
            "assertAccessible runs under Robolectric with @GraphicsMode(GraphicsMode.Mode.NATIVE): " +
                "legacy graphics do not measure text",
        )
    }
    val controls = onAllNodes(isRoot()).fetchSemanticsNodes().flatMap { it.reachableByTalkBack() }
    val texts = onAllNodes(isRoot(), useUnmergedTree = true).fetchSemanticsNodes().flatMap { it.withDescendants() }
    val found =
        controls.filter { it.isControl }.flatMap { it.controlProblems() } + texts.flatMap { it.textProblems() }
    val problems = found.filterNot { it in knownIssues } + (knownIssues - found.toSet()).map { "$it: no longer occurs" }
    if (problems.isNotEmpty()) {
        throw AssertionError("Accessibility problems (LB-406):\n" + problems.joinToString("\n") { "- $it" })
    }
}

private fun SemanticsNode.withDescendants(): List<SemanticsNode> =
    listOf(this) + children.flatMap { it.withDescendants() }

/** The node and its descendants that TalkBack reaches: a hidden node is not, nor are the children it merges. */
private fun SemanticsNode.reachableByTalkBack(): List<SemanticsNode> {
    val hidden = config.contains(SemanticsProperties.HideFromAccessibility)
    val self = if (hidden) emptyList() else listOf(this)
    val below =
        if (hidden &&
            config.isMergingSemanticsOfDescendants
        ) {
            emptyList()
        } else {
            children.flatMap { it.reachableByTalkBack() }
        }
    return self + below
}

/** A control that takes a touch, which needs a touch target. */
private val SemanticsNode.isTouchControl: Boolean
    get() =
        config.contains(SemanticsActions.OnClick) ||
            config.contains(SemanticsActions.OnLongClick) ||
            config.contains(SemanticsActions.SetProgress) ||
            config.contains(SemanticsActions.SetText)

/** A node that TalkBack lets its user act on, which needs a label. */
private val SemanticsNode.isControl: Boolean
    get() =
        isTouchControl ||
            config.contains(SemanticsActions.CustomActions) ||
            config.contains(SemanticsActions.Expand) ||
            config.contains(SemanticsActions.Collapse) ||
            config.contains(SemanticsActions.Dismiss)

/** What TalkBack says a control is: its description or its text, never only what was typed in a field. */
private val SemanticsNode.label: String
    get() =
        listOfNotNull(
            config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" "),
            config.getOrNull(SemanticsProperties.Text)?.joinToString(" "),
        ).joinToString(" ").trim()

private fun SemanticsNode.controlProblems(): List<String> {
    val name = label.ifEmpty { "A control at ${boundsInRoot.topLeft}" }
    return listOfNotNull(
        "$name has no label".takeIf { label.isEmpty() },
        touchTargetProblem(name).takeIf { isTouchControl },
    )
}

/** Its touch area as TalkBack gets it, if it is on screen and no scrolling list cuts it. */
private fun SemanticsNode.touchTargetProblem(name: String): String? {
    val bounds = talkBackBounds()?.takeUnless { touchBoundsInRoot.isEmpty || isCutByAScrollingAncestor }
    val (width, height) = with(layoutInfo.density) { (bounds?.width() ?: 0).toDp() to (bounds?.height() ?: 0).toDp() }
    return "$name: its touch target is ${width.text} × ${height.text} dp, under ${MIN_TOUCH_TARGET.text} dp"
        .takeIf { bounds != null && (width < MIN_TOUCH_TARGET || height < MIN_TOUCH_TARGET) }
}

/** The bounds that Compose gives TalkBack for the node, or null if TalkBack does not see it. */
private fun SemanticsNode.talkBackBounds(): ScreenRect? =
    (root as? ViewRootForTest)
        ?.view
        ?.accessibilityNodeProvider
        ?.createAccessibilityNodeInfo(id)
        ?.takeIf { it.isVisibleToUser }
        ?.let { info -> ScreenRect().also(info::getBoundsInScreen) }

/** Whether a scrolling ancestor shows only part of the node, which is whole once scrolled to. */
private val SemanticsNode.isCutByAScrollingAncestor: Boolean
    get() {
        val whole = Rect(positionInRoot, size.toSize())
        return generateSequence(parent) { it.parent }.filter { it.isScrolling }.any { !it.boundsInRoot.holds(whole) }
    }

private val SemanticsNode.isScrolling: Boolean
    get() =
        config.contains(SemanticsProperties.VerticalScrollAxisRange) ||
            config.contains(SemanticsProperties.HorizontalScrollAxisRange)

private fun Rect.holds(other: Rect): Boolean =
    other.left >= left - ROUNDING &&
        other.top >= top - ROUNDING &&
        other.right <= right + ROUNDING &&
        other.bottom <= bottom + ROUNDING

/** A size in dp to one decimal, without the decimal when it is whole: "24", "47.5". */
private val Dp.text: String
    get() = ((value * 10).roundToInt() / 10f).let { if (it % 1f == 0f) it.roundToInt().toString() else it.toString() }

private fun SemanticsNode.textProblems(): List<String> {
    val layouts = mutableListOf<TextLayoutResult>()
    config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
    return layouts.mapNotNull { layout ->
        val text = layout.layoutInput.text
        when {
            layout.isCutShort -> "\"$text\" is cut short"
            isCutByAContainer -> "\"$text\" is cut short by its container"
            else -> null
        }
    }
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

/** Whether a container that does not scroll shows only part of the node: a fixed-height bar, or the window. */
private val SemanticsNode.isCutByAContainer: Boolean
    get() {
        val shown = boundsInRoot
        val cut = shown.width < size.width - ROUNDING || shown.height < size.height - ROUNDING
        return cut && generateSequence(parent) { it.parent }.none { it.isScrolling }
    }
