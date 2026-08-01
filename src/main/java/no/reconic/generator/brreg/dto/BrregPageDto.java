package no.reconic.generator.brreg.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BrregPageDto(
        Integer size,
        Long totalElements,
        Integer totalPages,
        Integer number
) {
}
