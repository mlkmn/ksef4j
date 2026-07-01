package io.github.mlkmn.ksef4j.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class InvoiceMetadataPageTest {

  @Test
  void next_offset_is_page_offset_plus_item_count() {
    InvoiceMetadataPage page =
        new InvoiceMetadataPage(List.of(meta("A"), meta("B"), meta("C")), true, false, 10, 100);

    assertThat(page.nextOffset()).isEqualTo(13);
  }

  @Test
  void next_offset_on_empty_page_equals_page_offset() {
    InvoiceMetadataPage page = new InvoiceMetadataPage(List.of(), false, false, 50, 100);

    assertThat(page.nextOffset()).isEqualTo(50);
  }

  private static InvoiceMetadata meta(String ksef) {
    return new InvoiceMetadata(
        ksef, "FV/1", null, null, null, null, null, null, null, null, null, null, null);
  }
}
