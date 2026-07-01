package io.github.mlkmn.ksef4j.spring;

import io.github.mlkmn.ksef4j.Environment;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Binds {@code ksef.*} configuration for the Spring Boot starter.
 *
 * @param environment the KSeF environment to target
 * @param baseUrl optional advanced gateway/proxy override; null by default, which uses the
 *     environment's host
 * @param auth KSeF API authentication
 * @param context the taxpayer context the token acts for
 * @param archive local archive settings
 * @param upo UPO polling settings
 */
@ConfigurationProperties("ksef")
public record KsefProperties(
    @DefaultValue("test") Environment environment,
    URI baseUrl,
    @DefaultValue Auth auth,
    @DefaultValue Context context,
    @DefaultValue Archive archive,
    @DefaultValue Upo upo) {

  /**
   * KSeF API authentication.
   *
   * @param token the KSeF token; security-sensitive, keep out of logs and version control
   */
  public record Auth(String token) {}

  /**
   * The taxpayer context the token acts for.
   *
   * @param nip 10-digit NIP (no separators)
   */
  public record Context(String nip) {}

  /**
   * Local archive settings.
   *
   * @param directory directory sent invoices are archived under
   */
  public record Archive(Path directory) {}

  /**
   * UPO polling settings.
   *
   * @param pollTimeout maximum time to wait for the UPO
   */
  public record Upo(@DefaultValue("180s") Duration pollTimeout) {}
}
