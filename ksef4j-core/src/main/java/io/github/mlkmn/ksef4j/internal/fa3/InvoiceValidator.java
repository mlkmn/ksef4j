package io.github.mlkmn.ksef4j.internal.fa3;

import io.github.mlkmn.ksef4j.error.InvoiceValidationException;
import io.github.mlkmn.ksef4j.internal.fa3.generated.TKodWaluty;
import io.github.mlkmn.ksef4j.internal.fa3.generated.etd.TKodKraju;
import io.github.mlkmn.ksef4j.invoice.Address;
import io.github.mlkmn.ksef4j.invoice.Buyer;
import io.github.mlkmn.ksef4j.invoice.Invoice;
import io.github.mlkmn.ksef4j.invoice.Item;
import io.github.mlkmn.ksef4j.invoice.Seller;
import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates an {@link Invoice} against a curated list of FA(3)-derived constraints. Pure function,
 * stateless, thread-safe. Fails fast on the first violation by throwing {@link
 * InvoiceValidationException} with a message naming the offending field and value.
 *
 * <p>Rules cite the FA(3) v1-0E simple type they derive from. This is the single validation gate
 * for both {@link InvoiceMapper} and any future invoice builder: currency and country codes are
 * checked against {@code TKodWaluty.fromValue} / {@code TKodKraju.fromValue}, item lists must be
 * non-empty, and addresses must be present.
 */
public final class InvoiceValidator {

  private static final Pattern NIP_PATTERN = Pattern.compile("^\\d{10}$");
  private static final int MAX_512 = 512; // tns:TZnakowy512
  private static final int MAX_256 = 256; // tns:TZnakowy
  private static final int MAX_50 = 50; // tns:TZnakowy50

  private InvoiceValidator() {}

  public static void validate(Invoice invoice) {
    checkInvoiceNumber(invoice.invoiceNumber());
    checkCurrencyAndRate(invoice.currency(), invoice.exchangeRate());
    checkSeller(invoice.seller());
    checkBuyer(invoice.buyer());
    List<Item> items = invoice.items();
    if (items.isEmpty()) {
      throw fail("Invoice must have at least one item");
    }
    for (int i = 0; i < items.size(); i++) {
      checkItem(i + 1, items.get(i));
    }
  }

  private static void checkCurrencyAndRate(String currency, BigDecimal exchangeRate) {
    try {
      TKodWaluty.fromValue(currency);
    } catch (IllegalArgumentException e) {
      throw fail("Unsupported currency code '" + currency + "'");
    }
    boolean pln = "PLN".equals(currency);
    if (pln) {
      if (exchangeRate != null) {
        throw fail("PLN invoices must not carry an exchange rate, got " + exchangeRate);
      }
      return;
    }
    if (exchangeRate == null || exchangeRate.signum() <= 0) {
      throw fail(
          "Invoice in currency "
              + currency
              + " requires a strictly positive exchange rate, got "
              + exchangeRate);
    }
  }

  private static void checkInvoiceNumber(String invoiceNumber) {
    if (invoiceNumber == null || invoiceNumber.isBlank()) {
      throw fail("Invoice number must not be blank");
    }
    if (invoiceNumber.length() > MAX_256) {
      throw fail(
          "Invoice number must be at most "
              + MAX_256
              + " characters, got "
              + invoiceNumber.length());
    }
  }

  private static void checkSeller(Seller seller) {
    if (seller == null) {
      throw fail("Seller is required");
    }
    checkNip("Seller NIP", seller.nip());
    checkName("Seller name", seller.name());
    checkAddress("Seller address", seller.address());
  }

  private static void checkBuyer(Buyer buyer) {
    if (buyer == null) {
      throw fail("Buyer is required");
    }
    checkNip("Buyer NIP", buyer.nip());
    checkName("Buyer name", buyer.name());
    checkAddress("Buyer address", buyer.address());
  }

  private static void checkNip(String label, String nip) {
    if (nip == null || !NIP_PATTERN.matcher(nip).matches()) {
      throw fail(label + " must be exactly 10 digits, got '" + nip + "'");
    }
  }

  private static void checkName(String label, String name) {
    if (name == null || name.isBlank()) {
      throw fail(label + " must not be blank");
    }
    if (name.length() > MAX_512) {
      throw fail(label + " must be at most " + MAX_512 + " characters, got " + name.length());
    }
  }

  private static void checkAddress(String label, Address address) {
    if (address == null) {
      throw fail(label + " is required");
    }
    try {
      TKodKraju.fromValue(address.countryCode());
    } catch (IllegalArgumentException e) {
      throw fail(label + " has unsupported country code '" + address.countryCode() + "'");
    }
    if (address.line1() == null || address.line1().isBlank()) {
      throw fail(label + " line 1 must not be blank");
    }
    if (address.line1().length() > MAX_512) {
      throw fail(
          label
              + " line 1 must be at most "
              + MAX_512
              + " characters, got "
              + address.line1().length());
    }
    if (address.line2() != null && address.line2().length() > MAX_512) {
      throw fail(
          label
              + " line 2 must be at most "
              + MAX_512
              + " characters, got "
              + address.line2().length());
    }
  }

  private static void checkItem(int rowNumber, Item item) {
    String prefix = "Item " + rowNumber;
    if (item.vatRate() == null) {
      throw fail(prefix + " VAT rate must not be null");
    }
    if (item.description() == null || item.description().isBlank()) {
      throw fail(prefix + " description must not be blank");
    }
    if (item.description().length() > MAX_512) {
      throw fail(
          prefix
              + " description must be at most "
              + MAX_512
              + " characters, got "
              + item.description().length());
    }
    if (item.quantity() == null || item.quantity().signum() <= 0) {
      throw fail(prefix + " quantity must be strictly positive, got " + item.quantity());
    }
    if (item.unitPrice() == null || item.unitPrice().signum() < 0) {
      throw fail(prefix + " unit price must be non-negative, got " + item.unitPrice());
    }
    if (item.unit() != null && item.unit().length() > MAX_256) {
      throw fail(
          prefix + " unit must be at most " + MAX_256 + " characters, got " + item.unit().length());
    }
    if (item.pkwiu() != null && item.pkwiu().length() > MAX_50) {
      throw fail(
          prefix
              + " PKWiU must be at most "
              + MAX_50
              + " characters, got "
              + item.pkwiu().length());
    }
  }

  private static InvoiceValidationException fail(String message) {
    return new InvoiceValidationException(message);
  }
}
