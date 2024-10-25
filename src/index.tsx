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

export type TextReaderOptions = {
  visionIgnoreThreshold?: number;
};

type TextReaderType = {
  read(imagePath: string, options?: TextReaderOptions): Promise<string[]>;
};

/**
 * Extrae texto de una imagen.
 * @param imagePath - La URI de la imagen de la que se extraerá el texto.
 * @param options - Opciones adicionales.
 * @param options.visionIgnoreThreshold - Umbral de ignoración de la visión.
 * @returns Una promesa que se resuelve con el texto extraído.
 */
async function read(
  imagePath: string,
  options?: TextReaderOptions
): Promise<string[]> {
  return await TextReader.read(imagePath, options || {});
}

export default { read } as TextReaderType;
