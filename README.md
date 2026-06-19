# `react-native-text-reader`

A React Native library for extracting text from images using native iOS (Vision Framework) and Android (ML Kit) capabilities.

## Table of Contents

- [Installation](#installation)
- [Platform Setup](#platform-setup)
  - [iOS](#ios)
  - [Android](#android)
  - [Expo](#expo)
- [Usage](#usage)
  - [Basic Example](#basic-example)
  - [Detailed Result](#detailed-result)
- [Options](#options)
- [ScriptOptions Enum](#scriptoptions-enum)
- [Contributing](#contributing)
- [License](#license)

## Installation

Install the package with your preferred package manager:

```bash
npm install react-native-text-reader
```

```bash
pnpm add react-native-text-reader
```

```bash
yarn add react-native-text-reader
```

The published package is package-manager agnostic. Use whichever tool your app already uses.

### iOS

```bash
cd ios && pod install
```

### Android

No additional manual steps are required. Autolinking handles native setup.

## Expo

This library uses custom native code (Vision + ML Kit), so it does **not** work in **Expo Go**.

It **does** work with Expo development builds and EAS Build (tested with **Expo SDK 56**):

```bash
npx expo install react-native-text-reader expo-dev-client
```

Add the config plugin to your `app.json`:

```json
{
  "expo": {
    "plugins": ["react-native-text-reader"]
  }
}
```

Then create native projects and run a dev build:

```bash
npx expo prebuild
npx expo run:ios
# or
npx expo run:android
```

## Usage

### Basic Example

```typescript
import TextReader, { ScriptOptions } from 'react-native-text-reader';

const imagePath = 'file:///path/to/your/image.jpg';

const lines = await TextReader.read(imagePath, {
  visionIgnoreThreshold: 0.5, // iOS
  confidenceThreshold: 0.5, // Android
  script: ScriptOptions.LATIN, // Android
});

console.log('Extracted lines:', lines);
```

`read()` returns `Promise<string[]>` — one entry per detected line.

### Detailed Result

```typescript
const result = await TextReader.readDetailed(imagePath, {
  script: ScriptOptions.LATIN,
});

console.log(result.fullText);
console.log(result.lines);
console.log(result.details); // confidence, frame, languages
```

## Options

| Property | Type | Platform | Description |
|----------|------|----------|-------------|
| `visionIgnoreThreshold` | `number` | iOS | Confidence threshold (default: `0`) |
| `confidenceThreshold` | `number` | Android | Confidence threshold (default: `0`) |
| `script` | `ScriptOptions` | Android | Script model (default: `LATIN`) |
| `recognitionLevel` | `'fast' \| 'accurate'` | iOS | Speed vs accuracy (default: `accurate`) |
| `recognitionLanguages` | `string[]` | iOS | Language hints, e.g. `['en-US']` |
| `customWords` | `string[]` | iOS | Domain vocabulary hints |
| `useLanguageCorrection` | `boolean` | iOS | Enable language correction |
| `minimumTextHeight` | `number` | iOS | Ignore text smaller than this fraction |

## ScriptOptions

```typescript
export enum ScriptOptions {
  LATIN = 'Latin',
  CHINESE = 'Chinese',
  DEVANAGARI = 'Devanagari',
  JAPANESE = 'Japanese',
  KOREAN = 'Korean',
}
```

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Development in this repository uses **pnpm**.

## License

MIT — see [LICENSE](LICENSE).
