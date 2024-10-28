# `react-native-text-reader`

A simple React Native library for extracting text from images using native iOS (Vision Framework) and Android (ML Kit) capabilities.


## Table of Contents

- [Installation](#installation)
- [Platform Setup](#platform-setup)
  - [iOS](#ios)
  - [Android](#android)
- [Usage](#usage)
  - [Basic Example](#basic-example)
- [Options](#options)
- [ScriptOptions Enum](#scriptoptions-enum)
- [Contributing](#contributing)
- [License](#license)

### NOTE:
 I currently do not work extensively with React Native, so this project may not receive frequent updates. However, if anyone is interested in contributing, please feel free to reach out!

## Installation

Install the package using npm or yarn:

```bash
npm install react-native-text-reader
```

or

```bash
yarn add react-native-text-reader
```

After installing the package, make sure to link the native dependencies:

### iOS

If you're using iOS, run the following command to install the necessary native modules:

```bash
cd ios/ && pod install
```

### Android

No additional steps are required for Android.

---

## Usage

To use the text reader, import it and call the `read` method with the image path and options.

### Basic Example

```javascript
import TextReader, { ScriptOptions } from 'react-native-text-reader';

const imagePath = 'path/to/your/image.jpg';
const options = {
  visionIgnoreThreshold: 0.5, // iOS only
  script: ScriptOptions.LATIN, // Android only
};

const readTextFromImage = async () => {
  try {
    const text = await TextReader.read(imagePath, options);
    console.log('Extracted text:', text);
  } catch (error) {
    console.error('Error reading text:', error);
  }
};

readTextFromImage();

```

---

## Options

### `Options`

| Property                  | Type              | Description                                          |
|--------------------------|-------------------|------------------------------------------------------|
| `visionIgnoreThreshold`  | `number`          | The confidence threshold for iOS (default: 0.0)     |
| `script`                 | `ScriptOptions`   | The language script for Android (default: `LATIN`)  |

---

## ScriptOptions

The `ScriptOptions` enum allows you to specify different language scripts for Android:

```javascript
export enum ScriptOptions {
  LATIN = 'Latin',
  CHINESE = 'Chinese',
  DEVANAGARI = 'Devanagari',
  JAPANESE = 'Japanese',
  KOREAN = 'Korean',
}
```

---

## Contributing

Contributions are welcome! Please submit a pull request or open an issue for any enhancements or bugs.

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
