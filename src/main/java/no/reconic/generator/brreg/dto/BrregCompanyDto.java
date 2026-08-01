package no.reconic.generator.brreg.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BrregCompanyDto(
        String organisasjonsnummer,
        String navn,
        Integer antallAnsatte,
        BrregIndustryCodeDto naeringskode1,
        BrregAddressDto forretningsadresse,
        BrregAddressDto beliggenhetsadresse,
        String hjemmeside,
        String epostadresse,
        String telefon,
        Boolean konkurs,
        Boolean underAvvikling,
        String slettedato,
        String overordnetEnhet
) {
}
