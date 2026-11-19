import fs from 'node:fs';
import path from 'node:path';

const resDir = path.resolve(import.meta.dirname, '../android/app/src/main/res');

if (!fs.existsSync(resDir)) {
  console.error('Android res directory not found. Run `npx cap add android` first.');
  process.exit(1);
}

const drawableDir = path.join(resDir, 'drawable');
const mipmapAnyDpiDir = path.join(resDir, 'mipmap-anydpi-v26');
fs.mkdirSync(drawableDir, { recursive: true });
fs.mkdirSync(mipmapAnyDpiDir, { recursive: true });

// 1. Background layer (108x108 adaptive icon viewport — Sunlit Highlands 8-bit sky, sun, grass & earth)
const bgXml = `<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <!-- Sky top band -->
    <path android:fillColor="#3C74E6" android:pathData="M0,0h108v46h-108z" />
    <!-- Sky mid band -->
    <path android:fillColor="#68A4FC" android:pathData="M0,46h108v30h-108z" />
    <!-- Pixel sun -->
    <path android:fillColor="#FFF4A8" android:pathData="M74,22h12v12h-12z" />
    <!-- Ground outline & grass lip -->
    <path android:fillColor="#121526" android:pathData="M0,76h108v3h-108z" />
    <path android:fillColor="#48C848" android:pathData="M0,79h108v6h-108z" />
    <path android:fillColor="#208834" android:pathData="M0,85h108v4h-108z" />
    <!-- Sub-earth masonry -->
    <path android:fillColor="#A85C30" android:pathData="M0,89h108v19h-108z" />
    <path android:fillColor="#6C3618" android:pathData="M0,96h108v2h-108z" />
</vector>
`;

// 2. Foreground layer (108x108 viewport, crisp 8-bit runner centered inside 66dp safe zone [24..84])
const fgXml = `<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <!-- Dark 8-bit outline -->
    <path
        android:fillColor="#121324"
        android:pathData="M45,27h18v3h3v12h-3v3h3v6h-3v12h3v9h-3v6h-12v-6h-3v-3h-3v9h-12v-6h3v-9h3v-6h-3v-6h-12v-6h6v-3h12v-12h3z" />
    <!-- Navy cap & highlight -->
    <path android:fillColor="#5878D0" android:pathData="M48,30h12v3h-12z" />
    <path android:fillColor="#304488" android:pathData="M45,33h18v6h-18z" />
    <!-- Skin & amber visor -->
    <path android:fillColor="#FFCC98" android:pathData="M48,39h12v6h-12z" />
    <path android:fillColor="#FFE066" android:pathData="M54,39h9v3h-9z" />
    <!-- Flowing crimson scarf -->
    <path android:fillColor="#E83B48" android:pathData="M30,45h33v6h-33z" />
    <path android:fillColor="#A41E34" android:pathData="M27,48h12v3h-12z" />
    <!-- White tunic -->
    <path android:fillColor="#F4F2E8" android:pathData="M45,51h15v12h-15z" />
    <path android:fillColor="#B8C2DC" android:pathData="M54,54h6v6h-6z" />
    <!-- Navy shorts -->
    <path android:fillColor="#283668" android:pathData="M45,63h6v9h-6z M54,63h6v9h-6z" />
    <!-- Golden boots -->
    <path android:fillColor="#F09028" android:pathData="M39,72h12v6h-12z M54,72h12v6h-12z" />
    <!-- Floating pixel gold coin -->
    <path android:fillColor="#121324" android:pathData="M72,39h10v12h-10z" />
    <path android:fillColor="#FFD838" android:pathData="M74,41h6v8h-6z" />
    <path android:fillColor="#FFF8B8" android:pathData="M75,42h2v3h-2z" />
</vector>
`;

// 3. Monochrome layer for Android 13-17 Material You Themed Icons (single #FFFFFF tintable silhouette)
const monoXml = `<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <!-- Crisp 8-bit runner silhouette with flowing scarf & visor cutout -->
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M46,28h16v3h3v8h-10v3h7v3h-14v-3h-2v-11h0z" />
    <!-- Flowing scarf & body -->
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M27,46h36v5h-6v12h3v9h4v6h-12v-6h2v-6h-6v6h2v6h-12v-6h4v-9h3v-9h-9v-3h-9z" />
    <!-- Pixel coin accent -->
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M73,39h8v12h-8z" />
</vector>
`;

const adaptiveIconXml = `<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_pixel_stride_bg" />
    <foreground android:drawable="@drawable/ic_pixel_stride_fg" />
    <monochrome android:drawable="@drawable/ic_pixel_stride_monochrome" />
</adaptive-icon>
`;

fs.writeFileSync(path.join(drawableDir, 'ic_pixel_stride_bg.xml'), bgXml);
fs.writeFileSync(path.join(drawableDir, 'ic_pixel_stride_fg.xml'), fgXml);
fs.writeFileSync(path.join(drawableDir, 'ic_pixel_stride_monochrome.xml'), monoXml);
fs.writeFileSync(path.join(mipmapAnyDpiDir, 'ic_launcher.xml'), adaptiveIconXml);
fs.writeFileSync(path.join(mipmapAnyDpiDir, 'ic_launcher_round.xml'), adaptiveIconXml);

console.log('Applied Pixel Stride Adaptive + Material You Monochrome Android icons.');
