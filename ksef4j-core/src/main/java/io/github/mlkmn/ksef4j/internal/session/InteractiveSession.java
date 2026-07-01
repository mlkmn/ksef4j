package io.github.mlkmn.ksef4j.internal.session;

/**
 * Internal seam: sends one FA(3) invoice through a KSeF interactive session (open -> send -> close,
 * the order the UPO requires).
 */
public interface InteractiveSession {

  /**
   * Open a session, send the invoice, and close the session (close is required before the UPO can
   * be generated).
   *
   * @return the session and invoice references for UPO polling
   */
  SendReceipt send(byte[] fa3Xml, String accessToken);
}
