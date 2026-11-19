import type { AudioEvent } from './types';

// Authentic 8-bit NES-inspired Web Audio API chiptune SFX & music loop engine
export class ChiptuneAudio {
  private ctx: AudioContext | null = null;
  private muted = false;
  private musicTimer: number | null = null;
  private musicStep = 0;
  private musicPlaying = false;

  // Upbeat 16-step C-major / A-minor retro arcade bass + lead pattern (Hz)
  private readonly leadNotes = [
    523.25, 0, 659.25, 783.99, 659.25, 0, 880.0, 783.99,
    659.25, 587.33, 523.25, 0, 587.33, 659.25, 783.99, 0,
  ];
  private readonly bassNotes = [
    130.81, 130.81, 196.0, 130.81, 174.61, 174.61, 220.0, 174.61,
    146.83, 146.83, 220.0, 146.83, 196.0, 196.0, 246.94, 196.0,
  ];

  public isMuted(): boolean {
    return this.muted;
  }

  public setMuted(muted: boolean): void {
    this.muted = muted;
    if (muted) {
      this.stopMusic();
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
    osc.start(now);
    osc.stop(now + duration + 0.02);
  }

  private playNoise(duration: number, gainPeak: number): void {
    const ctx = this.ensureContext();
    if (!ctx) return;
    const sampleCount = Math.max(1, Math.floor(ctx.sampleRate * duration));
    const buffer = ctx.createBuffer(1, sampleCount, ctx.sampleRate);
    const data = buffer.getChannelData(0);
    for (let i = 0; i < sampleCount; i++) {
      data[i] = Math.random() * 2 - 1;
    }
    const src = ctx.createBufferSource();
    src.buffer = buffer;
    const gain = ctx.createGain();
    const now = ctx.currentTime;
    gain.gain.setValueAtTime(gainPeak, now);
    gain.gain.exponentialRampToValueAtTime(0.0001, now + duration);
    src.connect(gain);
    gain.connect(ctx.destination);
    src.start(now);
  }

  public startMusic(): void {
    if (this.muted || this.musicPlaying || typeof window === 'undefined') return;
    this.musicPlaying = true;
    this.musicTimer = window.setInterval(() => {
      if (this.muted || !this.musicPlaying) return;
      const lead = this.leadNotes[this.musicStep % this.leadNotes.length];
      const bass = this.bassNotes[this.musicStep % this.bassNotes.length];
      if (lead > 0) {
        this.playTone('square', lead, lead, 0.095, 0.022);
      }
      if (bass > 0) {
        this.playTone('triangle', bass, bass, 0.12, 0.04);
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
