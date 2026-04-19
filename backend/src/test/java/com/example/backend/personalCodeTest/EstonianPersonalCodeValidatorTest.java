package com.example.backend.personalCodeTest;

import com.example.backend.EstonianPersonalCodeValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class EstonianPersonalCodeValidatorTest {

    private final EstonianPersonalCodeValidator validator = new EstonianPersonalCodeValidator();

    @Test
    void isValidRejectsLengthFormatChecksumAndInvalidDate() {
        assertFalse(validator.isValid(null));
        assertFalse(validator.isValid("123"));
        assertFalse(validator.isValid("abc12345678"));

        String validCode = TestPersonalCodeFactory.personalCode(3, LocalDate.of(1990, 1, 15), 123);
        assertTrue(validator.isValid(validCode));
        assertFalse(validator.isValid(TestPersonalCodeFactory.withInvalidChecksum(validCode)));

        assertFalse(validator.isValid("39902310000"));
    }

    @Test
    void extractBirthDateSupportsCenturyDigitsAndValidatesDate() {
        String eighteenthCentury = TestPersonalCodeFactory.personalCode(1, LocalDate.of(1805, 1, 2), 321);
        String twentiethCentury = TestPersonalCodeFactory.personalCode(3, LocalDate.of(1999, 12, 31), 654);
        String twentyFirstCentury = TestPersonalCodeFactory.personalCode(5, LocalDate.of(2000, 2, 29), 987);

        assertEquals(LocalDate.of(1805, 1, 2), validator.extractBirthDate(eighteenthCentury));
        assertEquals(LocalDate.of(1999, 12, 31), validator.extractBirthDate(twentiethCentury));
        assertEquals(LocalDate.of(2000, 2, 29), validator.extractBirthDate(twentyFirstCentury));

        assertThrows(IllegalArgumentException.class, () -> validator.extractBirthDate("79901010005"));
        assertThrows(RuntimeException.class, () -> validator.extractBirthDate("39902310000"));
    }
}

