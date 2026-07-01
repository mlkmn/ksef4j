package io.github.mlkmn.ksef4j.internal.fa3;

import io.github.mlkmn.ksef4j.error.UnsupportedInvoiceFeatureException;
import io.github.mlkmn.ksef4j.internal.fa3.generated.Faktura;
import io.github.mlkmn.ksef4j.internal.fa3.generated.TAdres;
import io.github.mlkmn.ksef4j.internal.fa3.generated.TKodFormularza;
import io.github.mlkmn.ksef4j.internal.fa3.generated.TKodWaluty;
import io.github.mlkmn.ksef4j.internal.fa3.generated.TNaglowek;
import io.github.mlkmn.ksef4j.internal.fa3.generated.TPodmiot1;
import io.github.mlkmn.ksef4j.internal.fa3.generated.TPodmiot2;
import io.github.mlkmn.ksef4j.internal.fa3.generated.TRodzajFaktury;
import io.github.mlkmn.ksef4j.internal.fa3.generated.etd.TKodKraju;
import io.github.mlkmn.ksef4j.invoice.Address;
import io.github.mlkmn.ksef4j.invoice.Buyer;
import io.github.mlkmn.ksef4j.invoice.Invoice;
import io.github.mlkmn.ksef4j.invoice.Item;
import io.github.mlkmn.ksef4j.invoice.Seller;
import io.github.mlkmn.ksef4j.invoice.VatRate;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.Map;

/**
 * Pure-function mapper translating the English-named {@link Invoice} DTO to a
 * JAXB-generated FA(3) {@link Faktura} tree. v0.1 allowlist only: domestic VAT
 * invoice, one seller and one buyer (NIP + name), and VAT rates 0/5/8/23 per
 * line. Totals are computed at scale 2 with {@link RoundingMode#HALF_UP}.
 *
 * <p>The mapper rejects empty item lists with
 * {@link UnsupportedInvoiceFeatureException}. Stateless; thread-safe.
 */
public final class InvoiceMapper {

    private static final String SYSTEM_INFO = "ksef4j 0.1.0";
    private static final byte WARIANT_FORMULARZA = (byte) 3;
    private static final String UNIT_DEFAULT = "szt.";
    private static final RoundingMode RM = RoundingMode.HALF_UP;
    private static final int SCALE = 2;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    // FA(3) requires JST (Local Government Unit subordinate) and GV (VAT Group)
    // indicators on Podmiot2. v0.1 supports only standard B2B VAT invoices, so
    // both are fixed at "2" (no). To support JST or VAT Group invoices, model
    // these on the Buyer DTO and remove these constants.
    private static final BigInteger JST_NO = BigInteger.valueOf(2);
    private static final BigInteger GV_NO = BigInteger.valueOf(2);

    // FA(3) Adnotacje block: TWybor1_2 ("1" = yes, "2" = no), TWybor1 ("1" =
    // marker present). v0.1 invoices are plain domestic B2B VAT - no cash
    // method, no self-invoicing, no reverse charge, no split-payment, no
    // exemption, no new transport, no simplified procedure, no margin scheme.
    private static final byte WYBOR12_NO = (byte) 2;
    private static final Byte WYBOR1_PRESENT = (byte) 1;

    private static final DatatypeFactory DTF;

    static {
        try {
            DTF = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException("Unable to initialize DatatypeFactory", e);
        }
    }

    private InvoiceMapper() {}

    /** Translate {@code invoice} into the FA(3) {@link Faktura} JAXB tree. */
    public static Faktura toFaktura(Invoice invoice) {
        if (invoice.items().isEmpty()) {
            throw new UnsupportedInvoiceFeatureException("Invoice must have at least one item");
        }

        Faktura faktura = new Faktura();
        faktura.setNaglowek(buildNaglowek());
        faktura.setPodmiot1(buildPodmiot1(invoice.seller()));
        faktura.setPodmiot2(buildPodmiot2(invoice.buyer()));
        faktura.setFa(buildFa(invoice));
        return faktura;
    }

    private static TNaglowek buildNaglowek() {
        TNaglowek naglowek = new TNaglowek();

        TNaglowek.KodFormularza kodFormularza = new TNaglowek.KodFormularza();
        kodFormularza.setValue(TKodFormularza.FA);
        kodFormularza.setKodSystemowy("FA (3)");
        kodFormularza.setWersjaSchemy("1-0E");
        naglowek.setKodFormularza(kodFormularza);

        naglowek.setWariantFormularza(WARIANT_FORMULARZA);
        naglowek.setDataWytworzeniaFa(
                DTF.newXMLGregorianCalendar(OffsetDateTime.now(ZoneOffset.UTC).toString()));
        naglowek.setSystemInfo(SYSTEM_INFO);
        return naglowek;
    }

    private static Faktura.Podmiot1 buildPodmiot1(Seller seller) {
        if (seller.address() == null) {
            throw new UnsupportedInvoiceFeatureException("Seller address is required");
        }
        TPodmiot1 dane = new TPodmiot1();
        dane.setNIP(seller.nip());
        dane.setNazwa(seller.name());

        Faktura.Podmiot1 podmiot = new Faktura.Podmiot1();
        podmiot.setDaneIdentyfikacyjne(dane);
        podmiot.setAdres(buildAdres(seller.address()));
        return podmiot;
    }

    private static Faktura.Podmiot2 buildPodmiot2(Buyer buyer) {
        if (buyer.address() == null) {
            throw new UnsupportedInvoiceFeatureException("Buyer address is required");
        }
        TPodmiot2 dane = new TPodmiot2();
        dane.setNIP(buyer.nip());
        dane.setNazwa(buyer.name());

        Faktura.Podmiot2 podmiot = new Faktura.Podmiot2();
        podmiot.setDaneIdentyfikacyjne(dane);
        podmiot.setAdres(buildAdres(buyer.address()));
        podmiot.setJST(JST_NO);
        podmiot.setGV(GV_NO);
        return podmiot;
    }

    private static TAdres buildAdres(Address address) {
        TAdres adres = new TAdres();
        adres.setKodKraju(TKodKraju.fromValue(address.countryCode()));
        adres.setAdresL1(address.line1());
        if (address.line2() != null) {
            adres.setAdresL2(address.line2());
        }
        if (address.gln() != null) {
            adres.setGLN(address.gln());
        }
        return adres;
    }

    private static Faktura.Fa buildFa(Invoice invoice) {
        Faktura.Fa fa = new Faktura.Fa();
        fa.setKodWaluty(TKodWaluty.fromValue(invoice.currency()));
        fa.setP1(toXmlDate(invoice.issueDate()));
        fa.setP2(invoice.invoiceNumber());
        fa.setP6(toXmlDate(invoice.saleDate()));
        fa.setRodzajFaktury(TRodzajFaktury.VAT);

        BigDecimal exchangeRate = invoice.exchangeRate();
        if (exchangeRate != null) {
            fa.setKursWalutyZ(exchangeRate);
        }

        // Per-rate net accumulators
        Map<VatRate, BigDecimal> netByRate = new EnumMap<>(VatRate.class);

        int rowNumber = 1;
        for (Item item : invoice.items()) {
            Faktura.Fa.FaWiersz row = new Faktura.Fa.FaWiersz();
            row.setNrWierszaFa(BigInteger.valueOf(rowNumber++));
            row.setP7(item.description());
            // P_8A is unit name, P_8B is quantity (per FA(3) schema)
            row.setP8A(unitOf(item));
            row.setP8B(item.quantity());
            row.setP9A(item.unitPrice());
            if (item.pkwiu() != null) {
                row.setPKWiU(item.pkwiu());
            }
            BigDecimal lineNet = item.quantity().multiply(item.unitPrice()).setScale(SCALE, RM);
            row.setP11(lineNet);
            row.setP12(p12CodeFor(item.vatRate()));
            fa.getFaWiersz().add(row);

            netByRate.merge(item.vatRate(), lineNet, BigDecimal::add);
        }

        // Per-rate totals (skip-if-zero / absent rule)
        BigDecimal totalGross = BigDecimal.ZERO;

        BigDecimal net23 = netByRate.get(VatRate.VAT_23);
        if (net23 != null) {
            BigDecimal n = net23.setScale(SCALE, RM);
            BigDecimal v = vatOf(n, 23);
            fa.setP131(n);
            fa.setP141(v);
            if (exchangeRate != null) {
                fa.setP141W(plnVat(v, exchangeRate));
            }
            totalGross = totalGross.add(n).add(v);
        }

        BigDecimal net8 = netByRate.get(VatRate.VAT_8);
        if (net8 != null) {
            BigDecimal n = net8.setScale(SCALE, RM);
            BigDecimal v = vatOf(n, 8);
            fa.setP132(n);
            fa.setP142(v);
            if (exchangeRate != null) {
                fa.setP142W(plnVat(v, exchangeRate));
            }
            totalGross = totalGross.add(n).add(v);
        }

        BigDecimal net5 = netByRate.get(VatRate.VAT_5);
        if (net5 != null) {
            BigDecimal n = net5.setScale(SCALE, RM);
            BigDecimal v = vatOf(n, 5);
            fa.setP133(n);
            fa.setP143(v);
            if (exchangeRate != null) {
                fa.setP143W(plnVat(v, exchangeRate));
            }
            totalGross = totalGross.add(n).add(v);
        }

        BigDecimal net0 = netByRate.get(VatRate.VAT_0);
        if (net0 != null) {
            BigDecimal n = net0.setScale(SCALE, RM);
            // P_13_6_1: sum of 0% sales (excluding intra-EU dispatch and exports,
            // which use P_13_6_2/P_13_6_3). P_13_4 looks superficially similar but
            // is the taxi-ryczalt rate that the schema pairs with a required
            // P_14_4 - using it for plain 0% sales violates the content model.
            fa.setP1361(n);
            totalGross = totalGross.add(n);
        }

        fa.setP15(totalGross.setScale(SCALE, RM));
        fa.setAdnotacje(buildAdnotacjeNone());
        return fa;
    }

    private static Faktura.Fa.Adnotacje buildAdnotacjeNone() {
        Faktura.Fa.Adnotacje adnotacje = new Faktura.Fa.Adnotacje();
        adnotacje.setP16(WYBOR12_NO);
        adnotacje.setP17(WYBOR12_NO);
        adnotacje.setP18(WYBOR12_NO);
        adnotacje.setP18A(WYBOR12_NO);

        Faktura.Fa.Adnotacje.Zwolnienie zwolnienie = new Faktura.Fa.Adnotacje.Zwolnienie();
        zwolnienie.setP19N(WYBOR1_PRESENT);
        adnotacje.setZwolnienie(zwolnienie);

        Faktura.Fa.Adnotacje.NoweSrodkiTransportu nst = new Faktura.Fa.Adnotacje.NoweSrodkiTransportu();
        nst.setP22N(WYBOR1_PRESENT);
        adnotacje.setNoweSrodkiTransportu(nst);

        adnotacje.setP23(WYBOR12_NO);

        Faktura.Fa.Adnotacje.PMarzy pMarzy = new Faktura.Fa.Adnotacje.PMarzy();
        pMarzy.setPPMarzyN(WYBOR1_PRESENT);
        adnotacje.setPMarzy(pMarzy);

        return adnotacje;
    }

    private static String unitOf(Item item) {
        String unit = item.unit();
        return (unit == null || unit.isBlank()) ? UNIT_DEFAULT : unit;
    }

    private static String p12CodeFor(VatRate rate) {
        // The XSD P_12 enumeration accepts numeric strings for non-zero rates
        // ("23", "8", "5") but distinguishes domestic vs intra-EU vs export
        // zero rates by suffix ("0 KR" / "0 WDT" / "0 EX"). v0.1 ships
        // domestic only, so 0% maps to "0 KR".
        return rate == VatRate.VAT_0 ? "0 KR" : Integer.toString(rate.percent());
    }

    private static BigDecimal vatOf(BigDecimal net, int percent) {
        return net.multiply(BigDecimal.valueOf(percent))
                .divide(HUNDRED, SCALE, RM);
    }

    private static BigDecimal plnVat(BigDecimal vat, BigDecimal exchangeRate) {
        return vat.multiply(exchangeRate).setScale(SCALE, RM);
    }

    private static XMLGregorianCalendar toXmlDate(LocalDate date) {
        return DTF.newXMLGregorianCalendarDate(
                date.getYear(),
                date.getMonthValue(),
                date.getDayOfMonth(),
                DatatypeConstants.FIELD_UNDEFINED);
    }
}
