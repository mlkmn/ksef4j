package io.github.mlkmn.ksef4j;

import io.github.mlkmn.ksef4j.error.UpoVerificationException;
import io.github.mlkmn.ksef4j.internal.crypto.UpoSigningCertificates;
import io.github.mlkmn.ksef4j.internal.crypto.UpoXmlSignature;

import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;

/**
 * Verifies that a UPO was signed by the Polish Ministry of Finance, by validating its XML-DSig
 * signature against the bundled (pinned) Ministry signing certificate for the given environment.
 * Trust comes only from the pinned certificate - the UPO's embedded KeyInfo is ignored.
 *
 * <p>Throws {@link UpoVerificationException} if the signature is invalid, the document is
 * malformed, the pinned certificate is outside its validity window, or no signing certificate is
 * bundled for the environment. Stateless and thread-safe.
 */
public final class UpoSignatureVerifier {

    /** Verify {@code upoXml} against the bundled signing certificate for {@code environment}. */
    public void verify(byte[] upoXml, Environment environment) {
        X509Certificate cert = UpoSigningCertificates.load(environment);
        try {
            cert.checkValidity();
        } catch (CertificateExpiredException | CertificateNotYetValidException e) {
            throw new UpoVerificationException(
                    "Pinned UPO signing certificate for " + environment
                            + " is outside its validity window - update the bundled certificate", e);
        }
        UpoXmlSignature.verify(upoXml, cert.getPublicKey());
    }
}
