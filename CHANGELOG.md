# Changelog

## 2.0.0

### Breaking

- Android `minSdkVersion` raised from 21 to 23 (ML Kit Text Recognition v2 requirement).

### Added

- `readDetailed()` API with `fullText`, `lines`, and per-line metadata (`confidence`, `frame`, `recognizedLanguages`).
- iOS options: `recognitionLevel`, `recognitionLanguages`, `customWords`, `useLanguageCorrection`, `minimumTextHeight`.
- Android option: `confidenceThreshold` for filtering low-confidence lines.
- Expo config plugin (`app.plugin.js`) and Expo SDK 56 development build example app.
- Turbo Module spec (`NativeTextReader.ts`) with legacy bridge fallback.

### Changed

- iOS and Android now return line-level text in consistent reading order.
- iOS applies EXIF orientation correction before OCR.
- iOS OCR runs on a background queue.
- Android HTTP image loading uses timeouts and size limits.
- Android `TextRecognizer` is closed after each request.
- Structured error codes aligned across platforms (`ERR_EMPTY_PATH`, `ERR_IMAGE_LOADING`, `ERR_OCR`, etc.).
- Repository migrated to pnpm for development; consumers can still install with npm, pnpm, or yarn.
- Upgraded dev toolchain: pnpm 11, React Native 0.86, Jest 30, bob 0.43, commitlint 21.

### Fixed

- `requiresMainQueueSetup` mismatch between iOS `.mm` and `.swift`.
- Jest mock used wrong native method name (`readText` → `read` / `readDetailed`).

### Tooling

- Upgraded `release-it` to v20, `turbo` to v2, Node to v20.
- Added CI audit job and enabled unit tests.

## 1.5.0

- Previous stable release.
