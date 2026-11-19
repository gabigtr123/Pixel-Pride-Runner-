import React from 'react';
import { PixelStrideTitleLogo, SoundIcon } from './PixelIcons';

interface TitleOverlayProps {
  bestScore: number;
  muted: boolean;
  onStart: () => void;
  onToggleMute: () => void;
}

export function TitleOverlay({ bestScore, muted, onStart, onToggleMute }: TitleOverlayProps) {
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

      {/* Upper-center calm sky negative-space lockup & single primary CTA */}
      <div className="flex flex-col items-center -mt-20 sm:-mt-28 gap-5">
        <PixelStrideTitleLogo />

        <div className="flex flex-col items-center gap-3 pointer-events-auto">
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
