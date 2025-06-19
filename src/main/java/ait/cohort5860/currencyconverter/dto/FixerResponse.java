package ait.cohort5860.currencyconverter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
public class FixerResponse {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("base")
    private String base;

    @JsonProperty("date")
    private String date;

    @JsonProperty("rates")
    private Map<String, Double> rates;

    @JsonProperty("error")
    private FixerError error;

    @Data
    @NoArgsConstructor
    public static class FixerError {
        @JsonProperty("code")
        private int code;

        @JsonProperty("type")
        private String type;

        @JsonProperty("info")
        private String info;
    }
}
