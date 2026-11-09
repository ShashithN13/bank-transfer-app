package com.example.bankapp.config;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.bankapp.model.Customer;
import com.example.bankapp.repository.CustomerRepository;

@Component
public class DataLoader implements CommandLineRunner {

    private final CustomerRepository customerRepository;

    public DataLoader(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public void run(String... args) {
        if (customerRepository.count() != 0) {
            return;
        }

        customerRepository.saveAll(List.of(
                new Customer("Nuwan Jayasinghe", "nuwan@example.com", "100001", new BigDecimal("50000.00")),
                new Customer("Amaya Perera", "amaya@example.com", "100002", new BigDecimal("25000.00")),
                new Customer("Kavindu Silva", "kavindu@example.com", "100003", new BigDecimal("10000.00")),
                new Customer("Bank Fees", "bank@example.com", "999999", BigDecimal.ZERO)
        ));
    }
}
