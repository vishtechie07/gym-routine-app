# Habitual Kinetic — Design system (Stitch)

**Stitch project:** `projects/15758791910411793525` — *Habitual AI - Fitness Tracker*  
**Display name:** Habitual Kinetic  
**Generated:** dashboard screen + design system via Stitch MCP (`generate_screen_from_text`).

## North star

**“The Kinetic Sanctuary”** — Premium dark athletic UI: data feels like a luxury surface, not a spreadsheet. Depth via layered **surface** tones, not heavy borders.

## Color roles

| Token | Hex | Use |
|--------|-----|-----|
| background / surface | `#0a0f14` | Page canvas |
| surface_container / high | `#141a20`–`#1a2027` | Cards, sections |
| primary | `#3adffa` | Metrics, primary CTAs |
| primary_container | `#00cbe6` | Gradient end-stops |
| secondary | `#ac8aff` | AI-only accents |
| on_surface | `#e7ebf3` | Primary text |
| on_surface_variant | `#a7abb2` | Secondary text |
| outline_variant | `#43484e` | Ghost borders (low opacity only) |

## Rules

1. **No-line rule:** Avoid 1px borders between sections; use **tonal shifts** and vertical spacing.
2. **Glass:** Sticky header — ~70% opacity, `backdrop-filter: blur(12px)`.
3. **Glow:** Primary buttons — `box-shadow: 0 10px 30px rgba(34, 211, 238, 0.15)` (cyan tint, not black).
4. **Typography:** **Space Grotesk** for displays/headlines (tight tracking); **Inter** for body and numbers.

## Layout

- Max width **1280px**, centered.
- **Roundness:** `rounded-3xl` (xl / full) on cards and primary actions.

## Motion Lab (tutorials)

- **Purpose:** Motion Lab ingests tutorial videos from configured YouTube channels. `POST /api/lab/sync` upserts videos; when OpenAI is configured, missing embeddings are generated and stored for semantic search. `GET /api/lab/search` ranks by keywords and (when embeddings exist) blends in semantic similarity (response includes `smart`). `static/data/motion-lab-catalog.json` (105 items) backs the optional AI id-ranking endpoint `/api/ai/motion-lab-suggest`.
- **Stitch alignment:** Same Neo-Grid / Kinetic Sanctuary language — magenta `#ff41af` accents on media badges, cyan `#00f0ff` labels, no flat borders on hero (chromatic-panel + soft blurs).

## Implementation

This repo maps these tokens to Tailwind arbitrary classes and a small CSS block in `fitness-tracker.html`. Iterate in Stitch for new screens; keep this file in sync when tokens change.
