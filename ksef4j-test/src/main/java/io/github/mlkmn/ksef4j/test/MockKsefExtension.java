package io.github.mlkmn.ksef4j.test;

import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 extension owning a {@link MockKsef}: starts it once, resets to the default happy path
 * before each test (so per-test overrides do not leak), stops it after all tests. Delegates the
 * scenario DSL. Use via {@code @RegisterExtension static final MockKsefExtension ksef =
 * MockKsefExtension.create();}.
 */
public final class MockKsefExtension
    implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback {

  private MockKsef mock;

  private MockKsefExtension() {}

  /** Create a new extension instance; register it with {@code @RegisterExtension}. */
  public static MockKsefExtension create() {
    return new MockKsefExtension();
  }

  @Override
  public void beforeAll(ExtensionContext context) {
    mock = MockKsef.create();
  }

  @Override
  public void beforeEach(ExtensionContext context) {
    mock.reset();
  }

  @Override
  public void afterAll(ExtensionContext context) {
    if (mock != null) {
      mock.close();
    }
  }

  /** Base URL to pass to {@code KsefClient.builder().baseUrl(...)}. */
  public URI baseUrl() {
    return mock.baseUrl();
  }

  /** Returns a {@link SendScenario} for scripting the next invoice-send response. */
  public SendScenario onSend() {
    return mock.onSend();
  }

  /** Returns a {@link UpoScenario} for scripting the UPO document the mock serves. */
  public UpoScenario onUpo() {
    return mock.onUpo();
  }

  /** Returns a {@link QueryScenario} for scripting the next query response. */
  public QueryScenario onQuery() {
    return mock.onQuery();
  }

  /** Returns an {@link AuthScenario} for scripting the authentication handshake. */
  public AuthScenario onAuth() {
    return mock.onAuth();
  }

  /** The plaintext invoiceHash values the client submitted, in order. */
  public List<String> sentInvoiceHashes() {
    return mock.sentInvoiceHashes();
  }

  /** Number of requests received for the given path (path-only match). */
  public int requestCount(String path) {
    return mock.requestCount(path);
  }

  /**
   * All request URLs (path plus any query string) received by the mock, in chronological order.
   * Useful for {@code containsSubsequence} assertions on the full request flow.
   */
  public List<String> requestedUrls() {
    return mock.requestedUrls();
  }

  /**
   * Headers of the first request the mock received for {@code path} (path-only match, consistent
   * with requestCount). Returns an empty map if no such request was recorded.
   */
  public Map<String, String> firstRequestHeaders(String path) {
    return mock.firstRequestHeaders(path);
  }
}
