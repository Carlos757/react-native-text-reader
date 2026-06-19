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
