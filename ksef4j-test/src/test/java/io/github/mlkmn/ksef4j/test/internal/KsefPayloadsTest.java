package io.github.mlkmn.ksef4j.test.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class KsefPayloadsTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void errorEnvelope_with_quote_in_description_stays_valid_json() throws Exception {
    String json = KsefPayloads.errorEnvelope(21405, "bad \"quoted\" value");

    JsonNode root = mapper.readTree(json); // must not throw
    JsonNode detail = root.path("exception").path("exceptionDetailList").get(0);
    assertThat(detail.path("exceptionCode").asInt()).isEqualTo(21405);
    assertThat(detail.path("exceptionDescription").asText()).isEqualTo("bad \"quoted\" value");
  }

  @Test
  void errorEnvelope_simple_description_parses() {
    assertThatCode(
            () -> mapper.readTree(KsefPayloads.errorEnvelope(21301, "Authentication failure")))
        .doesNotThrowAnyException();
  }
}
