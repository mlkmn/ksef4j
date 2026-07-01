package io.github.mlkmn.ksef4j.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.mlkmn.ksef4j.Environment;
import io.github.mlkmn.ksef4j.KsefClient;
import io.github.mlkmn.ksef4j.query.InvoiceMetadata;
import io.github.mlkmn.ksef4j.query.InvoiceMetadataPage;
import io.github.mlkmn.ksef4j.query.InvoiceQuery;
import java.time.LocalDate;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Live read smoke test against the KSeF {@code test} environment: queries invoice metadata for the
 * configured NIP. Opt-in; run with {@code ./gradlew :ksef4j-core:smokeTest --tests
 * "*LiveQuerySmokeTest"} after exporting {@code KSEF_TOKEN} and {@code COMPANY_NIP}. Self-skips
 * when absent.
 *
 * <p>Read-only: it queries metadata the NIP is a party to (sales invoices the smoke send tests have
 * created over time) and never sends anything.
 */
@Tag("smoke")
class LiveQuerySmokeTest {

  @Test
  void queries_seller_invoice_metadata_from_test_environment() {
    String token = System.getenv("KSEF_TOKEN");
    String nip = System.getenv("COMPANY_NIP");
    assumeTrue(
        token != null && !token.isBlank() && nip != null && !nip.isBlank(),
        "KSEF_TOKEN and COMPANY_NIP must be set to run the live smoke test");

    KsefClient client =
        KsefClient.builder().environment(Environment.TEST).tokenAuth(token, nip).build();

    LocalDate to = LocalDate.now();
    LocalDate from = to.minusMonths(2); // safely under KSeF's 3-month maximum range
    InvoiceQuery query = InvoiceQuery.asSeller().issuedBetween(from, to).pageSize(50).build();
    System.out.println(
        "[smoke-query] querying invoices issued as seller "
            + nip
            + " between "
            + from
            + " and "
            + to
            + " on KSeF TEST");

    InvoiceMetadataPage page = client.queryInvoices(query);

    assertThat(page).isNotNull();
    assertThat(page.invoices()).isNotNull();
    assertThat(page.pageOffset()).isZero();
    assertThat(page.pageSize()).isEqualTo(50);

    System.out.println(
        "[smoke-query] page returned: "
            + page.invoices().size()
            + " invoice(s), hasMore="
            + page.hasMore()
            + ", nextOffset="
            + page.nextOffset());

    for (InvoiceMetadata m : page.invoices()) {
      System.out.println(
          "[smoke-query]   "
              + m.ksefNumber()
              + " | no="
              + m.invoiceNumber()
              + " | issued="
              + m.issueDate()
              + " | seller="
              + (m.seller() == null ? null : m.seller().nip())
              + " | buyer="
              + (m.buyer() == null ? null : m.buyer().nip())
              + " | gross="
              + m.grossAmount()
              + " | schema="
              + m.schema()
              + " | type="
              + m.invoiceType());
    }

    if (!page.invoices().isEmpty()) {
      InvoiceMetadata first = page.invoices().get(0);
      assertThat(first.ksefNumber()).isNotBlank();
      assertThat(first.seller()).isNotNull();
      System.out.println(
          "[smoke-query] confirmed: real KSeF metadata parsed (ksefNumber + seller populated)");
    } else {
      System.out.println(
          "[smoke-query] NOTE: zero invoices in the window - the request shape and response "
              + "envelope parsed OK, but field mapping was not exercised (no recent seller invoices in TEST)");
    }

    // Exercise the lazy auto-paging path over the wire too.
    long streamed;
    try (Stream<InvoiceMetadata> stream =
        client.streamInvoices(
            InvoiceQuery.asSeller().issuedBetween(from, to).pageSize(10).build())) {
      streamed = stream.count();
    }
    System.out.println(
        "[smoke-query] streamInvoices walked " + streamed + " invoice(s) across pages");
    assertThat(streamed).isGreaterThanOrEqualTo(page.invoices().size());

    System.out.println(
        "[smoke-query] confirmed: queryInvoices + streamInvoices succeeded against KSeF TEST");
  }
}
