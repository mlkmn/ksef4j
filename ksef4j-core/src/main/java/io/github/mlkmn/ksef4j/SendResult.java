package io.github.mlkmn.ksef4j;

import io.github.mlkmn.ksef4j.error.KsefException;
import io.github.mlkmn.ksef4j.error.UpoTimeoutException;

/**
 * Handle for an in-flight or completed invoice send. Lifecycle:
 *
 * <ul>
 *   <li>created by {@link KsefClient#send(io.github.mlkmn.ksef4j.invoice.Invoice)}; {@link
 *       #invoiceReferenceNumber()} and {@link #awaitUpo()} are valid.
 *   <li>once {@link #awaitUpo()} returns the {@link Upo}, subsequent calls return the same cached
 *       {@link Upo} without re-polling. If {@code awaitUpo} throws (timeout or business error) the
 *       result stays re-pollable -- a later call polls again.
 *   <li>after {@link #close()}, {@link #awaitUpo()} throws {@link IllegalStateException}; further
 *       {@code close()} calls are no-ops.
 * </ul>
 *
 * Not thread-safe. Designed for single-caller use within try-with-resources.
 */
public interface SendResult extends AutoCloseable {

  /**
   * KSeF-assigned tracking reference for the invoice, available immediately after {@code send()}.
   * This is the per-invoice reference returned by the send-invoice call, used for correlation; the
   * final KSeF number assigned during processing is on {@link Upo#ksefReferenceNumber()}.
   */
  String invoiceReferenceNumber();

  /**
   * Block until the UPO is available, the call is rejected, or the client's configured UPO
   * poll-timeout elapses (set via {@code KsefClient.Builder.upoPollTimeout(Duration)}).
   *
   * @throws UpoTimeoutException if the configured timeout is exceeded
   * @throws KsefException if KSeF reports a business error
   */
  Upo awaitUpo();

  /**
   * Idempotent state-only close. Does not throw. The KSeF session is already closed during send.
   */
  @Override
  void close();
}
