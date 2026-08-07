package com.qinglv.service.support;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnniversaryDateCalculatorTest {

    @Test
    void recurringLeapDayUsesLastDayOfFebruaryInNonLeapYear() {
        LocalDate anniversary = LocalDate.of(2024, 2, 29);

        assertEquals(0, AnniversaryDateCalculator.calculateDaysUntilNext(
                anniversary, 1, LocalDate.of(2025, 2, 28)));
        assertEquals(364, AnniversaryDateCalculator.calculateDaysUntilNext(
                anniversary, 1, LocalDate.of(2025, 3, 1)));
    }

    @Test
    void recurringDateMovesToNextYearOnlyAfterThisYearsOccurrence() {
        LocalDate anniversary = LocalDate.of(2020, 5, 20);

        assertEquals(1, AnniversaryDateCalculator.calculateDaysUntilNext(
                anniversary, 1, LocalDate.of(2026, 5, 19)));
        assertEquals(0, AnniversaryDateCalculator.calculateDaysUntilNext(
                anniversary, 1, LocalDate.of(2026, 5, 20)));
        assertEquals(364, AnniversaryDateCalculator.calculateDaysUntilNext(
                anniversary, 1, LocalDate.of(2026, 5, 21)));
    }

    @Test
    void nonRecurringPastDateStaysAtZero() {
        assertEquals(0, AnniversaryDateCalculator.calculateDaysUntilNext(
                LocalDate.of(2020, 1, 1), 0, LocalDate.of(2026, 1, 1)));
    }
}
