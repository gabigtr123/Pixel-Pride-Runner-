// Hand-crafted 8-bit pixel-art sprite matrices rendered via integer rectangles
// Palette keys map single characters to authentic NES/Famicom colors

export type PixelGrid = readonly string[];

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

// Lacramioara — Lily-of-the-valley retro adventurer (16x19 run, 16x17 jump, 16x11 duck)
export const LACRAMIOARA_PALETTE: Record<string, string> = {
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

const LACRAMIOARA_RUN_0: PixelGrid = [
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

const LACRAMIOARA_RUN_1: PixelGrid = [
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

const LACRAMIOARA_RUN_2: PixelGrid = [
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

const LACRAMIOARA_JUMP: PixelGrid = [
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

const LACRAMIOARA_DUCK: PixelGrid = [
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

export const LACRAMIOARA_FRAMES = {
  run: [LACRAMIOARA_RUN_0, LACRAMIOARA_RUN_1, LACRAMIOARA_RUN_2, LACRAMIOARA_RUN_1],
  jump: LACRAMIOARA_JUMP,
  duck: LACRAMIOARA_DUCK,
};

export interface CharacterSpriteSet {
  id: 'stride' | 'lacramioara';
  name: string;
  palette: Record<string, string>;
  frames: {
    run: PixelGrid[];
    jump: PixelGrid;
    duck: PixelGrid;
  };
}

export const CHARACTER_SPRITES: Record<'stride' | 'lacramioara', CharacterSpriteSet> = {
  stride: {
    id: 'stride',
    name: 'STRIDE',
    palette: RUNNER_PALETTE,
    frames: RUNNER_FRAMES,
  },
  lacramioara: {
    id: 'lacramioara',
    name: 'LACRAMIOARA',
    palette: LACRAMIOARA_PALETTE,
    frames: LACRAMIOARA_FRAMES,
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
