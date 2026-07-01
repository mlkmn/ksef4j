package io.github.mlkmn.ksef4j.internal.http;

import io.github.mlkmn.ksef4j.internal.crypto.EncryptedInvoice;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class JdkHttpTransportSessionTest {

    private FakeKsef fake;
    private JdkHttpTransport transport;

    @BeforeEach
    void setUp() throws Exception {
        fake = new FakeKsef();
        transport = new JdkHttpTransport(
                EnvironmentEndpoints.ofBaseUri(fake.baseUri()),
                Duration.ofSeconds(5), Duration.ofSeconds(5));
    }

    @AfterEach
    void tearDown() {
        fake.close();
    }

    @Test
    void open_session_sends_encryption_block_and_parses_reference() {
        fake.stubJson("/sessions/online", 200, "{\"referenceNumber\":\"SESS1\",\"validUntil\":\"x\"}");
        Requests.OpenSession req = Requests.OpenSession.from(
                "key".getBytes(StandardCharsets.UTF_8), "iv".getBytes(StandardCharsets.UTF_8),
                new Requests.FormCode("FA (3)", "1-0E", "FA"));

        Responses.OpenSession r = transport.openSession(req, "ACC");

        assertThat(r.referenceNumber()).isEqualTo("SESS1");
        FakeKsef.RecordedRequest captured = fake.requests.get(0);
        assertThat(captured.method()).isEqualTo("POST");
        assertThat(captured.headers().get("Authorization")).isEqualTo("Bearer ACC");
        assertThat(captured.body()).contains("\"encryptedSymmetricKey\":\"" + Base64.getEncoder().encodeToString("key".getBytes(StandardCharsets.UTF_8)) + "\"");
        assertThat(captured.body()).contains("\"systemCode\":\"FA (3)\"");
    }

    @Test
    void send_invoice_posts_encrypted_payload_and_parses_reference() {
        fake.stubJson("/sessions/online/SESS1/invoices", 202, "{\"referenceNumber\":\"INV1\"}");
        EncryptedInvoice enc = new EncryptedInvoice(
                "cipher".getBytes(StandardCharsets.UTF_8), "pt".getBytes(StandardCharsets.UTF_8), 6L,
                "ct".getBytes(StandardCharsets.UTF_8), 16L);

        Responses.SendInvoice r = transport.sendInvoice("SESS1", Requests.SendInvoice.from(enc), "ACC");

        assertThat(r.referenceNumber()).isEqualTo("INV1");
        String body = fake.requests.get(0).body();
        assertThat(body).contains("\"encryptedInvoiceContent\":\"" + Base64.getEncoder().encodeToString("cipher".getBytes(StandardCharsets.UTF_8)) + "\"");
        assertThat(body).contains("\"invoiceSize\":6");
        assertThat(body).contains("\"offlineMode\":false");
        FakeKsef.RecordedRequest captured = fake.requests.get(0);
        assertThat(captured.headers().get("Authorization")).isEqualTo("Bearer ACC");
    }

    @Test
    void fetch_session_status_parses_upo_pages() {
        fake.stubJson("/sessions/SESS1", 200,
                "{\"status\":{\"code\":200,\"description\":\"ok\",\"details\":[]},\"invoiceCount\":1,\"upo\":{\"pages\":[{\"referenceNumber\":\"U1\",\"downloadUrl\":\"http://x/u1\",\"downloadUrlExpirationDate\":\"y\"}]}}");

        Responses.SessionStatus s = transport.fetchSessionStatus("SESS1", "ACC");

        assertThat(s.status().code()).isEqualTo(200);
        assertThat(s.invoiceCount()).isEqualTo(1);
        assertThat(s.upo().pages().get(0).downloadUrl()).isEqualTo("http://x/u1");
        assertThat(fake.requests.get(0).headers().get("Authorization")).isEqualTo("Bearer ACC");
        assertThat(fake.requests.get(0).method()).isEqualTo("GET");
    }

    @Test
    void close_session_treats_204_as_success() {
        fake.stubBytes("/sessions/online/SESS1/close", 204, new byte[0], "application/json");

        transport.closeSession("SESS1", "ACC"); // must not throw

        FakeKsef.RecordedRequest captured = fake.requests.get(0);
        assertThat(captured.method()).isEqualTo("POST");
        assertThat(captured.path()).isEqualTo("/sessions/online/SESS1/close");
        assertThat(captured.headers().get("Authorization")).isEqualTo("Bearer ACC");
    }

    @Test
    void fetch_upo_returns_xml_bytes_without_authorization_header() {
        byte[] xml = "<UPO/>".getBytes(StandardCharsets.UTF_8);
        fake.stubBytes("/upo/U1", 200, xml, "application/xml");

        byte[] result = transport.fetchUpo(URI.create(fake.baseUri() + "/upo/U1"));

        assertThat(result).isEqualTo(xml);
        assertThat(fake.requests.get(0).headers()).doesNotContainKey("Authorization");
    }
}
