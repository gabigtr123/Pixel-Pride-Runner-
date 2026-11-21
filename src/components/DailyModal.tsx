import React from 'react';
import {
  computeActiveRunBonus,
  getStreakPerks,
  type DailyProgressState,
} from '../game/daily';
import { MagnetIcon, ShieldIcon } from './PixelIcons';

interface DailyModalProps {
  open: boolean;
  daily: DailyProgressState;
  onClaim: () => void;
  onClose: () => void;
}

export function DailyModal({ open, daily, onClaim, onClose }: DailyModalProps) {
  if (!open) return null;

  const todayPerks = getStreakPerks(daily.streakDays);
  const activeBonus = computeActiveRunBonus(daily);
  const completedCount = daily.missions.filter((m) => m.completed).length;
  const allCleared = completedCount === daily.missions.length && daily.missions.length > 0;

  return (
    <div
      className="fixed inset-0 z-40 flex items-center justify-center modal-safe-area bg-[#0B0D1A]/75"
      role="dialog"
      aria-modal="true"
      aria-labelledby="daily-modal-title"
      data-testid="daily-modal"
    >
      <div className="pixel-panel w-full max-w-md max-h-full overflow-y-auto p-4 sm:p-6 flex flex-col gap-4">
        {/* Header */}
        <div className="flex items-center justify-between gap-2 border-b-2 border-[#303A68] pb-3">
          <div className="flex flex-col">
            <h2
              id="daily-modal-title"
              className="font-pixel-display text-sm sm:text-base text-[#FFE05C] pixel-text-shadow tracking-wider"
            >
              DAILY QUESTS
            </h2>
            <span className="font-pixel-ui text-[11px] text-[#A2B0D8] tracking-wider mt-0.5">
              STREAK: DAY {daily.streakDays}
            </span>
          </div>

          <button
            type="button"
            onClick={onClose}
            aria-label="Close Daily Quests"
            className="pixel-icon-btn px-2.5 py-1.5 font-pixel-display text-[10px] cursor-pointer"
          >
            X
          </button>
        </div>

        {/* Daily Streak Bonus Section */}
        <div className="bg-[#12162D] border-2 border-[#3A467A] p-3 flex flex-col gap-2.5">
          <div className="flex items-center justify-between gap-2">
            <span className="font-pixel-display text-[10px] sm:text-xs text-[#FFF4A8]">
              DAILY PLAY BONUS
            </span>
            <span className="font-pixel-ui text-[11px] text-[#78F0A8]">
              +{todayPerks.startingScoreBonus} START PTS
            </span>
          </div>

          {/* 7-day visual streak strip */}
          <div className="grid grid-cols-7 gap-1">
            {[1, 2, 3, 4, 5, 6, 7].map((day) => {
              const reached = daily.streakDays >= day;
              const isCurrent = Math.min(7, daily.streakDays) === day;
              return (
                <div
                  key={day}
                  className={`flex flex-col items-center justify-center py-1.5 border text-[9px] font-pixel-ui ${
                    isCurrent
                      ? 'bg-[#293568] border-[#FFE05C] text-[#FFF4A8]'
                      : reached
                        ? 'bg-[#1D3B36] border-[#48C878] text-[#9CF0B8]'
                        : 'bg-[#171B34] border-[#2C3560] text-[#7582AC]'
                  }`}
                >
                  <span>D{day}</span>
                  <span className="text-[8px] mt-0.5">
                    {day >= 5 ? 'MAG' : day >= 3 ? 'SHD' : 'PTS'}
                  </span>
                </div>
              );
            })}
          </div>

          {/* Active streak perks preview */}
          <div className="flex flex-wrap items-center gap-2 font-pixel-ui text-[10px] text-[#D8E2FA]">
            <span>DAY 3+: SHIELD</span>
            <span>·</span>
            <span>DAY 5+: MAGNET</span>
            {todayPerks.startWithShield && (
              <span className="inline-flex items-center gap-1 text-[#78E8FF]">
                <ShieldIcon className="w-3.5 h-3.5" />
                ON
              </span>
            )}
            {todayPerks.startMagnetSeconds > 0 && (
              <span className="inline-flex items-center gap-1 text-[#FF98E0]">
                <MagnetIcon className="w-3.5 h-3.5" />
                {todayPerks.startMagnetSeconds}S
              </span>
            )}
          </div>

          {daily.claimedToday ? (
            <div className="text-center font-pixel-display text-[10px] text-[#78F0A8] py-1.5 bg-[#162C28] border border-[#38885C]">
              CLAIMED TODAY ✓ (+{activeBonus.startingScoreBonus} PTS PER RUN)
            </div>
          ) : (
            <button
              type="button"
              data-testid="claim-daily-btn"
              onClick={onClaim}
              className="pixel-btn-primary w-full font-pixel-display text-[11px] py-2.5 cursor-pointer"
            >
              CLAIM DAY {daily.streakDays} BONUS
            </button>
          )}
        </div>

        {/* 3 Daily Missions */}
        <div className="flex flex-col gap-2">
          <div className="flex items-center justify-between">
            <span className="font-pixel-display text-[10px] sm:text-xs text-[#E4EAF8]">
              TODAY&apos;S MISSIONS ({completedCount}/{daily.missions.length})
            </span>
            <span className="font-pixel-ui text-[10px] text-[#FFE05C]">
              {allCleared ? 'ALL CLEAR +1000!' : '3/3 = +1000 PTS'}
            </span>
          </div>

          {daily.missions.map((m) => {
            const pct = Math.min(100, Math.round((m.progress / Math.max(1, m.target)) * 100));
            return (
              <div
                key={m.id}
                className={`p-2.5 border-2 flex flex-col gap-1.5 ${
                  m.completed
                    ? 'bg-[#142A26] border-[#48C878]'
                    : 'bg-[#12162D] border-[#303A68]'
                }`}
              >
                <div className="flex items-center justify-between gap-2">
                  <span className="font-pixel-ui text-xs text-[#F8F6EE]">{m.title}</span>
                  <span
                    className={`font-pixel-display text-[9px] whitespace-nowrap ${
                      m.completed ? 'text-[#78F0A8]' : 'text-[#FFE05C]'
                    }`}
                  >
                    {m.completed ? 'DONE ✓' : `+${m.rewardPoints}`}
                  </span>
                </div>

                <div className="w-full h-2 bg-[#0B0E1D] border border-[#2C3560] overflow-hidden">
                  <div
                    className={`h-full ${m.completed ? 'bg-[#48C878]' : 'bg-[#F0A828]'}`}
                    style={{ width: `${pct}%` }}
                  />
                </div>

                <div className="flex justify-end font-pixel-ui text-[10px] text-[#A2B0D8]">
                  {m.progress} / {m.target}
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
