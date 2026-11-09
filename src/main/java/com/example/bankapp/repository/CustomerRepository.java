package com.example.bankapp.repository;

import com.example.bankapp.model.Customer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    boolean existsByEmail(String email);

    List<Customer> findByFullNameContainingIgnoreCaseOrderByFullName(String keyword);

    List<Customer> findAllByOrderByFullName();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Customer> findWithLockingByAccountNumber(String accountNumber);
}
