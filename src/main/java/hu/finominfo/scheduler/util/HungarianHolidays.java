package hu.finominfo.scheduler.util;

import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HungarianHolidays {

    // Define a method to get Hungarian holidays for a specific month
    public static List<LocalDate> getHolidaysForMonth(int year, Month month) {
        List<LocalDate> holidays = new ArrayList<>();

        // Add New Year's Day (January 1)
        holidays.add(LocalDate.of(year, Month.JANUARY, 1));

        // Add National Day (March 15)
        holidays.add(LocalDate.of(year, Month.MARCH, 15));

        // Add Easter Sunday and Monday and previous Friday (date varies each year)
        LocalDate easterDate = calculateEasterDate(year);
        holidays.add(easterDate);
        holidays.add(easterDate.plusDays(1));
        holidays.add(easterDate.minusDays(2));

        // Add Labour Day (May 1)
        holidays.add(LocalDate.of(year, Month.MAY, 1));

        // Add Whit Monday (Pentecost - date varies each year)
        LocalDate pentecost = easterDate.plus(49, ChronoUnit.DAYS);
        holidays.add(pentecost);

        // Add State Foundation Day (August 20)
        holidays.add(LocalDate.of(year, Month.AUGUST, 20));

        // Add National Day (October 23)
        holidays.add(LocalDate.of(year, Month.OCTOBER, 23));

        // Add All Saints' Day (November 1)
        holidays.add(LocalDate.of(year, Month.NOVEMBER, 1));

        // Add Christmas Day (December 25)
        holidays.add(LocalDate.of(year, Month.DECEMBER, 25));

        // Add Second Day of Christmas (December 26)
        holidays.add(LocalDate.of(year, Month.DECEMBER, 26));

        // Filter the holidays for the given month
        return holidays.stream()
                .filter(date -> date.getMonth() == month)
                .collect(Collectors.toList());
    }

    // Calculate the date of Easter Sunday using Zeller's Congruence
    private static LocalDate calculateEasterDate(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = ((h + l - 7 * m + 114) % 31) + 1;

        return LocalDate.of(year, month, day);
    }

    public static void main(String[] args) {
        int year = 2023;
        Month month = Month.AUGUST; // Change this to the desired month

        

        List<LocalDate> holidays = getHolidaysForMonth(year, month);

        System.out.println("Official Hungarian holidays in " + month + " " + year + ":");
        for (LocalDate holiday : holidays) {
            System.out.println(holiday);
        }
    }
}
