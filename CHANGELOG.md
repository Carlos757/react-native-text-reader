# Changelog

## Unreleased

### Added

- `readDocument()`: preset for printed documents (ID cards, forms). Uses `accurate` recognition, **disables language correction** — which otherwise pushes codes like an MRZ or a Mexican CURP toward dictionary words — and returns word-level boxes.
- `box` on every line and word: a normalized rect (`0-1`, **top-left origin**) that means the same on iOS and Android, so callers can reason about layout — columns, reading order, which value sits under which label — without branching per platform.
- `words` on each line (opt in with `includeWords`): every word with its own box and confidence. On iOS these were previously discarded when merging lines, which made same-height columns indistinguishable.
- `regionOfInterest` option: restrict recognition to a normalized area. Native on iOS (`VNImageBasedRequest.regionOfInterest`), implemented as a bitmap crop on Android.
- `coordinateSpace` on the detailed result, so a caller can tell whether the native module is new enough to provide `box`.

### Fixed

- **Android confidence was always reported as `1.0`.** It was read through reflection, and every failure fell back to `1.0f` — the worst possible default, since downstream code reads it as *maximum certainty*. It now calls `Text.Element.confidence` directly and **omits the field when the engine gives no value**: absence of a measurement is not a low measurement.
- `confidenceThreshold` no longer drops lines that carry no confidence value.

### Deprecated

- `frame` on `TextLine`. Its units and origin differ per platform (iOS: normalized ×1000, bottom-left; Android: pixels, top-left), so it was never comparable across platforms. It still ships unchanged — use `box` instead.

### Notes

- Fully backwards compatible: `read()` and `readDetailed()` keep their existing shape, and every new field is additive.
- Minimums unchanged: iOS 13+, Android API 23+.

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
