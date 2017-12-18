package hu.finominfo.sheduler.people;

import hu.finominfo.sheduler.period.Period;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kalman.kovacs@globessey.local on 2017.12.18.
 */
public class Person {
    private final String name;
    private final List<Period> notGoodPeriods = new ArrayList<>();
    private final List<Period> scheduledPeriods = new ArrayList<>();

    public Person(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<Period> getNotGoodPeriods() {
        return notGoodPeriods;
    }

    public List<Period> getScheduledPeriods() {
        return scheduledPeriods;
    }
}
