<!-- 
SYNC IMPACT REPORT
Version change: 0.0.0 -> 1.0.0
Modified principles: None (Initial creation)
Added sections: All (Initial creation)
Removed sections: None
Templates requiring updates: ⚠ pending (no templates exist yet)
Follow-up TODOs: TODO(RATIFICATION_DATE): Needs confirmation of exact project start date
-->

# Graduation Project Constitution

**Version:** 1.0.0
**Ratification Date:** TODO(RATIFICATION_DATE): exact date unknown
**Last Amended Date:** 2026-03-28

## 1. Architecture Principle: SpringBoot + Netty Separation
**Rule:** The project MUST separate RESTful business APIs (SpringBoot in `IM-Bootstrap`) from real-time communication handling (Netty in `Graduation-Netty`), sharing common entities in `Graduation-Common`.
**Rationale:** Ensures stateless HTTP scaling remains independent from stateful WebSocket/TCP connection management.

## 2. Communication Principle: WebSocket & JSON Message
**Rule:** Client-Server real-time communication MUST use standard WebSocket and pass JSON objects mapping to the `CompleteMessage` class.
**Rationale:** Using standard WebSockets enables any frontend (JS/TS) to easily connect without proprietary SDKs, and a unified `CompleteMessage` format simplifies message routing (via `appId` and `messageType`).

## 3. Connection Lifecycle Principle
**Rule:** Frontend MUST send an explicit establish-connection message (`appId:0`, `messageType:0`) immediately after WebSocket `onopen` to bind `userId` to the `Channel`. Heartbeats (`messageType:2`) MUST be sent periodically.
**Rationale:** Netty needs an application-level handshake to identify which user owns the connection, enabling correct message pushing and offline detection.

## 4. Documentation Alignment Principle
**Rule:** API documentation (e.g. `NETTY_API_DOCS.md`) and actual handler implementations (e.g. `LinkProcessor.java`) MUST remain strictly synchronized.
**Rationale:** Inconsistencies (such as heartbeat `messageType` being 3 in docs but 2 in code) cause integration failures. The code is the source of truth, but docs must be updated immediately upon changes.

## Governance & Rules
**Amendment Procedure:** Changes to this constitution require a PR that updates this document and increments the version according to Semantic Versioning.
**Versioning Policy:** MAJOR for architecture shifts; MINOR for new principles; PATCH for clarifications.
**Compliance Review:** All new PRs MUST be checked against these principles during code review.

### Fixed Rules for AI-Assisted Development
- 必须优先修改现有文件，不要随意创建新文件。
- 所有的修改必须经过完整测试，如果存在测试用例则必须运行。
- 在前端与后端交互的设计中，遇到设计分歧优先以实际的后端代码（如`LinkProcessor.java`）为准，同时修复文档。