package ait.cohort5860.currencyconverter.controller;

import ait.cohort5860.currencyconverter.dto.ConversionRequest;
import ait.cohort5860.currencyconverter.dto.ConversionResponse;
import ait.cohort5860.currencyconverter.service.CurrencyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping("/api/currency")
public class CurrencyController {

    private final CurrencyService currencyService;

    public CurrencyController(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    @PostMapping("/convert")
    public ResponseEntity<ConversionResponse> convert(@Valid @RequestBody ConversionRequest request) {
        if (!request.isValid()) {
            return ResponseEntity.badRequest().body(
                    ConversionResponse.error("Invalid request parameters")
            );
        }

        try {
            BigDecimal convertedAmount = currencyService.convertCurrency(
                    request.getFromCurrency(),
                    request.getToCurrency(),
                    request.getAmount()
            );
            Double exchangeRate = convertedAmount.divide(
                    request.getAmount(), 4, RoundingMode.HALF_UP
            ).doubleValue();

            ConversionResponse response = ConversionResponse.success(
                    request.getFromCurrency(),
                    request.getToCurrency(),
                    request.getAmount(),
                    convertedAmount,
                    exchangeRate
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ConversionResponse.error("Conversion error: " + e.getMessage())
            );
        }
    }
}
