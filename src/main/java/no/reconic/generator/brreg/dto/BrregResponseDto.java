package no.reconic.generator.brreg.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BrregResponseDto(
        @JsonProperty("_embedded") BrregEmbeddedDto embedded,
        BrregPageDto page
) {
}
