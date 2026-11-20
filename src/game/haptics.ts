// Lightweight Android/Web haptic feedback helper (safe no-op where unsupported)

export function triggerHaptic(kind: 'jump' | 'stomp' | 'powerup' | 'hurt' | 'mission'): void {
  if (typeof navigator === 'undefined' || typeof navigator.vibrate !== 'function') {
    return;
  }
  try {
    switch (kind) {
      case 'jump':
        navigator.vibrate(8);
        break;
      case 'stomp':
        navigator.vibrate(16);
        break;
      case 'powerup':
        navigator.vibrate([12, 20, 12]);
        break;
      case 'hurt':
        navigator.vibrate([28, 30, 28]);
        break;
      case 'mission':
        navigator.vibrate([15, 25, 20]);
        break;
    }
  } catch {
    // Ignore vibration errors in restricted contexts
  }
}
