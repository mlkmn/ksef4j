package io.github.mlkmn.ksef4j.spring;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mlkmn.ksef4j.Environment;
import io.github.mlkmn.ksef4j.KsefClient;
import io.github.mlkmn.ksef4j.SendResult;
import io.github.mlkmn.ksef4j.archive.ArchiveEntry;
import io.github.mlkmn.ksef4j.archive.ArchiveKey;
import io.github.mlkmn.ksef4j.archive.InvoiceArchive;
import io.github.mlkmn.ksef4j.invoice.Invoice;
import io.github.mlkmn.ksef4j.query.InvoiceMetadata;
import io.github.mlkmn.ksef4j.query.InvoiceMetadataPage;
import io.github.mlkmn.ksef4j.query.InvoiceQuery;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class KsefAutoConfigurationTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(KsefAutoConfiguration.class));

  @Test
  void createsClientWhenTokenPresent() {
    runner
        .withPropertyValues(
            "ksef.environment=test", "ksef.auth.token=tok", "ksef.context.nip=5260250274")
        .run(context -> assertThat(context).hasSingleBean(KsefClient.class));
  }

  @Test
  void noClientWhenTokenAbsent() {
    runner
        .withPropertyValues("ksef.context.nip=5260250274")
        .run(context -> assertThat(context).doesNotHaveBean(KsefClient.class));
  }

  @Test
  void bindsAllProperties() {
    runner
        .withPropertyValues(
            "ksef.environment=demo",
            "ksef.auth.token=tok",
            "ksef.context.nip=5260250274",
            "ksef.archive.directory=/var/ksef",
            "ksef.upo.poll-timeout=45s")
        .run(
            context -> {
              KsefProperties p = context.getBean(KsefProperties.class);
              assertThat(p.environment()).isEqualTo(Environment.DEMO);
              assertThat(p.auth().token()).isEqualTo("tok");
              assertThat(p.context().nip()).isEqualTo("5260250274");
              assertThat(p.archive().directory()).isEqualTo(Path.of("/var/ksef"));
              assertThat(p.upo().pollTimeout()).isEqualTo(Duration.ofSeconds(45));
            });
  }

  @Test
  void appliesDefaults() {
    runner
        .withPropertyValues("ksef.auth.token=tok", "ksef.context.nip=5260250274")
        .run(
            context -> {
              KsefProperties p = context.getBean(KsefProperties.class);
              assertThat(p.environment()).isEqualTo(Environment.TEST);
              assertThat(p.upo().pollTimeout()).isEqualTo(Duration.ofSeconds(180));
              assertThat(p.archive().directory()).isNull();
            });
  }

  @Test
  void customArchiveBeanAndDirectoryDoNotConflict() {
    runner
        .withUserConfiguration(CustomArchiveConfig.class)
        .withPropertyValues(
            "ksef.auth.token=tok",
            "ksef.context.nip=5260250274",
            "ksef.archive.directory=/var/ksef")
        .run(
            context -> {
              // The builder throws if handed both an archive and a directory; a clean start proves
              // the autoconfig de-duplicated. (End-to-end archive selection is covered in C3.)
              assertThat(context).hasNotFailed().hasSingleBean(KsefClient.class);
              assertThat(context).hasSingleBean(InvoiceArchive.class);
              assertThat(context.getBean(KsefProperties.class).archive().directory())
                  .isEqualTo(Path.of("/var/ksef"));
            });
  }

  @Test
  void backsOffWhenClientBeanProvided() {
    runner
        .withUserConfiguration(CustomClientConfig.class)
        .withPropertyValues("ksef.auth.token=tok", "ksef.context.nip=5260250274")
        .run(
            context -> {
              assertThat(context).hasSingleBean(KsefClient.class);
              assertThat(context.getBean(KsefClient.class)).isSameAs(CustomClientConfig.STUB);
            });
  }

  @Configuration
  static class CustomArchiveConfig {
    @Bean
    InvoiceArchive customArchive() {
      return new InvoiceArchive() {
        @Override
        public void store(ArchiveEntry entry) {}

        @Override
        public Optional<ArchiveEntry> find(ArchiveKey key) {
          return Optional.empty();
        }
      };
    }
  }

  @Configuration
  static class CustomClientConfig {
    static final KsefClient STUB =
        new KsefClient() {
          @Override
          public SendResult send(Invoice invoice) {
            throw new UnsupportedOperationException();
          }

          @Override
          public InvoiceMetadataPage queryInvoices(InvoiceQuery query) {
            throw new UnsupportedOperationException();
          }

          @Override
          public Stream<InvoiceMetadata> streamInvoices(InvoiceQuery query) {
            throw new UnsupportedOperationException();
          }
        };

    @Bean
    KsefClient ksefClient() {
      return STUB;
    }
  }
}
