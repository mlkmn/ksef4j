# Changelog

All notable changes to this project are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.0] - 2026-07-01

First public release. Feature-complete client for KSeF 2.0 across send and read, with first-class test support and a type-safe builder.

### Added
- Send: single-invoice submission over an interactive session (token auth, RSA-OAEP session-key wrap, AES-256 invoice encryption, UPO polling).
- UPO handling: integrity check against the sent document hash, plus opt-in Ministry XML-DSig signature verification (`verifyUpoSignature(true)`); TEST, DEMO, and PROD signing certificates bundled.
- Read: invoice metadata query (seller/buyer, date-ranged, offset paging and lazy streaming) and single-invoice download by KSeF number.
- Type-safe fluent `Invoice.builder()` with validate-on-build.
- `ksef4j-test`: WireMock-based mock KSeF server with a scenario DSL (`onSend`/`onQuery`/`onAuth`/`onUpo`) for downstream integration tests.
- Spring Boot starter: autoconfiguration and properties-driven setup (`ksef.*`).
- Local filesystem archive of sent invoices and UPOs.
- Multi-environment support (test, demo, prod) with a documented `baseUrl` override.

[Unreleased]: https://github.com/mlkmn/ksef4j/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/mlkmn/ksef4j/releases/tag/v1.0.0
