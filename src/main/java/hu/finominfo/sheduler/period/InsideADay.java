package hu.finominfo.sheduler.period;

/**
 * Created by kalman.kovacs@globessey.local on 2017.12.18.
 */
public enum InsideADay {
    DAY('d'),
    NIGHT('n');

    private final char sign;

    InsideADay(char sign) {

        this.sign = sign;
    }

    public char getSign() {
        return sign;
    }
}
