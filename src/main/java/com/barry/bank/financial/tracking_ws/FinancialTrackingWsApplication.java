package com.barry.bank.financial.tracking_ws;

import com.barry.bank.financial.tracking_ws.entities.*;
import com.barry.bank.financial.tracking_ws.enums.AccountStatus;
import com.barry.bank.financial.tracking_ws.enums.OperationType;
import com.barry.bank.financial.tracking_ws.repositories.BankAccountRepository;
import com.barry.bank.financial.tracking_ws.repositories.CustomerRepository;
import com.barry.bank.financial.tracking_ws.repositories.OperationRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import static com.barry.bank.financial.tracking_ws.enums.Gender.FEMALE;
import static com.barry.bank.financial.tracking_ws.enums.Gender.MALE;

@SpringBootApplication
public class FinancialTrackingWsApplication {

	public static void main(String[] args) {
		SpringApplication.run(FinancialTrackingWsApplication.class, args);
	}

		//@Bean
		CommandLineRunner start(
				CustomerRepository customerRepository,
				BankAccountRepository accountRepository,
				OperationRepository operationRepository) {
			return args -> {

				Random random = new Random();

				// ------------------------
				// 🔹 1. Création de 60 clients
				// ------------------------
				List<Customer> customers = generateCustomers(60);
				customerRepository.saveAll(customers);
				System.out.println("✅ Clients enregistrés : " + customers.size());

				// ------------------------
				// 🔹 2. Comptes (1 courant + 1 épargne) par client
				// ------------------------
				List<BankAccount> allAccounts = new ArrayList<>();

				customers.forEach(customer -> {

					// --- Compte courant
					CurrentAccount currentAccount = new CurrentAccount();
					currentAccount.setRib(generateFakeRib());
					currentAccount.setBalance(BigDecimal.valueOf(Math.random() * 8000 + 1000));
					currentAccount.setStatus(AccountStatus.CREATED);
					currentAccount.setOverDraft(BigDecimal.valueOf(1500));
					currentAccount.setCustomer(customer);

					// Date de création entre maintenant et 8 mois avant
					LocalDateTime createdAtCurrent = LocalDateTime.now().minusDays(random.nextInt(240));
					currentAccount.setCreatedAt(createdAtCurrent);

					accountRepository.save(currentAccount);
					allAccounts.add(currentAccount);

					// --- Compte épargne
					SavingAccount savingAccount = new SavingAccount();
					savingAccount.setRib(generateFakeRib());
					savingAccount.setBalance(BigDecimal.valueOf(Math.random() * 20000 + 3000));
					savingAccount.setStatus(AccountStatus.CREATED);
					savingAccount.setInterestRate(BigDecimal.valueOf(2.75));
					savingAccount.setCustomer(customer);

					LocalDateTime createdAtSaving = LocalDateTime.now().minusDays(random.nextInt(240));
					savingAccount.setCreatedAt(createdAtSaving);

					accountRepository.save(savingAccount);
					allAccounts.add(savingAccount);

					System.out.printf("💰 Comptes créés pour %s %s [%s, %s]%n",
							customer.getFirstName(), customer.getLastName(),
							currentAccount.getRib(), savingAccount.getRib());
				});

				// ------------------------
				// 🔹 3. 20 opérations aléatoires par compte
				// ------------------------
				allAccounts.forEach(account -> {
					LocalDateTime createdAt = account.getCreatedAt();

					IntStream.range(0, 20).forEach(i -> {
						Operation operation = new Operation();
						operation.setOperationNumber(generateOperationNumber());
						operation.setOperationAmount(BigDecimal.valueOf(Math.random() * 1500 + 50));

						OperationType type = Math.random() > 0.5 ? OperationType.CREDIT : OperationType.DEBIT;
						operation.setOperationType(type);
						operation.setDescription(type == OperationType.CREDIT
								? "Versement salaire"
								: "Achat ou retrait");

						// Génère une date d’opération entre la date de création du compte et aujourd’hui
						long daysBetween = ChronoUnit.DAYS.between(createdAt, LocalDateTime.now());
						LocalDateTime operationDate = createdAt.plusDays(random.nextInt((int) Math.max(daysBetween, 1)));
						operation.setOperationDate(operationDate);

						operation.setAccount(account);
						operationRepository.save(operation);
					});
				});

				System.out.printf("✅ Données initialisées : %d clients, %d comptes, %d opérations%n",
						customers.size(), allAccounts.size(), allAccounts.size() * 20);
			};
		}

		private List<Customer> generateCustomers(int count) {
			List<String> firstNames = List.of("John", "Jane", "Alice", "Robert", "Maria", "David", "Emma", "Liam",
					"Olivia", "Noah", "Sophia", "Ethan", "Isabella", "Mason", "Mia", "James", "Amelia", "Benjamin",
					"Charlotte", "Lucas", "Ava", "Logan", "Harper", "Elijah", "Ella", "Jacob", "Emily", "Michael", "Abigail", "William");

			List<String> lastNames = List.of("Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller",
					"Davis", "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson",
					"Thomas", "Taylor", "Moore", "Jackson", "Martin", "Lee", "Perez", "Thompson", "White",
					"Harris", "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson");

			Random random = new Random();
			List<Customer> customers = new ArrayList<>();

			for (int i = 0; i < count; i++) {
				String firstName = firstNames.get(random.nextInt(firstNames.size()));
				String lastName = lastNames.get(random.nextInt(lastNames.size()));
				String email = (firstName + "." + lastName + "@mail.com").toLowerCase();

				customers.add(new Customer(
						null,
						firstName,
						lastName,
						email,
						random.nextBoolean() ? MALE : FEMALE,
						new ArrayList<>()
				));
			}
			return customers;
		}

		private String generateFakeRib() {
			String bankCode = String.format("%04d", (int) (Math.random() * 9000 + 1000));
			String branchCode = String.format("%04d", (int) (Math.random() * 9000 + 1000));
			String accountNumber = String.format("%011d", (long) (Math.random() * 1_000_000_00000L));
			return "FR76 " + bankCode + " " + branchCode + " " +
					accountNumber.substring(0, 4) + " " +
					accountNumber.substring(4, 8) + " " +
					accountNumber.substring(8);
		}

		private String generateOperationNumber() {
			String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
			String randomPart = String.format("%06d", (int) (Math.random() * 999999));
			return "OP-" + datePart + "-" + randomPart;
		}


	}
