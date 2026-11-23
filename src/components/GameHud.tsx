import React from 'react';
import { CYCLES_PER_LEVEL, LEVEL_NAMES } from '../game/constants';
import type { HudSnapshot, OrientationMode } from '../game/types';
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
  orientation: OrientationMode;
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
  orientation,
  onToggleMute,
  onTogglePause,
  onRestart,
  onTouchJumpStart,
  onTouchJumpEnd,
  onTouchDuckStart,
  onTouchDuckEnd,
}: GameHudProps) {
  const isPortrait = orientation === 'portrait';
  const heartSlots = Array.from({ length: hud.maxHp }, (_, idx) => {
    if (idx < hud.hp) return 'full' as const;
    if (idx === hud.hp && hud.damageFlash) return 'cracked' as const;
    return 'empty' as const;
  });

  return (
    <div
      className="fixed inset-0 z-10 pointer-events-none flex flex-col justify-between safe-area-hud"
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

            <div className="flex flex-wrap items-center gap-x-2.5 gap-y-0.5">
              <span className="font-pixel-display text-[10px] sm:text-xs text-[#78F0A8] pixel-text-shadow tracking-wider whitespace-nowrap">
                LV {hud.level}
                <span className="font-pixel-ui text-[10px] text-[#FFE89C] ml-1">
                  {hud.dayCycleCount % CYCLES_PER_LEVEL}/{CYCLES_PER_LEVEL}
                </span>
              </span>

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

              {hud.doubleScoreSeconds > 0 && (
                <span className="inline-flex items-center gap-1 font-pixel-display text-[10px] sm:text-xs text-[#FFE05C] pixel-text-shadow whitespace-nowrap">
                  2X {hud.doubleScoreSeconds}S
                </span>
              )}

              {hud.slowMoSeconds > 0 && (
                <span className="inline-flex items-center gap-1 font-pixel-display text-[10px] sm:text-xs text-[#78F0FF] pixel-text-shadow whitespace-nowrap">
                  SLOW {hud.slowMoSeconds}S
                </span>
              )}

              {hud.turboDashSeconds > 0 && (
                <span className="inline-flex items-center gap-1 font-pixel-display text-[10px] sm:text-xs text-[#FFE848] pixel-text-shadow whitespace-nowrap">
                  ⚡ DASH {hud.turboDashSeconds}S
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

      {/* Transient level-up or biome arrival callout — unboxed typography in upper sky */}
      {hud.levelBannerTimer > 0 ? (
        <div className="self-center mt-2 text-center">
          <p className="font-pixel-display text-xs sm:text-sm text-[#78F0A8] pixel-text-shadow tracking-widest">
            LEVEL {hud.level} — {LEVEL_NAMES[(hud.level - 1) % LEVEL_NAMES.length]}
          </p>
        </div>
      ) : (
        hud.zoneBannerTimer > 0 && (
          <div className="self-center mt-2">
            <p className="font-pixel-display text-xs sm:text-sm text-[#FFF4A8] pixel-text-shadow tracking-widest">
              {hud.zoneName}
            </p>
          </div>
        )
      )}

      {/* Dedicated touch controls: Portrait = full-width underground deck; Landscape = bottom-right cluster away from runner */}
      <div
        data-testid="touch-controls"
        className={`touch-controls-bar pointer-events-auto ${
          isPortrait
            ? 'flex items-end justify-between pb-4 gap-4'
            : 'flex md:hidden items-end justify-end pb-1 gap-2.5'
        }`}
      >
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
          className={`pixel-btn-secondary font-pixel-display text-xs cursor-pointer ${
            isPortrait
              ? 'flex-1 max-w-[160px] py-4 min-h-[60px]'
              : 'px-4 py-3 min-w-[88px] min-h-[46px] opacity-85'
          }`}
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
          className={`pixel-btn-primary font-pixel-display text-xs cursor-pointer ${
            isPortrait
              ? 'flex-1 max-w-[160px] py-4 min-h-[60px]'
              : 'px-5 py-3 min-w-[96px] min-h-[46px] opacity-85'
          }`}
        >
          JUMP
        </button>
      </div>
    </div>
  );
}
