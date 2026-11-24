import { buildCustomSpriteSet, createDefaultCustomConfig } from '../game/creator';
import { CHARACTER_SPRITES } from '../game/sprites';
import type { CharacterId, CustomCharacterConfig } from '../game/types';

export function CharacterPreviewIcon({
  characterId,
  customCharacter,
  className = 'w-8 h-9',
}: {
  characterId: CharacterId;
  customCharacter?: CustomCharacterConfig;
  className?: string;
}) {
  const sprite =
    characterId === 'custom'
      ? buildCustomSpriteSet(customCharacter ?? createDefaultCustomConfig())
      : CHARACTER_SPRITES[characterId];
  const grid = sprite.frames.run[1];
  const rows = grid.length;
  const cols = grid[0].length;

  return (
    <svg
      viewBox={`0 0 ${cols} ${rows}`}
      className={className}
      shapeRendering="crispEdges"
      aria-hidden="true"
    >
      {grid.map((line, r) =>
        Array.from(line).map((ch, c) => {
          if (ch === '.' || ch === ' ') return null;
          const fill = sprite.palette[ch];
          if (!fill) return null;
          return <rect key={`${r}-${c}`} x={c} y={r} width="1" height="1" fill={fill} />;
        }),
      )}
    </svg>
  );
}

export function HeartSocketIcon({ state }: { state: 'full' | 'cracked' | 'empty' }) {
  return (
    <svg
      viewBox="0 0 12 11"
      className="w-7 h-6 sm:w-8 sm:h-7 shrink-0"
      shapeRendering="crispEdges"
      aria-hidden="true"
    >
      {/* Dark outer pixel silhouette */}
      <path
        d="M2 0h3v1h2V0h3v1h1v1h1v3h-1v1h-1v1H9v1H8v1H7v1H5V9H4V8H3V7H2V6H1V5H0V2h1V1h1V0z"
        fill="#111324"
      />
      {state === 'empty' ? (
        <>
          {/* Hollow dark socket */}
          <path
            d="M2 1h3v1h2V1h3v1h1v3h-1v1H9v1H8v1H7v1H5V8H4V7H3V6H2V5H1V2h1V1z"
            fill="#272D4F"
          />
          <rect x="2" y="2" width="2" height="1" fill="#3B4575" />
        </>
      ) : state === 'cracked' ? (
        <>
          <path
            d="M2 1h3v1h2V1h3v1h1v3h-1v1H9v1H8v1H7v1H5V8H4V7H3V6H2V5H1V2h1V1z"
            fill="#FF9858"
          />
          <rect x="2" y="2" width="2" height="2" fill="#FFF4D0" />
          {/* Crack line */}
          <path d="M6 2h1v2H6v1H5v2h1v1" fill="#111324" />
        </>
      ) : (
        <>
          {/* Full vibrant crimson heart with top highlight & bottom shade */}
          <path
            d="M2 1h3v1h2V1h3v1h1v3h-1v1H9v1H8v1H7v1H5V8H4V7H3V6H2V5H1V2h1V1z"
            fill="#F03854"
          />
          <rect x="2" y="2" width="2" height="1" fill="#FFB2C0" />
          <rect x="2" y="3" width="1" height="1" fill="#FFB2C0" />
          <rect x="9" y="2" width="1" height="3" fill="#B21836" />
          <rect x="8" y="5" width="1" height="2" fill="#B21836" />
          <rect x="6" y="7" width="2" height="1" fill="#B21836" />
        </>
      )}
    </svg>
  );
}

export function CoinIcon({ className = 'w-5 h-5' }: { className?: string }) {
  return (
    <svg viewBox="0 0 10 10" className={className} shapeRendering="crispEdges" aria-hidden="true">
      <rect x="2" y="0" width="6" height="10" fill="#181210" />
      <rect x="1" y="1" width="8" height="8" fill="#181210" />
      <rect x="0" y="2" width="10" height="6" fill="#181210" />
      <rect x="2" y="1" width="6" height="8" fill="#FFD838" />
      <rect x="1" y="2" width="8" height="6" fill="#FFD838" />
      <rect x="2" y="2" width="2" height="2" fill="#FFF8C0" />
      <rect x="4" y="3" width="2" height="4" fill="#D88018" />
      <rect x="7" y="5" width="1" height="2" fill="#D88018" />
    </svg>
  );
}

export function ShieldIcon({ className = 'w-5 h-5' }: { className?: string }) {
  return (
    <svg viewBox="0 0 12 12" className={className} shapeRendering="crispEdges" aria-hidden="true">
      <path d="M2 0h8v1h1v5h-1v2H9v1H8v1H7v1H5v-1H4V9H3V8H2V6H1V1h1V0z" fill="#101830" />
      <path d="M2 1h8v5H9v2H8v1H7v1H5V9H4V8H3V6H2V1z" fill="#58D8FF" />
      <rect x="3" y="2" width="3" height="3" fill="#E8FCFF" />
      <rect x="7" y="5" width="2" height="3" fill="#2878D8" />
    </svg>
  );
}

export function MagnetIcon({ className = 'w-5 h-5' }: { className?: string }) {
  return (
    <svg viewBox="0 0 10 10" className={className} shapeRendering="crispEdges" aria-hidden="true">
      <path d="M1 1h3v5h2V1h3v6H8v2H2V7H1V1z" fill="#181024" />
      <rect x="2" y="2" width="1" height="2" fill="#E8EEF8" />
      <rect x="7" y="2" width="1" height="2" fill="#E8EEF8" />
      <rect x="2" y="4" width="1" height="3" fill="#F04068" />
      <rect x="7" y="4" width="1" height="3" fill="#F04068" />
      <rect x="3" y="7" width="4" height="1" fill="#F04068" />
    </svg>
  );
}

export function SoundIcon({ muted }: { muted: boolean }) {
  return (
    <svg viewBox="0 0 16 16" className="w-4 h-4" shapeRendering="crispEdges" aria-hidden="true">
      <path d="M2 6h3l3-3h1v10H8l-3-3H2V6z" fill="currentColor" />
      {muted ? (
        <>
          <rect x="11" y="5" width="2" height="2" fill="#FF6878" />
          <rect x="13" y="7" width="2" height="2" fill="#FF6878" />
          <rect x="11" y="9" width="2" height="2" fill="#FF6878" />
          <rect x="13" y="5" width="2" height="2" fill="#FF6878" />
          <rect x="11" y="7" width="2" height="2" fill="#FF6878" />
          <rect x="13" y="9" width="2" height="2" fill="#FF6878" />
        </>
      ) : (
        <>
          <rect x="11" y="6" width="1" height="4" fill="#FFD872" />
          <rect x="13" y="4" width="1" height="8" fill="#FFD872" />
        </>
      )}
    </svg>
  );
}

export function PauseIcon() {
  return (
    <svg viewBox="0 0 16 16" className="w-4 h-4" shapeRendering="crispEdges" aria-hidden="true">
      <rect x="3" y="2" width="4" height="12" fill="currentColor" />
      <rect x="9" y="2" width="4" height="12" fill="currentColor" />
    </svg>
  );
}

export function RestartIcon() {
  return (
    <svg viewBox="0 0 16 16" className="w-4 h-4" shapeRendering="crispEdges" aria-hidden="true">
      <path d="M4 3h7v2H5v6h6V9h2v4H3V3h1z" fill="currentColor" />
      <path d="M10 1h2v5H7V4h3V1z" fill="currentColor" />
    </svg>
  );
}

export function PixelStrideTitleLogo() {
  return (
    <div className="flex flex-col items-center select-none">
      <svg
        viewBox="0 0 420 116"
        className="title-logo-svg w-[300px] sm:w-[440px] md:w-[520px] h-auto"
        role="img"
        aria-label="Pixel Stride"
        shapeRendering="crispEdges"
      >
        {/* Stepped 8-bit crimson scarf ribbon crest behind lockup */}
        <rect x="52" y="10" width="316" height="8" fill="#121324" />
        <rect x="44" y="18" width="332" height="86" fill="#121324" />
        <rect x="52" y="104" width="316" height="8" fill="#121324" />

        {/* Inner navy cartridge plaque */}
        <rect x="56" y="16" width="308" height="84" fill="#1A2246" />
        <rect x="50" y="22" width="320" height="72" fill="#1A2246" />
        {/* Bevel top highlight & bottom shade */}
        <rect x="56" y="16" width="308" height="4" fill="#465C9E" />
        <rect x="56" y="96" width="308" height="4" fill="#0F142C" />

        {/* Flowing crimson scarf tails on left & right */}
        <rect x="18" y="42" width="28" height="10" fill="#121324" />
        <rect x="8" y="50" width="22" height="10" fill="#121324" />
        <rect x="20" y="44" width="28" height="6" fill="#E83B48" />
        <rect x="10" y="52" width="22" height="6" fill="#C42438" />

        <rect x="374" y="42" width="28" height="10" fill="#121324" />
        <rect x="390" y="50" width="22" height="10" fill="#121324" />
        <rect x="372" y="44" width="28" height="6" fill="#E83B48" />
        <rect x="388" y="52" width="22" height="6" fill="#C42438" />

        {/* Corner pixel rivets */}
        <rect x="58" y="24" width="5" height="5" fill="#FFD872" />
        <rect x="357" y="24" width="5" height="5" fill="#FFD872" />
        <rect x="58" y="87" width="5" height="5" fill="#FFD872" />
        <rect x="357" y="87" width="5" height="5" fill="#FFD872" />

        {/* Chunky retro display title text with hard 8-bit stepped shadow */}
        <text
          x="210"
          y="56"
          textAnchor="middle"
          fill="#0B0D1A"
          fontFamily="'Press Start 2P', monospace"
          fontSize="30"
          letterSpacing="3"
          transform="translate(4, 4)"
        >
          PIXEL
        </text>
        <text
          x="210"
          y="56"
          textAnchor="middle"
          fill="#FFE05C"
          fontFamily="'Press Start 2P', monospace"
          fontSize="30"
          letterSpacing="3"
        >
          PIXEL
        </text>

        <text
          x="210"
          y="90"
          textAnchor="middle"
          fill="#0B0D1A"
          fontFamily="'Press Start 2P', monospace"
          fontSize="28"
          letterSpacing="4"
          transform="translate(4, 4)"
        >
          STRIDE
        </text>
        <text
          x="210"
          y="90"
          textAnchor="middle"
          fill="#F8F6EE"
          fontFamily="'Press Start 2P', monospace"
          fontSize="28"
          letterSpacing="4"
        >
          STRIDE
        </text>
      </svg>
    </div>
  );
}
