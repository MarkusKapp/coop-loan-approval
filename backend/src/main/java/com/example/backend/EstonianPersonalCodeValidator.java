package com.example.backend;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Slf4j
public class EstonianPersonalCodeValidator {

    private static final int[] WEIGHTS1 = {1, 2, 3, 4, 5, 6, 7, 8, 9, 1};
    private static final int[] WEIGHTS2 = {3, 4, 5, 6, 7, 8, 9, 1, 2, 3};

    public boolean isValid(String code) {
        if (code == null || !code.matches("\\d{11}")) {
            log.warn("Personal code validation failed: value is null or does not match 11-digit format");
            return false;
        }

        try {
            parseBirthDate(code);
            // LocalDate.of validates the BirthDate
        } catch (Exception _) {
            log.warn("Personal code validation failed: invalid encoded birth date or century digit");
            return false;
        }

        boolean valid = getCheckDigit(code) == (code.charAt(10) - '0');
        if (!valid) {
            log.warn("Personal code validation failed: checksum mismatch");
        } else {
            log.debug("Personal code validated successfully");
        }
        return valid;
    }

    private static int getCheckDigit(String code) {
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += (code.charAt(i) - '0') * WEIGHTS1[i];
        }
        int checkDigit = sum % 11;

        if (checkDigit == 10) {
            sum = 0;
            for (int i = 0; i < 10; i++) {
                sum += (code.charAt(i) - '0') * WEIGHTS2[i];
            }
            checkDigit = sum % 11;
            if (checkDigit == 10) checkDigit = 0;
        }
        return checkDigit;
    }

    private record BirthDate(int year, int month, int day) {}

    private static BirthDate parseBirthDate(String code) {
        int gender = code.charAt(0) - '0';
        int year = Integer.parseInt(code.substring(1, 3));
        int month = Integer.parseInt(code.substring(3, 5));
        int day = Integer.parseInt(code.substring(5, 7));

        int fullYear = switch (gender) {
            case 1, 2 -> 1800 + year;
            case 3, 4 -> 1900 + year;
            case 5, 6 -> 2000 + year;
            default -> throw new IllegalArgumentException("Invalid gender code: " + gender);
        };

        LocalDate.of(fullYear, month, day);

        return new BirthDate(fullYear, month, day);
    }

    public LocalDate extractBirthDate(String code) {
        BirthDate bd = parseBirthDate(code);
        log.debug("Extracted birth date from personal code");
        return LocalDate.of(bd.year(), bd.month(), bd.day());
    }
}