package io.github.mlkmn.ksef4j.internal.crypto;

import io.github.mlkmn.ksef4j.Environment;
import io.github.mlkmn.ksef4j.error.UpoVerificationException;

import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads the bundled Ministry of Finance UPO-signing certificate for an environment from
 * {@code /keys/<env>-upo-signing.pem}. Only TEST is bundled today; DEMO and PROD throw until
 * their certificate is captured and added (a zero-code drop-in). Caches per environment.
 */
public final class UpoSigningCertificates {

    private static final Map<Environment, X509Certificate> CACHE = new ConcurrentHashMap<>();

    private UpoSigningCertificates() {
    }

    public static X509Certificate load(Environment environment) {
        return CACHE.computeIfAbsent(environment, UpoSigningCertificates::read);
    }

    private static X509Certificate read(Environment environment) {
        String resource = "/keys/" + environment.name().toLowerCase(Locale.ROOT) + "-upo-signing.pem";
        try (InputStream in = UpoSigningCertificates.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new UpoVerificationException(
                        "UPO signing certificate not bundled for " + environment + " (expected " + resource + ")");
            }
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
        } catch (UpoVerificationException e) {
            throw e;
        } catch (Exception e) {
            throw new UpoVerificationException("Failed to parse bundled UPO signing certificate: " + resource, e);
        }
    }
}
