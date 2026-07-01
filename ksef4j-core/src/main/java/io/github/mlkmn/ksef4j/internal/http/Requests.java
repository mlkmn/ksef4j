package io.github.mlkmn.ksef4j.internal.http;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.mlkmn.ksef4j.internal.crypto.EncryptedInvoice;
import io.github.mlkmn.ksef4j.query.InvoiceQuery;
import io.github.mlkmn.ksef4j.query.SubjectRole;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;

/**
 * Internal: KSeF v2 request wire DTOs. The static factories perform the Base64 mapping from the
 * crypto layer's neutral byte outputs. Not supported API.
 *
 * <p>Kept in the same package as the sibling {@link Responses}, whose {@code QueryMetadata} shape
 * is deliberately coupled to {@code ksef4j-test}'s {@code QueryWire} (a compile-time anti-drift
 * decision, see PR #28); these request DTOs have no such external dependent today but should be
 * refactored with the same care given the shared package boundary.
 */
public final class Requests {

  private Requests() {}

  private static final Base64.Encoder B64 = Base64.getEncoder();

  public record ContextIdentifier(String type, String value) {}

  public record KsefTokenAuth(
      String challenge, ContextIdentifier contextIdentifier, String encryptedToken) {}

  public record FormCode(String systemCode, String schemaVersion, String value) {}

  public record Encryption(String encryptedSymmetricKey, String initializationVector) {}

  public record OpenSession(FormCode formCode, Encryption encryption) {
    public static OpenSession from(byte[] wrappedKey, byte[] iv, FormCode formCode) {
      return new OpenSession(
          formCode, new Encryption(B64.encodeToString(wrappedKey), B64.encodeToString(iv)));
    }
  }

  public record SendInvoice(
      String invoiceHash,
      long invoiceSize,
      String encryptedInvoiceHash,
      long encryptedInvoiceSize,
      String encryptedInvoiceContent,
      boolean offlineMode) {

    public static SendInvoice from(EncryptedInvoice invoice) {
      return new SendInvoice(
          B64.encodeToString(invoice.plaintextSha256()),
          invoice.plaintextSize(),
          B64.encodeToString(invoice.ciphertextSha256()),
          invoice.ciphertextSize(),
          B64.encodeToString(invoice.ciphertext()),
          false);
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record QueryMetadata(
      @JsonProperty("subjectType") String subjectType,
      @JsonProperty("dateRange") DateRange dateRange,
      @JsonProperty("sellerNip") String sellerNip,
      @JsonProperty("buyerIdentifier") BuyerIdentifier buyerIdentifier,
      @JsonProperty("ksefNumber") String ksefNumber,
      @JsonProperty("invoiceNumber") String invoiceNumber,
      @JsonProperty("currencyCodes") List<String> currencyCodes,
      @JsonProperty("invoiceTypes") List<String> invoiceTypes) {

    public record DateRange(
        @JsonProperty("dateType") String dateType,
        @JsonProperty("from") String from,
        @JsonProperty("to") String to) {}

    public record BuyerIdentifier(
        @JsonProperty("type") String type, @JsonProperty("value") String value) {}

    public static QueryMetadata from(InvoiceQuery q) {
      String subject =
          switch (q.role()) {
            case SELLER -> "Subject1";
            case BUYER -> "Subject2";
          };
      String dateType =
          switch (q.dateType()) {
            case ISSUE -> "Issue";
            case INVOICING -> "Invoicing";
            case PERMANENT_STORAGE -> "PermanentStorage";
          };
      DateRange range =
          new DateRange(
              dateType,
              q.from().atStartOfDay(ZoneOffset.UTC).toInstant().toString(),
              q.to().atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC).toInstant().toString());
      // The counterparty is the other party relative to the caller's role:
      // as seller, filter by the buyer; as buyer, filter by the seller.
      String sellerNip = q.role() == SubjectRole.BUYER ? q.counterpartyNip() : null;
      BuyerIdentifier buyerId =
          (q.role() == SubjectRole.SELLER && q.counterpartyNip() != null)
              ? new BuyerIdentifier("Nip", q.counterpartyNip())
              : null;
      List<String> currencies = q.currency() == null ? null : List.of(q.currency());
      List<String> types = q.invoiceType() == null ? null : List.of(q.invoiceType());
      return new QueryMetadata(
          subject, range, sellerNip, buyerId, q.ksefNumber(), q.invoiceNumber(), currencies, types);
    }
  }
}
