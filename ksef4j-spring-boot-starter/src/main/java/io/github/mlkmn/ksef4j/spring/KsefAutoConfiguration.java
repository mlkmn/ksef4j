package io.github.mlkmn.ksef4j.spring;

import io.github.mlkmn.ksef4j.KsefClient;
import io.github.mlkmn.ksef4j.archive.InvoiceArchive;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/** Autoconfigures a {@link KsefClient} bean from {@code ksef.*} properties. */
@AutoConfiguration
@ConditionalOnClass(KsefClient.class)
@EnableConfigurationProperties(KsefProperties.class)
@ConditionalOnProperty(prefix = "ksef.auth", name = "token")
public class KsefAutoConfiguration {

    /**
     * Build the client from the bound properties. A user-supplied {@link InvoiceArchive} bean
     * takes precedence over {@code ksef.archive.directory}; if neither is set the client uses
     * its no-op archive. Backs off if the application already defines a {@link KsefClient}.
     *
     * @param properties     the bound {@code ksef.*} configuration
     * @param archiveProvider optional application-supplied archive
     * @return the configured client
     */
    @Bean
    @ConditionalOnMissingBean
    public KsefClient ksefClient(KsefProperties properties, ObjectProvider<InvoiceArchive> archiveProvider) {
        KsefClient.Builder builder = KsefClient.builder()
                .environment(properties.environment())
                .tokenAuth(properties.auth().token(), properties.context().nip())
                .upoPollTimeout(properties.upo().pollTimeout());

        if (properties.baseUrl() != null) {
            builder.baseUrl(properties.baseUrl());
        }

        InvoiceArchive customArchive = archiveProvider.getIfAvailable();
        if (customArchive != null) {
            builder.archive(customArchive);
        } else if (properties.archive().directory() != null) {
            builder.archiveDirectory(properties.archive().directory());
        }
        return builder.build();
    }
}
