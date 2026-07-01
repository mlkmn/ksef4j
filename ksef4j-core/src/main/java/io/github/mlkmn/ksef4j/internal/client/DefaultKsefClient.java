package io.github.mlkmn.ksef4j.internal.client;

import io.github.mlkmn.ksef4j.KsefClient;
import io.github.mlkmn.ksef4j.SendResult;
import io.github.mlkmn.ksef4j.archive.InvoiceArchive;
import io.github.mlkmn.ksef4j.error.ResultTruncatedException;
import io.github.mlkmn.ksef4j.internal.auth.AuthSession;
import io.github.mlkmn.ksef4j.internal.fa3.InvoiceMapper;
import io.github.mlkmn.ksef4j.internal.fa3.InvoiceMarshaller;
import io.github.mlkmn.ksef4j.internal.fa3.InvoiceValidator;
import io.github.mlkmn.ksef4j.internal.fa3.generated.Faktura;
import io.github.mlkmn.ksef4j.internal.http.HttpTransport;
import io.github.mlkmn.ksef4j.internal.http.Requests;
import io.github.mlkmn.ksef4j.internal.http.Responses;
import io.github.mlkmn.ksef4j.internal.query.QueryMetadataMapper;
import io.github.mlkmn.ksef4j.internal.session.InteractiveSession;
import io.github.mlkmn.ksef4j.internal.session.SendReceipt;
import io.github.mlkmn.ksef4j.internal.session.UpoPoller;
import io.github.mlkmn.ksef4j.invoice.Invoice;
import io.github.mlkmn.ksef4j.query.InvoiceMetadata;
import io.github.mlkmn.ksef4j.query.InvoiceMetadataPage;
import io.github.mlkmn.ksef4j.query.InvoiceQuery;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Internal: the {@link KsefClient} facade. Sequences validate -> map -> marshal -> auth -> send,
 * then hands the references to a {@link DefaultSendResult} for UPO polling and archiving. Stateless
 * aside from its immutable collaborators; thread-safe and intended as a singleton.
 */
public final class DefaultKsefClient implements KsefClient {

  private final AuthSession auth;
  private final InteractiveSession session;
  private final UpoPoller poller;
  private final InvoiceArchive archive;
  private final Clock clock;
  private final Duration upoPollTimeout;
  private final UpoSignatureCheck signatureCheck;
  private final HttpTransport transport;
  private final InvoiceMarshaller marshaller = new InvoiceMarshaller();

  public DefaultKsefClient(
      AuthSession auth,
      InteractiveSession session,
      UpoPoller poller,
      InvoiceArchive archive,
      Clock clock,
      Duration upoPollTimeout,
      UpoSignatureCheck signatureCheck,
      HttpTransport transport) {
    this.auth = auth;
    this.session = session;
    this.poller = poller;
    this.archive = archive;
    this.clock = clock;
    this.upoPollTimeout = upoPollTimeout;
    this.signatureCheck = signatureCheck;
    this.transport = transport;
  }

  @Override
  public SendResult send(Invoice invoice) {
    InvoiceValidator.validate(invoice);
    Faktura faktura = InvoiceMapper.toFaktura(invoice);
    byte[] fa3Xml = marshaller.marshal(faktura);
    String accessToken = auth.accessToken();
    SendReceipt receipt = session.send(fa3Xml, accessToken);
    Instant sentAt = clock.instant();
    return new DefaultSendResult(
        poller,
        archive,
        receipt.sessionReferenceNumber(),
        receipt.invoiceReferenceNumber(),
        accessToken,
        invoice.seller().nip(),
        fa3Xml,
        sentAt,
        upoPollTimeout,
        signatureCheck);
  }

  @Override
  public InvoiceMetadataPage queryInvoices(InvoiceQuery query) {
    String accessToken = auth.accessToken();
    return fetchPage(query, query.pageOffset(), accessToken);
  }

  @Override
  public Stream<InvoiceMetadata> streamInvoices(InvoiceQuery query) {
    Iterator<InvoiceMetadata> it =
        new Iterator<>() {
          private String accessToken;
          private InvoiceMetadataPage page;
          private int index = 0;

          @Override
          public boolean hasNext() {
            if (page == null) {
              accessToken = auth.accessToken();
              page = fetchPage(query, query.pageOffset(), accessToken);
            }
            while (index >= page.invoices().size()) {
              if (page.truncated()) {
                throw new ResultTruncatedException(
                    "Query matched more than KSeF's 10000-record cap; "
                        + "narrow the date range or filters");
              }
              if (!page.hasMore()) {
                return false;
              }
              page = fetchPage(query, page.nextOffset(), accessToken);
              index = 0;
            }
            return true;
          }

          @Override
          public InvoiceMetadata next() {
            if (!hasNext()) {
              throw new NoSuchElementException();
            }
            return page.invoices().get(index++);
          }
        };
    return StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(it, Spliterator.ORDERED | Spliterator.NONNULL), false);
  }

  @Override
  public byte[] downloadInvoice(String ksefNumber) {
    String accessToken = auth.accessToken();
    return transport.downloadInvoice(ksefNumber, accessToken);
  }

  private InvoiceMetadataPage fetchPage(InvoiceQuery query, int pageOffset, String accessToken) {
    Responses.QueryMetadata wire =
        transport.queryInvoiceMetadata(
            Requests.QueryMetadata.from(query), pageOffset, query.pageSize(), accessToken);
    return QueryMetadataMapper.toPage(wire, pageOffset, query.pageSize());
  }
}
