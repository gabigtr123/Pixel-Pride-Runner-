// Hand-crafted 8-bit pixel-art sprite matrices rendered via integer rectangles
// Palette keys map single characters to authentic NES/Famicom colors

export type PixelGrid = readonly string[];

const spriteCache = new WeakMap<PixelGrid, WeakMap<Record<string, string>, Map<number, HTMLCanvasElement>>>();
const flashSpriteCache = new WeakMap<PixelGrid, Map<number, HTMLCanvasElement>>();

function getCachedSpriteCanvas(
  grid: PixelGrid,
  palette: Record<string, string>,
  pixelSize: number,
  whiteFlash: boolean,
): HTMLCanvasElement | null {
  if (typeof document === 'undefined' || grid.length === 0) return null;
  let sizeMap: Map<number, HTMLCanvasElement> | undefined;
  if (whiteFlash) {
    sizeMap = flashSpriteCache.get(grid);
    if (!sizeMap) {
      sizeMap = new Map();
      flashSpriteCache.set(grid, sizeMap);
    }
  } else {
    let palMap = spriteCache.get(grid);
    if (!palMap) {
      palMap = new WeakMap();
      spriteCache.set(grid, palMap);
    }
    sizeMap = palMap.get(palette);
    if (!sizeMap) {
      sizeMap = new Map();
      palMap.set(palette, sizeMap);
    }
  }

  const existing = sizeMap.get(pixelSize);
  if (existing) return existing;

  let maxCols = 0;
  for (let r = 0; r < grid.length; r++) {
    if (grid[r].length > maxCols) maxCols = grid[r].length;
  }
  if (maxCols === 0) return null;

  const offscreen = document.createElement('canvas');
  offscreen.width = Math.max(1, Math.ceil(maxCols * pixelSize));
  offscreen.height = Math.max(1, Math.ceil(grid.length * pixelSize));
  const octx = offscreen.getContext('2d');
  if (!octx) return null;
  octx.imageSmoothingEnabled = false;
  for (let row = 0; row < grid.length; row++) {
    const line = grid[row];
    for (let col = 0; col < line.length; col++) {
      const ch = line[col];
      if (ch === '.' || ch === ' ') continue;
      const color = whiteFlash ? '#FFFFFF' : palette[ch];
      if (!color) continue;
      octx.fillStyle = color;
      octx.fillRect(col * pixelSize, row * pixelSize, pixelSize, pixelSize);
    }
  }
  sizeMap.set(pixelSize, offscreen);
  return offscreen;
}

export function drawPixelGrid(
  ctx: CanvasRenderingContext2D,
  grid: PixelGrid,
  palette: Record<string, string>,
  x: number,
  y: number,
  pixelSize = 1,
  whiteFlash = false,
): void {
  const bx = Math.round(x);
  const by = Math.round(y);
  const cached = getCachedSpriteCanvas(grid, palette, pixelSize, whiteFlash);
  if (cached && typeof ctx.drawImage === 'function') {
    ctx.drawImage(cached, bx, by);
    return;
  }
  for (let row = 0; row < grid.length; row++) {
    const line = grid[row];
    for (let col = 0; col < line.length; col++) {
      const ch = line[col];
      if (ch === '.' || ch === ' ') continue;
      const color = whiteFlash ? '#FFFFFF' : palette[ch];
      if (!color) continue;
      ctx.fillStyle = color;
      ctx.fillRect(bx + col * pixelSize, by + row * pixelSize, pixelSize, pixelSize);
    }
  }
}

export const RUNNER_PALETTE: Record<string, string> = {
  K: '#121324', // dark outline
  H: '#304488', // navy helmet/cap
  h: '#5878D0', // cap highlight
  V: '#FFE066', // amber visor goggles
  S: '#FFCC98', // skin
  R: '#E83B48', // crimson scarf
  r: '#A41E34', // scarf shade
  T: '#F4F2E8', // tunic white
  t: '#B8C2DC', // tunic shade
  B: '#283668', // shorts/pants
  O: '#F09028', // boots/accent
  o: '#9E4C14', // boot shade
};

// 16x24 retro adventurer sprites (pixelSize = 1 on 480x270 canvas, super crisp)
const RUNNER_RUN_0: PixelGrid = [
  '.....KKKKKK.....',
  '....KhhhhHHK....',
  '...KhHHHHHHHK...',
  '...KHHKVVVVKK...',
  '...KSSKVVVVK....',
  '....KSSSSSK.....',
  '..KKKRRRRRKK....',
  '.KRrRRRRRRRK....',
  'Krr..KTTTTK.....',
  '.....KTTtTK.....',
  '....KTTTTTTK....',
  '....KTSKTtTK....',
  '....KK.KBBBK....',
  '.......KBBBK....',
  '......KBK.KBK...',
  '.....KBK...KBK..',
  '....KOK.....KOK.',
  '....KOoK....KOoK',
  '....KKKK....KKKK',
];

const RUNNER_RUN_1: PixelGrid = [
  '.....KKKKKK.....',
  '....KhhhhHHK....',
  '...KhHHHHHHHK...',
  '...KHHKVVVVKK...',
  '...KSSKVVVVK....',
  '....KSSSSSK.....',
  '.KKKKRRRRRKK....',
  'KRrRRRRRRRRK....',
  '.K...KTTTTK.....',
  '.....KTTtTK.....',
  '.....KTTTTK.....',
  '.....KTtTSK.....',
  '......KBBBK.....',
  '......KBBBK.....',
  '......KBKBK.....',
  '......KBKBK.....',
  '......KOKOK.....',
  '.....KOoKOoK....',
  '.....KKKKKKK....',
];

const RUNNER_RUN_2: PixelGrid = [
  '.....KKKKKK.....',
  '....KhhhhHHK....',
  '...KhHHHHHHHK...',
  '...KHHKVVVVKK...',
  '...KSSKVVVVK....',
  '....KSSSSSK.....',
  '..KKKRRRRRKK....',
  'KrrRRRRRRRRK....',
  '.KK..KTTTTK.....',
  '.....KTTtTK.....',
  '....KTTTTTTK....',
  '....KTtTKTSK....',
  '.....KBBBK.KK...',
  '.....KBBBK......',
  '....KBK.KBK.....',
  '....KBK..KBK....',
  '...KOK....KOK...',
  '...KOoK...KOoK..',
  '...KKKK...KKKK..',
];

const RUNNER_JUMP: PixelGrid = [
  '.....KKKKKK.....',
  '....KhhhhHHK....',
  '...KhHHHHHHHK...',
  '...KHHKVVVVKK...',
  '...KSSKVVVVK....',
  '....KSSSSSK.....',
  '..KKKRRRRRKK....',
  '.KrrRRRRRRRK....',
  'Kr...KTTTTKKK...',
  '.....KTTtTTTK...',
  '....KTTTTKTSK...',
  '....KBBBK.KK....',
  '...KBKKBBK......',
  '..KOK..KBK......',
  '..KOoK..KOK.....',
  '..KKKK..KOoK....',
  '........KKKK....',
];

const RUNNER_DUCK: PixelGrid = [
  '.....KKKKKK.....',
  '....KhhhhHHK....',
  '...KhHHHHHHHK...',
  '..KKHHKVVVVKK...',
  'KrrRRRRRRRRK....',
  '.KKKTTTTTTTK....',
  '...KTTtTTTSK....',
  '..KBBBBBBBK.....',
  '.KOKK..KOKK.....',
  '.KOoK..KOoK.....',
  '.KKKK..KKKK.....',
];

export const RUNNER_FRAMES = {
  run: [RUNNER_RUN_0, RUNNER_RUN_1, RUNNER_RUN_2, RUNNER_RUN_1],
  jump: RUNNER_JUMP,
  duck: RUNNER_DUCK,
};

// Lily — Lily-of-the-valley retro adventurer (16x19 run, 16x17 jump, 16x11 duck)
export const LILY_PALETTE: Record<string, string> = {
  K: '#121324', // dark outline
  H: '#7B3428', // chestnut hair & ponytail
  h: '#C86844', // hair highlight
  F: '#FFFDF5', // white lily blossom
  L: '#48C868', // emerald leaf hairband
  S: '#FFD0A4', // warm skin
  E: '#223868', // eye
  R: '#E85898', // rose-magenta scarf ribbon
  r: '#A82864', // ribbon shade
  T: '#38B87C', // emerald tunic
  t: '#207C58', // tunic shade
  B: '#2E2854', // deep plum leggings
  O: '#F4C848', // golden boots
  o: '#B8781C', // boot shade
};

const LILY_RUN_0: PixelGrid = [
  '.....KKKKKK.....',
  '....KhhHHFFK....',
  '...KhHHLLLHHK...',
  '..KHHHKSSSSEK...',
  '.KHHhKKSSSSSK...',
  '..KK..KSSSSK....',
  '..KKKRRRRRKK....',
  '.KRrRRRRRRRK....',
  'Krr..KTTTTK.....',
  '.....KTTtTK.....',
  '....KTTTTTTK....',
  '....KTSKTtTK....',
  '....KK.KBBBK....',
  '.......KBBBK....',
  '......KBK.KBK...',
  '.....KBK...KBK..',
  '....KOK.....KOK.',
  '....KOoK....KOoK',
  '....KKKK....KKKK',
];

const LILY_RUN_1: PixelGrid = [
  '.....KKKKKK.....',
  '....KhhHHFFK....',
  '...KhHHLLLHHK...',
  '..KHHHKSSSSEK...',
  '.KHHhKKSSSSSK...',
  '..KK..KSSSSK....',
  '.KKKKRRRRRKK....',
  'KRrRRRRRRRRK....',
  '.K...KTTTTK.....',
  '.....KTTtTK.....',
  '.....KTTTTK.....',
  '.....KTtTSK.....',
  '......KBBBK.....',
  '......KBBBK.....',
  '......KBKBK.....',
  '......KBKBK.....',
  '......KOKOK.....',
  '.....KOoKOoK....',
  '.....KKKKKKK....',
];

const LILY_RUN_2: PixelGrid = [
  '.....KKKKKK.....',
  '....KhhHHFFK....',
  '...KhHHLLLHHK...',
  '..KHHHKSSSSEK...',
  '.KHHhKKSSSSSK...',
  '..KK..KSSSSK....',
  '..KKKRRRRRKK....',
  'KrrRRRRRRRRK....',
  '.KK..KTTTTK.....',
  '.....KTTtTK.....',
  '....KTTTTTTK....',
  '....KTtTKTSK....',
  '.....KBBBK.KK...',
  '.....KBBBK......',
  '....KBK.KBK.....',
  '....KBK..KBK....',
  '...KOK....KOK...',
  '...KOoK...KOoK..',
  '...KKKK...KKKK..',
];

const LILY_JUMP: PixelGrid = [
  '.....KKKKKK.....',
  '....KhhHHFFK....',
  '...KhHHLLLHHK...',
  '.KKHHHKSSSSEK...',
  'KHHhKKKSSSSSK...',
  '.KK...KSSSSK....',
  '..KKKRRRRRKK....',
  '.KrrRRRRRRRK....',
  'Kr...KTTTTKKK...',
  '.....KTTtTTTK...',
  '....KTTTTKTSK...',
  '....KBBBK.KK....',
  '...KBKKBBK......',
  '..KOK..KBK......',
  '..KOoK..KOK.....',
  '..KKKK..KOoK....',
  '........KKKK....',
];

const LILY_DUCK: PixelGrid = [
  '.....KKKKKK.....',
  '....KhhHHFFK....',
  '..KKhHHLLLHHK...',
  '.KHHhHKSSSSEK...',
  'KrrRRRRRRRRK....',
  '.KKKTTTTTTTK....',
  '...KTTtTTTSK....',
  '..KBBBBBBBK.....',
  '.KOKK..KOKK.....',
  '.KOoK..KOoK.....',
  '.KKKK..KKKK.....',
];

export const LILY_FRAMES = {
  run: [LILY_RUN_0, LILY_RUN_1, LILY_RUN_2, LILY_RUN_1],
  jump: LILY_JUMP,
  duck: LILY_DUCK,
};

// Pixel Knight — Armored 4-HP retro champion (16x19 run, 16x17 jump, 16x11 duck)
export const KNIGHT_PALETTE: Record<string, string> = {
  K: '#121324', // dark outline
  H: '#8CA0C4', // steel helm
  h: '#DCE8FF', // steel highlight
  V: '#58E8FF', // glowing cyan visor
  S: '#48587C', // inner visor shadow
  R: '#4888FF', // royal cobalt plume/cape
  r: '#2448A8', // cape shade
  T: '#A8B8D8', // chestplate silver
  t: '#647498', // armor shade
  B: '#384468', // greaves
  O: '#FFE05C', // gilded boots
  o: '#B88418', // boot shade
};

export interface CharacterSpriteSet {
  id: 'stride' | 'lily' | 'knight' | 'custom';
  name: string;
  perk: string;
  palette: Record<string, string>;
  frames: {
    run: PixelGrid[];
    jump: PixelGrid;
    duck: PixelGrid;
  };
}

export const CHARACTER_SPRITES: Record<'stride' | 'lily' | 'knight', CharacterSpriteSet> = {
  stride: {
    id: 'stride',
    name: 'STRIDE',
    perk: '+25% STOMP BONUS',
    palette: RUNNER_PALETTE,
    frames: RUNNER_FRAMES,
  },
  lily: {
    id: 'lily',
    name: 'LILY',
    perk: 'COIN AURA + PETALS',
    palette: LILY_PALETTE,
    frames: LILY_FRAMES,
  },
  knight: {
    id: 'knight',
    name: 'KNIGHT',
    perk: '4 MAX HEARTS',
    palette: KNIGHT_PALETTE,
    frames: RUNNER_FRAMES,
  },
};

// Shell Hopper (18x16 animated beetle critter)
export const HOPPER_PALETTE: Record<string, string> = {
  K: '#141122',
  G: '#38B858',
  g: '#1E7440',
  L: '#9CF068',
  W: '#FFFFFF',
  R: '#F04858',
  Y: '#FFE058',
};

export const HOPPER_IDLE: PixelGrid = [
  '....KKKKKKKKKK....',
  '..KKLLLLGGGGGGKK..',
  '.KLLGGGGGGGGggggK.',
  '.KLGWWKGGWWKggggK.',
  'KGGGWKKGGWKKgggggK',
  'KGGGGGGGGGGggggggK',
  'KggggRRRRRRggggggK',
  '.KKKKKKKKKKKKKKKK.',
  '..KYYK......KYYK..',
  '.KYYKK......KKYYK.',
  '.KKKK........KKKK.',
];

export const HOPPER_CROUCH: PixelGrid = [
  '..................',
  '...KKKKKKKKKKKK...',
  '.KKYYYYYYYYYYYYKK.',
  'KYYGWWKGGWWKgggggK',
  'KGGGWKKGGWKKgggggK',
  'KggggRRRRRRggggggK',
  'KKKKKKKKKKKKKKKKKK',
  '.KYYK........KYYK.',
  '.KKKK........KKKK.',
];

// Sky Swooper (20x14 winged retro bat-drone)
export const SWOOPER_PALETTE: Record<string, string> = {
  K: '#121024',
  P: '#7844B8',
  p: '#48247C',
  L: '#B47CF8',
  E: '#FFE048',
  R: '#FF4860',
};

export const SWOOPER_UP: PixelGrid = [
  'KKK..............KKK',
  'KLLKK..........KKLLK',
  '.KPPPKK.KKKK.KKPPPK.',
  '..KppppKPPPPKppppK..',
  '...KKKKPPLLPKKKKK...',
  '......KPEKPEK.......',
  '......KPPPPPK.......',
  '.......KPRPK........',
  '........KKK.........',
];

export const SWOOPER_DOWN: PixelGrid = [
  '........KKKK........',
  '.......KPPPPK.......',
  '......KPPLLPK.......',
  '...KKKKPEKPEKKKK....',
  '..KPPPKPPPPPKPPPK...',
  '.KLLppKKPRPKKppLLK..',
  'KppKK...KKK...KKppK.',
  'KKK.............KKK.',
];

// Spike Totem (14x18 stone & steel spikes)
export const SPIKE_PALETTE: Record<string, string> = {
  K: '#141628',
  M: '#D8E2F4',
  m: '#8A98B8',
  d: '#4A5470',
  R: '#E84454',
};

export const SPIKE_SPRITE: PixelGrid = [
  '......KK......',
  '.....KMMK.....',
  '.....KMmK.....',
  '....KMMmmK....',
  '....KMMmmK....',
  '...KMMmmdmK...',
  '..KKMMmmdmKK..',
  '.KMMKMMmmdMMK.',
  '.KMmKMMmmdMmK.',
  'KMMmKRRRRRmmdK',
  'KMMmKdddddmmdK',
  'KmmmdKdddKdddK',
  'KddddddddddddK',
  'KKKKKKKKKKKKKK',
];

// Gold Coin 4-frame spin (10x10)
export const COIN_PALETTE: Record<string, string> = {
  K: '#261810',
  Y: '#FFD838',
  L: '#FFF8B8',
  O: '#D88018',
};

export const COIN_FRAMES: PixelGrid[] = [
  [
    '..KKKKKK..',
    '.KLLYYYYK.',
    'KLYYOOYYOK',
    'KLYOYLYYOK',
    'KLYOYLYYOK',
    'KLYYOOYYOK',
    '.KYYOOOOYK',
    '..KKKKKK..',
  ],
  [
    '...KKKK...',
    '..KLYYYK..',
    '..KLYOYK..',
    '..KLYOYK..',
    '..KLYOYK..',
    '..KLYOYK..',
    '..KYYOOK..',
    '...KKKK...',
  ],
  [
    '....KK....',
    '...KLYK...',
    '...KLYK...',
    '...KLYK...',
    '...KLYK...',
    '...KLYK...',
    '...KOYK...',
    '....KK....',
  ],
  [
    '...KKKK...',
    '..KLYYYK..',
    '..KLYOYK..',
    '..KLYOYK..',
    '..KLYOYK..',
    '..KLYOYK..',
    '..KYYOOK..',
    '...KKKK...',
  ],
];

// Powerup sprites (12x12)
export const SHIELD_PALETTE: Record<string, string> = {
  K: '#101830',
  C: '#58D8FF',
  L: '#E4FCFF',
  B: '#2878D8',
};

export const SHIELD_SPRITE: PixelGrid = [
  '..KKKKKKKK..',
  '.KLLLLCCCCK.',
  'KLCCLLLLCCBK',
  'KLCLLCCLLCBK',
  'KLCLLLLCCCBK',
  '.KCCBBBBCCK.',
  '..KCCBBCCK..',
  '...KCCCCK...',
  '....KKKK....',
];

export const HEART_PALETTE: Record<string, string> = {
  K: '#24101C',
  R: '#F03854',
  L: '#FFA4B4',
  D: '#9E1834',
};

export const HEART_SPRITE: PixelGrid = [
  '.KKK..KKK.',
  'KLLRKRRRDK',
  'KLRRRRRRDK',
  'KRRRRRRRDK',
  '.KRRRRRDK.',
  '..KRRRDK..',
  '...KRDK...',
  '....KK....',
];

export const MAGNET_PALETTE: Record<string, string> = {
  K: '#181024',
  R: '#F04068',
  M: '#E8EEF8',
  S: '#98A8C8',
};

export const MAGNET_SPRITE: PixelGrid = [
  '.KKK..KKK.',
  'KMMK..KMMK',
  'KSSK..KSSK',
  'KRRK..KRRK',
  'KRRK..KRRK',
  'KRRKKKKRRK',
  '.KRRRRRRK.',
  '..KKKKKK..',
];

export const DOUBLE_STAR_PALETTE: Record<string, string> = {
  K: '#241808',
  Y: '#FFE048',
  L: '#FFFDF0',
  O: '#F08820',
};

export const DOUBLE_STAR_SPRITE: PixelGrid = [
  '....KK....',
  '...KLYK...',
  '.KKLYYYKK.',
  'KLLYYYYYOK',
  '.KYYYYOOK.',
  '..KYYOOK..',
  '.KYYKKYOK.',
  '.KK....KK.',
];

export const SLOWMO_PALETTE: Record<string, string> = {
  K: '#10182C',
  G: '#FFE068',
  C: '#78F0FF',
  B: '#3888E8',
};

export const SLOWMO_SPRITE: PixelGrid = [
  '.KKKKKKKK.',
  '.KGGGGGGK.',
  '..KCCCCK..',
  '...KCCK...',
  '...KBBK...',
  '..KBBBBK..',
  '.KGGGGGGK.',
  '.KKKKKKKK.',
];

export const TURBO_DASH_PALETTE: Record<string, string> = {
  K: '#141224',
  W: '#FFFFFF',
  Y: '#FFE848',
  O: '#FF9820',
};

export const TURBO_DASH_SPRITE: PixelGrid = [
  '....KKKK..',
  '...KWWYK..',
  '..KWWYYK..',
  '.KWWYYKK..',
  'KWWYYYYYK.',
  'KKKYYYOK..',
  '..KYYOK...',
  '.KYYOK....',
  '.KOOOK....',
  '..KKK.....',
];

// v3 Pet Companion Sprites (10x9)
export const PET_FOX_PALETTE: Record<string, string> = {
  K: '#141224',
  O: '#F07824',
  L: '#FFB858',
  W: '#FFFDF5',
};

export const PET_FOX_SPRITE: PixelGrid = [
  '.KK....KK.',
  'KOWK..KOWK',
  'KOOOKKOOOK',
  'KOLLLLLLOK',
  'KOWKLLKWOK',
  '.KOOOOOOK.',
  'KKOOWWOOK.',
  'KWWK..K.K.',
  '.KK.......',
];

export const PET_OWL_PALETTE: Record<string, string> = {
  K: '#141224',
  C: '#A8DCFF',
  W: '#F4FAFF',
  Y: '#FFE048',
  B: '#4878B8',
};

export const PET_OWL_SPRITE: PixelGrid = [
  '..KK..KK..',
  '.KCCKKCCK.',
  'KCCWWWWCCK',
  'KCWYKKYWCK',
  'KCWWYYWWCK',
  'KBCWWWWCBK',
  '.KBWWWWBK.',
  '..KYYYYK..',
  '...KKKK...',
];

export const PET_DRAGON_PALETTE: Record<string, string> = {
  K: '#141224',
  R: '#E83848',
  O: '#FF9828',
  Y: '#FFE858',
  W: '#FFFFFF',
};

export const PET_DRAGON_SPRITE: PixelGrid = [
  '.KK....KK.',
  'KYYK..KRRK',
  '.KRRKKRRWK',
  'KRRRRRRKRY',
  'KROORRRRRK',
  '.KROOORRK.',
  '..KRRRRK..',
  '..K.KK.K..',
  '..........',
];

// v3 Mini-Boss Sprites (18x14, rendered at 2x scale -> 36x28px!)
export const BOSS_BEETLE_PALETTE: Record<string, string> = {
  K: '#121324',
  M: '#586888',
  L: '#98B0D8',
  R: '#FF4858',
  Y: '#FFE048',
};

export const BOSS_BEETLE_SPRITE: PixelGrid = [
  '.....KKKKKKKK.....',
  '...KKLLLLLLMMKK...',
  '..KLLLYYLLLLMMMK..',
  '.KLLMYYYYLLMMMMMK.',
  'KLLMMMMMMMMMMMMMMK',
  'KMRRMMLLMMLLMMMMMK',
  'KMRRMMMMMMMMMMMMMK',
  'KMMMMMMMMMMMMMMMMK',
  '.KKKKKKKKKKKKKKKK.',
  '..KMMK..KMMK..KMK.',
  '.KMMK..KMMK..KMMK.',
];

export const BOSS_GOLEM_PALETTE: Record<string, string> = {
  K: '#141224',
  S: '#C87838',
  L: '#F4B868',
  D: '#783C1C',
  G: '#FFE848',
};

export const BOSS_GOLEM_SPRITE: PixelGrid = [
  '....KKKKKKKKKK....',
  '...KLLLLLLLLSSK...',
  '..KLSSSSSSSSSSDK..',
  '.KLSSGGSSSSGGSSDK.',
  '.KSSSSSSSSSSSSSDK.',
  'KKLSSDDDDDDDDSSDK.',
  'KLSLLSSGGGGSSLLSDK',
  'KLSSSSSGGGGSSSSSDK',
  'KDDSSSSSSSSSSSSDDK',
  '.KKDDSSSDDDSSSDKK.',
  '..KDDDK....KDDDK..',
  '..KKKKK....KKKKK..',
];

export const BOSS_DRONE_PALETTE: Record<string, string> = {
  K: '#101428',
  B: '#384C78',
  L: '#88A8E0',
  C: '#48F4FF',
  R: '#FF4878',
};

export const BOSS_DRONE_SPRITE: PixelGrid = [
  '......KKKKKK......',
  '...KKKLLLLLLKKK...',
  '..KLLBBBBBBBBLLK..',
  '.KLBBCCCCCCCCBBLK.',
  'KLBBCCCRRRRCCCBBKK',
  'KBBBCCCRRRRCCCBBBK',
  'KLBBCCCCCCCCBBLKK.',
  '..KLLBBBBBBBBLLK..',
  '...KKKBBBBBBKKK...',
  '....KC......CK....',
  '....KK......KK....',
];

export const BOSS_WYRM_PALETTE: Record<string, string> = {
  K: '#121028',
  P: '#9848E8',
  L: '#E898FF',
  C: '#58F0FF',
  W: '#FFFFFF',
};

export const BOSS_WYRM_SPRITE: PixelGrid = [
  '..KK..........KK..',
  '.KCCK..KKKK..KCCK.',
  'KCCPKKPLLLLPKKPCCK',
  'KCPPPPPLLLPPPPPPPK',
  '.KPPWWPPPPWWPPPPK.',
  '..KPPPPCCPPPPPPK..',
  '...KPPCCCCPPPPK...',
  '..KLLPPPPPPPPLLK..',
  '.KCCK..KKKK..KCCK.',
  '..KK..........KK..',
];
