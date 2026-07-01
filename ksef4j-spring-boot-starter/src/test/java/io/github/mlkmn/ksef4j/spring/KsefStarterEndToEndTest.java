package io.github.mlkmn.ksef4j.spring;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mlkmn.ksef4j.KsefClient;
import io.github.mlkmn.ksef4j.SendResult;
import io.github.mlkmn.ksef4j.Upo;
import io.github.mlkmn.ksef4j.invoice.InvoiceFixtures;
import io.github.mlkmn.ksef4j.test.MockKsef;
import io.github.mlkmn.ksef4j.test.internal.KsefPayloads;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class KsefStarterEndToEndTest {

  private static final String NIP = "5260250274";

  @Test
  void autoconfigured_client_sends_end_to_end_via_base_url(@TempDir Path archiveDir)
      throws Exception {
    try (MockKsef mock = MockKsef.create()) {
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(KsefAutoConfiguration.class))
          .withPropertyValues(
              "ksef.environment=test",
              "ksef.base-url=" + mock.baseUrl(),
              "ksef.auth.token=test-token",
              "ksef.context.nip=" + NIP,
              "ksef.archive.directory=" + archiveDir)
          .run(
              context -> {
                assertThat(context).hasSingleBean(KsefClient.class);
                KsefClient client = context.getBean(KsefClient.class);
                Upo upo;
                try (SendResult result = client.send(InvoiceFixtures.singleLineVat23())) {
                  upo = result.awaitUpo();
                }
                assertThat(upo.ksefReferenceNumber()).isEqualTo(KsefPayloads.KSEF_NUMBER);
                assertThat(
                        Files.exists(
                            archiveDir
                                .resolve(NIP)
                                .resolve(KsefPayloads.KSEF_NUMBER)
                                .resolve("metadata.json")))
                    .isTrue();
              });
    }
  }
}
