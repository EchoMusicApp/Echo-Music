// Deterministic, self-contained SVG artwork generator.
// No external URLs, no emoji, no placeholder rectangles.
// Each seed produces a unique, coherent gradient + geometric composition.

function hash(seed: string): number {
  let h = 2166136261
  for (let i = 0; i < seed.length; i++) {
    h ^= seed.charCodeAt(i)
    h = Math.imul(h, 0x01000193)
  }
  return h >>> 0
}

function mulberry32(seed: number): () => number {
  let a = seed >>> 0
  return () => {
    a |= 0
    a = (a + 0x6d2b79f5) | 0
    let t = Math.imul(a ^ (a >>> 15), 1 | a)
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

const PALETTES: Array<[string, string, string]> = [
  ['#7c5cff', '#22d3ee', '#0b0a0f'],
  ['#f43f5e', '#f59e0b', '#1a0b0b'],
  ['#8b5cf6', '#ec4899', '#0f0a1a'],
  ['#10b981', '#0ea5e9', '#041410'],
  ['#f97316', '#ef4444', '#1a0d05'],
  ['#6366f1', '#a855f7', '#0a0b1a'],
  ['#14b8a6', '#f59e0b', '#041613'],
  ['#e11d48', '#7c3aed', '#14050a'],
  ['#0ea5e9', '#3b82f6', '#04101a'],
  ['#84cc16', '#22d3ee', '#0a1405'],
  ['#d946ef', '#f43f5e', '#140512'],
  ['#fbbf24', '#f97316', '#140d05'],
]

function hexToRgb(hex: string): [number, number, number] {
  const m = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex)
  if (!m?.[1] || !m[2] || !m[3]) return [0, 0, 0]
  return [parseInt(m[1], 16), parseInt(m[2], 16), parseInt(m[3], 16)]
}

function mix(a: string, b: string, t: number): string {
  const [r1, g1, b1] = hexToRgb(a)
  const [r2, g2, b2] = hexToRgb(b)
  const r = Math.round(r1 + (r2 - r1) * t)
  const g = Math.round(g1 + (g2 - g1) * t)
  const bl = Math.round(b1 + (b2 - b1) * t)
  return `rgb(${r},${g},${bl})`
}

export function artwork(seed: string, size = 240): string {
  const r = mulberry32(hash(seed))
  const palette = PALETTES[Math.floor(r() * PALETTES.length)] ?? PALETTES[0]!
  const [c1, c2, c3] = palette
  const style = Math.floor(r() * 5)
  const rot = Math.floor(r() * 360)

  let shapes = ''
  if (style === 0) {
    // Concentric arcs
    for (let i = 6; i >= 1; i--) {
      const opacity = (0.12 + i * 0.06).toFixed(2)
      const color = mix(c1, c2, i / 6)
      shapes += `<circle cx="50%" cy="50%" r="${i * 10}%" fill="none" stroke="${color}" stroke-width="6" stroke-opacity="${opacity}"/>`
    }
  } else if (style === 1) {
    // Diagonal beams
    for (let i = 0; i < 6; i++) {
      const color = mix(c1, c3, i / 5)
      const opacity = (0.25 + i * 0.1).toFixed(2)
      shapes += `<rect x="-20%" y="${i * 20}%" width="140%" height="12%" fill="${color}" transform="rotate(25 50% 50%)" opacity="${opacity}"/>`
    }
  } else if (style === 2) {
    // Overlapping circles
    for (let i = 0; i < 4; i++) {
      const color = mix(c1, c2, i / 3)
      const cx = 25 + r() * 50
      const cy = 25 + r() * 50
      const rad = 20 + r() * 25
      shapes += `<circle cx="${cx}" cy="${cy}" r="${rad}" fill="${color}" opacity="0.5"/>`
    }
  } else if (style === 3) {
    // Triangle composition
    for (let i = 0; i < 3; i++) {
      const color = mix(c1, c2, i / 2)
      const s = 90 - i * 22
      shapes += `<polygon points="50,10 ${50 - s / 2},${90 - i * 8} ${50 + s / 2},${90 - i * 8}" fill="${color}" opacity="${(0.3 + i * 0.15).toFixed(2)}" transform="rotate(${rot} 50% 50%)"/>`
    }
  } else {
    // Wave bands
    for (let i = 0; i < 5; i++) {
      const color = mix(c1, c2, i / 4)
      const y = 15 + i * 15
      shapes += `<path d="M0,${y} Q25,${y - 12} 50,${y} T100,${y}" fill="none" stroke="${color}" stroke-width="10" opacity="${(0.3 + i * 0.12).toFixed(2)}"/>`
    }
  }

  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 100 100" role="img">
<defs>
  <linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
    <stop offset="0%" stop-color="${c1}"/>
    <stop offset="55%" stop-color="${c2}"/>
    <stop offset="100%" stop-color="${c3}"/>
  </linearGradient>
  <radialGradient id="v" cx="50%" cy="35%" r="75%">
    <stop offset="0%" stop-color="rgba(255,255,255,0.14)"/>
    <stop offset="100%" stop-color="rgba(0,0,0,0.35)"/>
  </radialGradient>
</defs>
<rect width="100" height="100" rx="8" fill="url(#g)"/>
<rect width="100" height="100" rx="8" fill="url(#v)"/>
<g transform="rotate(${rot} 50 50)">${shapes}</g>
<rect width="100" height="100" rx="8" fill="none" stroke="rgba(0,0,0,0.25)" stroke-width="1"/>
</svg>`
  return `data:image/svg+xml,${encodeURIComponent(svg)}`
}
