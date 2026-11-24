import type { AudioEvent } from './types';

// Authentic 8-bit NES-inspired Web Audio API chiptune SFX & music loop engine
export class ChiptuneAudio {
  private ctx: AudioContext | null = null;
  private noiseBuffer: AudioBuffer | null = null;
  private muted = false;
  private musicTimer: number | null = null;
  private musicStep = 0;
  private musicPlaying = false;
  private biomeIndex = 0;

  // Distinct 16-step retro chiptune lead/bass themes per biome
  private readonly biomeThemes: { lead: number[]; bass: number[] }[] = [
    // 0: Sunlit Highlands (Upbeat C-major adventure)
    {
      lead: [523.25, 0, 659.25, 783.99, 659.25, 0, 880.0, 783.99, 659.25, 587.33, 523.25, 0, 587.33, 659.25, 783.99, 0],
      bass: [130.81, 130.81, 196.0, 130.81, 174.61, 174.61, 220.0, 174.61, 146.83, 146.83, 220.0, 146.83, 196.0, 196.0, 246.94, 196.0],
    },
    // 1: Amber Canyon (Driving D-minor desert groove)
    {
      lead: [587.33, 0, 659.25, 698.46, 880.0, 0, 698.46, 659.25, 587.33, 523.25, 587.33, 0, 698.46, 659.25, 523.25, 0],
      bass: [146.83, 146.83, 220.0, 146.83, 174.61, 174.61, 220.0, 174.61, 130.81, 130.81, 196.0, 130.81, 164.81, 164.81, 220.0, 164.81],
    },
    // 2: Starlight Citadel (Mysterious A-minor synth arp)
    {
      lead: [440.0, 523.25, 659.25, 880.0, 783.99, 0, 659.25, 523.25, 493.88, 587.33, 783.99, 0, 659.25, 587.33, 523.25, 0],
      bass: [110.0, 110.0, 164.81, 110.0, 130.81, 130.81, 196.0, 130.81, 123.47, 123.47, 185.0, 123.47, 164.81, 164.81, 220.0, 164.81],
    },
    // 3: Dawn Peaks (Bright G-major heroic climb)
    {
      lead: [392.0, 0, 493.88, 587.33, 783.99, 0, 659.25, 587.33, 523.25, 587.33, 659.25, 0, 783.99, 659.25, 587.33, 0],
      bass: [196.0, 196.0, 146.83, 196.0, 164.81, 164.81, 246.94, 164.81, 130.81, 130.81, 196.0, 130.81, 146.83, 146.83, 220.0, 146.83],
    },
    // 4: Frost Tundra (Crystalline F-major sparkle)
    {
      lead: [698.46, 0, 880.0, 1046.5, 880.0, 0, 783.99, 698.46, 659.25, 0, 783.99, 880.0, 698.46, 0, 523.25, 0],
      bass: [174.61, 174.61, 261.63, 174.61, 146.83, 146.83, 220.0, 146.83, 130.81, 130.81, 196.0, 130.81, 174.61, 174.61, 220.0, 174.61],
    },
    // 5: Volcanic Forge (Intense E-minor boss energy)
    {
      lead: [659.25, 659.25, 0, 783.99, 739.99, 659.25, 0, 587.33, 659.25, 0, 987.77, 880.0, 783.99, 739.99, 659.25, 0],
      bass: [164.81, 164.81, 164.81, 246.94, 146.83, 146.83, 146.83, 220.0, 130.81, 130.81, 196.0, 130.81, 146.83, 164.81, 196.0, 246.94],
    },
  ];

  public setBiome(zoneIndex: number): void {
    this.biomeIndex = Math.abs(zoneIndex) % this.biomeThemes.length;
  }

  public isMuted(): boolean {
    return this.muted;
  }

  public setMuted(muted: boolean): void {
    this.muted = muted;
    if (muted) {
      this.stopMusic();
      if (this.ctx && this.ctx.state === 'running') {
        void this.ctx.suspend().catch(() => {});
      }
    }
  }

  public toggleMute(): boolean {
    this.setMuted(!this.muted);
    return this.muted;
  }

  private ensureContext(): AudioContext | null {
    if (this.muted) return null;
    if (typeof window === 'undefined') return null;
    if (!this.ctx) {
      const AudioCtor =
        window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
      if (!AudioCtor) return null;
      this.ctx = new AudioCtor();
    }
    if (this.ctx.state === 'suspended') {
      void this.ctx.resume().catch(() => {});
    }
    return this.ctx;
  }

  private getNoiseBuffer(ctx: AudioContext): AudioBuffer {
    if (this.noiseBuffer && this.noiseBuffer.sampleRate === ctx.sampleRate) {
      return this.noiseBuffer;
    }
    const sampleCount = Math.max(1, Math.floor(ctx.sampleRate * 0.25));
    const buffer = ctx.createBuffer(1, sampleCount, ctx.sampleRate);
    const data = buffer.getChannelData(0);
    for (let i = 0; i < sampleCount; i++) {
      data[i] = Math.random() * 2 - 1;
    }
    this.noiseBuffer = buffer;
    return buffer;
  }

  private playTone(
    type: OscillatorType,
    freqStart: number,
    freqEnd: number,
    duration: number,
    gainPeak: number,
    delay = 0,
  ): void {
    const ctx = this.ensureContext();
    if (!ctx) return;
    const now = ctx.currentTime + delay;
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.type = type;
    osc.frequency.setValueAtTime(freqStart, now);
    if (freqEnd !== freqStart) {
      osc.frequency.exponentialRampToValueAtTime(Math.max(24, freqEnd), now + duration);
    }
    gain.gain.setValueAtTime(0.0001, now);
    gain.gain.linearRampToValueAtTime(gainPeak, now + 0.008);
    gain.gain.exponentialRampToValueAtTime(0.0001, now + duration);
    osc.connect(gain);
    gain.connect(ctx.destination);
    osc.onended = () => {
      osc.disconnect();
      gain.disconnect();
    };
    osc.start(now);
    osc.stop(now + duration + 0.02);
  }

  private playNoise(duration: number, gainPeak: number): void {
    const ctx = this.ensureContext();
    if (!ctx) return;
    const src = ctx.createBufferSource();
    src.buffer = this.getNoiseBuffer(ctx);
    const gain = ctx.createGain();
    const now = ctx.currentTime;
    gain.gain.setValueAtTime(gainPeak, now);
    gain.gain.exponentialRampToValueAtTime(0.0001, now + duration);
    src.connect(gain);
    gain.connect(ctx.destination);
    src.onended = () => {
      src.disconnect();
      gain.disconnect();
    };
    src.start(now);
    src.stop(now + duration + 0.02);
  }

  private bossMode = false;
  private readonly bossTheme = {
    lead: [440, 466.16, 523.25, 0, 587.33, 523.25, 466.16, 440, 659.25, 0, 587.33, 523.25, 466.16, 440, 392, 0],
    bass: [110, 110, 146.83, 110, 130.81, 130.81, 164.81, 130.81, 116.54, 116.54, 146.83, 116.54, 110, 130.81, 146.83, 164.81],
  };

  public setBossMode(active: boolean): void {
    this.bossMode = active;
  }

  public startMusic(): void {
    if (this.muted || this.musicPlaying || typeof window === 'undefined') return;
    this.musicPlaying = true;
    this.musicTimer = window.setInterval(() => {
      if (this.muted || !this.musicPlaying) return;
      const theme = this.bossMode
        ? this.bossTheme
        : (this.biomeThemes[this.biomeIndex] ?? this.biomeThemes[0]);
      const lead = theme.lead[this.musicStep % theme.lead.length];
      const bass = theme.bass[this.musicStep % theme.bass.length];
      if (lead > 0) {
        this.playTone(this.bossMode ? 'sawtooth' : 'square', lead, lead, 0.09, this.bossMode ? 0.028 : 0.022);
      }
      if (bass > 0) {
        this.playTone('triangle', bass, bass, 0.12, this.bossMode ? 0.048 : 0.04);
      }
      this.musicStep += 1;
    }, 145);
  }

  public stopMusic(): void {
    this.musicPlaying = false;
    if (this.musicTimer !== null && typeof window !== 'undefined') {
      window.clearInterval(this.musicTimer);
      this.musicTimer = null;
    }
  }

  public playEvents(events: AudioEvent[]): void {
    if (events.length === 0) return;
    for (let i = 0; i < events.length; i++) {
      const ev = events[i];
      if (this.muted) continue;

      switch (ev.type) {
        case 'jump':
          this.playTone('square', 220 * ev.pitchScale, 540 * ev.pitchScale, 0.11, 0.07);
          break;
        case 'doubleJump':
          this.playTone('square', 360 * ev.pitchScale, 780 * ev.pitchScale, 0.12, 0.065);
          this.playTone('triangle', 520 * ev.pitchScale, 920 * ev.pitchScale, 0.09, 0.05, 0.03);
          break;
        case 'land':
          this.playTone('triangle', 120, 62, 0.055, 0.05);
          break;
        case 'slide':
          this.playNoise(0.065, 0.035);
          break;
        case 'coin': {
          const boost = 1 + (ev.combo - 1) * 0.09;
          this.playTone('square', 987.77 * boost, 987.77 * boost, 0.05, 0.06);
          this.playTone('square', 1318.51 * boost, 1318.51 * boost, 0.11, 0.06, 0.05);
          break;
        }
        case 'powerup':
          this.playTone('square', 523.25, 523.25, 0.07, 0.065, 0);
          this.playTone('square', 659.25, 659.25, 0.07, 0.065, 0.06);
          this.playTone('square', 783.99, 783.99, 0.07, 0.065, 0.12);
          this.playTone('square', 1046.5, 1046.5, 0.15, 0.07, 0.18);
          break;
        case 'stomp':
          this.playTone('square', 190, 520, 0.1, 0.08);
          this.playNoise(0.06, 0.05);
          break;
        case 'telegraph':
          this.playTone('square', 620, 860, 0.07, 0.035);
          break;
        case 'shieldBreak':
          this.playTone('sawtooth', 720, 180, 0.18, 0.075);
          this.playNoise(0.11, 0.05);
          break;
        case 'hurt':
          this.playTone('sawtooth', 240, 74, 0.22, 0.09);
          this.playNoise(0.14, 0.07);
          break;
        case 'gameOver':
          this.stopMusic();
          this.playTone('square', 440, 440, 0.14, 0.075, 0);
          this.playTone('square', 392, 392, 0.14, 0.075, 0.14);
          this.playTone('square', 349.23, 349.23, 0.14, 0.075, 0.28);
          this.playTone('square', 261.63, 196, 0.34, 0.08, 0.42);
          break;
        case 'start':
          this.musicStep = 0;
          this.playTone('square', 523.25, 523.25, 0.06, 0.06, 0);
          this.playTone('square', 659.25, 659.25, 0.06, 0.06, 0.06);
          this.playTone('square', 1046.5, 1046.5, 0.14, 0.07, 0.12);
          break;
        case 'zoneUp':
          this.playTone('square', 587.33, 587.33, 0.07, 0.055, 0);
          this.playTone('square', 880.0, 880.0, 0.14, 0.06, 0.07);
          break;
      }
    }
    events.length = 0;
  }

  public dispose(): void {
    this.stopMusic();
    if (this.ctx) {
      void this.ctx.close().catch(() => {});
      this.ctx = null;
    }
  }
}
