import {
  BASE_SPEED,
  BIOMES,
  COMBO_DURATION,
  COYOTE_TIME,
  CYCLES_PER_LEVEL,
  DAY_NIGHT_CYCLE_SECONDS,
  DISTANCE_PER_ZONE,
  DOUBLE_JUMP_VELOCITY,
  FAST_FALL_GRAVITY,
  GRAVITY,
  GROUND_Y,
  JUMP_BUFFER_TIME,
  JUMP_CUT_MULTIPLIER,
  JUMP_VELOCITY,
  LEVEL_BONUS_POINTS,
  LEVEL_NAMES,
  MAX_FLOATING_TEXTS,
  MAX_PARTICLES,
  MAX_SPEED,
  PLAYER_DUCK_HEIGHT,
  PLAYER_STAND_HEIGHT,
  PLAYER_WIDTH,
  PLAYER_X,
  SAFE_OPENING_SECONDS,
  STOMP_BOUNCE_VELOCITY,
  VIRTUAL_WIDTH,
} from './constants';
import { getTrailColors, type RunModifiers } from './shop';
import type {
  BossEntity,
  BossKind,
  CharacterId,
  FloatingText,
  GamePhase,
  GameState,
  HazardEntity,
  HudSnapshot,
  InputFrame,
  Particle,
  PlayerState,
} from './types';

export function makeDateSeededRng(dateKey: string): () => number {
  let h = 2166136261 >>> 0;
  for (let i = 0; i < dateKey.length; i++) {
    h ^= dateKey.charCodeAt(i);
    h = Math.imul(h, 16777619);
  }
  let s = h >>> 0;
  return () => {
    s = (s + 0x6d2b79f5) | 0;
    let t = Math.imul(s ^ (s >>> 15), 1 | s);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

export interface Rect {
  x: number;
  y: number;
  width: number;
  height: number;
}

export function checkAABB(a: Rect, b: Rect): boolean {
  return (
    a.x < b.x + b.width &&
    a.x + a.width > b.x &&
    a.y < b.y + b.height &&
    a.y + a.height > b.y
  );
}

export function getPlayerHitbox(player: PlayerState): Rect {
  // Generous arcade hitbox slightly inset from visual sprite edges
  const insetX = 3;
  const insetTop = player.isDucking ? 2 : 3;
  return {
    x: player.x + insetX,
    y: player.y - player.height + insetTop,
    width: player.width - insetX * 2,
    height: player.height - insetTop,
  };
}

export function computeDifficulty(runTime: number, distance: number) {
  // Multi-knob progression: speed, spawn density, multi-archetype mix, elevated routes
  const speedRamp = Math.min(1, runTime / 115);
  const speed = BASE_SPEED + (MAX_SPEED - BASE_SPEED) * (1 - Math.pow(1 - speedRamp, 1.45));
  const minGapSec = Math.max(0.72, 1.55 - speedRamp * 0.78);
  const maxGapSec = Math.max(1.18, 2.3 - speedRamp * 0.98);
  const allowHopper = runTime >= 4.0;
  const allowSwooper = runTime >= 7.5;
  const allowFlame = runTime >= 11.5;
  const allowDoubleSpike = runTime >= 18.0;
  const zoneIndex = Math.floor(distance / DISTANCE_PER_ZONE) % BIOMES.length;

  return {
    speed,
    minGapSec,
    maxGapSec,
    allowHopper,
    allowSwooper,
    allowFlame,
    allowDoubleSpike,
    zoneIndex,
  };
}

function createParticlePool(): Particle[] {
  const pool: Particle[] = [];
  for (let i = 0; i < MAX_PARTICLES; i++) {
    pool.push({
      active: false,
      x: 0,
      y: 0,
      vx: 0,
      vy: 0,
      size: 2,
      color: '#FFFFFF',
      life: 0,
      maxLife: 0.4,
      gravity: 0,
    });
  }
  return pool;
}

function createTextPool(): FloatingText[] {
  const pool: FloatingText[] = [];
  for (let i = 0; i < MAX_FLOATING_TEXTS; i++) {
    pool.push({
      active: false,
      x: 0,
      y: 0,
      vy: -28,
      text: '',
      color: '#FFF4A8',
      life: 0,
      maxLife: 0.75,
    });
  }
  return pool;
}

export function createInitialPlayer(characterId: CharacterId = 'stride'): PlayerState {
  const maxHp = characterId === 'knight' ? 4 : 3;
  return {
    x: PLAYER_X,
    y: GROUND_Y,
    vy: 0,
    width: PLAYER_WIDTH,
    height: PLAYER_STAND_HEIGHT,
    standHeight: PLAYER_STAND_HEIGHT,
    duckHeight: PLAYER_DUCK_HEIGHT,
    onGround: true,
    onPlatform: false,
    isDucking: false,
    jumpsRemaining: 2,
    maxJumps: 2,
    coyoteTimer: COYOTE_TIME,
    jumpBufferTimer: 0,
    hp: maxHp,
    maxHp,
    invulnTimer: 0,
    hitFlashTimer: 0,
    hurtAnimationTimer: 0,
    shieldActive: false,
    magnetTimer: 0,
    doubleScoreTimer: 0,
    slowMoTimer: 0,
    turboDashTimer: 0,
    autoShieldTimer: 45,
    secondWindUsed: false,
    trailTimer: 0,
    scaleX: 1,
    scaleY: 1,
    runCycleDist: 0,
    slideDustTimer: 0,
    koActive: false,
    koVy: 0,
    koRot: 0,
  };
}

export function createInitialState(
  bestScore = 0,
  phase: GamePhase = 'title',
  characterId: CharacterId = 'stride',
): GameState {
  return {
    phase,
    gameMode: 'endless',
    characterId,
    trailId: 'classic',
    hatId: 'none',
    capeId: 'none',
    petId: 'none',
    petActionTimer: 16,
    gadgets: [],
    magnetDurationBonus: 0,
    coinBonusPoints: 0,
    powerupCooldownReduction: 0,
    elapsed: 0,
    runTime: 0,
    distance: 0,
    worldScroll: 0,
    speed: BASE_SPEED,
    score: 0,
    bonusScore: 0,
    bestScore,
    runStartBestScore: bestScore,
    coins: 0,
    stomps: 0,
    combo: 1,
    comboTimer: 0,
    zoneIndex: 0,
    zoneBannerTimer: phase === 'playing' ? 2.2 : 0,
    level: 1,
    dayCycleCount: 0,
    levelBannerTimer: 0,
    boss: null,
    bossesDefeated: 0,
    bossRushCooldown: 4,
    hitstopTimer: 0,
    shakeTrauma: 0,
    gameOverDelayTimer: 0,
    nextEntityId: 1,
    hazardCooldown: SAFE_OPENING_SECONDS,
    platformCooldown: 3.6,
    coinArcCooldown: 1.1,
    powerupCooldown: 11.5,
    player: createInitialPlayer(characterId),
    platforms: [],
    hazards: [],
    pickups: [],
    particles: createParticlePool(),
    particleCursor: 0,
    floatingTexts: createTextPool(),
    textCursor: 0,
    audioQueue: [],
    finalRun: null,
  };
}

export function startNewRun(
  state: GameState,
  modifiers?: Partial<RunModifiers>,
): GameState {
  const best = Math.max(state.bestScore, state.score);
  const next = createInitialState(best, 'playing', state.characterId);
  next.customCharacter = state.customCharacter;
  next.hatId = state.hatId;
  next.capeId = state.capeId;
  next.petId = state.petId;
  next.gameMode = state.gameMode;
  next.gadgets = [...state.gadgets];
  if (modifiers) {
    if (modifiers.trailId) {
      next.trailId = modifiers.trailId;
    }
    if (modifiers.hatId) {
      next.hatId = modifiers.hatId;
    }
    if (modifiers.capeId) {
      next.capeId = modifiers.capeId;
    }
    if (modifiers.petId) {
      next.petId = modifiers.petId;
    }
    if (modifiers.gameMode) {
      next.gameMode = modifiers.gameMode;
    }
    if (modifiers.gadgets) {
      next.gadgets = [...modifiers.gadgets];
    }
    if (modifiers.magnetDurationBonus && modifiers.magnetDurationBonus > 0) {
      next.magnetDurationBonus = modifiers.magnetDurationBonus;
    }
    if (modifiers.coinBonusPoints && modifiers.coinBonusPoints > 0) {
      next.coinBonusPoints = modifiers.coinBonusPoints;
    }
    if (modifiers.powerupCooldownReduction && modifiers.powerupCooldownReduction > 0) {
      next.powerupCooldownReduction = modifiers.powerupCooldownReduction;
      next.powerupCooldown = Math.max(5.5, next.powerupCooldown - modifiers.powerupCooldownReduction);
    }
    if (modifiers.startingScoreBonus && modifiers.startingScoreBonus > 0) {
      next.bonusScore += modifiers.startingScoreBonus;
      next.score = next.bonusScore;
      spawnFloatingText(
        next,
        next.player.x + 12,
        next.player.y - next.player.height - 10,
        `DAILY +${modifiers.startingScoreBonus}`,
        '#78F0A8',
      );
    }
    if (modifiers.startWithShield) {
      next.player.shieldActive = true;
    }
    if (modifiers.startMagnetSeconds && modifiers.startMagnetSeconds > 0) {
      next.player.magnetTimer = modifiers.startMagnetSeconds;
    }
    if (modifiers.startDoubleScoreSeconds && modifiers.startDoubleScoreSeconds > 0) {
      next.player.doubleScoreTimer = Math.max(
        next.player.doubleScoreTimer,
        modifiers.startDoubleScoreSeconds,
      );
    }
    if (modifiers.startTurboDashSeconds && modifiers.startTurboDashSeconds > 0) {
      next.player.turboDashTimer = modifiers.startTurboDashSeconds;
      spawnFloatingText(
        next,
        next.player.x + 12,
        next.player.y - next.player.height - 20,
        'HEAD START!',
        '#FFE848',
      );
    }
  }
  if (next.gadgets.includes('springBoots')) {
    next.player.maxJumps = 3;
    next.player.jumpsRemaining = 3;
  }
  if (next.gameMode === 'hardcore') {
    next.player.maxHp = 1;
    next.player.hp = 1;
    spawnFloatingText(
      next,
      VIRTUAL_WIDTH * 0.5,
      64,
      'HARDCORE 2X!',
      '#FF5874',
    );
  } else if (next.gameMode === 'bossRush') {
    next.bossRushCooldown = 3.5;
    next.hazardCooldown = 999;
    spawnFloatingText(
      next,
      VIRTUAL_WIDTH * 0.5,
      64,
      'BOSS RUSH!',
      '#FFE05C',
    );
  } else if (next.gameMode === 'daily') {
    spawnFloatingText(
      next,
      VIRTUAL_WIDTH * 0.5,
      64,
      'DAILY SEED!',
      '#78E8FF',
    );
  }
  next.audioQueue.push({ type: 'start' });
  return next;
}

export function returnToTitle(state: GameState): GameState {
  const best = Math.max(state.bestScore, state.score);
  const next = createInitialState(best, 'title', state.characterId);
  next.customCharacter = state.customCharacter;
  next.hatId = state.hatId;
  next.capeId = state.capeId;
  next.petId = state.petId;
  next.trailId = state.trailId;
  next.gameMode = state.gameMode;
  next.gadgets = [...state.gadgets];
  return next;
}

const BOSS_ROSTER: { kind: BossKind; name: string; hp: number; baseY: number }[] = [
  { kind: 'beetle', name: 'IRON BEETLE', hp: 3, baseY: GROUND_Y - 28 },
  { kind: 'golem', name: 'SAND GOLEM', hp: 4, baseY: GROUND_Y - 28 },
  { kind: 'drone', name: 'NEON DRONE', hp: 4, baseY: GROUND_Y - 54 },
  { kind: 'wyrm', name: 'CRYSTAL WYRM', hp: 5, baseY: GROUND_Y - 48 },
];

export function spawnBoss(state: GameState, pendingLevel: number): BossEntity {
  const idx = (((pendingLevel - 2) % BOSS_ROSTER.length) + BOSS_ROSTER.length) % BOSS_ROSTER.length;
  const def = BOSS_ROSTER[idx];
  const boss: BossEntity = {
    id: state.nextEntityId++,
    kind: def.kind,
    name: def.name,
    x: VIRTUAL_WIDTH + 38,
    y: def.baseY,
    targetX: 282,
    baseY: def.baseY,
    width: 36,
    height: 28,
    hp: def.hp,
    maxHp: def.hp,
    phaseTimer: 0,
    attackTimer: 1.9,
    hitFlash: 0,
    stompCooldown: 0,
    pendingLevel,
  };
  state.boss = boss;
  state.audioQueue.push({ type: 'telegraph' });
  spawnFloatingText(state, VIRTUAL_WIDTH * 0.5, 68, `BOSS: ${def.name}!`, '#FF5874');
  return boss;
}

export function spawnParticles(
  state: GameState,
  x: number,
  y: number,
  count: number,
  colors: string[],
  speedMin: number,
  speedMax: number,
  gravity: number,
  rng: () => number = Math.random,
  upwardBias = -40,
): void {
  for (let i = 0; i < count; i++) {
    const p = state.particles[state.particleCursor];
    state.particleCursor = (state.particleCursor + 1) % MAX_PARTICLES;
    const angle = rng() * Math.PI * 2;
    const mag = speedMin + rng() * (speedMax - speedMin);
    p.active = true;
    p.x = x + (rng() - 0.5) * 6;
    p.y = y + (rng() - 0.5) * 6;
    p.vx = Math.cos(angle) * mag;
    p.vy = Math.sin(angle) * mag + upwardBias;
    p.size = rng() > 0.55 ? 3 : 2;
    p.color = colors[Math.floor(rng() * colors.length) % colors.length];
    p.maxLife = 0.28 + rng() * 0.32;
    p.life = p.maxLife;
    p.gravity = gravity;
  }
}

export function spawnFloatingText(
  state: GameState,
  x: number,
  y: number,
  text: string,
  color: string,
): void {
  const ft = state.floatingTexts[state.textCursor];
  state.textCursor = (state.textCursor + 1) % MAX_FLOATING_TEXTS;
  ft.active = true;
  ft.x = x;
  ft.y = y;
  ft.vy = -34;
  ft.text = text;
  ft.color = color;
  ft.maxLife = 0.78;
  ft.life = ft.maxLife;
}

export function executeJump(state: GameState, rng: () => number = Math.random): boolean {
  const p = state.player;
  if (p.koActive) return false;

  const canGroundJump = p.onGround || p.coyoteTimer > 0;
  if (canGroundJump) {
    p.vy = JUMP_VELOCITY;
    p.onGround = false;
    p.onPlatform = false;
    p.coyoteTimer = 0;
    p.jumpBufferTimer = 0;
    p.jumpsRemaining = p.maxJumps - 1;
    p.isDucking = false;
    p.height = p.standHeight;
    p.scaleX = 0.84;
    p.scaleY = 1.18;
    spawnParticles(state, p.x + p.width * 0.5, p.y - 1, 5, ['#F4EFE0', '#C8D0E4'], 22, 65, 220, rng, -25);
    state.audioQueue.push({ type: 'jump', pitchScale: 0.92 + rng() * 0.16 });
    return true;
  }

  if (p.jumpsRemaining > 0) {
    p.vy = DOUBLE_JUMP_VELOCITY;
    p.jumpsRemaining -= 1;
    p.jumpBufferTimer = 0;
    p.isDucking = false;
    p.height = p.standHeight;
    p.scaleX = 0.82;
    p.scaleY = 1.22;
    const djColors =
      state.characterId === 'lily'
        ? ['#FFFDF5', '#FF98C8', '#78E8A8']
        : ['#FFF4A8', '#FFFFFF', '#78E0FF'];
    spawnParticles(state, p.x + p.width * 0.5, p.y - 2, 8, djColors, 35, 85, 90, rng, 15);
    state.audioQueue.push({ type: 'doubleJump', pitchScale: 0.93 + rng() * 0.14 });
    return true;
  }

  return false;
}

export function applyDamageToPlayer(state: GameState, rng: () => number = Math.random): boolean {
  const p = state.player;
  if (p.invulnTimer > 0 || p.turboDashTimer > 0 || p.koActive || state.phase !== 'playing') {
    return false;
  }

  if (p.shieldActive) {
    p.shieldActive = false;
    p.invulnTimer = 1.0;
    p.hitFlashTimer = 0.06;
    state.hitstopTimer = 0.045;
    state.shakeTrauma = Math.min(1, state.shakeTrauma + 0.5);
    spawnParticles(state, p.x + p.width * 0.5, p.y - p.height * 0.5, 16, ['#78E8FF', '#FFFFFF', '#48A8FF'], 55, 140, 140, rng, -20);
    spawnFloatingText(state, p.x + 8, p.y - p.height - 6, 'SHIELD SAVED!', '#78E8FF');
    state.audioQueue.push({ type: 'shieldBreak' });
    return false;
  }

  p.hp = Math.max(0, p.hp - 1);
  p.invulnTimer = 1.25;
  p.hitFlashTimer = 0.065;
  p.hurtAnimationTimer = 0.35;
  state.combo = 1;
  state.comboTimer = 0;
  state.hitstopTimer = 0.065;
  state.shakeTrauma = Math.min(1, state.shakeTrauma + 0.78);
  spawnParticles(state, p.x + p.width * 0.5, p.y - p.height * 0.5, 18, ['#FF4858', '#FFA848', '#FFFFFF'], 50, 155, 280, rng, -50);

  if (p.hp <= 0) {
    if (state.gadgets.includes('secondWind') && !p.secondWindUsed) {
      p.secondWindUsed = true;
      p.hp = Math.min(p.maxHp, 2);
      p.invulnTimer = 2.2;
      p.vy = -240;
      p.onGround = false;
      state.hazards = state.hazards.filter((h) => h.x < p.x - 20 || h.x > p.x + 150);
      spawnParticles(state, p.x + p.width * 0.5, p.y - p.height * 0.5, 24, ['#78F0A8', '#FFE05C', '#FFFFFF'], 70, 180, 160, rng, -60);
      spawnFloatingText(state, p.x + 10, p.y - p.height - 8, 'SECOND WIND!', '#78F0A8');
      state.audioQueue.push({ type: 'powerup', kind: 'heart' });
      return true;
    }
    p.koActive = true;
    p.vy = -210;
    p.onGround = false;
    state.gameOverDelayTimer = 0.55;
    state.audioQueue.push({ type: 'gameOver' });
  } else {
    p.vy = Math.min(p.vy, -170);
    p.onGround = false;
    spawnFloatingText(state, p.x + 8, p.y - p.height - 6, 'OUCH!', '#FF6878');
    state.audioQueue.push({ type: 'hurt' });
  }
  return true;
}

function spawnHazardPattern(state: GameState, rng: () => number): void {
  const diff = computeDifficulty(state.runTime, state.distance);
  const candidates: HazardEntity['kind'][] = ['spike'];
  if (diff.allowHopper) candidates.push('hopper', 'hopper');
  if (diff.allowSwooper) candidates.push('swooper', 'swooper');
  if (diff.allowFlame) candidates.push('flame');

  const chosen = candidates[Math.floor(rng() * candidates.length) % candidates.length];
  const spawnX = VIRTUAL_WIDTH + 24;

  if (chosen === 'spike') {
    const isDouble = diff.allowDoubleSpike && rng() < 0.36;
    const width = isDouble ? 28 : 14;
    state.hazards.push({
      id: state.nextEntityId++,
      kind: 'spike',
      x: spawnX,
      y: GROUND_Y - 18,
      width,
      height: 18,
      baseY: GROUND_Y - 18,
      vx: 0,
      vy: 0,
      phaseTimer: 0,
      telegraphTimer: 0,
      telegraphSounded: true,
      isActive: true,
      stomped: false,
      koTimer: 0,
      koVx: 0,
      koVy: 0,
      hitFlash: 0,
    });
  } else if (chosen === 'hopper') {
    state.hazards.push({
      id: state.nextEntityId++,
      kind: 'hopper',
      x: spawnX,
      y: GROUND_Y - 16,
      width: 18,
      height: 16,
      baseY: GROUND_Y - 16,
      vx: -26,
      vy: 0,
      phaseTimer: 0.35 + rng() * 0.4,
      telegraphTimer: 0,
      telegraphSounded: false,
      isActive: false,
      stomped: false,
      koTimer: 0,
      koVx: 0,
      koVy: 0,
      hitFlash: 0,
    });
  } else if (chosen === 'swooper') {
    // High duckable altitude OR mid jumpable altitude
    const highDuckLane = rng() < 0.65;
    const baseY = highDuckLane ? GROUND_Y - 31 : GROUND_Y - 56;
    state.hazards.push({
      id: state.nextEntityId++,
      kind: 'swooper',
      x: VIRTUAL_WIDTH + 54, // starts slightly off-screen while right-edge telegraph warns player
      y: baseY,
      width: 20,
      height: 14,
      baseY,
      vx: -54,
      vy: 0,
      phaseTimer: rng() * Math.PI * 2,
      telegraphTimer: 0.48,
      telegraphSounded: false,
      isActive: true,
      stomped: false,
      koTimer: 0,
      koVx: 0,
      koVy: 0,
      hitFlash: 0,
    });
  } else {
    // Telegraphed Flame Pillar: cycles idle -> telegraph -> flare
    state.hazards.push({
      id: state.nextEntityId++,
      kind: 'flame',
      x: spawnX,
      y: GROUND_Y - 14,
      width: 16,
      height: 14,
      baseY: GROUND_Y - 14,
      vx: 0,
      vy: 0,
      phaseTimer: 0.5 + rng() * 0.35,
      telegraphTimer: 0,
      telegraphSounded: false,
      isActive: false,
      stomped: false,
      koTimer: 0,
      koVx: 0,
      koVy: 0,
      hitFlash: 0,
    });
  }

  state.hazardCooldown = diff.minGapSec + rng() * (diff.maxGapSec - diff.minGapSec);
}

function spawnPlatformAndCoins(state: GameState, rng: () => number): void {
  const width = 64 + Math.floor(rng() * 3) * 16; // 64, 80, or 96
  const y = rng() < 0.55 ? GROUND_Y - 46 : GROUND_Y - 72;
  const x = VIRTUAL_WIDTH + 24;

  state.platforms.push({
    id: state.nextEntityId++,
    x,
    y,
    width,
    height: 10,
  });

  // Place a neat row of 3-5 coins atop the platform
  const coinCount = Math.min(5, Math.floor((width - 12) / 16));
  const startX = x + (width - coinCount * 16) * 0.5 + 3;
  for (let i = 0; i < coinCount; i++) {
    state.pickups.push({
      id: state.nextEntityId++,
      kind: 'coin',
      x: startX + i * 16,
      y: y - 16,
      width: 10,
      height: 10,
      bobOffset: i * 0.45,
      collected: false,
    });
  }

  state.platformCooldown = 4.2 + rng() * 2.8;
}

function spawnCoinArc(state: GameState, rng: () => number): void {
  const count = 4 + Math.floor(rng() * 3); // 4-6 coins
  const startX = VIRTUAL_WIDTH + 20;
  const isArc = rng() < 0.6;
  const baseY = isArc ? GROUND_Y - 26 : GROUND_Y - 18;
  for (let i = 0; i < count; i++) {
    const t = count > 1 ? i / (count - 1) : 0.5;
    const arcLift = isArc ? Math.sin(t * Math.PI) * 34 : 0;
    state.pickups.push({
      id: state.nextEntityId++,
      kind: 'coin',
      x: startX + i * 16,
      y: baseY - arcLift,
      width: 10,
      height: 10,
      bobOffset: i * 0.5,
      collected: false,
    });
  }
  state.coinArcCooldown = 2.6 + rng() * 2.0;
}

function spawnPowerup(state: GameState, rng: () => number): void {
  const p = state.player;
  const roll = rng();
  let kind: 'shield' | 'heart' | 'magnet' | 'doubleScore' | 'slowMo' | 'turboDash' = 'shield';
  if (p.hp < p.maxHp && roll < 0.28) {
    kind = 'heart';
  } else if (roll < 0.44 && !p.shieldActive) {
    kind = 'shield';
  } else if (roll < 0.6) {
    kind = 'magnet';
  } else if (roll < 0.74) {
    kind = 'doubleScore';
  } else if (roll < 0.87) {
    kind = 'slowMo';
  } else {
    kind = 'turboDash';
  }

  state.pickups.push({
    id: state.nextEntityId++,
    kind,
    x: VIRTUAL_WIDTH + 24,
    y: GROUND_Y - 54 - Math.floor(rng() * 24),
    width: 14,
    height: 14,
    bobOffset: 0,
    collected: false,
  });

  const baseCooldown = Math.max(6.5, 13.5 - state.powerupCooldownReduction);
  state.powerupCooldown = baseCooldown + rng() * 6.5;
}

function updateVisualTimers(state: GameState, dt: number): void {
  // Particles, hit flashes, camera shake, floating texts continue smoothly even during hitstop
  state.shakeTrauma = Math.max(0, state.shakeTrauma - dt * 2.2);

  if (state.player.hitFlashTimer > 0) {
    state.player.hitFlashTimer = Math.max(0, state.player.hitFlashTimer - dt);
  }
  if (state.player.hurtAnimationTimer > 0) {
    state.player.hurtAnimationTimer = Math.max(0, state.player.hurtAnimationTimer - dt);
  }

  // Smooth exponential damping for player squash & stretch
  const damp = 1 - Math.exp(-16 * dt);
  state.player.scaleX += (1 - state.player.scaleX) * damp;
  state.player.scaleY += (1 - state.player.scaleY) * damp;

  for (let i = 0; i < MAX_PARTICLES; i++) {
    const p = state.particles[i];
    if (!p.active) continue;
    p.life -= dt;
    if (p.life <= 0) {
      p.active = false;
      continue;
    }
    p.vy += p.gravity * dt;
    p.x += p.vx * dt;
    p.y += p.vy * dt;
  }

  for (let i = 0; i < MAX_FLOATING_TEXTS; i++) {
    const ft = state.floatingTexts[i];
    if (!ft.active) continue;
    ft.life -= dt;
    if (ft.life <= 0) {
      ft.active = false;
      continue;
    }
    ft.y += ft.vy * dt;
  }
}

export function stepSimulation(
  state: GameState,
  rawDt: number,
  input: InputFrame,
  rng: () => number = Math.random,
): void {
  const dt = Math.min(Math.max(rawDt, 0), 0.05);
  state.elapsed += dt;

  // Title screen live world parallax pan with trotting invulnerable runner
  if (state.phase === 'title') {
    const previewSpeed = BASE_SPEED * 0.48;
    state.worldScroll += previewSpeed * dt;
    state.player.runCycleDist += previewSpeed * dt;
    state.player.y = GROUND_Y;
    state.player.onGround = true;
    updateVisualTimers(state, dt);
    return;
  }

  if (state.phase === 'paused' || state.phase === 'gameover') {
    return;
  }

  updateVisualTimers(state, dt);

  // Hitstop freezes physics briefly on heavy impact while visuals tick
  if (state.hitstopTimer > 0) {
    state.hitstopTimer = Math.max(0, state.hitstopTimer - dt);
    return;
  }

  const p = state.player;

  // Handle KO tumble final beat before freezing Game Over snapshot
  if (p.koActive) {
    p.vy += GRAVITY * 0.9 * dt;
    p.y += p.vy * dt;
    p.koRot += 8.5 * dt;
    state.gameOverDelayTimer -= dt;
    if (state.gameOverDelayTimer <= 0) {
      const finalScore = Math.round(state.score);
      const isNewBest = finalScore > state.runStartBestScore;
      const bestScore = Math.max(state.bestScore, finalScore);
      state.bestScore = bestScore;
      state.finalRun = {
        score: finalScore,
        bestScore,
        distance: Math.round(state.distance),
        coins: state.coins,
        stomps: state.stomps,
        bossesDefeated: state.bossesDefeated,
        gameMode: state.gameMode,
        zoneName: BIOMES[state.zoneIndex].name,
        zoneIndex: state.zoneIndex,
        level: state.level,
        isNewBest,
      };
      state.phase = 'gameover';
    }
    return;
  }

  // Advance difficulty & biome
  state.runTime += dt;
  const diff = computeDifficulty(state.runTime, state.distance);
  const dashMult = p.turboDashTimer > 0 ? 1.45 : 1;
  const slowMult = p.slowMoTimer > 0 ? 0.65 : 1;
  const hardcoreSpeedMult = state.gameMode === 'hardcore' ? 1.25 : 1;
  const hardcoreScoreMult = state.gameMode === 'hardcore' ? 2 : 1;
  const owlScoreMult = state.petId === 'owl' ? 1.1 : 1;
  state.speed = diff.speed * slowMult * dashMult * hardcoreSpeedMult;
  const scrollDx = state.speed * dt;
  state.worldScroll += scrollDx;
  const distDelta = scrollDx * 0.085; // ~12-25m per second
  state.distance += distDelta;
  if (p.doubleScoreTimer > 0) {
    state.bonusScore += distDelta * 10;
  }
  if (state.gameMode === 'hardcore') {
    state.bonusScore += distDelta * 10;
  }
  if (state.petId === 'owl') {
    state.bonusScore += distDelta * 1;
  }

  // 3 Day/Night cycles (90s) -> Mini-Boss Encounter (defeating Boss advances Level & Landscape!)
  const completedCycles = Math.floor(state.runTime / DAY_NIGHT_CYCLE_SECONDS);
  if (completedCycles !== state.dayCycleCount) {
    state.dayCycleCount = completedCycles;
  }
  if (state.gameMode === 'bossRush') {
    if (!state.boss) {
      state.bossRushCooldown -= dt;
      if (state.bossRushCooldown <= 0) {
        spawnBoss(state, state.level + 1);
      }
    }
  } else {
    const targetLevel = 1 + Math.floor(completedCycles / CYCLES_PER_LEVEL);
    if (targetLevel > state.level && !state.boss) {
      spawnBoss(state, targetLevel);
    }
  }
  if (state.levelBannerTimer > 0) {
    state.levelBannerTimer = Math.max(0, state.levelBannerTimer - dt);
  }

  if (diff.zoneIndex !== state.zoneIndex) {
    state.zoneIndex = diff.zoneIndex;
    state.zoneBannerTimer = 2.4;
    state.audioQueue.push({ type: 'zoneUp' });
    spawnFloatingText(state, VIRTUAL_WIDTH * 0.5, 86, BIOMES[state.zoneIndex].name, '#FFF4A8');
  }
  if (state.zoneBannerTimer > 0) {
    state.zoneBannerTimer = Math.max(0, state.zoneBannerTimer - dt);
  }

  // Combo decay & power-up timers
  if (state.comboTimer > 0) {
    state.comboTimer = Math.max(0, state.comboTimer - dt);
    if (state.comboTimer === 0) {
      state.combo = 1;
    }
  }
  if (p.invulnTimer > 0) {
    p.invulnTimer = Math.max(0, p.invulnTimer - dt);
  }
  if (p.magnetTimer > 0) {
    p.magnetTimer = Math.max(0, p.magnetTimer - dt);
  }
  if (p.doubleScoreTimer > 0) {
    p.doubleScoreTimer = Math.max(0, p.doubleScoreTimer - dt);
  }
  if (p.slowMoTimer > 0) {
    p.slowMoTimer = Math.max(0, p.slowMoTimer - dt);
  }
  if (p.turboDashTimer > 0) {
    p.turboDashTimer = Math.max(0, p.turboDashTimer - dt);
    if (p.turboDashTimer === 0) {
      // Brief grace invulnerability when Turbo Dash ends
      p.invulnTimer = Math.max(p.invulnTimer, 0.75);
    }
  }
  if (state.gadgets.includes('autoShield') && !p.shieldActive) {
    p.autoShieldTimer -= dt;
    if (p.autoShieldTimer <= 0) {
      p.autoShieldTimer = 45;
      p.shieldActive = true;
      spawnParticles(state, p.x + p.width * 0.5, p.y - p.height * 0.5, 14, ['#78E8FF', '#FFFFFF'], 40, 110, 80, rng, -25);
      spawnFloatingText(state, p.x + 8, p.y - p.height - 6, 'AUTO SHIELD!', '#78E8FF');
      state.audioQueue.push({ type: 'powerup', kind: 'shield' });
    }
  }
  if (state.trailId !== 'classic' || p.turboDashTimer > 0) {
    p.trailTimer -= dt;
    if (p.trailTimer <= 0) {
      p.trailTimer = p.turboDashTimer > 0 ? 0.03 : 0.065;
      spawnParticles(
        state,
        p.x + 2,
        p.y - p.height * 0.45,
        p.turboDashTimer > 0 ? 2 : 1,
        p.turboDashTimer > 0 ? ['#FFFFFF', '#FFE848', '#FF9820'] : getTrailColors(state.trailId),
        10,
        28,
        25,
        rng,
        -6,
      );
    }
  }

  // Jump buffering & coyote timers
  if (input.jumpJustPressed) {
    p.jumpBufferTimer = JUMP_BUFFER_TIME;
  } else if (p.jumpBufferTimer > 0) {
    p.jumpBufferTimer = Math.max(0, p.jumpBufferTimer - dt);
  }

  if (p.onGround) {
    p.coyoteTimer = COYOTE_TIME;
  } else if (p.coyoteTimer > 0) {
    p.coyoteTimer = Math.max(0, p.coyoteTimer - dt);
  }

  // Consume buffered jump when legal
  if (p.jumpBufferTimer > 0) {
    if (p.onGround || p.coyoteTimer > 0 || p.jumpsRemaining > 0) {
      executeJump(state, rng);
    }
  }

  // Variable jump height cut on early release
  if (input.jumpJustReleased && p.vy < -60) {
    p.vy *= JUMP_CUT_MULTIPLIER;
  }

  // Ducking on ground / Fast-fall in air
  const wasDucking = p.isDucking;
  if (p.onGround) {
    p.isDucking = input.duckHeld;
    p.height = p.isDucking ? p.duckHeight : p.standHeight;
    if (p.isDucking && !wasDucking) {
      p.scaleX = 1.18;
      p.scaleY = 0.82;
      state.audioQueue.push({ type: 'slide' });
    }
    if (p.isDucking) {
      p.slideDustTimer -= dt;
      if (p.slideDustTimer <= 0) {
        p.slideDustTimer = 0.08;
        spawnParticles(state, p.x + 2, p.y - 1, 2, ['#F4EFE0', '#D0C8B8'], 15, 45, 90, rng, -18);
      }
    }
  } else {
    p.isDucking = false;
    p.height = p.standHeight;
  }

  // Vertical integration + one-way platform landing
  const prevY = p.y;
  const activeGravity = !p.onGround && input.duckHeld ? FAST_FALL_GRAVITY : GRAVITY;
  p.vy += activeGravity * dt;
  p.y += p.vy * dt;

  let landedThisFrame = false;
  const wasOnGround = p.onGround;
  p.onGround = false;
  p.onPlatform = false;

  // Check one-way elevated platforms (only when descending and not pressing duck to drop through)
  if (p.vy >= 0 && !input.duckHeld) {
    for (let i = 0; i < state.platforms.length; i++) {
      const plat = state.platforms[i];
      const horizontalOverlap = p.x + p.width - 3 > plat.x && p.x + 3 < plat.x + plat.width;
      if (horizontalOverlap && prevY <= plat.y + 4 && p.y >= plat.y) {
        p.y = plat.y;
        p.vy = 0;
        p.onGround = true;
        p.onPlatform = true;
        p.jumpsRemaining = p.maxJumps;
        landedThisFrame = !wasOnGround;
        break;
      }
    }
  }

  // Ground collision
  if (!p.onGround && p.y >= GROUND_Y) {
    p.y = GROUND_Y;
    p.vy = 0;
    p.onGround = true;
    p.jumpsRemaining = p.maxJumps;
    landedThisFrame = !wasOnGround;
  }

  if (landedThisFrame) {
    p.scaleX = 1.22;
    p.scaleY = 0.78;
    spawnParticles(state, p.x + p.width * 0.5, p.y - 1, 5, ['#E8E0D0', '#A89880'], 20, 62, 180, rng, -20);
    state.audioQueue.push({ type: 'land' });
  }

  if (p.onGround && !p.isDucking) {
    p.runCycleDist += scrollDx;
  }

  // Spawners (pause normal hazards during Boss fights & Boss Rush)
  if (!state.boss && state.gameMode !== 'bossRush') {
    state.hazardCooldown -= dt;
    if (state.hazardCooldown <= 0) {
      spawnHazardPattern(state, rng);
    }
  }

  state.platformCooldown -= dt;
  if (state.platformCooldown <= 0) {
    spawnPlatformAndCoins(state, rng);
  }

  state.coinArcCooldown -= dt;
  if (state.coinArcCooldown <= 0) {
    spawnCoinArc(state, rng);
  }

  state.powerupCooldown -= dt;
  if (state.powerupCooldown <= 0) {
    spawnPowerup(state, rng);
  }

  // Mini Dragon pet companion: zaps 1 visible hazard ahead every 20s
  if (state.petId === 'dragon') {
    state.petActionTimer -= dt;
    if (state.petActionTimer <= 0) {
      let targetIdx = -1;
      let bestX = Infinity;
      for (let i = 0; i < state.hazards.length; i++) {
        const h = state.hazards[i];
        if (!h.stomped && h.x > p.x + 10 && h.x < VIRTUAL_WIDTH - 4 && h.x < bestX) {
          bestX = h.x;
          targetIdx = i;
        }
      }
      if (targetIdx >= 0) {
        const victim = state.hazards[targetIdx];
        const pts = Math.round(75 * (p.doubleScoreTimer > 0 ? 2 : 1) * hardcoreScoreMult * owlScoreMult);
        state.bonusScore += pts;
        state.stomps += 1;
        spawnParticles(
          state,
          victim.x + victim.width * 0.5,
          victim.y + victim.height * 0.5,
          16,
          ['#FF6828', '#FFE048', '#FFFFFF'],
          55,
          155,
          190,
          rng,
          -45,
        );
        spawnFloatingText(state, victim.x, victim.y - 8, `+${pts} ZAP!`, '#FF9838');
        state.audioQueue.push({ type: 'stomp' });
        state.hazards.splice(targetIdx, 1);
        state.petActionTimer = 20;
      } else {
        state.petActionTimer = 0.4;
      }
    }
  }

  // Update platforms
  for (let i = state.platforms.length - 1; i >= 0; i--) {
    const plat = state.platforms[i];
    plat.x -= scrollDx;
    if (plat.x + plat.width < -24) {
      state.platforms.splice(i, 1);
    }
  }

  // Update hazards + collisions
  const playerBox = getPlayerHitbox(p);

  // Capture pre-collision vertical velocity so multi-enemy stomps on the same frame all register cleanly
  const initialVy = p.vy;

  // Update active Mini-Boss encounter
  if (state.boss) {
    const boss = state.boss;
    if (boss.hitFlash > 0) boss.hitFlash = Math.max(0, boss.hitFlash - dt);
    if (boss.stompCooldown > 0) boss.stompCooldown = Math.max(0, boss.stompCooldown - dt);

    if (boss.phaseTimer === 0 && boss.x > boss.targetX) {
      boss.x = Math.max(boss.targetX, boss.x - 115 * dt);
      if (boss.x <= boss.targetX) {
        boss.phaseTimer = 0.01;
      }
    } else {
      boss.phaseTimer += dt;
      const cycle = boss.phaseTimer % 4.6;

      if (boss.kind === 'drone' || boss.kind === 'wyrm') {
        boss.y = boss.baseY + Math.sin(state.elapsed * 3.1) * 16;
      }

      if (cycle >= 2.2 && cycle < 3.6) {
        // Charge across PLAYER_X so player can jump & stomp the boss!
        boss.x = Math.max(PLAYER_X - 16, boss.x - 235 * dt);
      } else if (cycle >= 3.6) {
        // Recoil smoothly back to arena perch
        boss.x = Math.min(boss.targetX, boss.x + 225 * dt);
      }

      boss.attackTimer -= dt;
      if (boss.attackTimer <= 0 && cycle < 2.0) {
        boss.attackTimer = Math.max(1.6, 2.5 - state.level * 0.1);
        const isAirBoss = boss.kind === 'drone' || boss.kind === 'wyrm';
        if (isAirBoss) {
          const laneY = rng() < 0.55 ? GROUND_Y - 31 : GROUND_Y - 54;
          state.hazards.push({
            id: state.nextEntityId++,
            kind: 'swooper',
            x: boss.x - 8,
            y: laneY,
            width: 20,
            height: 14,
            baseY: laneY,
            vx: -48,
            vy: 0,
            phaseTimer: rng() * Math.PI * 2,
            telegraphTimer: 0.25,
            telegraphSounded: false,
            isActive: true,
            stomped: false,
            koTimer: 0,
            koVx: 0,
            koVy: 0,
            hitFlash: 0,
          });
        } else {
          state.hazards.push({
            id: state.nextEntityId++,
            kind: 'hopper',
            x: boss.x - 10,
            y: GROUND_Y - 16,
            width: 18,
            height: 16,
            baseY: GROUND_Y - 16,
            vx: -34,
            vy: 0,
            phaseTimer: 0.3,
            telegraphTimer: 0,
            telegraphSounded: false,
            isActive: false,
            stomped: false,
            koTimer: 0,
            koVx: 0,
            koVy: 0,
            hitFlash: 0,
          });
        }
      }
    }

    const bossBox: Rect = {
      x: boss.x + 2,
      y: boss.y + 2,
      width: boss.width - 4,
      height: boss.height - 4,
    };

    if (checkAABB(playerBox, bossBox)) {
      const playerFallingFromAbove = initialVy > 0 && prevY <= boss.y + boss.height * 0.78;
      if (boss.stompCooldown <= 0 && (p.turboDashTimer > 0 || playerFallingFromAbove)) {
        boss.hp -= 1;
        boss.hitFlash = 0.2;
        boss.stompCooldown = 0.65;
        // Recoil boss back toward right side after taking a hit
        boss.phaseTimer = 3.6;
        p.vy = STOMP_BOUNCE_VELOCITY * 1.1;
        p.onGround = false;
        p.jumpsRemaining = p.maxJumps - 1;
        p.scaleX = 1.24;
        p.scaleY = 0.78;
        state.hitstopTimer = 0.05;
        state.shakeTrauma = Math.min(1, state.shakeTrauma + 0.45);
        state.audioQueue.push({ type: 'stomp' });
        spawnParticles(
          state,
          boss.x + boss.width * 0.5,
          boss.y + 8,
          16,
          ['#FFE05C', '#FF6848', '#FFFFFF'],
          55,
          155,
          230,
          rng,
          -55,
        );

        if (boss.hp <= 0) {
          state.bossesDefeated += 1;
          const bossScoreMult = (p.doubleScoreTimer > 0 ? 2 : 1) * hardcoreScoreMult * owlScoreMult;
          const bossPts = Math.round(500 * bossScoreMult);
          state.bonusScore += bossPts + LEVEL_BONUS_POINTS;
          state.coins += 25;
          state.level = Math.max(state.level + 1, boss.pendingLevel);
          state.levelBannerTimer = 2.8;
          const dropKind = p.hp < p.maxHp ? 'heart' : 'shield';
          state.pickups.push({
            id: state.nextEntityId++,
            kind: dropKind,
            x: Math.min(VIRTUAL_WIDTH - 48, boss.x),
            y: GROUND_Y - 42,
            width: 14,
            height: 14,
            bobOffset: 0,
            collected: false,
          });
          spawnParticles(
            state,
            boss.x + boss.width * 0.5,
            boss.y + boss.height * 0.5,
            28,
            ['#FFE05C', '#78F0A8', '#FF5874', '#FFFFFF'],
            70,
            195,
            240,
            rng,
            -75,
          );
          const levelTitle = LEVEL_NAMES[(state.level - 1) % LEVEL_NAMES.length];
          spawnFloatingText(
            state,
            VIRTUAL_WIDTH * 0.5,
            62,
            `BOSS KO! +${bossPts} +25C`,
            '#FFE05C',
          );
          spawnFloatingText(
            state,
            VIRTUAL_WIDTH * 0.5,
            78,
            `LV ${state.level} ${levelTitle}`,
            '#78F0A8',
          );
          state.audioQueue.push({ type: 'zoneUp' });
          state.boss = null;
          if (state.gameMode === 'bossRush') {
            state.bossRushCooldown = 4.0;
          }
        } else {
          spawnFloatingText(
            state,
            boss.x + 6,
            boss.y - 8,
            `HIT! ${boss.hp}/${boss.maxHp}`,
            '#FFE05C',
          );
        }
      } else if (boss.stompCooldown <= 0) {
        applyDamageToPlayer(state, rng);
      }
    }
  }

  for (let i = state.hazards.length - 1; i >= 0; i--) {
    const h = state.hazards[i];
    if (!h) continue;
    if (h.hitFlash > 0) h.hitFlash = Math.max(0, h.hitFlash - dt);

    if (h.stomped) {
      // KO topple arc instead of instant pop-out
      h.koTimer -= dt;
      h.x += h.koVx * dt - scrollDx * 0.35;
      h.vy += GRAVITY * 0.85 * dt;
      h.y += h.vy * dt;
      if (h.koTimer <= 0 || h.y > GROUND_Y + 40) {
        state.hazards.splice(i, 1);
      }
      continue;
    }

    if (h.kind === 'spike') {
      h.x -= scrollDx;
    } else if (h.kind === 'hopper') {
      h.x -= scrollDx - h.vx * dt;
      if (h.isActive) {
        h.vy += GRAVITY * 0.82 * dt;
        h.y += h.vy * dt;
        if (h.y >= h.baseY) {
          h.y = h.baseY;
          h.vy = 0;
          h.isActive = false;
          h.phaseTimer = 0.72;
        }
      } else if (h.telegraphTimer > 0) {
        h.telegraphTimer -= dt;
        if (h.telegraphTimer <= 0) {
          h.isActive = true;
          h.vy = -225;
        }
      } else {
        h.phaseTimer -= dt;
        if (h.phaseTimer <= 0 && h.x < VIRTUAL_WIDTH - 20) {
          // 0.35s crouch windup telegraph before hopping
          h.telegraphTimer = 0.35;
          if (!h.telegraphSounded) {
            h.telegraphSounded = true;
            state.audioQueue.push({ type: 'telegraph' });
          }
        }
      }
    } else if (h.kind === 'swooper') {
      if (h.telegraphTimer > 0) {
        h.telegraphTimer -= dt;
        if (!h.telegraphSounded) {
          h.telegraphSounded = true;
          state.audioQueue.push({ type: 'telegraph' });
        }
      } else {
        h.x -= scrollDx - h.vx * dt;
        h.phaseTimer += dt * 5.2;
        h.y = h.baseY + Math.sin(h.phaseTimer) * 7;
      }
    } else if (h.kind === 'flame') {
      h.x -= scrollDx;
      h.phaseTimer -= dt;
      if (!h.isActive && h.telegraphTimer <= 0 && h.phaseTimer <= 0) {
        h.telegraphTimer = 0.45;
        if (!h.telegraphSounded && h.x < VIRTUAL_WIDTH) {
          h.telegraphSounded = true;
          state.audioQueue.push({ type: 'telegraph' });
        }
      } else if (h.telegraphTimer > 0) {
        h.telegraphTimer -= dt;
        if (h.telegraphTimer <= 0) {
          h.isActive = true;
          h.height = 36;
          h.y = GROUND_Y - 36;
          h.phaseTimer = 0.85;
        }
      } else if (h.isActive && h.phaseTimer <= 0) {
        h.isActive = false;
        h.height = 14;
        h.y = GROUND_Y - 14;
        h.phaseTimer = 0.9;
        h.telegraphSounded = false;
      }
    }

    if (h.x + h.width < -32) {
      state.hazards.splice(i, 1);
      continue;
    }

    // Swoopers still in warning telegraph are not yet physically active
    if (h.kind === 'swooper' && h.telegraphTimer > 0) {
      continue;
    }

    // Inset hazard hitbox slightly for fair retro precision
    const hazardBox: Rect = {
      x: h.x + 2,
      y: h.y + 2,
      width: Math.max(4, h.width - 4),
      height: Math.max(4, h.height - 3),
    };

    if (checkAABB(playerBox, hazardBox)) {
      const stompable = h.kind === 'hopper' || h.kind === 'swooper';
      const playerFallingFromAbove = initialVy > 0 && prevY <= h.y + h.height * 0.72;

      if (p.turboDashTimer > 0) {
        // Invincible Turbo Dash smashes straight through any hazard!
        const scoreMult = (p.doubleScoreTimer > 0 ? 2 : 1) * hardcoreScoreMult * owlScoreMult;
        const pts = Math.round(100 * scoreMult);
        state.bonusScore += pts;
        state.stomps += 1;
        state.shakeTrauma = Math.min(1, state.shakeTrauma + 0.3);
        spawnParticles(state, h.x + h.width * 0.5, h.y + h.height * 0.5, 16, ['#FFFFFF', '#FFE848', '#FF9820'], 65, 175, 220, rng, -55);
        spawnFloatingText(state, h.x + 4, h.y - 6, `SMASH +${pts}`, '#FFE848');
        state.audioQueue.push({ type: 'stomp' });
        state.hazards.splice(i, 1);
      } else if (stompable && playerFallingFromAbove) {
        // Clean Mario/Sonic-style stomp kill!
        h.stomped = true;
        h.koTimer = 0.42;
        h.koVx = 65;
        h.vy = -175;
        h.hitFlash = 0.08;
        p.vy = STOMP_BOUNCE_VELOCITY;
        p.onGround = false;
        p.jumpsRemaining = p.maxJumps - 1;
        p.scaleX = 1.24;
        p.scaleY = 0.78;
        state.stomps += 1;
        state.combo = Math.min(5, state.combo + 1);
        state.comboTimer = COMBO_DURATION;
        const stompBase =
          state.characterId === 'stride'
            ? 188
            : state.characterId === 'custom'
              ? 173
              : 150;
        const scoreMult = (p.doubleScoreTimer > 0 ? 2 : 1) * hardcoreScoreMult * owlScoreMult;
        const pts = Math.round(stompBase * state.combo * scoreMult);
        state.bonusScore += pts;
        state.hitstopTimer = 0.04;
        state.shakeTrauma = Math.min(1, state.shakeTrauma + 0.35);
        spawnParticles(state, h.x + h.width * 0.5, h.y + 4, 14, ['#FFF4A8', '#FF9838', '#FFFFFF'], 45, 130, 240, rng, -45);
        spawnFloatingText(state, h.x + 4, h.y - 6, `STOMP +${pts}`, '#FFF4A8');
        state.audioQueue.push({ type: 'stomp' });
      } else {
        if (applyDamageToPlayer(state, rng)) {
          // Second Wind replaces state.hazards and lethal hits set koActive — exit loop safely!
          break;
        }
      }
    }
  }

  // Stop processing pickups on the frame the player is KO'd
  if (p.koActive || state.phase !== 'playing') {
    return;
  }

  // Update pickups (coins & powerups)
  for (let i = state.pickups.length - 1; i >= 0; i--) {
    const item = state.pickups[i];
    item.x -= scrollDx;
    item.bobOffset += dt * 4.5;

    // Coin magnet attraction (Magnet powerup > Pixel Fox pet > Mega Magnet gadget > Lily passive aura)
    const pullRadius =
      p.magnetTimer > 0
        ? 115
        : state.petId === 'fox'
          ? 95
          : state.gadgets.includes('megaMagnet')
            ? 68
            : state.characterId === 'lily'
              ? 48
              : 0;
    if (item.kind === 'coin' && pullRadius > 0) {
      const cx = p.x + p.width * 0.5;
      const cy = p.y - p.height * 0.5;
      const dx = cx - (item.x + item.width * 0.5);
      const dy = cy - (item.y + item.height * 0.5);
      const distSq = dx * dx + dy * dy;
      if (distSq < pullRadius * pullRadius && distSq > 1) {
        const dist = Math.sqrt(distSq);
        const pull = (p.magnetTimer > 0 ? 280 : state.petId === 'fox' ? 235 : 195) * dt;
        item.x += (dx / dist) * pull;
        item.y += (dy / dist) * pull;
      }
    }

    if (item.x + item.width < -20) {
      state.pickups.splice(i, 1);
      continue;
    }

    const itemBox: Rect = {
      x: item.x - 2,
      y: item.y - 2,
      width: item.width + 4,
      height: item.height + 4,
    };

    if (checkAABB(playerBox, itemBox)) {
      const scoreMult = (p.doubleScoreTimer > 0 ? 2 : 1) * hardcoreScoreMult * owlScoreMult;
      if (item.kind === 'coin') {
        state.coins += 1;
        state.combo = Math.min(5, state.comboTimer > 0 ? state.combo + (state.coins % 3 === 0 ? 1 : 0) : 1);
        state.comboTimer = COMBO_DURATION;
        const customMult = state.characterId === 'custom' ? 1.15 : 1;
        const gemBonus =
          state.gadgets.includes('goldenTouch') && state.coins % 5 === 0
            ? Math.round(50 * scoreMult)
            : 0;
        const pts =
          Math.round((50 + state.coinBonusPoints) * state.combo * scoreMult * customMult) +
          gemBonus;
        state.bonusScore += pts;
        spawnParticles(
          state,
          item.x + 5,
          item.y + 5,
          gemBonus > 0 ? 10 : 6,
          gemBonus > 0 ? ['#78F0FF', '#FFE848', '#FFFFFF'] : ['#FFE048', '#FFF8C4', '#F09820'],
          24,
          75,
          110,
          rng,
          -25,
        );
        spawnFloatingText(
          state,
          item.x,
          item.y - 4,
          gemBonus > 0 ? `GEM +${pts}` : `+${pts}`,
          gemBonus > 0 ? '#78F0FF' : '#FFE868',
        );
        state.audioQueue.push({ type: 'coin', combo: state.combo });
      } else if (item.kind === 'shield') {
        p.shieldActive = true;
        state.bonusScore += Math.round(100 * scoreMult);
        spawnParticles(state, item.x + 7, item.y + 7, 12, ['#78E8FF', '#FFFFFF', '#48A0FF'], 35, 105, 80, rng, -20);
        spawnFloatingText(state, item.x, item.y - 6, 'STAR SHIELD!', '#78E8FF');
        state.audioQueue.push({ type: 'powerup', kind: 'shield' });
      } else if (item.kind === 'heart') {
        if (p.hp < p.maxHp) {
          p.hp += 1;
          spawnFloatingText(state, item.x, item.y - 6, '+1 HEART', '#FF7088');
        } else {
          const heartPts = Math.round(250 * scoreMult);
          state.bonusScore += heartPts;
          spawnFloatingText(state, item.x, item.y - 6, `+${heartPts}`, '#FF7088');
        }
        spawnParticles(state, item.x + 7, item.y + 7, 12, ['#FF5874', '#FFB8C4', '#FFFFFF'], 35, 105, 90, rng, -25);
        state.audioQueue.push({ type: 'powerup', kind: 'heart' });
      } else if (item.kind === 'magnet') {
        p.magnetTimer = Math.max(p.magnetTimer, 6.5 + state.magnetDurationBonus);
        state.bonusScore += Math.round(100 * scoreMult);
        spawnParticles(state, item.x + 7, item.y + 7, 12, ['#FF90D0', '#FFE868', '#FFFFFF'], 35, 105, 80, rng, -20);
        spawnFloatingText(state, item.x, item.y - 6, 'COIN MAGNET!', '#FF98E0');
        state.audioQueue.push({ type: 'powerup', kind: 'magnet' });
      } else if (item.kind === 'doubleScore') {
        p.doubleScoreTimer = Math.max(p.doubleScoreTimer, 7.0);
        state.bonusScore += Math.round(100 * scoreMult);
        spawnParticles(state, item.x + 7, item.y + 7, 14, ['#FFE048', '#FFFDF0', '#F08820'], 40, 115, 80, rng, -25);
        spawnFloatingText(state, item.x, item.y - 6, '2X SCORE!', '#FFE05C');
        state.audioQueue.push({ type: 'powerup', kind: 'doubleScore' });
      } else if (item.kind === 'slowMo') {
        p.slowMoTimer = Math.max(p.slowMoTimer, 4.5);
        state.bonusScore += Math.round(100 * scoreMult);
        spawnParticles(state, item.x + 7, item.y + 7, 14, ['#78F0FF', '#FFFFFF', '#3888E8'], 35, 105, 60, rng, -20);
        spawnFloatingText(state, item.x, item.y - 6, 'SLOW-MO!', '#78F0FF');
        state.audioQueue.push({ type: 'powerup', kind: 'slowMo' });
      } else if (item.kind === 'turboDash') {
        p.turboDashTimer = Math.max(p.turboDashTimer, 4.0);
        state.bonusScore += Math.round(150 * scoreMult);
        spawnParticles(state, item.x + 7, item.y + 7, 18, ['#FFFFFF', '#FFE848', '#FF9820'], 50, 140, 80, rng, -30);
        spawnFloatingText(state, item.x, item.y - 6, 'TURBO DASH!', '#FFE848');
        state.audioQueue.push({ type: 'powerup', kind: 'turboDash' });
      }

      state.pickups.splice(i, 1);
    }
  }

  // Update total score
  state.score = Math.floor(state.distance * 10) + state.bonusScore;
  if (state.score > state.bestScore) {
    state.bestScore = state.score;
  }
}

export function extractHudSnapshot(state: GameState): HudSnapshot {
  return {
    phase: state.phase,
    gameMode: state.gameMode,
    characterId: state.characterId,
    score: Math.round(state.score),
    bestScore: Math.round(state.bestScore),
    distance: Math.round(state.distance),
    coins: state.coins,
    combo: state.combo,
    hp: state.player.hp,
    maxHp: state.player.maxHp,
    damageFlash: state.player.hurtAnimationTimer > 0,
    shieldActive: state.player.shieldActive,
    magnetSeconds: Math.ceil(state.player.magnetTimer),
    doubleScoreSeconds: Math.ceil(state.player.doubleScoreTimer),
    slowMoSeconds: Math.ceil(state.player.slowMoTimer),
    turboDashSeconds: Math.ceil(state.player.turboDashTimer),
    zoneName: BIOMES[state.zoneIndex].name,
    zoneIndex: state.zoneIndex,
    zoneBannerTimer: state.zoneBannerTimer,
    level: state.level,
    dayCycleCount: state.dayCycleCount,
    levelBannerTimer: state.levelBannerTimer,
    bossName: state.boss ? state.boss.name : null,
    bossHp: state.boss ? state.boss.hp : 0,
    bossMaxHp: state.boss ? state.boss.maxHp : 0,
    finalRun: state.finalRun,
  };
}
