package io.github.mlkmn.ksef4j.internal.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mlkmn.ksef4j.SendResult;
import io.github.mlkmn.ksef4j.Upo;
import io.github.mlkmn.ksef4j.error.InvoiceValidationException;
import io.github.mlkmn.ksef4j.error.UnsupportedInvoiceFeatureException;
import io.github.mlkmn.ksef4j.invoice.Invoice;
import io.github.mlkmn.ksef4j.invoice.InvoiceFixtures;
import io.github.mlkmn.ksef4j.invoice.Seller;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class DefaultKsefClientTest {

  private static final Upo UPO =
      new Upo(
          "ksef-ref-9",
          "upo-ref-9",
          Instant.parse("2026-06-28T10:16:00Z"),
          null,
          null,
          "<upo/>".getBytes(StandardCharsets.UTF_8));
  private final Clock clock = Clock.fixed(Instant.parse("2026-06-28T10:15:30Z"), ZoneOffset.UTC);

  private static final Duration POLL_TIMEOUT = Duration.ofSeconds(45);

  private DefaultKsefClient client(
      FakeAuthSession auth, FakeInteractiveSession session, FakeUpoPoller poller) {
    return new DefaultKsefClient(
        auth,
        session,
        poller,
        new RecordingArchive(),
        clock,
        POLL_TIMEOUT,
        null,
        new FakeQueryTransport());
  }

  @Test
  void send_validates_maps_marshals_then_calls_auth_and_session() {
    FakeAuthSession auth = new FakeAuthSession("access-token");
    FakeInteractiveSession session = new FakeInteractiveSession();

    SendResult result =
        client(auth, session, new FakeUpoPoller(UPO)).send(InvoiceFixtures.singleLineVat23());

    assertThat(auth.calls).isEqualTo(1);
    assertThat(session.calls).isEqualTo(1);
    assertThat(session.lastAccessToken).isEqualTo("access-token");
    // the real InvoiceMarshaller produced FA(3) XML carrying the invoice number
    String xml = new String(session.lastFa3Xml, StandardCharsets.UTF_8);
    assertThat(xml).contains("FV/2026/05/001");
    assertThat(result.invoiceReferenceNumber()).isEqualTo("invoice-1");
  }

  @Test
  void send_rejects_invalid_invoice_before_any_network_call() {
    FakeAuthSession auth = new FakeAuthSession("access-token");
    FakeInteractiveSession session = new FakeInteractiveSession();
    Invoice invalid = withSellerNip(InvoiceFixtures.singleLineVat23(), "123");

    assertThatThrownBy(() -> client(auth, session, new FakeUpoPoller(UPO)).send(invalid))
        .isInstanceOf(InvoiceValidationException.class);
    assertThat(auth.calls).isZero();
    assertThat(session.calls).isZero();
  }

  @Test
  void send_rejects_unsupported_invoice_before_any_network_call() {
    FakeAuthSession auth = new FakeAuthSession("access-token");
    FakeInteractiveSession session = new FakeInteractiveSession();
    Invoice base = InvoiceFixtures.singleLineVat23();
    Invoice empty =
        new Invoice(
            base.invoiceNumber(),
            base.issueDate(),
            base.saleDate(),
            base.currency(),
            base.exchangeRate(),
            base.seller(),
            base.buyer(),
            List.of());

    assertThatThrownBy(() -> client(auth, session, new FakeUpoPoller(UPO)).send(empty))
        .isInstanceOf(UnsupportedInvoiceFeatureException.class);
    assertThat(auth.calls).isZero();
    assertThat(session.calls).isZero();
  }

  @Test
  void awaitUpo_uses_client_configured_poll_timeout() {
    FakeUpoPoller poller = new FakeUpoPoller(UPO);
    SendResult result =
        client(new FakeAuthSession("access-token"), new FakeInteractiveSession(), poller)
            .send(InvoiceFixtures.singleLineVat23());

    assertThat(result.awaitUpo()).isSameAs(UPO);
    assertThat(poller.lastTimeout).isEqualTo(POLL_TIMEOUT);
  }

  private static Invoice withSellerNip(Invoice base, String nip) {
    Seller s = base.seller();
    return new Invoice(
        base.invoiceNumber(),
        base.issueDate(),
        base.saleDate(),
        base.currency(),
        base.exchangeRate(),
        new Seller(nip, s.name(), s.address()),
        base.buyer(),
        base.items());
  }
}
