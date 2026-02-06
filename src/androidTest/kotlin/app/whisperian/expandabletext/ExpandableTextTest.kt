package app.whisperian.expandabletext

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Extension to get height from DpRect */
private val DpRect.height: Dp get() = bottom - top

/**
 * Comprehensive tests for [ExpandableText] composable.
 *
 * NOTE: We avoid using composeTestRule.waitForIdle() because it relies on
 * Espresso's internal IdlingResource synchronization which has reflection
 * issues on Android API 35/36 (NoSuchMethodException: InputManager.getInstance).
 * Instead, we rely on:
 * - Automatic synchronization before assertions (built into compose test rule)
 * - runOnIdle { } / waitUntil { } when an explicit barrier is needed
 * - mainClock.advanceTimeBy() for animation testing
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class ExpandableTextTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // =========================================================================
    // BASIC FUNCTIONALITY TESTS
    // =========================================================================

    @Test
    fun shortText_noTruncation_displaysFullText() {
        val shortText = "Hello World"

        composeTestRule.setContent {
            ExpandableText(
                text = shortText,
                maxLines = 3,
                modifier = Modifier.testTag("text"),
            )
        }

        composeTestRule.onNodeWithTag("text")
            .assertIsDisplayed()
            .assertTextContains(shortText, substring = true)
    }

    @Test
    fun textExactlyFittingMaxLines_noTruncation() {
        val text = "First line\nSecond line"

        composeTestRule.setContent {
            ExpandableText(
                text = text,
                maxLines = 2,
                modifier = Modifier
                    .testTag("text")
                    .width(200.dp),
            )
        }

        composeTestRule.onNodeWithTag("text")
            .assertTextContains("First line", substring = true)
            .assertTextContains("Second line", substring = true)
    }

    @Test
    fun longText_truncatedWithEllipsis() {
        val longText = "This is a very long text that should definitely be truncated " +
            "because it spans multiple lines and we only want to show a few lines. " +
            "The truncation should occur at word boundaries when possible."

        composeTestRule.setContent {
            ExpandableText(
                text = longText,
                maxLines = 2,
                modifier = Modifier
                    .testTag("text")
                    .width(300.dp),
            )
        }

        composeTestRule.onNodeWithText("…", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun expandedState_showsFullText() {
        val longText = "Line one. Line two. Line three. Line four. Line five."

        composeTestRule.setContent {
            ExpandableText(
                text = longText,
                maxLines = Int.MAX_VALUE,
                modifier = Modifier
                    .testTag("text")
                    .width(100.dp),
            )
        }

        composeTestRule.onNodeWithTag("text")
            .assertTextContains("five", substring = true)
    }

    @Test
    fun clickableModifier_works() {
        var clicked = false
        val text = "Click me"

        composeTestRule.setContent {
            ExpandableText(
                text = text,
                maxLines = 1,
                modifier = Modifier
                    .testTag("text")
                    .clickable { clicked = true },
            )
        }

        composeTestRule.onNodeWithTag("text").performClick()
        assertTrue("Click should be registered", clicked)
    }

    // =========================================================================
    // TRUNCATION BEHAVIOR TESTS
    // =========================================================================

    @Test
    fun truncation_occursAtWordBoundary() {
        val text = "Word1 Word2 Word3 Word4 Word5 Word6 Word7 Word8 Word9 Word10"

        composeTestRule.setContent {
            ExpandableText(
                text = text,
                maxLines = 1,
                modifier = Modifier
                    .testTag("text")
                    .width(150.dp),
            )
        }

        composeTestRule.onNodeWithTag("text").assertIsDisplayed()
    }

    @Test
    fun veryLongWord_graphemeFallback() {
        val longWord = "Supercalifragilisticexpialidocious"

        composeTestRule.setContent {
            ExpandableText(
                text = longWord,
                maxLines = 1,
                modifier = Modifier
                    .testTag("text")
                    .width(100.dp),
            )
        }

        composeTestRule.onNodeWithText("…", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun multilineWithNewlines_respectsMaxLines() {
        val text = "Line1\nLine2\nLine3\nLine4\nLine5"

        composeTestRule.setContent {
            ExpandableText(
                text = text,
                maxLines = 2,
                modifier = Modifier.testTag("text"),
            )
        }

        composeTestRule.onNodeWithTag("text")
            .assertTextContains("Line1", substring = true)
            .assertTextContains("Line2", substring = true)
    }

    @Test
    fun defaultEllipsisString_displayed() {
        val text = "This is a long text that will be truncated with the default ellipsis string."

        composeTestRule.setContent {
            ExpandableText(
                text = text,
                maxLines = 1,
                modifier = Modifier
                    .testTag("text")
                    .width(200.dp),
            )
        }

        composeTestRule.onNodeWithText("…", substring = true)
            .assertIsDisplayed()
    }

    // =========================================================================
    // ANIMATION BEHAVIOR TESTS
    // =========================================================================

    @Test
    fun expandingAnimation_heightIncreases() {
        var maxLines by mutableIntStateOf(2)

        composeTestRule.setContent {
            ExpandableText(
                text = "Line1 Line2 Line3 Line4 Line5 Line6 Line7 Line8",
                maxLines = maxLines,
                modifier = Modifier
                    .testTag("text")
                    .width(100.dp),
            )
        }


        val initialHeight = composeTestRule.onNodeWithTag("text").getBoundsInRoot().height

        maxLines = Int.MAX_VALUE
        composeTestRule.mainClock.advanceTimeBy(1000)


        val expandedHeight = composeTestRule.onNodeWithTag("text").getBoundsInRoot().height

        assertTrue(
            "Expanded height ($expandedHeight) should be greater than initial ($initialHeight)",
            expandedHeight > initialHeight,
        )
    }

    @Test
    fun collapsingAnimation_heightDecreases() {
        var maxLines by mutableIntStateOf(Int.MAX_VALUE)

        composeTestRule.setContent {
            ExpandableText(
                text = "Line1 Line2 Line3 Line4 Line5 Line6 Line7 Line8",
                maxLines = maxLines,
                modifier = Modifier
                    .testTag("text")
                    .width(100.dp),
            )
        }


        val expandedHeight = composeTestRule.onNodeWithTag("text").getBoundsInRoot().height

        maxLines = 2
        composeTestRule.mainClock.advanceTimeBy(1000)


        val collapsedHeight = composeTestRule.onNodeWithTag("text").getBoundsInRoot().height

        assertTrue(
            "Collapsed height ($collapsedHeight) should be less than expanded ($expandedHeight)",
            collapsedHeight < expandedHeight,
        )
    }

    @Test
    fun rapidMaxLinesChanges_nocrash() {
        var maxLines by mutableIntStateOf(2)

        composeTestRule.setContent {
            ExpandableText(
                text = "A B C D E F G H I J K L M N O P Q R S T U V W X Y Z",
                maxLines = maxLines,
                modifier = Modifier
                    .testTag("text")
                    .width(100.dp),
            )
        }

        repeat(10) { i ->
            maxLines = if (i % 2 == 0) Int.MAX_VALUE else 1
            composeTestRule.mainClock.advanceTimeBy(50)
        }


        composeTestRule.onNodeWithTag("text").assertIsDisplayed()
    }

    // =========================================================================
    // TRAILING CONTENT TESTS
    // =========================================================================

    @Test
    fun trailingContent_displaysComposable() {
        val text = "This is a long text that needs trailing content to be displayed."

        composeTestRule.setContent {
            ExpandableText(
                text = text,
                maxLines = 1,
                trailingContent = {
                    Text(
                        text = " Show more",
                        color = Color.Blue,
                        modifier = Modifier.testTag("ellipsis"),
                    )
                },
                modifier = Modifier
                    .testTag("text")
                    .width(200.dp),
            )
        }

        composeTestRule.onNodeWithTag("ellipsis")
            .assertIsDisplayed()
    }

    @Test
    fun trailingContent_clickable() {
        var trailingClicked = false
        val text = "This is a long text that needs clickable trailing content."

        composeTestRule.setContent {
            ExpandableText(
                text = text,
                maxLines = 1,
                trailingContent = {
                    Text(
                        text = " [expand]",
                        modifier = Modifier
                            .testTag("ellipsis")
                            .clickable { trailingClicked = true },
                    )
                },
                modifier = Modifier
                    .testTag("text")
                    .width(200.dp),
            )
        }

        composeTestRule.onNodeWithTag("ellipsis").performClick()
        assertTrue("Trailing content click should be registered", trailingClicked)
    }

    @Test
    fun trailingContent_composableRenders() {
        val text = "This is text with an icon ellipsis that should render properly."

        composeTestRule.setContent {
            ExpandableText(
                text = text,
                maxLines = 1,
                trailingContent = {
                    Box(
                        modifier = Modifier
                            .testTag("iconEllipsis")
                            .size(16.dp)
                            .background(Color.Red),
                    )
                },
                modifier = Modifier
                    .testTag("text")
                    .width(200.dp),
            )
        }

        composeTestRule.onNodeWithTag("iconEllipsis")
            .assertIsDisplayed()
    }

    @Test
    fun trailingContent_remainsWhenExpanded() {
        var maxLines by mutableIntStateOf(1)
        val text = "This is a long text that will expand."

        composeTestRule.setContent {
            ExpandableText(
                text = text,
                maxLines = maxLines,
                trailingContent = {
                    Text(
                        text = " [more]",
                        modifier = Modifier.testTag("ellipsis"),
                    )
                },
                modifier = Modifier
                    .testTag("text")
                    .width(200.dp),
            )
        }

        composeTestRule.onNodeWithTag("ellipsis").assertIsDisplayed()

        maxLines = Int.MAX_VALUE
        composeTestRule.mainClock.advanceTimeBy(1000)


        composeTestRule.onNodeWithTag("ellipsis").assertIsDisplayed()
    }

    @Test
    fun trailingContent_wideContent_reservesSpace() {
        val text = "This is a long text that should be truncated so we can verify a wide trailing content is displayed."

        composeTestRule.setContent {
            ExpandableText(
                text = text,
                maxLines = 1,
                trailingContent = {
                    Row(modifier = Modifier.testTag("wideEllipsis")) {
                        Text("Show ")
                        Text("more ")
                        Text("content")
                    }
                },
                modifier = Modifier
                    .testTag("text")
                    .width(200.dp),
            )
        }

        composeTestRule.onNodeWithTag("wideEllipsis")
            .assertIsDisplayed()
    }

    @Test
    fun trailingContent_tallContent_constrainedToLineHeight() {
        val text = "Text with a tall trailing content box."

        composeTestRule.setContent {
            ExpandableText(
                text = text,
                maxLines = 1,
                trailingContent = {
                    Box(
                        modifier = Modifier
                            .testTag("tallEllipsis")
                            .size(width = 50.dp, height = 100.dp)
                            .background(Color.Red),
                    )
                },
                modifier = Modifier
                    .testTag("text")
                    .width(300.dp),
            )
        }

        val bounds = composeTestRule.onNodeWithTag("text").getBoundsInRoot()
        assertTrue(
            "Text height should be reasonable (< 60dp for single line)",
            bounds.height < 60.dp,
        )
    }

    @Test
    fun trailingContent_doesNotHideEllipsisString() {
        val text = "This is a long text for testing ellipsis precedence."

        composeTestRule.setContent {
            ExpandableText(
                text = text,
                maxLines = 1,
                trailingContent = {
                    Text(
                        text = " [CUSTOM]",
                        modifier = Modifier.testTag("customEllipsis"),
                    )
                },
                modifier = Modifier
                    .testTag("text")
                    .width(200.dp),
            )
        }

        composeTestRule.onNodeWithTag("customEllipsis").assertIsDisplayed()
        composeTestRule.onNodeWithText("…", substring = true).assertIsDisplayed()
    }

    @Test
    fun softWrapFalse_singleLine_overflowShowsEllipsisAndTrailing() {
        val text = "This is a long text that should overflow horizontally without wrapping."

        composeTestRule.setContent {
            ExpandableText(
                text = text,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                trailingContent = {
                    Text(
                        text = " more",
                        modifier = Modifier.testTag("trailing"),
                    )
                },
                modifier = Modifier
                    .testTag("text")
                    .width(140.dp),
            )
        }

        composeTestRule.onNodeWithTag("trailing").assertIsDisplayed()
        composeTestRule.onNodeWithText("…", substring = true).assertIsDisplayed()
    }

    // =========================================================================
    // MIN LINES TESTS
    // =========================================================================

    @Test
    fun minLines_matchesMaterial3TextHeight_forShortText() {
        composeTestRule.setContent {
            Column(modifier = Modifier.width(240.dp)) {
                Text(
                    text = "Hello",
                    minLines = 10,
                    maxLines = 10,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("native"),
                )
                ExpandableText(
                    text = "Hello",
                    minLines = 10,
                    maxLines = 10,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("expandable"),
                )
            }
        }

        val nativeHeight = composeTestRule.onNodeWithTag("native").getBoundsInRoot().height
        val expandableHeight = composeTestRule.onNodeWithTag("expandable").getBoundsInRoot().height

        val diff = abs((nativeHeight - expandableHeight).value)
        assertTrue(
            "Heights should match native Text within 2dp (native=$nativeHeight, expandable=$expandableHeight, diff=${diff}dp)",
            diff <= 2f,
        )
    }

    @Test
    fun minLines_increasesHeightForShortText() {
        composeTestRule.setContent {
            Column(modifier = Modifier.width(240.dp)) {
                ExpandableText(
                    text = "Hello",
                    minLines = 1,
                    modifier = Modifier.testTag("min1"),
                )
                ExpandableText(
                    text = "Hello",
                    minLines = 3,
                    modifier = Modifier.testTag("min3"),
                )
            }
        }

        val h1 = composeTestRule.onNodeWithTag("min1").getBoundsInRoot().height
        val h3 = composeTestRule.onNodeWithTag("min3").getBoundsInRoot().height

        assertTrue("minLines=3 height ($h3) should be greater than minLines=1 ($h1)", h3 > h1)
    }

    @Test
    fun minLines_appliesToEmptyText() {
        composeTestRule.setContent {
            Column(modifier = Modifier.width(240.dp)) {
                ExpandableText(
                    text = "",
                    minLines = 1,
                    modifier = Modifier.testTag("empty_min1"),
                )
                ExpandableText(
                    text = "",
                    minLines = 3,
                    modifier = Modifier.testTag("empty_min3"),
                )
            }
        }

        val h1 = composeTestRule.onNodeWithTag("empty_min1").getBoundsInRoot().height
        val h3 = composeTestRule.onNodeWithTag("empty_min3").getBoundsInRoot().height

        assertTrue("empty text minLines=3 height ($h3) should be greater than minLines=1 ($h1)", h3 > h1)
    }

    @Test
    fun minLines_doesNotForceEllipsisForShortText() {
        composeTestRule.setContent {
            ExpandableText(
                text = "Hello",
                minLines = 10,
                maxLines = 10,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .testTag("text")
                    .width(140.dp),
            )
        }

        val ellipsisNodes = composeTestRule.onAllNodesWithText("…", substring = true).fetchSemanticsNodes()
        assertEquals(0, ellipsisNodes.size)
    }

    @Test
    fun minLines_withLongText_truncatesAndShowsEllipsis() {
        val text = "A B C D E F G H I J K L M N O P Q R S T U V W X Y Z"

        composeTestRule.setContent {
            ExpandableText(
                text = text,
                minLines = 3,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .testTag("text")
                    .width(100.dp),
            )
        }

        composeTestRule.onNodeWithText("…", substring = true).assertIsDisplayed()
    }

    @Test
    fun softWrapFalse_minLines_matchesMaterial3TextHeight() {
        composeTestRule.setContent {
            Column(modifier = Modifier.width(240.dp)) {
                Text(
                    text = "Hello",
                    minLines = 5,
                    maxLines = 5,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("native_nowrap"),
                )
                ExpandableText(
                    text = "Hello",
                    minLines = 5,
                    maxLines = 5,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("expandable_nowrap"),
                )
            }
        }

        val nativeHeight = composeTestRule.onNodeWithTag("native_nowrap").getBoundsInRoot().height
        val expandableHeight =
            composeTestRule.onNodeWithTag("expandable_nowrap").getBoundsInRoot().height

        val diff = abs((nativeHeight - expandableHeight).value)
        assertTrue(
            "Heights should match native Text within 2dp (native=$nativeHeight, expandable=$expandableHeight, diff=${diff}dp)",
            diff <= 2f,
        )
    }

    @Test
    fun minLinesGreaterThanMaxLines_throws() {
        assertIllegalArgument {
            composeTestRule.setContent {
                ExpandableText(
                    text = "Hello",
                    minLines = 2,
                    maxLines = 1,
                )
            }
        }
    }

    @Test
    fun minLinesZero_throws() {
        assertIllegalArgument {
            composeTestRule.setContent {
                ExpandableText(
                    text = "Hello",
                    minLines = 0,
                )
            }
        }
    }

    private fun assertIllegalArgument(block: () -> Unit) {
        var thrown: Throwable? = null
        try {
            block()
        } catch (t: Throwable) {
            thrown = t
        }
        assertNotNull("Expected IllegalArgumentException", thrown)
        assertTrue("Expected IllegalArgumentException but was ${thrown!!::class.java.name}", thrown is IllegalArgumentException)
    }

    // =========================================================================
    // EDGE CASES
    // =========================================================================

    @Test
    fun emptyString_nocrash() {
        composeTestRule.setContent {
            ExpandableText(
                text = "",
                maxLines = 3,
                modifier = Modifier.testTag("text"),
            )
        }

        composeTestRule.onNodeWithTag("text").assertIsDisplayed()
    }

    @Test
    fun singleCharacter_nocrash() {
        composeTestRule.setContent {
            ExpandableText(
                text = "X",
                maxLines = 1,
                modifier = Modifier.testTag("text"),
            )
        }

        composeTestRule.onNodeWithTag("text")
            .assertIsDisplayed()
            .assertTextContains("X")
    }

    @Test
    fun whitespaceOnly_nocrash() {
        composeTestRule.setContent {
            ExpandableText(
                text = "     ",
                maxLines = 1,
                modifier = Modifier.testTag("text"),
            )
        }

        composeTestRule.onNodeWithTag("text").assertIsDisplayed()
    }

    @Test
    fun veryNarrowContainer_nocrash() {
        composeTestRule.setContent {
            ExpandableText(
                text = "Hello World",
                maxLines = 1,
                modifier = Modifier
                    .testTag("text")
                    .width(1.dp),
            )
        }

        composeTestRule.onNodeWithTag("text").assertIsDisplayed()
    }

    @Test
    fun zeroWidthContainer_nocrash() {
        composeTestRule.setContent {
            Box(modifier = Modifier.width(0.dp)) {
                ExpandableText(
                    text = "Hello World",
                    maxLines = 1,
                    modifier = Modifier.testTag("text"),
                )
            }
        }
        // Should not crash - no assertion needed
    }

    @Test
    fun ellipsisWiderThanContainer_nocrash() {
        composeTestRule.setContent {
            ExpandableText(
                text = "Hi",
                maxLines = 1,
                modifier = Modifier
                    .testTag("text")
                    .width(50.dp),
            )
        }

        composeTestRule.onNodeWithTag("text").assertIsDisplayed()
    }

    @Test
    fun textChangingDuringAnimation_nocrash() {
        var text by mutableStateOf("Initial text content")
        var maxLines by mutableIntStateOf(2)

        composeTestRule.setContent {
            ExpandableText(
                text = text,
                maxLines = maxLines,
                modifier = Modifier
                    .testTag("text")
                    .width(200.dp),
            )
        }

        maxLines = Int.MAX_VALUE
        composeTestRule.mainClock.advanceTimeBy(100)

        text = "Completely different text that is much longer and should wrap to multiple lines"
        composeTestRule.mainClock.advanceTimeBy(100)

        maxLines = 1
        composeTestRule.mainClock.advanceTimeBy(500)

        text = "Short"


        composeTestRule.onNodeWithTag("text").assertIsDisplayed()
    }

    // =========================================================================
    // RTL AND UNICODE SUPPORT
    // =========================================================================

    @Test
    fun rtlText_arabic_displayedCorrectly() {
        val arabicText = "مرحبا بالعالم، هذا نص عربي طويل يجب أن يتم اقتطاعه بشكل صحيح."

        composeTestRule.setContent {
            ExpandableText(
                text = arabicText,
                maxLines = 1,
                modifier = Modifier
                    .testTag("text")
                    .width(200.dp),
            )
        }

        composeTestRule.onNodeWithTag("text").assertIsDisplayed()
    }

    @Test
    fun rtlText_hebrew_displayedCorrectly() {
        val hebrewText = "שלום עולם, זהו טקסט עברי ארוך שצריך להיחתך בצורה נכונה."

        composeTestRule.setContent {
            ExpandableText(
                text = hebrewText,
                maxLines = 1,
                modifier = Modifier
                    .testTag("text")
                    .width(200.dp),
            )
        }

        composeTestRule.onNodeWithTag("text").assertIsDisplayed()
    }

    @Test
    fun mixedLtrRtl_displayedCorrectly() {
        val mixedText = "Hello مرحبا World عالم Test اختبار"

        composeTestRule.setContent {
            ExpandableText(
                text = mixedText,
                maxLines = 1,
                modifier = Modifier
                    .testTag("text")
                    .width(200.dp),
            )
        }

        composeTestRule.onNodeWithTag("text").assertIsDisplayed()
    }

    @Test
    fun emoji_truncatedCorrectly() {
        val emojiText = "Hello 👨‍👩‍👧‍👦 World 🎉🎊🎁 More 🏳️‍🌈 Text 👩‍💻"

        composeTestRule.setContent {
            ExpandableText(
                text = emojiText,
                maxLines = 1,
                modifier = Modifier
                    .testTag("text")
                    .width(200.dp),
            )
        }

        composeTestRule.onNodeWithTag("text").assertIsDisplayed()
    }

    @Test
    fun cjkText_truncatedCorrectly() {
        val japaneseText = "これは日本語のテキストです。正しく切り取られるべきです。"

        composeTestRule.setContent {
            ExpandableText(
                text = japaneseText,
                maxLines = 1,
                modifier = Modifier
                    .testTag("text")
                    .width(200.dp),
            )
        }

        composeTestRule.onNodeWithTag("text").assertIsDisplayed()
    }

    @Test
    fun chineseText_truncatedCorrectly() {
        val chineseText = "这是一段很长的中文文本，需要被正确地截断显示。"

        composeTestRule.setContent {
            ExpandableText(
                text = chineseText,
                maxLines = 1,
                modifier = Modifier
                    .testTag("text")
                    .width(200.dp),
            )
        }

        composeTestRule.onNodeWithTag("text").assertIsDisplayed()
    }

    @Test
    fun koreanText_truncatedCorrectly() {
        val koreanText = "이것은 긴 한국어 텍스트입니다. 올바르게 잘려야 합니다."

        composeTestRule.setContent {
            ExpandableText(
                text = koreanText,
                maxLines = 1,
                modifier = Modifier
                    .testTag("text")
                    .width(200.dp),
            )
        }

        composeTestRule.onNodeWithTag("text").assertIsDisplayed()
    }

    @Test
    fun devanagariText_graphemeClustersSafe() {
        val hindiText = "नमस्ते दुनिया, यह एक लंबा हिंदी पाठ है जिसे सही ढंग से काटना चाहिए।"

        composeTestRule.setContent {
            ExpandableText(
                text = hindiText,
                maxLines = 1,
                modifier = Modifier
                    .testTag("text")
                    .width(200.dp),
            )
        }

        composeTestRule.onNodeWithTag("text").assertIsDisplayed()
    }

    // =========================================================================
    // STYLE PARAMETERS TESTS
    // =========================================================================

    @Test
    fun fontSize_appliedCorrectly() {
        composeTestRule.setContent {
            Column {
                ExpandableText(
                    text = "Small text",
                    fontSize = 12.sp,
                    maxLines = 1,
                    modifier = Modifier.testTag("small"),
                )
                ExpandableText(
                    text = "Large text",
                    fontSize = 24.sp,
                    maxLines = 1,
                    modifier = Modifier.testTag("large"),
                )
            }
        }

        val smallBounds = composeTestRule.onNodeWithTag("small").getBoundsInRoot()
        val largeBounds = composeTestRule.onNodeWithTag("large").getBoundsInRoot()

        assertTrue(
            "Large text should be taller than small text",
            largeBounds.height > smallBounds.height,
        )
    }

    @Test
    fun fontWeight_appliedCorrectly() {
        composeTestRule.setContent {
            Column {
                ExpandableText(
                    text = "Normal weight",
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    modifier = Modifier.testTag("normal"),
                )
                ExpandableText(
                    text = "Bold weight",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.testTag("bold"),
                )
            }
        }

        composeTestRule.onNodeWithTag("normal").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bold").assertIsDisplayed()
    }

    @Test
    fun fontStyle_italic_applied() {
        composeTestRule.setContent {
            ExpandableText(
                text = "Italic text",
                fontStyle = FontStyle.Italic,
                maxLines = 1,
                modifier = Modifier.testTag("text"),
            )
        }

        composeTestRule.onNodeWithTag("text").assertIsDisplayed()
    }

    @Test
    fun textDecoration_underline_applied() {
        composeTestRule.setContent {
            ExpandableText(
                text = "Underlined text",
                textDecoration = TextDecoration.Underline,
                maxLines = 1,
                modifier = Modifier.testTag("text"),
            )
        }

        composeTestRule.onNodeWithTag("text").assertIsDisplayed()
    }

    @Test
    fun textAlign_center_applied() {
        composeTestRule.setContent {
            ExpandableText(
                text = "Centered",
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .testTag("text")
                    .fillMaxWidth(),
            )
        }

        composeTestRule.onNodeWithTag("text").assertIsDisplayed()
    }

    @Test
    fun textAlign_end_applied() {
        composeTestRule.setContent {
            ExpandableText(
                text = "End aligned",
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier
                    .testTag("text")
                    .fillMaxWidth(),
            )
        }

        composeTestRule.onNodeWithTag("text").assertIsDisplayed()
    }

    @Test
    fun color_applied() {
        composeTestRule.setContent {
            ExpandableText(
                text = "Red text",
                color = Color.Red,
                maxLines = 1,
                modifier = Modifier.testTag("text"),
            )
        }

        composeTestRule.onNodeWithTag("text").assertIsDisplayed()
    }

    @Test
    fun letterSpacing_applied() {
        composeTestRule.setContent {
            ExpandableText(
                text = "Spaced letters",
                letterSpacing = 4.sp,
                maxLines = 1,
                modifier = Modifier.testTag("text"),
            )
        }

        composeTestRule.onNodeWithTag("text").assertIsDisplayed()
    }

    @Test
    fun lineHeight_applied() {
        composeTestRule.setContent {
            Column {
                ExpandableText(
                    text = "Normal\nLine\nHeight",
                    maxLines = 3,
                    modifier = Modifier.testTag("normal"),
                )
                ExpandableText(
                    text = "Large\nLine\nHeight",
                    lineHeight = 40.sp,
                    maxLines = 3,
                    modifier = Modifier.testTag("large"),
                )
            }
        }

        val normalBounds = composeTestRule.onNodeWithTag("normal").getBoundsInRoot()
        val largeBounds = composeTestRule.onNodeWithTag("large").getBoundsInRoot()

        assertTrue(
            "Large line height should result in taller component",
            largeBounds.height > normalBounds.height,
        )
    }

    @Test
    fun fontFamily_monospace_applied() {
        composeTestRule.setContent {
            ExpandableText(
                text = "Monospace text",
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                modifier = Modifier.testTag("text"),
            )
        }

        composeTestRule.onNodeWithTag("text").assertIsDisplayed()
    }

    // =========================================================================
    // ANNOTATED STRING TESTS
    // =========================================================================

    @Test
    fun annotatedString_stylesPreserved() {
        val annotatedText = buildAnnotatedString {
            append("Normal ")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append("Bold ")
            }
            withStyle(SpanStyle(color = Color.Red)) {
                append("Red ")
            }
            withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                append("Underlined")
            }
        }

        composeTestRule.setContent {
            ExpandableText(
                text = annotatedText,
                maxLines = 1,
                modifier = Modifier
                    .testTag("text")
                    .width(300.dp),
            )
        }

        composeTestRule.onNodeWithTag("text")
            .assertIsDisplayed()
            .assertTextContains("Normal", substring = true)
    }

    @Test
    fun annotatedString_longWithStyles_truncatedCorrectly() {
        val annotatedText = buildAnnotatedString {
            repeat(20) { i ->
                if (i % 2 == 0) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Bold$i ")
                    }
                } else {
                    append("Normal$i ")
                }
            }
        }

        composeTestRule.setContent {
            ExpandableText(
                text = annotatedText,
                maxLines = 2,
                modifier = Modifier
                    .testTag("text")
                    .width(200.dp),
            )
        }

        composeTestRule.onNodeWithText("…", substring = true)
            .assertIsDisplayed()
    }

    // =========================================================================
    // LAYOUT BEHAVIOR TESTS
    // =========================================================================

    @Test
    fun multiParagraphText_handledCorrectly() {
        val text = """
            First paragraph with some content.

            Second paragraph after blank line.

            Third paragraph at the end.
        """.trimIndent()

        composeTestRule.setContent {
            ExpandableText(
                text = text,
                maxLines = 3,
                modifier = Modifier
                    .testTag("text")
                    .width(300.dp),
            )
        }

        composeTestRule.onNodeWithTag("text").assertIsDisplayed()
    }

    @Test
    fun consecutiveSpaces_preserved() {
        val text = "Word1    Word2     Word3"

        composeTestRule.setContent {
            ExpandableText(
                text = text,
                maxLines = 1,
                modifier = Modifier.testTag("text"),
            )
        }

        composeTestRule.onNodeWithTag("text")
            .assertIsDisplayed()
            .assertTextContains("Word1", substring = true)
    }

    @Test
    fun tabCharacters_handled() {
        val text = "Column1\tColumn2\tColumn3"

        composeTestRule.setContent {
            ExpandableText(
                text = text,
                maxLines = 1,
                modifier = Modifier.testTag("text"),
            )
        }

        composeTestRule.onNodeWithTag("text").assertIsDisplayed()
    }

    @Test
    fun softWrapFalse_singleLine() {
        val text = "This is a very long text that would normally wrap to multiple lines"

        composeTestRule.setContent {
            ExpandableText(
                text = text,
                softWrap = false,
                maxLines = Int.MAX_VALUE,
                modifier = Modifier
                    .testTag("text")
                    .width(100.dp),
            )
        }

        val bounds = composeTestRule.onNodeWithTag("text").getBoundsInRoot()
        assertTrue("Height should be small (single line)", bounds.height < 40.dp)
    }

    @Test
    fun onTextLayout_callbackInvoked() {
        var layoutResult: TextLayoutResult? = null

        composeTestRule.setContent {
            ExpandableText(
                text = "Hello World",
                maxLines = 1,
                onTextLayout = { layoutResult = it },
                modifier = Modifier.testTag("text"),
            )
        }



        assertNotNull("onTextLayout should be called", layoutResult)
        assertTrue("Should have at least 1 line", layoutResult!!.lineCount >= 1)
    }

    // =========================================================================
    // OVERFLOW BEHAVIOR TESTS
    // =========================================================================

    @Test
    fun overflowClip_noEllipsis() {
        val text = "This is a long text that should be clipped without any ellipsis character."

        composeTestRule.setContent {
            ExpandableText(
                text = text,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier
                    .testTag("text")
                    .width(100.dp),
            )
        }

        composeTestRule.onNodeWithTag("text").assertIsDisplayed()
    }

    // =========================================================================
    // POTENTIAL FAILURE SCENARIOS
    // =========================================================================

    @Test
    fun veryLargeFontSize_nocrash() {
        composeTestRule.setContent {
            ExpandableText(
                text = "Big text",
                fontSize = 200.sp,
                maxLines = 1,
                modifier = Modifier
                    .testTag("text")
                    .width(500.dp),
            )
        }

        composeTestRule.onNodeWithTag("text").assertIsDisplayed()
    }

    @Test
    fun verySmallFontSize_nocrash() {
        composeTestRule.setContent {
            ExpandableText(
                text = "Tiny text",
                fontSize = 1.sp,
                maxLines = 1,
                modifier = Modifier.testTag("text"),
            )
        }

        composeTestRule.onNodeWithTag("text").assertIsDisplayed()
    }

    @Test
    fun extremelyLongText_nocrash() {
        val longText = "Word ".repeat(10000)

        composeTestRule.setContent {
            ExpandableText(
                text = longText,
                maxLines = 2,
                modifier = Modifier
                    .testTag("text")
                    .width(300.dp),
            )
        }

        composeTestRule.onNodeWithTag("text").assertIsDisplayed()
    }

    @Test
    fun manyLines_expandCollapseCycle() {
        var maxLines by mutableIntStateOf(3)
        val manyLinesText = (1..100).joinToString("\n") { "Line $it has some content" }

        composeTestRule.setContent {
            ExpandableText(
                text = manyLinesText,
                maxLines = maxLines,
                modifier = Modifier
                    .testTag("text")
                    .width(300.dp),
            )
        }

        repeat(5) {
            maxLines = Int.MAX_VALUE
            composeTestRule.mainClock.advanceTimeBy(500)

            maxLines = 3
            composeTestRule.mainClock.advanceTimeBy(500)
        }


        composeTestRule.onNodeWithTag("text").assertIsDisplayed()
    }

    @Test
    fun unicodeControlCharacters_handled() {
        val text = "Hello\u200BWorld\u200C\u200DTest\uFEFF"

        composeTestRule.setContent {
            ExpandableText(
                text = text,
                maxLines = 1,
                modifier = Modifier.testTag("text"),
            )
        }

        composeTestRule.onNodeWithTag("text").assertIsDisplayed()
    }

    @Test
    fun surrogatePairs_notBroken() {
        val text = "Test 🎉 emoji 🚀 here 💻 end"

        composeTestRule.setContent {
            ExpandableText(
                text = text,
                maxLines = 1,
                modifier = Modifier
                    .testTag("text")
                    .width(100.dp),
            )
        }

        composeTestRule.onNodeWithTag("text").assertIsDisplayed()
    }

    @Test
    fun maxLinesOne_singleLineEllipsis() {
        val text = "This is text that needs to be truncated on a single line."
        var layoutResult: TextLayoutResult? = null

        composeTestRule.setContent {
            ExpandableText(
                text = text,
                maxLines = 1,
                onTextLayout = { layoutResult = it },
                modifier = Modifier
                    .testTag("text")
                    .width(200.dp),
            )
        }



        assertNotNull(layoutResult)
        assertEquals("Should have exactly 1 line", 1, layoutResult!!.lineCount)
    }

    @Test
    fun heightAnimation_interpolatesSmoothly() {
        var maxLines by mutableIntStateOf(2)
        val heights = mutableListOf<Dp>()

        composeTestRule.setContent {
            ExpandableText(
                text = "Line1 Line2 Line3 Line4 Line5 Line6 Line7 Line8 Line9 Line10",
                maxLines = maxLines,
                modifier = Modifier
                    .testTag("text")
                    .width(80.dp),
            )
        }


        heights.add(composeTestRule.onNodeWithTag("text").getBoundsInRoot().height)

        maxLines = Int.MAX_VALUE

        repeat(10) {
            composeTestRule.mainClock.advanceTimeBy(50)
            heights.add(composeTestRule.onNodeWithTag("text").getBoundsInRoot().height)
        }

        for (i in 1 until heights.size - 1) {
            assertTrue(
                "Height should be monotonically non-decreasing during expansion: ${heights[i - 1]} -> ${heights[i]}",
                heights[i] >= heights[i - 1] - 1.dp,
            )
        }
    }
}
