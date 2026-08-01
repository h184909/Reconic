package no.reconic.generator.model;

import java.util.Arrays;
import java.util.Optional;

public enum Municipality {
    STAVANGER("1103", "Stavanger"),
    SANDNES("1108", "Sandnes"),
    HA("1119", "Hå"),
    KLEPP("1120", "Klepp"),
    TIME("1121", "Time"),
    GJESDAL("1122", "Gjesdal"),
    SOLA("1124", "Sola"),
    RANDABERG("1127", "Randaberg");

    private final String number;
    private final String displayName;

    Municipality(String number, String displayName) {
        this.number = number;
        this.displayName = displayName;
    }

    public String getNumber() {
        return number;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Optional<Municipality> fromNumber(String number) {
        return Arrays.stream(values())
                .filter(municipality -> municipality.number.equals(number))
                .findFirst();
    }
}
