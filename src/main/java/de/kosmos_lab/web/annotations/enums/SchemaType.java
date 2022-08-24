package de.kosmos_lab.web.annotations.enums;

public enum SchemaType {
    DEFAULT(""),
    STRING("string"),
    ARRAY("array"),
    OBJECT("object"),
    INTEGER("integer"),
    BOOLEAN("boolean"),
    NUMBER("number");

    private String value;

    private SchemaType(String value) {
        this.value = value;
    }

    public String toString() {
        return this.value;
    }
    public int length() {
        return this.value.length();
    }
}