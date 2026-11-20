import React from 'react';
import type { ColorOption } from '../game/creator';
import type { DailyMission } from '../game/daily';
import type { HudSnapshot } from '../game/types';
import { CoinIcon } from './PixelIcons';

interface StatusModalProps {
  hud: HudSnapshot;
  newlyCompletedMissions: DailyMission[];
  newlyUnlockedCosmetics: ColorOption[];
  completedMissions: number;
  totalMissions: number;
  onOpenDaily: () => void;
  onResume: () => void;
  onRestart: () => void;
  onTitle: () => void;
}

export function StatusModal({
  hud,
  newlyCompletedMissions,
  newlyUnlockedCosmetics,
  completedMissions,
  totalMissions,
  onOpenDaily,
  onResume,
  onRestart,
  onTitle,
}: StatusModalProps) {
  if (hud.phase !== 'paused' && hud.phase !== 'gameover') {
    return null;
  }

  const isGameOver = hud.phase === 'gameover';
  const summary = hud.finalRun ?? {
    score: hud.score,
    bestScore: hud.bestScore,
    distance: hud.distance,
    coins: hud.coins,
    stomps: 0,
    zoneName: hud.zoneName,
    zoneIndex: hud.zoneIndex,
    isNewBest: false,
  };

  return (
    <div
      className="fixed inset-0 z-30 flex items-center justify-center p-4 bg-[#0B0D1A]/65"
      role="dialog"
      aria-modal="true"
      aria-labelledby="status-modal-title"
      data-testid={isGameOver ? 'gameover-modal' : 'pause-modal'}
    >
      <div className="pixel-panel w-full max-w-md p-6 sm:p-7 flex flex-col items-center text-center gap-4 sm:gap-5">
        <div className="flex flex-col items-center gap-2">
          <h2
            id="status-modal-title"
            className={`font-pixel-display text-xl sm:text-2xl tracking-wider pixel-text-shadow ${
              isGameOver ? 'text-[#FF6878]' : 'text-[#FFE05C]'
            }`}
          >
            {isGameOver ? 'RUN OVER' : 'PAUSED'}
          </h2>

          {isGameOver && summary.isNewBest && (
            <p className="font-pixel-ui text-xs text-[#78F0A8] tracking-widest">
              NEW HIGH SCORE!
            </p>
          )}
          <p className="font-pixel-ui text-xs text-[#B8C4E4] tracking-wider">
            {summary.zoneName}
          </p>
        </div>

        {/* Clean 3-stat tabular readout grid */}
        <div className="w-full grid grid-cols-3 gap-3 py-3 border-y-2 border-[#303A68]">
          <div className="flex flex-col items-center gap-1">
            <span className="font-pixel-ui text-[10px] text-[#A2B0D8] tracking-widest">SCORE</span>
            <span className="font-pixel-display text-sm sm:text-base text-[#FFFFFF]" data-testid="modal-score">
              {summary.score}
            </span>
          </div>

          <div className="flex flex-col items-center gap-1">
            <span className="font-pixel-ui text-[10px] text-[#A2B0D8] tracking-widest">BEST</span>
            <span className="font-pixel-display text-sm sm:text-base text-[#FFE05C]">
              {summary.bestScore}
            </span>
          </div>

          <div className="flex flex-col items-center gap-1">
            <span className="font-pixel-ui text-[10px] text-[#A2B0D8] tracking-widest">DIST</span>
            <span className="font-pixel-display text-sm sm:text-base text-[#78E8FF]">
              {summary.distance}M
            </span>
          </div>
        </div>

        {isGameOver && (
          <div className="w-full flex flex-col items-center gap-2.5">
            <div className="flex items-center justify-center gap-5 font-pixel-ui text-xs text-[#E4EAF8]">
              <span className="inline-flex items-center gap-1.5">
                <CoinIcon className="w-4 h-4" />
                {summary.coins} COINS
              </span>
              <span aria-hidden="true">·</span>
              <span>{summary.stomps} STOMPS</span>
            </div>

            {newlyCompletedMissions.length > 0 && (
              <div className="w-full bg-[#142A26] border-2 border-[#48C878] px-3 py-2 flex flex-col gap-1">
                <span className="font-pixel-display text-[10px] text-[#78F0A8]">
                  MISSION COMPLETE!
                </span>
                {newlyCompletedMissions.map((m) => (
                  <span key={m.id} className="font-pixel-ui text-[11px] text-[#E8FFF2]">
                    ✓ {m.title} (+{m.rewardPoints} PTS)
                  </span>
                ))}
              </div>
            )}

            {newlyUnlockedCosmetics.length > 0 && (
              <div className="w-full bg-[#241D3E] border-2 border-[#FFE05C] px-3 py-2 flex flex-col gap-1">
                <span className="font-pixel-display text-[10px] text-[#FFE05C]">
                  HERO COSMETIC UNLOCKED!
                </span>
                {newlyUnlockedCosmetics.map((c) => (
                  <span key={c.id} className="font-pixel-ui text-[11px] text-[#FFF4A8]">
                    ★ {c.name} (Available in Hero Creator)
                  </span>
                ))}
              </div>
            )}

            <button
              type="button"
              onClick={(e) => {
                e.currentTarget.blur();
                onOpenDaily();
              }}
              className="font-pixel-ui text-[11px] text-[#FFE05C] underline underline-offset-4 cursor-pointer hover:text-[#FFF4A8]"
            >
              DAILY MISSIONS: {completedMissions}/{totalMissions} — VIEW
            </button>
          </div>
        )}

        {/* Primary & secondary CTAs */}
        <div className="w-full flex flex-col sm:flex-row items-center justify-center gap-3 pt-1">
          {isGameOver ? (
            <button
              type="button"
              data-start="true"
              onClick={(e) => {
                e.currentTarget.blur();
                onRestart();
              }}
              className="pixel-btn-primary w-full sm:w-auto font-pixel-display text-xs sm:text-sm px-6 py-3.5 whitespace-nowrap cursor-pointer"
            >
              TRY AGAIN
            </button>
          ) : (
            <button
              type="button"
              onClick={(e) => {
                e.currentTarget.blur();
                onResume();
              }}
              className="pixel-btn-primary w-full sm:w-auto font-pixel-display text-xs sm:text-sm px-6 py-3.5 whitespace-nowrap cursor-pointer"
            >
              RESUME
            </button>
          )}

          {!isGameOver && (
            <button
              type="button"
              onClick={(e) => {
                e.currentTarget.blur();
                onRestart();
              }}
              className="pixel-btn-secondary w-full sm:w-auto font-pixel-display text-xs px-5 py-3.5 whitespace-nowrap cursor-pointer"
            >
              RESTART
            </button>
          )}

          <button
            type="button"
            onClick={(e) => {
              e.currentTarget.blur();
              onTitle();
            }}
            className="pixel-btn-secondary w-full sm:w-auto font-pixel-display text-xs px-5 py-3.5 whitespace-nowrap cursor-pointer"
          >
            TITLE
          </button>
        </div>
      </div>
    </div>
  );
}
