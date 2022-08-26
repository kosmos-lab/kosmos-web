package de.kosmos_lab.web.server.example;

import java.io.File;

public class StartExampleWebServer {
    public static void main(String[] args) {
        try {
            new ExampleWebServer(new File("config/config.json"),false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
