# Expandable Text

[![Maven Central](https://img.shields.io/maven-central/v/app.whisperian/expandable-text-compose.svg)](https://central.sonatype.com/namespace/app.whisperian)

An unopinionated wrapper over the Jetpack Compose Text composable that provides animated text expansion and collapse when `maxLines` changes.

> [!WARNING]
> This package was developed mostly with AI, but has been tested across a lot of edge cases.

## Why this library exists

We needed a way to smoothly animate expanding/collapsing text. The only working library we found used M2 and was too opinionated. It hard-coded Material 2 `Text` and appended a "read more" string inside the text. So we built this minimal, unopinionated alternative.

## Features

- 🎬 **Smooth animations** - Height transitions are animated using spring physics
- 📝 **Ellipsis support** - Truncates at word boundaries when collapsed
- 🔤 **RTL support**
- 🎨 **Full Text API** - Supports all standard Text parameters (fontSize, fontWeight, etc.)
- 🎛️ **Flexible control** - Customize animation via `animationSpec`
- 📦 **Lightweight** - Just Compose + Material3

## Installation

Add the dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("app.whisperian:expandable-text-compose:0.1.0")
}
```

## Usage

If you already use Compose `Text`, `ExpandableText` is designed to be a drop-in replacement.

The only differences from `Text`:
- The `inlineContent` parameter is not currently supported.
- The `overflow` parameter only supports `Clip` and `Ellipsis` (default is `Ellipsis`).
- The `trailingContent` parameter is optional and lets you append a small composable at the end of the visible text (for example, a "Read more" button). The content is constrained to the text’s line height and will be clipped if it exceeds that height.
- The `animationSpec` parameter is optional and allows you to customize the animation used for height changes.

### Example

```kotlin
var expanded by remember { mutableStateOf(false) }

ExpandableText(
    text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
    maxLines = if (expanded) Int.MAX_VALUE else 3,
    trailingContent = if (expanded) null else {
        { Text(" more", modifier = Modifier.clickable { expanded = true }, color = MaterialTheme.colorScheme.primary) }
    },
    // Optional override
    animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
)
```

## Requirements
- Minimum SDK: 21
