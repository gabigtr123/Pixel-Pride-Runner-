import test from 'node:test';
import assert from 'node:assert/strict';
import {
  applyDamageToPlayer,
  checkAABB,
  computeDifficulty,
  createInitialState,
  getPlayerHitbox,
  makeDateSeededRng,
  returnToTitle,
  startNewRun,
  stepSimulation,
} from './engine';
import {
  claimAchievementReward,
  createDefaultAchievementsState,
  evaluateAchievements,
} from './achievements';
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
  buyBooster,
  buyExtraGadgetSlot,
  buyKnightCharacter,
  buyOrEquipCape,
  buyOrEquipHat,
  buyOrEquipPet,
  buyOrEquipTrail,
  buyOrToggleGadget,
  buyUpgrade,
  consumeArmedBoosters,
  createDefaultShopState,
  getMaxGadgetSlots,
  recordLeaderboardRun,
} from './shop';
import type { InputFrame } from './types';
import {
  computeNightFactor,
  createDefaultWeatherSettings,
  getEffectiveWeather,
  mapWmoCodeToWeather,
} from './weather';

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
  const cfg = { ...createDefaultCustomConfig(), name: 'LUNA', gender: 'female' as const };
  const sprite = buildCustomSpriteSet(cfg);
  assert.equal(sprite.name, 'LUNA');
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

test('v2.1 Winter Theme and Live Weather Sync map WMO codes and resolve effective weather', () => {
  assert.equal(mapWmoCodeToWeather(0), 'clear');
  assert.equal(mapWmoCodeToWeather(45), 'fog');
  assert.equal(mapWmoCodeToWeather(63), 'rain');
  assert.equal(mapWmoCodeToWeather(73), 'snow');
  assert.equal(mapWmoCodeToWeather(95), 'storm');

  const base = createDefaultWeatherSettings();
  assert.equal(getEffectiveWeather(base), null, 'classic biome auto uses biome default');

  const winter = { ...base, theme: 'winter' as const };
  assert.equal(getEffectiveWeather(winter), 'snow', 'winter theme defaults to snow');

  const liveRain = { ...winter, source: 'live' as const, liveWeather: 'rain' as const };
  assert.equal(getEffectiveWeather(liveRain), 'rain', 'live sync overrides winter default');

  const manualStorm = { ...liveRain, source: 'manual' as const, manualWeather: 'storm' as const };
  assert.equal(getEffectiveWeather(manualStorm), 'storm', 'manual weather takes precedence');

  // Day/Night cycle checks (30s full cycle -> 15s half-period)
  assert.equal(computeNightFactor({ ...base, timeOfDay: 'day' }, 15), 0);
  assert.equal(computeNightFactor({ ...base, timeOfDay: 'night' }, 0), 1);
  assert.equal(computeNightFactor({ ...liveRain, liveIsDay: false }, 0), 1, 'live night sync');
  assert.equal(computeNightFactor({ ...liveRain, liveIsDay: true }, 15), 0, 'live day sync');
  assert.ok(computeNightFactor(base, 0) < 0.01, 'auto cycle starts at bright day');
  assert.ok(computeNightFactor(base, 15) > 0.99, 'auto cycle reaches full night at 15s half-period');
});

test('v2.3 Level system advances every 3 Day/Night cycles (90s), awards bonus, and loops landscapes', () => {
  const rng = makeSeededRng(101);
  const st = startNewRun(createInitialState(0, 'title'));
  assert.equal(st.level, 1);
  assert.equal(st.dayCycleCount, 0);

  // Advance just before 3 cycles (89.5s) -> still Level 1, cycle 2
  st.runTime = 89.5;
  stepSimulation(st, 1 / 60, IDLE_INPUT, rng);
  assert.equal(st.dayCycleCount, 2);
  assert.equal(st.level, 1);

  // Cross 3 cycles (90s) -> spawns Mini-Boss #1 (Iron Beetle); defeating it advances to Level 2!
  const bonusBefore = st.bonusScore;
  st.runTime = 90.05;
  stepSimulation(st, 1 / 60, IDLE_INPUT, rng);
  assert.equal(st.dayCycleCount, 3);
  assert.ok(st.boss, 'crossing 90s spawns a Mini-Boss');
  assert.equal(st.boss?.kind, 'beetle');

  // Defeat Boss #1 by stomping from above with Turbo Dash
  st.boss!.hp = 1;
  st.boss!.x = st.player.x;
  st.boss!.y = st.player.y - 16;
  st.player.turboDashTimer = 2;
  stepSimulation(st, 1 / 60, IDLE_INPUT, rng);
  assert.equal(st.boss, null, 'boss clears after KO');
  assert.equal(st.bossesDefeated, 1);
  assert.equal(st.level, 2, 'defeating Boss #1 advances to Level 2');
  assert.ok(st.bonusScore >= bonusBefore + 750, 'boss KO + level up awards bonus points');
  assert.ok(st.levelBannerTimer > 2, 'level up shows banner');

  // New run resets cleanly back to Level 1
  const reset = startNewRun(st);
  assert.equal(reset.level, 1);
  assert.equal(reset.dayCycleCount, 0);
  assert.equal(reset.boss, null);
});

test('v2.4 Shop 2.0 handles Wardrobe, 2-slot + unlockable 3rd Gadget slot, and consumable Boosters', () => {
  let shop = addCoinsToBank(createDefaultShopState(), 2500);

  // Wardrobe hats & capes
  shop = buyOrEquipHat(shop, 'crown');
  assert.equal(shop.activeHat, 'crown');
  shop = buyOrEquipCape(shop, 'jetpack');
  assert.equal(shop.activeCape, 'jetpack');

  // 2 gadget slots by default
  assert.equal(getMaxGadgetSlots(shop), 2);
  shop = buyOrToggleGadget(shop, 'springBoots');
  shop = buyOrToggleGadget(shop, 'secondWind');
  shop = buyOrToggleGadget(shop, 'megaMagnet'); // bought, stays unequipped at 2/2 cap
  assert.equal(shop.equippedGadgets.length, 2);

  // Unlock 3rd extra gadget slot (500C) and equip 3rd gadget
  shop = buyExtraGadgetSlot(shop);
  assert.equal(getMaxGadgetSlots(shop), 3);
  shop = buyOrToggleGadget(shop, 'megaMagnet');
  assert.equal(shop.equippedGadgets.length, 3);

  // Buy & consume pre-run boosters
  shop = buyBooster(shop, 'headStart');
  assert.equal(shop.boosters.headStart, 1);
  assert.equal(shop.armedBoosters.includes('headStart'), true);
  const { nextShop, consumed } = consumeArmedBoosters(shop);
  assert.deepEqual(consumed, ['headStart']);
  assert.equal(nextShop.boosters.headStart, 0);
  assert.equal(nextShop.armedBoosters.length, 0);
});

test('v2.4 Gadgets & Turbo Dash grant Triple Jump, Second Wind revive, and invincible hazard smash', () => {
  const rng = makeSeededRng(204);
  const st = startNewRun(createInitialState(0, 'title'), {
    gadgets: ['springBoots', 'secondWind'],
    startTurboDashSeconds: 4,
  });

  // Spring Boots grants 3 jumps
  assert.equal(st.player.maxJumps, 3);
  assert.equal(st.player.turboDashTimer, 4);

  // Turbo Dash smashes hazards while invincible without hurting player
  st.hazards.push({
    id: 999,
    kind: 'spike',
    x: st.player.x,
    y: st.player.y - 14,
    width: 16,
    height: 14,
    baseY: st.player.y - 14,
    vx: 0,
    vy: 0,
    phaseTimer: 0,
    telegraphTimer: 0,
    telegraphSounded: false,
    isActive: true,
    stomped: false,
    koTimer: 0,
    koVx: 0,
    koVy: 0,
    hitFlash: 0,
  });
  const hpBefore = st.player.hp;
  stepSimulation(st, 1 / 60, IDLE_INPUT, rng);
  assert.equal(st.player.hp, hpBefore, 'Turbo Dash must protect from spikes');
  assert.equal(st.hazards.length, 0, 'Turbo Dash must smash the hazard');

  // Second Wind revives player once when hitting 0 HP
  st.player.turboDashTimer = 0;
  st.player.invulnTimer = 0;
  st.player.hp = 1;
  applyDamageToPlayer(st, rng);
  assert.equal(st.player.koActive, false, 'Second Wind prevents KO');
  assert.equal(st.player.hp, 2, 'Second Wind restores 2 HP');
  assert.equal(st.player.secondWindUsed, true);
});

test('v3.0 Boss Rush spawns back-to-back Mini-Bosses and awards +25 coins + Heart/Shield drop on KO', () => {
  const rng = makeSeededRng(301);
  const st = startNewRun(createInitialState(0, 'title'), { gameMode: 'bossRush' });
  assert.equal(st.gameMode, 'bossRush');

  // Step 4 seconds -> Boss #1 (Iron Beetle) spawns
  for (let i = 0; i < 240; i++) {
    stepSimulation(st, 1 / 60, IDLE_INPUT, rng);
  }
  assert.ok(st.boss, 'Boss Rush spawns first boss after cooldown');
  assert.equal(st.boss?.name, 'IRON BEETLE');

  // Stomp Boss #1 from above to deal final hit
  st.boss!.hp = 1;
  st.boss!.x = st.player.x;
  st.boss!.y = st.player.y - 14;
  st.player.y = st.boss!.y + 2;
  st.player.vy = 180;
  st.player.onGround = false;
  const coinsBefore = st.coins;
  stepSimulation(st, 1 / 60, IDLE_INPUT, rng);
  assert.equal(st.boss, null);
  assert.equal(st.bossesDefeated, 1);
  assert.equal(st.coins, coinsBefore + 25, 'Boss KO awards +25 coins');
  assert.ok(
    st.player.shieldActive || st.pickups.some((pk) => pk.kind === 'heart' || pk.kind === 'shield'),
    'Boss KO grants or drops Heart/Shield pickup',
  );
});

test('v3.0 Game Modes (Daily Seed determinism, Hardcore 1 HP/1.25x) & Pet Companions work properly', () => {
  // Daily Seed determinism
  const rngA = makeDateSeededRng('2026-11-24');
  const rngB = makeDateSeededRng('2026-11-24');
  for (let i = 0; i < 10; i++) {
    assert.equal(rngA(), rngB(), 'same date key must produce identical sequence');
  }

  // Hardcore mode: 1 HP & 1.25x speed
  const endless = startNewRun(createInitialState(0, 'title'), { gameMode: 'endless' });
  const hardcore = startNewRun(createInitialState(0, 'title'), { gameMode: 'hardcore' });
  assert.equal(hardcore.player.maxHp, 1);
  assert.equal(hardcore.player.hp, 1);
  stepSimulation(endless, 1 / 60, IDLE_INPUT, makeSeededRng(10));
  stepSimulation(hardcore, 1 / 60, IDLE_INPUT, makeSeededRng(10));
  assert.ok(hardcore.speed > endless.speed * 1.2, 'Hardcore runs 1.25x faster');

  // Pet Shop + Mini Dragon zap
  let shop = addCoinsToBank(createDefaultShopState(), 1000);
  shop = buyOrEquipPet(shop, 'dragon');
  assert.equal(shop.activePet, 'dragon');

  const dragonRun = startNewRun(createInitialState(0, 'title'), { petId: 'dragon' });
  dragonRun.petActionTimer = 0.01;
  dragonRun.hazards.push({
    id: 777,
    kind: 'spike',
    x: dragonRun.player.x + 60,
    y: GROUND_Y - 18,
    width: 14,
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
  stepSimulation(dragonRun, 1 / 60, IDLE_INPUT, makeSeededRng(11));
  assert.equal(dragonRun.hazards.length, 0, 'Mini Dragon zaps nearest visible hazard');
  assert.ok(dragonRun.bonusScore >= 75, 'Mini Dragon zap awards +75 bonus');
});

test('v3.0 Trophy Room unlocks achievements and pays claimable coin rewards once', () => {
  let shop = addCoinsToBank(createDefaultShopState(), 2000);
  shop = buyOrEquipHat(shop, 'crown');
  shop = buyOrEquipCape(shop, 'wings');
  shop = buyOrEquipPet(shop, 'fox');
  shop = buyExtraGadgetSlot(shop);

  const initial = createDefaultAchievementsState();
  const { next: afterRun, newlyUnlocked } = evaluateAchievements(initial, shop, {
    score: 12000,
    distance: 3200,
    coins: 120,
    stomps: 35,
    level: 4,
    bossesDefeated: 1,
    isHardcore: true,
    isStormOrRain: true,
    isWinter: true,
  });

  assert.ok(newlyUnlocked.length >= 9, 'unlocks multiple achievements on milestone run');
  assert.ok(afterRun.unlockedIds.includes('first_boss'));
  assert.ok(afterRun.unlockedIds.includes('fashion_icon'));
  assert.ok(afterRun.unlockedIds.includes('pet_lover'));
  assert.ok(afterRun.unlockedIds.includes('gadget_master'));

  const claim1 = claimAchievementReward(afterRun, 'first_boss');
  assert.equal(claim1.rewardCoins, 80);
  assert.ok(claim1.next.claimedIds.includes('first_boss'));

  // Double-claiming yields 0 coins
  const claim2 = claimAchievementReward(claim1.next, 'first_boss');
  assert.equal(claim2.rewardCoins, 0);
});

test('v3.0 Run Card assigns COMMON, RARE, EPIC, and LEGENDARY trading card rarities', async () => {
  const { getCardRarity } = await import('./runCard');
  const base = {
    score: 500,
    bestScore: 500,
    distance: 80,
    coins: 10,
    stomps: 2,
    zoneName: 'SUNLIT HIGHLANDS',
    zoneIndex: 0,
    level: 1,
    bossesDefeated: 0,
    gameMode: 'endless' as const,
    isNewBest: false,
  };
  assert.equal(getCardRarity(base).rarity, 'COMMON');
  assert.equal(getCardRarity({ ...base, score: 1500, level: 2 }).rarity, 'RARE');
  assert.equal(getCardRarity({ ...base, score: 4500, bossesDefeated: 1 }).rarity, 'EPIC');
  assert.equal(getCardRarity({ ...base, score: 12000, bossesDefeated: 2 }).rarity, 'LEGENDARY');
});

test('v3.0.1 Run Card PNG helpers format filename, share text, and base64 payload', async () => {
  const { buildRunCardFilename, buildRunCardShareText, extractPngBase64 } = await import('./runCard');
  assert.equal(buildRunCardFilename(4280.9), 'pixel-stride-card-4280.png');
  assert.equal(buildRunCardFilename(-5), 'pixel-stride-card-0.png');
  assert.equal(extractPngBase64('data:image/png;base64,iVBORw0KGgo='), 'iVBORw0KGgo=');
  assert.equal(extractPngBase64('iVBORw0KGgo='), 'iVBORw0KGgo=');
  const text = buildRunCardShareText({
    score: 4280,
    bestScore: 5000,
    distance: 310,
    coins: 45,
    stomps: 8,
    zoneName: 'AMBER CANYON',
    zoneIndex: 1,
    level: 2,
    bossesDefeated: 1,
    gameMode: 'endless',
    isNewBest: false,
  });
  assert.ok(text.includes('4,280') || text.includes('4280'));
  assert.ok(text.includes('LV 2'));
  assert.ok(text.includes('310m'));
});

test('v3.0.1 Android manifest script verifies FileProvider and idempotently adds SDK-capped storage permissions', async () => {
  const { patchAndroidManifest, verifyFilePathsXml } = await import('../../scripts/configure-android-manifest.mjs');
  const sampleManifest = `<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application android:allowBackup="true">
        <provider android:name="androidx.core.content.FileProvider" android:authorities="\${applicationId}.fileprovider" />
    </application>
    <uses-permission android:name="android.permission.INTERNET" />
</manifest>
`;
  const once = patchAndroidManifest(sampleManifest);
  assert.ok(once.includes('android:requestLegacyExternalStorage="true"'));
  assert.ok(once.includes('android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32"'));
  assert.ok(once.includes('android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="29"'));

  // Idempotent on second run
  const twice = patchAndroidManifest(once);
  assert.equal(twice, once);

  // Fails fast if FileProvider or <cache-path> is missing
  assert.throws(() => patchAndroidManifest('<manifest><application></application></manifest>'));
  verifyFilePathsXml('<paths><cache-path name="my_cache_images" path="." /></paths>');
  assert.throws(() => verifyFilePathsXml('<paths><external-path name="ext" path="." /></paths>'));
});

