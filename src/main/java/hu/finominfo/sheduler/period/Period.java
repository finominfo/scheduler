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

    public static Period create (String expression) {
        String trimmedExpression = expression.trim();
        char lastChar = trimmedExpression.charAt(trimmedExpression.length() - 1);
        if (Character.isDigit(lastChar)) {
            return new Period(Integer.valueOf(trimmedExpression), InsideADay.ALL);
        } else {
            return new Period(Integer.valueOf(trimmedExpression.substring(0, trimmedExpression.length() - 1)), InsideADay.get(lastChar));
        }
    }
}
