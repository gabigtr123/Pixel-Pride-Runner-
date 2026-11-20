export type GamePhase = 'title' | 'playing' | 'paused' | 'gameover';

export type CharacterId = 'stride' | 'lacramioara' | 'knight' | 'custom';

export type CharacterGender = 'male' | 'female';

export interface CustomCharacterConfig {
  name: string;
  gender: CharacterGender;
  skinId: string;
  hairId: string;
  scarfId: string;
  outfitId: string;
}

export type TrailId = 'classic' | 'rainbow' | 'blossom' | 'ember';

export type OrientationMode = 'portrait' | 'landscape';

export interface RenderViewport {
  mode: OrientationMode;
  width: number;
  height: number;
  cameraX: number;
  worldOffsetY: number;
}

export type HazardKind = 'spike' | 'hopper' | 'swooper' | 'flame';

export type PickupKind = 'coin' | 'shield' | 'heart' | 'magnet' | 'doubleScore' | 'slowMo';

export type AudioEvent =
  | { type: 'jump'; pitchScale: number }
  | { type: 'doubleJump'; pitchScale: number }
  | { type: 'land' }
  | { type: 'slide' }
  | { type: 'coin'; combo: number }
  | { type: 'powerup'; kind: 'shield' | 'heart' | 'magnet' | 'doubleScore' | 'slowMo' }
  | { type: 'stomp' }
  | { type: 'telegraph' }
  | { type: 'shieldBreak' }
  | { type: 'hurt' }
  | { type: 'gameOver' }
  | { type: 'start' }
  | { type: 'zoneUp' };

export interface InputFrame {
  jumpHeld: boolean;
  jumpJustPressed: boolean;
  jumpJustReleased: boolean;
  duckHeld: boolean;
  duckJustPressed: boolean;
}

export interface PlayerState {
  x: number;
  y: number; // bottom Y coordinate (feet)
  vy: number;
  width: number;
  height: number;
  standHeight: number;
  duckHeight: number;
  onGround: boolean;
  onPlatform: boolean;
  isDucking: boolean;
  jumpsRemaining: number;
  maxJumps: number;
  coyoteTimer: number;
  jumpBufferTimer: number;
  hp: number;
  maxHp: number;
  invulnTimer: number;
  hitFlashTimer: number;
  hurtAnimationTimer: number;
  shieldActive: boolean;
  magnetTimer: number;
  doubleScoreTimer: number;
  slowMoTimer: number;
  trailTimer: number;
  scaleX: number;
  scaleY: number;
  runCycleDist: number;
  slideDustTimer: number;
  koActive: boolean;
  koVy: number;
  koRot: number;
}

export interface PlatformEntity {
  id: number;
  x: number;
  y: number; // top surface Y
  width: number;
  height: number;
}

export interface HazardEntity {
  id: number;
  kind: HazardKind;
  x: number;
  y: number; // top-left Y
  width: number;
  height: number;
  baseY: number;
  vx: number;
  vy: number;
  phaseTimer: number;
  telegraphTimer: number;
  telegraphSounded: boolean;
  isActive: boolean; // used by flame flare & hopper hop
  stomped: boolean;
  koTimer: number;
  koVx: number;
  koVy: number;
  hitFlash: number;
}

export interface PickupEntity {
  id: number;
  kind: PickupKind;
  x: number;
  y: number;
  width: number;
  height: number;
  bobOffset: number;
  collected: boolean;
}

export interface Particle {
  active: boolean;
  x: number;
  y: number;
  vx: number;
  vy: number;
  size: number;
  color: string;
  life: number;
  maxLife: number;
  gravity: number;
}

export interface FloatingText {
  active: boolean;
  x: number;
  y: number;
  vy: number;
  text: string;
  color: string;
  life: number;
  maxLife: number;
}

export interface BiomePalette {
  name: string;
  skyTop: string;
  skyMid: string;
  skyHorizon: string;
  celestialType: 'sun' | 'sunset' | 'moon' | 'dawn';
  celestialColor: string;
  starAlpha: number;
  cloudMain: string;
  cloudShadow: string;
  farMountain: string;
  nearHill: string;
  ruinColor: string;
  ruinHighlight: string;
  grassTop: string;
  grassTrim: string;
  dirtMain: string;
  dirtDark: string;
  dirtAccent: string;
  weather?: 'snow' | 'embers';
}

export interface RunSnapshot {
  score: number;
  bestScore: number;
  distance: number;
  coins: number;
  stomps: number;
  zoneName: string;
  zoneIndex: number;
  isNewBest: boolean;
}

export interface HudSnapshot {
  phase: GamePhase;
  characterId: CharacterId;
  score: number;
  bestScore: number;
  distance: number;
  coins: number;
  combo: number;
  hp: number;
  maxHp: number;
  damageFlash: boolean;
  shieldActive: boolean;
  magnetSeconds: number;
  doubleScoreSeconds: number;
  slowMoSeconds: number;
  zoneName: string;
  zoneIndex: number;
  zoneBannerTimer: number;
  finalRun: RunSnapshot | null;
}

export interface GameState {
  phase: GamePhase;
  characterId: CharacterId;
  customCharacter?: CustomCharacterConfig;
  trailId: TrailId;
  magnetDurationBonus: number;
  coinBonusPoints: number;
  powerupCooldownReduction: number;
  elapsed: number;
  runTime: number;
  distance: number; // meters-like unit
  worldScroll: number; // pixels traveled
  speed: number; // pixels/sec
  score: number;
  bonusScore: number;
  bestScore: number;
  runStartBestScore: number;
  coins: number;
  stomps: number;
  combo: number;
  comboTimer: number;
  zoneIndex: number;
  zoneBannerTimer: number;
  hitstopTimer: number;
  shakeTrauma: number;
  gameOverDelayTimer: number;
  nextEntityId: number;
  hazardCooldown: number;
  platformCooldown: number;
  coinArcCooldown: number;
  powerupCooldown: number;
  player: PlayerState;
  platforms: PlatformEntity[];
  hazards: HazardEntity[];
  pickups: PickupEntity[];
  particles: Particle[];
  particleCursor: number;
  floatingTexts: FloatingText[];
  textCursor: number;
  audioQueue: AudioEvent[];
  finalRun: RunSnapshot | null;
}
