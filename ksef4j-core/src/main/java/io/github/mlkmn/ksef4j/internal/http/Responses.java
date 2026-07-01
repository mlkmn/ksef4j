package io.github.mlkmn.ksef4j.internal.http;

import java.math.BigDecimal;
import java.util.List;

/**
 * Internal: KSeF v2 response wire DTOs. Records map the JSON 1:1; datetimes stay as String (the
 * transport does not interpret them). Not supported API.
 */
public final class Responses {

  private Responses() {}

  public record Token(String token, String validUntil) {}

  public record Status(int code, String description, List<String> details) {}

  public record Challenge(String challenge, String timestamp, long timestampMs, String clientIp) {}

  public record AuthSubmit(Token authenticationToken, String referenceNumber) {}

  public record AuthStatus(Status status) {}

  public record InvoiceStatus(Status status) {}

  public record TokenPair(Token accessToken, Token refreshToken) {}

  public record AccessToken(Token accessToken) {}

  public record OpenSession(String referenceNumber, String validUntil) {}

  public record SendInvoice(String referenceNumber) {}

  public record UpoPage(
      String referenceNumber, String downloadUrl, String downloadUrlExpirationDate) {}

  public record Upo(List<UpoPage> pages) {}

  public record SessionStatus(
      Status status,
      Integer invoiceCount,
      Integer successfulInvoiceCount,
      Integer failedInvoiceCount,
      String validUntil,
      String dateCreated,
      String dateUpdated,
      Upo upo) {}

  // validFrom/validTo are carried for completeness but are NOT used for selection or expiry:
  // HttpCertificateResolver trusts the parsed certificate's own notBefore/notAfter instead.
  public record CertificateInfo(
      String certificate, List<String> usage, String validFrom, String validTo) {}

  // isTruncated is set by KSeF when the result set hit the 10000-record per-query cap.
  public record QueryMetadata(List<Entry> invoices, boolean hasMore, boolean isTruncated) {

    public record Entry(
        String ksefNumber,
        String invoiceNumber,
        String issueDate,
        String acquisitionDate,
        String permanentStorageDate,
        Party seller,
        Buyer buyer,
        BigDecimal netAmount,
        BigDecimal grossAmount,
        BigDecimal vatAmount,
        String currency,
        String invoiceType,
        FormCode formCode,
        String invoiceHash) {}

    public record Party(String nip, String name) {}

    public record Buyer(Identifier identifier, String name) {}

    public record Identifier(String type, String value) {}

    public record FormCode(String systemCode, String schemaVersion, String value) {}
  }
}
