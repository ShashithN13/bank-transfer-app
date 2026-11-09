package com.example.bankapp.service;

import com.example.bankapp.exception.AccountNotFoundException;
import com.example.bankapp.exception.BankException;
import com.example.bankapp.exception.InsufficientBalanceException;
import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.BankTransactionRepository;
import com.example.bankapp.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class BankService {

    private static final String BANK_ACCOUNT = "999999";
    private static final BigDecimal TRANSFER_FEE_RATE = new BigDecimal("0.01");

    private final CustomerRepository customerRepository;
    private final BankTransactionRepository transactionRepository;

    public BankService(CustomerRepository customerRepository,
                       BankTransactionRepository transactionRepository) {
        this.customerRepository = customerRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<Customer> getCustomers(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return customerRepository.findAllByOrderByFullName().stream()
                    .filter(customer -> !BANK_ACCOUNT.equals(customer.getAccountNumber()))
                    .toList();
        }
        return customerRepository.findByFullNameContainingIgnoreCaseOrderByFullName(keyword).stream()
                .filter(customer -> !BANK_ACCOUNT.equals(customer.getAccountNumber()))
                .toList();
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAllByOrderByFullName();
    }

    public List<BankTransaction> getRecentTransactions() {
        return transactionRepository.findTop10ByOrderByCreatedAtDesc();
    }

    public List<BankTransaction> getTransactionsForAccount(String accountNumber) {
        return transactionRepository.findByFromAccountOrToAccountOrderByCreatedAtDesc(
                accountNumber, accountNumber);
    }

    public List<BankTransaction> getLargeTransfers(BigDecimal minimum) {
        return transactionRepository.findByStatusAndAmountGreaterThanOrderByCreatedAtDesc(
                TransactionStatus.SUCCESS, minimum);
    }

    public Customer getCustomer(String accountNumber) {
        return customerRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    public long countCustomers() {
        return customerRepository.count() - (customerRepository.existsByAccountNumber(BANK_ACCOUNT) ? 1 : 0);
    }

    public BigDecimal getTotalBalance() {
        return getAllCustomers().stream()
                .filter(customer -> !BANK_ACCOUNT.equals(customer.getAccountNumber()))
                .map(Customer::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public long countTransactions(TransactionStatus status) {
        return transactionRepository.countByStatus(status);
    }

    public BigDecimal getTotalSent(String accountNumber) {
        BigDecimal total = transactionRepository.totalSentByAccount(accountNumber, TransactionStatus.SUCCESS);
        return total == null ? BigDecimal.ZERO : total;
    }

    @Transactional
    public void addCustomer(Customer customer) {
        if (customerRepository.existsByAccountNumber(customer.getAccountNumber())) {
            throw new BankException("Account number is already in use");
        }
        if (customerRepository.existsByEmail(customer.getEmail())) {
            throw new BankException("Email address is already in use");
        }
        customerRepository.save(customer);
    }

    @Transactional
    public void transfer(String fromAccount, String toAccount, BigDecimal amount, boolean simulateFailure) {
        validateAmount(amount);
        if (fromAccount.equals(toAccount)) {
            throw new BankException("Sender and receiver accounts must be different");
        }

        Customer sender = lockCustomer(fromAccount);
        Customer receiver = lockCustomer(toAccount);
        BigDecimal fee = amount.multiply(TRANSFER_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal debit = amount.add(fee);
        if (sender.getBalance().compareTo(debit) < 0) {
            throw new InsufficientBalanceException(fromAccount);
        }

        sender.setBalance(sender.getBalance().subtract(debit));
        if (simulateFailure) {
            throw new BankException("Simulated failure; transfer rolled back");
        }

        receiver.setBalance(receiver.getBalance().add(amount));
        Customer bank = lockCustomer(BANK_ACCOUNT);
        bank.setBalance(bank.getBalance().add(fee));
        transactionRepository.save(new BankTransaction(fromAccount, toAccount, amount,
                TransactionStatus.SUCCESS, "Transfer completed (1% fee: LKR " + fee + ")"));
    }

    @Transactional
    public void deposit(String accountNumber, BigDecimal amount) {
        validateAmount(amount);
        Customer customer = lockCustomer(accountNumber);
        customer.setBalance(customer.getBalance().add(amount));
        transactionRepository.save(new BankTransaction(null, accountNumber, amount,
                TransactionStatus.SUCCESS, "Cash deposit"));
    }

    @Transactional
    public void withdraw(String accountNumber, BigDecimal amount) {
        validateAmount(amount);
        Customer customer = lockCustomer(accountNumber);
        if (customer.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(accountNumber);
        }
        customer.setBalance(customer.getBalance().subtract(amount));
        transactionRepository.save(new BankTransaction(accountNumber, null, amount,
                TransactionStatus.SUCCESS, "Cash withdrawal"));
    }

    private Customer lockCustomer(String accountNumber) {
        return customerRepository.findWithLockingByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankException("Amount must be greater than zero");
        }
    }
}
