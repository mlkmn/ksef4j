# Bundled KSeF environment certificates

Six certificates are bundled, one per (environment, KSeF usage) pair. All were sourced from each environment's `GET https://<host>/v2/security/public-key-certificates` endpoint and selected by their `usage` field.

| File | Environment | Usage (KSeF field) | Purpose in ksef4j | Retrieved |
|---|---|---|---|---|
| test-token.pem | KSeF TEST | KsefTokenEncryption | Encrypts the authentication token sent during session init | 2026-06-28 |
| test-symmetric.pem | KSeF TEST | SymmetricKeyEncryption | Encrypts the AES session key used for invoice upload | 2026-06-28 |
| demo-token.pem | KSeF DEMO | KsefTokenEncryption | Encrypts the authentication token sent during session init | 2026-06-29 |
| demo-symmetric.pem | KSeF DEMO | SymmetricKeyEncryption | Encrypts the AES session key used for invoice upload | 2026-06-29 |
| prod-token.pem | KSeF PROD | KsefTokenEncryption | Encrypts the authentication token sent during session init | 2026-06-29 |
| prod-symmetric.pem | KSeF PROD | SymmetricKeyEncryption | Encrypts the AES session key used for invoice upload | 2026-06-29 |

## Certificate details

All six certificates share the same issuer chain and validity window:

- Subject: CN=Ministerstwo Finansow, O=Ministerstwo Finansow, L=Warszawa, S=mazowieckie, C=PL
- Issuer: CN=Certum SMIME RSA CA, O=Asseco Data Systems S.A., C=PL
- Valid: 2025-09-29 to 2027-09-29
- Algorithm: RSA 2048-bit
- Encryption scheme: RSA-OAEP / SHA-256 (per KSeF auth specification)

## Loading

`BundledCertificates.load(Environment, KeyUsage)` is the single classpath-loading path; `ClasspathKeyResolver` delegates to it and caches the resulting `PublicKey` per (environment, usage) pair. All three environments (TEST, DEMO, PROD) resolve offline without any network call.

## Certificate rotation and dynamic refresh

KSeF rotates these certificates; each carries an expiry date (currently 2027-09-29 for all six). `HttpCertificateResolver` (planned) refreshes them dynamically near expiry by fetching the live `/v2/security/public-key-certificates` endpoint, so the bundled files serve as a fast offline default rather than the only source. When KSeF publishes new certificates before the planned dynamic resolver is available, replace the corresponding `.pem` files and re-bundle.

## UPO-signing certificates

Separately from the encryption certificates above, the Ministry signs each UPO (the official confirmation) with an XAdES signature. The opt-in `UpoSignatureVerifier` checks that signature against a pinned signing certificate captured from a genuine UPO per environment. These are leaf certificates extracted from the `ds:KeyInfo` of a real UPO fetched over TLS from each environment; trust is the pinned certificate, and the UPO's embedded KeyInfo is otherwise ignored.

| File | Environment | Purpose in ksef4j | Captured |
|---|---|---|---|
| test-upo-signing.pem | KSeF TEST | Pinned key `UpoSignatureVerifier` trusts for TEST UPOs | 2026-06-29 |
| demo-upo-signing.pem | KSeF DEMO | Pinned key `UpoSignatureVerifier` trusts for DEMO UPOs | 2026-07-01 |
| prod-upo-signing.pem | KSeF PROD | Pinned key `UpoSignatureVerifier` trusts for PROD UPOs | 2026-07-01 |

All three environments are now captured. Each is the leaf extracted from a genuine UPO's `ds:KeyInfo`; the PROD leaf (serial `43CF16451D52663497280106F60DC3A3`) was taken from a real PROD UPO and its signature verified end-to-end against the bundled cert before adding. All three share the same issuer chain (`CN=Certum SMIME RSA CA, O=Asseco Data Systems S.A.`) and 2025-09-29 to 2027-09-29 validity window as the encryption certificates above.

Note: some accounting tools (e.g. iFirma) pretty-print the UPO XML on export. Signature verification is whitespace-sensitive (C14N over the enveloped document), so a re-indented UPO will fail verification even though the certificate is correct; verify against the raw single-line UPO bytes as returned by KSeF.
