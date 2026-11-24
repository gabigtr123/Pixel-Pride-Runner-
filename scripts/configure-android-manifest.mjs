import fs from 'node:fs';
import path from 'node:path';
import { pathToFileURL } from 'node:url';

const REQUIRED_PERMISSIONS = [
  {
    name: 'android.permission.ACCESS_COARSE_LOCATION',
    tag: '    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />',
  },
  {
    name: 'android.permission.ACCESS_FINE_LOCATION',
    tag: '    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />',
  },
  {
    name: 'android.permission.READ_EXTERNAL_STORAGE',
    tag: '    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />',
  },
  {
    name: 'android.permission.WRITE_EXTERNAL_STORAGE',
    tag: '    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="29" />',
  },
];

const OPTIONAL_FEATURES = [
  {
    name: 'android.hardware.location.gps',
    tag: '    <uses-feature android:name="android.hardware.location.gps" android:required="false" />',
  },
];

export function patchAndroidManifest(manifestXml) {
  let updated = manifestXml;

  if (!updated.includes('androidx.core.content.FileProvider')) {
    throw new Error('AndroidManifest.xml is missing androidx.core.content.FileProvider required by @capacitor/share.');
  }

  // Enable requestLegacyExternalStorage for Android 10 (API 29) Documents writes; ignored on Android 11+ (Scoped Storage)
  if (!updated.includes('android:requestLegacyExternalStorage=')) {
    updated = updated.replace(
      /<application\b([^>]*)>/,
      (match, attrs) => `<application${attrs}\n        android:requestLegacyExternalStorage="true">`,
    );
  }

  // Add location permissions (@capacitor/geolocation) and SDK-capped legacy storage permissions (@capacitor/filesystem)
  const missingTags = [
    ...REQUIRED_PERMISSIONS.filter((p) => !updated.includes(p.name)).map((p) => p.tag),
    ...OPTIONAL_FEATURES.filter((f) => !updated.includes(f.name)).map((f) => f.tag),
  ];
  if (missingTags.length > 0) {
    updated = updated.replace(
      /<\/manifest>\s*$/,
      `${missingTags.join('\n')}\n</manifest>\n`,
    );
  }

  return updated;
}

export function verifyFilePathsXml(filePathsXml) {
  if (!filePathsXml.includes('<cache-path')) {
    throw new Error('res/xml/file_paths.xml is missing <cache-path> required by @capacitor/share.');
  }
}

function main() {
  const androidMainDir = path.resolve(import.meta.dirname, '../android/app/src/main');
  const manifestPath = path.join(androidMainDir, 'AndroidManifest.xml');
  const filePathsPath = path.join(androidMainDir, 'res/xml/file_paths.xml');

  if (!fs.existsSync(manifestPath)) {
    console.error('AndroidManifest.xml not found. Run `npx cap add android` first.');
    process.exit(1);
  }

  if (!fs.existsSync(filePathsPath)) {
    console.error('res/xml/file_paths.xml not found. Run `npx cap add android` first.');
    process.exit(1);
  }

  const filePathsXml = fs.readFileSync(filePathsPath, 'utf8');
  verifyFilePathsXml(filePathsXml);

  const manifestXml = fs.readFileSync(manifestPath, 'utf8');
  const patched = patchAndroidManifest(manifestXml);
  fs.writeFileSync(manifestPath, patched, 'utf8');

  console.log('Verified FileProvider & file_paths.xml, and configured AndroidManifest.xml permissions for @capacitor/share + @capacitor/filesystem.');
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  main();
}
