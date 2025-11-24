package com.barry.bank.financial.tracking_ws;

import com.barry.bank.financial.tracking_ws.entities.BankAccount;
import com.barry.bank.financial.tracking_ws.entities.CurrentAccount;
import com.barry.bank.financial.tracking_ws.entities.Customer;
import com.barry.bank.financial.tracking_ws.entities.Operation;
import com.barry.bank.financial.tracking_ws.entities.SavingAccount;
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
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
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

			// -----------------------------------------------
			// 1️⃣ Création de 2 000 clients
			// -----------------------------------------------
			List<Customer> customers = generateCustomers(2000);
			customerRepository.saveAll(customers);
			System.out.println("✅ Clients enregistrés : " + customers.size());


			// -----------------------------------------------
			// 2️⃣ Comptes (1 courant + 1 épargne) par client
			// Dates entre 20 mai 2022 et 20 mai 2025
			// -----------------------------------------------
			LocalDateTime maxAccountDate = LocalDateTime.of(2025, 5, 20, 0, 0);
			LocalDateTime minAccountDate = maxAccountDate.minusYears(3);

			List<BankAccount> allAccounts = new ArrayList<>();

			customers.forEach(customer -> {

				// ---- Génération d'une date intelligente pour les comptes
				LocalDateTime createdAtCurrent = randomDate(minAccountDate, maxAccountDate, random);
				LocalDateTime createdAtSaving = randomDate(minAccountDate, maxAccountDate, random);

				// ---- Compte courant
				CurrentAccount currentAccount = new CurrentAccount();
				currentAccount.setRib(generateFakeRib());
				currentAccount.setBalance(BigDecimal.valueOf(Math.random() * 8000 + 1000));
				currentAccount.setStatus(AccountStatus.CREATED);
				currentAccount.setOverDraft(BigDecimal.valueOf(1500));
				currentAccount.setCustomer(customer);
				currentAccount.setCreatedAt(createdAtCurrent);

				accountRepository.save(currentAccount);
				allAccounts.add(currentAccount);

				// ---- Compte épargne
				SavingAccount savingAccount = new SavingAccount();
				savingAccount.setRib(generateFakeRib());
				savingAccount.setBalance(BigDecimal.valueOf(Math.random() * 20000 + 3000));
				savingAccount.setStatus(AccountStatus.CREATED);
				savingAccount.setInterestRate(BigDecimal.valueOf(2.75));
				savingAccount.setCustomer(customer);
				savingAccount.setCreatedAt(createdAtSaving);

				accountRepository.save(savingAccount);
				allAccounts.add(savingAccount);
			});

			System.out.println("💰 Comptes créés : " + allAccounts.size());


			// -----------------------------------------------
			// 3️⃣ 1000 opérations par compte
			// Dates entre 20 juin 2022 et 20 juin 2025
			// mais ≥ date de création du compte
			// -----------------------------------------------
			LocalDateTime maxOperationDate = LocalDateTime.of(2025, 6, 20, 0, 0);
			LocalDateTime minOperationDate = maxOperationDate.minusYears(3);

			allAccounts.forEach(account -> {

				IntStream.range(0, 1000).forEach(i -> {

					LocalDateTime opDate = randomDate(
							account.getCreatedAt().isAfter(minOperationDate)
									? account.getCreatedAt()
									: minOperationDate,
							maxOperationDate,
							random
					);

					Operation operation = new Operation();
					operation.setOperationNumber(generateOperationNumber());
					operation.setOperationAmount(BigDecimal.valueOf(Math.random() * 1500 + 50));

					OperationType type = Math.random() > 0.5 ? OperationType.CREDIT : OperationType.DEBIT;
					operation.setOperationType(type);
					operation.setDescription(type == OperationType.CREDIT ? "Versement" : "Retrait / Achat");

					operation.setOperationDate(opDate);
					operation.setAccount(account);

					operationRepository.save(operation);
				});
			});

			System.out.println("📌 Total opérations : " + (allAccounts.size() * 1000));
			System.out.println("🎉 Données entièrement générées !");
		};
	}

	/* -----------------------------------------------
       Génération d’une date aléatoire entre 2 bornes
    ----------------------------------------------- */
	private LocalDateTime randomDate(LocalDateTime start, LocalDateTime end, Random random) {
		long seconds = ChronoUnit.SECONDS.between(start, end);
		long randomSec = (long) (random.nextDouble() * seconds);
		return start.plusSeconds(randomSec);
	}


	private List<Customer> generateCustomers(int count) {

		List<String> firstNames = List.of(
				"John","Jane","Alice","Robert","Maria","David","Emma","Liam","Olivia","Noah","Sophia",
				"Ethan","Isabella","Mason","Mia","James","Amelia","Benjamin","Charlotte","Lucas","Ava",
				"Logan","Harper","Elijah","Ella","Jacob","Emily","Michael","Abigail","William","Chloe",
				"Daniel","Grace","Samuel","Victoria","Henry","Scarlett","Gabriel","Hannah","Jack","Aria",
				"Alexander","Luna","Sebastian","Zoe","Mateo","Nora","Julian","Riley"
		);

		List<String> lastNames = List.of(
				"Smith","Johnson","Williams","Brown","Jones","Garcia","Miller","Davis","Rodriguez",
				"Martinez","Hernandez","Lopez","Gonzalez","Wilson","Anderson","Thomas","Taylor",
				"Moore","Jackson","Martin","Lee","Perez","Thompson","White","Harris","Sanchez",
				"Clark","Ramirez","Lewis","Robinson","Walker","Young","Allen","King","Wright","Scott",
				"Torres","Nguyen","Hill","Flores","Green","Adams","Nelson","Baker","Hall","Rivera"
		);

		Random random = new Random();
		Set<String> usedEmails = new HashSet<>();

		List<Customer> customers = new ArrayList<>();

		for (int i = 0; i < count; i++) {

			String firstName = firstNames.get(random.nextInt(firstNames.size()));
			String lastName = lastNames.get(random.nextInt(lastNames.size()));

			// ---- Générer email unique
			String baseEmail = (firstName + "." + lastName).toLowerCase();
			String email = baseEmail + "@mail.com";

			int counter = 1;
			while (usedEmails.contains(email)) {
				email = baseEmail + counter + "@mail.com";
				counter++;
			}
			usedEmails.add(email);

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
