# StumpVision — Elite Cricket Analytics & On-Device DRS

StumpVision is a high-performance, offline-first cricket analytics Android camera application designed to democratize professional television-grade analysis. Developed using modern Jetpack Compose, CameraX, Room Database, and optimized on-device hardware-accelerated video rendering pipelines.

StumpVision bridges the gap between grassroots cricket play and premium coaching by turning phone cameras into high-fidelity tracking hubs.

---

## 🚀 Key Features & High-Level Specifications

### 1. Real-Time Projective Simulation & DRS
- **HawkEye Coordinates Map**: Accurate 3D prediction tracing ball release and bounce offsets relative to target wickets.
- **Visual Trajectory Overlays**: Uses dynamic Bezier coordinate lines inside the HUD (cyan flight trails pre-bounce, crimson deflection vectors post-bounce).
- **Pitch Dispersion Plots**: High-contrast scatter points indicating pitch wear-and-tear areas.

### 2. On-Device Snicko-Meter
- **Acoustic Sync Tracking**: Synchronizes live microphone audio threshold sensors with the capture timeline to find wood-on-leather edge frequencies.
- **Integrated Waveform Charting**: Frame-by-frame scrubbing displays exact millisecond audio pulses beside visual contact points.

### 3. Hardware Accelerated Video Export (H.264/MP4 Engine)
- **Zero Cloud Encoding Overhead**: Compiles highly-detailed coaching summaries completely on-device.
- **Overlay Rendering**: Combines 3D stumps, Pop creases, trajectory trails, speed stats, and DRS verdict graphic layers onto a portrait portrait view (480x640 portrait container capped at 1.2 Mbps).
- **Auto-Scanner Notification**: Automatically indexes files into Scoped Movies Storage (`Movies/StumpVision/`) for frictionless team sharing.

### 4. Interactive Spells & Session Aggergation
- **Advanced Dashboard**: View detailed bowling trends with premium micro-gradients demonstrating dot-ball rates, economic averages, and release speed records.
- **Relational Offline Data**: Uses a fully offline SQLite Room architecture to cascade session records securely.

### 5. AI Master Coach
- **Low-Latency Feedback**: Prompts are optimized to provide tactical, constructive recommendations (focusing on seam movement, crease positioning, and release heights).
- **Word Conservation Limits**: Strategic actions are formatted under 120 words to enable immediate scanning on the pitch.

---

## 🎨 Visual System & UI Experience

StumpVision uses a specialized low-light Material Design 3 theme adjusted perfectly for batting tunnels, outdoor nets, and sunset coaches.

- **Workspace Base**: Slate Navy Deep Canvas (`#0F172A`)
- **Metric Overlays**: Dark Graphite Glassmorphism (`#1E293B`)
- **Accent Lines & HUD Highlights**: Neon Amber (`#FBBF24`) & Cyber Cyan (`#38BDF8`)
- **Tactical Pitch Accents**: Academy Green Turf Glow (`#10B981`)
- **Typography Pairing**: Fixed-width telemetry readouts (`JetBrains Mono`) matched with modern sans-serif fonts for swift scanning.

---

## 📁 Specifications Architecture (OpenSpec)

Following correct architectural separation of concerns, complete technical specifications are documented under the `/specs/` index:

- **[Master Spec Index File](./specs/README.md)**: Main subdirectory map.
- **[Aesthetic & Product Proposals](./specs/proposals/001_core_proposal.md)**: Design concepts, styling rules, typography pairings, and feature definitions.
- **[Technical Architecture & database Schemas](./specs/architecture/001_technical_spec.md)**: Local Room table parameters, offscreen `MediaCodec` codec channels, and GenAI coaching prompts.
- **[Task Status & Verification Checklist](./specs/tasks/001_backlog_status.md)**: Trackable milestones, local compilation commands, and verification recipes.

---

## 🛠️ Compilation & Local Setup

### Prerequisities
- **Minimum SDK**: Android API 24+ (Android 7.0)
- **Target SDK**: Android API 36 (Android 16)
- **Required IDE**: Android Studio Ladybug (or newer version supporting Gradle Version Catalog and Kotlin DSL)

### Local Build Steps
1. Clone the repository:
   ```bash
   git clone <repository_url>
   ```
2. Open the project inside **Android Studio**.
3. Create a raw `.env` file at the root or configure your local environmental path with a valid GEMINI_API_KEY. (Refer to the `.env.example` placeholder).
4. Synchronize Gradle files and build the applet:
   ```bash
   ./gradlew assembleDebug
   ```
5. Install the generated debug APK found inside `app/build/outputs/apk/debug/` onto your device or launch on any standard Android Emulator with camera-capturing capabilities.
