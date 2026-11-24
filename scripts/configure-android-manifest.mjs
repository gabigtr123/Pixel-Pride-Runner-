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

const APPLICATION_ATTRS = [
  { key: 'android:appCategory', value: 'game' },
  { key: 'android:enableOnBackInvokedCallback', value: 'true' },
  { key: 'android:usesCleartextTraffic', value: 'false' },
  { key: 'android:requestLegacyExternalStorage', value: 'true' },
];

export function computeVersionCode(version) {
  const match = String(version).trim().match(/^(\d+)\.(\d+)\.(\d+)/);
  if (!match) {
    throw new Error(`Invalid semver version "${version}" in package.json`);
  }
  const major = Number(match[1]);
  const minor = Number(match[2]);
  const patch = Number(match[3]);
  return major * 10000 + minor * 100 + patch;
}

export function patchAppBuildGradle(gradleText, version) {
  const versionCode = computeVersionCode(version);
  let updated = gradleText;

  // Ensure explicit targetSdkVersion for Android 17 (API 37)
  if (!updated.includes('targetSdkVersion') && !updated.includes('targetSdk ')) {
    updated = updated.replace(
      /(minSdkVersion\s+rootProject\.ext\.minSdkVersion)/,
      '$1\n        targetSdkVersion rootProject.ext.targetSdkVersion',
    );
  }

  updated = updated.replace(/versionCode\s+\d+/, `versionCode ${versionCode}`);
  updated = updated.replace(/versionName\s+"[^"]*"/, `versionName "${version}"`);

  return updated;
}

export function buildAndroid17StylesXml() {
  return `<?xml version="1.0" encoding="utf-8"?>
<resources>

    <!-- Base application theme -->
    <style name="AppTheme" parent="Theme.AppCompat.DayNight.NoActionBar">
        <item name="colorPrimary">@color/colorPrimary</item>
        <item name="colorPrimaryDark">@color/colorPrimaryDark</item>
        <item name="colorAccent">@color/colorAccent</item>
        <item name="android:windowBackground">#111324</item>
    </style>

    <!-- Edge-to-edge full-screen game theme for Android 15-17 (API 35-37) -->
    <style name="AppTheme.NoActionBar" parent="Theme.AppCompat.DayNight.NoActionBar">
        <item name="windowActionBar">false</item>
        <item name="windowNoTitle">true</item>
        <item name="android:windowBackground">#111324</item>
        <item name="android:windowLayoutInDisplayCutoutMode">always</item>
    </style>

    <!-- Native Android 12-17 Material SplashScreen -->
    <style name="AppTheme.NoActionBarLaunch" parent="Theme.SplashScreen">
        <item name="windowSplashScreenBackground">#111324</item>
        <item name="windowSplashScreenAnimatedIcon">@mipmap/ic_launcher</item>
        <item name="postSplashScreenTheme">@style/AppTheme.NoActionBar</item>
    </style>
</resources>
`;
}

export function patchAndroidManifest(manifestXml) {
  let updated = manifestXml;

  if (!updated.includes('androidx.core.content.FileProvider')) {
    throw new Error('AndroidManifest.xml is missing androidx.core.content.FileProvider required by @capacitor/share.');
  }

  // Add Android 14-17 game category, Predictive Back, HTTPS-only hardening, and API 29 legacy storage flag
  const missingAttrs = APPLICATION_ATTRS.filter((a) => !updated.includes(`${a.key}=`)).map(
    (a) => `        ${a.key}="${a.value}"`,
  );
  if (missingAttrs.length > 0) {
    updated = updated.replace(
      /<application\b([^>]*)>/,
      (_match, attrs) => `<application${attrs}\n${missingAttrs.join('\n')}>`,
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
  const rootDir = path.resolve(import.meta.dirname, '..');
  const pkg = JSON.parse(fs.readFileSync(path.join(rootDir, 'package.json'), 'utf8'));
  const androidAppDir = path.join(rootDir, 'android/app');
  const androidMainDir = path.join(androidAppDir, 'src/main');
  const manifestPath = path.join(androidMainDir, 'AndroidManifest.xml');
  const filePathsPath = path.join(androidMainDir, 'res/xml/file_paths.xml');
  const stylesPath = path.join(androidMainDir, 'res/values/styles.xml');
  const appGradlePath = path.join(androidAppDir, 'build.gradle');

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
  fs.writeFileSync(manifestPath, patchAndroidManifest(manifestXml), 'utf8');

  if (fs.existsSync(appGradlePath)) {
    const gradleText = fs.readFileSync(appGradlePath, 'utf8');
    fs.writeFileSync(appGradlePath, patchAppBuildGradle(gradleText, pkg.version), 'utf8');
  }

  fs.mkdirSync(path.dirname(stylesPath), { recursive: true });
  fs.writeFileSync(stylesPath, buildAndroid17StylesXml(), 'utf8');

  const versionCode = computeVersionCode(pkg.version);
  console.log(
    `Configured Android 17 APK (versionName=${pkg.version}, versionCode=${versionCode}, Game Mode, Predictive Back, Dark SplashScreen, Location & Storage permissions).`,
  );
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  main();
}
