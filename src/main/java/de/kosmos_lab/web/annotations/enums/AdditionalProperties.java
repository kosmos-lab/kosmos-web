package de.kosmos_lab.web.annotations.enums;

public enum AdditionalProperties {
    DEFAULT(""),
    TRUE("true"),
    FALSE("false");


    private String value;

    private AdditionalProperties(String value) {
        this.value = value;
    }

    public String toString() {
        return this.value;
    }
    public int length() {
        return this.value.length();
    }
}