package no.reconic.generator.web;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import no.reconic.generator.model.IndustrySegment;
import no.reconic.generator.model.Municipality;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LeadSearchForm {

    @Min(value = 5, message = "Minimum ansatte må være minst 5")
    @Max(value = 10_000, message = "Minimum ansatte er for høyt")
    private int minEmployees = 25;

    @Min(value = 5, message = "Maksimum ansatte må være minst 5")
    @Max(value = 10_000, message = "Maksimum ansatte er for høyt")
    private int maxEmployees = 120;

    @NotEmpty(message = "Velg minst én kommune")
    private List<String> municipalityNumbers = new ArrayList<>(
            Arrays.stream(Municipality.values()).map(Municipality::getNumber).toList()
    );

    @NotEmpty(message = "Velg minst ett bransjesegment")
    private List<String> segmentNames = new ArrayList<>(
            Arrays.stream(IndustrySegment.values()).map(Enum::name).toList()
    );

    private boolean includeSubunits;

    @AssertTrue(message = "Minimum ansatte kan ikke være høyere enn maksimum ansatte")
    public boolean isEmployeeRangeValid() {
        return minEmployees <= maxEmployees;
    }

    public int getMinEmployees() {
        return minEmployees;
    }

    public void setMinEmployees(int minEmployees) {
        this.minEmployees = minEmployees;
    }

    public int getMaxEmployees() {
        return maxEmployees;
    }

    public void setMaxEmployees(int maxEmployees) {
        this.maxEmployees = maxEmployees;
    }

    public List<String> getMunicipalityNumbers() {
        return municipalityNumbers;
    }

    public void setMunicipalityNumbers(List<String> municipalityNumbers) {
        this.municipalityNumbers = municipalityNumbers == null ? new ArrayList<>() : municipalityNumbers;
    }

    public List<String> getSegmentNames() {
        return segmentNames;
    }

    public void setSegmentNames(List<String> segmentNames) {
        this.segmentNames = segmentNames == null ? new ArrayList<>() : segmentNames;
    }

    public boolean isIncludeSubunits() {
        return includeSubunits;
    }

    public void setIncludeSubunits(boolean includeSubunits) {
        this.includeSubunits = includeSubunits;
    }
}
