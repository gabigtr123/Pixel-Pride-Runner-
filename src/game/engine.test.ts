import test from 'node:test';
import assert from 'node:assert/strict';
import {
  applyDamageToPlayer,
  checkAABB,
  computeDifficulty,
  createInitialState,
  getPlayerHitbox,
  returnToTitle,
  startNewRun,
  stepSimulation,
} from './engine';
import { GROUND_Y, PLAYER_DUCK_HEIGHT, PLAYER_STAND_HEIGHT, PLAYER_X } from './constants';
import {
  buildCustomSpriteSet,
  createDefaultCareerStats,
  createDefaultCustomConfig,
  getNewlyUnlockedCosmetics,
  HAIR_OPTIONS,
  isOptionUnlocked,
  recordCareerRun,
} from './creator';
import {
  applyRunToDailyMissions,
  claimDailyBonus,
  computeActiveRunBonus,
  generateDailyMissions,
  getStreakPerks,
  syncDailyStateForDate,
} from './daily';
import { getRenderViewport } from './renderer';
import {
  addCoinsToBank,
  buyKnightCharacter,
  buyOrEquipTrail,
  buyUpgrade,
  createDefaultShopState,
  recordLeaderboardRun,
} from './shop';
import type { InputFrame } from './types';

const IDLE_INPUT: InputFrame = {
  jumpHeld: false,
  jumpJustPressed: false,
  jumpJustReleased: false,
  duckHeld: false,
  duckJustPressed: false,
};

function makeSeededRng(seed = 42): () => number {
  let s = seed >>> 0;
  return () => {
    s = (s * 1664525 + 1013904223) >>> 0;
    return s / 0xffffffff;
  };
}

test('player survives the opening 2.4 seconds without input', () => {
  const rng = makeSeededRng(7);
  const state = startNewRun(createInitialState(0, 'title'));
  assert.equal(state.phase, 'playing');

  for (let i = 0; i < 144; i++) {
    stepSimulation(state, 1 / 60, IDLE_INPUT, rng);
  }

  assert.equal(state.phase, 'playing');
  assert.equal(state.player.hp, 3);
  assert.ok(state.distance > 20, 'runner should advance distance');
  assert.ok(state.score > 200, 'score should accumulate with distance');
});

test('jump, variable jump cut, and double jump work cleanly', () => {
  const rng = makeSeededRng(11);
  const state = startNewRun(createInitialState(0, 'title'));

  // Jump takeoff
  stepSimulation(
    state,
    1 / 60,
    { ...IDLE_INPUT, jumpHeld: true, jumpJustPressed: true },
    rng,
  );
  assert.equal(state.player.onGround, false);
  assert.equal(state.player.jumpsRemaining, 1);
  assert.ok(state.player.vy < -250);

  // Release jump early -> variable jump cut halves upward velocity
  const vyBeforeCut = state.player.vy;
  stepSimulation(
    state,
    1 / 60,
    { ...IDLE_INPUT, jumpJustReleased: true },
    rng,
  );
  assert.ok(state.player.vy > vyBeforeCut * 0.6, 'early release cuts upward velocity');

  // Double jump mid-air
  stepSimulation(
    state,
    1 / 60,
    { ...IDLE_INPUT, jumpHeld: true, jumpJustPressed: true },
    rng,
  );
  assert.equal(state.player.jumpsRemaining, 0);
  assert.ok(state.player.vy < -220, 'double jump launches upward');
});

test('jump buffering fires immediately upon landing', () => {
  const rng = makeSeededRng(19);
  const state = startNewRun(createInitialState(0, 'title'));

  // Position player slightly above ground, descending with 0 jumps left
  state.player.y = GROUND_Y - 5;
  state.player.onGround = false;
  state.player.coyoteTimer = 0;
  state.player.jumpsRemaining = 0;
  state.player.vy = 240;

  // Press jump just before touching ground
  stepSimulation(
    state,
    1 / 60,
    { ...IDLE_INPUT, jumpHeld: true, jumpJustPressed: true },
    rng,
  );
  // Next frame lands & consumes buffered jump
  stepSimulation(state, 1 / 60, { ...IDLE_INPUT, jumpHeld: true }, rng);
  stepSimulation(state, 1 / 60, { ...IDLE_INPUT, jumpHeld: true }, rng);

  assert.ok(state.player.vy < -150, 'buffered jump should fire automatically on touchdown');
});

test('ducking shrinks player hitbox height and fast-fall accelerates descent', () => {
  const rng = makeSeededRng(23);
  const state = startNewRun(createInitialState(0, 'title'));
  const standingBox = getPlayerHitbox(state.player);
  assert.equal(state.player.height, PLAYER_STAND_HEIGHT);

  stepSimulation(state, 1 / 60, { ...IDLE_INPUT, duckHeld: true, duckJustPressed: true }, rng);
  const duckingBox = getPlayerHitbox(state.player);
  assert.equal(state.player.isDucking, true);
  assert.equal(state.player.height, PLAYER_DUCK_HEIGHT);
  assert.ok(duckingBox.height < standingBox.height, 'ducking hitbox must be shorter');

  // Air fast-fall check
  const stateA = startNewRun(createInitialState(0, 'title'));
  const stateB = startNewRun(createInitialState(0, 'title'));
  stateA.player.y = GROUND_Y - 80;
  stateA.player.onGround = false;
  stateB.player.y = GROUND_Y - 80;
  stateB.player.onGround = false;

  stepSimulation(stateA, 0.05, IDLE_INPUT, rng);
  stepSimulation(stateB, 0.05, { ...IDLE_INPUT, duckHeld: true }, rng);
  assert.ok(stateB.player.vy > stateA.player.vy, 'holding duck in mid-air accelerates downward velocity');
});

test('stomping a hopper grants bonus score, bounce, and KO topple without hurting player', () => {
  const rng = makeSeededRng(31);
  const state = startNewRun(createInitialState(0, 'title'));
  state.hazardCooldown = 99;

  // Place hopper directly beneath falling player
  state.hazards.push({
    id: 99,
    kind: 'hopper',
    x: state.player.x,
    y: GROUND_Y - 16,
    width: 18,
    height: 16,
    baseY: GROUND_Y - 16,
    vx: 0,
    vy: 0,
    phaseTimer: 10,
    telegraphTimer: 0,
    telegraphSounded: true,
    isActive: false,
    stomped: false,
    koTimer: 0,
    koVx: 0,
    koVy: 0,
    hitFlash: 0,
  });

  state.player.y = GROUND_Y - 17;
  state.player.onGround = false;
  state.player.vy = 180;

  stepSimulation(state, 1 / 60, IDLE_INPUT, rng);
  stepSimulation(state, 1 / 60, IDLE_INPUT, rng);

  assert.equal(state.player.hp, 3, 'player should take no damage on stomp');
  assert.equal(state.stomps, 1);
  assert.ok(state.bonusScore >= 150, 'stomp should award bonus score');
  assert.ok(state.player.vy < -200, 'player should bounce up after stomp');
  assert.equal(state.hazards[0].stomped, true, 'enemy enters KO topple rather than disappearing');
});

test('shield absorbs a hit and loss is reachable at 0 HP leading to gameover', () => {
  const rng = makeSeededRng(41);
  const state = startNewRun(createInitialState(0, 'title'));
  state.player.shieldActive = true;

  // First hit consumed by shield
  const hurt1 = applyDamageToPlayer(state, rng);
  assert.equal(hurt1, false);
  assert.equal(state.player.shieldActive, false);
  assert.equal(state.player.hp, 3);

  // Deplete all 3 hearts cleanly
  for (let hit = 0; hit < 3; hit++) {
    state.player.invulnTimer = 0;
    state.hitstopTimer = 0;
    const damaged = applyDamageToPlayer(state, rng);
    assert.equal(damaged, true);
  }

  assert.equal(state.player.hp, 0);
  assert.equal(state.player.koActive, true);

  // Advance past hitstop + 0.55s KO beat -> gameover with immutable snapshot
  for (let i = 0; i < 60; i++) {
    stepSimulation(state, 1 / 60, IDLE_INPUT, rng);
  }

  assert.equal(state.phase, 'gameover');
  assert.ok(state.finalRun !== null);
  assert.equal(state.finalRun.score, Math.round(state.score));
});

test('difficulty ramps speed, unlocks enemy archetypes, and transitions biomes', () => {
  const early = computeDifficulty(1, 20);
  const mid = computeDifficulty(20, 450);
  const late = computeDifficulty(80, 1300);

  assert.ok(mid.speed > early.speed);
  assert.ok(late.speed > mid.speed);
  assert.equal(early.allowSwooper, false);
  assert.equal(mid.allowSwooper, true);
  assert.equal(mid.allowFlame, true);
  assert.equal(early.zoneIndex, 0);
  assert.equal(mid.zoneIndex, 1);
  assert.equal(late.zoneIndex, 3);
});

test('checkAABB detects overlaps accurately', () => {
  assert.equal(
    checkAABB({ x: 0, y: 0, width: 10, height: 10 }, { x: 9, y: 9, width: 10, height: 10 }),
    true,
  );
  assert.equal(
    checkAABB({ x: 0, y: 0, width: 10, height: 10 }, { x: 10, y: 0, width: 10, height: 10 }),
    false,
  );
});

test('finalRun.isNewBest is true when player beats previous best score during run', () => {
  const rng = makeSeededRng(55);
  const state = startNewRun(createInitialState(100, 'title'));

  // Run long enough to surpass bestScore of 100 (live bestScore updates during run)
  for (let i = 0; i < 120; i++) {
    stepSimulation(state, 1 / 60, IDLE_INPUT, rng);
  }
  assert.ok(state.score > 100, 'score should exceed starting best score');
  assert.equal(state.bestScore, state.score, 'live bestScore tracks current score');

  // Trigger KO and advance to gameover
  for (let hit = 0; hit < 3; hit++) {
    state.player.invulnTimer = 0;
    state.hitstopTimer = 0;
    applyDamageToPlayer(state, rng);
  }
  for (let i = 0; i < 60; i++) {
    stepSimulation(state, 1 / 60, IDLE_INPUT, rng);
  }

  assert.equal(state.phase, 'gameover');
  assert.ok(state.finalRun !== null);
  assert.equal(state.finalRun.isNewBest, true, 'isNewBest must reflect beating runStartBestScore');
});

test('selected character (lily) persists across startNewRun and returnToTitle', () => {
  const initial = createInitialState(0, 'title', 'lily');
  assert.equal(initial.characterId, 'lily');

  const playing = startNewRun(initial);
  assert.equal(playing.characterId, 'lily');

  const backToTitle = returnToTitle(playing);
  assert.equal(backToTitle.characterId, 'lily');
});

test('portrait and landscape viewports provide dedicated framing without clipping the runner', () => {
  const portrait = getRenderViewport('portrait');
  const landscape = getRenderViewport('landscape');

  assert.ok(portrait.height > portrait.width, 'portrait viewport must be vertical');
  assert.ok(landscape.width > landscape.height, 'landscape viewport must be horizontal');

  // Player sits cleanly inside portrait view with generous lookahead ahead
  const playerScreenX = PLAYER_X - portrait.cameraX;
  assert.ok(playerScreenX >= 24 && playerScreenX <= 80, 'player should be framed near left edge');
  assert.ok(portrait.width - playerScreenX >= 240, 'portrait view must keep >= 240px runway lookahead');

  // Ground sits above the bottom underground control deck in portrait mode
  const groundScreenY = GROUND_Y + portrait.worldOffsetY;
  assert.ok(portrait.height - groundScreenY >= 140, 'portrait mode must leave underground space for touch deck');
});

test('daily missions are deterministic per date and track run completion', () => {
  const m1 = generateDailyMissions('2026-11-19');
  const m2 = generateDailyMissions('2026-11-19');
  assert.equal(m1.length, 3);
  assert.deepEqual(m1, m2, 'same calendar date must generate identical missions');

  const day1 = claimDailyBonus(syncDailyStateForDate(null, '2026-11-19'));
  const { next: afterRun, newlyCompleted } = applyRunToDailyMissions(day1, {
    score: 9999,
    distance: 2000,
    coins: 100,
    stomps: 20,
    zoneIndex: 3,
  });
  assert.equal(newlyCompleted.length, 3, 'big run should complete all 3 daily missions');
  assert.equal(afterRun.missions.every((m) => m.completed), true);
  assert.ok(computeActiveRunBonus(afterRun).startingScoreBonus > 1000, 'all-clear bonus applies');
});

test('daily play streak increments on consecutive days, resets on gaps, and grants run perks', () => {
  const day1 = claimDailyBonus(syncDailyStateForDate(null, '2026-11-19'));
  assert.equal(day1.streakDays, 1);

  const day2 = claimDailyBonus(syncDailyStateForDate(day1, '2026-11-20'));
  assert.equal(day2.streakDays, 2);

  const day3 = claimDailyBonus(syncDailyStateForDate(day2, '2026-11-21'));
  assert.equal(day3.streakDays, 3);
  const perksDay3 = getStreakPerks(day3.streakDays);
  assert.equal(perksDay3.startWithShield, true, 'Day 3+ streak grants starting Star Shield');

  const perksDay5 = getStreakPerks(5);
  assert.ok(perksDay5.startMagnetSeconds >= 4, 'Day 5+ streak grants starting Coin Magnet');

  // Missing a day resets streak back to Day 1
  const afterGap = syncDailyStateForDate(day3, '2026-11-24');
  assert.equal(afterGap.streakDays, 1);

  // startNewRun applies daily bonus perks directly to the run state
  const boosted = startNewRun(createInitialState(0, 'title'), perksDay5);
  assert.equal(boosted.bonusScore, perksDay5.startingScoreBonus);
  assert.equal(boosted.player.shieldActive, true);
  assert.equal(boosted.player.magnetTimer, perksDay5.startMagnetSeconds);
});

test('v2 Pixel Knight starts with 4 max hearts and Slow-Mo slows scroll speed', () => {
  const rng = makeSeededRng(77);
  const knightState = startNewRun(createInitialState(0, 'title', 'knight'));
  assert.equal(knightState.player.maxHp, 4);
  assert.equal(knightState.player.hp, 4);

  stepSimulation(knightState, 1 / 60, IDLE_INPUT, rng);
  const normalSpeed = knightState.speed;

  knightState.player.slowMoTimer = 4.5;
  stepSimulation(knightState, 1 / 60, IDLE_INPUT, rng);
  assert.ok(knightState.speed < normalSpeed * 0.75, 'Slow-Mo must reduce world speed');
});

test('v2 Pixel Shop handles coin bank, Knight unlock, upgrades, trails, and Top-5 leaderboard', () => {
  let shop = createDefaultShopState();
  shop = addCoinsToBank(shop, 250);
  assert.equal(shop.coinsBank, 250);

  shop = buyKnightCharacter(shop);
  assert.equal(shop.unlockedCharacters.includes('knight'), true);
  assert.equal(shop.coinsBank, 150);

  shop = buyUpgrade(shop, 'magnetLevel');
  assert.equal(shop.upgrades.magnetLevel, 1);
  assert.equal(shop.coinsBank, 110);

  shop = buyOrEquipTrail(shop, 'rainbow');
  assert.equal(shop.activeTrail, 'rainbow');
  assert.equal(shop.coinsBank, 50);

  for (const s of [300, 1200, 800, 2500, 100, 1900]) {
    shop = recordLeaderboardRun(shop, {
      score: s,
      distance: 100,
      characterId: 'lily',
      dateKey: '2026-11-20',
    });
  }
  assert.equal(shop.leaderboard.length, 5);
  assert.equal(shop.leaderboard[0].score, 2500);
  assert.equal(shop.leaderboard[4].score, 300);
});

test('v2 Hero Creator builds Male/Female sprites and unlocks cosmetics via career missions', () => {
  const cfg = { ...createDefaultCustomConfig(), name: 'SOFIA', gender: 'female' as const };
  const sprite = buildCustomSpriteSet(cfg);
  assert.equal(sprite.name, 'SOFIA');
  assert.equal(sprite.id, 'custom');

  const stats0 = createDefaultCareerStats();
  const crownGold = HAIR_OPTIONS.find((h) => h.id === 'royal-gold')!;
  assert.equal(isOptionUnlocked(crownGold, stats0), false, 'Crown Gold starts locked');

  const stats1 = recordCareerRun(
    stats0,
    { score: 3000, distance: 1600, coins: 80, stomps: 12, zoneIndex: 3 },
    3,
  );
  const unlocked = getNewlyUnlockedCosmetics(stats0, stats1);
  assert.equal(unlocked.length, 6, 'completing all career goals unlocks all 6 mission cosmetics');
  assert.equal(isOptionUnlocked(crownGold, stats1), true);
});
