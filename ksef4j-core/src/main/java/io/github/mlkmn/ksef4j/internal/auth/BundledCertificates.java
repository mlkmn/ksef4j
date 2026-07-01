package io.github.mlkmn.ksef4j.internal.auth;

import io.github.mlkmn.ksef4j.Environment;
import io.github.mlkmn.ksef4j.error.KsefAuthenticationException;

import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Locale;

/**
 * Internal: loads a bundled X.509 certificate for an (environment, usage) pair from
 * {@code /keys/<env>-<usage>.pem} on the classpath. v0.1 bundles TEST, DEMO and PROD,
 * both usages. Shared by {@link ClasspathKeyResolver} and {@link HttpCertificateResolver}.
 */
final class BundledCertificates {

    private BundledCertificates() {
    }

    static X509Certificate load(Environment environment, KeyUsage usage) {
        String resource = "/keys/" + environment.name().toLowerCase(Locale.ROOT) + "-" + suffix(usage) + ".pem";
        try (InputStream in = BundledCertificates.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new KsefAuthenticationException("Bundled KSeF certificate missing from classpath: " + resource);
            }
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
        } catch (KsefAuthenticationException e) {
            throw e;
        } catch (Exception e) {
            throw new KsefAuthenticationException("Failed to parse bundled KSeF certificate: " + resource, e);
        }
    }

    private static String suffix(KeyUsage usage) {
        return switch (usage) {
            case TOKEN_ENCRYPTION -> "token";
            case SYMMETRIC_KEY_ENCRYPTION -> "symmetric";
        };
    }
}
