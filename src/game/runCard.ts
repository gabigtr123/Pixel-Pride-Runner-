import { BIOMES, LEVEL_NAMES } from './constants';
import { buildCustomSpriteSet } from './creator';
import {
  CHARACTER_SPRITES,
  drawPixelGrid,
  PET_DRAGON_PALETTE,
  PET_DRAGON_SPRITE,
  PET_FOX_PALETTE,
  PET_FOX_SPRITE,
  PET_OWL_PALETTE,
  PET_OWL_SPRITE,
} from './sprites';
import type {
  CapeId,
  CharacterId,
  CustomCharacterConfig,
  HatId,
  PetId,
  RunSnapshot,
} from './types';

export interface RunCardData {
  summary: RunSnapshot;
  characterId: CharacterId;
  customCharacter?: CustomCharacterConfig;
  hatId: HatId;
  capeId: CapeId;
  petId: PetId;
  dateLabel?: string;
}

export type CardRarity = 'COMMON' | 'RARE' | 'EPIC' | 'LEGENDARY';

export function getCardRarity(summary: RunSnapshot): {
  rarity: CardRarity;
  borderOuter: string;
  borderInner: string;
  accent: string;
  glow: string;
} {
  if (summary.score >= 10000 || summary.bossesDefeated >= 2 || (summary.gameMode === 'hardcore' && summary.score >= 2500)) {
    return {
      rarity: 'LEGENDARY',
      borderOuter: '#FFE05C',
      borderInner: '#FF9828',
      accent: '#FFE878',
      glow: 'rgba(255, 224, 92, 0.35)',
    };
  }
  if (summary.score >= 4000 || summary.bossesDefeated >= 1 || summary.isNewBest) {
    return {
      rarity: 'EPIC',
      borderOuter: '#C878FF',
      borderInner: '#7848E8',
      accent: '#E8A8FF',
      glow: 'rgba(200, 120, 255, 0.3)',
    };
  }
  if (summary.score >= 1200 || summary.level >= 2) {
    return {
      rarity: 'RARE',
      borderOuter: '#58E8FF',
      borderInner: '#2888D8',
      accent: '#98F4FF',
      glow: 'rgba(88, 232, 255, 0.25)',
    };
  }
  return {
    rarity: 'COMMON',
    borderOuter: '#98A8D8',
    borderInner: '#485888',
    accent: '#D8E2FF',
    glow: 'rgba(152, 168, 216, 0.2)',
  };
}

function drawCardHeroExtras(
  ctx: CanvasRenderingContext2D,
  hatId: HatId,
  capeId: CapeId,
  anchorX: number,
  anchorY: number,
  spriteW: number,
  spriteH: number,
  scale: number,
): void {
  const s = scale;
  const backX = anchorX - (spriteW * s) / 2 - s;
  const bodyY = anchorY - spriteH * s + 6 * s;

  // Back accessory (Cape / Jetpack / Wings)
  if (capeId === 'hero') {
    ctx.fillStyle = '#141224';
    ctx.fillRect(backX - 6 * s, bodyY - s, 8 * s, 9 * s);
    ctx.fillStyle = '#E83044';
    ctx.fillRect(backX - 5 * s, bodyY, 6 * s, 7 * s);
    ctx.fillStyle = '#FF7888';
    ctx.fillRect(backX - 4 * s, bodyY, 4 * s, 2 * s);
  } else if (capeId === 'jetpack') {
    ctx.fillStyle = '#141224';
    ctx.fillRect(backX - 4 * s, bodyY, 6 * s, 9 * s);
    ctx.fillStyle = '#B8C8E0';
    ctx.fillRect(backX - 3 * s, bodyY + s, 4 * s, 7 * s);
    ctx.fillStyle = '#E84048';
    ctx.fillRect(backX - 3 * s, bodyY + 2 * s, 4 * s, 2 * s);
    ctx.fillStyle = '#FF9820';
    ctx.fillRect(backX - 3 * s, bodyY + 9 * s, 4 * s, 4 * s);
    ctx.fillStyle = '#FFF8A8';
    ctx.fillRect(backX - 2 * s, bodyY + 9 * s, 2 * s, 2 * s);
  } else if (capeId === 'wings') {
    ctx.fillStyle = '#141224';
    ctx.fillRect(backX - 7 * s, bodyY - 3 * s, 9 * s, 8 * s);
    ctx.fillStyle = '#FFFDF5';
    ctx.fillRect(backX - 6 * s, bodyY - 2 * s, 7 * s, 6 * s);
    ctx.fillStyle = '#FFE068';
    ctx.fillRect(backX - 5 * s, bodyY - s, 5 * s, 2 * s);
  }

  // Hat / Headgear
  const headTopY = anchorY - spriteH * s;
  if (hatId === 'crown') {
    ctx.fillStyle = '#141224';
    ctx.fillRect(anchorX - 5 * s, headTopY - 4 * s, 10 * s, 5 * s);
    ctx.fillStyle = '#FFE048';
    ctx.fillRect(anchorX - 4 * s, headTopY - 3 * s, 2 * s, 3 * s);
    ctx.fillRect(anchorX - 1 * s, headTopY - 4 * s, 2 * s, 4 * s);
    ctx.fillRect(anchorX + 2 * s, headTopY - 3 * s, 2 * s, 3 * s);
    ctx.fillRect(anchorX - 4 * s, headTopY - 1 * s, 8 * s, 2 * s);
    ctx.fillStyle = '#FF3858';
    ctx.fillRect(anchorX - 1 * s, headTopY - 1 * s, 2 * s, 1 * s);
  } else if (hatId === 'wizard') {
    ctx.fillStyle = '#141224';
    ctx.fillRect(anchorX - 6 * s, headTopY - 1 * s, 12 * s, 3 * s);
    ctx.fillRect(anchorX - 4 * s, headTopY - 4 * s, 8 * s, 4 * s);
    ctx.fillRect(anchorX - 2 * s, headTopY - 7 * s, 4 * s, 3 * s);
    ctx.fillStyle = '#5844C8';
    ctx.fillRect(anchorX - 5 * s, headTopY, 10 * s, 1 * s);
    ctx.fillRect(anchorX - 3 * s, headTopY - 3 * s, 6 * s, 3 * s);
    ctx.fillRect(anchorX - 1 * s, headTopY - 6 * s, 2 * s, 3 * s);
    ctx.fillStyle = '#FFE05C';
    ctx.fillRect(anchorX - 3 * s, headTopY - 1 * s, 6 * s, 1 * s);
  } else if (hatId === 'santa') {
    ctx.fillStyle = '#141224';
    ctx.fillRect(anchorX - 5 * s, headTopY - 4 * s, 10 * s, 5 * s);
    ctx.fillStyle = '#E8283C';
    ctx.fillRect(anchorX - 4 * s, headTopY - 3 * s, 7 * s, 3 * s);
    ctx.fillRect(anchorX - 2 * s, headTopY - 4 * s, 4 * s, 1 * s);
    ctx.fillStyle = '#FFFFFF';
    ctx.fillRect(anchorX - 4 * s, headTopY - 1 * s, 8 * s, 2 * s);
    ctx.fillRect(anchorX + 3 * s, headTopY - 2 * s, 2 * s, 2 * s);
  } else if (hatId === 'visor') {
    const eyeY = headTopY + 4 * s;
    ctx.fillStyle = '#121324';
    ctx.fillRect(anchorX - 2 * s, eyeY - 1 * s, 8 * s, 4 * s);
    ctx.fillStyle = '#48F0FF';
    ctx.fillRect(anchorX - 1 * s, eyeY, 6 * s, 2 * s);
    ctx.fillStyle = '#FFFFFF';
    ctx.fillRect(anchorX + 3 * s, eyeY, 2 * s, 1 * s);
  } else if (hatId === 'halo') {
    ctx.fillStyle = '#FFE848';
    ctx.fillRect(anchorX - 4 * s, headTopY - 5 * s, 8 * s, 2 * s);
    ctx.fillStyle = '#FFFFFF';
    ctx.fillRect(anchorX - 2 * s, headTopY - 5 * s, 4 * s, 1 * s);
  }
}

export function renderRunCardToCanvas(
  canvas: HTMLCanvasElement,
  data: RunCardData,
): void {
  const W = 640;
  const H = 900;
  canvas.width = W;
  canvas.height = H;
  const ctx = canvas.getContext('2d');
  if (!ctx) return;

  ctx.imageSmoothingEnabled = false;
  const { summary, characterId, customCharacter, hatId, capeId, petId } = data;
  const rarityInfo = getCardRarity(summary);
  const biome = BIOMES[summary.zoneIndex % BIOMES.length] ?? BIOMES[0];
  const levelName = LEVEL_NAMES[(summary.level - 1) % LEVEL_NAMES.length];

  // 1. Outer trading card base & holographic pixel border
  ctx.fillStyle = '#0D1022';
  ctx.fillRect(0, 0, W, H);

  // Outer rarity border (thick 8-bit beveled frame)
  ctx.fillStyle = rarityInfo.borderOuter;
  ctx.fillRect(12, 12, W - 24, H - 24);
  ctx.fillStyle = '#111428';
  ctx.fillRect(22, 22, W - 44, H - 44);
  ctx.fillStyle = rarityInfo.borderInner;
  ctx.fillRect(28, 28, W - 56, H - 56);
  ctx.fillStyle = '#141932';
  ctx.fillRect(34, 34, W - 68, H - 68);

  // Corner pixel rivets
  const corners = [
    [18, 18],
    [W - 34, 18],
    [18, H - 34],
    [W - 34, H - 34],
  ];
  for (const [cx, cy] of corners) {
    ctx.fillStyle = '#111428';
    ctx.fillRect(cx, cy, 16, 16);
    ctx.fillStyle = rarityInfo.accent;
    ctx.fillRect(cx + 4, cy + 4, 8, 8);
  }

  // 2. Top Card Header (Brand + Rarity + Mode)
  ctx.font = 'bold 24px "Press Start 2P", monospace';
  ctx.textAlign = 'left';
  ctx.fillStyle = '#0B0D1A';
  ctx.fillText('PIXEL STRIDE', 54, 88);
  ctx.fillStyle = '#FFE05C';
  ctx.fillText('PIXEL STRIDE', 50, 84);

  // Rarity pill top-right
  ctx.fillStyle = rarityInfo.borderInner;
  ctx.fillRect(W - 210, 56, 158, 34);
  ctx.strokeStyle = rarityInfo.borderOuter;
  ctx.lineWidth = 3;
  ctx.strokeRect(W - 210, 56, 158, 34);
  ctx.font = 'bold 12px "Press Start 2P", monospace';
  ctx.textAlign = 'center';
  ctx.fillStyle = '#FFFFFF';
  ctx.fillText(rarityInfo.rarity, W - 131, 79);

  // Sub-header bar: Mode + New Best tag
  const modeLabel =
    summary.gameMode === 'hardcore'
      ? 'HARDCORE 2X'
      : summary.gameMode === 'bossRush'
        ? 'BOSS RUSH'
        : summary.gameMode === 'daily'
          ? 'DAILY SEED'
          : 'ENDLESS MODE';
  ctx.font = 'bold 12px "Press Start 2P", monospace';
  ctx.textAlign = 'left';
  ctx.fillStyle = summary.gameMode === 'hardcore' ? '#FF6880' : '#78E8FF';
  ctx.fillText(modeLabel, 52, 114);

  if (summary.isNewBest) {
    ctx.textAlign = 'right';
    ctx.fillStyle = '#78F0A8';
    ctx.fillText('★ NEW HIGH SCORE ★', W - 52, 114);
  }

  // 3. Central Pixel-Art Illustration Window (Trading Card Artwork)
  const artX = 50;
  const artY = 132;
  const artW = W - 100;
  const artH = 340;

  ctx.fillStyle = '#0A0C1A';
  ctx.fillRect(artX - 4, artY - 4, artW + 8, artH + 8);

  // Biome sky gradient bands
  const skyGrad = ctx.createLinearGradient(0, artY, 0, artY + artH - 64);
  skyGrad.addColorStop(0, biome.skyTop);
  skyGrad.addColorStop(0.55, biome.skyMid);
  skyGrad.addColorStop(1, biome.skyHorizon);
  ctx.fillStyle = skyGrad;
  ctx.fillRect(artX, artY, artW, artH);

  // Retro pixel stars & celestial body
  ctx.fillStyle = 'rgba(255, 250, 220, 0.75)';
  const starCoords = [
    [82, 162], [148, 190], [220, 158], [310, 178], [412, 155], [510, 185], [548, 160],
  ];
  for (const [sx, sy] of starCoords) {
    ctx.fillRect(sx, sy, 4, 4);
  }
  ctx.fillStyle = biome.celestialColor;
  ctx.fillRect(artX + artW - 96, artY + 32, 48, 48);
  ctx.fillStyle = 'rgba(255,255,255,0.4)';
  ctx.fillRect(artX + artW - 90, artY + 38, 16, 16);

  // Distant pixel mountains
  ctx.fillStyle = biome.farMountain;
  const peaks = [
    [artX, artY + 230, 130],
    [artX + 95, artY + 210, 160],
    [artX + 230, artY + 225, 150],
    [artX + 360, artY + 200, 180],
  ];
  for (const [px, py, pw] of peaks) {
    ctx.beginPath();
    ctx.moveTo(px, artY + artH - 60);
    ctx.lineTo(px + pw / 2, py);
    ctx.lineTo(px + pw, artY + artH - 60);
    ctx.closePath();
    ctx.fill();
  }

  // Pixel ground inside art window
  const groundTop = artY + artH - 62;
  ctx.fillStyle = biome.grassTop;
  ctx.fillRect(artX, groundTop, artW, 14);
  ctx.fillStyle = biome.grassTrim;
  ctx.fillRect(artX, groundTop + 14, artW, 6);
  ctx.fillStyle = biome.dirtMain;
  ctx.fillRect(artX, groundTop + 20, artW, 42);
  ctx.fillStyle = biome.dirtDark;
  for (let gx = artX + 12; gx < artX + artW - 12; gx += 36) {
    ctx.fillRect(gx, groundTop + 30, 18, 8);
    ctx.fillRect(gx + 14, groundTop + 46, 14, 8);
  }

  // Hero spotlight glow
  ctx.fillStyle = rarityInfo.glow;
  ctx.fillRect(W / 2 - 84, groundTop - 160, 168, 160);

  // Draw Hero Sprite at 7x crisp pixel scale + Wardrobe!
  const spriteSet =
    characterId === 'custom' && customCharacter
      ? buildCustomSpriteSet(customCharacter)
      : characterId === 'custom'
        ? CHARACTER_SPRITES.stride
        : (CHARACTER_SPRITES[characterId] ?? CHARACTER_SPRITES.stride);
  const heroGrid = spriteSet.frames.run[0];
  const heroScale = 7;
  const heroW = heroGrid[0].length;
  const heroH = heroGrid.length;
  const heroCenterX = Math.round(W / 2 + (petId !== 'none' ? 28 : 0));
  const heroFeetY = groundTop + 2;

  // Ground shadow
  ctx.fillStyle = 'rgba(10, 12, 24, 0.45)';
  ctx.fillRect(heroCenterX - 44, groundTop - 4, 88, 10);

  // Cape behind hero
  drawCardHeroExtras(ctx, 'none', capeId, heroCenterX, heroFeetY, heroW, heroH, heroScale);
  // Hero sprite
  drawPixelGrid(
    ctx,
    heroGrid,
    spriteSet.palette,
    Math.round(heroCenterX - (heroW * heroScale) / 2),
    Math.round(heroFeetY - heroH * heroScale),
    heroScale,
    false,
  );
  // Hat on top of hero
  drawCardHeroExtras(ctx, hatId, 'none', heroCenterX, heroFeetY, heroW, heroH, heroScale);

  // Draw equipped Pet Companion beside hero!
  if (petId !== 'none') {
    const petScale = 6;
    const petX = heroCenterX - 134;
    if (petId === 'fox') {
      ctx.fillStyle = 'rgba(10, 12, 24, 0.35)';
      ctx.fillRect(petX + 6, groundTop - 4, 48, 8);
      drawPixelGrid(ctx, PET_FOX_SPRITE, PET_FOX_PALETTE, petX, groundTop - 54, petScale, false);
    } else if (petId === 'owl') {
      drawPixelGrid(ctx, PET_OWL_SPRITE, PET_OWL_PALETTE, petX, groundTop - 132, petScale, false);
    } else if (petId === 'dragon') {
      drawPixelGrid(ctx, PET_DRAGON_SPRITE, PET_DRAGON_PALETTE, petX, groundTop - 122, petScale, false);
    }
  }

  // Hero Nameplate Ribbon overlaid at bottom of art frame
  ctx.fillStyle = 'rgba(12, 16, 36, 0.88)';
  ctx.fillRect(artX + 12, artY + 12, 260, 42);
  ctx.strokeStyle = rarityInfo.borderOuter;
  ctx.lineWidth = 2;
  ctx.strokeRect(artX + 12, artY + 12, 260, 42);
  ctx.font = 'bold 14px "Press Start 2P", monospace';
  ctx.textAlign = 'left';
  ctx.fillStyle = '#FFFFFF';
  ctx.fillText(spriteSet.name, artX + 26, artY + 39);

  // 4. Level & Biome Banner Strip
  ctx.fillStyle = '#1D254A';
  ctx.fillRect(50, 486, W - 100, 42);
  ctx.strokeStyle = '#3B4A84';
  ctx.lineWidth = 2;
  ctx.strokeRect(50, 486, W - 100, 42);
  ctx.font = 'bold 12px "Press Start 2P", monospace';
  ctx.textAlign = 'center';
  ctx.fillStyle = '#78F0A8';
  ctx.fillText(`LV ${summary.level} ${levelName} · ${summary.zoneName}`, W / 2, 513);

  // 5. Primary Score Hero Box
  ctx.fillStyle = '#10142A';
  ctx.fillRect(50, 542, W - 100, 96);
  ctx.strokeStyle = rarityInfo.borderOuter;
  ctx.lineWidth = 3;
  ctx.strokeRect(50, 542, W - 100, 96);

  ctx.font = 'bold 11px "Press Start 2P", monospace';
  ctx.fillStyle = '#A8B8E8';
  ctx.fillText('FINAL SCORE', W / 2, 568);
  ctx.font = 'bold 34px "Press Start 2P", monospace';
  ctx.fillStyle = '#FFE05C';
  ctx.fillText(summary.score.toLocaleString(), W / 2, 620);

  // 6. 4-Cell Stat Grid (BEST, DIST, COINS, BOSS KO / STOMPS)
  const stats = [
    { label: 'BEST SCORE', value: summary.bestScore.toLocaleString(), color: '#FFFFFF' },
    { label: 'DISTANCE', value: `${summary.distance}M`, color: '#78E8FF' },
    { label: 'COINS', value: `${summary.coins} C`, color: '#FFE05C' },
    {
      label: summary.bossesDefeated > 0 ? 'BOSSES KO' : 'STOMPS',
      value: summary.bossesDefeated > 0 ? `${summary.bossesDefeated} KO` : String(summary.stomps),
      color: summary.bossesDefeated > 0 ? '#FF6888' : '#78F0A8',
    },
  ];
  const cellW = (W - 100 - 16) / 2;
  const cellH = 74;
  for (let i = 0; i < stats.length; i++) {
    const col = i % 2;
    const row = Math.floor(i / 2);
    const cx = 50 + col * (cellW + 16);
    const cy = 654 + row * (cellH + 14);
    ctx.fillStyle = '#182042';
    ctx.fillRect(cx, cy, cellW, cellH);
    ctx.strokeStyle = '#38467C';
    ctx.lineWidth = 2;
    ctx.strokeRect(cx, cy, cellW, cellH);

    ctx.font = 'bold 10px "Press Start 2P", monospace';
    ctx.textAlign = 'center';
    ctx.fillStyle = '#98ACDC';
    ctx.fillText(stats[i].label, cx + cellW / 2, cy + 26);

    ctx.font = 'bold 18px "Press Start 2P", monospace';
    ctx.fillStyle = stats[i].color;
    ctx.fillText(stats[i].value, cx + cellW / 2, cy + 56);
  }

  // 7. Trading Card Footer
  const dateStr = data.dateLabel ?? new Date().toISOString().slice(0, 10);
  ctx.font = 'bold 10px "Press Start 2P", monospace';
  ctx.textAlign = 'left';
  ctx.fillStyle = '#8898C8';
  ctx.fillText(`PIXEL STRIDE v3 · ${dateStr}`, 52, H - 50);

  ctx.textAlign = 'right';
  ctx.fillStyle = rarityInfo.accent;
  ctx.fillText('PLAY FREE ON WEB & ANDROID', W - 52, H - 50);
}

export function runCardCanvasToBlob(canvas: HTMLCanvasElement): Promise<Blob | null> {
  return new Promise((resolve) => {
    canvas.toBlob((blob) => resolve(blob), 'image/png');
  });
}
