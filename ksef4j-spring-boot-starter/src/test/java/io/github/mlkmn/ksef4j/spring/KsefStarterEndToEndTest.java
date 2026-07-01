package io.github.mlkmn.ksef4j.spring;

import io.github.mlkmn.ksef4j.KsefClient;
import io.github.mlkmn.ksef4j.SendResult;
import io.github.mlkmn.ksef4j.Upo;
import io.github.mlkmn.ksef4j.internal.http.FakeKsef;
import io.github.mlkmn.ksef4j.internal.http.KsefHappyPath;
import io.github.mlkmn.ksef4j.invoice.InvoiceFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class KsefStarterEndToEndTest {

    @Test
    void autoconfigured_client_sends_end_to_end_via_base_url(@TempDir Path archiveDir) throws Exception {
        try (FakeKsef fake = new FakeKsef()) {
            KsefHappyPath.install(fake);
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(KsefAutoConfiguration.class))
                    .withPropertyValues(
                            "ksef.environment=test",
                            "ksef.base-url=" + fake.baseUri(),
                            "ksef.auth.token=test-token",
                            "ksef.context.nip=" + KsefHappyPath.NIP,
                            "ksef.archive.directory=" + archiveDir)
                    .run(context -> {
                        assertThat(context).hasSingleBean(KsefClient.class);
                        KsefClient client = context.getBean(KsefClient.class);
                        Upo upo;
                        try (SendResult result = client.send(InvoiceFixtures.singleLineVat23())) {
                            upo = result.awaitUpo();
                        }
                        assertThat(upo.ksefReferenceNumber()).isEqualTo(KsefHappyPath.KSEF_NUMBER);
                        assertThat(Files.exists(archiveDir
                                .resolve(KsefHappyPath.NIP)
                                .resolve(KsefHappyPath.KSEF_NUMBER)
                                .resolve("metadata.json"))).isTrue();
                    });
        }
    }
}
