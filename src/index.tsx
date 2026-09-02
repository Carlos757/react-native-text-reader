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

/**
 * @deprecated Sus unidades y su origen difieren entre plataformas, así que no
 * es comparable cross-platform. Usa `TextLine.box`.
 */
export type TextFrame = {
  top: number;
  left: number;
  width: number;
  height: number;
};

export type TextBox = {
  x: number;
  y: number;
  width: number;
  height: number;
};

export type TextWord = {
  text: string;
  box?: TextBox;
  confidence?: number;
};

export type TextLine = {
  text: string;
  /**
   * Confianza del motor, 0-1, cuando la expone. `undefined` significa que no
   * hay dato — nunca se rellena con un valor inventado.
   */
  confidence?: number;
  /** @deprecated Usa `box`. */
  frame?: TextFrame;
  box?: TextBox;
  /** Palabras de la línea; solo se llena si se pidió `includeWords`. */
  words?: TextWord[];
  recognizedLanguages?: string[];
};

export type DetailedResult = {
  fullText: string;
  lines: string[];
  details: TextLine[];
  /**
   * Sistema de coordenadas de las cajas. Ausente en módulos nativos anteriores
   * a la 2.1, donde `box` tampoco existe.
   */
  coordinateSpace?: 'normalized-top-left';
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
  /** Devolver cada palabra con su caja, además de la línea completa. */
  includeWords?: boolean;
  /** Zona a leer, normalizada (0-1) con origen arriba-izquierda. */
  regionOfInterest?: TextBox;
};

type TextReaderNative = {
  read(imagePath: string, options?: Options): Promise<string[]>;
  readDetailed(imagePath: string, options?: Options): Promise<DetailedResult>;
  readDocument(imagePath: string, options?: Options): Promise<DetailedResult>;
};

const DEFAULT_OPTIONS: Options = {
  script: ScriptOptions.LATIN,
};

function normalizeDetailedResult(result: DetailedResult): DetailedResult {
  return {
    fullText: result.fullText ?? '',
    lines: result.lines ?? [],
    coordinateSpace: result.coordinateSpace,
    details: (result.details ?? []).map((detail) => ({
      text: detail.text,
      confidence: detail.confidence,
      frame: detail.frame,
      box: detail.box,
      words: detail.words,
      recognizedLanguages: detail.recognizedLanguages,
    })),
  };
}

const DOCUMENT_OPTIONS: Options = {
  script: ScriptOptions.LATIN,
  recognitionLevel: 'accurate',
  useLanguageCorrection: false,
  minimumTextHeight: 0.008,
  includeWords: true,
};

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

async function readDocument(
  imagePath: string,
  options?: Options
): Promise<DetailedResult> {
  return readDetailed(imagePath, { ...DOCUMENT_OPTIONS, ...options });
}

export default { read, readDetailed, readDocument } as TextReaderNative;
