import React from 'react';
import type { HudSnapshot } from '../game/types';
import {
  CoinIcon,
  HeartSocketIcon,
  MagnetIcon,
  PauseIcon,
  RestartIcon,
  ShieldIcon,
  SoundIcon,
} from './PixelIcons';

interface GameHudProps {
  hud: HudSnapshot;
  muted: boolean;
  onToggleMute: () => void;
  onTogglePause: () => void;
  onRestart: () => void;
  onTouchJumpStart: () => void;
  onTouchJumpEnd: () => void;
  onTouchDuckStart: () => void;
  onTouchDuckEnd: () => void;
}

export function GameHud({
  hud,
  muted,
  onToggleMute,
  onTogglePause,
  onRestart,
  onTouchJumpStart,
  onTouchJumpEnd,
  onTouchDuckStart,
  onTouchDuckEnd,
}: GameHudProps) {
  const heartSlots = Array.from({ length: hud.maxHp }, (_, idx) => {
    if (idx < hud.hp) return 'full' as const;
    if (idx === hud.hp && hud.damageFlash) return 'cracked' as const;
    return 'empty' as const;
  });

  return (
    <div
      className="fixed inset-0 z-10 pointer-events-none flex flex-col justify-between p-3 sm:p-5"
      data-testid="game-hud"
    >
      {/* Compact top perimeter HUD strip — keeps >= 85% viewport unobstructed */}
      <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-2 sm:gap-3 w-full">
        {/* Top-left: Vitality heart sockets + mobile utility buttons */}
        <div className="flex items-start justify-between sm:justify-start gap-2">
          <div className="flex flex-col gap-1">
            <div className="flex items-center gap-1.5" aria-label={`Health ${hud.hp} of ${hud.maxHp}`}>
              {heartSlots.map((slotState, index) => (
                <HeartSocketIcon key={index} state={slotState} />
              ))}
            </div>

            <div className="flex items-center gap-2.5">
              <span className="font-pixel-ui text-[11px] sm:text-xs text-[#FFF9E8] pixel-text-shadow tracking-wider whitespace-nowrap">
                {hud.distance}M
                <span className="hidden sm:inline"> · {hud.zoneName}</span>
              </span>

              {hud.shieldActive && (
                <span className="inline-flex items-center gap-1 font-pixel-ui text-[11px] sm:text-xs text-[#78E8FF] pixel-text-shadow whitespace-nowrap">
                  <ShieldIcon className="w-4 h-4" />
                  SHIELD
                </span>
              )}

              {hud.magnetSeconds > 0 && (
                <span className="inline-flex items-center gap-1 font-pixel-ui text-[11px] sm:text-xs text-[#FF98E0] pixel-text-shadow whitespace-nowrap">
                  <MagnetIcon className="w-4 h-4" />
                  {hud.magnetSeconds}S
                </span>
              )}
            </div>
          </div>

          {/* Mobile-only compact utility buttons on row 1 right so nothing clips at 390px */}
          <div className="flex sm:hidden items-center gap-1.5 pointer-events-auto shrink-0">
            <button
              type="button"
              tabIndex={-1}
              onClick={(e) => {
                e.currentTarget.blur();
                onToggleMute();
              }}
              aria-label={muted ? 'Unmute audio (M)' : 'Mute audio (M)'}
              className="pixel-icon-btn w-9 h-9 flex items-center justify-center cursor-pointer"
            >
              <SoundIcon muted={muted} />
            </button>
            <button
              type="button"
              tabIndex={-1}
              onClick={(e) => {
                e.currentTarget.blur();
                onTogglePause();
              }}
              aria-label="Pause game (P)"
              className="pixel-icon-btn w-9 h-9 flex items-center justify-center cursor-pointer"
            >
              <PauseIcon />
            </button>
            <button
              type="button"
              tabIndex={-1}
              onClick={(e) => {
                e.currentTarget.blur();
                onRestart();
              }}
              aria-label="Restart run (R)"
              className="pixel-icon-btn w-9 h-9 flex items-center justify-center cursor-pointer"
            >
              <RestartIcon />
            </button>
          </div>
        </div>

        {/* Top-right: Tabular arcade score readout + desktop utility icons */}
        <div className="flex items-start justify-between sm:justify-end gap-3 sm:gap-5">
          <div className="flex items-end justify-between sm:justify-end w-full sm:w-auto gap-4 sm:gap-7">
            {/* Coins & live combo */}
            <div className="flex flex-col items-start sm:items-end">
              <span className="font-pixel-ui text-[10px] sm:text-[11px] text-[#E6EEFF] pixel-text-shadow tracking-wider">
                COINS
              </span>
              <div className="flex items-center gap-1.5 mt-0.5">
                <CoinIcon className="w-4 h-4" />
                <span
                  className="font-pixel-display text-sm sm:text-base text-[#FFE05C] pixel-text-shadow"
                  data-testid="hud-coins"
                >
                  {hud.coins}
                </span>
                {hud.combo > 1 && (
                  <span className="font-pixel-display text-xs text-[#78F0A8] pixel-text-shadow">
                    x{hud.combo}
                  </span>
                )}
              </div>
            </div>

            {/* Primary Score readout (2.5:1 hero-to-kicker ratio) */}
            <div className="flex flex-col items-end">
              <div className="flex items-center gap-3 font-pixel-ui text-[10px] sm:text-[11px] text-[#E6EEFF] pixel-text-shadow tracking-wider">
                <span>SCORE</span>
                <span className="hidden md:inline text-[#FFE89C]">
                  HI {hud.bestScore.toString().padStart(6, '0')}
                </span>
              </div>
              <span
                className="font-pixel-display text-base sm:text-2xl text-[#FFFFFF] pixel-text-shadow-lg leading-tight mt-0.5"
                data-testid="hud-score"
              >
                {hud.score.toString().padStart(6, '0')}
              </span>
            </div>
          </div>

          {/* Desktop square utility icon buttons */}
          <div className="hidden sm:flex items-center gap-1.5 pointer-events-auto shrink-0">
            <button
              type="button"
              tabIndex={-1}
              onClick={(e) => {
                e.currentTarget.blur();
                onToggleMute();
              }}
              aria-label={muted ? 'Unmute audio (M)' : 'Mute audio (M)'}
              title="Audio (M)"
              className="pixel-icon-btn w-10 h-10 flex items-center justify-center cursor-pointer"
            >
              <SoundIcon muted={muted} />
            </button>
            <button
              type="button"
              tabIndex={-1}
              onClick={(e) => {
                e.currentTarget.blur();
                onTogglePause();
              }}
              aria-label="Pause game (P)"
              title="Pause (P)"
              className="pixel-icon-btn w-10 h-10 flex items-center justify-center cursor-pointer"
            >
              <PauseIcon />
            </button>
            <button
              type="button"
              tabIndex={-1}
              onClick={(e) => {
                e.currentTarget.blur();
                onRestart();
              }}
              aria-label="Restart run (R)"
              title="Restart (R)"
              className="pixel-icon-btn w-10 h-10 flex items-center justify-center cursor-pointer"
            >
              <RestartIcon />
            </button>
          </div>
        </div>
      </div>

      {/* Transient biome arrival callout — unboxed typography in upper sky */}
      {hud.zoneBannerTimer > 0 && (
        <div className="self-center mt-2">
          <p className="font-pixel-display text-xs sm:text-sm text-[#FFF4A8] pixel-text-shadow tracking-widest">
            {hud.zoneName}
          </p>
        </div>
      )}

      {/* Mobile / narrow viewport touch controls (kept tucked at bottom corners) */}
      <div className="flex md:hidden items-end justify-between pb-2 pointer-events-auto">
        <button
          type="button"
          tabIndex={-1}
          onPointerDown={(e) => {
            e.preventDefault();
            onTouchDuckStart();
          }}
          onPointerUp={onTouchDuckEnd}
          onPointerCancel={onTouchDuckEnd}
          onPointerLeave={onTouchDuckEnd}
          aria-label="Duck or Fast-Fall"
          className="pixel-btn-secondary font-pixel-display text-xs px-5 py-4 min-w-[108px] min-h-[52px]"
        >
          DUCK
        </button>

        <button
          type="button"
          tabIndex={-1}
          onPointerDown={(e) => {
            e.preventDefault();
            onTouchJumpStart();
          }}
          onPointerUp={onTouchJumpEnd}
          onPointerCancel={onTouchJumpEnd}
          onPointerLeave={onTouchJumpEnd}
          aria-label="Jump"
          className="pixel-btn-primary font-pixel-display text-xs px-6 py-4 min-w-[116px] min-h-[52px]"
        >
          JUMP
        </button>
      </div>
    </div>
  );
}
