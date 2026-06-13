---
openspec: "1.0.0"
type: "tasks"
id: "001"
title: "StumpVision Feature Backlog & Task Status"
status: "approved"
created_at: "2026-06-13"
updated_at: "2026-06-13"
---

# StumpVision: Feature Backlog & Verification Status

This document represents the official engineering roadmap, tasks status list, and testing criteria for StumpVision core features.

---

## 1. Feature Backlog & Implementation Status

### Milestone 1: Data Architecture & Persistence
- [x] Configure SQLite Room database entities (`SessionEntity`, `DeliveryEntity`).
- [x] Implement local DAOs with cascading deletion constraints.
- [x] Integrate Jetpack ViewModels mapping database flow values to mutable states.

### Milestone 2: Camera Viewfinder & Calibration HUD
- [x] Setup CameraX preview stream mapping inside custom Compose components.
- [x] Write calibration slider modifiers updating overlay height and opacity settings.
- [x] Establish raw threshold frequency audio detection capturing edge sounds.

### Milestone 3: 3D DRS HawkEye Projection & Snicko
- [x] Build coordinate flight graphics plotting custom pre/post bounce splines.
- [x] Draft high-fidelity synchronized audio waveform charts for precise edge reviews.
- [x] Render precise 3D wicket, popping crease, and bail vector targets.

### Milestone 4: Hardware-Accelerated Video Compilation
- [x] Create `VideoExportHelper` configuring MediaCodec and MediaMuxer pipelines.
- [x] Standardizeportrait resolution overlays rendering metadata HUDs.
- [x] Automate media scanners index notifications on successful scoped storage exports.

### Milestone 5: Generative Team Strategy Feedback
- [x] Establish secure, lightweight REST payload formatting telemetry details.
- [x] Integrate Master Bowling Analyst criteria bounds inside prompt models.
- [x] Output action results cleanly within the Coach’s notes markdown interface.

---

## 2. Verification & Verification Recipes

To secure build delivery and verify code health across changes, the following steps are performed:

### 1. Verification Compilation
Checks code consistency, dependencies, syntax correctness, and build rules constraints.
- **Command**: `gradle assembleDebug` or the automated verification tool wrapper `compile_applet`.

### 2. Off-Screen Rendering Checks
Ensures on-device video compilation doesn't consume redundant memory buffers.
- **Recipe**: Ensure `VideoExportHelper` is initiated within the proper background coroutine context to keep the UI frame rate steady at 60 fps during compilation on-device.
