import React from 'react';
import { CHARACTER_SPRITES } from '../game/sprites';
import type { CharacterId } from '../game/types';
import { CharacterPreviewIcon, PixelStrideTitleLogo, SoundIcon } from './PixelIcons';

const CHARACTER_IDS: CharacterId[] = ['stride', 'lacramioara'];

interface TitleOverlayProps {
  bestScore: number;
  muted: boolean;
  characterId: CharacterId;
  onSelectCharacter: (id: CharacterId) => void;
  onStart: () => void;
  onToggleMute: () => void;
}

export function TitleOverlay({
  bestScore,
  muted,
  characterId,
  onSelectCharacter,
  onStart,
  onToggleMute,
}: TitleOverlayProps) {
  return (
    <div
      className="fixed inset-0 z-20 flex flex-col justify-between pointer-events-none safe-area-hud"
      data-testid="title-screen"
    >
      {/* Top right sound toggle only — keeps upper sky clean for the title lockup */}
      <div className="flex items-start justify-end">
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
      <div className="flex flex-col items-center -mt-12 sm:-mt-20 gap-4">
        <PixelStrideTitleLogo />

        {/* Retro 8-bit character selector */}
        <div
          className="flex items-center gap-2.5 sm:gap-3 pointer-events-auto"
          role="radiogroup"
          aria-label="Choose character"
        >
          {CHARACTER_IDS.map((id) => {
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
                  onSelectCharacter(id);
                }}
                className={`flex items-center gap-2 px-3 py-2 cursor-pointer transition-transform duration-75 active:translate-y-0.5 ${
                  selected
                    ? 'bg-[#1F2952] border-[3px] border-[#FFE05C] shadow-[0_4px_0_#0A0C18] text-[#FFF4A8]'
                    : 'bg-[#141830]/90 border-2 border-[#6B79A8] shadow-[0_3px_0_#0A0C18] text-[#C8D2EE] hover:border-[#F4EFE0]'
                }`}
              >
                <CharacterPreviewIcon characterId={id} className="w-7 h-8 shrink-0" />
                <span className="font-pixel-display text-[10px] sm:text-xs tracking-wider whitespace-nowrap">
                  {info.name}
                </span>
              </button>
            );
          })}
        </div>

        <div className="flex flex-col items-center gap-2.5 pointer-events-auto">
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
