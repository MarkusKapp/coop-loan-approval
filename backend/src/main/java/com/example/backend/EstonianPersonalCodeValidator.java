package com.example.backend;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class EstonianPersonalCodeValidator {

    private static final int[] WEIGHTS1 = {1, 2, 3, 4, 5, 6, 7, 8, 9, 1};
    private static final int[] WEIGHTS2 = {3, 4, 5, 6, 7, 8, 9, 1, 2, 3};

    public boolean isValid(String code) {
        if (code == null || !code.matches("\\d{11}")) return false;

        BirthDate bd;
        try {
            bd = parseBirthDate(code);
        } catch (RuntimeException ex) {
            return false;
        }

        int month = bd.month();
        int day = bd.day();

        if (month < 1 || month > 12) return false;
        if (day < 1 || day > 31) return false;

        // 3. Kontrollnumber
        int checkDigit = getCheckDigit(code);

        return checkDigit == (code.charAt(10) - '0');
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

    public record BirthDate(int year, int month, int day) {}

    public static BirthDate parseBirthDate(String code) {
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

        // LocalDate validates impossible dates like 31.02 automatically.
        LocalDate.of(fullYear, month, day);

        return new BirthDate(fullYear, month, day);
    }

    public LocalDate extractBirthDate(String code) {
        BirthDate bd = parseBirthDate(code);
        return LocalDate.of(bd.year(), bd.month(), bd.day());
    }
}