# ksef4j

An opinionated, Spring-Boot-first Java library for **KSeF 2.0** — Poland's National e-Invoice System (Krajowy System e-Faktur).

[![Maven Central](https://img.shields.io/maven-central/v/io.github.mlkmn/ksef4j-core?label=Maven%20Central)](https://central.sonatype.com/namespace/io.github.mlkmn)
[![Build](https://github.com/mlkmn/ksef4j/actions/workflows/build.yml/badge.svg)](https://github.com/mlkmn/ksef4j/actions/workflows/build.yml)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Java 21+](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 4.0+](https://img.shields.io/badge/Spring_Boot-4.0%2B-6db33f.svg)](https://spring.io/projects/spring-boot)

> **Available on Maven Central.** Add the dependency (see [Installation](#installation)) and send invoices. Apache-2.0.

---

## What is ksef4j?

`ksef4j` is a higher-level Java client for the KSeF 2.0 REST API. It targets the common case — issuing structured invoices in FA(3) format — with sensible defaults, a fluent builder API, and first-class Spring Boot integration.

It builds on top of the patterns established by the official Ministry of Finance SDK and aims to remove the boilerplate that comes with low-level DTO-driven clients.

## Why ksef4j?

- **Zero-friction install.** One Maven Central coordinate - no GitHub Packages PAT, no custom repository.
- **Spring Boot starter included.** Autoconfiguration, sensible defaults, properties-driven setup. Drop in the dependency, set a token, send invoices.
- **Framework-agnostic core.** `ksef4j-core` has no Spring dependency. Use it from plain Java, Quarkus, Micronaut, or anywhere else.
- **Fluent, high-level API.** Build invoices from YAML templates or programmatically. Send with one method call. UPO retrieval handled.
- **Modern stack.** Java 21, Spring Boot 4, JDK `HttpClient`, no transitive bloat.

## Requirements

- **Java 21** or later
- **Spring Boot 4.0+** (only for `ksef4j-spring-boot-starter`; the core module is framework-agnostic)
- A KSeF token, generated in [MCU](https://ksef.podatki.gov.pl) after authenticating with Trusted Profile (Profil Zaufany)

## Installation

> Available on Maven Central as of 1.0.0 - add the dependency below, no extra repository configuration needed.

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

Build the invoice with the fluent builder (recommended for Java construction; `build()` validates and throws `InvoiceValidationException` if the invoice is incomplete or invalid) or load it from YAML with `Invoice.fromYaml(...)`, which remains the recommended path for config-driven use:

```java
Invoice invoice =
    Invoice.builder()
        .invoiceNumber("FV/2026/1")
        .issueDate(LocalDate.of(2026, 6, 30))
        .currency("EUR")
        .exchangeRate("4.30")
        .seller(s -> s.nip("5260250274").name("Seller Sp. z o.o.")
            .address(a -> a.countryCode("PL").line1("ul. Glowna 1")))
        .buyer(b -> b.nip("1234567890").name("Buyer Sp. z o.o.")
            .address(a -> a.countryCode("PL").line1("ul. Boczna 2")))
        .addItem(i -> i.description("Consulting").quantity("10").unitPrice("100.00").vat(23))
        .build(); // throws InvoiceValidationException if incomplete or invalid
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
        // result.xml(), result.ksefNumber(), ...
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

### Testing your integration (`ksef4j-test`)

Add the mock module in test scope and point the client at it - no live KSeF needed:

```kotlin
testImplementation("io.github.mlkmn:ksef4j-test:1.0.0")
```

```java
import io.github.mlkmn.ksef4j.test.MockKsefExtension;

@RegisterExtension
static final MockKsefExtension ksef = MockKsefExtension.create();   // full happy path pre-wired

@Test
void sends_an_invoice() {
    KsefClient client = KsefClient.builder()
            .environment(Environment.TEST)
            .baseUrl(ksef.baseUrl())
            .tokenAuth("any-token", "1234567890")
            .build();

    Upo upo = client.send(Invoice.fromYaml(template)).awaitUpo();   // works out of the box

    // Script alternate outcomes:
    ksef.onSend().reject(21405, "Validation error");   // -> KsefBusinessException
    ksef.onQuery().returns(invoiceMetadata1, invoiceMetadata2);
}
```

The mock is WireMock-based and pulls WireMock only into your test classpath; `ksef4j-core` itself has no such dependency. UPO signature verification (`verifyUpoSignature(true)`) is not simulated offline.

### UPO result

`awaitUpo()` blocks until KSeF issues the UPO and returns a `Upo` record:

| Field | Type | Description |
|---|---|---|
| `ksefNumber` | `String` | KSeF-assigned reference for the accepted invoice |
| `upoReferenceNumber` | `String` | KSeF-assigned reference for the UPO document itself |
| `issuedAt` | `Instant` | Instant the UPO was issued (UTC) |
| `documentHash` | `String` | Base64 SHA-256 of the sent FA(3) document, sourced from the UPO's `SkrotDokumentu`; `null` if the UPO omits it |
| `invoiceNumber` | `String` | Invoice number the UPO confirms, sourced from `NumerFaktury`; `null` if absent |
| `xml` | `byte[]` | Raw UPO XML returned by KSeF |

Before returning the `Upo`, `awaitUpo()` verifies the UPO's `documentHash` against the hash of the invoice that was sent. If the hashes do not match, the receipt does not correspond to the submitted document and `UpoVerificationException` (a `KsefException` subtype) is thrown. When the UPO carries no hash (`documentHash` is `null`) the check is skipped.

#### UPO signature verification (opt-in)

ksef4j can also verify the Ministry of Finance's XML-DSig signature on the UPO, confirming the document was genuinely signed by KSeF and has not been tampered with. Trust is established through a pinned Ministry signing certificate bundled in the library (the TEST, DEMO, and PROD certificates are all bundled).

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

### Running the live smoke tests (optional)

Two opt-in live probes live in `ksef4j-core`, tagged `smoke` and excluded from the normal build. Both self-skip when their env vars are absent. Surface their diagnostic output with `--no-daemon` (the `smokeTest` task already streams stdout); for raw HTTP wire detail append `-Djdk.httpclient.HttpClient.log=requests,headers`.

A KSeF token is scoped to one environment and one NIP: a `test` token does not authenticate against `demo` or `prod`. There is no shared token across environments.

**Send happy-path (`LiveKsefSmokeTest`)** sends one invoice and waits for the UPO, printing the KSeF reference number, timings, and contract confirmations. It targets the `test` environment only and never touches demo or production - sending submits a real invoice to the government system, so the target is intentionally not configurable.

    export KSEF_TOKEN=...        # a KSeF test-environment token
    export COMPANY_NIP=...       # the NIP the token is scoped to (becomes the invoice issuer)
    ./gradlew :ksef4j-core:smokeTest --tests "*LiveKsefSmokeTest" --no-daemon

**Read query (`LiveQuerySmokeTest`)** is read-only - it queries invoice metadata the NIP is a party to and sends nothing - so it can run against any environment. Select the target with `KSEF_ENV` (`TEST` | `DEMO` | `PROD`, default `TEST`). The token is taken from the environment-specific `KSEF_TOKEN_<ENV>` when set, falling back to plain `KSEF_TOKEN`. Export all three environment tokens once (plus the shared `COMPANY_NIP`) and switch environments with `KSEF_ENV` alone - no swapping between runs:

    export COMPANY_NIP=...       # shared across environments
    export KSEF_TOKEN_TEST=...
    export KSEF_TOKEN_DEMO=...
    export KSEF_TOKEN_PROD=...
    for env in TEST DEMO PROD; do
      KSEF_ENV=$env ./gradlew :ksef4j-core:smokeTest --tests "*LiveQuerySmokeTest" --no-daemon
    done

On Windows PowerShell:

    $env:COMPANY_NIP="..."; $env:KSEF_TOKEN_TEST="..."; $env:KSEF_TOKEN_DEMO="..."; $env:KSEF_TOKEN_PROD="..."
    foreach ($e in 'TEST','DEMO','PROD') { $env:KSEF_ENV=$e; .\gradlew.bat :ksef4j-core:smokeTest --tests "*LiveQuerySmokeTest" --no-daemon }

If you only have one environment's token, exporting plain `KSEF_TOKEN` and `COMPANY_NIP` and running with the default `KSEF_ENV=TEST` still works.

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

### Shipped (v0.1-v0.5)

- KSeF token authentication (challenge → encrypt → access token)
- Invoice loading from YAML
- Hand-coded FA(3) invoice validation (InvoiceValidator); opt-in XSD schema check via the validateFixtures Gradle task
- Single-invoice send via interactive session
- UPO polling with configurable timeout
- UPO Ministry signature verification (opt-in, `verifyUpoSignature(true)`; TEST, DEMO, and PROD certs bundled)
- Local archive of sent invoices and UPOs
- Spring Boot autoconfiguration
- Environment switching (test, demo, prod)
- Invoice metadata query (seller/buyer, date-ranged)
- Single-invoice download by KSeF number
- Type-safe fluent Invoice builder with validate-on-build
- Mock KSeF server for downstream tests (ksef4j-test)

### Planned

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
| v0.1    | Send: single invoice, UPO handling, Spring Boot starter (shipped) |
| v0.2    | Read: invoice metadata query (shipped)             |
| v0.3    | Test support: mock KSeF server (shipped)           |
| v0.4    | Read: invoice download (shipped)                   |
| v0.5    | Type-safe invoice builder (shipped)                |
| 1.0     | Stable send + read + test + builder; published to Maven Central (shipped) |
| post-1.0 | CLI (`ksef send`, `ksef validate`, `ksef list-invoices`); format converters (FA(3) -> PDF/HTML) - demand-permitting |
| later   | Offline/batch, then export/admin/XAdES - demand-gated sibling modules  |

## Project structure

```
ksef4j/
├── ksef4j-core/                 # Framework-agnostic client (send + read)
├── ksef4j-spring-boot-starter/  # Spring Boot autoconfiguration
└── ksef4j-test/                 # Mock KSeF server / test harness
```

Planned post-1.0 (not in the repository yet): `ksef4j-cli` (command-line tool) and `ksef4j-converters` (FA(3) -> PDF/HTML). See the [roadmap](#roadmap).

## Documentation

Documentation lives in this README for 1.0; see the roadmap for what each release added.

For background on KSeF 2.0 itself, see:

- [Official KSeF developer portal](https://ksef.podatki.gov.pl/ksef-na-okres-obligatoryjny/wsparcie-dla-integratorow/)
- [KSeF technical documentation](https://github.com/CIRFMF/ksef-docs)
- [FA(3) logical structure](https://ksef.podatki.gov.pl/ksef-na-okres-obligatoryjny/struktura-logiczna-fa-3)

## Contributing

Contributions, bug reports, and feature requests are welcome. Please open an issue first to discuss substantial changes.

Before submitting a PR, run `./gradlew spotlessApply` to auto-format your code with google-java-format (2-space indent).
CI runs `spotlessCheck` as part of every build and will fail if formatting is not applied.

## License

Licensed under the [Apache License, Version 2.0](LICENSE).
