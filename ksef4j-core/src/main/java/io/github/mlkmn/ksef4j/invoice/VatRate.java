package io.github.mlkmn.ksef4j.invoice;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Polish VAT rates supported by v0.1: 0%, 5%, 8%, 23%. Maps to FA(3) {@code FaWiersz.P_12}.
 */
public enum VatRate {
    VAT_0(0),
    VAT_5(5),
    VAT_8(8),
    VAT_23(23);

    private final int percent;

    VatRate(int percent) {
        this.percent = percent;
    }

    public int percent() {
        return percent;
    }

    /** Look up by integer percent (0, 5, 8, 23). Throws if the percent is unsupported. */
    @JsonCreator
    public static VatRate ofPercent(int percent) {
        for (VatRate r : values()) {
            if (r.percent == percent) {
                return r;
            }
        }
        throw new IllegalArgumentException(
                "Unsupported VAT rate: " + percent + " (v0.1 supports 0, 5, 8, 23)");
    }
}
