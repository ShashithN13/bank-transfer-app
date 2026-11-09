package com.example.bankapp.repository;

import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface BankTransactionRepository
        extends JpaRepository<BankTransaction, Long> {

    List<BankTransaction> findTop10ByOrderByCreatedAtDesc();

    List<BankTransaction> findByFromAccountOrToAccountOrderByCreatedAtDesc(
            String fromAccount, String toAccount);

    @Query("""
            select t
            from BankTransaction t
            where t.status = com.example.bankapp.model.TransactionStatus.SUCCESS
              and t.fromAccount is not null
              and t.toAccount is not null
              and t.amount > :minimum
            order by t.createdAt desc
            """)
    List<BankTransaction> findSuccessfulTransfersAbove(@Param("minimum") BigDecimal minimum);

    long countByStatus(TransactionStatus status);

    @Query("""
            select coalesce(sum(t.amount), 0)
            from BankTransaction t
            where t.fromAccount = :accountNumber
              and t.status = com.example.bankapp.model.TransactionStatus.SUCCESS
              and t.toAccount is not null
            """)
    BigDecimal totalSentByAccount(@Param("accountNumber") String accountNumber);
}