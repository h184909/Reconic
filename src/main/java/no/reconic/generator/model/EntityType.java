package no.reconic.generator.model;

public enum EntityType {
    MAIN_UNIT("Hovedenhet"),
    SUBUNIT("Underenhet");

    private final String displayName;

    EntityType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
