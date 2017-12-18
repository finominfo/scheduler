package hu.finominfo.sheduler.period;

/**
 * Created by kalman.kovacs@globessey.local on 2017.12.18.
 */
public class Period {
    private final int day;
    private final InsideADay insideADay;

    public Period(int day, InsideADay insideADay) {
        this.day = day;
        this.insideADay = insideADay;
    }

    public int getDay() {
        return day;
    }

    public InsideADay getInsideADay() {
        return insideADay;
    }
}
