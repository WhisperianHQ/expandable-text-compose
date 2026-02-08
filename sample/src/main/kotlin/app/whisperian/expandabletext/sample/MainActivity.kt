package app.whisperian.expandabletext.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.whisperian.expandabletext.ExpandableText
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_EXPANDABLE_TEXT
        val textRepeat = intent.getIntExtra(EXTRA_TEXT_REPEAT, 1).coerceAtLeast(1)
        val itemCount = intent.getIntExtra(EXTRA_ITEM_COUNT, 100).coerceAtLeast(1)
        val cpuLoadThreads = intent.getIntExtra(EXTRA_CPU_LOAD_THREADS, 0).coerceAtLeast(0)
        val expandAll = intent.getBooleanExtra(EXTRA_EXPAND_ALL, false)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SampleScreen(
                        mode = mode,
                        textRepeat = textRepeat,
                        itemCount = itemCount,
                        cpuLoadThreads = cpuLoadThreads,
                        expandAll = expandAll,
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_MODE = "mode"
        const val EXTRA_TEXT_REPEAT = "text_repeat"
        const val EXTRA_ITEM_COUNT = "item_count"
        const val EXTRA_CPU_LOAD_THREADS = "cpu_load_threads"
        const val EXTRA_EXPAND_ALL = "expand_all"
        const val MODE_EXPANDABLE_TEXT = "expandable_text"
        const val MODE_ANIMATE_CONTENT_SIZE = "animate_content_size"
    }
}

private const val TOGGLE_BUTTON_TEXT = "TOGGLE"
private const val TOGGLE_BUTTON_CONTENT_DESCRIPTION = "benchmark_toggle_button"
private const val LIST_CONTENT_DESCRIPTION = "benchmark_list"

private fun longText(index: Int, repeatCount: Int): String = buildString {
    repeat(repeatCount) {
        append("Item $index: Lorem ipsum dolor sit amet, consectetur adipiscing elit. ")
        append("Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. ")
        append("Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ")
        append("ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit ")
        append("in voluptate velit esse cillum dolore eu fugiat nulla pariatur. ")
    }
}

@Suppress("FunctionNaming")
@androidx.compose.runtime.Composable
private fun SampleScreen(
    mode: String,
    textRepeat: Int,
    itemCount: Int,
    cpuLoadThreads: Int,
    expandAll: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    val itemTexts = remember(itemCount, textRepeat) {
        (0 until itemCount).map { index -> longText(index, textRepeat) }
    }

    if (cpuLoadThreads > 0) {
        DisposableEffect(cpuLoadThreads) {
            val keepRunning = AtomicBoolean(true)
            val workers = List(cpuLoadThreads) { workerIndex ->
                thread(start = true, isDaemon = true, name = "benchmark-cpu-load-$workerIndex") {
                    var accumulator = workerIndex + 1L
                    while (keepRunning.get()) {
                        accumulator = accumulator * 1664525L + 1013904223L
                        if ((accumulator and 0x3FFF) == 0L) {
                            Thread.yield()
                        }
                    }
                }
            }
            onDispose {
                keepRunning.set(false)
                workers.forEach { worker -> worker.join(100) }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Mode: $mode",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "expanded=$expanded repeat=$textRepeat cpuLoadThreads=$cpuLoadThreads",
            style = MaterialTheme.typography.bodySmall,
        )

        Button(
            onClick = { expanded = !expanded },
            modifier = Modifier
                .padding(top = 12.dp, bottom = 12.dp)
                .semantics {
                    contentDescription = TOGGLE_BUTTON_CONTENT_DESCRIPTION
                },
        ) {
            Text(TOGGLE_BUTTON_TEXT)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .semantics {
                    contentDescription = LIST_CONTENT_DESCRIPTION
                },
        ) {
            itemsIndexed(itemTexts) { index, text ->
                val maxLines = if (expanded && (expandAll || index == 0)) Int.MAX_VALUE else 3

                when (mode) {
                    MainActivity.MODE_ANIMATE_CONTENT_SIZE -> {
                        Column(
                            modifier = Modifier.animateContentSize(
                                animationSpec = tween(durationMillis = 300),
                            ),
                        ) {
                            Text(
                                text = text,
                                maxLines = maxLines,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                        }
                    }
                    else -> {
                        ExpandableText(
                            text = text,
                            maxLines = maxLines,
                        )
                    }
                }

                Text(text = "", modifier = Modifier.padding(bottom = 12.dp))
            }
        }
    }
}
