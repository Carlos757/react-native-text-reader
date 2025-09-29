module.exports = {
  presets: [
    ['module:react-native-builder-bob/babel-preset', { modules: 'commonjs' }],
    ['@babel/preset-env', { targets: { node: 'current' } }],
  ],
  plugins: [
    '@babel/plugin-transform-class-properties',
    '@babel/plugin-proposal-class-properties',
  ],
};
