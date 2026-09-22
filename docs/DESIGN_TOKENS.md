# DESIGN_TOKENS.md — Single source of visual truth

Direction (TASK-009 refresh): Dribbble 25812917 hierarchy (dark sidebar + light canvas + summary cards + central table + status rail), inspo ochre/charcoal consensus, ui-ux-pro dashboard checklist (stat cards, micro-hover, footer). Tokens only — no page-specific hex.

## Colors (light-first; dark = sidebar + status surfaces only)
- `--bg-canvas: #F6F5F1` (warm light gray, content canvas)
- `--bg-surface: #FFFFFF` (cards, tables)
- `--bg-sidebar: #16161A` (dark nav; text on it must pass contrast) + `--bg-sidebar-hover: rgba(255,255,255,.08)`
- `--text-primary: #1A1A1E` `--text-secondary: #5F6570` `--text-on-dark: #F5F4EF` `--text-on-dark-dim: #B9B4A8`
- `--accent: #9A6206` (deepened amber/gold, 4.5:1 on white; validated in TASK-009 refresh against inspo ochre consensus) `--accent-strong: #7A4C05` `--accent-soft: #F5E7C6`
- `--success: #1E7E34` `--warning: #9A6700` `--danger: #B3261E` `--info: #1D4ED8` (each with `-soft` bg)
- Status chips pair bg+text (never color alone): queued(info-soft), in-service(warning-soft), completed(success-soft), cancelled/danger-soft + icon+label.

## Typography
- Family: Inter (grotesk sans fallback: system-ui). Display/heading 600–700, body 400–500, mono (`--font-mono`) for OTP boxes, queue numbers/times (tabular-nums).
- Scale: display 32/40, h1 24/32, h2 20/28, body 16/24, small 14/20, caption 12/16.

## Spacing / radius / shadow / breakpoints
- Spacing 4-based: 4 8 12 16 24 32 48 64. Section rhythm `clamp(72px,10vw,140px)`; card padding 20–24.
- Radius: cards/tables 16, inputs/buttons 10, chips 999, OTP boxes 10.
- Shadows: card `0 1px 2px rgba(16,16,26,.06), 0 8px 24px -12px rgba(16,16,26,.18)`; lift `...25` for button hover. No glassmorphism.
- Breakpoints: 390 / 768 / 1024 / 1440. Container max 1280, padding-inline ≥24 mobile.

## Components (behavior, not full CSS — implemented in tokens.css)
Button (primary dark, secondary outline, danger-ghost; hover lift 150ms, active settle; disabled state; ≥44px mobile), Input/Select (label, hint, error slot, focus ring), OTP boxes (48×56, mono 24px, focus ring, group label), Card, Stat card (icon dot + label + value + link), Table (sticky header, row hover, collapses to cards ≤768), StatusBadge (pill + icon + text), Modal (focus trap, Esc, overlay click), Loading (skeleton rows), EmptyState (title+body+CTA), ErrorMessage (+Retry button), QueueStatus, Sidebar (brand mark, nav label, active amber rail, session footer), Footer (dark, brand + tagline + note).

## Rules
- No gradients, no decorative animation (micro-hover only, motion-safe), no fake stats/charts — every number comes from an API.
- Status = text + icon + color. Contrast checked before ship.
