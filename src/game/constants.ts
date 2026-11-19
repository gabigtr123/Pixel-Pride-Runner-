import type { BiomePalette } from './types';

// Native 16:9 retro pixel resolution (cleanly scales on 1440x900 and mobile)
export const VIRTUAL_WIDTH = 480;
export const VIRTUAL_HEIGHT = 270;

export const GROUND_Y = 224;
export const PLAYER_X = 76;

export const PLAYER_WIDTH = 16;
export const PLAYER_STAND_HEIGHT = 24;
export const PLAYER_DUCK_HEIGHT = 13;

// Platformer feel constants (pixels/sec & pixels/sec^2)
export const GRAVITY = 1080;
export const FAST_FALL_GRAVITY = 2400;
export const JUMP_VELOCITY = -340;
export const DOUBLE_JUMP_VELOCITY = -305;
export const STOMP_BOUNCE_VELOCITY = -295;
export const JUMP_CUT_MULTIPLIER = 0.45;
export const COYOTE_TIME = 0.11;
export const JUMP_BUFFER_TIME = 0.12;

export const BASE_SPEED = 148;
export const MAX_SPEED = 295;
export const SAFE_OPENING_SECONDS = 2.6;
export const DISTANCE_PER_ZONE = 400;

export const MAX_PARTICLES = 96;
export const MAX_FLOATING_TEXTS = 16;
export const COMBO_DURATION = 3.2;

export const BEST_SCORE_STORAGE_KEY = 'pixel_stride_best_score_v1';

export const BIOMES: BiomePalette[] = [
  {
    name: 'SUNLIT HIGHLANDS',
    skyTop: '#3C74E6',
    skyMid: '#68A4FC',
    skyHorizon: '#A8D8FF',
    celestialType: 'sun',
    celestialColor: '#FFF4A8',
    starAlpha: 0,
    cloudMain: '#FFFFFF',
    cloudShadow: '#C8E0F8',
    farMountain: '#4468B0',
    nearHill: '#2C8854',
    ruinColor: '#566480',
    ruinHighlight: '#8898B8',
    grassTop: '#48C848',
    grassTrim: '#208834',
    dirtMain: '#A85C30',
    dirtDark: '#6C3618',
    dirtAccent: '#D08048',
  },
  {
    name: 'AMBER CANYON',
    skyTop: '#6C2848',
    skyMid: '#C85434',
    skyHorizon: '#F8B050',
    celestialType: 'sunset',
    celestialColor: '#FFE084',
    starAlpha: 0.18,
    cloudMain: '#FFD49C',
    cloudShadow: '#D07450',
    farMountain: '#7C3448',
    nearHill: '#9C422C',
    ruinColor: '#5E282C',
    ruinHighlight: '#B0583C',
    grassTop: '#E49838',
    grassTrim: '#A8581E',
    dirtMain: '#843824',
    dirtDark: '#521E18',
    dirtAccent: '#B85C34',
  },
  {
    name: 'STARLIGHT CITADEL',
    skyTop: '#0E1230',
    skyMid: '#1C2454',
    skyHorizon: '#34407A',
    celestialType: 'moon',
    celestialColor: '#F4F6D8',
    starAlpha: 1,
    cloudMain: '#404E88',
    cloudShadow: '#262F5C',
    farMountain: '#1B2248',
    nearHill: '#263458',
    ruinColor: '#36446C',
    ruinHighlight: '#6074A8',
    grassTop: '#38A88C',
    grassTrim: '#1E6860',
    dirtMain: '#383E5C',
    dirtDark: '#20243C',
    dirtAccent: '#58628A',
  },
  {
    name: 'DAWN PEAKS',
    skyTop: '#38306C',
    skyMid: '#8C4E78',
    skyHorizon: '#F4A488',
    celestialType: 'dawn',
    celestialColor: '#FFF0C4',
    starAlpha: 0.35,
    cloudMain: '#FFE6DC',
    cloudShadow: '#C48494',
    farMountain: '#54447C',
    nearHill: '#385C78',
    ruinColor: '#485070',
    ruinHighlight: '#90A0C4',
    grassTop: '#78D0B8',
    grassTrim: '#3C8888',
    dirtMain: '#545E78',
    dirtDark: '#30384E',
    dirtAccent: '#8290B0',
  },
];
