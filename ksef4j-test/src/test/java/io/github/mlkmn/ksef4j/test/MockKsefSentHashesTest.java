package io.github.mlkmn.ksef4j.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

class MockKsefSentHashesTest {

  @Test
  void sentInvoiceHashes_throws_on_unparseable_request_body() throws Exception {
    try (MockKsef mock = MockKsef.create()) {
      URI invoices =
          URI.create(
              mock.baseUrl() + "/sessions/online/" + MockKsefDefaults.SESSION_REF + "/invoices");
      HttpClient.newHttpClient()
          .send(
              HttpRequest.newBuilder(invoices)
                  .header("Content-Type", "application/json")
                  .POST(HttpRequest.BodyPublishers.ofString("not-json"))
                  .build(),
              HttpResponse.BodyHandlers.discarding());

      assertThatThrownBy(mock::sentInvoiceHashes).isInstanceOf(IllegalStateException.class);
    }
  }

  @Test
  void sentInvoiceHashes_returns_hash_for_valid_body() throws Exception {
    try (MockKsef mock = MockKsef.create()) {
      URI invoices =
          URI.create(
              mock.baseUrl() + "/sessions/online/" + MockKsefDefaults.SESSION_REF + "/invoices");
      HttpClient.newHttpClient()
          .send(
              HttpRequest.newBuilder(invoices)
                  .header("Content-Type", "application/json")
                  .POST(HttpRequest.BodyPublishers.ofString("{\"invoiceHash\":\"abc123\"}"))
                  .build(),
              HttpResponse.BodyHandlers.discarding());

      assertThat(mock.sentInvoiceHashes()).containsExactly("abc123");
    }
  }
}
