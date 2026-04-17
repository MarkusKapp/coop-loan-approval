package com.example.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class EstonianPersonalCodeValidatorTest {

    private final EstonianPersonalCodeValidator validator = new EstonianPersonalCodeValidator();

    @Test
    void invalidWhenLengthOrFormatIsWrong() {
        assertFalse(validator.isValid(null));
        assertFalse(validator.isValid("123"));
        assertFalse(validator.isValid("abc12345678"));
    }

    @Test
    void invalidWhenGenderDigitIsOutOfRange() {
        assertFalse(validator.isValid("79001010005"));
    }
}

