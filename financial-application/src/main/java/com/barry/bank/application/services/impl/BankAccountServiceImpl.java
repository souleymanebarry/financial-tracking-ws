package com.barry.bank.application.services.impl;

import com.barry.bank.application.services.BankAccountService;
import com.barry.bank.application.services.OperationService;
import com.barry.bank.domain.entities.BankAccount;
import com.barry.bank.domain.entities.CurrentAccount;
import com.barry.bank.domain.entities.Customer;
import com.barry.bank.domain.entities.Operation;
import com.barry.bank.domain.entities.SavingAccount;
import com.barry.bank.domain.exception.BusinessRuleException;
import com.barry.bank.domain.exception.ResourceNotFoundException;
import com.barry.bank.persistence.repositories.BankAccountRepository;
import com.barry.bank.persistence.repositories.CustomerRepository;
import com.barry.bank.persistence.repositories.OperationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static com.barry.bank.domain.entities.enums.AccountStatus.CREATED;


@Service
@Log4j2
@RequiredArgsConstructor
public class BankAccountServiceImpl implements BankAccountService {

    private final BankAccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final OperationRepository operationRepository;
    private final OperationService operationService;

    private static final String COUNTRY_CODE = "FR";
    private static final String CONTROL_KEY = "76";

    @Override
    @Transactional
    public CurrentAccount createCurrentAccount(CurrentAccount account, Customer customer){
        if (account.getOverDraft() == null) {
            log.warn("Attempt to create CurrentAccount without overDraft for CustomerID: {}", customer.getCustomerId());
            throw new BusinessRuleException("CurrentAccount must have an overDraft");
        }
        ensureCustomerExists(customer);
        initializeDefaultValues(account);
        attachAccountToCustomer(account, customer);
        CurrentAccount savedAccount = accountRepository.save(account);
        log.info("CurrentAccount saved successfully with ID: {}, accountHolderName: {}, rib: {}",
                savedAccount.getAccountId(), savedAccount.getCustomer().getLastName(), account.getRib());
        return savedAccount;
    }

    @Override
    @Transactional
    public SavingAccount createSavingAccount(SavingAccount account, Customer customer) {
        if (account.getInterestRate() == null) {
            log.warn("Attempt to create SavingAccount without InterestRate for CustomerID: {}", customer.getCustomerId());
            throw new BusinessRuleException("SavingAccount must have an InterestRate");
        }
        ensureCustomerExists(customer);
        initializeDefaultValues(account);
        attachAccountToCustomer(account, customer);
        SavingAccount savedAccount = accountRepository.save(account);
        log.info("SavingAccount saved successfully with ID: {}, accountHolderName: {}, rib: {}",
                savedAccount.getAccountId(), savedAccount.getCustomer().getLastName(), account.getRib());
        return savedAccount;
    }

    @Override
    @Transactional
    public void transferBetweenAccounts(UUID sourceAccountId, UUID destinationAccountId, BigDecimal amount) {
        if (sourceAccountId == null || destinationAccountId == null || amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Invalid transfer attempt: sourceAccountId={}, destinationAccountId={}, amount={}",
                    sourceAccountId, destinationAccountId, amount);
            throw new IllegalArgumentException("Transfer amount must be greater than zero and account IDs must not be null");
        }
        BankAccount sourceAccount = getAccountById(sourceAccountId);
        BankAccount destinationAccount = getAccountById(destinationAccountId);
        ensureAccountsAreDifferent(sourceAccount, destinationAccount);

        String description = String.format("Transfer from %s to %s", sourceAccountId, destinationAccountId);
        operationService.debitAccount(sourceAccountId, amount, description);
        operationService.creditAccount(destinationAccountId, amount, description);

        log.info("Transfer completed: from={}, to={}, amount={}", sourceAccountId, destinationAccountId, amount);
    }

    @Override
    public List<BankAccount> getAccounts(int page, int size) {
        List<BankAccount> accounts = accountRepository.findAll(PageRequest.of(page, size)).getContent();
        log.info("Successfully retrieved {} bank accounts for page={}, size={}", accounts.size(), page, size);
        return accounts;
    }

    @Override
    public Page<Operation> getAccountOperationsPage(UUID accountId, int page, int size) {
        validateAccountId(accountId);
        getAccountById(accountId);
        Page<Operation> operations = operationRepository
                .findByAccount_AccountId(accountId, PageRequest.of(page, size, Sort.by("operationDate").descending()));
        log.info("Successfully retrieved operations for accountId={}, page={}, size={}", accountId, page, size);
        return operations;
    }

    @Override
    public List<Operation> getAccountOperations(UUID accountId) {
        validateAccountId(accountId);
        getAccountById(accountId);
        List<Operation> operations = operationRepository.findByAccount_AccountId(
                accountId, Sort.by(Sort.Direction.DESC, "operationDate"));
        log.info("Successfully retrieved {} operations for accountId={}", operations.size(), accountId);
        return operations;
    }

    @Override
    public BankAccount getAccountById(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> {
                    log.warn("Account not found with ID: {}", accountId);
                    return new ResourceNotFoundException("Account not found with Id: " + accountId);
                });
    }

    @Override
    public List<BankAccount> getAllAccounts() {
        List<BankAccount> accountList = accountRepository.findAll();
        log.info("Successfully retrieved {} accounts", accountList.size());
        return accountList;
    }

    @Override
    @Transactional
    public void deleteAccountsByCustomer(UUID customerId) {
        // Bulk delete in FK-safe order: operations first, then accounts (2 constant queries, no N+1).
        operationRepository.deleteByCustomerId(customerId);
        accountRepository.deleteByCustomerId(customerId);
        log.info("Deleted accounts and their operations for customerId={}", customerId);
    }

    private void validateAccountId(UUID accountId) {
        if (accountId == null) {
            log.warn("Attempt to use non-existent accountId");
            throw new IllegalArgumentException("AccountId must not be null");
        }
    }

    private void ensureAccountsAreDifferent(BankAccount sourceAccount, BankAccount destinationAccount) {
        if (sourceAccount.getAccountId().equals(destinationAccount.getAccountId())) {
            log.error("Transfer to same account: sourceId={}", sourceAccount.getAccountId());
            throw new BusinessRuleException("Source account must be different from destination account");
        }
    }

    private void ensureCustomerExists(Customer customer) {
        final UUID customerId = customer.getCustomerId();
        if (!customerRepository.existsById(customerId)) {
            log.error("Customer not found with ID: {}", customerId);
            throw new ResourceNotFoundException("Customer not found with ID: " + customerId);
        }
    }

    private void attachAccountToCustomer(BankAccount account, Customer customer) {
        account.setCustomer(customer);
        customer.getBankAccounts().add(account);
        account.setStatus(CREATED);
    }

    private void initializeDefaultValues(BankAccount account) {
        account.setBalance(Optional.ofNullable(account.getBalance()).orElse(BigDecimal.ZERO));
        account.setCreatedAt(Optional.ofNullable(account.getCreatedAt()).orElse(LocalDateTime.now()));
        account.setRib(Optional.ofNullable(account.getRib()).orElse(generateFakeRib()));
    }

    /**
     * Génère un RIB pseudo-français unique.
     * Exemple : FR76 12345 67890 12345678901 34
     */
    private String generateFakeRib() {
        var random = ThreadLocalRandom.current();
        int maxAttempts = 10;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            var bankCode = String.format("%05d", random.nextInt(10_000, 100_000));
            var branchCode = String.format("%05d", random.nextInt(10_000, 100_000));
            var accountNumber = String.format("%011d", random.nextLong(0, 100_000_000_000L));
            var ribKey = String.format("%02d", random.nextInt(0, 100));
            String rib = "%s%s %s %s %s %s".formatted(COUNTRY_CODE, CONTROL_KEY, bankCode, branchCode, accountNumber, ribKey);
            if (!accountRepository.existsByRib(rib)) {
                return rib;
            }
            log.warn("RIB collision on attempt {}/{}", attempt, maxAttempts);
        }
        throw new IllegalStateException("Unable to generate a unique RIB after " + maxAttempts + " attempts");
    }
}
