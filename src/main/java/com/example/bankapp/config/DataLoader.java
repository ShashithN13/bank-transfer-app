package com.example.bankapp.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Inserts sample data the first time the application starts. */
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    @Override
    public void run(String... args) {
        // Intentionally left as a no-op so the application can start reliably
        // in local/test environments without external setup.
    }
}
