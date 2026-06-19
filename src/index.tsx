import { NativeModules, Platform } from 'react-native';
import NativeTextReaderModule from './NativeTextReader';

const LINKING_ERROR =
  `The package 'react-native-text-reader' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go (use a development build instead)\n';

const TextReaderModule = NativeTextReaderModule ?? NativeModules.TextReader;

const TextReader = TextReaderModule
  ? TextReaderModule
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

export type RecognitionLevel = 'fast' | 'accurate';

export type TextFrame = {
  top: number;
  left: number;
  width: number;
  height: number;
};

export type TextLine = {
  text: string;
  confidence?: number;
  frame?: TextFrame;
  recognizedLanguages?: string[];
};

export type DetailedResult = {
  fullText: string;
  lines: string[];
  details: TextLine[];
};

export type Options = {
  visionIgnoreThreshold?: number;
  confidenceThreshold?: number;
  script?: ScriptOptions;
  recognitionLevel?: RecognitionLevel;
  recognitionLanguages?: string[];
  customWords?: string[];
  useLanguageCorrection?: boolean;
  minimumTextHeight?: number;
};

type TextReaderNative = {
  read(imagePath: string, options?: Options): Promise<string[]>;
  readDetailed(imagePath: string, options?: Options): Promise<DetailedResult>;
};

const DEFAULT_OPTIONS: Options = {
  script: ScriptOptions.LATIN,
};

function normalizeDetailedResult(result: DetailedResult): DetailedResult {
  return {
    fullText: result.fullText ?? '',
    lines: result.lines ?? [],
    details: (result.details ?? []).map((detail) => ({
      text: detail.text,
      confidence: detail.confidence,
      frame: detail.frame,
      recognizedLanguages: detail.recognizedLanguages,
    })),
  };
}

/**
 * Extracts text lines from an image.
 */
async function read(imagePath: string, options?: Options): Promise<string[]> {
  const detailed = await readDetailed(imagePath, options);
  return detailed.lines;
}

/**
 * Extracts text with confidence, bounding boxes, and language metadata.
 */
async function readDetailed(
  imagePath: string,
  options?: Options
): Promise<DetailedResult> {
  const result = await TextReader.readDetailed(
    imagePath,
    options ?? DEFAULT_OPTIONS
  );
  return normalizeDetailedResult(result);
}

export default { read, readDetailed } as TextReaderNative;
