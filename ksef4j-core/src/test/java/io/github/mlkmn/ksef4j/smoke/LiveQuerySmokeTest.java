package io.github.mlkmn.ksef4j.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.mlkmn.ksef4j.Environment;
import io.github.mlkmn.ksef4j.KsefClient;
import io.github.mlkmn.ksef4j.query.InvoiceMetadata;
import io.github.mlkmn.ksef4j.query.InvoiceMetadataPage;
import io.github.mlkmn.ksef4j.query.InvoiceQuery;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Live read smoke test against a KSeF environment: queries invoice metadata for the configured NIP,
 * both as seller (sales invoices) and as buyer (purchase invoices). Opt-in; run with {@code
 * ./gradlew :ksef4j-core:smokeTest --tests "*LiveQuerySmokeTest"} after exporting {@code
 * KSEF_TOKEN} and {@code COMPANY_NIP}. Self-skips when absent.
 *
 * <p>The target environment is selected by {@code KSEF_ENV} ({@code TEST} | {@code DEMO} | {@code
 * PROD}, default {@code TEST}). The token is read from the environment-specific {@code
 * KSEF_TOKEN_<ENV>} (e.g. {@code KSEF_TOKEN_PROD}) when set, falling back to plain {@code
 * KSEF_TOKEN} - so all three environments' tokens can be exported once and selected by {@code
 * KSEF_ENV} without swapping. {@code COMPANY_NIP} is shared across environments.
 *
 * <p>Read-only: it queries metadata the NIP is a party to and never sends anything.
 */
@Tag("smoke")
class LiveQuerySmokeTest {

  @Test
  void queries_seller_invoice_metadata_from_configured_environment() {
    Setup setup = resolveSetup();

    LocalDate to = LocalDate.now();
    LocalDate from = to.minusMonths(2); // safely under KSeF's 3-month maximum range
    InvoiceQuery query = InvoiceQuery.asSeller().issuedBetween(from, to).pageSize(50).build();
    System.out.println(
        "[smoke-query:seller] querying invoices issued as seller "
            + setup.nip()
            + " between "
            + from
            + " and "
            + to
            + " on KSeF "
            + setup.environment());

    InvoiceMetadataPage page = setup.client().queryInvoices(query);

    assertThat(page).isNotNull();
    assertThat(page.invoices()).isNotNull();
    assertThat(page.pageOffset()).isZero();
    assertThat(page.pageSize()).isEqualTo(50);

    printPage("seller", page);

    if (!page.invoices().isEmpty()) {
      InvoiceMetadata first = page.invoices().get(0);
      assertThat(first.ksefNumber()).isNotBlank();
      assertThat(first.seller()).isNotNull();
      System.out.println(
          "[smoke-query:seller] confirmed: real KSeF metadata parsed (ksefNumber + seller"
              + " populated)");
    } else {
      System.out.println(
          "[smoke-query:seller] NOTE: zero invoices in the window - the request shape and response "
              + "envelope parsed OK, but field mapping was not exercised (no recent seller invoices"
              + " on "
              + setup.environment()
              + ")");
    }

    // Exercise the lazy auto-paging path over the wire too.
    long streamed;
    try (Stream<InvoiceMetadata> stream =
        setup
            .client()
            .streamInvoices(InvoiceQuery.asSeller().issuedBetween(from, to).pageSize(10).build())) {
      streamed = stream.count();
    }
    System.out.println(
        "[smoke-query:seller] streamInvoices walked " + streamed + " invoice(s) across pages");
    assertThat(streamed).isGreaterThanOrEqualTo(page.invoices().size());

    System.out.println(
        "[smoke-query:seller] confirmed: queryInvoices + streamInvoices succeeded against KSeF "
            + setup.environment());
  }

  @Test
  void queries_buyer_invoice_metadata_from_configured_environment() {
    Setup setup = resolveSetup();

    LocalDate to = LocalDate.now();
    LocalDate from = to.minusMonths(2); // safely under KSeF's 3-month maximum range
    InvoiceQuery query = InvoiceQuery.asBuyer().receivedBetween(from, to).pageSize(50).build();
    System.out.println(
        "[smoke-query:buyer] querying invoices received as buyer "
            + setup.nip()
            + " between "
            + from
            + " and "
            + to
            + " on KSeF "
            + setup.environment());

    InvoiceMetadataPage page = setup.client().queryInvoices(query);

    assertThat(page).isNotNull();
    assertThat(page.invoices()).isNotNull();
    assertThat(page.pageOffset()).isZero();
    assertThat(page.pageSize()).isEqualTo(50);

    printPage("buyer", page);

    if (!page.invoices().isEmpty()) {
      InvoiceMetadata first = page.invoices().get(0);
      assertThat(first.ksefNumber()).isNotBlank();
      assertThat(first.buyer()).isNotNull();
      System.out.println(
          "[smoke-query:buyer] confirmed: real KSeF metadata parsed (ksefNumber + buyer populated)");
    } else {
      System.out.println(
          "[smoke-query:buyer] NOTE: zero invoices in the window - the request shape and response "
              + "envelope parsed OK, but field mapping was not exercised (no recent purchase"
              + " invoices on "
              + setup.environment()
              + ")");
    }

    // Exercise the lazy auto-paging path over the wire too.
    long streamed;
    try (Stream<InvoiceMetadata> stream =
        setup
            .client()
            .streamInvoices(
                InvoiceQuery.asBuyer().receivedBetween(from, to).pageSize(10).build())) {
      streamed = stream.count();
    }
    System.out.println(
        "[smoke-query:buyer] streamInvoices walked " + streamed + " invoice(s) across pages");
    assertThat(streamed).isGreaterThanOrEqualTo(page.invoices().size());

    System.out.println(
        "[smoke-query:buyer] confirmed: queryInvoices + streamInvoices succeeded against KSeF "
            + setup.environment());
  }

  @Test
  void downloads_invoice_xml_from_configured_environment() {
    Setup setup = resolveSetup();

    LocalDate to = LocalDate.now();
    LocalDate from = to.minusMonths(2);
    InvoiceMetadataPage page =
        setup
            .client()
            .queryInvoices(InvoiceQuery.asSeller().issuedBetween(from, to).pageSize(10).build());
    assumeFalse(
        page.invoices().isEmpty(),
        "no invoices in the window to download on " + setup.environment());

    String ksefNumber = page.invoices().get(0).ksefNumber();
    System.out.println(
        "[smoke-download] downloading invoice " + ksefNumber + " from KSeF " + setup.environment());

    byte[] xml = setup.client().downloadInvoice(ksefNumber);

    assertThat(xml).isNotEmpty();
    String text = new String(xml, StandardCharsets.UTF_8);
    // The returned XML may be KSeF-normalized, so assert structure, not byte-equality.
    assertThat(text).contains("Faktura");
    System.out.println(
        "[smoke-download] downloaded "
            + xml.length
            + " bytes; contains its KSeF number="
            + text.contains(ksefNumber));
    System.out.println(
        "[smoke-download] confirmed: downloadInvoice succeeded against KSeF "
            + setup.environment());
  }

  private static void printPage(String role, InvoiceMetadataPage page) {
    System.out.println(
        "[smoke-query:"
            + role
            + "] page returned: "
            + page.invoices().size()
            + " invoice(s), hasMore="
            + page.hasMore()
            + ", nextOffset="
            + page.nextOffset());
    for (InvoiceMetadata m : page.invoices()) {
      System.out.println(
          "[smoke-query:"
              + role
              + "]   "
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
  }

  private static Setup resolveSetup() {
    String envName = System.getenv().getOrDefault("KSEF_ENV", "TEST").toUpperCase(Locale.ROOT);
    String token =
        firstNonBlank(System.getenv("KSEF_TOKEN_" + envName), System.getenv("KSEF_TOKEN"));
    String nip = System.getenv("COMPANY_NIP");
    assumeTrue(
        token != null && !token.isBlank() && nip != null && !nip.isBlank(),
        "KSEF_TOKEN_"
            + envName
            + " (or KSEF_TOKEN) and COMPANY_NIP must be set to run the live smoke test");

    Environment environment = Environment.valueOf(envName);
    KsefClient client = KsefClient.builder().environment(environment).tokenAuth(token, nip).build();
    return new Setup(environment, client, nip);
  }

  private static String firstNonBlank(String preferred, String fallback) {
    return (preferred != null && !preferred.isBlank()) ? preferred : fallback;
  }

  private record Setup(Environment environment, KsefClient client, String nip) {}
}
