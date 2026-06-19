const { getDefaultConfig } = require('expo/metro-config');

// SDK 56+ auto-configures watchFolders and nodeModulesPaths for pnpm workspaces.
module.exports = getDefaultConfig(__dirname);
