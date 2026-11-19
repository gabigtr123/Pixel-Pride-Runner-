import {
  BIOMES,
  DISTANCE_PER_ZONE,
  GROUND_Y,
  MAX_FLOATING_TEXTS,
  MAX_PARTICLES,
  VIRTUAL_HEIGHT,
  VIRTUAL_WIDTH,
} from './constants';
import {
  COIN_FRAMES,
  COIN_PALETTE,
  drawPixelGrid,
  HEART_PALETTE,
  HEART_SPRITE,
  HOPPER_CROUCH,
  HOPPER_IDLE,
  HOPPER_PALETTE,
  MAGNET_PALETTE,
  MAGNET_SPRITE,
  RUNNER_FRAMES,
  RUNNER_PALETTE,
  SHIELD_PALETTE,
  SHIELD_SPRITE,
  SPIKE_PALETTE,
  SPIKE_SPRITE,
  SWOOPER_DOWN,
  SWOOPER_PALETTE,
  SWOOPER_UP,
} from './sprites';
import type { BiomePalette, GameState } from './types';

// Deterministic starfield coordinates for night/dawn biomes
const STARS = Array.from({ length: 34 }, (_, idx) => ({
  x: (idx * 73 + 19) % VIRTUAL_WIDTH,
  y: (idx * 41 + 11) % 126,
  twinkleOffset: idx * 0.9,
  big: idx % 5 === 0,
}));

// Deterministic pixel cloud shapes
const CLOUDS = [
  { offset: 28, y: 34, w: 44, h: 12 },
  { offset: 158, y: 52, w: 32, h: 10 },
  { offset: 292, y: 26, w: 52, h: 14 },
  { offset: 410, y: 64, w: 36, h: 10 },
];

function hexToRgb(hex: string): [number, number, number] {
  const clean = hex.replace('#', '');
  const n = parseInt(clean, 16);
  return [(n >> 16) & 255, (n >> 8) & 255, n & 255];
}

function lerpColor(a: string, b: string, t: number): string {
  const [ar, ag, ab] = hexToRgb(a);
  const [br, bg, bb] = hexToRgb(b);
  const r = Math.round(ar + (br - ar) * t);
  const g = Math.round(ag + (bg - ag) * t);
  const bl = Math.round(ab + (bb - ab) * t);
  return `rgb(${r},${g},${bl})`;
}

function getBlendedBiome(state: GameState): BiomePalette {
  const currIdx = state.zoneIndex % BIOMES.length;
  const nextIdx = (currIdx + 1) % BIOMES.length;
  const curr = BIOMES[currIdx];
  const next = BIOMES[nextIdx];

  // Smooth transition during the last 15% of each biome zone
  const zoneProgress = (state.distance % DISTANCE_PER_ZONE) / DISTANCE_PER_ZONE;
  if (zoneProgress < 0.85 || state.phase === 'title') {
    return curr;
  }
  const t = Math.min(1, (zoneProgress - 0.85) / 0.15);
  return {
    name: curr.name,
    celestialType: curr.celestialType,
    starAlpha: curr.starAlpha + (next.starAlpha - curr.starAlpha) * t,
    skyTop: lerpColor(curr.skyTop, next.skyTop, t),
    skyMid: lerpColor(curr.skyMid, next.skyMid, t),
    skyHorizon: lerpColor(curr.skyHorizon, next.skyHorizon, t),
    celestialColor: lerpColor(curr.celestialColor, next.celestialColor, t),
    cloudMain: lerpColor(curr.cloudMain, next.cloudMain, t),
    cloudShadow: lerpColor(curr.cloudShadow, next.cloudShadow, t),
    farMountain: lerpColor(curr.farMountain, next.farMountain, t),
    nearHill: lerpColor(curr.nearHill, next.nearHill, t),
    ruinColor: lerpColor(curr.ruinColor, next.ruinColor, t),
    ruinHighlight: lerpColor(curr.ruinHighlight, next.ruinHighlight, t),
    grassTop: lerpColor(curr.grassTop, next.grassTop, t),
    grassTrim: lerpColor(curr.grassTrim, next.grassTrim, t),
    dirtMain: lerpColor(curr.dirtMain, next.dirtMain, t),
    dirtDark: lerpColor(curr.dirtDark, next.dirtDark, t),
    dirtAccent: lerpColor(curr.dirtAccent, next.dirtAccent, t),
  };
}

function drawSkyAndCelestial(ctx: CanvasRenderingContext2D, state: GameState, biome: BiomePalette): void {
  // Stepped retro 8-bit sky bands (no blurry modern gradients)
  ctx.fillStyle = biome.skyTop;
  ctx.fillRect(0, 0, VIRTUAL_WIDTH, 74);
  ctx.fillStyle = biome.skyMid;
  ctx.fillRect(0, 74, VIRTUAL_WIDTH, 76);
  ctx.fillStyle = biome.skyHorizon;
  ctx.fillRect(0, 150, VIRTUAL_WIDTH, GROUND_Y - 150);

  // Subtle stepped dither seams between sky bands
  ctx.fillStyle = biome.skyMid;
  for (let x = 0; x < VIRTUAL_WIDTH; x += 4) {
    ctx.fillRect(x, 72, 2, 2);
  }
  ctx.fillStyle = biome.skyHorizon;
  for (let x = 2; x < VIRTUAL_WIDTH; x += 4) {
    ctx.fillRect(x, 148, 2, 2);
  }

  // Twinkling stars
  if (biome.starAlpha > 0.02) {
    ctx.save();
    for (let i = 0; i < STARS.length; i++) {
      const s = STARS[i];
      const twinkle = 0.45 + 0.55 * Math.sin(state.elapsed * 3.2 + s.twinkleOffset);
      ctx.globalAlpha = biome.starAlpha * twinkle;
      ctx.fillStyle = '#FFF9E0';
      const sz = s.big ? 2 : 1;
      ctx.fillRect(s.x, s.y, sz, sz);
    }
    ctx.restore();
  }

  // Celestial body (pixel sun / sunset disc / crescent moon)
  const cx = 398;
  const cy = biome.celestialType === 'sunset' ? 128 : 54;
  ctx.fillStyle = biome.celestialColor;
  ctx.fillRect(cx - 10, cy - 14, 20, 28);
  ctx.fillRect(cx - 14, cy - 10, 28, 20);
  if (biome.celestialType === 'moon') {
    ctx.fillStyle = biome.skyTop;
    ctx.fillRect(cx - 4, cy - 12, 14, 22);
    ctx.fillRect(cx - 8, cy - 8, 14, 14);
  }
}

function drawParallaxWorld(ctx: CanvasRenderingContext2D, state: GameState, biome: BiomePalette): void {
  const scroll = state.worldScroll;

  // 1. Pixel clouds (slow parallax)
  const cloudScroll = scroll * 0.1;
  for (let i = 0; i < CLOUDS.length; i++) {
    const c = CLOUDS[i];
    const span = VIRTUAL_WIDTH + 90;
    const x = Math.round(((c.offset - cloudScroll) % span + span) % span) - 60;
    ctx.fillStyle = biome.cloudShadow;
    ctx.fillRect(x + 2, c.y + 4, c.w, c.h - 2);
    ctx.fillStyle = biome.cloudMain;
    ctx.fillRect(x, c.y + 2, c.w, c.h - 4);
    ctx.fillRect(x + 6, c.y - 4, c.w - 14, 8);
    ctx.fillRect(x + 12, c.y - 8, Math.max(10, c.w - 26), 6);
  }

  // 2. Far pixel mountains (stepped silhouettes with sunlit snow/ridge caps)
  const mtnScroll = scroll * 0.2;
  const mtnTile = 120;
  const mtnOffset = -((mtnScroll % mtnTile + mtnTile) % mtnTile);
  for (let base = mtnOffset - mtnTile; base < VIRTUAL_WIDTH + mtnTile; base += mtnTile) {
    const bx = Math.round(base);
    // Stepped pyramid peak 1
    for (let step = 0; step < 14; step++) {
      const w = 64 - step * 4;
      ctx.fillStyle = step >= 11 ? biome.cloudShadow : biome.farMountain;
      ctx.fillRect(bx + 8 + step * 2, GROUND_Y - 12 - step * 5, w, 6);
    }
    // Stepped peak 2
    for (let step = 0; step < 10; step++) {
      const w = 52 - step * 4;
      ctx.fillStyle = step >= 8 ? biome.cloudShadow : biome.farMountain;
      ctx.fillRect(bx + 62 + step * 2, GROUND_Y - 12 - step * 5, w, 6);
    }
  }

  // 3. Midground pixel hills, pixel pines & ruins/battlements
  const hillScroll = scroll * 0.42;
  const hillTile = 160;
  const hillOffset = -((hillScroll % hillTile + hillTile) % hillTile);
  for (let base = hillOffset - hillTile; base < VIRTUAL_WIDTH + hillTile; base += hillTile) {
    const bx = Math.round(base);
    // Rolling stepped hill
    ctx.fillStyle = biome.nearHill;
    ctx.fillRect(bx, GROUND_Y - 26, 98, 26);
    ctx.fillRect(bx + 12, GROUND_Y - 34, 74, 8);
    ctx.fillRect(bx + 26, GROUND_Y - 40, 46, 6);
    // Hilltop grass accent highlight
    ctx.fillStyle = biome.grassTrim;
    ctx.fillRect(bx + 26, GROUND_Y - 40, 46, 2);
    ctx.fillRect(bx + 12, GROUND_Y - 34, 14, 2);

    // Classic 8-bit pixel pine trees flanking the hill
    ctx.fillStyle = biome.dirtDark;
    ctx.fillRect(bx + 16, GROUND_Y - 10, 4, 10);
    ctx.fillRect(bx + 78, GROUND_Y - 8, 4, 8);
    ctx.fillStyle = biome.grassTrim;
    ctx.fillRect(bx + 10, GROUND_Y - 16, 16, 6);
    ctx.fillRect(bx + 12, GROUND_Y - 22, 12, 6);
    ctx.fillRect(bx + 15, GROUND_Y - 27, 6, 5);
    ctx.fillRect(bx + 73, GROUND_Y - 14, 14, 6);
    ctx.fillRect(bx + 76, GROUND_Y - 19, 8, 5);

    // Ancient pixel aqueduct / ruin tower
    ctx.fillStyle = biome.ruinColor;
    ctx.fillRect(bx + 106, GROUND_Y - 44, 28, 44);
    ctx.fillRect(bx + 102, GROUND_Y - 48, 8, 6);
    ctx.fillRect(bx + 116, GROUND_Y - 48, 8, 6);
    ctx.fillRect(bx + 130, GROUND_Y - 48, 8, 6);
    ctx.fillStyle = biome.ruinHighlight;
    ctx.fillRect(bx + 108, GROUND_Y - 42, 4, 38);
    ctx.fillStyle = biome.skyHorizon;
    ctx.fillRect(bx + 116, GROUND_Y - 26, 8, 12); // window arch
  }
}

function drawGround(ctx: CanvasRenderingContext2D, state: GameState, biome: BiomePalette): void {
  const tileSize = 16;
  const scrollOffset = -Math.floor(((state.worldScroll % tileSize) + tileSize) % tileSize);

  // Sub-earth fill
  ctx.fillStyle = biome.dirtMain;
  ctx.fillRect(0, GROUND_Y, VIRTUAL_WIDTH, VIRTUAL_HEIGHT - GROUND_Y);

  // Top grass lip with dark 8-bit outline
  ctx.fillStyle = '#121526';
  ctx.fillRect(0, GROUND_Y - 1, VIRTUAL_WIDTH, 2);
  ctx.fillStyle = biome.grassTop;
  ctx.fillRect(0, GROUND_Y + 1, VIRTUAL_WIDTH, 5);
  ctx.fillStyle = biome.grassTrim;
  ctx.fillRect(0, GROUND_Y + 6, VIRTUAL_WIDTH, 3);

  // Classic NES checkerboard / brick ground masonry
  for (let x = scrollOffset - tileSize; x < VIRTUAL_WIDTH + tileSize; x += tileSize) {
    // Grass blades overhang notch
    ctx.fillStyle = biome.grassTrim;
    ctx.fillRect(x + 4, GROUND_Y + 9, 4, 2);
    ctx.fillRect(x + 12, GROUND_Y + 9, 2, 3);

    // Brick rows
    ctx.fillStyle = biome.dirtDark;
    ctx.fillRect(x, GROUND_Y + 14, tileSize - 1, 1);
    ctx.fillRect(x + 8, GROUND_Y + 14, 1, 12);
    ctx.fillRect(x, GROUND_Y + 26, tileSize - 1, 1);
    ctx.fillRect(x, GROUND_Y + 26, 1, 14);
    ctx.fillRect(x, GROUND_Y + 39, tileSize - 1, 1);

    // Brick highlight pips
    ctx.fillStyle = biome.dirtAccent;
    ctx.fillRect(x + 2, GROUND_Y + 16, 4, 2);
    ctx.fillRect(x + 10, GROUND_Y + 28, 4, 2);
  }
}

function drawPlatforms(ctx: CanvasRenderingContext2D, state: GameState, biome: BiomePalette): void {
  for (let i = 0; i < state.platforms.length; i++) {
    const plat = state.platforms[i];
    const x = Math.round(plat.x);
    const y = Math.round(plat.y);

    // Outline
    ctx.fillStyle = '#121526';
    ctx.fillRect(x - 1, y - 1, plat.width + 2, plat.height + 2);
    // Grass/trim top
    ctx.fillStyle = biome.grassTop;
    ctx.fillRect(x, y, plat.width, 3);
    // Masonry body
    ctx.fillStyle = biome.dirtMain;
    ctx.fillRect(x, y + 3, plat.width, plat.height - 3);
    ctx.fillStyle = biome.dirtDark;
    for (let bx = x + 8; bx < x + plat.width; bx += 16) {
      ctx.fillRect(bx, y + 3, 2, plat.height - 3);
    }
    ctx.fillStyle = biome.dirtAccent;
    for (let bx = x + 2; bx < x + plat.width - 4; bx += 16) {
      ctx.fillRect(bx, y + 4, 4, 2);
    }
  }
}

function drawPickups(ctx: CanvasRenderingContext2D, state: GameState): void {
  const spinFrame = Math.floor(state.elapsed * 9) % COIN_FRAMES.length;
  for (let i = 0; i < state.pickups.length; i++) {
    const item = state.pickups[i];
    const bob = Math.round(Math.sin(item.bobOffset) * 2.5);
    const x = Math.round(item.x);
    const y = Math.round(item.y + bob);

    if (item.kind === 'coin') {
      drawPixelGrid(ctx, COIN_FRAMES[spinFrame], COIN_PALETTE, x, y, 1);
    } else if (item.kind === 'shield') {
      drawPixelGrid(ctx, SHIELD_SPRITE, SHIELD_PALETTE, x, y, 1);
    } else if (item.kind === 'heart') {
      drawPixelGrid(ctx, HEART_SPRITE, HEART_PALETTE, x + 1, y + 1, 1);
    } else if (item.kind === 'magnet') {
      drawPixelGrid(ctx, MAGNET_SPRITE, MAGNET_PALETTE, x + 1, y + 1, 1);
    }
  }
}

function drawHazards(ctx: CanvasRenderingContext2D, state: GameState): void {
  const wingToggle = Math.floor(state.elapsed * 12) % 2 === 0;
  for (let i = 0; i < state.hazards.length; i++) {
    const h = state.hazards[i];
    const flash = h.hitFlash > 0;

    if (h.kind === 'spike') {
      const count = h.width > 18 ? 2 : 1;
      for (let s = 0; s < count; s++) {
        drawPixelGrid(ctx, SPIKE_SPRITE, SPIKE_PALETTE, h.x + s * 14, h.y + 4, 1, flash);
      }
    } else if (h.kind === 'hopper') {
      ctx.save();
      if (h.stomped) {
        ctx.translate(Math.round(h.x + h.width * 0.5), Math.round(h.y + h.height * 0.5));
        ctx.scale(1, -1); // upside-down KO topple
        drawPixelGrid(ctx, HOPPER_IDLE, HOPPER_PALETTE, -9, -5, 1, flash);
      } else {
        const isTelegraph = h.telegraphTimer > 0;
        const grid = isTelegraph ? HOPPER_CROUCH : HOPPER_IDLE;
        const blinkFlash = isTelegraph && Math.floor(state.elapsed * 22) % 2 === 0;
        drawPixelGrid(ctx, grid, HOPPER_PALETTE, h.x, h.y + 5, 1, flash || blinkFlash);
      }
      ctx.restore();
    } else if (h.kind === 'swooper') {
      // Right-edge warning telegraph chevron before swooper enters
      if (h.telegraphTimer > 0 && !h.stomped) {
        const blink = Math.floor(state.elapsed * 20) % 2 === 0;
        const wy = Math.round(h.baseY + 2);
        ctx.fillStyle = blink ? '#FFE048' : '#FF4858';
        ctx.fillRect(VIRTUAL_WIDTH - 18, wy, 12, 12);
        ctx.fillStyle = '#141224';
        ctx.fillRect(VIRTUAL_WIDTH - 13, wy + 2, 2, 5);
        ctx.fillRect(VIRTUAL_WIDTH - 13, wy + 8, 2, 2);
      } else {
        ctx.save();
        if (h.stomped) {
          ctx.translate(Math.round(h.x + h.width * 0.5), Math.round(h.y + h.height * 0.5));
          ctx.scale(1, -1);
          drawPixelGrid(ctx, SWOOPER_UP, SWOOPER_PALETTE, -10, -5, 1, flash);
        } else {
          drawPixelGrid(
            ctx,
            wingToggle ? SWOOPER_UP : SWOOPER_DOWN,
            SWOOPER_PALETTE,
            h.x,
            h.y + 2,
            1,
            flash,
          );
        }
        ctx.restore();
      }
    } else if (h.kind === 'flame') {
      const x = Math.round(h.x);
      // Stone brazier base
      ctx.fillStyle = '#141628';
      ctx.fillRect(x, GROUND_Y - 12, 16, 12);
      ctx.fillStyle = '#586484';
      ctx.fillRect(x + 2, GROUND_Y - 10, 12, 10);
      ctx.fillStyle = '#92A2C8';
      ctx.fillRect(x + 2, GROUND_Y - 10, 4, 8);

      if (h.telegraphTimer > 0) {
        // Rising ember telegraph warning
        const pulse = Math.floor(state.elapsed * 24) % 2 === 0;
        ctx.fillStyle = pulse ? '#FFF4A8' : '#FF8828';
        ctx.fillRect(x + 3, GROUND_Y - 16, 10, 5);
        ctx.fillRect(x + 5, GROUND_Y - 20, 6, 4);
      } else if (h.isActive) {
        // Tall animated 8-bit flame jet
        const flicker = Math.floor(state.elapsed * 18) % 2;
        ctx.fillStyle = '#E83828';
        ctx.fillRect(x + 2, GROUND_Y - 35 + flicker, 12, 24);
        ctx.fillRect(x + 4, GROUND_Y - 39 - flicker, 8, 6);
        ctx.fillStyle = '#FF9C28';
        ctx.fillRect(x + 4, GROUND_Y - 32, 8, 21);
        ctx.fillStyle = '#FFF4A8';
        ctx.fillRect(x + 6, GROUND_Y - 26 + flicker, 4, 15);
      }
    }
  }
}

function drawPlayer(ctx: CanvasRenderingContext2D, state: GameState): void {
  const p = state.player;
  // Invulnerability blink frame
  if (p.invulnTimer > 0 && !p.koActive && Math.floor(state.elapsed * 20) % 2 === 0 && p.hitFlashTimer <= 0) {
    return;
  }

  // Contact shadow on ground or platform
  if (!p.koActive) {
    const shadowY = p.onPlatform ? p.y : GROUND_Y;
    const altitude = Math.max(0, shadowY - p.y);
    const shadowW = Math.max(6, Math.round(14 - altitude * 0.08));
    ctx.fillStyle = 'rgba(12, 14, 30, 0.45)';
    ctx.fillRect(Math.round(p.x + (p.width - shadowW) * 0.5), Math.round(shadowY - 1), shadowW, 3);
  }

  // Select sprite frame synced to distance traveled
  let grid = RUNNER_FRAMES.jump;
  if (p.isDucking) {
    grid = RUNNER_FRAMES.duck;
  } else if (p.onGround) {
    const idx = Math.floor(p.runCycleDist / 16) % RUNNER_FRAMES.run.length;
    grid = RUNNER_FRAMES.run[idx];
  }

  const spriteH = grid.length;
  const spriteW = grid[0].length;

  ctx.save();
  const anchorX = Math.round(p.x + p.width * 0.5);
  const anchorY = Math.round(p.y);
  ctx.translate(anchorX, anchorY);
  if (p.koActive) {
    ctx.translate(0, -spriteH * 0.5);
    ctx.rotate(p.koRot);
    ctx.translate(0, spriteH * 0.5);
  }
  ctx.scale(p.scaleX, p.scaleY);

  drawPixelGrid(
    ctx,
    grid,
    RUNNER_PALETTE,
    -spriteW * 0.5,
    -spriteH,
    1,
    p.hitFlashTimer > 0,
  );
  ctx.restore();

  // Orbiting Star Shield barrier (diegetic on-character visual)
  if (p.shieldActive && !p.koActive) {
    const cx = Math.round(p.x + p.width * 0.5);
    const cy = Math.round(p.y - p.height * 0.5);
    const r = 16;
    ctx.fillStyle = 'rgba(88, 216, 255, 0.22)';
    ctx.fillRect(cx - r + 2, cy - r + 2, (r - 2) * 2, (r - 2) * 2);
    for (let i = 0; i < 4; i++) {
      const a = state.elapsed * 5 + (i * Math.PI) / 2;
      const sx = Math.round(cx + Math.cos(a) * r) - 1;
      const sy = Math.round(cy + Math.sin(a) * r) - 1;
      ctx.fillStyle = i % 2 === 0 ? '#FFFFFF' : '#58D8FF';
      ctx.fillRect(sx, sy, 3, 3);
    }
  }

  // Magnet field pixel arcs
  if (p.magnetTimer > 0 && !p.koActive) {
    const cx = Math.round(p.x + p.width * 0.5);
    const cy = Math.round(p.y - p.height * 0.5);
    const pulse = 18 + Math.round(Math.sin(state.elapsed * 12) * 3);
    ctx.fillStyle = 'rgba(255, 144, 216, 0.55)';
    ctx.fillRect(cx - pulse, cy - 2, 2, 4);
    ctx.fillRect(cx + pulse - 2, cy - 2, 2, 4);
    ctx.fillRect(cx - 2, cy - pulse, 4, 2);
  }
}

function drawParticlesAndPopups(ctx: CanvasRenderingContext2D, state: GameState): void {
  for (let i = 0; i < MAX_PARTICLES; i++) {
    const pt = state.particles[i];
    if (!pt.active) continue;
    ctx.fillStyle = pt.color;
    ctx.fillRect(Math.round(pt.x), Math.round(pt.y), pt.size, pt.size);
  }

  ctx.font = 'bold 7px "Press Start 2P", monospace';
  ctx.textAlign = 'center';
  for (let i = 0; i < MAX_FLOATING_TEXTS; i++) {
    const ft = state.floatingTexts[i];
    if (!ft.active) continue;
    const x = Math.round(ft.x);
    const y = Math.round(ft.y);
    ctx.fillStyle = '#121324';
    ctx.fillText(ft.text, x + 1, y + 1);
    ctx.fillStyle = ft.color;
    ctx.fillText(ft.text, x, y);
  }
}

export function renderGameWorld(ctx: CanvasRenderingContext2D, state: GameState): void {
  ctx.imageSmoothingEnabled = false;
  const biome = getBlendedBiome(state);

  ctx.save();
  // Decaying trauma*trauma pixel camera shake
  if (state.shakeTrauma > 0.01) {
    const mag = state.shakeTrauma * state.shakeTrauma * 6;
    const ox = Math.round((Math.sin(state.elapsed * 67) * mag));
    const oy = Math.round((Math.cos(state.elapsed * 83) * mag));
    ctx.translate(ox, oy);
  }

  drawSkyAndCelestial(ctx, state, biome);
  drawParallaxWorld(ctx, state, biome);
  drawGround(ctx, state, biome);
  drawPlatforms(ctx, state, biome);
  drawPickups(ctx, state);
  drawHazards(ctx, state);
  drawPlayer(ctx, state);
  drawParticlesAndPopups(ctx, state);
  ctx.restore();

  // Subtle peripheral crimson warning edge when at 1 heart during active play
  if (state.phase === 'playing' && state.player.hp === 1 && !state.player.koActive) {
    const pulse = 0.14 + 0.1 * Math.sin(state.elapsed * 7);
    ctx.fillStyle = `rgba(232, 48, 72, ${pulse.toFixed(3)})`;
    ctx.fillRect(0, 0, VIRTUAL_WIDTH, 4);
    ctx.fillRect(0, VIRTUAL_HEIGHT - 4, VIRTUAL_WIDTH, 4);
    ctx.fillRect(0, 0, 4, VIRTUAL_HEIGHT);
    ctx.fillRect(VIRTUAL_WIDTH - 4, 0, 4, VIRTUAL_HEIGHT);
  }
}
