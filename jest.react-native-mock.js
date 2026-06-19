/* eslint-env jest */
module.exports = {
  NativeModules: {
    TextReader: {
      read: jest.fn(),
      readDetailed: jest.fn(),
    },
  },
  Platform: {
    OS: 'ios',
    select: (obj) => obj.ios ?? obj.default,
  },
  TurboModuleRegistry: {
    get: jest.fn(() => null),
  },
};
