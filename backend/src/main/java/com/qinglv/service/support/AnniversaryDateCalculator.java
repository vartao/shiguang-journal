package com.qinglv.service.support;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * 纪念日日期计算。把纯日期规则从服务编排中分离，便于覆盖闰日等边界。
 */
public final class AnniversaryDateCalculator {

    private AnniversaryDateCalculator() {
    }

    public static Metrics calculate(LocalDate originalDate, Integer repeatYearly, LocalDate today) {
        Objects.requireNonNull(originalDate, "originalDate must not be null");
        Objects.requireNonNull(today, "today must not be null");

        long daysPassed = ChronoUnit.DAYS.between(originalDate, today);
        long years = ChronoUnit.YEARS.between(originalDate, today);
        long daysUntilNext = calculateDaysUntilNext(originalDate, repeatYearly, today);
        return new Metrics(daysPassed, daysUntilNext, years);
    }

    public static long calculateDaysUntilNext(LocalDate originalDate, Integer repeatYearly, LocalDate today) {
        Objects.requireNonNull(originalDate, "originalDate must not be null");
        Objects.requireNonNull(today, "today must not be null");

        if (!Integer.valueOf(1).equals(repeatYearly)) {
            return originalDate.isBefore(today) ? 0 : ChronoUnit.DAYS.between(today, originalDate);
        }

        LocalDate nextDate = occurrenceInYear(originalDate, today.getYear());
        if (nextDate.isBefore(today)) {
            nextDate = occurrenceInYear(originalDate, today.getYear() + 1);
        }
        return ChronoUnit.DAYS.between(today, nextDate);
    }

    private static LocalDate occurrenceInYear(LocalDate originalDate, int year) {
        YearMonth month = YearMonth.of(year, originalDate.getMonth());
        // 2 月 29 日在非闰年按 2 月最后一天纪念，避免 withYear 抛 DateTimeException。
        int day = Math.min(originalDate.getDayOfMonth(), month.lengthOfMonth());
        return month.atDay(day);
    }

    public record Metrics(long daysPassed, long daysUntilNext, long years) {
    }
}
