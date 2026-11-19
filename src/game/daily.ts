export type MissionKind = 'coins' | 'distance' | 'stomps' | 'score' | 'zone' | 'runs';

export interface DailyMission {
  id: string;
  kind: MissionKind;
  title: string;
  target: number;
  progress: number;
  rewardPoints: number;
  completed: boolean;
}

export interface DailyProgressState {
  dateKey: string; // YYYY-MM-DD
  streakDays: number;
  lastActiveDate: string | null;
  claimedToday: boolean;
  missions: DailyMission[];
}

export interface DailyRunBonus {
  startingScoreBonus: number;
  startWithShield: boolean;
  startMagnetSeconds: number;
}

export interface RunSummaryInput {
  score: number;
  distance: number;
  coins: number;
  stomps: number;
  zoneIndex: number; // 0-based
}

export const DAILY_STORAGE_KEY = 'pixel_stride_daily_v1';

export function getLocalDateKey(now: Date = new Date()): string {
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, '0');
  const d = String(now.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

export function getPreviousDateKey(dateKey: string): string {
  const [y, m, d] = dateKey.split('-').map(Number);
  const dt = new Date(Date.UTC(y, (m || 1) - 1, d || 1));
  dt.setUTCDate(dt.getUTCDate() - 1);
  const py = dt.getUTCFullYear();
  const pm = String(dt.getUTCMonth() + 1).padStart(2, '0');
  const pd = String(dt.getUTCDate()).padStart(2, '0');
  return `${py}-${pm}-${pd}`;
}

function hashDateKey(dateKey: string): number {
  let h = 2166136261 >>> 0;
  for (let i = 0; i < dateKey.length; i++) {
    h ^= dateKey.charCodeAt(i);
    h = Math.imul(h, 16777619) >>> 0;
  }
  return h >>> 0;
}

interface MissionTemplate {
  kind: MissionKind;
  title: (target: number) => string;
  targets: number[];
  rewardPoints: number;
}

const MISSION_POOL: MissionTemplate[] = [
  {
    kind: 'coins',
    title: (t) => `Collect ${t} Coins`,
    targets: [20, 30, 45],
    rewardPoints: 400,
  },
  {
    kind: 'distance',
    title: (t) => `Run ${t}m Total`,
    targets: [400, 600, 900],
    rewardPoints: 500,
  },
  {
    kind: 'stomps',
    title: (t) => `Stomp ${t} Enemies`,
    targets: [3, 5, 7],
    rewardPoints: 600,
  },
  {
    kind: 'score',
    title: (t) => `Score ${t} in 1 Run`,
    targets: [2000, 3500, 5000],
    rewardPoints: 650,
  },
  {
    kind: 'zone',
    title: (t) => `Reach Zone ${t}`,
    targets: [2, 3],
    rewardPoints: 600,
  },
  {
    kind: 'runs',
    title: (t) => `Play ${t} Runs Today`,
    targets: [2, 3, 4],
    rewardPoints: 350,
  },
];

export function generateDailyMissions(dateKey: string): DailyMission[] {
  let seed = hashDateKey(dateKey);
  const nextRand = () => {
    seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
    return seed / 0xffffffff;
  };

  // Pick 3 distinct mission templates deterministically for this date
  const available = [...MISSION_POOL];
  const chosen: DailyMission[] = [];
  for (let i = 0; i < 3 && available.length > 0; i++) {
    const idx = Math.floor(nextRand() * available.length) % available.length;
    const tpl = available.splice(idx, 1)[0];
    const target = tpl.targets[Math.floor(nextRand() * tpl.targets.length) % tpl.targets.length];
    chosen.push({
      id: `${dateKey}-${tpl.kind}`,
      kind: tpl.kind,
      title: tpl.title(target),
      target,
      progress: 0,
      rewardPoints: tpl.rewardPoints,
      completed: false,
    });
  }
  return chosen;
}

export function getStreakPerks(streakDays: number): DailyRunBonus {
  const day = Math.max(1, streakDays);
  const clamped = Math.min(7, day);
  const scoreTable = [0, 250, 500, 750, 1000, 1250, 1500, 2000];
  return {
    startingScoreBonus: scoreTable[clamped],
    startWithShield: day >= 3,
    startMagnetSeconds: day >= 7 ? 6 : day >= 6 ? 5 : day >= 5 ? 4 : 0,
  };
}

export function computeActiveRunBonus(daily: DailyProgressState): DailyRunBonus {
  const base = daily.claimedToday
    ? getStreakPerks(daily.streakDays)
    : { startingScoreBonus: 0, startWithShield: false, startMagnetSeconds: 0 };

  let missionBonus = 0;
  let completedCount = 0;
  for (const m of daily.missions) {
    if (m.completed) {
      missionBonus += m.rewardPoints;
      completedCount += 1;
    }
  }
  const allClearBonus = completedCount === daily.missions.length && daily.missions.length > 0 ? 1000 : 0;

  return {
    startingScoreBonus: base.startingScoreBonus + missionBonus + allClearBonus,
    startWithShield: base.startWithShield,
    startMagnetSeconds: base.startMagnetSeconds,
  };
}

export function syncDailyStateForDate(
  prev: DailyProgressState | null,
  todayKey: string,
): DailyProgressState {
  if (!prev) {
    return {
      dateKey: todayKey,
      streakDays: 1,
      lastActiveDate: null,
      claimedToday: false,
      missions: generateDailyMissions(todayKey),
    };
  }

  if (prev.dateKey === todayKey) {
    return prev;
  }

  const yesterdayKey = getPreviousDateKey(todayKey);
  const continuedStreak = prev.lastActiveDate === yesterdayKey || prev.dateKey === yesterdayKey && prev.claimedToday;
  const nextStreak = continuedStreak ? prev.streakDays + 1 : 1;

  return {
    dateKey: todayKey,
    streakDays: nextStreak,
    lastActiveDate: prev.lastActiveDate,
    claimedToday: false,
    missions: generateDailyMissions(todayKey),
  };
}

export function claimDailyBonus(daily: DailyProgressState): DailyProgressState {
  if (daily.claimedToday) return daily;
  return {
    ...daily,
    claimedToday: true,
    lastActiveDate: daily.dateKey,
  };
}

export function applyRunToDailyMissions(
  daily: DailyProgressState,
  run: RunSummaryInput,
): { next: DailyProgressState; newlyCompleted: DailyMission[] } {
  const newlyCompleted: DailyMission[] = [];

  const nextMissions = daily.missions.map((m) => {
    if (m.completed) return m;

    let nextProgress = m.progress;
    switch (m.kind) {
      case 'coins':
        nextProgress = m.progress + Math.max(0, Math.round(run.coins));
        break;
      case 'distance':
        nextProgress = m.progress + Math.max(0, Math.round(run.distance));
        break;
      case 'stomps':
        nextProgress = m.progress + Math.max(0, Math.round(run.stomps));
        break;
      case 'runs':
        nextProgress = m.progress + 1;
        break;
      case 'score':
        nextProgress = Math.max(m.progress, Math.round(run.score));
        break;
      case 'zone':
        nextProgress = Math.max(m.progress, run.zoneIndex + 1);
        break;
    }

    const clamped = Math.min(m.target, nextProgress);
    const completed = clamped >= m.target;
    const updated: DailyMission = {
      ...m,
      progress: clamped,
      completed,
    };
    if (completed) {
      newlyCompleted.push(updated);
    }
    return updated;
  });

  return {
    next: {
      ...daily,
      lastActiveDate: daily.dateKey,
      missions: nextMissions,
    },
    newlyCompleted,
  };
}

export function loadDailyProgress(todayKey: string = getLocalDateKey()): DailyProgressState {
  try {
    const raw = window.localStorage.getItem(DAILY_STORAGE_KEY);
    if (!raw) return syncDailyStateForDate(null, todayKey);
    const parsed = JSON.parse(raw) as DailyProgressState;
    if (!parsed || typeof parsed.dateKey !== 'string' || !Array.isArray(parsed.missions)) {
      return syncDailyStateForDate(null, todayKey);
    }
    return syncDailyStateForDate(parsed, todayKey);
  } catch {
    return syncDailyStateForDate(null, todayKey);
  }
}

export function saveDailyProgress(daily: DailyProgressState): void {
  try {
    window.localStorage.setItem(DAILY_STORAGE_KEY, JSON.stringify(daily));
  } catch {
    // Ignore storage failures in sandboxed frames
  }
}
