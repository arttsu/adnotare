# Adnotare

<video src="https://github.com/user-attachments/assets/c5d4e83e-631c-4eef-a45f-702f99bee27e"></video>

Adnotare is a desktop tool for improving collaboration with LLMs.

Paste an LLM response (or any text), highlight specific passages,
attach a predefined prompt (with an optional note) to each selection,
and copy annotations as structured text.

Instead of manually describing context, you reference exact spans of
text -- so follow-up prompts are clearer and faster to write.

## Development

### Environment Setup

Install: 

- Java (via [SDKMAN!](https://sdkman.io/))
- [Clojure](https://clojure.org/guides/install_clojure)
- [just](https://github.com/casey/just#installation)

### Development Commands

Run `just` to see all available commands.

## Packaging

### Local macOS build

Requirements:

- JDK 21+ with `jpackage` available
- Clojure CLI

Build a local unsigned app bundle:

```bash
just package-mac
```

Outputs:

- `target/dist/Adnotare.app`
- `target/dist/Adnotare-macos.zip`

Launch:

```bash
open -n target/dist/Adnotare.app
```

If Finder blocks the app, right-click -> Open once.

### Cross-platform dist task (for CI and local testing)

Build an archived app-image for a specific OS:

```bash
just dist mac v0.1.0 arm64
just dist win v0.1.0 x64
just dist linux v0.1.0 x64
```

Archives are written to `target/dist/archives`.

## CI and Releases

Workflow: `.github/workflows/ci.yml`

How it works:

1. Matrix build runs on `macos-latest`, `windows-latest`, `ubuntu-latest`.
2. Each runner invokes `clj -T:build dist` with OS/version/arch args.
3. Dist archives are uploaded as workflow artifacts.
4. On tag pushes matching `v*`, CI also creates a GitHub Release and attaches all archives.

Versioning behavior:

- Tag builds use the tag directly (for example `v0.1.0`).
- PR builds use `dev-<sha8>`.

Expected release asset names:

- `Adnotare-macos-<arch>-vX.Y.Z.zip`
- `Adnotare-windows-x64-vX.Y.Z.zip`
- `Adnotare-linux-x64-vX.Y.Z.tar.gz`

### Creating a release

1. Commit and push changes to `main`.
2. Create and push a version tag:

```bash
git tag v0.1.0
git push origin v0.1.0
```

3. Wait for the CI workflow to finish.
4. Download artifacts from the generated GitHub Release page.

For a step-by-step operational checklist, see `RELEASING.md`.

## License

MIT
