<p align="center">
  <img src="assets/banner.jpg" alt="Pixel Stride — 8-Bit Endless Runner" width="100%" />
</p>

<h1 align="center">PIXEL STRIDE</h1>

<p align="center">
  <strong>An 8-bit retro endless runner with playable characters, parallax pixel-art biomes, chiptune audio, tight platforming physics, and Android 17 support.</strong>
</p>

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-F5C542?style=for-the-badge" alt="MIT License" /></a>
  <img src="https://img.shields.io/badge/React-19-61DAFB?style=for-the-badge&logo=react&logoColor=111324" alt="React 19" />
  <img src="https://img.shields.io/badge/TypeScript-7-3178C6?style=for-the-badge&logo=typescript&logoColor=white" alt="TypeScript" />
  <img src="https://img.shields.io/badge/Vite-8-646CFF?style=for-the-badge&logo=vite&logoColor=white" alt="Vite 8" />
  <img src="https://img.shields.io/badge/Tailwind_CSS-4-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white" alt="Tailwind CSS 4" />
  <img src="https://img.shields.io/badge/Android_17-API_37_%7C_Capacitor_9-3DDC84?style=for-the-badge&logo=android&logoColor=111324" alt="Android 17 API 37" />
</p>

---

## ✨ Features

- **Playable Characters & 8-Bit Hero Creator** — Choose between **Stride** (classic scarfed retro adventurer), **Lily** (8-bit lily-of-the-valley runner with blossom petals & passive coin aura), **Pixel Knight** (4 HP armored hero), or design your own **Custom Hero** (Male/Female, presets, and mission-unlockable cosmetics) directly on the title screen.
- **6 Parallax Pixel-Art Biomes** — Seamlessly transition every 400m through **Sunlit Highlands**, **Amber Canyon**, **Starlight Citadel**, **Dawn Peaks**, **Frost Tundra**, and **Volcanic Forge**, each with custom skies, weather effects, mountains, ruins, terrain palettes, and chiptune themes.
- **Crisp Platformer Physics** — Native `480×270` 16:9 pixel-perfect Canvas rendering with variable-height jump cut, double jump, coyote time (`110ms`), jump buffering (`120ms`), ground slide/duck, and mid-air fast-fall.
- **4 Hazard Archetypes & Stomp Mechanics** — Dodge **Spikes**, stomp **Hoppers** for a bounce + bonus points, duck under **Swoopers**, and time your leaps past **Flame** flares as speed and difficulty scale dynamically.
- **Pickups, Power-Ups & Pixel Shop** — Collect **Coins** to build combo multipliers and spend them in the **Pixel Shop** on permanent upgrades, cosmetic trails, and **Pixel Knight**, plus **Shield**, **Heart**, **Magnet**, **2X Score**, and **Slow-Mo** power-ups.
- **Procedural Chiptune Audio** — Built-in Web Audio API synthesizer (`ChiptuneAudio`) generating retro music loops and sound effects with cached noise buffers, automatic node cleanup, and persisted mute state.
- **Daily Missions & Login Streak Bonuses** — 3 fresh daily quests every calendar day plus a 7-day play streak unlocking starting score boosts, a free **Star Shield** (Day 3+), and a **Coin Magnet** (Day 5+) at the start of every run.
- **Dedicated Portrait & Landscape Modes** — Automatically switches between true vertical Portrait (`300×520` full-screen with underground touch control deck) and wide Landscape (`480×270` with bottom-right thumb controls away from the runner).
- **Android 17 (API 37) Ready** — Packaged with **Capacitor 9** targeting Android 17 (`compileSdkVersion = 37`, `targetSdkVersion = 37`), custom pixel-art Adaptive Icon with **Material You Themed Icon (`<monochrome>`)** support, edge-to-edge safe-area insets (`viewport-fit=cover` + `env(safe-area-inset-*)`), adaptive tablet/foldable touch controls (`pointer: coarse`), and automatic background audio pause.

---

## 📦 Downloads & Releases

Pre-built **Web** and **Android 17** packages are published on the [GitHub Releases](https://github.com/gabigtr123/Pixel-Pride-Runner-/releases) page:

| Platform | Latest Release | Artifact | How to Run / Install |
| :--- | :--- | :--- | :--- |
| **Android / Web PWA (Live)** | [Play / Install PWA](https://gabigtr123.github.io/Pixel-Pride-Runner-/) | GitHub Pages | Open in Chrome on Android and tap **Install app** — full-screen offline play, automatic updates, **0 Play Protect warnings**. |
| **Android 17 Signed APK** | [`v2.0.0-beta.4`](https://github.com/gabigtr123/Pixel-Pride-Runner-/releases/tag/v2.0.0-beta.4) | `pixel-stride-v2.0.0-beta.4-android.apk` | Signed Release APK (API 37 / Capacitor 9) with Material You themed icon & persistent keystore. |
| **Web (Static Bundle)** | [`v2.0.0-beta.4`](https://github.com/gabigtr123/Pixel-Pride-Runner-/releases/tag/v2.0.0-beta.4) | `pixel-stride-v2.0.0-beta.4-web.zip` | Unzip and serve with any static host (`npx serve .`, Netlify, Vercel, or GitHub Pages). |

---

## 🎮 Controls

| Action | Keyboard | Mouse / Touch |
| :--- | :--- | :--- |
| **Choose Character** | — | Tap **STRIDE**, **LILY**, **KNIGHT**, or **CUSTOM** on Title Screen |
| **Daily Quests & Bonus** | — | Tap **DAY X** button on Title Screen or Game Over |
| **Start / Restart** | `Enter` or `Space` (on Title / Game Over), `R` | Tap **START RUN / TRY AGAIN** |
| **Jump / Double Jump** | `Space`, `↑`, or `W` *(hold for higher jump)* | Tap / hold canvas or **JUMP** button |
| **Duck / Slide / Fast-Fall** | `↓` or `S` | Hold **DUCK** button |
| **Pause / Resume** | `P` or `Esc` (`Space` / `Enter` also resumes) | Tap **Pause** HUD icon |
| **Mute / Unmute Audio** | `M` | Tap **Sound** HUD icon |

> **Tip:** Append `?autostart=1` to the URL to jump straight into a run without the title overlay.

---

## 🚀 Getting Started

### Prerequisites

- **Node.js** `24+` (LTS, required by Capacitor 9 CLI)
- **npm** `10+`
- *(Optional, for local Android builds)* **JDK 21** & **Android 17 SDK (API 37)**

### Installation & Local Development

```bash
# 1. Clone the repository
git clone https://github.com/gabigtr123/Pixel-Pride-Runner-.git
cd Pixel-Pride-Runner-

# 2. Install dependencies
npm install

# 3. Start the dev server (http://localhost:3000)
npm run dev
```

### Available Scripts

| Command | Description |
| :--- | :--- |
| `npm run dev` | Start Vite development server on port `3000` |
| `npm run build` | Build production Web & PWA bundle into `dist/` |
| `npm run preview` | Preview the production build locally |
| `npm run lint` | Run TypeScript type-checking (`tsc --noEmit`) |
| `npm test` | Run deterministic game-engine unit tests (`node:test` via `tsx`) |
| `npm run android:sync` | Build Web bundle and sync Capacitor 9 Android assets |
| `npm run clean` | Remove build output (`dist/`) |

### Automated Releases (GitHub Actions)

- Push a single version tag `v*` (e.g., `v2.0.0`) to automatically run lint/tests, build both the **Signed Android 17 APK** and the **Web/PWA `.zip`**, and publish one unified GitHub Release via `.github/workflows/release.yml`.
- Every push to `main` automatically deploys the live installable PWA to GitHub Pages via `.github/workflows/deploy-pages.yml`.

---

## 🗂️ Project Structure

```text
Pixel-Pride-Runner-/
├── .github/
│   ├── ISSUE_TEMPLATE/         # Bug report & feature request forms
│   ├── workflows/
│   │   ├── deploy-pages.yml    # Auto-deploy live PWA to GitHub Pages on main
│   │   └── release.yml         # Unified Android 17 Signed APK + Web zip release
│   └── PULL_REQUEST_TEMPLATE.md
├── assets/
│   └── banner.jpg              # README pixel-art banner
├── public/
│   ├── icon-192.png            # PWA 192×192 pixel-art icon
│   ├── icon-512.png            # PWA 512×512 pixel-art icon
│   ├── icon.svg                # Crisp SVG pixel-art favicon
│   ├── manifest.webmanifest    # Installable PWA manifest
│   └── sw.js                   # Offline-capable Service Worker
├── src/
│   ├── components/
│   │   ├── CreatorModal.tsx    # 8-bit Hero Creator (Male/Female, presets & mission unlocks)
│   │   ├── DailyModal.tsx      # Daily Quests & 7-day streak bonus modal
│   │   ├── GameHud.tsx         # Score, HP hearts, zone banner, portrait/landscape touch controls
│   │   ├── PixelIcons.tsx      # Crisp pixel-art SVG UI icons & character previews
│   │   ├── ShopModal.tsx       # Pixel Shop (upgrades, trails, Knight, Top-5 leaderboard)
│   │   ├── StatusModal.tsx     # Pause & Game Over overlays with mission completion
│   │   └── TitleOverlay.tsx    # Title screen, Daily/Shop/Creator buttons & character selector
│   ├── game/
│   │   ├── audio.ts            # Procedural Web Audio chiptune soundtrack & SFX
│   │   ├── constants.ts        # Physics tuning, storage keys & 6 biome palettes
│   │   ├── creator.ts          # Custom hero sprite composer, presets & career mission unlocks
│   │   ├── daily.ts            # Deterministic Daily Missions & login streak engine
│   │   ├── engine.ts           # Fixed-step simulation, collision, spawning, scoring
│   │   ├── engine.test.ts      # 16 seeded unit tests (physics, combat, shop, creator, daily)
│   │   ├── haptics.ts          # Tactile vibration helper for Android & mobile browsers
│   │   ├── renderer.ts         # Dual Portrait (300×520) & Landscape (480×270) Canvas renderer
│   │   ├── shop.ts             # Coin bank, upgrades, trails & Top-5 leaderboard state
│   │   ├── sprites.ts          # 8-bit sprite matrices (Stride, Lily, Pixel Knight, enemies)
│   │   └── types.ts            # Core state, character, viewport, HUD & audio types
│   ├── App.tsx                 # Main game loop, orientation hub & React HUD bridge
│   ├── index.css               # Retro pixel styling, safe-area & Tailwind CSS v4
│   └── main.tsx                # Entry point & PWA Service Worker registration
├── capacitor.config.json       # Capacitor 9 Android configuration
├── index.html
├── package.json
├── tsconfig.json
└── vite.config.ts
```

---

## 🤝 Contributing

Contributions, bug reports, and feature ideas are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a pull request.

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.
