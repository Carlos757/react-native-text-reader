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
};

export type NativeTextLine = {
  text: string;
  confidence?: number;
  frame?: {
    top: number;
    left: number;
    width: number;
    height: number;
  };
  recognizedLanguages?: string[];
};

export type NativeDetailedResult = {
  fullText: string;
  lines: string[];
  details: NativeTextLine[];
};

export interface Spec extends TurboModule {
  read(imagePath: string, options?: NativeOptions): Promise<string[]>;
  readDetailed(
    imagePath: string,
    options?: NativeOptions
  ): Promise<NativeDetailedResult>;
}

export default TurboModuleRegistry.get<Spec>('TextReader');
