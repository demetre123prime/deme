# Deme FPS + Keys HUD (Minecraft 1.21.11)

A lightweight, client-side Fabric HUD.

## On-screen display

- Live FPS counter at the bottom-left.
- WASD key visualizer at the bottom-left.
- SPACE and SHIFT indicators.
- Every key has its own accent color; a key lights up when pressed.
- No telemetry, account access, network calls, or automated gameplay.

## Build

The GitHub Actions workflow builds the mod on pushes and pull requests. Open the repository's **Actions** tab and download the `deme-fps-keys-hud` artifact from a successful run. The artifact contains the compiled mod JAR and sources JAR.

## Install

1. Use Minecraft Java Edition **1.21.11** with **Fabric Loader** and **Fabric API 0.141.6+1.21.11**.
2. Download the compiled mod JAR from the Actions artifact (not the sources JAR).
3. Put the mod JAR in your Minecraft `mods` folder.
4. Launch your Fabric 1.21.11 profile.

The HUD is client-side only.
