# Echo Music Design Guidelines

Echo Music follows a **custom, modern aesthetic** that blends some Material Design principles with unique, iOS-inspired patterns. 

This document is the definitive guide for designing and implementing UI in the Echo Music codebase. All new UI work and refactors must follow these custom principles rather than strictly adhering to Google's Material Design 3 spec.

---

## 1. Color System & Theming

We use a dynamic color system, but apply it in a custom way to achieve a unique look.

### Dynamic Color & Seed
*   **Dynamic First:** Colors must come from `MaterialTheme.colorScheme`. For devices running Android 12+, dynamic system colors are extracted. On older devices, the default theme seed `0xFFED5564` is used to generate the palette using `materialKolor`.
*   **Translucency:** A core part of the Echo Music look is translucent surfaces. For example, cards often use `surfaceVariant.copy(alpha = 0.3f)` rather than solid M3 container colors.

### Semantic Color Roles
Use the correct semantic color roles as defined by our theme:
*   **Primary (`primary` / `onPrimary`):** Used for the most prominent components across the app, active states, and filled buttons.
*   **Surface (`surface` / `onSurface`):** Backgrounds for the app and solid menus.
*   **Translucent Surfaces:** Custom translucent backgrounds (like `surfaceVariant.copy(alpha = 0.3f)`) are heavily used for cards, segmented buttons, and grouped lists to create a softer, layered aesthetic.

---

## 2. Liquid Glass System (Glassmorphism)

A signature part of Echo Music's design is the **Liquid Glass** effect, which provides high-quality blur, refraction, and translucency to navigation bars, headers, and media players.

### Core Implementation
The Liquid Glass effect is driven by a custom `Modifier.liquidGlass()` extension found in `GlassEffect.kt`. It utilizes a heavily customized RenderEffect pipeline (available on Android 12 / API 31+) over a recorded `Backdrop`.

```kotlin
// Basic Usage
Modifier.liquidGlass(
    config = LocalGlassEffectConfig.current,
    shape = RoundedCornerShape(24.dp),
    applyEdgeEffects = true // Set to false for full-screen surfaces
)
```

### Technical Details & Parameters

1. **Backdrop Resolution Scaling:** To maintain high performance, the glass surface records and processes its backdrop at a lower resolution (down to `33%`), relying on the blur to hide the upscaling. The `glassResolutionScale` function dynamically adjusts this based on the requested blur radius.
2. **API Requirements:** Glass requires Android 12 (`Build.VERSION_CODES.S`). Devices on older versions gracefully fall back to solid/standard transparency by checking `isGlassSupported()`.
3. **Lens Refraction (Edge Effects):** Small pills (like floating bottom bars) read as physical glass by applying edge effects. These include lens refraction (`lensHeight`, `lensAmount`), a specular highlight rim (`Highlight.Default`), and a drop shadow (`Shadow.Default`). **Note:** These are enabled via `applyEdgeEffects = true` and rely on `CornerBasedShape`s. (Using a non-corner-based shape throws `UnsupportedOperationException`).
4. **Full-Screen Blur Tuning:** For large surfaces like the full-screen player background, edge effects should be disabled (`applyEdgeEffects = false`) to avoid a stray band of light. Instead, they use a `PLAYER_BLUR_MULTIPLIER` (4x the standard pill blur) to match the deep-blurred material seen in Apple Music.

### Color and Vibrancy
*   **Vibrancy:** Configurable saturation multiplier (default `1.5x` saturation) applies directly via `colorControls` RenderEffect.
*   **Surface Tint:** Apple's glass is typically a light material on light content and dark on dark. If `surfaceTintColor` is unspecified, it defaults to `0xFFFAFAFA` for light mode and `0xFF121212` for dark mode, layered with a typical `surfaceOpacity` of `0.4f`.
*   **Flat Integration:** Action buttons (like FABs or overflow menus) placed on top of Liquid Glass surfaces must use flat elevations (`elevation = 0.dp`). M3 drop shadows interact poorly with translucent layers and will render as ugly dark blobs beneath the component.

---

## 3. Components in Detail

Do NOT strictly force Material 3 components if they break the app's custom aesthetic. Match the existing components found in the app.

### Buttons & Controls
*   **Segmented Controls:** We frequently use custom segmented buttons (e.g., Row with rounded buttons) rather than M3 standard tabs or segmented buttons.
*   **Rounded Shapes:** Elements heavily lean towards large corner radii (`RoundedCornerShape(24.dp)` or `CircleShape`), providing a friendlier, softer UI.
*   **Settings Groups:** We use a grouped-list style (similar to iOS Settings) by encapsulating lists within a `surfaceContainerHighest` card with `24.dp` corners, rather than separate pill cards.

### Cards & Surfaces
*   **Custom Cards:** Unlike standard M3 cards (which use solid `surfaceContainer` colors), Echo Music cards typically use:
    *   *Container:* `surfaceVariant.copy(alpha = 0.3f)`
    *   *Shape:* `RoundedCornerShape(24.dp)` or `28.dp`
    *   *Elevation:* 0.dp (flat, translucent look).
*   Grouped items within cards are a common pattern.

### Navigation & Headers
*   **Top App Bars:** We often use custom implementations or standard `TopAppBar` rather than `LargeTopAppBar`. Headers are sometimes manually placed over scrolling content with custom fade-in animations rather than using standard M3 `Scaffold` scroll behaviors.
*   **Bottom Navigation Bar:** Custom floating tab bars (`ui/component/floatingtabbar/`) are preferred over the standard M3 `NavigationBar`.

---

## 4. Typography & Iconography

*   **Typography:** Always use `MaterialTheme.typography` but respect the app's established font weights and sizes. Echo Music leans towards bold, expressive headers and softer, highly legible body text.
*   **Iconography:** We use a mix of Material Symbols/Icons Extended (`androidx.compose.material.material-icons-extended`) and custom SVG drawables. Check `scripts/compose_svg_drawable.py` and existing drawables before importing new vector assets to avoid duplication.

---

## 5. Extending the Design System

Before adding a brand new UI component, always check `ui/component/` to see if an existing one already implements our conventions.

**Key Rule:** When working on UI, **look at the existing screens** (like the original Listen Together or Settings screens) and copy their specific visual style, spacing, and modifier chains. Do NOT refactor existing screens to match standard Material 3 unless explicitly requested. Our custom aesthetic takes precedence over M3 guidelines.


## Ambient Mode Canvas

Ambient Mode may layer muted Canvas video artwork inside the existing album-art square. Keep the original square size, rounded clipping, and interaction surface unchanged; Canvas is a non-interactive visual layer above the normal album art and follows playback state. The existing glow background remains separate underneath the screen.
