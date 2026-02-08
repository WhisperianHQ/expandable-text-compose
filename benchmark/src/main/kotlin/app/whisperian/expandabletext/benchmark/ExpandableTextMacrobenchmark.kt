package app.whisperian.expandabletext.benchmark

import android.content.Intent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class ExpandableTextMacrobenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun expandableText_stressToggleAndScroll() {
        runScenario(
            mode = MODE_EXPANDABLE_TEXT,
            textRepeat = 8,
            itemCount = 120,
            cpuLoadThreads = 0,
        )
    }

    @Test
    fun animateContentSize_stressToggleAndScroll() {
        runScenario(
            mode = MODE_ANIMATE_CONTENT_SIZE,
            textRepeat = 8,
            itemCount = 120,
            cpuLoadThreads = 0,
        )
    }

    @Test
    fun expandableText_underCpuLoad() {
        runScenario(
            mode = MODE_EXPANDABLE_TEXT,
            textRepeat = 8,
            itemCount = 120,
            cpuLoadThreads = 2,
        )
    }

    @Test
    fun animateContentSize_underCpuLoad() {
        runScenario(
            mode = MODE_ANIMATE_CONTENT_SIZE,
            textRepeat = 8,
            itemCount = 120,
            cpuLoadThreads = 2,
        )
    }

    private fun runScenario(
        mode: String,
        textRepeat: Int,
        itemCount: Int,
        cpuLoadThreads: Int,
    ) {
        benchmarkRule.measureRepeated(
            packageName = SAMPLE_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(),
            iterations = 10,
            setupBlock = {
                pressHome()
                startActivityAndWait { intent ->
                    intent.action = Intent.ACTION_MAIN
                    intent.addCategory(Intent.CATEGORY_LAUNCHER)
                    intent.`package` = SAMPLE_PACKAGE
                    intent.putExtra(EXTRA_MODE, mode)
                    intent.putExtra(EXTRA_TEXT_REPEAT, textRepeat)
                    intent.putExtra(EXTRA_ITEM_COUNT, itemCount)
                    intent.putExtra(EXTRA_CPU_LOAD_THREADS, cpuLoadThreads)
                    intent.putExtra(EXTRA_EXPAND_ALL, true)
                }

                checkNotNull(device.wait(Until.findObject(By.desc(TOGGLE_BUTTON_CONTENT_DESCRIPTION)), 5_000))
                checkNotNull(device.wait(Until.findObject(By.desc(LIST_CONTENT_DESCRIPTION)), 5_000))
            },
        ) {
            val toggleButton = checkNotNull(
                device.wait(Until.findObject(By.desc(TOGGLE_BUTTON_CONTENT_DESCRIPTION)), 5_000),
            )
            val list = checkNotNull(
                device.wait(Until.findObject(By.desc(LIST_CONTENT_DESCRIPTION)), 5_000),
            )

            // Toggle expand/collapse a few times.
            repeat(8) {
                toggleButton.click()
                device.waitForIdle()
            }

            // Scroll the list to include additional layout work during/after toggles.
            repeat(6) {
                list.swipe(Direction.UP, /* percent = */ 0.8f, /* speed = */ 3000)
                device.waitForIdle()
            }
        }
    }

    private companion object {
        const val SAMPLE_PACKAGE = "app.whisperian.expandabletext.sample"
        const val EXTRA_MODE = "mode"
        const val EXTRA_TEXT_REPEAT = "text_repeat"
        const val EXTRA_ITEM_COUNT = "item_count"
        const val EXTRA_CPU_LOAD_THREADS = "cpu_load_threads"
        const val EXTRA_EXPAND_ALL = "expand_all"
        const val MODE_EXPANDABLE_TEXT = "expandable_text"
        const val MODE_ANIMATE_CONTENT_SIZE = "animate_content_size"
        const val TOGGLE_BUTTON_CONTENT_DESCRIPTION = "benchmark_toggle_button"
        const val LIST_CONTENT_DESCRIPTION = "benchmark_list"
    }
}
