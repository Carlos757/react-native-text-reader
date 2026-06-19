const { createRunOncePlugin } = require('expo/config-plugins');

/**
 * Ensures react-native-text-reader is configured for Expo development builds.
 * Requires expo-dev-client — does not work in Expo Go.
 */
function withReactNativeTextReader(config) {
  return config;
}

module.exports = createRunOncePlugin(
  withReactNativeTextReader,
  'react-native-text-reader',
  '2.0.0'
);
