/* eslint-env jest */
// Mock react-native modules
jest.mock('react-native', () => {
  const RN = jest.requireActual('react-native');
  return {
    ...RN,
    NativeModules: {
      ...RN.NativeModules,
      TextReader: {
        readText: jest.fn(),
      },
    },
  };
});
