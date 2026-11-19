<p align="center">
  <img src="assets/banner.jpg" alt="Pixel Stride — 8-Bit Endless Runner" width="100%" />
</p>

<h1 align="center">PIXEL STRIDE</h1>

<p align="center">
  <strong>An 8-bit retro endless runner with parallax pixel-art biomes, chiptune audio, tight platforming physics, and classic arcade scoring.</strong>
</p>

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-F5C542?style=for-the-badge" alt="MIT License" /></a>
  <img src="https://img.shields.io/badge/React-19-61DAFB?style=for-the-badge&logo=react&logoColor=111324" alt="React 19" />
  <img src="https://img.shields.io/badge/TypeScript-7-3178C6?style=for-the-badge&logo=typescript&logoColor=white" alt="TypeScript" />
  <img src="https://img.shields.io/badge/Vite-8-646CFF?style=for-the-badge&logo=vite&logoColor=white" alt="Vite 8" />
  <img src="https://img.shields.io/badge/Tailwind_CSS-4-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white" alt="Tailwind CSS 4" />
  <img src="https://img.shields.io/badge/Canvas_2D-480×270_Pixel_Art-FF6B35?style=for-the-badge" alt="Canvas 2D Pixel Art" />
</p>

---

## ✨ Features

- **4 Parallax Pixel-Art Biomes** — Seamlessly transition every 400m through **Sunlit Highlands**, **Amber Canyon**, **Starlight Citadel**, and **Dawn Peaks**, each with custom skies, celestial bodies, clouds, mountains, ruins, and terrain palettes.
- **Crisp Platformer Physics** — Native `480×270` 16:9 pixel-perfect Canvas rendering with variable-height jump cut, double jump, coyote time (`110ms`), jump buffering (`120ms`), ground slide/duck, and mid-air fast-fall.
- **4 Hazard Archetypes & Stomp Mechanics** — Dodge **Spikes**, stomp **Hoppers** for a bounce + bonus points, duck under **Swoopers**, and time your leaps past **Flame** flares as speed and difficulty scale dynamically.
- **Pickups & Combo System** — Collect **Coins** to build combo multipliers, plus **Shield** (absorbs a hit), **Heart** (restores HP), and **Magnet** (pulls nearby coins) power-ups.
- **Procedural Chiptune Audio** — Built-in Web Audio API synthesizer (`ChiptuneAudio`) generating retro music loops and sound effects for jumps, stomps, coins, power-ups, and zone transitions — zero external audio assets required.
- **Desktop & Mobile Ready** — Play with keyboard, mouse click/tap, or on-screen touch controls, with automatic local high-score persistence.

---

## 🎮 Controls

| Action | Keyboard | Mouse / Touch |
| :--- | :--- | :--- |
| **Start / Restart** | `Enter` or `Space` (on Title / Game Over), `R` | Tap **Start / Retry** button |
| **Jump / Double Jump** | `Space`, `↑`, or `W` *(hold for higher jump)* | Tap / hold canvas or **JUMP** button |
| **Duck / Slide / Fast-Fall** | `↓` or `S` | Hold **DUCK** button |
| **Pause / Resume** | `P` or `Esc` | Tap **Pause** HUD icon |
| **Mute / Unmute Audio** | `M` | Tap **Sound** HUD icon |

> **Tip:** Append `?autostart=1` to the URL to jump straight into a run without the title overlay.

---

## 🚀 Getting Started

### Prerequisites

- **Node.js** `20+` (LTS recommended)
- **npm** `10+`

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
| `npm run build` | Build production bundle into `dist/` |
| `npm run preview` | Preview the production build locally |
| `npm run lint` | Run TypeScript type-checking (`tsc --noEmit`) |
| `npm test` | Run deterministic game-engine unit tests (`node:test` via `tsx`) |
| `npm run clean` | Remove build artifacts (`dist/` and `server.js`) |

---

## 🗂️ Project Structure

```text
Pixel-Pride-Runner-/
├── assets/
│   └── banner.jpg              # README pixel-art banner
├── src/
│   ├── components/
│   │   ├── GameHud.tsx         # Score, HP hearts, zone banner, touch controls
│   │   ├── PixelIcons.tsx      # Crisp pixel-art SVG UI icons
│   │   ├── StatusModal.tsx     # Pause & Game Over overlays
│   │   └── TitleOverlay.tsx    # Title screen & best score badge
│   ├── game/
│   │   ├── audio.ts            # Procedural Web Audio chiptune soundtrack & SFX
│   │   ├── constants.ts        # Physics tuning, resolution, and 4 biome palettes
│   │   ├── engine.ts           # Fixed-step simulation, collision, spawning, scoring
│   │   ├── engine.test.ts      # Seeded unit tests for physics, combat & difficulty
│   │   ├── renderer.ts         # Layered 480×270 pixel-art Canvas 2D renderer
│   │   ├── sprites.ts          # Procedural pixel-art sprite definitions
│   │   └── types.ts            # Core state, entity, HUD, and audio types
│   ├── App.tsx                 # Main game loop, input hub, and React HUD bridge
│   ├── index.css               # Retro pixel styling & Tailwind CSS v4 setup
│   └── main.tsx                # Application entry point
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
