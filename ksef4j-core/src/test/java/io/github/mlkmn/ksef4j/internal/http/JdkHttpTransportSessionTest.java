package io.github.mlkmn.ksef4j.internal.http;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.github.mlkmn.ksef4j.internal.crypto.EncryptedInvoice;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JdkHttpTransportSessionTest {

  private WireMockServer wm;
  private JdkHttpTransport transport;

  @BeforeEach
  void setUp() {
    wm = new WireMockServer(options().dynamicPort());
    wm.start();
    transport =
        new JdkHttpTransport(
            EnvironmentEndpoints.ofBaseUri(URI.create(wm.baseUrl())),
            Duration.ofSeconds(5),
            Duration.ofSeconds(5));
  }

  @AfterEach
  void tearDown() {
    wm.stop();
  }

  @Test
  void open_session_sends_encryption_block_and_parses_reference() {
    wm.stubFor(
        post(urlEqualTo("/sessions/online"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"referenceNumber\":\"SESS1\",\"validUntil\":\"x\"}")));
    Requests.OpenSession req =
        Requests.OpenSession.from(
            "key".getBytes(StandardCharsets.UTF_8),
            "iv".getBytes(StandardCharsets.UTF_8),
            new Requests.FormCode("FA (3)", "1-0E", "FA"));

    Responses.OpenSession r = transport.openSession(req, "ACC");

    assertThat(r.referenceNumber()).isEqualTo("SESS1");
    LoggedRequest captured = wm.findAll(anyRequestedFor(anyUrl())).get(0);
    assertThat(captured.getMethod().value()).isEqualTo("POST");
    assertThat(captured.getHeader("Authorization")).isEqualTo("Bearer ACC");
    assertThat(captured.getBodyAsString())
        .contains(
            "\"encryptedSymmetricKey\":\""
                + Base64.getEncoder().encodeToString("key".getBytes(StandardCharsets.UTF_8))
                + "\"");
    assertThat(captured.getBodyAsString()).contains("\"systemCode\":\"FA (3)\"");
  }

  @Test
  void send_invoice_posts_encrypted_payload_and_parses_reference() {
    wm.stubFor(
        post(urlEqualTo("/sessions/online/SESS1/invoices"))
            .willReturn(
                aResponse()
                    .withStatus(202)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"referenceNumber\":\"INV1\"}")));
    EncryptedInvoice enc =
        new EncryptedInvoice(
            "cipher".getBytes(StandardCharsets.UTF_8),
            "pt".getBytes(StandardCharsets.UTF_8),
            6L,
            "ct".getBytes(StandardCharsets.UTF_8),
            16L);

    Responses.SendInvoice r = transport.sendInvoice("SESS1", Requests.SendInvoice.from(enc), "ACC");

    assertThat(r.referenceNumber()).isEqualTo("INV1");
    LoggedRequest captured = wm.findAll(anyRequestedFor(anyUrl())).get(0);
    assertThat(captured.getBodyAsString())
        .contains(
            "\"encryptedInvoiceContent\":\""
                + Base64.getEncoder().encodeToString("cipher".getBytes(StandardCharsets.UTF_8))
                + "\"");
    assertThat(captured.getBodyAsString()).contains("\"invoiceSize\":6");
    assertThat(captured.getBodyAsString()).contains("\"offlineMode\":false");
    assertThat(captured.getHeader("Authorization")).isEqualTo("Bearer ACC");
  }

  @Test
  void fetch_session_status_parses_upo_pages() {
    wm.stubFor(
        get(urlEqualTo("/sessions/SESS1"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{\"status\":{\"code\":200,\"description\":\"ok\",\"details\":[]},\"invoiceCount\":1,\"upo\":{\"pages\":[{\"referenceNumber\":\"U1\",\"downloadUrl\":\"http://x/u1\",\"downloadUrlExpirationDate\":\"y\"}]}}")));

    Responses.SessionStatus s = transport.fetchSessionStatus("SESS1", "ACC");

    assertThat(s.status().code()).isEqualTo(200);
    assertThat(s.invoiceCount()).isEqualTo(1);
    assertThat(s.upo().pages().get(0).downloadUrl()).isEqualTo("http://x/u1");
    LoggedRequest captured = wm.findAll(anyRequestedFor(anyUrl())).get(0);
    assertThat(captured.getHeader("Authorization")).isEqualTo("Bearer ACC");
    assertThat(captured.getMethod().value()).isEqualTo("GET");
  }

  @Test
  void close_session_treats_204_as_success() {
    wm.stubFor(
        post(urlEqualTo("/sessions/online/SESS1/close")).willReturn(aResponse().withStatus(204)));

    transport.closeSession("SESS1", "ACC"); // must not throw

    LoggedRequest captured = wm.findAll(anyRequestedFor(anyUrl())).get(0);
    assertThat(captured.getMethod().value()).isEqualTo("POST");
    assertThat(captured.getUrl()).isEqualTo("/sessions/online/SESS1/close");
    assertThat(captured.getHeader("Authorization")).isEqualTo("Bearer ACC");
  }

  @Test
  void fetch_upo_returns_xml_bytes_without_authorization_header() {
    byte[] xml = "<UPO/>".getBytes(StandardCharsets.UTF_8);
    wm.stubFor(
        get(urlEqualTo("/upo/U1"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/xml")
                    .withBody(xml)));

    byte[] result = transport.fetchUpo(URI.create(wm.baseUrl() + "/upo/U1"));

    assertThat(result).isEqualTo(xml);
    assertThat(wm.findAll(anyRequestedFor(anyUrl())).get(0).containsHeader("Authorization"))
        .isFalse();
  }
}
