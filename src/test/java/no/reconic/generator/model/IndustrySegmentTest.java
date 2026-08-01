package no.reconic.generator.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IndustrySegmentTest {

    @Test
    void mapsConstructionNaceCode() {
        assertThat(IndustrySegment.fromNaceCode("43.210"))
                .contains(IndustrySegment.CONSTRUCTION);
    }

    @Test
    void mapsAccountingNaceCode() {
        assertThat(IndustrySegment.fromNaceCode("69.201"))
                .contains(IndustrySegment.LEGAL_ACCOUNTING);
    }

    @Test
    void ignoresUnselectedIndustry() {
        assertThat(IndustrySegment.fromNaceCode("62.010"))
                .isEmpty();
    }
}
