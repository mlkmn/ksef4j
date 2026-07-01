package io.github.mlkmn.ksef4j.internal.client;

import io.github.mlkmn.ksef4j.KsefClient;
import io.github.mlkmn.ksef4j.SendResult;
import io.github.mlkmn.ksef4j.archive.InvoiceArchive;
import io.github.mlkmn.ksef4j.internal.auth.AuthSession;
import io.github.mlkmn.ksef4j.internal.fa3.InvoiceMapper;
import io.github.mlkmn.ksef4j.internal.fa3.InvoiceMarshaller;
import io.github.mlkmn.ksef4j.internal.fa3.InvoiceValidator;
import io.github.mlkmn.ksef4j.internal.fa3.generated.Faktura;
import io.github.mlkmn.ksef4j.internal.session.InteractiveSession;
import io.github.mlkmn.ksef4j.internal.session.SendReceipt;
import io.github.mlkmn.ksef4j.internal.session.UpoPoller;
import io.github.mlkmn.ksef4j.invoice.Invoice;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Internal: the {@link KsefClient} facade. Sequences validate -> map -> marshal -> auth -> send,
 * then hands the references to a {@link DefaultSendResult} for UPO polling and archiving.
 * Stateless aside from its immutable collaborators; thread-safe and intended as a singleton.
 */
public final class DefaultKsefClient implements KsefClient {

    private final AuthSession auth;
    private final InteractiveSession session;
    private final UpoPoller poller;
    private final InvoiceArchive archive;
    private final Clock clock;
    private final Duration upoPollTimeout;
    private final UpoSignatureCheck signatureCheck;
    private final InvoiceMarshaller marshaller = new InvoiceMarshaller();

    public DefaultKsefClient(AuthSession auth, InteractiveSession session, UpoPoller poller,
                             InvoiceArchive archive, Clock clock, Duration upoPollTimeout,
                             UpoSignatureCheck signatureCheck) {
        this.auth = auth;
        this.session = session;
        this.poller = poller;
        this.archive = archive;
        this.clock = clock;
        this.upoPollTimeout = upoPollTimeout;
        this.signatureCheck = signatureCheck;
    }

    @Override
    public SendResult send(Invoice invoice) {
        InvoiceValidator.validate(invoice);
        Faktura faktura = InvoiceMapper.toFaktura(invoice);
        byte[] fa3Xml = marshaller.marshal(faktura);
        String accessToken = auth.accessToken();
        SendReceipt receipt = session.send(fa3Xml, accessToken);
        Instant sentAt = clock.instant();
        return new DefaultSendResult(poller, archive,
                receipt.sessionReferenceNumber(), receipt.invoiceReferenceNumber(),
                accessToken, invoice.seller().nip(), fa3Xml, sentAt, upoPollTimeout, signatureCheck);
    }
}
