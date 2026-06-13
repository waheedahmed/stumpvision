---
openspec: "1.0.0"
type: "proposal"
id: "001"
title: "StumpVision Core Product & UX Proposal"
status: "approved"
created_at: "2026-06-13"
updated_at: "2026-06-13"
---

# StumpVision: Core Product & UX Proposal

## 1. Product Philosophy & Value Pillars

Elite cricket tracking systems (like HawkEye) cost upwards of $100,000, leaving amateur, school, and suburban club players with no means of precise analysis. **StumpVision** bridges this gap by leveraging standard on-device smartphone cameras and real-time computation to simulate 3D projection, snicko edge-detection, and tactical coach reviews instantly.

### Core Pillars
1. **Low Cost Simulation**: High precision tracking from regular portrait camera perspectives.
2. **On-Device Confidentiality**: Fully offline analysis with Room database synchronization and on-device H.264 video rendering.
3. **Conversational Feedback**: Translate raw numbers (seam, drop, length) into immediate, easy-to-understand recommendations from an automated Elite Analyst.

---

## 2. Feature & Capability Specification

### Feature A: Session Analytics Coach Dashboard
- Central hub listing all active bowling spells and drills.
- Core metrics are aggregated beautifully using elegant micro-gradients (e.g., economy, dot-ball %, average speed).
- A trigger button activates the localized GenAI spell-coaching mechanism to provide real-time strategic assistance.

### Feature B: CameraX HUD & Gridline Calibration
- A real-time camera viewfinder leveraging Android CameraX capabilities.
- Live coaching gridlines overlay with density calibration sliders to configure vertical and horizontal perspective offsets on random pitches.
- Synchronized sound recording thresholds displaying feedback spikes to correlate BAT/BALL contacts.

### Feature C: 3D DRS & Review Canvas
- Ball pitch indicators demonstrating precisely where deliveries land on the surface relative to standard cricket length categories (e.g. Good Length, Full Pitch, Short).
- Immersive Snicko audio wave progression chart corresponding with delivery frames to track slight edges.
- HawkEye predictive simulation drawing pre-bounce flight (cyan) and post-bounce impact forecasts (crimson red) on wickets.

### Feature D: Hardware-Accelerated Analytical Video Compiler
- Automatically burns analytical telemetry, flight trails, concentric tracking targets, and speed stats directly onto a high-performance, compact, on-device MP4 clip.
- Direct quick-share options to quickly export drills straight into standard gallery directories.

### Feature E: Automated Elite Coach
- Integrates specialized custom prompt parameters instructing the model as an **Elite Bowling Analyst and Master Coach**.
- Delivers highly specialized tactical cues keeping text lengths under 120 words to ensure rapid scanning during session nets.

---

## 3. Human Interface & Aesthetic Guidelines

### Material Design 3 Styling
- **Canvas Base**: Slate Deep Navy (`#0F172A`) for optimal outdoor net context viewing.
- **Card Frames**: Graphite underlays (`#1E293B`) separating visual metrics cleanly.
- **Active Accents**: Cyber Cyan (`#38BDF8`) for dynamic paths, glowing indicators, and UI trims.
- **Pitch/Spells Accent**: Warm Cricket Green (`#10B981` / `#065F46`).
- **Telemetry Headings**: Fixed-width typefaces (`JetBrains Mono` / `Typeface.MONOSPACE`) to ensure rapid numeric tracking.

### Interface Constraints
- **Tap Tolerances**: Minimum touch targets are strictly maintained at `48.dp x 48.dp`.
- **Dynamic Adaptability**: UI scale seamlessly across compact mobile devices and wide coaching tablets.
