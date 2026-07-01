/**
 * Top-level public API of ksef4j.
 *
 * <ul>
 *   <li>{@link io.github.mlkmn.ksef4j.KsefClient}: entry point for building and sending invoices.</li>
 *   <li>{@link io.github.mlkmn.ksef4j.SendResult}: handle for an in-flight or completed send.</li>
 *   <li>{@link io.github.mlkmn.ksef4j.Upo}: receipt of acceptance.</li>
 *   <li>{@link io.github.mlkmn.ksef4j.Environment}: deployment selector (test / demo / prod).</li>
 * </ul>
 *
 * <p>Anything under {@code io.github.mlkmn.ksef4j.internal.*} is not supported public API
 * and may change without notice.
 */
package io.github.mlkmn.ksef4j;
