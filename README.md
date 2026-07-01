# ksef4j

An opinionated, Spring-Boot-first Java library for **KSeF 2.0** — Poland's National e-Invoice System (Krajowy System e-Faktur).

[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Java 21+](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 4.0+](https://img.shields.io/badge/Spring_Boot-4.0%2B-6db33f.svg)](https://spring.io/projects/spring-boot)

> **🚧 Pre-alpha — work in progress.** Not yet published to Maven Central. The API shown below is illustrative of the intended ergonomics and may change before the first release. Track [v0.1 milestone](#roadmap) for status.

---

## What is ksef4j?

`ksef4j` is a higher-level Java client for the KSeF 2.0 REST API. It targets the common case — issuing structured invoices in FA(3) format — with sensible defaults, a fluent builder API, and first-class Spring Boot integration.

It builds on top of the patterns established by the official Ministry of Finance SDK and aims to remove the boilerplate that comes with low-level DTO-driven clients.

## Why ksef4j?

- **Zero-friction installation.** Published to Maven Central — no GitHub Packages PAT, no custom repository configuration.
- **Spring Boot starter included.** Autoconfiguration, sensible defaults, properties-driven setup. Drop in the dependency, set a token, send invoices.
- **Framework-agnostic core.** `ksef4j-core` has no Spring dependency. Use it from plain Java, Quarkus, Micronaut, or anywhere else.
- **Fluent, high-level API.** Build invoices from YAML templates or programmatically. Send with one method call. UPO retrieval handled.
- **Modern stack.** Java 21, Spring Boot 4, JDK `HttpClient`, no transitive bloat.

## Requirements

- **Java 21** or later
- **Spring Boot 4.0+** (only for `ksef4j-spring-boot-starter`; the core module is framework-agnostic)
- A KSeF token, generated in [MCU](https://ksef.podatki.gov.pl) after authenticating with Trusted Profile (Profil Zaufany)

## Installation

> Not yet published. The coordinates below reflect the planned first release (1.0.0); see the [roadmap](#roadmap) for what 1.0 includes.

### Maven

```xml
<dependency>
    <groupId>io.github.mlkmn</groupId>
    <artifactId>ksef4j-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

For non-Spring projects, depend on the core module directly:

```xml
<dependency>
    <groupId>io.github.mlkmn</groupId>
    <artifactId>ksef4j-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

```kotlin
implementation("io.github.mlkmn:ksef4j-spring-boot-starter:1.0.0")
```

## Quick start

### Spring Boot

Add the starter dependency, then configure in `application.yml`:

```yaml
ksef:
  environment: prod        # test | demo | prod
  # base-url: https://ksef-gateway.internal/v2   # advanced: proxy/gateway fronting the environment
  auth:
    token: ${KSEF_TOKEN}
  context:
    nip: ${COMPANY_NIP}
```

Inject `KsefClient` and send:

```java
@Service
public class InvoicingService {

    private final KsefClient ksef;

    public InvoicingService(KsefClient ksef) {
        this.ksef = ksef;
    }

    public void sendMonthlyInvoice(Path template) {
        var invoice = Invoice.fromYaml(template);
        var result  = ksef.send(invoice).awaitUpo();
        // result.upoXml(), result.ksefReferenceNumber(), ...
    }
}
```

### Standalone (no Spring)

```java
var client = KsefClient.builder()
    .environment(Environment.PROD)
    // .baseUrl(URI.create("https://ksef-gateway.internal/v2"))  // advanced: proxy/gateway
    .tokenAuth(System.getenv("KSEF_TOKEN"), nip)
    .upoPollTimeout(Duration.ofSeconds(30))
    .build();

var invoice = Invoice.fromYaml(Path.of("invoice.yaml"));
var result  = client.send(invoice).awaitUpo();
```

### Reading invoices

Query invoice metadata for your NIP, as seller or as buyer, over a required date range:

```java
import io.github.mlkmn.ksef4j.query.InvoiceQuery;
import io.github.mlkmn.ksef4j.query.InvoiceMetadata;
import io.github.mlkmn.ksef4j.query.InvoiceMetadataPage;

import java.time.LocalDate;

// One page (offset-based, full control):
InvoiceQuery query = InvoiceQuery.asSeller()
        .issuedBetween(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))
        .counterpartyNip("1234567890")   // optional
        .pageSize(100)
        .build();

InvoiceMetadataPage page = ksef.queryInvoices(query);
for (InvoiceMetadata invoice : page.invoices()) {
    System.out.println(invoice.ksefNumber() + " " + invoice.grossAmount());
}

// Or stream every match, paging lazily under the hood:
ksef.streamInvoices(InvoiceQuery.asBuyer()
                .receivedBetween(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))
                .build())
        .forEach(invoice -> System.out.println(invoice.invoiceNumber()));
```

Metadata is the invoice header (parties, amounts, dates, KSeF number) - not the full FA(3) body. Downloading invoice content is on the roadmap (v0.4). A single query returns at most 10000 records over a date range of at most 3 calendar months; to retrieve more, narrow the date range.

### UPO result

`awaitUpo()` blocks until KSeF issues the UPO and returns a `Upo` record:

| Field | Type | Description |
|---|---|---|
| `ksefReferenceNumber` | `String` | KSeF-assigned reference for the accepted invoice |
| `upoReferenceNumber` | `String` | KSeF-assigned reference for the UPO document itself |
| `issuedAt` | `Instant` | Instant the UPO was issued (UTC) |
| `documentHash` | `String` | Base64 SHA-256 of the sent FA(3) document, sourced from the UPO's `SkrotDokumentu`; `null` if the UPO omits it |
| `invoiceNumber` | `String` | Invoice number the UPO confirms, sourced from `NumerFaktury`; `null` if absent |
| `xml` | `byte[]` | Raw UPO XML returned by KSeF |

Before returning the `Upo`, `awaitUpo()` verifies the UPO's `documentHash` against the hash of the invoice that was sent. If the hashes do not match, the receipt does not correspond to the submitted document and `UpoVerificationException` (a `KsefException` subtype) is thrown. When the UPO carries no hash (`documentHash` is `null`) the check is skipped.

#### UPO signature verification (opt-in)

ksef4j can also verify the Ministry of Finance's XML-DSig signature on the UPO, confirming the document was genuinely signed by KSeF and has not been tampered with. Trust is established through a pinned Ministry signing certificate bundled in the library (the TEST certificate is bundled today; DEMO and PROD certificates will be added once captured from those environments).

To enable automatic verification during `awaitUpo()`, set the opt-in flag on the builder (default is off):

```java
KsefClient client = KsefClient.builder()
    .environment(Environment.TEST)
    .tokenAuth(token, nip)
    .verifyUpoSignature(true)   // verify Ministry XML-DSig on every UPO
    .build();
```

You can also verify a UPO independently with `UpoSignatureVerifier`:

```java
// verify returns void; throws UpoVerificationException on a bad or untrusted signature
new UpoSignatureVerifier().verify(upo.xml(), Environment.TEST);
```

`UpoVerificationException` (a `KsefException` subtype) is thrown when the signature is invalid, the document is malformed, the pinned certificate is outside its validity window, or no signing certificate is bundled for the requested environment.

### Invoice template (YAML)

PLN invoice (simplest case):

```yaml
invoiceNumber: "2026/03/001"
issueDate: "2026-03-31"
seller:
  nip: "5260250274"          # quote NIPs — they parse as numbers otherwise
  name: "Example Sp. z o.o."
buyer:
  nip: "1234567890"
  name: "Customer Sp. z o.o."
items:
  - description: "Consulting services, March 2026"
    quantity: 1
    unitPrice: 10000.00
    vatRate: 23
currency: PLN
```

EUR invoice with exchange rate, custom unit, and PKWiU code:

```yaml
invoiceNumber: "2026/03/002"
issueDate: "2026-03-31"
seller:
  nip: "5260250274"
  name: "Example Sp. z o.o."
buyer:
  nip: "1234567890"
  name: "Foreign Customer GmbH"
items:
  - description: "Software development, March 2026"
    quantity: 160
    unitPrice: 85.00
    vatRate: 23
    unit: godzina
    pkwiu: "62.01.11.0"
currency: EUR
exchangeRate: 4.2489
```

The `exchangeRate` field (FA(3) `KursWalutyZ`) is required for non-PLN invoices and must be absent for PLN. The library computes the per-VAT-band VAT-in-PLN amount (`P_14_xW`) from it automatically. The rate is caller-supplied; automatic NBP rate resolution is planned for a later release.

`unit` is the unit of measure (FA(3) `P_8A`); it defaults to `szt.` when omitted. `pkwiu` is an optional per-line PKWiU classification code (FA(3) `PKWiU`).

Supported invoice fields: `invoiceNumber`, `issueDate`, `saleDate`, `currency`, `exchangeRate`, `seller`, `buyer`, `items`. Supported item fields: `description`, `quantity`, `unitPrice`, `vatRate`, `unit`, `pkwiu`.

### Running the live smoke test (optional)

A live happy-path check against the KSeF `test` environment lives in `ksef4j-core` as an opt-in test. It is excluded from the normal build. To run it, export a KSeF test-environment token and the NIP it is scoped to, then invoke the dedicated task:

    export KSEF_TOKEN=...        # a KSeF test-environment token
    export COMPANY_NIP=...       # the NIP the token is scoped to (becomes the invoice issuer)
    ./gradlew :ksef4j-core:smokeTest --no-daemon

On Windows PowerShell:

    $env:KSEF_TOKEN="..."; $env:COMPANY_NIP="..."; .\gradlew.bat :ksef4j-core:smokeTest --no-daemon

It sends one invoice and waits for the UPO, printing the KSeF reference number, timings, and contract confirmations. Without the env vars the task self-skips. For raw HTTP wire detail, append `-Djdk.httpclient.HttpClient.log=requests,headers`. The test submits a real invoice to the government test environment; it never touches demo or production.

## Configuration

Full configuration reference for the Spring Boot starter:

| Property                          | Default | Description                                  |
|-----------------------------------|---------|----------------------------------------------|
| `ksef.environment`                | `test`  | One of `test`, `demo`, `prod`                |
| `ksef.auth.token`                 | —       | KSeF token generated in MCU                  |
| `ksef.context.nip`                | —       | Taxpayer NIP used as the API context         |
| `ksef.archive.directory`          | —       | Directory where sent invoices and UPOs are persisted |
| `ksef.upo.poll-timeout`           | `180s`  | How long to wait for UPO after sending       |

## Features

### v0.1 (in development)

- KSeF token authentication (challenge → encrypt → access token)
- Invoice loading from YAML
- Local FA(3) validation against XSD
- Single-invoice send via interactive session
- UPO polling with configurable timeout
- UPO Ministry signature verification (opt-in, `verifyUpoSignature(true)`; TEST cert bundled)
- Local archive of sent invoices and UPOs
- Spring Boot autoconfiguration
- Environment switching (test, demo, prod)

### Planned

- Type-safe FA(3) builder with XSD-driven compile-time validation
- Embedded mock server and test fixtures (`@KsefMockTest`)
- Resilience patterns (retry, idempotency keys, backoff)
- CLI: `ksef send`, `ksef validate`, `ksef list-invoices`
- Offline mode with [Latarnia](https://api-latarnia.ksef.mf.gov.pl) integration
- Observability (Micrometer metrics, OpenTelemetry tracing)
- Format converters (XLSX → FA(3), CSV → FA(3), FA(2) → FA(3))
- XAdES authentication
- Invoice visualization (FA(3) XML → PDF/HTML)
- Batch session API

## Roadmap

| Version | Focus                                              |
|---------|----------------------------------------------------|
| v0.1    | Send: single invoice, UPO handling, Spring Boot starter (current) |
| v0.2    | Read: invoice metadata query                       |
| v0.3    | Test support: mock KSeF server                     |
| v0.4    | Read: invoice download                             |
| v0.5    | Type-safe invoice builder                          |
| 1.0     | Stable send + read + test + builder; published to Maven Central |
| later   | CLI, converters; offline/batch and export/admin as demand warrants |

## Project structure

```
ksef4j/
├── ksef4j-core/                    # Framework-agnostic client (send + read)
├── ksef4j-spring-boot-starter/     # Spring Boot autoconfiguration
├── ksef4j-test/                    # Mock KSeF server / test harness (planned, v0.3)
├── ksef4j-cli/                     # Command-line tool (planned)
└── ksef4j-converters/              # FA(3) -> human-readable view (planned)
```

## Documentation

Full documentation will be published at the project site once v0.1 is released.

For background on KSeF 2.0 itself, see:

- [Official KSeF developer portal](https://ksef.podatki.gov.pl/ksef-na-okres-obligatoryjny/wsparcie-dla-integratorow/)
- [KSeF technical documentation](https://github.com/CIRFMF/ksef-docs)
- [FA(3) logical structure](https://ksef.podatki.gov.pl/ksef-na-okres-obligatoryjny/struktura-logiczna-fa-3)

## Contributing

Contributions, bug reports, and feature requests are welcome. Please open an issue first to discuss substantial changes.

## License

Licensed under the [Apache License, Version 2.0](LICENSE).
