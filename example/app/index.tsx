import { useState } from 'react';
import {
  ActivityIndicator,
  Button,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import * as ImagePicker from 'expo-image-picker';
import TextReader, { ScriptOptions } from 'react-native-text-reader';

export default function HomeScreen() {
  const [lines, setLines] = useState<string[]>([]);
  const [fullText, setFullText] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const pickAndRead = async () => {
    setError(null);
    setLoading(true);

    try {
      const permission =
        await ImagePicker.requestMediaLibraryPermissionsAsync();
      if (!permission.granted) {
        throw new Error('Media library permission is required.');
      }

      const result = await ImagePicker.launchImageLibraryAsync({
        mediaTypes: ['images'],
        quality: 1,
      });

      if (result.canceled || !result.assets[0]?.uri) {
        return;
      }

      const detailed = await TextReader.readDetailed(result.assets[0].uri, {
        script: ScriptOptions.LATIN,
        confidenceThreshold: 0,
        visionIgnoreThreshold: 0,
      });

      setLines(detailed.lines);
      setFullText(detailed.fullText);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to read image.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>react-native-text-reader</Text>
      <Text style={styles.subtitle}>Expo development build example</Text>

      <Button title="Pick image and read text" onPress={pickAndRead} />

      {loading ? <ActivityIndicator style={styles.loader} /> : null}
      {error ? <Text style={styles.error}>{error}</Text> : null}

      <ScrollView style={styles.results}>
        {fullText ? <Text style={styles.fullText}>{fullText}</Text> : null}
        {lines.map((line, index) => (
          <Text key={`${index}-${line}`} style={styles.line}>
            {line}
          </Text>
        ))}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 24,
    paddingTop: 72,
    gap: 12,
  },
  title: {
    fontSize: 22,
    fontWeight: '700',
  },
  subtitle: {
    color: '#666',
    marginBottom: 8,
  },
  loader: {
    marginTop: 12,
  },
  error: {
    color: '#b00020',
  },
  results: {
    marginTop: 12,
  },
  fullText: {
    fontWeight: '600',
    marginBottom: 8,
  },
  line: {
    marginBottom: 4,
  },
});
