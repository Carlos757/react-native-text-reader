import { NativeModules } from 'react-native';
import TextReader, { ScriptOptions } from '../index';

const { TextReader: NativeTextReader } = NativeModules;

describe('TextReader', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('exports ScriptOptions enum values', () => {
    expect(ScriptOptions.LATIN).toBe('Latin');
    expect(ScriptOptions.CHINESE).toBe('Chinese');
    expect(ScriptOptions.JAPANESE).toBe('Japanese');
  });

  it('read returns lines from readDetailed', async () => {
    NativeTextReader.readDetailed.mockResolvedValueOnce({
      fullText: 'Hello\nWorld',
      lines: ['Hello', 'World'],
      details: [
        { text: 'Hello', confidence: 0.99 },
        { text: 'World', confidence: 0.98 },
      ],
    });

    const lines = await TextReader.read('file:///tmp/test.jpg');

    expect(NativeTextReader.readDetailed).toHaveBeenCalledWith(
      'file:///tmp/test.jpg',
      { script: ScriptOptions.LATIN }
    );
    expect(lines).toEqual(['Hello', 'World']);
  });

  it('readDocument uses document-friendly defaults', async () => {
    NativeTextReader.readDetailed.mockResolvedValueOnce({
      fullText: 'CURP',
      lines: ['CURP'],
      details: [{ text: 'CURP' }],
    });

    await TextReader.readDocument('file:///tmp/ine.jpg');

    // La corrección lingüística apagada es lo importante: empuja códigos como
    // una CURP o una MRZ hacia palabras del diccionario.
    expect(NativeTextReader.readDetailed).toHaveBeenCalledWith(
      'file:///tmp/ine.jpg',
      {
        script: ScriptOptions.LATIN,
        recognitionLevel: 'accurate',
        useLanguageCorrection: false,
        minimumTextHeight: 0.008,
        includeWords: true,
      }
    );
  });

  it('readDocument lets the caller override any default', async () => {
    NativeTextReader.readDetailed.mockResolvedValueOnce({
      fullText: '',
      lines: [],
      details: [],
    });

    await TextReader.readDocument('file:///tmp/ine.jpg', {
      useLanguageCorrection: true,
      includeWords: false,
    });

    expect(NativeTextReader.readDetailed).toHaveBeenCalledWith(
      'file:///tmp/ine.jpg',
      expect.objectContaining({
        useLanguageCorrection: true,
        includeWords: false,
      })
    );
  });

  it('keeps box, words and coordinateSpace from the native payload', async () => {
    NativeTextReader.readDetailed.mockResolvedValueOnce({
      fullText: 'VIGENCIA 2031',
      lines: ['VIGENCIA 2031'],
      coordinateSpace: 'normalized-top-left',
      details: [
        {
          text: 'VIGENCIA 2031',
          box: { x: 0.1, y: 0.8, width: 0.3, height: 0.04 },
          words: [
            {
              text: 'VIGENCIA',
              box: { x: 0.1, y: 0.8, width: 0.15, height: 0.04 },
            },
            {
              text: '2031',
              box: { x: 0.28, y: 0.8, width: 0.12, height: 0.04 },
            },
          ],
        },
      ],
    });

    const result = await TextReader.readDetailed('file:///tmp/ine.jpg');

    expect(result.coordinateSpace).toBe('normalized-top-left');
    expect(result.details[0]?.box).toEqual({
      x: 0.1,
      y: 0.8,
      width: 0.3,
      height: 0.04,
    });
    expect(result.details[0]?.words).toHaveLength(2);
  });

  it('leaves confidence undefined when the engine did not report it', async () => {
    NativeTextReader.readDetailed.mockResolvedValueOnce({
      fullText: 'Sin confianza',
      lines: ['Sin confianza'],
      details: [{ text: 'Sin confianza' }],
    });

    const result = await TextReader.readDetailed('file:///tmp/test.jpg');

    // Nunca se rellena con 1: "no hay medida" y "medida máxima" no son lo mismo.
    expect(result.details[0]?.confidence).toBeUndefined();
  });

  it('readDetailed normalizes native response', async () => {
    NativeTextReader.readDetailed.mockResolvedValueOnce({
      fullText: 'Line 1',
      lines: ['Line 1'],
      details: [{ text: 'Line 1', confidence: 0.95 }],
    });

    const result = await TextReader.readDetailed('file:///tmp/test.jpg', {
      script: ScriptOptions.KOREAN,
      confidenceThreshold: 0.5,
    });

    expect(result).toEqual({
      fullText: 'Line 1',
      lines: ['Line 1'],
      details: [{ text: 'Line 1', confidence: 0.95 }],
    });
  });
});
