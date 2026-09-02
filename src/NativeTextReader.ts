import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export type NativeOptions = {
  visionIgnoreThreshold?: number;
  confidenceThreshold?: number;
  script?: string;
  recognitionLevel?: 'fast' | 'accurate';
  recognitionLanguages?: string[];
  customWords?: string[];
  useLanguageCorrection?: boolean;
  minimumTextHeight?: number;
  includeWords?: boolean;
  regionOfInterest?: NativeBox;
};

export type NativeBox = {
  x: number;
  y: number;
  width: number;
  height: number;
};

export type NativeWord = {
  text: string;
  box?: NativeBox;
  confidence?: number;
};

export type NativeTextLine = {
  text: string;
  confidence?: number;
  /**
   * @deprecated Sus unidades y su origen difieren entre plataformas (iOS:
   * normalizado x1000 con origen abajo; Android: píxeles con origen arriba),
   * así que no es comparable cross-platform. Usa `box`.
   */
  frame?: {
    top: number;
    left: number;
    width: number;
    height: number;
  };
  box?: NativeBox;
  words?: NativeWord[];
  recognizedLanguages?: string[];
};

export type NativeDetailedResult = {
  fullText: string;
  lines: string[];
  details: NativeTextLine[];
  coordinateSpace?: 'normalized-top-left';
};

export interface Spec extends TurboModule {
  read(imagePath: string, options?: NativeOptions): Promise<string[]>;
  readDetailed(
    imagePath: string,
    options?: NativeOptions
  ): Promise<NativeDetailedResult>;
}

export default TurboModuleRegistry.get<Spec>('TextReader');
