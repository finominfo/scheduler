package hu.finominfo.scheduler.people;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kalman.kovacs@globessey.local on 2017.12.18.
 */
public class Person {
    private final String name;
    private final List<Integer> hatedDays = new ArrayList<>();
    private final List<Integer> wantedDays = new ArrayList<>();

    private volatile boolean experienced = true;
    private volatile boolean heWantsNextWeekend = false;

    public Person(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<Integer> getHatedDays() {
        return hatedDays;
    }

    public List<Integer> getWantedDays() {
        return wantedDays;
    }

    public boolean isExperienced() {
        return experienced;
    }

    public void setExperienced(boolean experienced) {
        this.experienced = experienced;
    }

    public boolean isHeWantsNextWeekend() {
        return heWantsNextWeekend;
    }

    public void setHeWantsNextWeekend(boolean heWantsNextWeekend) {
        this.heWantsNextWeekend = heWantsNextWeekend;
    }
}
