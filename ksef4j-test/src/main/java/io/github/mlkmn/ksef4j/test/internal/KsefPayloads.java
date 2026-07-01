package io.github.mlkmn.ksef4j.test.internal;

/** Canned KSeF response bodies for the mock's default happy path. Not part of the public API. */
public final class KsefPayloads {

  private KsefPayloads() {}

  public static final String VALID_UNTIL = "2027-01-01T00:00:00Z";

  public static final String SESSION_REF = "SESS1";
  public static final String INVOICE_REF = "INV1";
  public static final String UPO_PAGE_REF = "U1";
  public static final String KSEF_NUMBER = "5260250274-20260629-010001234567-AB";
  public static final String UPO_ISSUED_AT = "2026-06-29T10:15:30Z";

  /** Canned FA(3) body returned by the mock's default single-invoice download stub. */
  public static final String DOWNLOADED_INVOICE_XML =
      "<Faktura xmlns=\"http://crd.gov.pl/wzor/2023/06/29/12648/\"><Naglowek/></Faktura>";

  public static final String OPEN_SESSION =
      "{\"referenceNumber\":\"" + SESSION_REF + "\",\"validUntil\":\"" + VALID_UNTIL + "\"}";
  public static final String SEND_INVOICE_ACCEPTED =
      "{\"referenceNumber\":\"" + INVOICE_REF + "\"}";

  public static String sessionInProgress() {
    return "{\"status\":{\"code\":200,\"description\":\"in progress\",\"details\":[]},"
        + "\"invoiceCount\":0,\"upo\":{\"pages\":[]}}";
  }

  public static String sessionUpoReady(String upoDownloadUrl) {
    return "{\"status\":{\"code\":200,\"description\":\"ok\",\"details\":[]},\"invoiceCount\":1,"
        + "\"upo\":{\"pages\":[{\"referenceNumber\":\""
        + UPO_PAGE_REF
        + "\",\"downloadUrl\":\""
        + upoDownloadUrl
        + "\",\"downloadUrlExpirationDate\":\""
        + VALID_UNTIL
        + "\"}]}}";
  }

  /** UPO XML (v4-3) with the SkrotDokumentu placeholder filled by the echo transformer. */
  public static String upoXml(String documentHashBase64) {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<Potwierdzenie xmlns=\"http://upo.schematy.mf.gov.pl/KSeF/v4-3\">\n"
        + "  <Dokument>\n"
        + "    <NumerKSeFDokumentu>"
        + KSEF_NUMBER
        + "</NumerKSeFDokumentu>\n"
        + "    <NumerFaktury>FV/MOCK/001</NumerFaktury>\n"
        + "    <DataNadaniaNumeruKSeF>"
        + UPO_ISSUED_AT
        + "</DataNadaniaNumeruKSeF>\n"
        + "    <SkrotDokumentu>"
        + documentHashBase64
        + "</SkrotDokumentu>\n"
        + "  </Dokument>\n"
        + "</Potwierdzenie>\n";
  }

  public static String invoiceAccepted() {
    return "{\"status\":{\"code\":200,\"description\":\"ok\",\"details\":[]}}";
  }

  /** KSeF business-error envelope as returned by rejected API calls. */
  public static String errorEnvelope(int code, String description) {
    return "{\"exception\":{\"exceptionDetailList\":[{\"exceptionCode\":"
        + code
        + ",\"exceptionDescription\":\""
        + description
        + "\",\"details\":[]}],"
        + "\"serviceCode\":\"mock\",\"timestamp\":\"2026-06-29T10:00:00Z\"}}";
  }

  public static final String CHALLENGE =
      "{\"challenge\":\"abc\",\"timestamp\":\"2026-06-29T10:00:00Z\","
          + "\"timestampMs\":1782000000000,\"clientIp\":\"1.2.3.4\"}";

  public static final String KSEF_TOKEN =
      "{\"authenticationToken\":{\"token\":\"AUTHJWT\",\"validUntil\":\""
          + VALID_UNTIL
          + "\"},"
          + "\"referenceNumber\":\"REF1\"}";

  public static final String AUTH_STATUS_OK =
      "{\"status\":{\"code\":200,\"description\":\"ok\",\"details\":[]}}";

  public static final String TOKEN_REDEEM =
      "{\"accessToken\":{\"token\":\"ACC\",\"validUntil\":\""
          + VALID_UNTIL
          + "\"},"
          + "\"refreshToken\":{\"token\":\"REF\",\"validUntil\":\""
          + VALID_UNTIL
          + "\"}}";
}
