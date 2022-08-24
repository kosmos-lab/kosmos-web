package de.kosmos_lab.web.annotations.enums;

public enum SecurityType {
    HTTP("http"),
    APIKEY("apiKey"),
    OAUTH2("oauth2"),
    OPENIDCONNECT("openIdConnect");
    private String value;

    private SecurityType(String value) {
        this.value = value;
    }

    public String toString() {
        return String.valueOf(this.value);
    }
}