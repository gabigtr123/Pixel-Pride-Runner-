# Contributing to Pixel Stride

Thank you for your interest in contributing to **Pixel Stride**! Whether you're fixing a bug, tuning platformer physics, designing a new biome, or improving documentation, your help is appreciated.

## Getting Started

1. **Fork** the repository on GitHub.
2. **Clone** your fork locally:
   ```bash
   git clone https://github.com/<your-username>/Pixel-Pride-Runner-.git
   cd Pixel-Pride-Runner-
   ```
3. **Install dependencies**:
   ```bash
   npm install
   ```
4. **Create a feature or fix branch** from `main`:
   ```bash
   git checkout -b feat/your-feature-name
   ```
5. **Run the dev server**:
   ```bash
   npm run dev
   ```

## Development Guidelines

- **Keep simulation deterministic**: Core game logic lives in `src/game/engine.ts` and accepts an injectable RNG (`rng?: () => number`) so mechanics remain reproducible and unit-testable.
- **Maintain pixel-art fidelity**: Rendering targets a native `480×270` virtual canvas (`src/game/renderer.ts` & `src/game/sprites.ts`). Avoid blurry fractional coordinates or non-pixel assets.
- **TypeScript strictness**: Keep all code strictly typed with no `tsc` errors.
- **Write or update tests**: Any changes to physics, collision, difficulty scaling, or damage rules should include tests in `src/game/engine.test.ts`.

## Verification Before Opening a PR

Run all checks locally before pushing:

```bash
npm run lint   # TypeScript type-checking
npm test       # Unit tests for the game engine
npm run build  # Verify production build succeeds
```

## Commit Message Convention

We follow [Conventional Commits](https://www.conventionalcommits.org/):

- `feat:` — New feature (e.g., `feat: add volcanic biome palette`)
- `fix:` — Bug fix (e.g., `fix: prevent double jump reset on slope edge`)
- `docs:` — Documentation changes
- `refactor:` — Code restructuring without behavior changes
- `test:` — Adding or updating tests
- `chore:` — Tooling, dependencies, or CI maintenance

## Submitting a Pull Request

1. Push your branch to your fork:
   ```bash
   git push origin feat/your-feature-name
   ```
2. Open a Pull Request against `main` using the repository's PR template.
3. Describe **what** changed, **why**, and include screenshots or clips for visual/gameplay tweaks.

## Reporting Bugs & Suggesting Features

Please use the GitHub Issue Templates when opening an issue:
- **Bug Report** — include reproduction steps, expected vs. actual behavior, and browser/OS details.
- **Feature Request** — describe the gameplay or developer experience improvement and why it fits Pixel Stride.
