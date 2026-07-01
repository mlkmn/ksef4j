package io.github.mlkmn.ksef4j.internal.client;

/**
 * Internal seam: verify a UPO's signature (bytes already bound to an environment by the wiring).
 */
@FunctionalInterface
public interface UpoSignatureCheck {
  void check(byte[] upoXml);
}
