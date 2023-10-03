package hu.finominfo.scheduler.people;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by kalman.kovacs@globessey.local on 2017.12.18.
 */
public class Person {
    private final String name;
    private final List<Integer> hatedDays = new ArrayList<>();
    private final Set<Integer> wantedDays = new HashSet<>();
    private final Map<Integer, Type> types = new HashMap<>();
    private final AtomicInteger numOfScheduled = new AtomicInteger(0);
    private final AtomicInteger manualDayDifference = new AtomicInteger(0);

    private volatile boolean hatesWeekends = false;
    private volatile boolean hatesWeekdays = false;
    private volatile boolean hatesMondays = false;
    private volatile boolean hatesTuesdays = false;
    private volatile boolean wantedTuesdays = false;

    private volatile boolean hatesWednesdays = false;
    private volatile boolean hatesThursdays = false;
    private volatile boolean hatesFridays = false;

    public AtomicInteger getNumOfScheduled() {
        return numOfScheduled;
    }

    public AtomicInteger getManualDayDifference() {
        return manualDayDifference;
    }

    public Person(String name) {
        this.name = name;
        for (int i = -5; i < 36; i++) {
            types.put(i, Type.FO_AND_BO);
        }
    }

    public String getName() {
        return name;
    }

    public Map<Integer, Type> getTypes() {
        return types;
    }

    public Type getType(int day) {
        return types.get(day);
    }

    public void setType(int day, Type type) {
        types.put(day, type);
    }

    public List<Integer> getHatedDays() {
        return hatedDays;
    }

    public Set<Integer> getWantedDays() {
        return wantedDays;
    }

    public boolean isHatesWeekends() {
        return hatesWeekends;
    }

    public void setHatesWeekends(boolean hatesWeekends) {
        this.hatesWeekends = hatesWeekends;
    }

    public boolean isHatesWeekdays() {
        return hatesWeekdays;
    }

    public void setHatesWeekdays(boolean hatesWeekdays) {
        this.hatesWeekdays = hatesWeekdays;
    }

    public boolean isHatesMondays() {
        return hatesMondays;
    }

    public void setHatesMondays(boolean hatesMondays) {
        this.hatesMondays = hatesMondays;
    }

    public boolean isHatesTuesdays() {
        return hatesTuesdays;
    }

    public void setHatesTuesdays(boolean hatesTuesdays) {
        this.hatesTuesdays = hatesTuesdays;
    }

    public boolean isWantedTuesdays() {
        return wantedTuesdays;
    }

    public void setWantedTuesdays(boolean wantedTuesdays) {
        this.wantedTuesdays = wantedTuesdays;
    }

    public boolean isHatesWednesdays() {
        return hatesWednesdays;
    }

    public void setHatesWednesdays(boolean hatesWednesdays) {
        this.hatesWednesdays = hatesWednesdays;
    }

    public boolean isHatesThursdays() {
        return hatesThursdays;
    }

    public void setHatesThursdays(boolean hatesThursdays) {
        this.hatesThursdays = hatesThursdays;
    }

    public boolean isHatesFridays() {
        return hatesFridays;
    }

    public void setHatesFridays(boolean hatesFridays) {
        this.hatesFridays = hatesFridays;
    }

}
