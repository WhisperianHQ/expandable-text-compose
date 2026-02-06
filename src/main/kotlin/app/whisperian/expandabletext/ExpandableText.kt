package app.whisperian.expandabletext

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import android.os.Build
import android.util.Log
import kotlin.math.roundToInt
import kotlin.math.abs
import java.util.Locale
import java.text.BreakIterator as JavaBreakIterator
import android.icu.text.BreakIterator as IcuBreakIterator

/**
 * A text composable that animates smoothly when [maxLines] changes.
 *
 * Unlike the standard [Text] composable which jumps to the new line count immediately,
 * this composable animates the height transition when expanding or collapsing.
 *
 * This composable mirrors the standard [Text] API as closely as possible, with the key
 * difference being that height changes are animated rather than instant.
 *
 * @param text The text to display.
 * @param modifier Modifier to be applied to the layout.
 * @param minLines The minimum number of lines to display. Must be greater than 0 and less than or equal to [maxLines]
 *                 (unless [maxLines] is [Int.MAX_VALUE]).
 * @param maxLines The maximum number of lines to display. When this value changes,
 *                 the text will animate to the new height instead of jumping.
 *                 Use [Int.MAX_VALUE] to show all lines (expanded state).
 * @param color [Color] to apply to the text. If [Color.Unspecified], uses [LocalContentColor].
 * @param fontSize The size of glyphs to use when painting the text.
 * @param fontStyle The typeface variant to use (e.g., italic).
 * @param fontWeight The typeface thickness to use (e.g., bold).
 * @param fontFamily The font family to use.
 * @param letterSpacing The amount of space to add between each letter.
 * @param textDecoration The decorations to paint on the text (e.g., underline).
 * @param textAlign The alignment of the text within its container.
 * @param lineHeight Line height for the text.
 * @param softWrap Whether the text should break at soft line breaks.
 * @param overflow Overflow behavior supported by [ExpandableText]. Only [TextOverflow.Ellipsis]
 *                 and [TextOverflow.Clip] are supported.
 * @param onTextLayout Callback invoked when the text layout is calculated.
 * @param style Style configuration for the text. Values provided in other parameters
 *              (e.g., [fontSize]) will override values in the style.
 * @param animationSpec The animation specification used for the expand/collapse animation.
 * @param trailingContent Optional composable appended at the end of the visible text.
 *                        You decide when it appears by passing null or a composable.
 *                        The content will be constrained to the text's line height and clipped if it exceeds.
 *
 * @sample
 * ```
 * var expanded by remember { mutableStateOf(false) }
 *
 * ExpandableText(
 *     text = longText,
 *     maxLines = if (expanded) Int.MAX_VALUE else 3,
 *     modifier = Modifier.clickable { expanded = !expanded }
 * )
 * ```
 *
 * @sample
 * ```
 * // With custom trailing content
 * ExpandableText(
 *     text = longText,
 *     maxLines = if (expanded) Int.MAX_VALUE else 3,
 *     trailingContent = {
 *         Text(
 *             text = " more",
 *             color = MaterialTheme.colorScheme.primary,
 *             modifier = Modifier.clickable { expanded = true }
 *         )
 *     }
 * )
 * ```
 */
@Composable
fun ExpandableText(
    text: String,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    softWrap: Boolean = true,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current,
    animationSpec: AnimationSpec<Float> = spring(
        stiffness = Spring.StiffnessMediumLow,
        dampingRatio = Spring.DampingRatioNoBouncy,
    ),
    trailingContent: (@Composable () -> Unit)? = null,
) {
    // Merge style with individual parameters (individual params take precedence)
    val mergedStyle = style.merge(
        TextStyle(
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            textDecoration = textDecoration,
            textAlign = textAlign ?: TextAlign.Unspecified,
            lineHeight = lineHeight,
        )
    )

    val resolvedColor = mergedStyle.color.takeOrElse { LocalContentColor.current }

    ExpandableTextInternal(
        text = AnnotatedString(text),
        modifier = modifier,
        minLines = minLines,
        maxLines = maxLines,
        color = resolvedColor,
        softWrap = softWrap,
        overflow = overflow,
        onTextLayout = onTextLayout,
        style = mergedStyle,
        animationSpec = animationSpec,
        trailingContent = trailingContent,
    )
}

/**
 * A text composable that animates smoothly when [maxLines] changes.
 *
 * This overload accepts [AnnotatedString] for rich text support with spans
 * and inline content.
 *
 * @param text The annotated text to display.
 * @param modifier Modifier to be applied to the layout.
 * @param minLines The minimum number of lines to display. Must be greater than 0 and less than or equal to [maxLines]
 *                 (unless [maxLines] is [Int.MAX_VALUE]).
 * @param maxLines The maximum number of lines to display.
 * @param color [Color] to apply to the text.
 * @param fontSize The size of glyphs to use when painting the text.
 * @param fontStyle The typeface variant to use (e.g., italic).
 * @param fontWeight The typeface thickness to use (e.g., bold).
 * @param fontFamily The font family to use.
 * @param letterSpacing The amount of space to add between each letter.
 * @param textDecoration The decorations to paint on the text (e.g., underline).
 * @param textAlign The alignment of the text within its container.
 * @param lineHeight Line height for the text.
 * @param softWrap Whether the text should break at soft line breaks.
 * @param overflow Overflow behavior supported by [ExpandableText]. Only [TextOverflow.Ellipsis]
 *                 and [TextOverflow.Clip] are supported.
 * @param onTextLayout Callback invoked when the text layout is calculated.
 * @param style Style configuration for the text.
 * @param animationSpec The animation specification used for the expand/collapse animation.
 * @param trailingContent Optional composable appended at the end of the visible text.
 *                        You decide when it appears by passing null or a composable.
 */
@Composable
fun ExpandableText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    softWrap: Boolean = true,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current,
    animationSpec: AnimationSpec<Float> = spring(
        stiffness = Spring.StiffnessMediumLow,
        dampingRatio = Spring.DampingRatioNoBouncy,
    ),
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val mergedStyle = style.merge(
        TextStyle(
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            textDecoration = textDecoration,
            textAlign = textAlign ?: TextAlign.Unspecified,
            lineHeight = lineHeight,
        )
    )

    val resolvedColor = mergedStyle.color.takeOrElse { LocalContentColor.current }

    ExpandableTextInternal(
        text = text,
        modifier = modifier,
        minLines = minLines,
        maxLines = maxLines,
        color = resolvedColor,
        softWrap = softWrap,
        overflow = overflow,
        onTextLayout = onTextLayout,
        style = mergedStyle,
        animationSpec = animationSpec,
        trailingContent = trailingContent,
    )
}

// ============================================================================
// Internal Implementation
// ============================================================================
//
// ARCHITECTURE NOTES - Why we use SubcomposeLayout + FixedHeightLayout:
//
// Problem: When animating height collapse, we want to show the ellipsis near the
// end of the animation (not during). So we switch from "full text" to "truncated
// text with ellipsis" when the animation is almost finished.
//
// The simpler approach would be:
//
//   BoxWithConstraints {
//       val animatedHeightPx = ...
//       val shouldShowTruncation = animatedHeightPx <= targetHeightPx + threshold
//       val content = if (shouldShowTruncation) truncatedText else fullText
//       StaticHeightLayout(heightPx = animatedHeightPx) {
//           Text(content)
//       }
//   }
//
// This fails because BoxWithConstraints reports its content's natural height
// to the parent. When we switch to truncated content (shorter), the content's
// natural height shrinks. Even though StaticHeightLayout clips internally,
// BoxWithConstraints tells the parent "my content is now smaller", causing
// the outer layout to collapse immediately - a visible jump.
//
// We tried clamping StaticHeightLayout to ignore placeable.height and always
// report animatedHeightPx, but BoxWithConstraints still propagates the content
// size change to its parent through its own measurement.
//
// Solution: Replace BoxWithConstraints with SubcomposeLayout as the outermost
// wrapper, and wrap all content in FixedHeightLayout which ALWAYS reports
// animatedHeightPx as its height to the parent, regardless of content changes.
//
// Performance impact: Negligible. The expensive operation (TextMeasurer.measure)
// is unchanged. FixedHeightLayout is a thin O(1) wrapper. SubcomposeLayout is
// what BoxWithConstraints uses internally anyway.
//
// ============================================================================

private const val TRAILING_INLINE_ID = "expandable_text_trailing"
private const val DEFAULT_ELLIPSIS_STRING = " …"
private const val TAG = "ExpandableText"

@Composable
private fun ExpandableTextInternal(
    text: AnnotatedString,
    modifier: Modifier,
    minLines: Int,
    maxLines: Int,
    color: Color,
    softWrap: Boolean,
    overflow: TextOverflow,
    onTextLayout: ((TextLayoutResult) -> Unit)?,
    style: TextStyle,
    animationSpec: AnimationSpec<Float>,
    trailingContent: (@Composable () -> Unit)?,
) {
    require(minLines > 0) {
        "minLines must be greater than 0"
    }
    require(maxLines > 0 || maxLines == Int.MAX_VALUE) {
        "maxLines must be greater than 0 or Int.MAX_VALUE"
    }
    if (maxLines != Int.MAX_VALUE) {
        require(minLines <= maxLines) {
            "minLines must be less than or equal to maxLines"
        }
    }

    val resolvedOverflow = when (overflow) {
        TextOverflow.Ellipsis,
        TextOverflow.Clip,
        -> overflow
        else -> {
            Log.e(
                TAG,
                "Unsupported TextOverflow=$overflow passed to ExpandableText. " +
                    "Falling back to TextOverflow.Ellipsis.",
            )
            TextOverflow.Ellipsis
        }
    }

    val ellipsisString = if (resolvedOverflow == TextOverflow.Ellipsis) DEFAULT_ELLIPSIS_STRING else ""

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val latestAnimationSpec by rememberUpdatedState(animationSpec)

    // Use SubcomposeLayout as the outer wrapper to control the reported height.
    // This ensures the parent always sees `animatedHeightPx` as the height,
    // preventing layout jumps when we switch between full and truncated content.
    SubcomposeLayout(
        // Make the semantics behave like a regular Text: a `Modifier.testTag()` on
        // ExpandableText should expose the composed text semantics for tests/accessibility.
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            // NOTE: When a custom trailing composable is used, callers may tag/click it.
            // If we merge semantics in that case, those descendants disappear from the
            // merged semantics tree and tests (and accessibility) can't target them.
            .semantics(mergeDescendants = trailingContent == null) {},
    ) { constraints ->
        val maxWidthPx = constraints.maxWidth

        val measurementConstraints = Constraints(
            minWidth = 0,
            maxWidth = maxWidthPx,
            minHeight = 0,
            maxHeight = Constraints.Infinity,
        )

        // Use textMeasurer for reliable layout info
        val fullLayout = textMeasurer.measure(
            text = text,
            style = style,
            constraints = measurementConstraints,
            maxLines = Int.MAX_VALUE,
            softWrap = softWrap,
        )

        val fullLineCount = fullLayout.lineCount
        val resolvedMaxLines = if (maxLines == Int.MAX_VALUE) fullLineCount.coerceAtLeast(1) else maxLines

        val lineHeightPx = if (fullLineCount > 0) {
            fullLayout.getLineBottom(0) - fullLayout.getLineTop(0)
        } else {
            textMeasurer.measure(
                text = AnnotatedString("A"),
                style = style,
                constraints = measurementConstraints,
                maxLines = 1,
                softWrap = softWrap,
            ).size.height.toFloat()
        }

        val minHeightPx = lineHeightPx * minLines

        val fullHeightPx = fullLayout.size.height.toFloat()
        val heightForMaxLinesPx = if (fullLineCount == 0) {
            0f
        } else if (resolvedMaxLines >= fullLineCount) {
            fullHeightPx
        } else {
            fullLayout.getLineBottom(resolvedMaxLines - 1)
        }

        val targetHeightPx = maxOf(heightForMaxLinesPx, minHeightPx)

        // Phase 2: Subcompose the animated content
        val contentPlaceable = subcompose("content") {
            ExpandableTextContent(
                text = text,
                fullLayout = fullLayout,
                fullLineCount = fullLineCount,
                resolvedMaxLines = resolvedMaxLines,
                targetHeightPx = targetHeightPx,
                fullHeightPx = fullHeightPx,
                maxLines = maxLines,
                lineHeightPx = lineHeightPx,
                color = color,
                softWrap = softWrap,
                overflow = resolvedOverflow,
                onTextLayout = onTextLayout,
                style = style,
                animationSpec = latestAnimationSpec,
                ellipsisString = ellipsisString,
                trailingContent = trailingContent,
                textMeasurer = textMeasurer,
                maxWidthPx = maxWidthPx,
            )
        }.firstOrNull()?.measure(constraints)

        val width = contentPlaceable?.width ?: constraints.minWidth

        // The content composable will provide its animated height via intrinsic measurement,
        // but we need to get the animated height to report it. Since we can't easily
        // get the animated value here, we'll rely on the content's measured height.
        // The key is that the content always measures to animatedHeightPx.
        val height = contentPlaceable?.height ?: constraints.minHeight

        layout(width, height) {
            contentPlaceable?.placeRelative(0, 0)
        }
    }
}

/**
 * Inner content composable that handles animation state and rendering.
 * This is separated so the outer SubcomposeLayout can measure it and get the animated height.
 */
@Composable
private fun ExpandableTextContent(
    text: AnnotatedString,
    fullLayout: TextLayoutResult,
    fullLineCount: Int,
    resolvedMaxLines: Int,
    targetHeightPx: Float,
    fullHeightPx: Float,
    maxLines: Int,
    lineHeightPx: Float,
    color: Color,
    softWrap: Boolean,
    overflow: TextOverflow,
    onTextLayout: ((TextLayoutResult) -> Unit)?,
    style: TextStyle,
    animationSpec: AnimationSpec<Float>,
    ellipsisString: String,
    trailingContent: (@Composable () -> Unit)?,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    maxWidthPx: Int,
) {
    val density = LocalDensity.current

    val animatedHeight = remember(text, style, maxWidthPx) { Animatable(targetHeightPx) }

    LaunchedEffect(targetHeightPx) {
        if (abs(animatedHeight.value - targetHeightPx) < 0.5f) {
            animatedHeight.snapTo(targetHeightPx)
        } else {
            animatedHeight.animateTo(targetHeightPx, animationSpec)
        }
    }

    val animatedHeightPx = animatedHeight.value
    val isAtTarget = abs(animatedHeightPx - targetHeightPx) < 0.5f

    // Small tolerance so the collapsed/truncated presentation can appear at the
    // perceived end of the animation (springs may keep running with tiny deltas).
    val nearEndThresholdPx = with(density) { 3.dp.toPx() }

    // Truncation is shown at the visual end of collapse.
    // During most of the animation we render full text and rely on clipping.
    val hasWidthOverflow = fullLayout.hasVisualOverflow

    val shouldShowCollapsedTruncation =
        overflow == TextOverflow.Ellipsis &&
            maxLines != Int.MAX_VALUE &&
            (resolvedMaxLines < fullLineCount || hasWidthOverflow) &&
            (isAtTarget || animatedHeightPx <= targetHeightPx + nearEndThresholdPx)

    // Performance: when we show the collapsed/truncated version, cap maxLines as well
    // (we still keep overflow=Clip to avoid native ellipsis behavior).
    val renderMaxLines = if (shouldShowCollapsedTruncation) resolvedMaxLines else Int.MAX_VALUE
    val hasTrailingContent = trailingContent != null

    val ellipsisWidthPx = remember(style, ellipsisString) {
        if (ellipsisString.isEmpty()) {
            0
        } else {
            textMeasurer.measure(
                text = AnnotatedString(ellipsisString),
                style = style,
                maxLines = 1,
            ).size.width
        }
    }

    // Wrap everything in FixedHeightLayout to ensure the REPORTED height is always
    // animatedHeightPx, regardless of content changes. This prevents layout jumps.
    FixedHeightLayout(
        heightPx = animatedHeightPx,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (hasTrailingContent) {
            // Trailing content path (inline at end of displayed text)
            val lineHeightLimitPx = lineHeightPx.roundToInt().coerceAtLeast(0)

            SubcomposeLayout(modifier = Modifier.fillMaxWidth()) { constraints ->
                val trailingPlaceable = subcompose("trailing_measure") {
                    // This subcomposition is ONLY for measurement; ensure it does not
                    // leak into the semantics tree (avoids duplicate testTags).
                    Box(modifier = Modifier.clearAndSetSemantics { }) {
                        trailingContent()
                    }
                }.firstOrNull()?.measure(
                    Constraints(
                        minWidth = 0,
                        maxWidth = constraints.maxWidth,
                        minHeight = 0,
                        maxHeight = lineHeightLimitPx,
                    )
                )

                val trailingWidthPx = trailingPlaceable?.width ?: 0
                val trailingHeightPx = trailingPlaceable?.height ?: lineHeightLimitPx

                val reservedWidthPx = ellipsisWidthPx + trailingWidthPx

                val displayText = if (shouldShowCollapsedTruncation) {
                    fullLayout.buildCollapsedTextWithPlaceholder(
                        originalText = text,
                        ellipsisString = ellipsisString,
                        reservedWidthPx = reservedWidthPx,
                        availableWidthPx = constraints.maxWidth,
                        maxLines = resolvedMaxLines,
                        hasWidthOverflow = hasWidthOverflow,
                        locale = Locale.getDefault(),
                    )
                } else {
                    buildAnnotatedString {
                        append(text)
                        appendInlineContent(TRAILING_INLINE_ID)
                    }
                }

                val inlineContent = mapOf(
                    TRAILING_INLINE_ID to InlineTextContent(
                        placeholder = Placeholder(
                            width = with(density) { trailingWidthPx.toSp() },
                            height = with(density) { trailingHeightPx.toSp() },
                            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                        ),
                    ) {
                        trailingContent()
                    }
                )

                val textPlaceable = subcompose("text_with_trailing") {
                    BasicText(
                        text = displayText,
                        style = style.copy(color = color),
                        softWrap = softWrap,
                        maxLines = renderMaxLines,
                        overflow = TextOverflow.Clip,
                        onTextLayout = onTextLayout ?: {},
                        inlineContent = inlineContent,
                    )
                }.firstOrNull()?.measure(constraints)

                layout(textPlaceable?.width ?: 0, textPlaceable?.height ?: 0) {
                    textPlaceable?.placeRelative(0, 0)
                }
            }
        } else {
            // String ellipsis path (original behavior)
            val displayText = if (shouldShowCollapsedTruncation) {
                remember(
                    text,
                    fullLayout,
                    resolvedMaxLines,
                    ellipsisWidthPx,
                    ellipsisString,
                    hasWidthOverflow,
                ) {
                    fullLayout.buildCollapsedTextWordBoundary(
                        originalText = text,
                        ellipsisString = ellipsisString,
                        ellipsisWidthPx = ellipsisWidthPx,
                        availableWidthPx = maxWidthPx,
                        maxLines = resolvedMaxLines,
                        hasWidthOverflow = hasWidthOverflow,
                        locale = Locale.getDefault(),
                    )
                }
            } else {
                text
            }

            BasicText(
                text = displayText,
                style = style.copy(color = color),
                softWrap = softWrap,
                maxLines = renderMaxLines,
                overflow = TextOverflow.Clip,
                onTextLayout = onTextLayout ?: {},
            )
        }
    }
}

/**
 * A layout that always reports [heightPx] as its height to the parent,
 * regardless of the content's natural height. Content is clipped to this height.
 *
 * This is the key component that prevents layout jumps when switching between
 * full and truncated content during animations. See ARCHITECTURE NOTES at the
 * top of this file for why this is necessary.
 *
 * Performance: O(1) overhead - measures one child and returns fixed dimensions.
 */
@Composable
private fun FixedHeightLayout(
    heightPx: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier.clipToBounds(),
        content = content,
    ) { measurables, constraints ->
        if (measurables.isEmpty()) {
            val height = heightPx.roundToInt().coerceIn(constraints.minHeight, constraints.maxHeight)
            return@Layout layout(constraints.minWidth, height) {}
        }

        // Measure content with unbounded height
        val looseConstraints = Constraints(
            minWidth = constraints.minWidth,
            maxWidth = constraints.maxWidth,
            minHeight = 0,
            maxHeight = Constraints.Infinity,
        )

        val placeable = measurables.first().measure(looseConstraints)

        val width = placeable.width.coerceIn(constraints.minWidth, constraints.maxWidth)

        // IMPORTANT: Always use heightPx as the reported height, NOT placeable.height.
        // This ensures the parent sees a stable height during animations,
        // even when we switch between full and truncated content.
        val height = heightPx.roundToInt().coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(width, height) {
            placeable.placeRelative(0, 0)
        }
    }
}

private fun TextLayoutResult.buildCollapsedTextWithPlaceholder(
    originalText: AnnotatedString,
    ellipsisString: String,
    reservedWidthPx: Int,
    availableWidthPx: Int,
    maxLines: Int,
    hasWidthOverflow: Boolean,
    locale: Locale,
): AnnotatedString {
    val rawText = originalText.text
    if ((maxLines >= lineCount && !hasWidthOverflow) || lineCount == 0 || rawText.isEmpty()) {
        return originalText
    }
    if (maxLines <= 0) {
        return buildAnnotatedString {
            append(ellipsisString)
            appendInlineContent(TRAILING_INLINE_ID)
        }
    }

    val lastLineIndex = maxLines - 1
    val lineStartIndex = getLineStart(lastLineIndex)
    val originalLineEnd = getLineEnd(lineIndex = lastLineIndex, visibleEnd = true)
    var cutIndex = originalLineEnd

    // Defensive: some platform/Compose versions may report a line end that includes
    // the hard line break. If we treat that as the cursor position, getCursorRect()
    // can appear at the far edge, causing unnecessary trimming into the visible text.
    while (cutIndex > lineStartIndex && rawText[cutIndex - 1].isHardLineBreak()) {
        cutIndex--
    }

    if (cutIndex <= 0) {
        return buildAnnotatedString {
            append(ellipsisString)
            appendInlineContent(TRAILING_INLINE_ID)
        }
    }

    // IMPORTANT: TextLayoutResult.size.width may be smaller than the available width
    // (it can shrink-wrap to the longest line). For ellipsis fitting we must use the
    // actual layout constraint width so we don't trim unnecessarily.
    val widthPx = availableWidthPx
    if (widthPx <= 0) {
        return buildAnnotatedString {
            append(ellipsisString)
            appendInlineContent(TRAILING_INLINE_ID)
        }
    }

    // Move cutIndex left until we have enough space for the ellipsis on the last visible line.
    var glyphRect = getBoundingBox((cutIndex - 1).coerceAtLeast(0))
    val hadToMakeRoom: Boolean
    when (getParagraphDirection(cutIndex - 1)) {
        ResolvedTextDirection.Ltr -> {
            val minRight = (widthPx - reservedWidthPx).coerceAtLeast(0).toFloat()
            val initialRight = glyphRect.right
            while (cutIndex > 0 && glyphRect.right > minRight) {
                cutIndex--
                if (cutIndex <= 0) break
                glyphRect = getBoundingBox((cutIndex - 1).coerceAtLeast(0))
            }
            hadToMakeRoom = glyphRect.right < initialRight
        }

        ResolvedTextDirection.Rtl -> {
            val maxLeft = reservedWidthPx.coerceAtMost(widthPx).toFloat()
            val initialLeft = glyphRect.left
            while (cutIndex > 0 && glyphRect.left < maxLeft) {
                cutIndex--
                if (cutIndex <= 0) break
                glyphRect = getBoundingBox((cutIndex - 1).coerceAtLeast(0))
            }
            hadToMakeRoom = glyphRect.left > initialLeft
        }
    }

    if (cutIndex <= 0) {
        return buildAnnotatedString {
            append(ellipsisString)
            appendInlineContent(TRAILING_INLINE_ID)
        }
    }

    val boundary = if (hadToMakeRoom) {
        val wordBoundary = chooseBoundaryBefore(
            text = rawText,
            index = cutIndex,
            locale = locale,
        ).coerceIn(lineStartIndex, cutIndex)

        if (wordBoundary <= lineStartIndex) {
            precedingGraphemeBoundary(rawText, cutIndex, locale).coerceIn(lineStartIndex, cutIndex)
        } else {
            wordBoundary
        }
    } else {
        cutIndex
    }

    var trimEnd = boundary
    while (trimEnd > lineStartIndex && rawText[trimEnd - 1].isWhitespace()) {
        trimEnd--
    }

    if (trimEnd <= 0) {
        return buildAnnotatedString {
            append(ellipsisString)
            appendInlineContent(TRAILING_INLINE_ID)
        }
    }

    val kept = originalText.subSequence(0, trimEnd)

    val safeEllipsis = if (ellipsisString.isEmpty()) {
        ""
    } else {
        when (getParagraphDirection((trimEnd - 1).coerceAtLeast(0))) {
            ResolvedTextDirection.Rtl -> "\u2068$ellipsisString\u2069" // isolate suffix in RTL/mixed text
            ResolvedTextDirection.Ltr -> ellipsisString
        }
    }

    return buildAnnotatedString {
        append(kept)
        append(safeEllipsis)
        appendInlineContent(TRAILING_INLINE_ID)
    }
}

private fun TextLayoutResult.buildCollapsedTextWordBoundary(
    originalText: AnnotatedString,
    ellipsisString: String,
    ellipsisWidthPx: Int,
    availableWidthPx: Int,
    maxLines: Int,
    hasWidthOverflow: Boolean,
    locale: Locale,
): AnnotatedString {
    val rawText = originalText.text
    if ((maxLines >= lineCount && !hasWidthOverflow) || lineCount == 0 || rawText.isEmpty()) {
        return originalText
    }
    if (maxLines <= 0) return AnnotatedString(ellipsisString)

    val lastLineIndex = maxLines - 1
    val lineStartIndex = getLineStart(lastLineIndex)
    val originalLineEnd = getLineEnd(lineIndex = lastLineIndex, visibleEnd = true)
    var cutIndex = originalLineEnd

    // See buildCollapsedTextWithPlaceholder(): avoid treating a hard line break as
    // a visible character when computing cursor rects and available ellipsis space.
    while (cutIndex > lineStartIndex && rawText[cutIndex - 1].isHardLineBreak()) {
        cutIndex--
    }

    if (cutIndex <= 0) return AnnotatedString(ellipsisString)

    // IMPORTANT: TextLayoutResult.size.width may shrink-wrap to the text's intrinsic width.
    // Use the available layout constraint width for ellipsis fitting.
    val widthPx = availableWidthPx
    if (widthPx <= 0) return AnnotatedString(ellipsisString)

    // Move cutIndex left until we have enough space for the ellipsis on the last visible line.
    var glyphRect = getBoundingBox((cutIndex - 1).coerceAtLeast(0))
    val hadToMakeRoom: Boolean
    when (getParagraphDirection(cutIndex - 1)) {
        ResolvedTextDirection.Ltr -> {
            val minRight = (widthPx - ellipsisWidthPx).coerceAtLeast(0).toFloat()
            val initialRight = glyphRect.right
            while (cutIndex > 0 && glyphRect.right > minRight) {
                cutIndex--
                if (cutIndex <= 0) break
                glyphRect = getBoundingBox((cutIndex - 1).coerceAtLeast(0))
            }
            hadToMakeRoom = glyphRect.right < initialRight
        }

        ResolvedTextDirection.Rtl -> {
            val maxLeft = ellipsisWidthPx.coerceAtMost(widthPx).toFloat()
            val initialLeft = glyphRect.left
            while (cutIndex > 0 && glyphRect.left < maxLeft) {
                cutIndex--
                if (cutIndex <= 0) break
                glyphRect = getBoundingBox((cutIndex - 1).coerceAtLeast(0))
            }
            hadToMakeRoom = glyphRect.left > initialLeft
        }
    }

    if (cutIndex <= 0) return AnnotatedString(ellipsisString)

    // KEY FIX: Only snap to word boundary if we had to cut characters to make room for ellipsis.
    // If ellipsis fits after the natural line end, keep all characters of line N.
    // This ensures when expanding, no new text appears on line N (only ellipsis disappears).
    val boundary = if (hadToMakeRoom) {
        // We cut into line N's content. Snap to a word boundary that's still on line N.
        // Find the last word boundary that's >= lineStartIndex but <= cutIndex.
        val wordBoundary = chooseBoundaryBefore(
            text = rawText,
            index = cutIndex,
            locale = locale,
        ).coerceIn(lineStartIndex, cutIndex)

        // If word boundary would cut the entire last line, use grapheme boundary instead
        if (wordBoundary <= lineStartIndex) {
            precedingGraphemeBoundary(rawText, cutIndex, locale).coerceIn(lineStartIndex, cutIndex)
        } else {
            wordBoundary
        }
    } else {
        // No room constraints violated - keep the natural line ending.
        cutIndex
    }

    // Trim trailing whitespace from the kept substring.
    var trimEnd = boundary
    while (trimEnd > lineStartIndex && rawText[trimEnd - 1].isWhitespace()) {
        trimEnd--
    }

    if (trimEnd <= 0) return AnnotatedString(ellipsisString)

    val kept = originalText.subSequence(0, trimEnd)
    val safeEllipsis = when (getParagraphDirection((trimEnd - 1).coerceAtLeast(0))) {
        ResolvedTextDirection.Rtl -> "\u2068$ellipsisString\u2069" // isolate suffix in RTL/mixed text
        ResolvedTextDirection.Ltr -> ellipsisString
    }
    return AnnotatedString.Builder().apply {
        append(kept)
        append(safeEllipsis)
    }.toAnnotatedString()
}

private fun chooseBoundaryBefore(
    text: String,
    index: Int,
    locale: Locale,
): Int {
    if (index <= 0) return 0
    if (index >= text.length) return text.length

    val useWord = shouldUseWordBoundaries(text, locale)

    val boundary = if (useWord) {
        precedingWordBoundary(text, index, locale)
    } else {
        precedingGraphemeBoundary(text, index, locale)
    }

    return when {
        boundary <= 0 -> precedingGraphemeBoundary(text, index, locale)
        boundary > index -> index
        else -> boundary
    }
}

private fun shouldUseWordBoundaries(text: String, locale: Locale): Boolean {
    val hasSeparators = text.any { ch ->
        ch.isWhitespace() || ch == '-' || ch == '.' || ch == ',' || ch == '/' || ch == '\\'
    }
    if (!hasSeparators) return false

    // Require at least 2 word tokens (cheap confidence signal).
    return countWordTokens(text, locale, maxToCount = 2) >= 2
}

private fun countWordTokens(text: String, locale: Locale, maxToCount: Int): Int {
    var count = 0

    if (Build.VERSION.SDK_INT >= 24) {
        @Suppress("NewApi")
        val it = IcuBreakIterator.getWordInstance(locale)
        it.setText(text)
        var start = it.first()
        var end = it.next()
        while (end != IcuBreakIterator.DONE) {
            if (start < end) {
                val segment = text.substring(start, end)
                if (segment.any { ch -> ch.isLetterOrDigit() }) {
                    count++
                    if (count >= maxToCount) return count
                }
            }
            start = end
            end = it.next()
        }
        return count
    }

    val it = JavaBreakIterator.getWordInstance(locale)
    it.setText(text)
    var start = it.first()
    var end = it.next()
    while (end != JavaBreakIterator.DONE) {
        if (start < end) {
            val segment = text.substring(start, end)
            if (segment.any { ch -> ch.isLetterOrDigit() }) {
                count++
                if (count >= maxToCount) return count
            }
        }
        start = end
        end = it.next()
    }
    return count
}

private fun precedingWordBoundary(text: String, index: Int, locale: Locale): Int {
    if (Build.VERSION.SDK_INT >= 24) {
        @Suppress("NewApi")
        val it = IcuBreakIterator.getWordInstance(locale)
        it.setText(text)
        val boundary = it.preceding(index)
        return if (boundary == IcuBreakIterator.DONE) 0 else boundary
    }

    val it = JavaBreakIterator.getWordInstance(locale)
    it.setText(text)
    val boundary = it.preceding(index)
    return if (boundary == JavaBreakIterator.DONE) 0 else boundary
}

private fun precedingGraphemeBoundary(text: String, index: Int, locale: Locale): Int {
    if (Build.VERSION.SDK_INT >= 24) {
        @Suppress("NewApi")
        val it = IcuBreakIterator.getCharacterInstance(locale)
        it.setText(text)
        val boundary = it.preceding(index)
        return if (boundary == IcuBreakIterator.DONE) 0 else boundary
    }

    val it = JavaBreakIterator.getCharacterInstance(locale)
    it.setText(text)
    val boundary = it.preceding(index)
    return if (boundary == JavaBreakIterator.DONE) 0 else boundary
}

private fun Char.isHardLineBreak(): Boolean = this == '\n' || this == '\r' || this == '\u2028' || this == '\u2029'
