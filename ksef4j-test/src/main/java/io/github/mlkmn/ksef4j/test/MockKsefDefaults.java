package io.github.mlkmn.ksef4j.test;

/**
 * The fixed reference values the {@link MockKsef} happy path emits. Downstream tests can assert
 * against these instead of hard-coding literals - e.g. {@code
 * assertThat(upo.ksefNumber()).isEqualTo(MockKsefDefaults.KSEF_NUMBER)}.
 */
public final class MockKsefDefaults {

  private MockKsefDefaults() {}

  /** Session reference number the mock assigns on {@code POST /sessions/online}. */
  public static final String SESSION_REF = "SESS1";

  /** Per-invoice reference number the mock returns on send. */
  public static final String INVOICE_REF = "INV1";

  /** UPO page (and UPO) reference number in the mock's session status. */
  public static final String UPO_PAGE_REF = "U1";

  /** KSeF number the mock's UPO confirms. */
  public static final String KSEF_NUMBER = "5260250274-20260629-010001234567-AB";

  /** Instant (ISO-8601) the mock's UPO reports as its issue time. */
  public static final String UPO_ISSUED_AT = "2026-06-29T10:15:30Z";
}
