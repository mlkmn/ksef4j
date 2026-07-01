package io.github.mlkmn.ksef4j.internal.fa3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mlkmn.ksef4j.error.UnsupportedInvoiceFeatureException;
import io.github.mlkmn.ksef4j.internal.fa3.generated.Faktura;
import io.github.mlkmn.ksef4j.internal.fa3.generated.TKodFormularza;
import io.github.mlkmn.ksef4j.internal.fa3.generated.TKodWaluty;
import io.github.mlkmn.ksef4j.internal.fa3.generated.TNaglowek;
import io.github.mlkmn.ksef4j.internal.fa3.generated.TRodzajFaktury;
import io.github.mlkmn.ksef4j.invoice.Address;
import io.github.mlkmn.ksef4j.invoice.Buyer;
import io.github.mlkmn.ksef4j.invoice.Invoice;
import io.github.mlkmn.ksef4j.invoice.InvoiceFixtures;
import io.github.mlkmn.ksef4j.invoice.Item;
import io.github.mlkmn.ksef4j.invoice.Seller;
import io.github.mlkmn.ksef4j.invoice.VatRate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class InvoiceMapperTest {

  @Test
  void single_line_invoice_maps_seller_buyer_dates_and_one_line() {
    Faktura f = InvoiceMapper.toFaktura(InvoiceFixtures.singleLineVat23());

    // Header
    TNaglowek naglowek = f.getNaglowek();
    assertThat(naglowek).isNotNull();
    assertThat(naglowek.getKodFormularza().getValue()).isEqualTo(TKodFormularza.FA);
    assertThat(naglowek.getKodFormularza().getKodSystemowy()).isEqualTo("FA (3)");
    assertThat(naglowek.getKodFormularza().getWersjaSchemy()).isEqualTo("1-0E");
    assertThat(naglowek.getWariantFormularza()).isEqualTo((byte) 3);
    assertThat(naglowek.getDataWytworzeniaFa()).isNotNull();
    assertThat(naglowek.getSystemInfo()).isEqualTo("ksef4j 0.1.0");

    // Podmiot1 (seller)
    assertThat(f.getPodmiot1()).isNotNull();
    assertThat(f.getPodmiot1().getDaneIdentyfikacyjne().getNIP()).isEqualTo("5260250274");
    assertThat(f.getPodmiot1().getDaneIdentyfikacyjne().getNazwa()).isEqualTo("Example Sp. z o.o.");
    assertThat(f.getPodmiot1().getAdres().getKodKraju().value()).isEqualTo("PL");
    assertThat(f.getPodmiot1().getAdres().getAdresL1()).isEqualTo("ul. Marszalkowska 1/2");

    // Podmiot2 (buyer)
    assertThat(f.getPodmiot2()).isNotNull();
    assertThat(f.getPodmiot2().getDaneIdentyfikacyjne().getNIP()).isEqualTo("1234567890");
    assertThat(f.getPodmiot2().getDaneIdentyfikacyjne().getNazwa())
        .isEqualTo("Customer Sp. z o.o.");
    assertThat(f.getPodmiot2().getAdres().getKodKraju().value()).isEqualTo("PL");
    assertThat(f.getPodmiot2().getAdres().getAdresL1()).isEqualTo("ul. Pulawska 100");

    // Fa header
    Faktura.Fa fa = f.getFa();
    assertThat(fa).isNotNull();
    assertThat(fa.getKodWaluty()).isEqualTo(TKodWaluty.PLN);
    assertThat(fa.getP1().toString()).isEqualTo("2026-05-09");
    assertThat(fa.getP2()).isEqualTo("FV/2026/05/001");
    // saleDate defaults to issueDate
    assertThat(fa.getP6().toString()).isEqualTo("2026-05-09");
    assertThat(fa.getRodzajFaktury()).isEqualTo(TRodzajFaktury.VAT);

    // Single line
    List<Faktura.Fa.FaWiersz> rows = fa.getFaWiersz();
    assertThat(rows).hasSize(1);
    Faktura.Fa.FaWiersz row = rows.get(0);
    assertThat(row.getNrWierszaFa()).isEqualTo(java.math.BigInteger.ONE);
    assertThat(row.getP7()).isEqualTo("Consulting services, March 2026");
    // P_8A = unit name, P_8B = quantity (per schema)
    assertThat(row.getP8A()).isEqualTo("szt.");
    assertThat(row.getP8B()).isEqualByComparingTo(new BigDecimal("1"));
    assertThat(row.getP9A()).isEqualByComparingTo(new BigDecimal("10000.00"));
    assertThat(row.getP11()).isEqualByComparingTo(new BigDecimal("10000.00"));
    assertThat(row.getP12()).isEqualTo("23");

    // Totals: P_13_1 = sum net 23%, P_14_1 = sum vat 23%
    assertThat(fa.getP131()).isEqualByComparingTo(new BigDecimal("10000.00"));
    assertThat(fa.getP141()).isEqualByComparingTo(new BigDecimal("2300.00"));
    // Other rates not present
    assertThat(fa.getP132()).isNull();
    assertThat(fa.getP142()).isNull();
    assertThat(fa.getP133()).isNull();
    assertThat(fa.getP143()).isNull();
    assertThat(fa.getP1361()).isNull();
    // Total gross
    assertThat(fa.getP15()).isEqualByComparingTo(new BigDecimal("12300.00"));
  }

  @Test
  void multi_line_mixed_rates_computes_per_rate_totals_and_gross() {
    Faktura f = InvoiceMapper.toFaktura(InvoiceFixtures.multiLineMixedRates());

    // saleDate is explicit and different from issueDate
    assertThat(f.getFa().getP1().toString()).isEqualTo("2026-05-09");
    assertThat(f.getFa().getP6().toString()).isEqualTo("2026-05-08");
    assertThat(f.getFa().getP2()).isEqualTo("FV/2026/05/002");
    assertThat(f.getFa().getKodWaluty()).isEqualTo(TKodWaluty.PLN);

    // Address mapped through for both parties
    assertThat(f.getPodmiot1().getAdres().getKodKraju().value()).isEqualTo("PL");
    assertThat(f.getPodmiot1().getAdres().getAdresL1()).isEqualTo("ul. Marszalkowska 1/2");
    assertThat(f.getPodmiot2().getAdres().getKodKraju().value()).isEqualTo("PL");
    assertThat(f.getPodmiot2().getAdres().getAdresL1()).isEqualTo("ul. Pulawska 100");

    List<Faktura.Fa.FaWiersz> rows = f.getFa().getFaWiersz();
    assertThat(rows).hasSize(3);

    // Row 1: 2 * 100.00 = 200.00 net @ 23%
    assertThat(rows.get(0).getNrWierszaFa()).isEqualTo(java.math.BigInteger.ONE);
    assertThat(rows.get(0).getP7()).isEqualTo("Item A 23%");
    assertThat(rows.get(0).getP8A()).isEqualTo("szt.");
    assertThat(rows.get(0).getP8B()).isEqualByComparingTo(new BigDecimal("2"));
    assertThat(rows.get(0).getP9A()).isEqualByComparingTo(new BigDecimal("100.00"));
    assertThat(rows.get(0).getP11()).isEqualByComparingTo(new BigDecimal("200.00"));
    assertThat(rows.get(0).getP12()).isEqualTo("23");

    // Row 2: 3 * 50.00 = 150.00 net @ 8%
    assertThat(rows.get(1).getNrWierszaFa()).isEqualTo(java.math.BigInteger.valueOf(2));
    assertThat(rows.get(1).getP11()).isEqualByComparingTo(new BigDecimal("150.00"));
    assertThat(rows.get(1).getP12()).isEqualTo("8");

    // Row 3: 1 * 75.00 = 75.00 net @ 0%
    assertThat(rows.get(2).getNrWierszaFa()).isEqualTo(java.math.BigInteger.valueOf(3));
    assertThat(rows.get(2).getP11()).isEqualByComparingTo(new BigDecimal("75.00"));
    // P_12 distinguishes domestic 0% ("0 KR") from intra-EU ("0 WDT") /
    // export ("0 EX"); v0.1 only emits the domestic code.
    assertThat(rows.get(2).getP12()).isEqualTo("0 KR");

    // Per-rate totals
    // 23%: net 200.00, VAT 46.00
    assertThat(f.getFa().getP131()).isEqualByComparingTo(new BigDecimal("200.00"));
    assertThat(f.getFa().getP141()).isEqualByComparingTo(new BigDecimal("46.00"));
    // 8%: net 150.00, VAT 12.00
    assertThat(f.getFa().getP132()).isEqualByComparingTo(new BigDecimal("150.00"));
    assertThat(f.getFa().getP142()).isEqualByComparingTo(new BigDecimal("12.00"));
    // 5%: not present
    assertThat(f.getFa().getP133()).isNull();
    assertThat(f.getFa().getP143()).isNull();
    // 0%: net 75.00 in P_13_6_1 (sum of 0% sales, excl. intra-EU/exports);
    // no VAT field pairs with this rate.
    assertThat(f.getFa().getP1361()).isEqualByComparingTo(new BigDecimal("75.00"));

    // Total gross: 200 + 46 + 150 + 12 + 75 + 0 = 483.00
    assertThat(f.getFa().getP15()).isEqualByComparingTo(new BigDecimal("483.00"));
  }

  @Test
  void item_unit_overrides_default() {
    Invoice invoice =
        invoiceWithItem(
            new Item(
                "Service",
                new BigDecimal("8"),
                new BigDecimal("100.00"),
                VatRate.VAT_23,
                "godzina",
                null));

    Faktura f = InvoiceMapper.toFaktura(invoice);

    assertThat(f.getFa().getFaWiersz().get(0).getP8A()).isEqualTo("godzina");
  }

  @Test
  void item_blank_unit_falls_back_to_default() {
    Invoice invoice =
        invoiceWithItem(
            new Item(
                "Service",
                new BigDecimal("8"),
                new BigDecimal("100.00"),
                VatRate.VAT_23,
                "   ",
                null));

    Faktura f = InvoiceMapper.toFaktura(invoice);

    assertThat(f.getFa().getFaWiersz().get(0).getP8A()).isEqualTo("szt.");
  }

  @Test
  void item_pkwiu_emitted_when_present() {
    Invoice invoice =
        invoiceWithItem(
            new Item(
                "Service",
                new BigDecimal("8"),
                new BigDecimal("100.00"),
                VatRate.VAT_23,
                null,
                "62.01.11.0"));

    Faktura f = InvoiceMapper.toFaktura(invoice);

    assertThat(f.getFa().getFaWiersz().get(0).getPKWiU()).isEqualTo("62.01.11.0");
  }

  @Test
  void item_pkwiu_absent_when_null() {
    Invoice invoice =
        invoiceWithItem(
            new Item(
                "Service",
                new BigDecimal("8"),
                new BigDecimal("100.00"),
                VatRate.VAT_23,
                null,
                null));

    Faktura f = InvoiceMapper.toFaktura(invoice);

    assertThat(f.getFa().getFaWiersz().get(0).getPKWiU()).isNull();
  }

  @Test
  void empty_items_rejected_with_unsupported_feature_exception() {
    Address sellerAddress = new Address("PL", "ul. Marszalkowska 1/2", null, null);
    Address buyerAddress = new Address("PL", "ul. Pulawska 100", null, null);
    Invoice empty =
        new Invoice(
            "FV/2026/05/003",
            LocalDate.of(2026, 5, 9),
            null,
            null,
            null,
            new Seller("5260250274", "Example Sp. z o.o.", sellerAddress),
            new Buyer("1234567890", "Customer Sp. z o.o.", buyerAddress),
            List.<Item>of());
    assertThatThrownBy(() -> InvoiceMapper.toFaktura(empty))
        .isInstanceOf(UnsupportedInvoiceFeatureException.class)
        .hasMessageContaining("at least one item");
  }

  @Test
  void eur_invoice_emits_exchange_rate_and_vat_in_pln_twin() {
    Faktura f = InvoiceMapper.toFaktura(InvoiceFixtures.eurSingleLineVat23());
    Faktura.Fa fa = f.getFa();

    assertThat(fa.getKodWaluty()).isEqualTo(TKodWaluty.EUR);
    assertThat(fa.getKursWalutyZ()).isEqualByComparingTo(new BigDecimal("4.2489"));
    // EUR amounts unchanged
    assertThat(fa.getP131()).isEqualByComparingTo(new BigDecimal("1000.00"));
    assertThat(fa.getP141()).isEqualByComparingTo(new BigDecimal("230.00"));
    assertThat(fa.getP15()).isEqualByComparingTo(new BigDecimal("1230.00"));
    // VAT-in-PLN twin: 230.00 * 4.2489 = 977.247 -> 977.25 (HALF_UP)
    assertThat(fa.getP141W()).isEqualByComparingTo(new BigDecimal("977.25"));
  }

  @Test
  void eur_invoice_emits_a_twin_per_taxed_band() {
    Faktura.Fa fa = InvoiceMapper.toFaktura(InvoiceFixtures.eurTwoTaxedBands()).getFa();

    // 23%: VAT 230.00 * 4 = 920.00 ; 8%: VAT 40.00 * 4 = 160.00
    assertThat(fa.getP141W()).isEqualByComparingTo(new BigDecimal("920.00"));
    assertThat(fa.getP142W()).isEqualByComparingTo(new BigDecimal("160.00"));
  }

  @Test
  void pln_invoice_emits_no_exchange_rate_and_no_twins() {
    Faktura.Fa fa = InvoiceMapper.toFaktura(InvoiceFixtures.singleLineVat23()).getFa();

    assertThat(fa.getKursWalutyZ()).isNull();
    assertThat(fa.getP141W()).isNull();
  }

  private static Invoice invoiceWithItem(Item item) {
    Invoice base = InvoiceFixtures.singleLineVat23();
    return new Invoice(
        base.invoiceNumber(),
        base.issueDate(),
        base.saleDate(),
        base.currency(),
        base.exchangeRate(),
        base.seller(),
        base.buyer(),
        List.of(item));
  }
}
