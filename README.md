<p align="center">
  <img src="assets/banner.jpg" alt="Pixel Stride — 8-Bit Endless Runner" width="100%" />
</p>

<h1 align="center">PIXEL STRIDE</h1>

<p align="center">
  <strong>An 8-bit retro endless runner with playable heroes, a custom Hero Creator, Pixel Shop, 6 parallax biomes with dynamic weather, procedural chiptune audio, and Android 17 support.</strong>
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

### 🏃 Playable Roster & 8-Bit Hero Creator
| Character | Role | Unlock | Signature Perk |
| :--- | :--- | :--- | :--- |
| **STRIDE** | Male Runner | Default | `+25%` Stomp Bonus |
| **LILY** | Female Runner | Default | Passive Coin Aura + Blossom Petals |
| **PIXEL KNIGHT** | Armored Hero | 100 Coins in Pixel Shop | Starts every run with **4 Hearts** |
| **CUSTOM HERO** | Hero Creator (`Male` / `Female`) | Default | `+15%` Coin & Stomp Bonus + **6 mission-unlockable cosmetics** |

- **8-Bit Hero Creator** — Toggle between **Male** & **Female** styles, pick a custom name or English preset (`ALEX`, `JACK`, `MAX`, `FINN`, `COLE`, `LEO` / `EMMA`, `RUBY`, `CHLOE`, `LUNA`, `ZOE`, `NOVA`), customize skin tone, hair/cap, scarf/ribbon, and outfit colors, and unlock 6 exclusive cosmetics (`CROWN GOLD`, `CYBER NEON`, `SOLAR GOLD`, `FROST SILK`, `SHADOW`, `STARLIGHT`) through career milestones.

### 🌍 Gameplay, Biomes & Progression
- **6 Parallax Pixel-Art Biomes** — Seamlessly transition every 400m through **Sunlit Highlands**, **Amber Canyon**, **Starlight Citadel**, **Dawn Peaks**, **Frost Tundra** *(falling snow)*, and **Volcanic Forge** *(rising embers)*, each with custom skies, parallax layers, and its own chiptune theme.
- **Pixel Shop & Top-5 Leaderboard** — Bank collected coins across runs to unlock **Pixel Knight**, permanent upgrades (**Magnet Time**, **Gold Value**, **Lucky Drops**), and cosmetic runner trails (**Classic**, **Rainbow**, **Blossom**, **Ember**), plus track your local **Top 5** high scores.
- **5 Power-Ups & Combo System** — Chain **Coins** for combo multipliers and grab **Star Shield**, **Heart**, **Coin Magnet**, **2X Score**, and **Slow-Mo** pickups.
- **Crisp Platformer Physics & 4 Hazards** — Variable-height jump cut, double jump, coyote time (`110ms`), jump buffering (`120ms`), ground slide/duck, mid-air fast-fall, and dynamic encounters with **Spikes**, stompable **Hoppers**, **Swoopers**, and **Flames**.
- **Daily Missions & 7-Day Login Streak** — 3 fresh daily quests every calendar day plus a 7-day play streak unlocking starting score boosts, a free **Star Shield** (Day 3+), and a **Coin Magnet** (Day 5+).
- **Procedural Chiptune Audio & Haptics** — Built-in Web Audio API synthesizer (`ChiptuneAudio`) with per-biome 16-step loops, retro SFX, and tactile mobile vibration feedback on stomps, power-ups, hits, and mission unlocks.
- **Dedicated Portrait & Landscape Modes** — Automatically adapts between true vertical Portrait (`300×520` full-screen with underground touch control deck) and wide Landscape (`480×270` with bottom-right thumb controls).
- **Android 17 (API 37) Ready** — Packaged with **Capacitor 9** (`compileSdkVersion = 37`, `targetSdkVersion = 37`), custom pixel-art Adaptive Icon with **Material You Themed Icon (`<monochrome>`)** support, edge-to-edge safe-area insets (`viewport-fit=cover` + `env(safe-area-inset-*)`), and automatic background audio pause.

---

## 📦 Downloads & Releases

Pre-built **Web** and **Android 17** packages are published on the [GitHub Releases](https://github.com/gabigtr123/Pixel-Pride-Runner-/releases) page:

| Platform | Latest Release | Artifact | How to Run / Install |
| :--- | :--- | :--- | :--- |
| **Android / Web PWA (Live)** | [Play / Install PWA](https://gabigtr123.github.io/Pixel-Pride-Runner-/) | GitHub Pages | Open in Chrome on Android and tap **Install app** — full-screen offline play, automatic updates, **0 Play Protect warnings**. |
| **Android 17 Signed APK** | [`v2.0.1`](https://github.com/gabigtr123/Pixel-Pride-Runner-/releases/tag/v2.0.1) | `pixel-stride-v2.0.1-android.apk` | Signed Release APK (API 37 / Capacitor 9) with Material You themed icon & persistent keystore. |
| **Web (Static Bundle)** | [`v2.0.1`](https://github.com/gabigtr123/Pixel-Pride-Runner-/releases/tag/v2.0.1) | `pixel-stride-v2.0.1-web.zip` | Unzip and serve with any static host (`npx serve .`, Netlify, Vercel, or GitHub Pages). |

---

## 🎮 Controls

| Action | Keyboard | Mouse / Touch |
| :--- | :--- | :--- |
| **Choose Hero** | — | Tap **STRIDE**, **LILY**, **KNIGHT**, or **CUSTOM** on Title Screen |
| **Hero Creator** | `Esc` closes modal | Tap **EDIT HERO** on Title Screen |
| **Pixel Shop / Top 5** | `Esc` closes modal | Tap **Coins / SHOP** button on Title Screen |
| **Daily Quests & Streak** | `Esc` closes modal | Tap **DAY X** button on Title Screen or Game Over |
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
| `npm run android:sync` | Build Web bundle, sync Capacitor 9 Android assets, and apply Material You icons |
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
├── scripts/
│   └── apply-android-icons.mjs # Generates Android Adaptive & Material You monochrome icons
├── src/
│   ├── components/
│   │   ├── CreatorModal.tsx    # 8-bit Hero Creator (Male/Female, presets & mission unlocks)
│   │   ├── DailyModal.tsx      # Daily Quests & 7-day streak bonus modal
│   │   ├── GameHud.tsx         # Score, HP hearts, power-up timers, portrait/landscape controls
│   │   ├── PixelIcons.tsx      # Crisp pixel-art SVG UI icons & character previews
│   │   ├── ShopModal.tsx       # Pixel Shop (Knight, upgrades, trails, Top-5 leaderboard)
│   │   ├── StatusModal.tsx     # Pause & Game Over overlays with mission/cosmetic unlocks
│   │   └── TitleOverlay.tsx    # Title screen, Daily/Shop/Creator buttons & hero selector
│   ├── game/
│   │   ├── audio.ts            # Procedural Web Audio chiptune soundtrack (6 biomes) & SFX
│   │   ├── constants.ts        # Physics tuning, storage keys & 6 biome palettes
│   │   ├── creator.ts          # Custom hero sprite composer, English presets & unlocks
│   │   ├── daily.ts            # Deterministic Daily Missions & 7-day streak engine
│   │   ├── engine.ts           # Fixed-step simulation, collision, power-ups, spawning
│   │   ├── engine.test.ts      # 16 seeded unit tests (physics, combat, shop, creator, daily)
│   │   ├── haptics.ts          # Tactile vibration helper for Android & mobile browsers
│   │   ├── renderer.ts         # Dual Portrait (300×520) & Landscape (480×270) Canvas renderer
│   │   ├── shop.ts             # Coin bank, upgrades, trails & Top-5 leaderboard state
│   │   ├── sprites.ts          # 8-bit sprite matrices (Stride, Lily, Pixel Knight, enemies)
│   │   └── types.ts            # Core state, character, viewport, HUD & audio types
│   ├── App.tsx                 # Main game loop, modal/input hub & React HUD bridge
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
