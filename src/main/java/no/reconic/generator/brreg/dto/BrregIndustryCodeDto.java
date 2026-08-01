package no.reconic.generator.brreg.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BrregIndustryCodeDto(
        String kode,
        String beskrivelse
) {
}
