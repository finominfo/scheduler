package hu.finominfo.sheduler.period;

/**
 * Created by kalman.kovacs@globessey.local on 2017.12.18.
 */
public enum InsideADay {
    DAY('d'),
    NIGHT('n'),
    ALL('a');

    private final char sign;

    InsideADay(char sign) {

        this.sign = sign;
    }

    public char getSign() {
        return sign;
    }

    public static InsideADay get(char sign) {
        for (InsideADay insideADay : InsideADay.values()) {
            if (insideADay.getSign() == sign) {
                return insideADay;
            }
        }
        return null;
    }
}
