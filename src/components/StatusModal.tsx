import React, { useEffect, useRef, useState } from 'react';
import type { ColorOption } from '../game/creator';
import type { DailyMission } from '../game/daily';
import { renderRunCardToCanvas, runCardCanvasToBlob } from '../game/runCard';
import type { CapeId, CustomCharacterConfig, HatId, HudSnapshot, PetId } from '../game/types';
import { CoinIcon } from './PixelIcons';

interface StatusModalProps {
  hud: HudSnapshot;
  customCharacter: CustomCharacterConfig;
  hatId: HatId;
  capeId: CapeId;
  petId: PetId;
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
  customCharacter,
  hatId,
  capeId,
  petId,
  newlyCompletedMissions,
  newlyUnlockedCosmetics,
  completedMissions,
  totalMissions,
  onOpenDaily,
  onResume,
  onRestart,
  onTitle,
}: StatusModalProps) {
  const [showCard, setShowCard] = useState(false);
  const [cardStatus, setCardStatus] = useState<string | null>(null);
  const cardCanvasRef = useRef<HTMLCanvasElement | null>(null);

  const isGameOver = hud.phase === 'gameover';
  const summary = hud.finalRun ?? {
    score: hud.score,
    bestScore: hud.bestScore,
    distance: hud.distance,
    coins: hud.coins,
    stomps: 0,
    bossesDefeated: 0,
    gameMode: hud.gameMode,
    zoneName: hud.zoneName,
    zoneIndex: hud.zoneIndex,
    level: hud.level,
    isNewBest: false,
  };

  useEffect(() => {
    if (!isGameOver) {
      setShowCard(false);
      setCardStatus(null);
    }
  }, [isGameOver]);

  useEffect(() => {
    if (!showCard || !cardCanvasRef.current) return;
    renderRunCardToCanvas(cardCanvasRef.current, {
      summary,
      characterId: hud.characterId,
      customCharacter,
      hatId,
      capeId,
      petId,
    });
  }, [showCard, summary, hud.characterId, customCharacter, hatId, capeId, petId]);

  if (hud.phase !== 'paused' && hud.phase !== 'gameover') {
    return null;
  }

  const triggerDownload = (blob: Blob) => {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `pixel-stride-card-${summary.score}.png`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
    setCardStatus('SAVED PNG!');
    window.setTimeout(() => setCardStatus(null), 2200);
  };

  const handleSaveCardPng = async () => {
    if (!cardCanvasRef.current) return;
    const blob = await runCardCanvasToBlob(cardCanvasRef.current);
    if (blob) triggerDownload(blob);
  };

  const handleShareCardPng = async () => {
    if (!cardCanvasRef.current) return;
    const blob = await runCardCanvasToBlob(cardCanvasRef.current);
    if (!blob) return;
    const file = new File([blob], `pixel-stride-card-${summary.score}.png`, {
      type: 'image/png',
    });
    const text = `🏃 Pixel Stride — Score ${summary.score.toLocaleString()} · LV ${summary.level} (${summary.distance}m)!`;
    try {
      if (
        typeof navigator !== 'undefined' &&
        typeof navigator.canShare === 'function' &&
        navigator.canShare({ files: [file] })
      ) {
        await navigator.share({ title: 'Pixel Stride Run Card', text, files: [file] });
        return;
      }
      // Fallback: download PNG card directly so user can attach it to X / Discord
      triggerDownload(blob);
    } catch {
      // User dismissed share sheet
    }
  };

  if (isGameOver && showCard) {
    return (
      <div
        className="fixed inset-0 z-30 flex items-center justify-center modal-safe-area bg-[#0B0D1A]/80"
        role="dialog"
        aria-modal="true"
        aria-label="Shareable Run Card"
        data-testid="run-card-modal"
      >
        <div className="pixel-panel w-full max-w-sm max-h-full overflow-y-auto p-3.5 sm:p-4 flex flex-col items-center gap-3">
          <canvas
            ref={cardCanvasRef}
            className="w-full max-w-[270px] sm:max-w-[300px] h-auto border-2 border-[#121324] shadow-[0_6px_0_#0A0C18]"
            style={{ imageRendering: 'pixelated' }}
          />
          {cardStatus && (
            <span className="font-pixel-display text-[9px] text-[#78F0A8]">{cardStatus}</span>
          )}
          <div className="w-full grid grid-cols-2 gap-2">
            <button
              type="button"
              onClick={(e) => {
                e.currentTarget.blur();
                void handleShareCardPng();
              }}
              className="pixel-btn-primary py-2.5 px-2 font-pixel-display text-[9px] sm:text-[10px] cursor-pointer"
            >
              📤 SHARE PNG
            </button>
            <button
              type="button"
              onClick={(e) => {
                e.currentTarget.blur();
                void handleSaveCardPng();
              }}
              className="pixel-btn-secondary py-2.5 px-2 font-pixel-display text-[9px] sm:text-[10px] text-[#FFE05C] cursor-pointer"
            >
              ⬇ SAVE PNG
            </button>
          </div>
          <button
            type="button"
            onClick={(e) => {
              e.currentTarget.blur();
              setShowCard(false);
            }}
            className="font-pixel-ui text-[11px] text-[#B8C4E4] underline underline-offset-4 cursor-pointer hover:text-[#FFFFFF]"
          >
            ← BACK TO SUMMARY
          </button>
        </div>
      </div>
    );
  }

  return (
    <div
      className="fixed inset-0 z-30 flex items-center justify-center modal-safe-area bg-[#0B0D1A]/65"
      role="dialog"
      aria-modal="true"
      aria-labelledby="status-modal-title"
      data-testid={isGameOver ? 'gameover-modal' : 'pause-modal'}
    >
      <div className="pixel-panel w-full max-w-md max-h-full overflow-y-auto p-5 sm:p-7 flex flex-col items-center text-center gap-4 sm:gap-5">
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
            LV {summary.level} · {summary.zoneName}
            {summary.gameMode !== 'endless' ? ` · ${summary.gameMode.toUpperCase()}` : ''}
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
            <div className="flex flex-wrap items-center justify-center gap-3 font-pixel-ui text-xs text-[#E4EAF8]">
              <span className="inline-flex items-center gap-1.5">
                <CoinIcon className="w-4 h-4" />
                {summary.coins} COINS
              </span>
              <span aria-hidden="true">·</span>
              <span>{summary.stomps} STOMPS</span>
              {summary.bossesDefeated > 0 && (
                <>
                  <span aria-hidden="true">·</span>
                  <span className="text-[#FFE05C]">🐉 {summary.bossesDefeated} BOSS KO</span>
                </>
              )}
            </div>

            <button
              type="button"
              data-testid="open-run-card-btn"
              onClick={(e) => {
                e.currentTarget.blur();
                setShowCard(true);
              }}
              className="pixel-btn-secondary px-4 py-2 font-pixel-display text-[9px] text-[#FFE05C] cursor-pointer"
            >
              🃏 VIEW &amp; SHARE RUN CARD
            </button>

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
