package com.example.backend.personalCodeTest;

import java.time.LocalDate;

public final class TestPersonalCodeFactory {

    private static final int[] WEIGHTS1 = {1, 2, 3, 4, 5, 6, 7, 8, 9, 1};
    private static final int[] WEIGHTS2 = {3, 4, 5, 6, 7, 8, 9, 1, 2, 3};

    private TestPersonalCodeFactory() {
    }

    public static String personalCode(int genderDigit, LocalDate birthDate, int sequence) {
        if (genderDigit < 1 || genderDigit > 6) {
            throw new IllegalArgumentException("Gender digit must be between 1 and 6");
        }
        if (sequence < 0 || sequence > 999) {
            throw new IllegalArgumentException("Sequence must be between 000 and 999");
        }

        String firstTenDigits = String.format(
                "%d%02d%02d%02d%03d",
                genderDigit,
                birthDate.getYear() % 100,
                birthDate.getMonthValue(),
                birthDate.getDayOfMonth(),
                sequence
        );

        return firstTenDigits + checksum(firstTenDigits);
    }

    public static String withInvalidChecksum(String validCode) {
        char lastDigit = validCode.charAt(validCode.length() - 1);
        char replacement = lastDigit == '9' ? '0' : (char) (lastDigit + 1);
        return validCode.substring(0, validCode.length() - 1) + replacement;
    }

    private static int checksum(String code) {
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
            if (checkDigit == 10) {
                checkDigit = 0;
            }
        }

        return checkDigit;
    }
}
