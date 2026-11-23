# Design: Echo Web

The shared app system. Keep routes, interactions, and local catalog behavior intact.

## System

- Genre: playful listening instrument
- Shape of first page: app screen
- Theme: matte charcoal surfaces, silver text, violet signal accent
- Axes: dark/ui-sans/violet, posture: three-column app shell
- Dial: ENERGY 2 / RHYTHM 3 / MOTION 1
- Signature: the selected track, artwork, and direct playback action
- Surface language: machined edges, compact readouts, and clear separation without decorative glow

## Tokens

`src/tokens.css` is the type, spacing, and motion scale. `src/index.css` defines the Echo palette and theme preferences.

```css
:root {
  --color-paper: var(--echo-bg);
  --color-paper-2: var(--echo-surface);
  --color-paper-3: var(--echo-surface-2);
  --color-ink: var(--echo-text);
  --color-ink-2: var(--echo-text-muted);
  --color-muted: var(--echo-text-muted);
  --color-rule: var(--echo-border);
  --color-accent: var(--echo-accent);
  --color-accent-ink: var(--echo-accent-ink);
  --color-focus: var(--echo-accent);
  --font-display: var(--sans);
  --font-body: var(--sans);
  --radius-card: 4px;
  --radius-pill: 999px;
  --shadow-card: none;
}
```

## Controls

- Primary: violet fill with near-black ink, compact height and a 44px minimum hit area where needed.
- Secondary: quiet surface or outline, same control shape and readable text.

## Motion

- Character: brisk state feedback only. No decorative reveals or loops; reduced motion is respected.

## Exports

`src/tokens.css` and `src/index.css` are the source of truth for this app.
