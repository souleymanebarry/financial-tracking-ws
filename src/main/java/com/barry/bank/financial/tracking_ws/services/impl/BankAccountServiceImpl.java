package com.barry.bank.financial.tracking_ws.services.impl;

import com.barry.bank.financial.tracking_ws.entities.BankAccount;
import com.barry.bank.financial.tracking_ws.entities.CurrentAccount;
import com.barry.bank.financial.tracking_ws.entities.Customer;
import com.barry.bank.financial.tracking_ws.entities.Operation;
import com.barry.bank.financial.tracking_ws.entities.SavingAccount;
import com.barry.bank.financial.tracking_ws.enums.OperationType;
import com.barry.bank.financial.tracking_ws.repositories.BankAccountRepository;
import com.barry.bank.financial.tracking_ws.repositories.CustomerRepository;
import com.barry.bank.financial.tracking_ws.repositories.OperationRepository;
import com.barry.bank.financial.tracking_ws.services.BankAccountService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.lang3.StringUtils;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static com.barry.bank.financial.tracking_ws.enums.AccountStatus.CREATED;
import static com.barry.bank.financial.tracking_ws.enums.OperationType.CREDIT;
import static com.barry.bank.financial.tracking_ws.enums.OperationType.DEBIT;

@Service
@Log4j2
@RequiredArgsConstructor
public class BankAccountServiceImpl implements BankAccountService {

    private final BankAccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final OperationRepository operationRepository;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String COUNTRY_CODE = "FR";
    private static final String CONTROL_KEY = "76";

    public CurrentAccount createCurrentAccount(CurrentAccount account, Customer customer){
        if (account.getOverDraft() == null) {
            log.warn("Attempt to create CurrentAccount without overDraft for CustomerID: {}", customer.getCustomerId());
            throw new IllegalStateException("CurrentAccount must have an overDraft");
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
    public SavingAccount createSavingAccount(SavingAccount account, Customer customer) {
        if (account.getInterestRate() == null) {
            log.warn("Attempt to create SavingAccount without InterestRate for CustomerID: {}", customer.getCustomerId());
            throw new IllegalStateException("SavingAccount must have an InterestRate");
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
    public void debitAccount(UUID accountId, BigDecimal amount, String description) {
        if (accountId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Invalid debit attempt: accountId= {}, withdrawalAmount= {}", accountId, amount);
            throw new IllegalArgumentException("AccountId must not be null and amount must greater than zero");
        }

        BankAccount account = getAccountById(accountId);

        if (account.getBalance().compareTo(amount) < 0) {
            log.error("Attempt to withDraw an amount greater than the balance: balance: {}, amount: {}",account.getBalance(), amount);
            throw new IllegalStateException("Insufficient balance for a debit transaction");
        }

        BigDecimal oldBalance = account.getBalance();
        BigDecimal newBalance = oldBalance.subtract(amount);

        updateAccountBalance(account, newBalance);
        recordOperation(amount, account, DEBIT, description);

        log.info("Debit Operation completed successfully for account: ID: {}, amountToWithDraw: {}, oldBalance: {} newBalance: {}, rib: {}",
                accountId, amount, oldBalance, newBalance, account.getRib());
    }

    @Override
    @Transactional
    public void creditAccount(UUID accountId, BigDecimal amount, String description) {
        if (accountId == null || amount == null) {
            log.warn("Attempt to use non-existent accountId or amount: accountId: {}, creditAmount: {}"
                    , accountId, amount);
            throw new IllegalArgumentException("AccountId or amount must not be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("Attempt to credit an amount less than or equal to zero: accountId: {}, amount: {}", accountId, amount);
            throw new IllegalArgumentException("Credit amount must be greater than zero");
        }
        BankAccount account = getAccountById(accountId);
        final BigDecimal oldBalance = account.getBalance();
        final BigDecimal newBalance = oldBalance.add(amount);
        updateAccountBalance(account, newBalance);
        recordOperation(amount, account, CREDIT, description);
        log.info("Credit Operation completed successfully for account: ID: {}, creditAmount: {}, oldBalance: {} newBalance: {}, rib: {}",
                accountId, amount, oldBalance, newBalance, account.getRib());
    }

    @Override
    @Transactional
    public void transferBetweenAccounts(UUID sourceAccountId, UUID destinationAccountId, BigDecimal amount) {
        if (sourceAccountId == null || destinationAccountId == null || amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Invalid transfer attempt: sourceAccountId: {}, destinationAccountId: {}, amount: {}",
                    sourceAccountId, destinationAccountId, amount);
            throw new IllegalArgumentException("Transfer amount must be greater than zero and account IDs must not be null");
        }
        BankAccount sourceAccount = getAccountById(sourceAccountId);
        BankAccount destinationAccount = getAccountById(destinationAccountId);
        ensureAccountsAreDifferent(sourceAccount, destinationAccount);
        String description = String.format("Transfer from %s to %s", sourceAccountId, destinationAccountId);
        debitAccount(sourceAccountId, amount, description);
        creditAccount(destinationAccountId, amount, description);
        log.info("Transfer completed successfully: from: {} to: {}, amount: {}",
                sourceAccountId, destinationAccountId, amount);
    }

    private void ensureAccountsAreDifferent(BankAccount sourceAccount, BankAccount destinationAccount) {
        if (sourceAccount.getAccountId().equals(destinationAccount.getAccountId())) {
            log.error("Attempt to transfer amount to the same Account: sourceAccountId: {}, destinationAccountId: {}",
                    sourceAccount.getAccountId(), destinationAccount.getAccountId());
            throw new IllegalStateException("Source account must be different from destination account");
        }
    }

    @Override
    public List<BankAccount> getAccountsPaginated(int page, int size) {
        validatePaginationParams(page, size);

        List<BankAccount>  accounts  = accountRepository.findAll(PageRequest.of(page, size)).getContent();
        log.info("Successfully retrieved {} bank accounts for page: {}, size: {}", accounts.size(), page, size);
        return accounts;
    }

    @Override
    public Page<Operation> getAccountOperationsPage(UUID accountId, int page, int size) {
        validatePaginationParams(page, size);
        validateAccountId(accountId);

        getAccountById(accountId);  // check if the account exist
        Page<Operation> operations = operationRepository
                        .findByAccount_AccountId(accountId, PageRequest.of(page, size, Sort.by("operationDate").descending()));
        log.info("Successfully retrieved operations for accountId: {} page: {}, size: {}", accountId, page, size);

        return operations;
    }

    @Override
    public List<Operation> getAccountTransactionHistory(UUID accountId) {
        validateAccountId(accountId);
        getAccountById(accountId); // check if the account exist

        List<Operation> operations = operationRepository.findByAccount_AccountId(
                accountId,
                Sort.by(Sort.Direction.DESC, "operationDate"));
        log.info("Successfully retrieved {} operations for accountId: {}", operations.size(), accountId);
        return operations;
    }

    @Override
    public BankAccount getAccountById(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> {
                    log.warn("Attempt to retrieve account with non-existent ID: {}", accountId);
                    return new IllegalArgumentException("Account not found with Id: " + accountId);
                });
    }

    @Override
    public List<BankAccount> getAccountsWithoutPaginations() {
        List<BankAccount> accountList = accountRepository.findAll();
        log.info("Successfully retrieved {} accounts from the db : ", accountList.size());
        return accountList;
    }

    private void validateAccountId(UUID accountId) {
        if (accountId == null) {
            log.warn("Attempt to use non-existent accountId");
            throw new IllegalArgumentException("AccountId must not be null");
        }
    }

    private void validatePaginationParams(int page, int size) {
        if (page < 0 || size <= 0) {
            log.warn("Page must be greater than or equal to zero, and  size must greater than zero. Page: {}, size: {}", page, size);
            throw new IllegalArgumentException("Page must be greater than or equal to zero, and size must be greater than zero");
        }
    }

    private void recordOperation(BigDecimal amount, BankAccount account, OperationType operationType, String description) {
        String defaultDescription = getDefaultDescription(operationType);
        Operation operation = Operation.builder()
                .account(account)
                .operationDate(LocalDateTime.now())
                .operationAmount(amount)
                .operationNumber(generateOperationNumber())
                .operationType(operationType)
                .description(StringUtils.isNotBlank(description) ? description : defaultDescription).build();
        operationRepository.save(operation);
    }

    private String getDefaultDescription(OperationType operationType) {
        return switch (operationType) {
            case DEBIT -> "Debit operation";
            case CREDIT -> "Credit operation";
        };
    }

    private void updateAccountBalance(BankAccount account, BigDecimal newBalance) {
        account.setBalance(newBalance);
        accountRepository.save(account);
    }

    private void attachAccountToCustomer(BankAccount account, Customer customer) {
        account.setCustomer(customer);
        customer.getBankAccounts().add(account); // cohérence bidirectionnelle
        account.setStatus(CREATED);
    }

    private void initializeDefaultValues(BankAccount account) {
        account.setBalance(Optional.ofNullable(account.getBalance()).orElse(BigDecimal.ZERO));
        account.setCreatedAt(Optional.ofNullable(account.getCreatedAt()).orElse(LocalDateTime.now()));
        account.setRib(Optional.ofNullable(account.getRib()).orElse(generateFakeRib()));
    }

    @SuppressWarnings("java:S2201")
    private void ensureCustomerExists(Customer customer) {
        final UUID customerID = customer.getCustomerId();
        customerRepository.findById(customerID)
                .orElseThrow(() -> {
                    log.warn("Attempt to retrieve customer with a non-existent ID: {}", customerID);
                    return new IllegalArgumentException("Customer not found with ID: " + customerID);
                });
    }

    private String generateOperationNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int n = RANDOM.nextInt(1_000_000); // 0 → 999999
        String randomPart = String.format("%06d", n);
        return "OP-" + datePart + "-" + randomPart;
    }

    /**
     * Génère un RIB pseudo-français unique.
     * Exemple : FR76 12345 67890 12345678901 34
     */
    private String generateFakeRib() {
        var random = ThreadLocalRandom.current();
        String rib;

        do {
            var bankCode = String.format("%05d", random.nextInt(10_000, 100_000));
            var branchCode = String.format("%05d", random.nextInt(10_000, 100_000));
            var accountNumber = String.format("%011d", random.nextLong(0, 100_000_000_000L));
            var ribKey = String.format("%02d", random.nextInt(0, 100));

            rib = "%s%s %s %s %s %s".formatted(COUNTRY_CODE, CONTROL_KEY, bankCode, branchCode, accountNumber, ribKey);
        } while (accountRepository.existsByRib(rib));

        return rib;
    }
}
