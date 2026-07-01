package io.github.mlkmn.ksef4j.test.internal;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.extension.ResponseTransformerV2;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.util.List;

/**
 * Fills the UPO's SkrotDokumentu with the plaintext invoiceHash the client submitted on the most
 * recent send, so the client's UPO integrity check passes without the mock decrypting anything.
 */
public final class UpoEchoTransformer implements ResponseTransformerV2 {

  public static final String NAME = "ksef-upo-echo";
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final Admin admin;

  public UpoEchoTransformer(Admin admin) {
    this.admin = admin;
  }

  @Override
  public Response transform(Response response, ServeEvent serveEvent) {
    List<LoggedRequest> sends =
        admin
            .findRequestsMatching(
                postRequestedFor(
                        urlEqualTo("/sessions/online/" + KsefPayloads.SESSION_REF + "/invoices"))
                    .build())
            .getRequests();
    if (sends.isEmpty()) {
      return response;
    }
    String hash;
    try {
      JsonNode body = MAPPER.readTree(sends.get(sends.size() - 1).getBodyAsString());
      hash = body.path("invoiceHash").asText("");
    } catch (Exception e) {
      hash = "";
    }
    return Response.Builder.like(response).but().body(KsefPayloads.upoXml(hash)).build();
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public boolean applyGlobally() {
    return false;
  }
}
