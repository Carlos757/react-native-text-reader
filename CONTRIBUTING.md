# Contributing

Contributions are welcome.

## Development workflow

This project is a monorepo managed with [pnpm workspaces](https://pnpm.io/workspaces).

Packages:

- Library package in the repository root
- Expo example app in `example/`

### Setup

```sh
corepack enable
corepack use pnpm@11.8.0
nvm use   # Node.js 22 (see .nvmrc)
pnpm install
```

> Development in this repository requires pnpm. Consumers can still install the published package with npm, pnpm, or yarn.

### Example app

The example uses **Expo SDK 56** (React Native 0.85) with **expo-dev-client** (not Expo Go). Requires **Node.js 22+**.

Generate native projects once (or after native/plugin changes):

```sh
pnpm example:prebuild
```

Run on a device or simulator:

```sh
pnpm example:ios
pnpm example:android
```

Start Metro only:

```sh
pnpm --filter react-native-text-reader-example start
```

### Quality checks

```sh
pnpm typecheck
pnpm lint
pnpm test
pnpm audit
```

### Publishing

```sh
pnpm release
```

We use [release-it](https://github.com/release-it/release-it) with conventional changelog.

### Commit convention

We follow [Conventional Commits](https://www.conventionalcommits.org/en).

## Sending a pull request

- Keep pull requests focused.
- Ensure lint, typecheck, and tests pass.
- Update README/CHANGELOG when the public API changes.
