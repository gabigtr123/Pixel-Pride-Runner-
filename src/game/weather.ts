import { Capacitor } from '@capacitor/core';
import { Geolocation } from '@capacitor/geolocation';
import { DAY_NIGHT_CYCLE_SECONDS } from './constants';
import type { BiomePalette } from './types';

export type ThemeMode = 'classic' | 'winter';
export type WeatherCondition = 'clear' | 'rain' | 'storm' | 'snow' | 'fog';
export type WeatherSource = 'biome' | 'live' | 'manual';
export type TimeOfDayMode = 'auto' | 'day' | 'night';

export interface WeatherSettings {
  theme: ThemeMode;
  timeOfDay: TimeOfDayMode;
  source: WeatherSource;
  manualWeather: WeatherCondition;
  liveWeather: WeatherCondition | null;
  liveIsDay: boolean | null;
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
  skyTop: '#2E72C4',
  skyMid: '#68B4F0',
  skyHorizon: '#D8F2FF',
  celestialColor: '#FFF6B8',
  celestialType: 'sun',
  starAlpha: 0,
  cloudMain: '#FFFFFF',
  cloudShadow: '#BCE0F8',
  farMountain: '#4E7CB2',
  nearHill: '#7EAAD4',
  ruinColor: '#9AC0E0',
  ruinHighlight: '#F0F9FF',
  grassTop: '#F4FAFF',
  grassTrim: '#98D4F8',
  dirtMain: '#4A6888',
  dirtDark: '#263B56',
  dirtAccent: '#78A0C4',
};

export function createDefaultWeatherSettings(): WeatherSettings {
  return {
    theme: 'classic',
    timeOfDay: 'auto',
    source: 'biome',
    manualWeather: 'snow',
    liveWeather: null,
    liveIsDay: null,
    locationName: '',
    temperatureC: null,
    latitude: null,
    longitude: null,
    lastSyncedAt: null,
  };
}

/**
 * Computes normalized night factor in [0, 1] where 0 = bright day, 0.5 = sunset/dawn, 1 = night.
 */
export function computeNightFactor(settings: WeatherSettings, elapsedSeconds: number): number {
  if (settings.timeOfDay === 'day') return 0;
  if (settings.timeOfDay === 'night') return 1;
  if (settings.source === 'live' && settings.liveIsDay !== null) {
    return settings.liveIsDay ? 0 : 1;
  }
  // Smooth 30-second Day -> Sunset -> Night -> Dawn cycle (3 cycles = 90s per level)
  return 0.5 - 0.5 * Math.cos((elapsedSeconds / DAY_NIGHT_CYCLE_SECONDS) * Math.PI * 2);
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
): Promise<{ weather: WeatherCondition; temperatureC: number; isDay: boolean }> {
  const url =
    `https://api.open-meteo.com/v1/forecast?latitude=${encodeURIComponent(latitude.toFixed(4))}` +
    `&longitude=${encodeURIComponent(longitude.toFixed(4))}&current=weather_code,temperature_2m,is_day`;
  const res = await fetch(url);
  if (!res.ok) {
    throw new Error(`Weather API error (${res.status})`);
  }
  const data = (await res.json()) as {
    current?: { weather_code?: number; temperature_2m?: number; is_day?: number };
  };
  const code = typeof data.current?.weather_code === 'number' ? data.current.weather_code : 0;
  const temp = typeof data.current?.temperature_2m === 'number' ? Math.round(data.current.temperature_2m) : 0;
  const isDay = data.current?.is_day !== 0;
  return {
    weather: mapWmoCodeToWeather(code),
    temperatureC: temp,
    isDay,
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

export type DeviceLocationResult =
  | { ok: true; latitude: number; longitude: number }
  | { ok: false; reason: 'unsupported' | 'denied' | 'disabled' | 'unavailable'; message: string };

export async function detectDeviceLocation(): Promise<DeviceLocationResult> {
  if (Capacitor.isNativePlatform()) {
    try {
      const status = await Geolocation.checkPermissions();
      const hasAccess =
        status.location === 'granted' || status.coarseLocation === 'granted';
      if (!hasAccess) {
        const requested = await Geolocation.requestPermissions({
          permissions: ['coarseLocation', 'location'],
        });
        if (
          requested.location !== 'granted' &&
          requested.coarseLocation !== 'granted'
        ) {
          return {
            ok: false,
            reason: 'denied',
            message: 'Location permission denied. Allow Location in Android Settings or pick a city below!',
          };
        }
      }
      const pos = await Geolocation.getCurrentPosition({
        enableHighAccuracy: false,
        timeout: 10000,
        maximumAge: 300000,
      });
      return {
        ok: true,
        latitude: pos.coords.latitude,
        longitude: pos.coords.longitude,
      };
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err ?? '');
      if (/disabled|services|location settings/i.test(msg)) {
        return {
          ok: false,
          reason: 'disabled',
          message: 'Device Location / GPS is turned off. Turn on Location or pick a city below!',
        };
      }
      if (/denied|permission/i.test(msg)) {
        return {
          ok: false,
          reason: 'denied',
          message: 'Location permission denied. Allow Location in Android Settings or pick a city below!',
        };
      }
      return {
        ok: false,
        reason: 'unavailable',
        message: 'Could not get GPS fix right now. Try again or pick a city below!',
      };
    }
  }

  if (typeof navigator === 'undefined' || !navigator.geolocation) {
    return {
      ok: false,
      reason: 'unsupported',
      message: 'Geolocation is not supported on this device. Pick a city below!',
    };
  }

  return new Promise<DeviceLocationResult>((resolve) => {
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        resolve({
          ok: true,
          latitude: pos.coords.latitude,
          longitude: pos.coords.longitude,
        });
      },
      (err) => {
        if (err.code === err.PERMISSION_DENIED) {
          resolve({
            ok: false,
            reason: 'denied',
            message: 'Location permission denied. Search or tap a city below!',
          });
        } else {
          resolve({
            ok: false,
            reason: 'unavailable',
            message: 'Could not determine location. Search or tap a city below!',
          });
        }
      },
      { enableHighAccuracy: false, timeout: 10000, maximumAge: 300000 },
    );
  });
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
      timeOfDay:
        parsed.timeOfDay === 'day' || parsed.timeOfDay === 'night' ? parsed.timeOfDay : 'auto',
      source: parsed.source === 'live' || parsed.source === 'manual' ? parsed.source : 'biome',
      manualWeather: validWeather(parsed.manualWeather) ? parsed.manualWeather : def.manualWeather,
      liveWeather: validWeather(parsed.liveWeather) ? parsed.liveWeather : null,
      liveIsDay: typeof parsed.liveIsDay === 'boolean' ? parsed.liveIsDay : null,
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
