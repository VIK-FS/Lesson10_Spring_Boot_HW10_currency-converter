package ait.cohort5860.currencyconverter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversionResponse {

    private String fromCurrency;
    private String toCurrency;
    private BigDecimal originalAmount;
    private BigDecimal convertedAmount;
    private Double exchangeRate;
    private LocalDateTime timestamp;
    private boolean success;
    private String errorMessage;

    public static ConversionResponse success(String fromCurrency, String toCurrency,
                                             BigDecimal originalAmount, BigDecimal convertedAmount,
                                             Double exchangeRate) {
        return ConversionResponse.builder()
                .fromCurrency(fromCurrency)
                .toCurrency(toCurrency)
                .originalAmount(originalAmount)
                .convertedAmount(convertedAmount)
                .exchangeRate(exchangeRate)
                .timestamp(LocalDateTime.now())
                .success(true)
                .build();
    }

    public static ConversionResponse error(String errorMessage) {
        return ConversionResponse.builder()
                .success(false)
                .errorMessage(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
