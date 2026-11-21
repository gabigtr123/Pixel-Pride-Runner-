import React from 'react';
import {
  HAIR_OPTIONS,
  isOptionUnlocked,
  OUTFIT_OPTIONS,
  PRESET_NAMES,
  SCARF_OPTIONS,
  SKIN_OPTIONS,
  type CareerStats,
  type ColorOption,
} from '../game/creator';
import type { CharacterGender, CustomCharacterConfig } from '../game/types';
import { CharacterPreviewIcon } from './PixelIcons';

interface CreatorModalProps {
  open: boolean;
  config: CustomCharacterConfig;
  stats: CareerStats;
  onChange: (next: CustomCharacterConfig) => void;
  onSaveAndUse: () => void;
  onClose: () => void;
}

function SwatchSection({
  title,
  options,
  selectedId,
  stats,
  onSelect,
}: {
  title: string;
  options: ColorOption[];
  selectedId: string;
  stats: CareerStats;
  onSelect: (id: string) => void;
}) {
  return (
    <div className="flex flex-col gap-1.5">
      <span className="font-pixel-display text-[9px] text-[#E4EAF8] tracking-wider">{title}</span>
      <div className="flex flex-wrap gap-1.5">
        {options.map((opt) => {
          const unlocked = isOptionUnlocked(opt, stats);
          const selected = selectedId === opt.id;
          const progressText = opt.unlock
            ? `${Math.min(stats[opt.unlock.stat], opt.unlock.target)}/${opt.unlock.target}`
            : '';
          return (
            <button
              key={opt.id}
              type="button"
              disabled={!unlocked}
              title={unlocked ? opt.name : `Locked: ${opt.unlock?.label} (${progressText})`}
              onClick={() => onSelect(opt.id)}
              className={`px-2 py-1.5 border-2 flex items-center gap-1.5 text-left ${
                selected
                  ? 'bg-[#293568] border-[#FFE05C] text-[#FFF4A8] cursor-pointer'
                  : unlocked
                    ? 'bg-[#12162D] border-[#3A467A] text-[#D8E2FA] hover:border-[#F8F6EE] cursor-pointer'
                    : 'bg-[#0F1224] border-[#262E52] text-[#7A88B4] cursor-not-allowed'
              }`}
            >
              <span
                className="w-3.5 h-3.5 border border-[#0B0D1A] shrink-0"
                style={{
                  background: `linear-gradient(135deg, ${opt.primary} 55%, ${opt.secondary} 55%)`,
                  opacity: unlocked ? 1 : 0.45,
                }}
              />
              <span className="font-pixel-ui text-[10px] whitespace-nowrap">
                {unlocked ? opt.name : `🔒 ${opt.name}`}
              </span>
            </button>
          );
        })}
      </div>
    </div>
  );
}

export function CreatorModal({
  open,
  config,
  stats,
  onChange,
  onSaveAndUse,
  onClose,
}: CreatorModalProps) {
  if (!open) return null;

  const lockedItems = [...HAIR_OPTIONS, ...SCARF_OPTIONS, ...OUTFIT_OPTIONS].filter(
    (opt) => !isOptionUnlocked(opt, stats),
  );

  const handleGenderChange = (gender: CharacterGender) => {
    const currentIsPreset =
      PRESET_NAMES.male.includes(config.name) || PRESET_NAMES.female.includes(config.name);
    onChange({
      ...config,
      gender,
      name: currentIsPreset ? PRESET_NAMES[gender][0] : config.name,
    });
  };

  return (
    <div
      className="fixed inset-0 z-40 flex items-center justify-center modal-safe-area bg-[#0B0D1A]/75"
      role="dialog"
      aria-modal="true"
      aria-labelledby="creator-modal-title"
      data-testid="creator-modal"
    >
      <div className="pixel-panel w-full max-w-lg max-h-full overflow-y-auto p-4 sm:p-5 flex flex-col gap-3.5">
        {/* Header */}
        <div className="flex items-center justify-between gap-2 border-b-2 border-[#303A68] pb-2.5">
          <div>
            <h2
              id="creator-modal-title"
              className="font-pixel-display text-sm sm:text-base text-[#FFE05C] pixel-text-shadow"
            >
              HERO CREATOR
            </h2>
            <p className="font-pixel-ui text-[10px] text-[#78F0A8] mt-0.5">
              PERK: +15% COIN &amp; STOMP BONUS
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close Hero Creator"
            className="pixel-icon-btn px-2.5 py-1.5 font-pixel-display text-[10px] cursor-pointer"
          >
            X
          </button>
        </div>

        {/* Live Preview + Style & Name */}
        <div className="flex flex-col sm:flex-row items-center gap-3.5 bg-[#12162D] border-2 border-[#3A467A] p-3">
          <div className="w-24 h-24 bg-[#1D2852] border-2 border-[#5870B8] flex flex-col items-center justify-center shrink-0">
            <CharacterPreviewIcon
              characterId="custom"
              customCharacter={config}
              className="w-16 h-18"
            />
          </div>

          <div className="flex-1 w-full flex flex-col gap-2">
            {/* Gender / Style Toggle */}
            <div className="grid grid-cols-2 gap-2">
              {(['male', 'female'] as const).map((g) => (
                <button
                  key={g}
                  type="button"
                  onClick={() => handleGenderChange(g)}
                  className={`py-1.5 font-pixel-display text-[10px] border-2 cursor-pointer ${
                    config.gender === g
                      ? 'bg-[#293568] border-[#FFE05C] text-[#FFF4A8]'
                      : 'bg-[#141830] border-[#3A467A] text-[#A2B0D8]'
                  }`}
                >
                  {g === 'male' ? 'MALE' : 'FEMALE'}
                </button>
              ))}
            </div>

            {/* Name Input */}
            <input
              type="text"
              maxLength={12}
              value={config.name}
              onChange={(e) => onChange({ ...config, name: e.target.value.toUpperCase() })}
              placeholder="HERO NAME"
              aria-label="Hero Name"
              className="w-full bg-[#0B0E1D] border-2 border-[#4C5C96] px-2.5 py-1.5 font-pixel-display text-xs text-[#FFFFFF] focus:border-[#FFE05C] focus:outline-none"
            />

            {/* Predefined Names */}
            <div className="flex flex-wrap gap-1">
              {PRESET_NAMES[config.gender].map((preset) => (
                <button
                  key={preset}
                  type="button"
                  onClick={() => onChange({ ...config, name: preset })}
                  className={`px-2 py-0.5 font-pixel-ui text-[10px] border cursor-pointer ${
                    config.name === preset
                      ? 'bg-[#293568] border-[#FFE05C] text-[#FFF4A8]'
                      : 'bg-[#171C38] border-[#303A68] text-[#B8C4E4] hover:border-[#F8F6EE]'
                  }`}
                >
                  {preset}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Color Swatches */}
        <SwatchSection
          title="SKIN TONE"
          options={SKIN_OPTIONS}
          selectedId={config.skinId}
          stats={stats}
          onSelect={(skinId) => onChange({ ...config, skinId })}
        />
        <SwatchSection
          title="HAIR / CAP"
          options={HAIR_OPTIONS}
          selectedId={config.hairId}
          stats={stats}
          onSelect={(hairId) => onChange({ ...config, hairId })}
        />
        <SwatchSection
          title="SCARF / RIBBON"
          options={SCARF_OPTIONS}
          selectedId={config.scarfId}
          stats={stats}
          onSelect={(scarfId) => onChange({ ...config, scarfId })}
        />
        <SwatchSection
          title="OUTFIT"
          options={OUTFIT_OPTIONS}
          selectedId={config.outfitId}
          stats={stats}
          onSelect={(outfitId) => onChange({ ...config, outfitId })}
        />

        {/* Mission Unlocks Checklist */}
        {lockedItems.length > 0 && (
          <div className="bg-[#12162D] border-2 border-[#303A68] p-2.5 flex flex-col gap-1.5">
            <span className="font-pixel-display text-[9px] text-[#FFE05C]">
              MISSION UNLOCKS ({6 - lockedItems.length}/6)
            </span>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-1">
              {lockedItems.map((item) => {
                const rule = item.unlock!;
                const current = Math.min(stats[rule.stat], rule.target);
                return (
                  <div
                    key={item.id}
                    className="font-pixel-ui text-[10px] text-[#B8C4E4] flex items-center justify-between gap-2 bg-[#0E1226] px-2 py-1 border border-[#242C50]"
                  >
                    <span>
                      🔒 <strong className="text-[#F8F6EE]">{item.name}</strong>: {rule.label}
                    </span>
                    <span className="text-[#78E8FF] shrink-0">
                      {current}/{rule.target}
                    </span>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* Save & Equip CTA */}
        <button
          type="button"
          data-testid="save-custom-hero-btn"
          onClick={onSaveAndUse}
          className="pixel-btn-primary w-full font-pixel-display text-xs py-3 cursor-pointer"
        >
          PLAY AS {(config.name.trim() || 'HERO').slice(0, 12)}
        </button>
      </div>
    </div>
  );
}
