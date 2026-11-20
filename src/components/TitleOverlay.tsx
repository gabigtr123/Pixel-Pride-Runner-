import React from 'react';
import { CHARACTER_SPRITES } from '../game/sprites';
import type { CharacterId, OrientationMode } from '../game/types';
import { CharacterPreviewIcon, CoinIcon, PixelStrideTitleLogo, SoundIcon } from './PixelIcons';

const CHARACTER_IDS: CharacterId[] = ['stride', 'lacramioara', 'knight'];

interface TitleOverlayProps {
  bestScore: number;
  muted: boolean;
  characterId: CharacterId;
  unlockedCharacters: CharacterId[];
  coinsBank: number;
  orientation: OrientationMode;
  streakDays: number;
  hasUnclaimedDaily: boolean;
  completedMissions: number;
  totalMissions: number;
  onSelectCharacter: (id: CharacterId) => void;
  onOpenDaily: () => void;
  onOpenShop: () => void;
  onStart: () => void;
  onToggleMute: () => void;
}

export function TitleOverlay({
  bestScore,
  muted,
  characterId,
  unlockedCharacters,
  coinsBank,
  orientation,
  streakDays,
  hasUnclaimedDaily,
  completedMissions,
  totalMissions,
  onSelectCharacter,
  onOpenDaily,
  onOpenShop,
  onStart,
  onToggleMute,
}: TitleOverlayProps) {
  const isPortrait = orientation === 'portrait';
  const activeSprite = CHARACTER_SPRITES[characterId] ?? CHARACTER_SPRITES.stride;

  return (
    <div
      className="fixed inset-0 z-20 flex flex-col justify-between pointer-events-none safe-area-hud"
      data-testid="title-screen"
    >
      {/* Top bar: Daily Quests + Pixel Shop on left, Sound toggle on right */}
      <div className="flex items-start justify-between gap-2">
        <div className="flex items-center gap-2 pointer-events-auto">
          <button
            type="button"
            data-testid="open-daily-btn"
            onClick={(e) => {
              e.currentTarget.blur();
              onOpenDaily();
            }}
            tabIndex={-1}
            aria-label="Open Daily Quests and Streak Bonus"
            className="relative pixel-icon-btn h-11 px-2.5 sm:px-3 flex items-center gap-1.5 sm:gap-2 cursor-pointer"
          >
            <span className="font-pixel-display text-[10px] text-[#FFE05C] tracking-wider whitespace-nowrap">
              DAY {streakDays}
            </span>
            <span className="font-pixel-ui text-[11px] text-[#E4EAF8] whitespace-nowrap">
              {completedMissions}/{totalMissions}
            </span>
            {hasUnclaimedDaily && (
              <span
                className="w-2.5 h-2.5 bg-[#78F0A8] border border-[#111324] animate-pulse"
                aria-hidden="true"
              />
            )}
          </button>

          <button
            type="button"
            data-testid="open-shop-btn"
            onClick={(e) => {
              e.currentTarget.blur();
              onOpenShop();
            }}
            tabIndex={-1}
            aria-label="Open Pixel Shop and Leaderboard"
            className="pixel-icon-btn h-11 px-2.5 sm:px-3 flex items-center gap-1.5 cursor-pointer"
          >
            <CoinIcon className="w-4 h-4 shrink-0" />
            <span className="font-pixel-display text-[10px] text-[#FFF4A8] whitespace-nowrap">
              {coinsBank}
            </span>
            <span className="hidden sm:inline font-pixel-ui text-[10px] text-[#78E8FF]">
              SHOP
            </span>
          </button>
        </div>

        <button
          type="button"
          onClick={(e) => {
            e.currentTarget.blur();
            onToggleMute();
          }}
          tabIndex={-1}
          aria-label={muted ? 'Unmute audio (M)' : 'Mute audio (M)'}
          className="pointer-events-auto pixel-icon-btn w-11 h-11 flex items-center justify-center cursor-pointer"
        >
          <SoundIcon muted={muted} />
        </button>
      </div>

      {/* Upper-center calm sky negative-space lockup, character selector & primary CTA */}
      <div
        className={`flex flex-col items-center ${
          isPortrait ? '-mt-24 gap-3.5' : '-mt-8 sm:-mt-16 gap-2.5 sm:gap-3.5'
        }`}
      >
        <PixelStrideTitleLogo />

        {/* Retro 8-bit character selector (Stride, Lacramioara, Pixel Knight) */}
        <div className="flex flex-col items-center gap-1.5 pointer-events-auto">
          <div
            className="flex flex-wrap items-center justify-center gap-2"
            role="radiogroup"
            aria-label="Choose character"
          >
            {CHARACTER_IDS.map((id) => {
              const unlocked = unlockedCharacters.includes(id);
              const selected = characterId === id;
              const info = CHARACTER_SPRITES[id];
              return (
                <button
                  key={id}
                  type="button"
                  role="radio"
                  aria-checked={selected}
                  data-testid={`select-char-${id}`}
                  onClick={(e) => {
                    e.currentTarget.blur();
                    if (unlocked) {
                      onSelectCharacter(id);
                    } else {
                      onOpenShop();
                    }
                  }}
                  className={`flex items-center gap-1.5 px-2.5 py-1.5 cursor-pointer transition-transform duration-75 active:translate-y-0.5 ${
                    selected
                      ? 'bg-[#1F2952] border-[3px] border-[#FFE05C] shadow-[0_4px_0_#0A0C18] text-[#FFF4A8]'
                      : unlocked
                        ? 'bg-[#141830]/90 border-2 border-[#6B79A8] shadow-[0_3px_0_#0A0C18] text-[#C8D2EE] hover:border-[#F4EFE0]'
                        : 'bg-[#111428]/80 border-2 border-[#3C4670] text-[#8A96BE] hover:border-[#FFE05C]'
                  }`}
                >
                  <CharacterPreviewIcon
                    characterId={id}
                    className={`w-6 h-7 shrink-0 ${unlocked ? '' : 'opacity-60'}`}
                  />
                  <span className="font-pixel-display text-[9px] sm:text-[10px] tracking-wider whitespace-nowrap">
                    {unlocked ? info.name : `${info.name} 🔒`}
                  </span>
                </button>
              );
            })}
          </div>
          <span className="font-pixel-ui text-[10px] text-[#78F0A8] bg-[#111324]/75 px-2.5 py-0.5 tracking-wider">
            PERK: {activeSprite.perk}
          </span>
        </div>

        <div className="flex flex-col items-center gap-2 pointer-events-auto">
          <button
            type="button"
            data-start="true"
            onClick={(e) => {
              e.currentTarget.blur();
              onStart();
            }}
            className="pixel-btn-primary font-pixel-display text-sm sm:text-base px-8 py-4 tracking-wider whitespace-nowrap cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-[#FFF4A8]"
          >
            START RUN
          </button>

          {bestScore > 0 && (
            <div className="font-pixel-ui text-xs sm:text-sm text-[#FFF4A8] pixel-text-shadow tracking-wider">
              HIGH SCORE {bestScore.toString().padStart(6, '0')}
            </div>
          )}
        </div>
      </div>

      {/* Flat, borderless single-line control hint in bottom safe margin */}
      <div className="flex justify-center pb-1">
        <p className="font-pixel-ui text-xs text-[#FFF9E8] bg-[#111324]/80 px-4 py-1.5 text-center tracking-wide">
          SPACE / UP Jump &amp; Double-Jump · DOWN Duck / Fast-Fall · P Pause · M Audio
        </p>
      </div>
    </div>
  );
}
