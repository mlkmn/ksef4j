package io.github.mlkmn.ksef4j.internal.client;

import io.github.mlkmn.ksef4j.internal.session.InteractiveSession;
import io.github.mlkmn.ksef4j.internal.session.SendReceipt;

/**
 * Test double for {@link InteractiveSession}: records the sent bytes and token, returns a fixed
 * receipt.
 */
final class FakeInteractiveSession implements InteractiveSession {

  int calls;
  byte[] lastFa3Xml;
  String lastAccessToken;
  SendReceipt receipt = new SendReceipt("session-1", "invoice-1");

  @Override
  public SendReceipt send(byte[] fa3Xml, String accessToken) {
    calls++;
    lastFa3Xml = fa3Xml;
    lastAccessToken = accessToken;
    return receipt;
  }
}
