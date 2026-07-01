package io.github.mlkmn.ksef4j.internal.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mlkmn.ksef4j.Upo;
import io.github.mlkmn.ksef4j.error.KsefBusinessException;
import io.github.mlkmn.ksef4j.error.UpoTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultUpoPollerTest {

  private static final Instant START = Instant.parse("2026-06-28T10:00:00Z");
  private static final String SESSION = "SESS1";
  private static final String INVOICE = "INV1";
  private static final String ACCESS = "ACCESS";
  private static final byte[] UPO_XML =
      ("<u:P xmlns:u=\"urn:x\">"
              + "<u:NumerKSeFDokumentu>KSEF-123</u:NumerKSeFDokumentu>"
              + "<u:DataNadaniaNumeruKSeF>2026-06-28T10:01:00Z</u:DataNadaniaNumeruKSeF>"
              + "</u:P>")
          .getBytes(StandardCharsets.UTF_8);

  private MutableClock clock;
  private FakeUpoTransport transport;
  private DefaultUpoPoller poller;

  @BeforeEach
  void setUp() {
    clock = new MutableClock(START);
    transport = new FakeUpoTransport();
    transport.upoXml = UPO_XML;
    // flat 1s backoff keeps the arithmetic obvious; stub Sleeper advances the clock
    poller =
        new DefaultUpoPoller(
            transport, attempt -> Duration.ofSeconds(1), clock, d -> clock.advance(d));
    // default: invoice is accepted so existing session-focused tests reach Phase 2 unchanged
    transport.invoiceStatuses.add(FakeUpoTransport.invoiceStatus(200));
  }

  @Test
  void ready_on_first_poll_returns_assembled_upo() {
    transport.statuses.add(FakeUpoTransport.ready("UPOREF1", "http://x/u1"));

    Upo upo = poller.pollUntilSettled(SESSION, INVOICE, ACCESS, Duration.ofSeconds(30));

    assertThat(upo.ksefNumber()).isEqualTo("KSEF-123");
    assertThat(upo.upoReferenceNumber()).isEqualTo("UPOREF1");
    assertThat(upo.issuedAt()).isEqualTo(Instant.parse("2026-06-28T10:01:00Z"));
    assertThat(upo.xml()).isEqualTo(UPO_XML);
    assertThat(transport.sessionStatusCount.get()).isEqualTo(1);
    assertThat(transport.lastUpoUrl.toString()).isEqualTo("http://x/u1");
  }

  @Test
  void polls_until_ready() {
    transport.statuses.add(FakeUpoTransport.inProgress());
    transport.statuses.add(FakeUpoTransport.inProgress());
    transport.statuses.add(FakeUpoTransport.ready("UPOREF1", "http://x/u1"));

    Upo upo = poller.pollUntilSettled(SESSION, INVOICE, ACCESS, Duration.ofSeconds(30));

    assertThat(upo.ksefNumber()).isEqualTo("KSEF-123");
    assertThat(transport.sessionStatusCount.get()).isEqualTo(3);
    assertThat(clock.instant()).isEqualTo(START.plusSeconds(2)); // two 1s backoff waits
  }

  @Test
  void timeout_throws_upo_timeout() {
    transport.statuses.add(FakeUpoTransport.inProgress());

    assertThatThrownBy(
            () -> poller.pollUntilSettled(SESSION, INVOICE, ACCESS, Duration.ofSeconds(5)))
        .isInstanceOf(UpoTimeoutException.class)
        .asInstanceOf(
            org.assertj.core.api.InstanceOfAssertFactories.type(UpoTimeoutException.class))
        .satisfies(
            ex ->
                org.assertj.core.api.Assertions.assertThat(ex.timeout())
                    .isEqualTo(Duration.ofSeconds(5)));
  }

  @Test
  void terminal_failure_throws_business_exception() {
    transport.statuses.add(FakeUpoTransport.failure(445));

    assertThatThrownBy(
            () -> poller.pollUntilSettled(SESSION, INVOICE, ACCESS, Duration.ofSeconds(30)))
        .isInstanceOf(KsefBusinessException.class)
        .hasMessageContaining("445");
  }

  @Test
  void success_code_without_upo_keeps_polling() {
    transport.statuses.add(FakeUpoTransport.successNoUpo());
    transport.statuses.add(FakeUpoTransport.ready("UPOREF1", "http://x/u1"));

    Upo upo = poller.pollUntilSettled(SESSION, INVOICE, ACCESS, Duration.ofSeconds(30));

    assertThat(upo.upoReferenceNumber()).isEqualTo("UPOREF1");
    assertThat(transport.sessionStatusCount.get()).isEqualTo(2);
  }

  @Test
  void intermediate_1xx_statuses_keep_polling() {
    transport.statuses.add(FakeUpoTransport.processing(150));
    transport.statuses.add(FakeUpoTransport.processing(170));
    transport.statuses.add(FakeUpoTransport.ready("UPOREF1", "http://x/u1"));

    Upo upo = poller.pollUntilSettled(SESSION, INVOICE, ACCESS, Duration.ofSeconds(30));

    assertThat(upo.upoReferenceNumber()).isEqualTo("UPOREF1");
    assertThat(transport.sessionStatusCount.get()).isEqualTo(3);
  }

  @Test
  void interrupted_sleep_throws_transport_and_restores_flag() {
    transport.statuses.add(FakeUpoTransport.inProgress());
    DefaultUpoPoller interruptingPoller =
        new DefaultUpoPoller(
            transport,
            attempt -> Duration.ofSeconds(1),
            clock,
            d -> {
              throw new InterruptedException("test");
            });

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                interruptingPoller.pollUntilSettled(
                    SESSION, INVOICE, ACCESS, Duration.ofSeconds(30)))
        .isInstanceOf(io.github.mlkmn.ksef4j.error.KsefTransportException.class);
    org.assertj.core.api.Assertions.assertThat(Thread.interrupted()).isTrue();
  }

  @Test
  void rejected_invoice_fails_fast_with_reason_and_no_session_poll() {
    transport.invoiceStatuses.clear();
    transport.invoiceStatuses.add(FakeUpoTransport.invoiceStatus(440, "Niepoprawny NIP nabywcy"));

    assertThatThrownBy(
            () -> poller.pollUntilSettled(SESSION, INVOICE, ACCESS, Duration.ofSeconds(30)))
        .isInstanceOf(KsefBusinessException.class)
        .hasMessageContaining("440")
        .hasMessageContaining("Niepoprawny NIP nabywcy");
    assertThat(transport.sessionStatusCount.get()).isZero(); // never polled the session
  }

  @Test
  void waits_for_invoice_acceptance_then_fetches_upo() {
    transport.invoiceStatuses.clear();
    transport.invoiceStatuses.add(FakeUpoTransport.invoiceStatus(150));
    transport.invoiceStatuses.add(FakeUpoTransport.invoiceStatus(200));
    transport.statuses.add(FakeUpoTransport.ready("UPOREF1", "http://x/u1"));

    Upo upo = poller.pollUntilSettled(SESSION, INVOICE, ACCESS, Duration.ofSeconds(30));

    assertThat(upo.ksefNumber()).isEqualTo("KSEF-123");
    assertThat(transport.invoiceStatusCount.get()).isEqualTo(2);
  }

  @Test
  void times_out_while_invoice_still_processing() {
    transport.invoiceStatuses.clear();
    transport.invoiceStatuses.add(FakeUpoTransport.invoiceStatus(150));

    assertThatThrownBy(
            () -> poller.pollUntilSettled(SESSION, INVOICE, ACCESS, Duration.ofSeconds(5)))
        .isInstanceOf(UpoTimeoutException.class);
  }

  static final class MutableClock extends Clock {
    private Instant now;

    MutableClock(Instant start) {
      this.now = start;
    }

    void advance(Duration d) {
      now = now.plus(d);
    }

    @Override
    public Instant instant() {
      return now;
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }
  }
}
