package ait.cohort5860.currencyconverter.service;

/*
import ait.cohort5860.currencyconverter.dto.FixerResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@Service
@Slf4j
public class CurrencyService {

    @Value("${fixer.api.key}")
    private String apiKey;

    @Value("${fixer.api.url}")
    private String apiUrl;

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private final boolean useMockData;

    public CurrencyService() {
        this.client = new OkHttpClient().newBuilder().build();
        this.objectMapper = new ObjectMapper();
        this.useMockData = apiKey == null || apiKey.trim().isEmpty() || apiKey.equals("your-api-key-here");
    }

    @PostConstruct
    public void validateApiKey() {
        if (useMockData) {
            log.warn("Using mock data as Fixer API key is invalid or not configured. Set a valid key in 'fixer.api.key' for real API calls.");
        }
    }

    public BigDecimal convertCurrency(String fromCurrency, String toCurrency, BigDecimal amount) {
        if (!isValidCurrency(fromCurrency) || !isValidCurrency(toCurrency)) {
            throw new IllegalArgumentException("Invalid currency format");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (!isSupportedCurrency(fromCurrency) || !isSupportedCurrency(toCurrency)) {
            throw new IllegalArgumentException("Currency not supported by API");
        }

        try {
            FixerResponse response = getExchangeRates(fromCurrency, toCurrency);

            if (!response.isSuccess()) {
                if (response.getError() != null) {
                    throw new RuntimeException("API Error: " + response.getError().getInfo());
                }
                throw new RuntimeException("Failed to get exchange rates");
            }

            Double targetRate = response.getRates().get(toCurrency.toUpperCase());
            if (targetRate == null) {
                throw new RuntimeException("Currency " + toCurrency + " not found");
            }

            BigDecimal result = amount.multiply(BigDecimal.valueOf(targetRate));
            return result.setScale(2, RoundingMode.HALF_UP);

        } catch (Exception e) {
            log.error("Error converting currency: {}", e.getMessage());
            throw new RuntimeException("Currency conversion error: " + e.getMessage(), e);
        }
    }

    @Cacheable(value = "exchangeRates", key = "#baseCurrency.toUpperCase() + '-' + #targetCurrency.toUpperCase()")
    public FixerResponse getExchangeRates(String baseCurrency, String toCurrency) {
        if (useMockData) {
            log.debug("Using mock exchange rates for {} to {}", baseCurrency, toCurrency);
            FixerResponse response = new FixerResponse();
            response.setSuccess(true);
            response.setBase(baseCurrency.toUpperCase());
            response.setTimestamp(System.currentTimeMillis() / 1000);
            response.setRates(Map.of(
                    toCurrency.toUpperCase(), getMockRate(baseCurrency, toCurrency)
            ));
            return response;
        }

        try {
            String url = apiUrl + "?base=" + baseCurrency.toUpperCase() + "&symbols=" + toCurrency.toUpperCase();

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", apiKey)
                    .method("GET", null)
                    .build();

            log.debug("Request to Fixer API: {}", url);

            try (Response response = client.newCall(request).execute()) {
                checkRateLimits(response);

                if (!response.isSuccessful()) {
                    String responseBody = response.body() != null ? response.body().string() : "No response body";
                    log.error("HTTP Error: Code={}, Body={}", response.code(), responseBody);
                    throw new IOException("HTTP Error: " + response.code() + ", Body: " + responseBody);
                }

                String responseBody = response.body().string();
                log.debug("API Response: {}", responseBody);

                return objectMapper.readValue(responseBody, FixerResponse.class);
            }
        } catch (IOException e) {
            log.error("Error fetching exchange rates: {}", e.getMessage());
            throw new RuntimeException("Error fetching exchange rates", e);
        }
    }

    private void checkRateLimits(Response response) {
        String limit = response.header("X-RateLimit-Limit");
        String remaining = response.header("X-RateLimit-Remaining");
        String reset = response.header("X-RateLimit-Reset");

        if (limit == null || remaining == null || reset == null) {
            log.warn("Rate limit headers not provided by API");
            return;
        }

        try {
            int remainingRequests = Integer.parseInt(remaining);
            long resetTimestamp = Long.parseLong(reset);
            LocalDateTime resetTime = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(resetTimestamp), ZoneId.systemDefault());

            log.info("API Rate Limits: Limit={}, Remaining={}, Reset at {}", limit, remaining, resetTime);

            if (remainingRequests <= 10) {
                log.warn("Low API quota: {} requests remaining until {}", remainingRequests, resetTime);
            }
        } catch (NumberFormatException e) {
            log.error("Invalid rate limit header format: {}", e.getMessage());
        }
    }

    public boolean isValidCurrency(String currency) {
        return currency != null && currency.matches("^[A-Za-z]{3}$");
    }

    @Cacheable(value = "supportedCurrencies", key = "'SUPPORTED_CURRENCIES'")
    public boolean isSupportedCurrency(String currency) {
        try {
            FixerResponse response = getAllCurrencies();
            return response.getRates().containsKey(currency.toUpperCase());
        } catch (Exception e) {
            log.error("Error checking supported currency: {}", e.getMessage());
            return false;
        }
    }

    private FixerResponse getAllCurrencies() {
        if (useMockData) {
            log.debug("Using mock supported currencies");
            FixerResponse response = new FixerResponse();
            response.setSuccess(true);
            response.setBase("USD");
            response.setTimestamp(System.currentTimeMillis() / 1000);
            response.setRates(Map.of(
                    "USD", 1.0,
                    "EUR", 0.92,
                    "GBP", 0.79,
                    "JPY", 158.0
            ));
            return response;
        }

        try {
            String url = apiUrl + "?base=USD";
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", apiKey)
                    .method("GET", null)
                    .build();

            log.debug("Request to Fixer API: {}", url);

            try (Response response = client.newCall(request).execute()) {
                checkRateLimits(response);

                if (!response.isSuccessful()) {
                    String responseBody = response.body() != null ? response.body().string() : "No response body";
                    log.error("HTTP Error: Code={}, Body={}", response.code(), responseBody);
                    throw new IOException("HTTP Error: " + response.code() + ", Body: " + responseBody);
                }

                String responseBody = response.body().string();
                log.debug("API Response: {}", responseBody);

                return objectMapper.readValue(responseBody, FixerResponse.class);
            }
        } catch (IOException e) {
            log.error("Error fetching supported currencies: {}", e.getMessage());
            throw new RuntimeException("Error fetching supported currencies", e);
        }
    }

    private double getMockRate(String fromCurrency, String toCurrency) {
        // Mock exchange rates relative to USD
        Map<String, Double> rates = Map.of(
                "USD", 1.0,
                "EUR", 0.92,
                "GBP", 0.79,
                "JPY", 158.0
        );
        double fromRate = rates.getOrDefault(fromCurrency.toUpperCase(), 1.0);
        double toRate = rates.getOrDefault(toCurrency.toUpperCase(), 1.0);
        return toRate / fromRate;
    }
}
*/


import ait.cohort5860.currencyconverter.dto.FixerResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@Slf4j
public class CurrencyService {

    @Value("${fixer.api.key}")
    private String apiKey;

    @Value("${fixer.api.url}")
    private String apiUrl;

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public CurrencyService() {
        this.client = new OkHttpClient().newBuilder().build();
        this.objectMapper = new ObjectMapper();
    }

    public BigDecimal convertCurrency(String fromCurrency, String toCurrency, BigDecimal amount) {
        if (!isValidCurrency(fromCurrency) || !isValidCurrency(toCurrency)) {
            throw new IllegalArgumentException("Invalid currency format");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (!isSupportedCurrency(fromCurrency) || !isSupportedCurrency(toCurrency)) {
            throw new IllegalArgumentException("Currency not supported by API");
        }

        try {
            FixerResponse response = getExchangeRates(fromCurrency, toCurrency);

            if (!response.isSuccess()) {
                if (response.getError() != null) {
                    throw new RuntimeException("API Error: " + response.getError().getInfo());
                }
                throw new RuntimeException("Failed to get exchange rates");
            }

            Double targetRate = response.getRates().get(toCurrency.toUpperCase());
            if (targetRate == null) {
                throw new RuntimeException("Currency " + toCurrency + " not found");
            }

            BigDecimal result = amount.multiply(BigDecimal.valueOf(targetRate));
            return result.setScale(2, RoundingMode.HALF_UP);

        } catch (Exception e) {
            log.error("Error converting currency: {}", e.getMessage());
            throw new RuntimeException("Currency conversion error: " + e.getMessage(), e);
        }
    }

    @Cacheable(value = "exchangeRates", key = "#baseCurrency.toUpperCase() + '-' + #targetCurrency.toUpperCase()")
    public FixerResponse getExchangeRates(String baseCurrency, String targetCurrency) {
        try {
            String url = apiUrl + "?base=" + baseCurrency.toUpperCase() + "&symbols=" + targetCurrency.toUpperCase();

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", apiKey)
                    .method("GET", null)
                    .build();

            log.debug("Request to Fixer API: {}", url);

            try (Response response = client.newCall(request).execute()) {
                checkRateLimits(response);

                if (!response.isSuccessful()) {
                    String responseBody = response.body() != null ? response.body().string() : "No response body";
                    log.error("HTTP Error: Code={}, Body={}", response.code(), responseBody);
                    throw new IOException("HTTP Error: " + response.code() + ", Body: " + responseBody);
                }

                String responseBody = response.body().string();
                log.debug("API Response: {}", responseBody);

                return objectMapper.readValue(responseBody, FixerResponse.class);
            }
        } catch (IOException e) {
            log.error("Error fetching exchange rates: {}", e.getMessage());
            throw new RuntimeException("Error fetching exchange rates", e);
        }
    }

    private void checkRateLimits(Response response) {
        String limit = response.header("X-RateLimit-Limit");
        String remaining = response.header("X-RateLimit-Remaining");
        String reset = response.header("X-RateLimit-Reset");

        if (limit == null || remaining == null || reset == null) {
            log.warn("Rate limit headers not provided by API");
            return;
        }

        try {
            int remainingRequests = Integer.parseInt(remaining);
            long resetTimestamp = Long.parseLong(reset);
            LocalDateTime resetTime = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(resetTimestamp), ZoneId.systemDefault());

            log.info("API Rate Limits: Limit={}, Remaining={}, Reset at {}", limit, remaining, resetTime);

            if (remainingRequests <= 10) {
                log.warn("Low API quota: {} requests remaining until {}", remainingRequests, resetTime);
            }
        } catch (NumberFormatException e) {
            log.error("Invalid rate limit header format: {}", e.getMessage());
        }
    }

    public boolean isValidCurrency(String currency) {
        return currency != null && currency.matches("^[A-Za-z]{3}$");
    }

    @Cacheable(value = "supportedCurrencies", key = "'SUPPORTED_CURRENCIES'")
    public boolean isSupportedCurrency(String currency) {
        try {
            FixerResponse response = getAllCurrencies();
            return response.getRates().containsKey(currency.toUpperCase());
        } catch (Exception e) {
            log.error("Error checking supported currency: {}", e.getMessage());
            return false;
        }
    }

    private FixerResponse getAllCurrencies() {
        try {
            String url = apiUrl + "?base=USD";
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", apiKey)
                    .method("GET", null)
                    .build();

            log.debug("Request to Fixer API: {}", url);

            try (Response response = client.newCall(request).execute()) {
                checkRateLimits(response);

                if (!response.isSuccessful()) {
                    String responseBody = response.body() != null ? response.body().string() : "No response body";
                    log.error("HTTP Error: Code={}, Body={}", response.code(), responseBody);
                    throw new IOException("HTTP Error: " + response.code() + ", Body: " + responseBody);
                }

                String responseBody = response.body().string();
                log.debug("API Response: {}", responseBody);

                return objectMapper.readValue(responseBody, FixerResponse.class);
            }
        } catch (IOException e) {
            log.error("Error fetching supported currencies: {}", e.getMessage());
            throw new RuntimeException("Error fetching supported currencies", e);
        }
    }
}
