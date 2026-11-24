import { buildCustomSpriteSet } from '../game/creator';
import { CHARACTER_SPRITES } from '../game/sprites';
import type { CharacterId, CustomCharacterConfig, GameMode, OrientationMode } from '../game/types';
import { CharacterPreviewIcon, CoinIcon, PixelStrideTitleLogo, SoundIcon } from './PixelIcons';

const CHARACTER_IDS: CharacterId[] = ['stride', 'lily', 'knight', 'custom'];

const GAME_MODES: { id: GameMode; label: string; hint: string }[] = [
  { id: 'endless', label: 'ENDLESS', hint: 'Classic run · Boss every 90s' },
  { id: 'daily', label: 'DAILY', hint: 'Same seed for everyone today' },
  { id: 'hardcore', label: 'HARDCORE', hint: '1 HP · 1.25× Speed · 2× Coins & Score' },
  { id: 'bossRush', label: 'BOSS RUSH', hint: 'Back-to-back Mini-Boss gauntlets!' },
];

interface TitleOverlayProps {
  bestScore: number;
  muted: boolean;
  characterId: CharacterId;
  customCharacter: CustomCharacterConfig;
  unlockedCharacters: CharacterId[];
  coinsBank: number;
  orientation: OrientationMode;
  streakDays: number;
  hasUnclaimedDaily: boolean;
  hasUnclaimedTrophy: boolean;
  completedMissions: number;
  totalMissions: number;
  weatherLabel: string;
  gameMode: GameMode;
  onSelectCharacter: (id: CharacterId) => void;
  onSelectGameMode: (mode: GameMode) => void;
  onOpenDaily: () => void;
  onOpenShop: () => void;
  onOpenTrophy: () => void;
  onOpenCreator: () => void;
  onOpenWeather: () => void;
  onStart: () => void;
  onToggleMute: () => void;
}

export function TitleOverlay({
  bestScore,
  muted,
  characterId,
  customCharacter,
  unlockedCharacters,
  coinsBank,
  orientation,
  streakDays,
  hasUnclaimedDaily,
  hasUnclaimedTrophy,
  completedMissions,
  totalMissions,
  weatherLabel,
  gameMode,
  onSelectCharacter,
  onSelectGameMode,
  onOpenDaily,
  onOpenShop,
  onOpenTrophy,
  onOpenCreator,
  onOpenWeather,
  onStart,
  onToggleMute,
}: TitleOverlayProps) {
  const isPortrait = orientation === 'portrait';
  const customSprite = buildCustomSpriteSet(customCharacter);
  const activeSprite =
    characterId === 'custom'
      ? customSprite
      : (CHARACTER_SPRITES[characterId] ?? CHARACTER_SPRITES.stride);
  const activeModeInfo = GAME_MODES.find((m) => m.id === gameMode) ?? GAME_MODES[0];

  return (
    <div
      className="fixed inset-0 z-20 flex flex-col justify-between pointer-events-none safe-area-hud"
      data-testid="title-screen"
    >
      {/* Top bar: Daily Quests + Pixel Shop + Trophy + Weather/Theme on left, Sound toggle on right */}
      <div className="flex flex-col gap-3 sm:gap-4">
        <div className="flex items-start justify-between gap-2">
          <div className="flex flex-wrap items-center gap-1.5 sm:gap-2 pointer-events-auto">
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

            <button
              type="button"
              data-testid="open-trophy-btn"
              onClick={(e) => {
                e.currentTarget.blur();
                onOpenTrophy();
              }}
              tabIndex={-1}
              aria-label="Open Trophy Room and Achievements"
              className="relative pixel-icon-btn h-11 px-2.5 sm:px-3 flex items-center gap-1 cursor-pointer"
            >
              <span className="text-sm leading-none">🏆</span>
              <span className="hidden sm:inline font-pixel-ui text-[10px] text-[#FFE05C]">
                TROPHIES
              </span>
              {hasUnclaimedTrophy && (
                <span
                  className="w-2.5 h-2.5 bg-[#78F0A8] border border-[#111324] animate-pulse"
                  aria-hidden="true"
                />
              )}
            </button>

            <button
              type="button"
              data-testid="open-weather-btn"
              onClick={(e) => {
                e.currentTarget.blur();
                onOpenWeather();
              }}
              tabIndex={-1}
              aria-label="Open Winter Theme and Live Weather Sync"
              className="pixel-icon-btn h-11 px-2.5 sm:px-3 flex items-center gap-1 cursor-pointer"
            >
              <span className="font-pixel-ui text-[11px] text-[#E8FCFF] whitespace-nowrap">
                {weatherLabel}
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
            className="pointer-events-auto pixel-icon-btn w-11 h-11 flex items-center justify-center cursor-pointer shrink-0"
          >
            <SoundIcon muted={muted} />
          </button>
        </div>

        {/* In Portrait mode, anchor the Title Logo cleanly in the upper sky so the horizon & running hero stay 100% visible */}
        {isPortrait && (
          <div className="flex justify-center pt-1">
            <PixelStrideTitleLogo />
          </div>
        )}
      </div>

      {/* Landscape center lockup OR Portrait underground masonry deck */}
      <div
        className={`title-center-stack flex flex-col items-center ${
          isPortrait ? 'gap-2 pb-2' : '-mt-4 md:-mt-10 gap-2 sm:gap-2.5'
        }`}
      >
        {!isPortrait && <PixelStrideTitleLogo />}

        {/* Retro 8-bit character selector (Stride, Lily, Pixel Knight, Custom Hero) */}
        <div className="flex flex-col items-center gap-1.5 pointer-events-auto">
          <div
            className="flex flex-wrap items-center justify-center gap-1.5 sm:gap-2"
            role="radiogroup"
            aria-label="Choose character"
          >
            {CHARACTER_IDS.map((id) => {
              const unlocked = id === 'custom' || unlockedCharacters.includes(id);
              const selected = characterId === id;
              const info = id === 'custom' ? customSprite : CHARACTER_SPRITES[id];
              return (
                <button
                  key={id}
                  type="button"
                  role="radio"
                  aria-checked={selected}
                  data-testid={`select-char-${id}`}
                  onClick={(e) => {
                    e.currentTarget.blur();
                    if (id === 'custom' && selected) {
                      onOpenCreator();
                    } else if (unlocked) {
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
                    customCharacter={id === 'custom' ? customCharacter : undefined}
                    className={`w-6 h-7 shrink-0 ${unlocked ? '' : 'opacity-60'}`}
                  />
                  <span className="font-pixel-display text-[9px] sm:text-[10px] tracking-wider whitespace-nowrap">
                    {unlocked ? info.name : `${info.name} 🔒`}
                  </span>
                </button>
              );
            })}
          </div>

          <div className="flex items-center gap-2">
            <span className="font-pixel-ui text-[10px] text-[#78F0A8] bg-[#111324]/75 px-2.5 py-0.5 tracking-wider">
              PERK: {activeSprite.perk}
            </span>
            <button
              type="button"
              data-testid="open-creator-btn"
              onClick={(e) => {
                e.currentTarget.blur();
                onOpenCreator();
              }}
              className="font-pixel-display text-[9px] text-[#FFE05C] bg-[#1A2246] border border-[#FFE05C] px-2 py-0.5 cursor-pointer hover:bg-[#293568]"
            >
              EDIT HERO
            </button>
          </div>
        </div>

        {/* 4-Mode Selector Pills */}
        <div className="flex flex-col items-center gap-1 pointer-events-auto">
          <div className="flex flex-wrap items-center justify-center gap-1">
            {GAME_MODES.map((m) => {
              const active = gameMode === m.id;
              return (
                <button
                  key={m.id}
                  type="button"
                  data-testid={`mode-${m.id}`}
                  onClick={(e) => {
                    e.currentTarget.blur();
                    onSelectGameMode(m.id);
                  }}
                  className={`px-2 py-1 font-pixel-display text-[8px] sm:text-[9px] border-2 cursor-pointer transition-colors ${
                    active
                      ? m.id === 'hardcore'
                        ? 'bg-[#481626] border-[#FF5874] text-[#FFD0D8]'
                        : m.id === 'bossRush'
                          ? 'bg-[#3A240C] border-[#FFE05C] text-[#FFF4A8]'
                          : 'bg-[#1F2952] border-[#78E8FF] text-[#E8FCFF]'
                      : 'bg-[#12162D]/85 border-[#3A467A] text-[#98A8D4] hover:border-[#C8D4F4]'
                  }`}
                >
                  {m.label}
                </button>
              );
            })}
          </div>
          <span className="font-pixel-ui text-[9px] text-[#D4E0FF] bg-[#111324]/75 px-2 py-0.5">
            {activeModeInfo.hint}
          </span>
        </div>

        <div className="flex flex-col items-center gap-1.5 pointer-events-auto">
          <button
            type="button"
            data-start="true"
            onClick={(e) => {
              e.currentTarget.blur();
              onStart();
            }}
            className={`title-start-btn pixel-btn-primary font-pixel-display text-sm sm:text-base tracking-wider whitespace-nowrap cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-[#FFF4A8] ${
              isPortrait ? 'px-7 py-2.5' : 'px-8 py-3.5'
            }`}
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

      {/* Flat, borderless single-line control hint in bottom safe margin (desktop only) */}
      {!isPortrait && (
        <div className="desktop-control-hint flex justify-center pb-1">
          <p className="font-pixel-ui text-xs text-[#FFF9E8] bg-[#111324]/80 px-4 py-1.5 text-center tracking-wide">
            SPACE / UP Jump &amp; Double-Jump · DOWN Duck / Fast-Fall · P Pause · M Audio
          </p>
        </div>
      )}
    </div>
  );
}
