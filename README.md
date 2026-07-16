# Collage Widget

An Android app that turns your photos into hand-torn paper cutouts and arranges
them as a collage on your home screen.

Pick photos → the subject is cut out on-device → each cutout gets an irregular,
hand-torn paper edge → arrange them on a board → add it to your home screen as a
widget.

**Everything happens on the device.** Photos are never uploaded, and the app
requests no internet permission.

## Features

- On-device background removal (ML Kit Subject Segmentation)
- Procedural torn-paper edges — seeded per piece, so an edge never shimmers
  between renders
- Collage editor: drag, pinch, rotate, resize handles, z-order, duplicate
- Board backgrounds: transparent, a photo from your gallery, 16 gradient presets
  (including procedural mesh gradients), and generated paper/cork/wood/linen
  textures
- A live clock overlay — the system ticks it natively, so it costs no battery
- Prebuilt templates
- Home-screen widgets, resizable, one collage per widget

## Build

Requires JDK 17 and the Android SDK (`android-36`).

```bash
./gradlew assembleDebug
```

Put your SDK location in `local.properties`:

```properties
sdk.dir=/path/to/Android/sdk
```

### Release builds

Copy `keystore.properties.example` to `keystore.properties` and fill it in, or
set the `PAPERCUT_*` environment variables. Without either, release builds are
unsigned.

```bash
./gradlew bundleRelease   # AAB, for Play
./gradlew assembleRelease # APK, for testing the R8 build
```

## Architecture

| Layer | What's there |
|---|---|
| `model/` | Domain types. **Enums here are persisted by name — see `proguard-rules.pro` before renaming any constant.** |
| `segmentation/` | ML Kit wrapper. Model-agnostic: bitmap → alpha matte. |
| `render/` | The drawing. `PaperEdgeProcessor` (torn edges), `BoardBackgroundRenderer`, `CollageRenderer`, `ClockRenderer`. |
| `data/` | Room + codecs that flatten sealed types into text columns. |
| `ui/` | Compose screens. |
| `widget/` | `AppWidgetProvider`, config activity, RemoteViews. |

Two things worth knowing before changing the rendering:

**The editor and the widget share one renderer.** `BoardBackgroundRenderer` and
`CollageRenderer` draw into a plain `android.graphics.Canvas`, called from both
Compose previews and the widget's bitmap export. That's deliberate — two
implementations would drift, and the widget must match what the user arranged.

**The widget is a bitmap.** RemoteViews can't do custom drawing, so the collage
is rendered to a transparent PNG sized to the widget and shown in one
`ImageView`. The only live element is the clock, which is a real `TextClock`
layered on top — one of the few views the system updates by itself.

## Tech

Kotlin · Jetpack Compose · Material 3 · Room · ML Kit Subject Segmentation ·
Coil · minSdk 26 / targetSdk 36
