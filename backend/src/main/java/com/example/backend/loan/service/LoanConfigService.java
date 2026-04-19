package com.example.backend.loan.service;

import com.example.backend.loan.entity.LoanConfig;
import com.example.backend.loan.repository.LoanConfigRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanConfigService {

    private static final String KEY_EURIBOR = "euribor_6m";
    private static final String KEY_MAX_AGE = "customer_max_age";
    private static final BigDecimal DEFAULT_EURIBOR = new BigDecimal("3.856");
    private static final int DEFAULT_MAX_AGE = 70;

    private final LoanConfigRepository loanConfigRepository;

    public BigDecimal getEuribor() {
        return getValue(KEY_EURIBOR, BigDecimal::new);
    }

    public int getMaxCustomerAge() {
        return getValue(KEY_MAX_AGE, Integer::parseInt);
    }

    public LoanConfig setMaxCustomerAge(int value) {
        return setValue(KEY_MAX_AGE, String.valueOf(value));
    }

    public LoanConfig setEuribor(BigDecimal rate) {
        log.info("Euribor updated to {}%", rate);
        return setEuriborValue(rate);
    }

    public List<LoanConfig> getAll() {
        return loanConfigRepository.findAll();
    }

    // Seeds required default configuration values at startup.
    @PostConstruct
    public void initEuribor() {
        ensureDefault(KEY_EURIBOR, DEFAULT_EURIBOR.toPlainString(), "6-month Euribor rate (%)");
        ensureDefault(KEY_MAX_AGE, String.valueOf(DEFAULT_MAX_AGE), "Maximum customer age in years");
    }

    private LoanConfig setEuriborValue(BigDecimal value) {
        return setValue(KEY_EURIBOR, value.toPlainString());
    }

    // Creates a config entry only when the key does not already exist.
    private void ensureDefault(String key, String value, String description) {
        if (loanConfigRepository.existsById(key)) {
            return;
        }

        LoanConfig config = LoanConfig.builder()
                .key(key)
                .value(value)
                .description(description)
                .updatedAt(LocalDateTime.now())
                .build();
        loanConfigRepository.save(config);
        log.info("Seeded missing loan config '{}' with default value '{}'.", key, value);
    }

    // Loads and parses a config value for the provided key.
    private <T> T getValue(String key, Function<String, T> parser) {
        return loanConfigRepository.findById(key)
                .map(c -> parser.apply(c.getValue()))
                .orElseThrow(() -> new IllegalStateException("Missing loan config: " + key));
    }

    // Upserts a config value and refreshes its update timestamp.
    private LoanConfig setValue(String key, String value) {
        LoanConfig config = loanConfigRepository.findById(key)
                .orElse(LoanConfig.builder()
                        .key(key)
                        .value(value)
                        .description(defaultDescriptionFor(key))
                        .build());
        config.setValue(value);
        config.setUpdatedAt(LocalDateTime.now());
        log.info("Updated loan config '{}' to '{}'", key, value);
        return loanConfigRepository.save(config);
    }

    // Provides a default description for known configuration keys.
    private String defaultDescriptionFor(String key) {
        if (KEY_EURIBOR.equals(key)) {
            return "6-month Euribor rate (%)";
        }
        if (KEY_MAX_AGE.equals(key)) {
            return "Maximum customer age in years";
        }
        return null;
    }
}