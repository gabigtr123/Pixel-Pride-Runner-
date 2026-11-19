import React, { useCallback, useEffect, useRef, useState } from 'react';
import { DailyModal } from './components/DailyModal';
import { GameHud } from './components/GameHud';
import { StatusModal } from './components/StatusModal';
import { TitleOverlay } from './components/TitleOverlay';
import { ChiptuneAudio } from './game/audio';
import {
  BEST_SCORE_STORAGE_KEY,
  CHARACTER_STORAGE_KEY,
  MUTED_STORAGE_KEY,
} from './game/constants';
import {
  applyRunToDailyMissions,
  claimDailyBonus,
  computeActiveRunBonus,
  getLocalDateKey,
  loadDailyProgress,
  saveDailyProgress,
  syncDailyStateForDate,
  type DailyMission,
  type DailyProgressState,
} from './game/daily';
import {
  createInitialState,
  extractHudSnapshot,
  returnToTitle,
  startNewRun,
  stepSimulation,
} from './game/engine';
import { getRenderViewport, renderGameWorld } from './game/renderer';
import type { CharacterId, GameState, HudSnapshot, InputFrame, OrientationMode } from './game/types';

function detectOrientation(): OrientationMode {
  if (typeof window === 'undefined') return 'landscape';
  return window.innerHeight > window.innerWidth ? 'portrait' : 'landscape';
}

function loadBestScore(): number {
  try {
    const raw = window.localStorage.getItem(BEST_SCORE_STORAGE_KEY);
    const parsed = raw ? parseInt(raw, 10) : 0;
    return Number.isFinite(parsed) && parsed > 0 ? parsed : 0;
  } catch {
    return 0;
  }
}

function saveBestScore(score: number): void {
  try {
    window.localStorage.setItem(BEST_SCORE_STORAGE_KEY, String(Math.round(score)));
  } catch {
    // Ignore storage failures in sandboxed frames
  }
}

function loadMuted(): boolean {
  try {
    return window.localStorage.getItem(MUTED_STORAGE_KEY) === '1';
  } catch {
    return false;
  }
}

function saveMuted(muted: boolean): void {
  try {
    window.localStorage.setItem(MUTED_STORAGE_KEY, muted ? '1' : '0');
  } catch {
    // Ignore storage failures in sandboxed frames
  }
}

function loadCharacter(): CharacterId {
  try {
    const raw = window.localStorage.getItem(CHARACTER_STORAGE_KEY);
    return raw === 'lacramioara' ? 'lacramioara' : 'stride';
  } catch {
    return 'stride';
  }
}

function saveCharacter(id: CharacterId): void {
  try {
    window.localStorage.setItem(CHARACTER_STORAGE_KEY, id);
  } catch {
    // Ignore storage failures in sandboxed frames
  }
}

function shouldAutostart(): boolean {
  try {
    const params = new URLSearchParams(window.location.search);
    return params.get('autostart') === '1';
  } catch {
    return false;
  }
}

function hudChanged(a: HudSnapshot, b: HudSnapshot): boolean {
  return (
    a.phase !== b.phase ||
    a.characterId !== b.characterId ||
    a.score !== b.score ||
    a.bestScore !== b.bestScore ||
    a.distance !== b.distance ||
    a.coins !== b.coins ||
    a.combo !== b.combo ||
    a.hp !== b.hp ||
    a.damageFlash !== b.damageFlash ||
    a.shieldActive !== b.shieldActive ||
    a.magnetSeconds !== b.magnetSeconds ||
    a.zoneIndex !== b.zoneIndex ||
    (a.zoneBannerTimer > 0) !== (b.zoneBannerTimer > 0) ||
    a.finalRun !== b.finalRun
  );
}

export default function App() {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const audioRef = useRef<ChiptuneAudio | null>(null);
  const stateRef = useRef<GameState | null>(null);

  // Unified input hub tracking continuous held intents and single-frame edge triggers
  const inputRef = useRef<{
    jumpHeld: boolean;
    jumpQueued: boolean;
    jumpReleaseQueued: boolean;
    duckHeld: boolean;
    duckQueued: boolean;
    keysHeld: Set<string>;
  }>({
    jumpHeld: false,
    jumpQueued: false,
    jumpReleaseQueued: false,
    duckHeld: false,
    duckQueued: false,
    keysHeld: new Set<string>(),
  });

  const [muted, setMuted] = useState<boolean>(() => (typeof window !== 'undefined' ? loadMuted() : false));
  const [orientation, setOrientation] = useState<OrientationMode>(() => detectOrientation());
  const orientationRef = useRef<OrientationMode>(orientation);
  orientationRef.current = orientation;
  const [daily, setDaily] = useState<DailyProgressState>(() =>
    typeof window !== 'undefined' ? loadDailyProgress() : syncDailyStateForDate(null, getLocalDateKey()),
  );
  const dailyRef = useRef<DailyProgressState>(daily);
  dailyRef.current = daily;
  const [dailyOpen, setDailyOpen] = useState(false);
  const [newlyCompletedMissions, setNewlyCompletedMissions] = useState<DailyMission[]>([]);
  const lastRecordedRunRef = useRef<GameState['finalRun']>(null);
  const [hud, setHud] = useState<HudSnapshot>(() => {
    const initialPhase = typeof window !== 'undefined' && shouldAutostart() ? 'playing' : 'title';
    const initial = createInitialState(
      typeof window !== 'undefined' ? loadBestScore() : 0,
      initialPhase,
      typeof window !== 'undefined' ? loadCharacter() : 'stride',
    );
    return extractHudSnapshot(initial);
  });

  const hudRef = useRef<HudSnapshot>(hud);

  const syncDomAndHud = useCallback((nextState: GameState, force = false) => {
    if (document.body.dataset.state !== nextState.phase) {
      document.body.dataset.state = nextState.phase;
    }
    if (
      nextState.phase === 'gameover' &&
      nextState.finalRun &&
      nextState.finalRun !== lastRecordedRunRef.current
    ) {
      lastRecordedRunRef.current = nextState.finalRun;
      const synced = syncDailyStateForDate(dailyRef.current, getLocalDateKey());
      const { next: updatedDaily, newlyCompleted } = applyRunToDailyMissions(synced, {
        score: nextState.finalRun.score,
        distance: nextState.finalRun.distance,
        coins: nextState.finalRun.coins,
        stomps: nextState.finalRun.stomps,
        zoneIndex: nextState.finalRun.zoneIndex,
      });
      dailyRef.current = updatedDaily;
      setDaily(updatedDaily);
      saveDailyProgress(updatedDaily);
      setNewlyCompletedMissions(newlyCompleted);
      if (newlyCompleted.length > 0) {
        audioRef.current?.playEvents([{ type: 'powerup', kind: 'shield' }]);
      }
    }
    const nextHud = extractHudSnapshot(nextState);
    if (force || hudChanged(hudRef.current, nextHud)) {
      hudRef.current = nextHud;
      setHud(nextHud);
    }
  }, []);

  const clearHeldInputs = useCallback(() => {
    const inp = inputRef.current;
    inp.keysHeld.clear();
    inp.jumpHeld = false;
    inp.jumpQueued = false;
    inp.jumpReleaseQueued = false;
    inp.duckHeld = false;
    inp.duckQueued = false;
  }, []);

  const handleStartOrRestart = useCallback(() => {
    const curr = stateRef.current;
    if (!curr) return;
    clearHeldInputs();
    setDailyOpen(false);
    setNewlyCompletedMissions([]);
    if (document.activeElement instanceof HTMLElement) {
      document.activeElement.blur();
    }
    const syncedDaily = syncDailyStateForDate(dailyRef.current, getLocalDateKey());
    if (syncedDaily !== dailyRef.current) {
      dailyRef.current = syncedDaily;
      setDaily(syncedDaily);
      saveDailyProgress(syncedDaily);
    }
    const bonus = computeActiveRunBonus(syncedDaily);
    const next = startNewRun(curr, bonus);
    stateRef.current = next;
    saveBestScore(next.bestScore);
    audioRef.current?.playEvents(next.audioQueue);
    audioRef.current?.startMusic();
    syncDomAndHud(next, true);
  }, [clearHeldInputs, syncDomAndHud]);

  const handleClaimDaily = useCallback(() => {
    const synced = syncDailyStateForDate(dailyRef.current, getLocalDateKey());
    const claimed = claimDailyBonus(synced);
    dailyRef.current = claimed;
    setDaily(claimed);
    saveDailyProgress(claimed);
    audioRef.current?.playEvents([{ type: 'powerup', kind: 'shield' }]);
  }, []);

  const handleTogglePause = useCallback(() => {
    const curr = stateRef.current;
    if (!curr) return;
    if (curr.phase === 'playing') {
      curr.phase = 'paused';
      clearHeldInputs();
      audioRef.current?.stopMusic();
      syncDomAndHud(curr, true);
    } else if (curr.phase === 'paused') {
      curr.phase = 'playing';
      clearHeldInputs();
      if (document.activeElement instanceof HTMLElement) {
        document.activeElement.blur();
      }
      audioRef.current?.startMusic();
      syncDomAndHud(curr, true);
    }
  }, [clearHeldInputs, syncDomAndHud]);

  const handleReturnToTitle = useCallback(() => {
    const curr = stateRef.current;
    if (!curr) return;
    clearHeldInputs();
    audioRef.current?.stopMusic();
    const next = returnToTitle(curr);
    stateRef.current = next;
    saveBestScore(next.bestScore);
    syncDomAndHud(next, true);
  }, [clearHeldInputs, syncDomAndHud]);

  const handleSelectCharacter = useCallback(
    (id: CharacterId) => {
      saveCharacter(id);
      const curr = stateRef.current;
      if (!curr) return;
      curr.characterId = id;
      syncDomAndHud(curr, true);
    },
    [syncDomAndHud],
  );

  const handleToggleMute = useCallback(() => {
    const audio = audioRef.current;
    if (!audio) return;
    const nowMuted = audio.toggleMute();
    setMuted(nowMuted);
    saveMuted(nowMuted);
    if (!nowMuted && stateRef.current?.phase === 'playing') {
      audio.startMusic();
    }
  }, []);

  // Store latest handlers in refs so the mount-once game loop & keyboard listeners never tear down
  const handlersRef = useRef({
    handleStartOrRestart,
    handleTogglePause,
    handleToggleMute,
    clearHeldInputs,
    syncDomAndHud,
  });
  handlersRef.current = {
    handleStartOrRestart,
    handleTogglePause,
    handleToggleMute,
    clearHeldInputs,
    syncDomAndHud,
  };

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d', { alpha: false });
    if (!ctx) return;

    const audio = new ChiptuneAudio();
    audio.setMuted(loadMuted());
    audioRef.current = audio;

    const autoStart = shouldAutostart();
    const initialBest = loadBestScore();
    const initialCharacter = loadCharacter();
    const initialState = createInitialState(initialBest, autoStart ? 'playing' : 'title', initialCharacter);
    stateRef.current = initialState;
    document.body.dataset.state = initialState.phase;
    handlersRef.current.syncDomAndHud(initialState, true);

    const isJumpCode = (code: string) =>
      code === 'Space' || code === 'ArrowUp' || code === 'KeyW';
    const isDuckCode = (code: string) => code === 'ArrowDown' || code === 'KeyS';

    const onKeyDown = (e: KeyboardEvent) => {
      const st = stateRef.current;
      if (!st) return;

      if (e.code === 'KeyM') {
        e.preventDefault();
        handlersRef.current.handleToggleMute();
        return;
      }

      if (e.code === 'KeyP' || e.code === 'Escape') {
        if (st.phase === 'playing' || st.phase === 'paused') {
          e.preventDefault();
          handlersRef.current.handleTogglePause();
        }
        return;
      }

      if (e.code === 'KeyR') {
        if (st.phase === 'playing' || st.phase === 'paused' || st.phase === 'gameover') {
          e.preventDefault();
          handlersRef.current.handleStartOrRestart();
        }
        return;
      }

      // Title, Paused & Game Over keyboard handshake
      if (st.phase === 'paused' && (e.code === 'Enter' || e.code === 'Space')) {
        e.preventDefault();
        handlersRef.current.handleTogglePause();
        return;
      }

      if (st.phase === 'title' && (e.code === 'Enter' || e.code === 'Space')) {
        e.preventDefault();
        handlersRef.current.handleStartOrRestart();
        return;
      }

      if (st.phase === 'gameover' && (e.code === 'Enter' || e.code === 'Space')) {
        e.preventDefault();
        handlersRef.current.handleStartOrRestart();
        return;
      }

      if (st.phase !== 'playing') return;

      const inp = inputRef.current;
      if (isJumpCode(e.code)) {
        e.preventDefault();
        if (!inp.keysHeld.has(e.code)) {
          inp.keysHeld.add(e.code);
          inp.jumpHeld = true;
          inp.jumpQueued = true;
        }
      } else if (isDuckCode(e.code)) {
        e.preventDefault();
        if (!inp.keysHeld.has(e.code)) {
          inp.keysHeld.add(e.code);
          inp.duckHeld = true;
          inp.duckQueued = true;
        }
      }
    };

    const onKeyUp = (e: KeyboardEvent) => {
      const inp = inputRef.current;
      inp.keysHeld.delete(e.code);
      if (isJumpCode(e.code)) {
        const stillHoldingJump =
          inp.keysHeld.has('Space') || inp.keysHeld.has('ArrowUp') || inp.keysHeld.has('KeyW');
        if (!stillHoldingJump && inp.jumpHeld) {
          inp.jumpHeld = false;
          inp.jumpReleaseQueued = true;
        }
      } else if (isDuckCode(e.code)) {
        inp.duckHeld = inp.keysHeld.has('ArrowDown') || inp.keysHeld.has('KeyS');
      }
    };

    const onFocusLoss = () => {
      handlersRef.current.clearHeldInputs();
    };

    const onVisibility = () => {
      if (document.hidden) {
        handlersRef.current.clearHeldInputs();
        audio.stopMusic();
        const st = stateRef.current;
        if (st && st.phase === 'playing') {
          st.phase = 'paused';
          handlersRef.current.syncDomAndHud(st, true);
        }
      }
    };

    let lastRenderedMode: OrientationMode = orientationRef.current;

    const syncCanvasSize = (mode: OrientationMode) => {
      const vp = getRenderViewport(mode);
      if (canvas.width !== vp.width || canvas.height !== vp.height) {
        canvas.width = vp.width;
        canvas.height = vp.height;
      }
      document.body.dataset.orientation = mode;
    };

    syncCanvasSize(orientationRef.current);

    const onResize = () => {
      const nextMode = detectOrientation();
      if (nextMode !== orientationRef.current) {
        orientationRef.current = nextMode;
        setOrientation(nextMode);
        syncCanvasSize(nextMode);
      }
    };

    window.addEventListener('keydown', onKeyDown);
    window.addEventListener('keyup', onKeyUp);
    window.addEventListener('blur', onFocusLoss);
    window.addEventListener('resize', onResize);
    window.addEventListener('orientationchange', onResize);
    document.addEventListener('visibilitychange', onVisibility);

    let rafId = 0;
    let lastTime = performance.now();
    let persistedBest = initialBest;
    let lastRenderedPhase = '';

    const frame = (now: number) => {
      const rawDt = (now - lastTime) / 1000;
      lastTime = now;

      const st = stateRef.current;
      if (st) {
        const mode = orientationRef.current;
        const isFrozen = st.phase === 'paused' || st.phase === 'gameover';
        if (!isFrozen) {
          const inp = inputRef.current;
          const inputFrame: InputFrame = {
            jumpHeld: inp.jumpHeld,
            jumpJustPressed: inp.jumpQueued,
            jumpJustReleased: inp.jumpReleaseQueued,
            duckHeld: inp.duckHeld,
            duckJustPressed: inp.duckQueued,
          };
          inp.jumpQueued = false;
          inp.jumpReleaseQueued = false;
          inp.duckQueued = false;

          stepSimulation(st, rawDt, inputFrame);
          audio.playEvents(st.audioQueue);

          if (st.bestScore > persistedBest) {
            persistedBest = st.bestScore;
            saveBestScore(persistedBest);
          }

          syncCanvasSize(mode);
          renderGameWorld(ctx, st, mode);
          lastRenderedPhase = st.phase;
          lastRenderedMode = mode;
          handlersRef.current.syncDomAndHud(st);
        } else if (lastRenderedPhase !== st.phase || lastRenderedMode !== mode) {
          syncCanvasSize(mode);
          renderGameWorld(ctx, st, mode);
          lastRenderedPhase = st.phase;
          lastRenderedMode = mode;
          handlersRef.current.syncDomAndHud(st);
        }
      }

      rafId = window.requestAnimationFrame(frame);
    };

    rafId = window.requestAnimationFrame(frame);

    return () => {
      window.cancelAnimationFrame(rafId);
      window.removeEventListener('keydown', onKeyDown);
      window.removeEventListener('keyup', onKeyUp);
      window.removeEventListener('blur', onFocusLoss);
      window.removeEventListener('resize', onResize);
      window.removeEventListener('orientationchange', onResize);
      document.removeEventListener('visibilitychange', onVisibility);
      audio.dispose();
    };
  }, []);

  // Touch & pointer canvas tap-to-jump for effortless desktop/mobile play
  const handleTouchJumpStart = useCallback(() => {
    const st = stateRef.current;
    if (!st || st.phase !== 'playing') return;
    const inp = inputRef.current;
    inp.jumpHeld = true;
    inp.jumpQueued = true;
  }, []);

  const handleTouchJumpEnd = useCallback(() => {
    const inp = inputRef.current;
    if (inp.jumpHeld) {
      inp.jumpHeld = false;
      inp.jumpReleaseQueued = true;
    }
  }, []);

  const handleTouchDuckStart = useCallback(() => {
    const st = stateRef.current;
    if (!st || st.phase !== 'playing') return;
    const inp = inputRef.current;
    inp.duckHeld = true;
    inp.duckQueued = true;
  }, []);

  const handleTouchDuckEnd = useCallback(() => {
    inputRef.current.duckHeld = false;
  }, []);

  const handleCanvasPointerDown = useCallback(
    (e: React.PointerEvent<HTMLCanvasElement>) => {
      if (e.button !== 0) return;
      const st = stateRef.current;
      if (!st || st.phase !== 'playing') return;
      handleTouchJumpStart();
    },
    [handleTouchJumpStart],
  );

  const handleCanvasPointerUp = useCallback(() => {
    handleTouchJumpEnd();
  }, [handleTouchJumpEnd]);

  const viewport = getRenderViewport(orientation);
  const completedMissions = daily.missions.filter((m) => m.completed).length;

  return (
    <main
      className="relative w-screen h-screen overflow-hidden bg-[#111324]"
      data-orientation={orientation}
    >
      {/* Full-bleed pixel canvas in both true Portrait (300x520) and Landscape (480x270) modes */}
      <div className="fixed inset-0 flex items-center justify-center bg-[#111324]">
        <canvas
          ref={canvasRef}
          width={viewport.width}
          height={viewport.height}
          data-testid="game-canvas"
          onPointerDown={handleCanvasPointerDown}
          onPointerUp={handleCanvasPointerUp}
          onPointerCancel={handleCanvasPointerUp}
          className="pixel-canvas w-full h-full object-cover object-left"
        />
      </div>

      {hud.phase === 'title' && (
        <TitleOverlay
          bestScore={hud.bestScore}
          muted={muted}
          characterId={hud.characterId}
          orientation={orientation}
          streakDays={daily.streakDays}
          hasUnclaimedDaily={!daily.claimedToday}
          completedMissions={completedMissions}
          totalMissions={daily.missions.length}
          onSelectCharacter={handleSelectCharacter}
          onOpenDaily={() => setDailyOpen(true)}
          onStart={handleStartOrRestart}
          onToggleMute={handleToggleMute}
        />
      )}

      {hud.phase !== 'title' && (
        <GameHud
          hud={hud}
          muted={muted}
          orientation={orientation}
          onToggleMute={handleToggleMute}
          onTogglePause={handleTogglePause}
          onRestart={handleStartOrRestart}
          onTouchJumpStart={handleTouchJumpStart}
          onTouchJumpEnd={handleTouchJumpEnd}
          onTouchDuckStart={handleTouchDuckStart}
          onTouchDuckEnd={handleTouchDuckEnd}
        />
      )}

      <StatusModal
        hud={hud}
        newlyCompletedMissions={newlyCompletedMissions}
        completedMissions={completedMissions}
        totalMissions={daily.missions.length}
        onOpenDaily={() => setDailyOpen(true)}
        onResume={handleTogglePause}
        onRestart={handleStartOrRestart}
        onTitle={handleReturnToTitle}
      />

      <DailyModal
        open={dailyOpen}
        daily={daily}
        onClaim={handleClaimDaily}
        onClose={() => setDailyOpen(false)}
      />
    </main>
  );
}
