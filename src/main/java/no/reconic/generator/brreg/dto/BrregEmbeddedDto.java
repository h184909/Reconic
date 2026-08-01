package no.reconic.generator.brreg.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BrregEmbeddedDto(
        List<BrregCompanyDto> enheter,
        List<BrregCompanyDto> underenheter
) {
}
