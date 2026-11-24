import { useState } from 'react';
import {
  ACHIEVEMENTS_CATALOG,
  type AchievementsStorageState,
} from '../game/achievements';

interface TrophyModalProps {
  achievements: AchievementsStorageState;
  coinsBank: number;
  onClaim: (id: string) => void;
  onClose: () => void;
}

type TrophyTab = 'trophies' | 'stats';

export function TrophyModal({
  achievements,
  coinsBank,
  onClaim,
  onClose,
}: TrophyModalProps) {
  const [tab, setTab] = useState<TrophyTab>('trophies');
  const unlockedSet = new Set(achievements.unlockedIds);
  const claimedSet = new Set(achievements.claimedIds);
  const unlockedCount = achievements.unlockedIds.length;
  const unclaimedCount = achievements.unlockedIds.filter((id) => !claimedSet.has(id)).length;
  const s = achievements.stats;

  return (
    <div
      className="modal-safe-area absolute inset-0 z-30 flex items-center justify-center bg-black/75 p-3 backdrop-blur-[2px]"
      onPointerDown={(e) => e.stopPropagation()}
    >
      <div className="pixel-panel flex max-h-[92%] w-full max-w-[460px] flex-col gap-2.5 overflow-hidden px-4 py-3.5 text-[#F6F4E8]">
        {/* Header */}
        <div className="flex items-center justify-between gap-2">
          <div>
            <h2 className="pixel-text-shadow text-xs text-[#FFE05C] sm:text-sm">
              🏆 TROPHY ROOM
            </h2>
            <p className="mt-1 text-[8px] text-[#98D8FF]">
              {unlockedCount}/{ACHIEVEMENTS_CATALOG.length} UNLOCKED
              {unclaimedCount > 0 ? ` • ${unclaimedCount} READY!` : ''}
            </p>
          </div>
          <div className="flex items-center gap-2">
            <span className="border-2 border-[#121324] bg-[#241A08] px-2 py-1 text-[9px] text-[#FFE05C]">
              ● {coinsBank} C
            </span>
            <button
              type="button"
              onClick={onClose}
              className="pixel-btn px-2.5 py-1.5 text-[9px]"
            >
              ✕
            </button>
          </div>
        </div>

        {/* Tabs */}
        <div className="grid grid-cols-2 gap-1.5">
          <button
            type="button"
            onClick={() => setTab('trophies')}
            className={`pixel-btn px-2 py-2 text-[8px] ${
              tab === 'trophies' ? 'pixel-btn-primary' : ''
            }`}
          >
            TROPHIES ({unlockedCount}/{ACHIEVEMENTS_CATALOG.length})
          </button>
          <button
            type="button"
            onClick={() => setTab('stats')}
            className={`pixel-btn px-2 py-2 text-[8px] ${
              tab === 'stats' ? 'pixel-btn-primary' : ''
            }`}
          >
            CAREER STATS
          </button>
        </div>

        {/* Body */}
        <div className="flex-1 space-y-1.5 overflow-y-auto pr-1">
          {tab === 'trophies' ? (
            ACHIEVEMENTS_CATALOG.map((ach) => {
              const unlocked = unlockedSet.has(ach.id);
              const claimed = claimedSet.has(ach.id);
              return (
                <div
                  key={ach.id}
                  className={`flex items-center justify-between gap-2 border-2 border-[#121324] px-2.5 py-2 ${
                    unlocked ? 'bg-[#1D2B4C]' : 'bg-[#12172B] opacity-75'
                  }`}
                >
                  <div className="flex min-w-0 items-center gap-2">
                    <span className="text-base leading-none">{ach.icon}</span>
                    <div className="min-w-0">
                      <p
                        className={`truncate text-[9px] ${
                          unlocked ? 'text-[#FFE05C]' : 'text-[#90A0C4]'
                        }`}
                      >
                        {ach.title}
                      </p>
                      <p className="mt-0.5 text-[7px] leading-relaxed text-[#C2D0F0]">
                        {ach.desc}
                      </p>
                    </div>
                  </div>
                  <div className="shrink-0">
                    {claimed ? (
                      <span className="border-2 border-[#121324] bg-[#163826] px-2 py-1 text-[7px] text-[#78F0A8]">
                        ✓ CLAIMED
                      </span>
                    ) : unlocked ? (
                      <button
                        type="button"
                        onClick={() => onClaim(ach.id)}
                        className="pixel-btn pixel-btn-primary px-2 py-1 text-[8px]"
                      >
                        +{ach.rewardCoins} C
                      </button>
                    ) : (
                      <span className="border-2 border-[#121324] bg-[#1A2038] px-2 py-1 text-[7px] text-[#8090B8]">
                        +{ach.rewardCoins} C
                      </span>
                    )}
                  </div>
                </div>
              );
            })
          ) : (
            <div className="grid grid-cols-2 gap-2">
              {[
                { label: 'TOTAL RUNS', value: String(s.totalRuns), color: '#FFE05C' },
                { label: 'BEST SCORE', value: s.bestScore.toLocaleString(), color: '#78F0A8' },
                { label: 'TOTAL DISTANCE', value: `${s.totalDistance.toLocaleString()}M`, color: '#98D8FF' },
                { label: 'COINS EARNED', value: s.totalCoins.toLocaleString(), color: '#FFE05C' },
                { label: 'ENEMIES STOMPED', value: String(s.totalStomps), color: '#FF9858' },
                { label: 'BOSSES KO', value: String(s.bossesDefeated), color: '#FF6888' },
                { label: 'HIGHEST LEVEL', value: `LV ${s.bestLevel}`, color: '#78F0A8' },
                { label: 'HARDCORE RUNS', value: String(s.hardcoreRuns), color: '#FF5874' },
              ].map((stat) => (
                <div
                  key={stat.label}
                  className="border-2 border-[#121324] bg-[#161E3A] px-2.5 py-2.5 text-center"
                >
                  <p className="text-[7px] text-[#98B0DC]">{stat.label}</p>
                  <p className="mt-1.5 text-xs" style={{ color: stat.color }}>
                    {stat.value}
                  </p>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
