package ait.cohort5860.currencyconverter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversionRequest {

    @NotBlank(message = "Source currency is required")
    @Pattern(regexp = "^[A-Za-z]{3}$", message = "Source currency must be a 3-letter code")
    private String fromCurrency;

    @NotBlank(message = "Target currency is required")
    @Pattern(regexp = "^[A-Za-z]{3}$", message = "Target currency must be a 3-letter code")
    private String toCurrency;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    private BigDecimal amount;

    public boolean isValid() {
        return fromCurrency != null && !fromCurrency.trim().isEmpty() &&
                toCurrency != null && !toCurrency.trim().isEmpty() &&
                amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }
}
