import React, { useEffect, useRef, useState } from 'react';
import {
  detectDeviceLocation,
  fetchLiveWeather,
  getWeatherEmoji,
  PRESET_CITIES,
  searchCity,
  type CitySearchResult,
  type WeatherCondition,
  type WeatherSettings,
  type WeatherSource,
} from '../game/weather';

interface WeatherModalProps {
  open: boolean;
  settings: WeatherSettings;
  onChange: (next: WeatherSettings) => void;
  onClose: () => void;
}

const WEATHER_OPTIONS: { id: WeatherCondition; label: string }[] = [
  { id: 'clear', label: 'CLEAR' },
  { id: 'rain', label: 'RAIN' },
  { id: 'storm', label: 'STORM' },
  { id: 'snow', label: 'SNOW' },
  { id: 'fog', label: 'FOG' },
];

export function WeatherModal({ open, settings, onChange, onClose }: WeatherModalProps) {
  const [cityQuery, setCityQuery] = useState('');
  const [results, setResults] = useState<CitySearchResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [statusMsg, setStatusMsg] = useState<string | null>(null);
  const settingsRef = useRef(settings);
  settingsRef.current = settings;
  const abortRef = useRef<AbortController | null>(null);

  useEffect(() => {
    if (!open) {
      abortRef.current?.abort();
      abortRef.current = null;
    }
    return () => {
      abortRef.current?.abort();
      abortRef.current = null;
    };
  }, [open]);

  if (!open) return null;

  const beginRequest = () => {
    abortRef.current?.abort();
    const ctrl = new AbortController();
    abortRef.current = ctrl;
    return ctrl;
  };

  const syncCoordinates = async (lat: number, lon: number, label: string) => {
    const ctrl = beginRequest();
    setLoading(true);
    setStatusMsg(null);
    try {
      const live = await fetchLiveWeather(lat, lon, ctrl.signal);
      if (ctrl.signal.aborted) return;
      onChange({
        ...settingsRef.current,
        source: 'live',
        liveWeather: live.weather,
        liveIsDay: live.isDay,
        temperatureC: live.temperatureC,
        locationName: label.toUpperCase(),
        latitude: lat,
        longitude: lon,
        lastSyncedAt: Date.now(),
      });
      setResults([]);
      setStatusMsg(
        `Synced: ${label} (${live.weather.toUpperCase()}, ${live.temperatureC}°C, ${live.isDay ? 'DAY' : 'NIGHT'})`,
      );
    } catch {
      if (ctrl.signal.aborted) return;
      setStatusMsg('Could not reach Open-Meteo weather service. Check internet connection.');
    } finally {
      if (abortRef.current === ctrl) {
        abortRef.current = null;
        setLoading(false);
      }
    }
  };

  const handleDetectLocation = async () => {
    const ctrl = beginRequest();
    setLoading(true);
    setStatusMsg('Detecting device location...');
    const loc = await detectDeviceLocation();
    if (ctrl.signal.aborted) return;
    if (!loc.ok) {
      setLoading(false);
      setStatusMsg(loc.message);
      return;
    }
    await syncCoordinates(loc.latitude, loc.longitude, 'MY LOCATION');
  };

  const handleSearchCity = async (e: React.FormEvent) => {
    e.preventDefault();
    if (cityQuery.trim().length < 2) return;
    const ctrl = beginRequest();
    setLoading(true);
    setStatusMsg(null);
    try {
      const found = await searchCity(cityQuery, ctrl.signal);
      if (ctrl.signal.aborted) return;
      setResults(found);
      if (found.length === 0) {
        setStatusMsg('No matching city found. Try another name.');
      }
    } catch {
      if (ctrl.signal.aborted) return;
      setStatusMsg('City search failed. Check internet connection.');
    } finally {
      if (abortRef.current === ctrl) {
        abortRef.current = null;
        setLoading(false);
      }
    }
  };

  const setSource = (source: WeatherSource) => {
    onChange({ ...settingsRef.current, source });
  };

  return (
    <div
      className="fixed inset-0 z-40 flex items-center justify-center modal-safe-area bg-[#0B0D1A]/75"
      role="dialog"
      aria-modal="true"
      aria-labelledby="weather-modal-title"
      data-testid="weather-modal"
    >
      <div className="pixel-panel w-full max-w-md max-h-full overflow-y-auto p-4 sm:p-5 flex flex-col gap-3.5">
        {/* Header */}
        <div className="flex items-center justify-between gap-2 border-b-2 border-[#303A68] pb-2.5">
          <div>
            <h2
              id="weather-modal-title"
              className="font-pixel-display text-sm sm:text-base text-[#78E8FF] pixel-text-shadow"
            >
              THEME &amp; WEATHER
            </h2>
            <p className="font-pixel-ui text-[10px] text-[#B8C4E4] mt-0.5">
              WINTER MODE &amp; LIVE WEATHER SYNC (ALPHA)
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close Weather and Theme"
            className="pixel-icon-btn px-2.5 py-1.5 font-pixel-display text-[10px] cursor-pointer"
          >
            X
          </button>
        </div>

        {/* Section 1: Seasonal Theme */}
        <div className="flex flex-col gap-1.5">
          <span className="font-pixel-display text-[9px] text-[#FFE05C] tracking-wider">
            SEASONAL THEME
          </span>
          <div className="grid grid-cols-2 gap-2">
            <button
              type="button"
              onClick={() => onChange({ ...settings, theme: 'classic' })}
              className={`py-2 px-2 font-pixel-display text-[10px] border-2 cursor-pointer ${
                settings.theme === 'classic'
                  ? 'bg-[#293568] border-[#FFE05C] text-[#FFF4A8]'
                  : 'bg-[#12162D] border-[#3A467A] text-[#A2B0D8]'
              }`}
            >
              ☀️ CLASSIC
            </button>
            <button
              type="button"
              data-testid="toggle-winter-theme"
              onClick={() => onChange({ ...settings, theme: 'winter' })}
              className={`py-2 px-2 font-pixel-display text-[10px] border-2 cursor-pointer ${
                settings.theme === 'winter'
                  ? 'bg-[#1B3A66] border-[#78E8FF] text-[#E8FCFF]'
                  : 'bg-[#12162D] border-[#3A467A] text-[#A2B0D8]'
              }`}
            >
              ❄️ WINTER
            </button>
          </div>
        </div>

        {/* Section 2: Day / Night Cycle */}
        <div className="flex flex-col gap-1.5">
          <span className="font-pixel-display text-[9px] text-[#FFE05C] tracking-wider">
            DAY / NIGHT CYCLE
          </span>
          <div className="grid grid-cols-3 gap-1.5">
            {(
              [
                { id: 'auto', label: '🔄 AUTO' },
                { id: 'day', label: '☀️ DAY' },
                { id: 'night', label: '🌙 NIGHT' },
              ] as const
            ).map((opt) => (
              <button
                key={opt.id}
                type="button"
                onClick={() => onChange({ ...settings, timeOfDay: opt.id })}
                className={`py-1.5 px-1 font-pixel-display text-[8px] sm:text-[9px] border-2 cursor-pointer ${
                  settings.timeOfDay === opt.id
                    ? 'bg-[#293568] border-[#FFE05C] text-[#FFF4A8]'
                    : 'bg-[#12162D] border-[#3A467A] text-[#A2B0D8]'
                }`}
              >
                {opt.label}
              </button>
            ))}
          </div>
        </div>

        {/* Section 3: Weather Mode */}
        <div className="flex flex-col gap-2">
          <div className="flex items-center justify-between">
            <span className="font-pixel-display text-[9px] text-[#FFE05C] tracking-wider">
              WEATHER ENGINE
            </span>
            <span className="font-pixel-ui text-[9px] text-[#78F0A8] bg-[#142A26] border border-[#48C878] px-1.5 py-0.5">
              ALPHA
            </span>
          </div>

          <div className="grid grid-cols-3 gap-1.5">
            {(
              [
                { id: 'biome', label: 'BIOME AUTO' },
                { id: 'live', label: 'LIVE SYNC' },
                { id: 'manual', label: 'MANUAL' },
              ] as const
            ).map((m) => (
              <button
                key={m.id}
                type="button"
                onClick={() => setSource(m.id)}
                className={`py-1.5 px-1 font-pixel-display text-[8px] sm:text-[9px] border-2 cursor-pointer ${
                  settings.source === m.id
                    ? 'bg-[#293568] border-[#FFE05C] text-[#FFF4A8]'
                    : 'bg-[#12162D] border-[#3A467A] text-[#A2B0D8]'
                }`}
              >
                {m.label}
              </button>
            ))}
          </div>

          {settings.source === 'biome' && (
            <p className="font-pixel-ui text-[11px] text-[#B8C4E4] bg-[#12162D] border border-[#303A68] p-2.5">
              {settings.theme === 'winter'
                ? 'Winter Theme is ON — snowfall & frosty terrain with full Day/Night cycle.'
                : 'Weather follows each biome automatically with dynamic Day/Night cycle.'}
            </p>
          )}

          {settings.source === 'manual' && (
            <div className="grid grid-cols-3 sm:grid-cols-5 gap-1.5">
              {WEATHER_OPTIONS.map((opt) => (
                <button
                  key={opt.id}
                  type="button"
                  onClick={() =>
                    onChange({ ...settings, source: 'manual', manualWeather: opt.id })
                  }
                  className={`py-2 px-1 border-2 flex flex-col items-center gap-1 cursor-pointer ${
                    settings.manualWeather === opt.id
                      ? 'bg-[#293568] border-[#FFE05C] text-[#FFF4A8]'
                      : 'bg-[#12162D] border-[#3A467A] text-[#C8D2EE]'
                  }`}
                >
                  <span className="text-sm leading-none">{getWeatherEmoji(opt.id)}</span>
                  <span className="font-pixel-display text-[8px]">{opt.label}</span>
                </button>
              ))}
            </div>
          )}

          {settings.source === 'live' && (
            <div className="flex flex-col gap-2.5 bg-[#12162D] border-2 border-[#303A68] p-2.5">
              {/* Current Synced Status */}
              {settings.liveWeather && (
                <div className="flex items-center justify-between gap-2 bg-[#1A2448] border border-[#4C64A8] px-2.5 py-2">
                  <div className="flex items-center gap-2">
                    <span className="text-base">{getWeatherEmoji(settings.liveWeather)}</span>
                    <div className="flex flex-col">
                      <span className="font-pixel-display text-[9px] text-[#FFF4A8]">
                        {settings.locationName || 'SYNCED LOCATION'}
                      </span>
                      <span className="font-pixel-ui text-[10px] text-[#78E8FF]">
                        {settings.liveWeather.toUpperCase()}
                        {settings.temperatureC !== null ? ` · ${settings.temperatureC}°C` : ''}
                        {settings.liveIsDay !== null
                          ? ` · ${settings.liveIsDay ? 'DAY' : 'NIGHT'}`
                          : ''}
                      </span>
                    </div>
                  </div>
                  {settings.latitude !== null && settings.longitude !== null && (
                    <button
                      type="button"
                      disabled={loading}
                      onClick={() =>
                        void syncCoordinates(
                          settings.latitude!,
                          settings.longitude!,
                          settings.locationName || 'MY LOCATION',
                        )
                      }
                      className="px-2 py-1 font-pixel-display text-[8px] bg-[#253264] border border-[#78E8FF] text-[#E8FCFF] cursor-pointer"
                    >
                      REFRESH
                    </button>
                  )}
                </div>
              )}

              {/* Device GPS Button */}
              <button
                type="button"
                disabled={loading}
                onClick={() => void handleDetectLocation()}
                className="pixel-btn-primary w-full py-2 font-pixel-display text-[9px] cursor-pointer"
              >
                {loading ? 'SYNCING...' : '📍 DETECT DEVICE LOCATION'}
              </button>

              {/* City Search */}
              <form onSubmit={handleSearchCity} className="flex gap-1.5">
                <input
                  type="text"
                  value={cityQuery}
                  onChange={(e) => setCityQuery(e.target.value)}
                  placeholder="Search city (e.g. Brasov)"
                  aria-label="Search city for live weather"
                  className="flex-1 bg-[#0B0E1D] border-2 border-[#4C5C96] px-2 py-1.5 font-pixel-ui text-xs text-[#FFFFFF] focus:border-[#FFE05C] focus:outline-none"
                />
                <button
                  type="submit"
                  disabled={loading}
                  className="px-2.5 py-1.5 font-pixel-display text-[9px] bg-[#252C52] border-2 border-[#96A4D4] text-[#F8F6EE] cursor-pointer"
                >
                  GO
                </button>
              </form>

              {/* Search Results */}
              {results.length > 0 && (
                <div className="flex flex-col gap-1">
                  {results.map((c) => (
                    <button
                      key={`${c.name}-${c.latitude}-${c.longitude}`}
                      type="button"
                      disabled={loading}
                      onClick={() =>
                        void syncCoordinates(
                          c.latitude,
                          c.longitude,
                          c.country ? `${c.name}, ${c.country}` : c.name,
                        )
                      }
                      className="text-left px-2 py-1.5 bg-[#192040] border border-[#4C5C96] hover:border-[#FFE05C] font-pixel-ui text-[11px] text-[#F8F6EE] cursor-pointer disabled:opacity-60"
                    >
                      📍 {c.name}
                      {c.country ? `, ${c.country}` : ''}
                    </button>
                  ))}
                </div>
              )}

              {/* Quick Cities */}
              <div className="flex flex-wrap gap-1">
                {PRESET_CITIES.map((city) => (
                  <button
                    key={city.name}
                    type="button"
                    disabled={loading}
                    onClick={() =>
                      void syncCoordinates(
                        city.latitude,
                        city.longitude,
                        `${city.name}, ${city.country}`,
                      )
                    }
                    className="px-2 py-1 bg-[#171C38] border border-[#303A68] hover:border-[#78E8FF] font-pixel-ui text-[10px] text-[#C8D2EE] cursor-pointer"
                  >
                    {city.name}
                  </button>
                ))}
              </div>

              {statusMsg && (
                <p className="font-pixel-ui text-[10px] text-[#FFE05C]">{statusMsg}</p>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
