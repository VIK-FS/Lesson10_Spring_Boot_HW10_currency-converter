package ait.cohort5860.currencyconverter;

import ait.cohort5860.currencyconverter.service.CurrencyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Scanner;

@Component
@Profile("!test") // Запускается только если профиль НЕ "test"
public class ConsoleAppl implements CommandLineRunner {

    @Autowired
    private CurrencyService currencyService;

    @Autowired
    private ApplicationContext context;

    @Override
    public void run(String... args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Currency Converter ===");
        System.out.println("Type 'exit' to quit");

        while (true) {
            try {
                System.out.print("\nEnter source currency (e.g., USD): ");
                String fromCurrency = scanner.nextLine().trim();

                if ("exit".equalsIgnoreCase(fromCurrency)) {
                    System.out.println("Goodbye!");
                    scanner.close();
                    SpringApplication.exit(context, () -> 0); // Грациозное завершение
                    return;
                }

                if (!currencyService.isValidCurrency(fromCurrency)) {
                    System.out.println("Error: Invalid currency format. Use 3-letter code (e.g., USD)");
                    continue;
                }
                if (!currencyService.isSupportedCurrency(fromCurrency)) {
                    System.out.println("Error: Source currency is not supported by API");
                    continue;
                }

                System.out.print("Enter target currency (e.g., EUR): ");
                String toCurrency = scanner.nextLine().trim();

                if ("exit".equalsIgnoreCase(toCurrency)) {
                    System.out.println("Goodbye!");
                    scanner.close();
                    SpringApplication.exit(context, () -> 0); // Грациозное завершение
                    return;
                }

                if (!currencyService.isValidCurrency(toCurrency)) {
                    System.out.println("Error: Invalid currency format. Use 3-letter code (e.g., EUR)");
                    continue;
                }
                if (!currencyService.isSupportedCurrency(toCurrency)) {
                    System.out.println("Error: Target currency is not supported by API");
                    continue;
                }

                System.out.print("Enter amount to convert: ");
                String amountStr = scanner.nextLine().trim();

                if ("exit".equalsIgnoreCase(amountStr)) {
                    System.out.println("Goodbye!");
                    scanner.close();
                    SpringApplication.exit(context, () -> 0); // Грациозное завершение
                    return;
                }

                BigDecimal amount;
                try {
                    amount = new BigDecimal(amountStr);
                    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                        System.out.println("Error: Amount must be a positive number");
                        continue;
                    }
                    if (amount.compareTo(new BigDecimal("0.01")) < 0) {
                        System.out.println("Error: Amount must be at least 0.01");
                        continue;
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Error: Invalid amount format");
                    continue;
                }

                System.out.println("Converting...");
                BigDecimal result = currencyService.convertCurrency(fromCurrency, toCurrency, amount);

                System.out.println("=== RESULT ===");
                System.out.printf("%.2f %s = %.2f %s%n",
                        amount, fromCurrency.toUpperCase(),
                        result, toCurrency.toUpperCase());

            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                System.out.println("Try again or type 'exit' to quit");
            }
        }
    }
}