package com.example.demologin.config;

import io.github.cdimascio.dotenv.Dotenv;

public class DotenvLoader {
    public static void loadEnv() {
        Dotenv dotenv = Dotenv.configure()
                .filename(".env") // optional, default is .env
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .load();
        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
        });
    }
}
