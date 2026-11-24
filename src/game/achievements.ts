import type { ShopState } from './shop';

export interface AchievementDef {
  id: string;
  title: string;
  desc: string;
  icon: string;
  rewardCoins: number;
}

export interface TrophyCareerStats {
  totalRuns: number;
  totalDistance: number;
  totalCoins: number;
  totalStomps: number;
  bossesDefeated: number;
  bestScore: number;
  bestLevel: number;
  hardcoreRuns: number;
  stormRuns: number;
  winterRuns: number;
}

export interface AchievementsStorageState {
  unlockedIds: string[];
  claimedIds: string[];
  stats: TrophyCareerStats;
}

export const ACHIEVEMENTS_STORAGE_KEY = 'pixel_stride_achievements_v1';

export const ACHIEVEMENTS_CATALOG: AchievementDef[] = [
  {
    id: 'first_boss',
    title: 'BOSS SLAYER',
    desc: 'Defeat your first Mini-Boss',
    icon: '🐉',
    rewardCoins: 80,
  },
  {
    id: 'boss_master',
    title: 'BOSS HUNTER',
    desc: 'Defeat 5 Mini-Bosses total',
    icon: '⚔️',
    rewardCoins: 200,
  },
  {
    id: 'level_4',
    title: 'WORLD TRAVELER',
    desc: 'Reach Level 4 (Crystal Peaks)',
    icon: '🌍',
    rewardCoins: 150,
  },
  {
    id: 'storm_chaser',
    title: 'STORM CHASER',
    desc: 'Complete a run during Rain or Storm',
    icon: '⛈️',
    rewardCoins: 50,
  },
  {
    id: 'winter_runner',
    title: 'WINTER SPIRIT',
    desc: 'Complete a run in Winter Theme',
    icon: '❄️',
    rewardCoins: 50,
  },
  {
    id: 'fashion_icon',
    title: 'FASHION ICON',
    desc: 'Equip both a Hat and a Cape/Wings',
    icon: '👑',
    rewardCoins: 75,
  },
  {
    id: 'gadget_master',
    title: 'TECH GENIUS',
    desc: 'Unlock the 3rd Extra Gadget Slot',
    icon: '🛠️',
    rewardCoins: 120,
  },
  {
    id: 'pet_lover',
    title: 'BEST FRIEND',
    desc: 'Adopt any Pet Companion in Shop',
    icon: '🦊',
    rewardCoins: 80,
  },
  {
    id: 'hardcore_survivor',
    title: 'NO FEAR',
    desc: 'Score 1,500+ in Hardcore mode',
    icon: '💀',
    rewardCoins: 120,
  },
  {
    id: 'stomp_hunter',
    title: 'STOMP KING',
    desc: 'Stomp 30 enemies total',
    icon: '🥾',
    rewardCoins: 70,
  },
  {
    id: 'marathon_runner',
    title: 'MARATHONER',
    desc: 'Run 3,000m total distance',
    icon: '🏃',
    rewardCoins: 100,
  },
  {
    id: 'high_roller',
    title: 'ARCADE LEGEND',
    desc: 'Score 10,000+ in a single run',
    icon: '🏆',
    rewardCoins: 150,
  },
];

export function createDefaultTrophyStats(): TrophyCareerStats {
  return {
    totalRuns: 0,
    totalDistance: 0,
    totalCoins: 0,
    totalStomps: 0,
    bossesDefeated: 0,
    bestScore: 0,
    bestLevel: 1,
    hardcoreRuns: 0,
    stormRuns: 0,
    winterRuns: 0,
  };
}

export function createDefaultAchievementsState(): AchievementsStorageState {
  return {
    unlockedIds: [],
    claimedIds: [],
    stats: createDefaultTrophyStats(),
  };
}

export function evaluateAchievements(
  current: AchievementsStorageState,
  shop: ShopState,
  lastRun?: {
    score: number;
    distance: number;
    coins: number;
    stomps: number;
    level: number;
    bossesDefeated: number;
    isHardcore: boolean;
    isStormOrRain: boolean;
    isWinter: boolean;
  },
): { next: AchievementsStorageState; newlyUnlocked: AchievementDef[] } {
  const stats: TrophyCareerStats = lastRun
    ? {
        totalRuns: current.stats.totalRuns + 1,
        totalDistance: current.stats.totalDistance + Math.max(0, Math.round(lastRun.distance)),
        totalCoins: current.stats.totalCoins + Math.max(0, Math.round(lastRun.coins)),
        totalStomps: current.stats.totalStomps + Math.max(0, Math.round(lastRun.stomps)),
        bossesDefeated: current.stats.bossesDefeated + Math.max(0, Math.round(lastRun.bossesDefeated)),
        bestScore: Math.max(current.stats.bestScore, Math.round(lastRun.score)),
        bestLevel: Math.max(current.stats.bestLevel, lastRun.level),
        hardcoreRuns:
          current.stats.hardcoreRuns + (lastRun.isHardcore && lastRun.score >= 1500 ? 1 : 0),
        stormRuns: current.stats.stormRuns + (lastRun.isStormOrRain ? 1 : 0),
        winterRuns: current.stats.winterRuns + (lastRun.isWinter ? 1 : 0),
      }
    : current.stats;

  const unlockedSet = new Set(current.unlockedIds);
  const newlyUnlocked: AchievementDef[] = [];

  const check = (id: string, condition: boolean) => {
    if (condition && !unlockedSet.has(id)) {
      unlockedSet.add(id);
      const def = ACHIEVEMENTS_CATALOG.find((a) => a.id === id);
      if (def) newlyUnlocked.push(def);
    }
  };

  check('first_boss', stats.bossesDefeated >= 1);
  check('boss_master', stats.bossesDefeated >= 5);
  check('level_4', stats.bestLevel >= 4);
  check('storm_chaser', stats.stormRuns >= 1);
  check('winter_runner', stats.winterRuns >= 1);
  check('fashion_icon', shop.activeHat !== 'none' && shop.activeCape !== 'none');
  check('gadget_master', shop.extraGadgetSlot);
  check('pet_lover', shop.unlockedPets.some((p) => p !== 'none'));
  check('hardcore_survivor', stats.hardcoreRuns >= 1);
  check('stomp_hunter', stats.totalStomps >= 30);
  check('marathon_runner', stats.totalDistance >= 3000);
  check('high_roller', stats.bestScore >= 10000);

  return {
    next: {
      ...current,
      unlockedIds: Array.from(unlockedSet),
      stats,
    },
    newlyUnlocked,
  };
}

export function claimAchievementReward(
  state: AchievementsStorageState,
  id: string,
): { next: AchievementsStorageState; rewardCoins: number } {
  if (!state.unlockedIds.includes(id) || state.claimedIds.includes(id)) {
    return { next: state, rewardCoins: 0 };
  }
  const def = ACHIEVEMENTS_CATALOG.find((a) => a.id === id);
  if (!def) return { next: state, rewardCoins: 0 };
  return {
    next: {
      ...state,
      claimedIds: [...state.claimedIds, id],
    },
    rewardCoins: def.rewardCoins,
  };
}

export function loadAchievementsState(): AchievementsStorageState {
  try {
    const raw = window.localStorage.getItem(ACHIEVEMENTS_STORAGE_KEY);
    if (!raw) return createDefaultAchievementsState();
    const parsed = JSON.parse(raw) as Partial<AchievementsStorageState>;
    const def = createDefaultAchievementsState();
    const validIds = new Set(ACHIEVEMENTS_CATALOG.map((a) => a.id));
    return {
      unlockedIds: Array.isArray(parsed.unlockedIds)
        ? Array.from(new Set(parsed.unlockedIds.filter((id) => validIds.has(id))))
        : [],
      claimedIds: Array.isArray(parsed.claimedIds)
        ? Array.from(new Set(parsed.claimedIds.filter((id) => validIds.has(id))))
        : [],
      stats: {
        totalRuns: Math.max(0, parsed.stats?.totalRuns ?? def.stats.totalRuns),
        totalDistance: Math.max(0, parsed.stats?.totalDistance ?? def.stats.totalDistance),
        totalCoins: Math.max(0, parsed.stats?.totalCoins ?? def.stats.totalCoins),
        totalStomps: Math.max(0, parsed.stats?.totalStomps ?? def.stats.totalStomps),
        bossesDefeated: Math.max(0, parsed.stats?.bossesDefeated ?? def.stats.bossesDefeated),
        bestScore: Math.max(0, parsed.stats?.bestScore ?? def.stats.bestScore),
        bestLevel: Math.max(1, parsed.stats?.bestLevel ?? def.stats.bestLevel),
        hardcoreRuns: Math.max(0, parsed.stats?.hardcoreRuns ?? def.stats.hardcoreRuns),
        stormRuns: Math.max(0, parsed.stats?.stormRuns ?? def.stats.stormRuns),
        winterRuns: Math.max(0, parsed.stats?.winterRuns ?? def.stats.winterRuns),
      },
    };
  } catch {
    return createDefaultAchievementsState();
  }
}

export function saveAchievementsState(state: AchievementsStorageState): void {
  try {
    window.localStorage.setItem(ACHIEVEMENTS_STORAGE_KEY, JSON.stringify(state));
  } catch {
    // Ignore storage failures
  }
}
