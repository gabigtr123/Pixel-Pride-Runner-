import { LILY_FRAMES, RUNNER_FRAMES, type CharacterSpriteSet } from './sprites';
import type { CharacterGender, CustomCharacterConfig } from './types';

export interface CareerStats {
  totalRuns: number;
  totalCoins: number;
  totalDistance: number;
  totalStomps: number;
  bestScore: number;
  maxZoneReached: number; // 1-based (1..6)
  dailyMissionsCompleted: number;
}

export interface CosmeticUnlockRule {
  stat: keyof CareerStats;
  target: number;
  label: string;
}

export interface ColorOption {
  id: string;
  name: string;
  primary: string;
  secondary: string;
  accent?: string;
  unlock?: CosmeticUnlockRule;
}

export interface CreatorStorageState {
  config: CustomCharacterConfig;
  stats: CareerStats;
}

export const CREATOR_STORAGE_KEY = 'pixel_stride_creator_v1';

export const PRESET_NAMES: Record<CharacterGender, string[]> = {
  male: ['ALEX', 'LEO', 'MAX', 'KAI', 'FINN', 'COLE'],
  female: ['MAYA', 'NOVA', 'ARIA', 'ZOE', 'CLARA', 'MIRA'],
};

export const SKIN_OPTIONS: ColorOption[] = [
  { id: 'peach', name: 'PEACH', primary: '#FFCC98', secondary: '#FFD0A4' },
  { id: 'warm', name: 'WARM', primary: '#F0B478', secondary: '#F4BE88' },
  { id: 'tan', name: 'TAN', primary: '#C8844C', secondary: '#D49058' },
  { id: 'deep', name: 'DEEP', primary: '#7E4A2C', secondary: '#925A38' },
];

export const HAIR_OPTIONS: ColorOption[] = [
  { id: 'navy', name: 'NAVY', primary: '#304488', secondary: '#5878D0', accent: '#FFE066' },
  { id: 'chestnut', name: 'CHESTNUT', primary: '#7B3428', secondary: '#C86844', accent: '#FFFDF5' },
  { id: 'blonde', name: 'BLONDE', primary: '#D8A028', secondary: '#FFE478', accent: '#FFFDF5' },
  { id: 'raven', name: 'RAVEN', primary: '#24283C', secondary: '#4C5478', accent: '#78E8FF' },
  { id: 'emerald', name: 'EMERALD', primary: '#207C58', secondary: '#48C878', accent: '#FFF4A8' },
  {
    id: 'royal-gold',
    name: 'CROWN GOLD',
    primary: '#E09818',
    secondary: '#FFF088',
    accent: '#FFFFFF',
    unlock: { stat: 'bestScore', target: 2500, label: 'Score 2500+ in 1 run' },
  },
  {
    id: 'cyber-neon',
    name: 'CYBER NEON',
    primary: '#1888B8',
    secondary: '#68F4FF',
    accent: '#FF70D8',
    unlock: { stat: 'maxZoneReached', target: 3, label: 'Reach Zone 3' },
  },
];

export const SCARF_OPTIONS: ColorOption[] = [
  { id: 'crimson', name: 'CRIMSON', primary: '#E83B48', secondary: '#A41E34' },
  { id: 'rose', name: 'ROSE', primary: '#E85898', secondary: '#A82864' },
  { id: 'sky', name: 'SKY', primary: '#48A8FF', secondary: '#2460B8' },
  { id: 'sunburst', name: 'SUNBURST', primary: '#F09828', secondary: '#B45C14' },
  { id: 'violet', name: 'VIOLET', primary: '#9858E8', secondary: '#5C2C9E' },
  {
    id: 'prism-flame',
    name: 'SOLAR GOLD',
    primary: '#FFE048',
    secondary: '#D87C18',
    unlock: { stat: 'totalStomps', target: 10, label: 'Stomp 10 enemies total' },
  },
  {
    id: 'frost-silk',
    name: 'FROST SILK',
    primary: '#A8F4FF',
    secondary: '#48A8D8',
    unlock: { stat: 'totalDistance', target: 1500, label: 'Run 1500m total' },
  },
];

export const OUTFIT_OPTIONS: ColorOption[] = [
  { id: 'snow', name: 'CLASSIC', primary: '#F4F2E8', secondary: '#B8C2DC', accent: '#283668' },
  { id: 'emerald', name: 'FOREST', primary: '#38B87C', secondary: '#207C58', accent: '#2E2854' },
  { id: 'cobalt', name: 'COBALT', primary: '#4878E0', secondary: '#284898', accent: '#1C2444' },
  { id: 'sunset', name: 'SUNSET', primary: '#E86848', secondary: '#A43828', accent: '#2C2240' },
  {
    id: 'shadow-ninja',
    name: 'SHADOW',
    primary: '#2E344E',
    secondary: '#1A1E32',
    accent: '#141626',
    unlock: { stat: 'totalCoins', target: 75, label: 'Collect 75 coins total' },
  },
  {
    id: 'starlight',
    name: 'STARLIGHT',
    primary: '#7848D8',
    secondary: '#482498',
    accent: '#FFE05C',
    unlock: { stat: 'dailyMissionsCompleted', target: 3, label: 'Complete 3 Daily Missions' },
  },
];

export function createDefaultCustomConfig(): CustomCharacterConfig {
  return {
    name: 'ALEX',
    gender: 'male',
    skinId: 'peach',
    hairId: 'navy',
    scarfId: 'crimson',
    outfitId: 'snow',
  };
}

export function createDefaultCareerStats(): CareerStats {
  return {
    totalRuns: 0,
    totalCoins: 0,
    totalDistance: 0,
    totalStomps: 0,
    bestScore: 0,
    maxZoneReached: 1,
    dailyMissionsCompleted: 0,
  };
}

export function createDefaultCreatorState(): CreatorStorageState {
  return {
    config: createDefaultCustomConfig(),
    stats: createDefaultCareerStats(),
  };
}

export function isOptionUnlocked(option: ColorOption, stats: CareerStats): boolean {
  if (!option.unlock) return true;
  return stats[option.unlock.stat] >= option.unlock.target;
}

export function recordCareerRun(
  stats: CareerStats,
  run: { score: number; distance: number; coins: number; stomps: number; zoneIndex: number },
  newlyCompletedDailyCount: number,
): CareerStats {
  return {
    totalRuns: stats.totalRuns + 1,
    totalCoins: stats.totalCoins + Math.max(0, Math.round(run.coins)),
    totalDistance: stats.totalDistance + Math.max(0, Math.round(run.distance)),
    totalStomps: stats.totalStomps + Math.max(0, Math.round(run.stomps)),
    bestScore: Math.max(stats.bestScore, Math.round(run.score)),
    maxZoneReached: Math.max(stats.maxZoneReached, run.zoneIndex + 1),
    dailyMissionsCompleted: stats.dailyMissionsCompleted + Math.max(0, newlyCompletedDailyCount),
  };
}

export function getNewlyUnlockedCosmetics(before: CareerStats, after: CareerStats): ColorOption[] {
  const all = [...HAIR_OPTIONS, ...SCARF_OPTIONS, ...OUTFIT_OPTIONS];
  return all.filter((opt) => !isOptionUnlocked(opt, before) && isOptionUnlocked(opt, after));
}

export function buildCustomSpriteSet(config: CustomCharacterConfig): CharacterSpriteSet {
  const skin = SKIN_OPTIONS.find((o) => o.id === config.skinId) ?? SKIN_OPTIONS[0];
  const hair = HAIR_OPTIONS.find((o) => o.id === config.hairId) ?? HAIR_OPTIONS[0];
  const scarf = SCARF_OPTIONS.find((o) => o.id === config.scarfId) ?? SCARF_OPTIONS[0];
  const outfit = OUTFIT_OPTIONS.find((o) => o.id === config.outfitId) ?? OUTFIT_OPTIONS[0];

  const palette: Record<string, string> = {
    K: '#121324',
    H: hair.primary,
    h: hair.secondary,
    V: hair.accent ?? '#FFE066',
    F: hair.accent ?? '#FFFDF5',
    L: scarf.primary,
    S: config.gender === 'female' ? skin.secondary : skin.primary,
    E: '#223868',
    R: scarf.primary,
    r: scarf.secondary,
    T: outfit.primary,
    t: outfit.secondary,
    B: outfit.accent ?? '#283668',
    O: '#F09028',
    o: '#9E4C14',
  };

  return {
    id: 'custom',
    name: (config.name.trim() || 'HERO').slice(0, 12).toUpperCase(),
    perk: '+15% COIN & STOMP',
    palette,
    frames: config.gender === 'female' ? LILY_FRAMES : RUNNER_FRAMES,
  };
}

export function loadCreatorState(): CreatorStorageState {
  try {
    const raw = window.localStorage.getItem(CREATOR_STORAGE_KEY);
    if (!raw) return createDefaultCreatorState();
    const parsed = JSON.parse(raw) as Partial<CreatorStorageState>;
    const def = createDefaultCreatorState();
    return {
      config: {
        name:
          typeof parsed.config?.name === 'string' && parsed.config.name.trim().length > 0
            ? parsed.config.name.trim().slice(0, 12).toUpperCase()
            : def.config.name,
        gender: parsed.config?.gender === 'female' ? 'female' : 'male',
        skinId: SKIN_OPTIONS.some((o) => o.id === parsed.config?.skinId)
          ? (parsed.config!.skinId as string)
          : def.config.skinId,
        hairId: HAIR_OPTIONS.some((o) => o.id === parsed.config?.hairId)
          ? (parsed.config!.hairId as string)
          : def.config.hairId,
        scarfId: SCARF_OPTIONS.some((o) => o.id === parsed.config?.scarfId)
          ? (parsed.config!.scarfId as string)
          : def.config.scarfId,
        outfitId: OUTFIT_OPTIONS.some((o) => o.id === parsed.config?.outfitId)
          ? (parsed.config!.outfitId as string)
          : def.config.outfitId,
      },
      stats: {
        totalRuns: Math.max(0, parsed.stats?.totalRuns ?? 0),
        totalCoins: Math.max(0, parsed.stats?.totalCoins ?? 0),
        totalDistance: Math.max(0, parsed.stats?.totalDistance ?? 0),
        totalStomps: Math.max(0, parsed.stats?.totalStomps ?? 0),
        bestScore: Math.max(0, parsed.stats?.bestScore ?? 0),
        maxZoneReached: Math.max(1, parsed.stats?.maxZoneReached ?? 1),
        dailyMissionsCompleted: Math.max(0, parsed.stats?.dailyMissionsCompleted ?? 0),
      },
    };
  } catch {
    return createDefaultCreatorState();
  }
}

export function saveCreatorState(state: CreatorStorageState): void {
  try {
    window.localStorage.setItem(CREATOR_STORAGE_KEY, JSON.stringify(state));
  } catch {
    // Ignore storage failures
  }
}
