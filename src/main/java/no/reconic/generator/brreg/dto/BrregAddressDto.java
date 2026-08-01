package no.reconic.generator.brreg.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BrregAddressDto(
        List<String> adresse,
        String postnummer,
        String poststed,
        String kommune,
        String kommunenummer,
        String land,
        String landkode
) {
}
