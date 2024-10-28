import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-text-reader' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const TextReader = NativeModules.TextReader
  ? NativeModules.TextReader
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

export enum ScriptOptions {
  LATIN = 'Latin',
  CHINESE = 'Chinese',
  DEVANAGARI = 'Devanagari',
  JAPANESE = 'Japanese',
  KOREAN = 'Korean',
}

// Options
export type Options = {
  visionIgnoreThreshold?: number; // only iOS
  script?: ScriptOptions; // only Android
};

type TextReaderType = {
  read(imagePath: string, options?: Options): Promise<string[]>;
};

/**
 * Extracts text from an image.
 * @param imagePath - Image path
 * @param options - Additional options
 * @param options.visionIgnoreThreshold - Vision ignore threshold(iOS)
 * @param options.script - Language script (Android)
 */
async function read(imagePath: string, options: Options): Promise<string[]> {
  return await TextReader.read(
    imagePath,
    options || {
      script: ScriptOptions.LATIN,
    }
  );
}

export default { read } as TextReaderType;
