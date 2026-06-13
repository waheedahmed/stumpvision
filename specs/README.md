# OpenSpec: StumpVision Specifications Index

Welcome to the official **OpenSpec** repository for StumpVision. To ensure maximum maintainability, clarity, and structural separation of concerns, the specifications have been divided into specialized modules.

---

## Directory Structure Map

```
/specs/
├── README.md                   # This master index / directory map
├── proposals/
│   └── 001_core_proposal.md    # Product goals, user experience, and visual system specification
├── architecture/
│   └── 001_technical_spec.md  # Room database schemas, video pipeline, and API parameters
└── tasks/
    └── 001_backlog_status.md   # Feature roadmap, checklist, and verification protocols
```

---

## 1. Feature proposals & User Experience
*Located at:* [proposals/001_core_proposal.md](./proposals/001_core_proposal.md)
Detailed specification of the core vision, feature scope (A through E), color systems, spacing specs, and interactive behaviors designed to democratize professional cricket coaching analytics.

## 2. Technical Architecture & Database Design
*Located at:* [architecture/001_technical_spec.md](./architecture/001_technical_spec.md)
The under-the-hood structural guide. Details the complete schemas for `sessions` and `deliveries` tables, foreign keys, constraints, the on-device H.264 offline `MediaCodec` compiling pipeline, and direct-REST Gemini Prompt layout.

## 3. Implementation Tasks & Backlog
*Located at:* [tasks/001_backlog_status.md](./tasks/001_backlog_status.md)
Trackable tasks backlog for our engineering phases. Includes state management plans, UI screen mappings, unit test criteria, and verification recipes.
