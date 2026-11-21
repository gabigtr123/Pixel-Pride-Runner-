import type { BiomePalette } from './types';

export type ThemeMode = 'classic' | 'winter';
export type WeatherCondition = 'clear' | 'rain' | 'storm' | 'snow' | 'fog';
export type WeatherSource = 'biome' | 'live' | 'manual';

export interface WeatherSettings {
  theme: ThemeMode;
  source: WeatherSource;
  manualWeather: WeatherCondition;
  liveWeather: WeatherCondition | null;
  locationName: string;
  temperatureC: number | null;
  latitude: number | null;
  longitude: number | null;
  lastSyncedAt: number | null;
}

export interface CitySearchResult {
  name: string;
  country: string;
  latitude: number;
  longitude: number;
}

export const WEATHER_STORAGE_KEY = 'pixel_stride_weather_v1';

export const PRESET_CITIES: CitySearchResult[] = [
  { name: 'Bucharest', country: 'RO', latitude: 44.4268, longitude: 26.1025 },
  { name: 'London', country: 'GB', latitude: 51.5074, longitude: -0.1278 },
  { name: 'New York', country: 'US', latitude: 40.7128, longitude: -74.006 },
  { name: 'Tokyo', country: 'JP', latitude: 35.6762, longitude: 139.6503 },
  { name: 'Oslo', country: 'NO', latitude: 59.9139, longitude: 10.7522 },
  { name: 'Reykjavik', country: 'IS', latitude: 64.1466, longitude: -21.9426 },
];

export const WINTER_BIOME_PALETTE: BiomePalette = {
  name: 'WINTER WONDERLAND',
  skyTop: '#162C58',
  skyMid: '#3464A8',
  skyHorizon: '#B8E4FF',
  celestialColor: '#F4FAFF',
  celestialType: 'moon',
  starAlpha: 0.72,
  cloudMain: '#F4FAFF',
  cloudShadow: '#A8D0F0',
  farMountain: '#3B6298',
  nearHill: '#6E9CC8',
  ruinColor: '#8EB4D8',
  ruinHighlight: '#E8F6FF',
  grassTop: '#F4FAFF',
  grassTrim: '#98D4F8',
  dirtMain: '#4A6888',
  dirtDark: '#263B56',
  dirtAccent: '#78A0C4',
};

export function createDefaultWeatherSettings(): WeatherSettings {
  return {
    theme: 'classic',
    source: 'biome',
    manualWeather: 'snow',
    liveWeather: null,
    locationName: '',
    temperatureC: null,
    latitude: null,
    longitude: null,
    lastSyncedAt: null,
  };
}

/**
 * Maps WMO Weather interpretation codes (Open-Meteo) to 5 pixel-art weather types.
 */
export function mapWmoCodeToWeather(code: number): WeatherCondition {
  if (code === 45 || code === 48) return 'fog';
  if (code >= 95 && code <= 99) return 'storm';
  if ((code >= 71 && code <= 77) || code === 85 || code === 86) return 'snow';
  if (
    (code >= 51 && code <= 67) ||
    (code >= 80 && code <= 82)
  ) {
    return 'rain';
  }
  return 'clear';
}

export function getWeatherEmoji(weather: WeatherCondition): string {
  switch (weather) {
    case 'snow':
      return '❄️';
    case 'rain':
      return '🌧️';
    case 'storm':
      return '⛈️';
    case 'fog':
      return '🌫️';
    default:
      return '☀️';
  }
}

export function getEffectiveWeather(settings: WeatherSettings): WeatherCondition | null {
  if (settings.source === 'manual') return settings.manualWeather;
  if (settings.source === 'live' && settings.liveWeather) return settings.liveWeather;
  if (settings.theme === 'winter') return 'snow';
  return null;
}

export async function fetchLiveWeather(
  latitude: number,
  longitude: number,
): Promise<{ weather: WeatherCondition; temperatureC: number }> {
  const url =
    `https://api.open-meteo.com/v1/forecast?latitude=${encodeURIComponent(latitude.toFixed(4))}` +
    `&longitude=${encodeURIComponent(longitude.toFixed(4))}&current=weather_code,temperature_2m`;
  const res = await fetch(url);
  if (!res.ok) {
    throw new Error(`Weather API error (${res.status})`);
  }
  const data = (await res.json()) as {
    current?: { weather_code?: number; temperature_2m?: number };
  };
  const code = typeof data.current?.weather_code === 'number' ? data.current.weather_code : 0;
  const temp = typeof data.current?.temperature_2m === 'number' ? Math.round(data.current.temperature_2m) : 0;
  return {
    weather: mapWmoCodeToWeather(code),
    temperatureC: temp,
  };
}

export async function searchCity(query: string): Promise<CitySearchResult[]> {
  const trimmed = query.trim();
  if (trimmed.length < 2) return [];
  const url = `https://geocoding-api.open-meteo.com/v1/search?name=${encodeURIComponent(trimmed)}&count=4&language=en&format=json`;
  const res = await fetch(url);
  if (!res.ok) {
    throw new Error(`Geocoding API error (${res.status})`);
  }
  const data = (await res.json()) as {
    results?: Array<{
      name?: string;
      country_code?: string;
      latitude?: number;
      longitude?: number;
    }>;
  };
  if (!Array.isArray(data.results)) return [];
  return data.results
    .filter(
      (r): r is { name: string; country_code?: string; latitude: number; longitude: number } =>
        typeof r.name === 'string' && typeof r.latitude === 'number' && typeof r.longitude === 'number',
    )
    .map((r) => ({
      name: r.name,
      country: r.country_code ?? '',
      latitude: r.latitude,
      longitude: r.longitude,
    }));
}

export function loadWeatherSettings(): WeatherSettings {
  try {
    const raw = window.localStorage.getItem(WEATHER_STORAGE_KEY);
    if (!raw) return createDefaultWeatherSettings();
    const parsed = JSON.parse(raw) as Partial<WeatherSettings>;
    const def = createDefaultWeatherSettings();
    const validWeather = (v: unknown): v is WeatherCondition =>
      v === 'clear' || v === 'rain' || v === 'storm' || v === 'snow' || v === 'fog';
    return {
      theme: parsed.theme === 'winter' ? 'winter' : 'classic',
      source: parsed.source === 'live' || parsed.source === 'manual' ? parsed.source : 'biome',
      manualWeather: validWeather(parsed.manualWeather) ? parsed.manualWeather : def.manualWeather,
      liveWeather: validWeather(parsed.liveWeather) ? parsed.liveWeather : null,
      locationName: typeof parsed.locationName === 'string' ? parsed.locationName.slice(0, 32) : '',
      temperatureC:
        typeof parsed.temperatureC === 'number' && Number.isFinite(parsed.temperatureC)
          ? Math.round(parsed.temperatureC)
          : null,
      latitude:
        typeof parsed.latitude === 'number' && Number.isFinite(parsed.latitude) ? parsed.latitude : null,
      longitude:
        typeof parsed.longitude === 'number' && Number.isFinite(parsed.longitude) ? parsed.longitude : null,
      lastSyncedAt:
        typeof parsed.lastSyncedAt === 'number' && Number.isFinite(parsed.lastSyncedAt)
          ? parsed.lastSyncedAt
          : null,
    };
  } catch {
    return createDefaultWeatherSettings();
  }
}

export function saveWeatherSettings(settings: WeatherSettings): void {
  try {
    window.localStorage.setItem(WEATHER_STORAGE_KEY, JSON.stringify(settings));
  } catch {
    // Ignore storage failures
  }
}
