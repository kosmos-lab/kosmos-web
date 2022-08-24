package de.kosmos_lab.web.annotations.enums;

public enum SecurityIn {
    NONE(""),

    HEADER("header"),
    QUERY("query"),
    COOKIE("cookie");

    private String value;

    private SecurityIn(String value) {
        this.value = value;
    }

    public String toString() {
        return String.valueOf(this.value);
    }
}