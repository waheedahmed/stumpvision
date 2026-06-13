---
openspec: "1.0.0"
type: "architecture"
id: "001"
title: "StumpVision Database & Video Pipeline Technical Specification"
status: "approved"
created_at: "2026-06-13"
updated_at: "2026-06-13"
---

# StumpVision Tech Spec: Database, Codecs & AI Systems

This document describes the technical architecture, offline relational models, physical video compilation pipes, and prompt constraints implemented for the StumpVision engine.

---

## 1. Local Database Persistence (Room Schema)

StumpVision implements an offline-first, cascading relational model between `sessions` (overall drills) and `deliveries` (singular tracking clips).

```
┌──────────────────┐               ┌────────────────────┐
│  SessionEntity   │ 1           * │   DeliveryEntity   │
│  (sessions)      ├──────────────>│   (deliveries)     │
│                  │               │   (sessionId FK)   │
└──────────────────┘               └────────────────────┘
```

### Table Structure: `sessions`
Defines overall session trends and bowler performance aggregation.

| Field Name | Type | Key / Constraint | Description |
|---|---|---|---|
| `id` | `Int` | Primary Key (Auto-Gen) | Unique drill identify index. |
| `date` | `Long` | None | Epoch millisecond timestamp of drill. |
| `bowlerName` | `String` | None | Name of the active bowler. |
| `sessionName` | `String` | None | Drill/net context classification. |
| `totalDeliveries` | `Int` | None | Count of completed tracked deliveries. |
| `averageSpeed` | `Double` | None | Aggregated mean release velocity in km/h. |
| `economyRate` | `Double` | None | Calculated economy metrics based on targets. |
| `dotBallPercentage` | `Double` | None | Proportion of ideal defensive length bowls. |
| `boundaryPercentage` | `Double` | None | Proportion of overpitched/wide bowls. |
| `coachNotes` | `String?` | Nullable | Persisted markdown strategical insights. |

### Table Structure: `deliveries`
Holds on-pitch coordinates, speed, seam, DRS reviews, and Snicko waveforms per ball.

| Field Name | Type | Key / Constraint | Description |
|---|---|---|---|
| `id` | `Int` | Primary Key (Auto-Gen) | Unique delivery identifier. |
| `sessionId` | `Int` | Foreign Key (Cascade Delete) | Reference to the parent session id. |
| `deliveryNum` | `Int` | None | Sequence delivery index in spell (e.g. 1-6). |
| `speedKmph` | `Double` | None | Release velocity metric. |
| `swingType` | `String` | None | `In-Swing`, `Out-Swing`, or `Straight`. |
| `seamMovement` | `String` | None | `Leg-cutter`, `Off-cutter`, or `None`. |
| `spinRateRpm` | `Int` | None | Revolutions per minute parameter (0-3000). |
| `pitchLocationX` | `Float` | None | Landing relative horizontal point (0.0 to 1.0). |
| `pitchLocationY` | `Float` | None | Landing relative vertical point (0.0 to 1.0). |
| `lineLengthClass` | `String` | None | Qualitative location label (e.g., "Full Toss"). |
| `bounceAngle` | `Double` | None | Pre vs post bounce vector impact degrees. |
| `isDrsReviewed` | `Boolean` | None | State whether HawkEye review was invoked. |
| `drsVerdict` | `String` | None | `OUT`, `NOT_OUT`, `UMPIRES_CALL`, `NOT_REVIEWED`. |
| `drsConfidence` | `Double` | None | Predictive tracking system confidence (0.00-1.00). |
| `isEdgeDetected` | `Boolean` | None | Echoed Snicko wave leather-on-willow verdict. |
| `wasBoundary` | `Boolean` | None | True if wide/poor landing coordinate. |
| `wasDotBall` | `Boolean` | None | True if optimal landing length. |
| `audioWaveform` | `String` | None | Serialized frequency values mapping the Snicko sensor. |

---

## 2. On-Device Video Compilation Engine

To provide hardware-accelerated feedback without remote servers, StumpVision runs `VideoExportHelper` drawing telemetry overlays inside on-device frame buffers.

### MediaCodec pipeline
- **Video Format**: H.264 AVC (Advanced Video Coding).
- **MimeType**: `video/avc`
- **Resolution**: 480x640 portrait perspective (optimized for smartphone views).
- **Framerate**: Strict 30 frames per second.
- **Bitrate Profile**: 1.2 Mbps ensuring lightning-fast on-device encode speeds (< 200ms per delivery) and optimized gallery storage sizes.
- **Color Format**: Surface input configuration pipelines (`MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface`).

### Rendering Pipeline (Offscreen Canvas Drawing)
Frames are sequenced and written via structured canvas contexts drawing:
1. **Target Reticle HUD**: Cybernetic HUD crosshairs tracking exact wicket areas.
2. **3D Pitch Grid**: Coordinate-calibrated stumps, crease lines, and landing indicators.
3. **Pulsing Indicator Rings**: Radial alpha animation pulses expanding outward from the pitch coordinate.
4. **Trajectory Splines**: Flowing bezier lines plotting flight paths (cyan pre-bounce / vibrant red post-bounce).
5. **HUD Metadata Plates**: Clean high-contrast overlay frames displaying the bowler's name, ball number, velocity metrics, and DRS outcome stamp.

---

## 3. automated GenAI prompt architecture

StumpVision provides localized strategic assistance through a lightweight, context-aware prompt architecture interacting via REST.

### Prompt Formulation
```
System Role: You are an Elite Level bowling coach and master analyst.
Context:
- Pitch landing spots, release velocities, seam variances, and swing types.
Goals:
- Point out errors (e.g. over-pitching, leaking boundaries drift).
- Deliver 2 actionable instructions on wrist adjustments, crease alignment, or line.
Limits:
- Under 120 words. Concise, direct, elite coaching vocabulary.
```
