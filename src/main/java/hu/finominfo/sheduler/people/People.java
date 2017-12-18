package hu.finominfo.sheduler.people;

import hu.finominfo.sheduler.common.Globals;
import hu.finominfo.sheduler.period.Period;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by kalman.kovacs@globessey.local on 2017.12.18.
 */
public class People {
    private Map<String, Person> people = new HashMap<>();

    public People() throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(Globals.getInstance().getConfigFile())));
        String lines[] = content.split("\\r?\\n");
        Arrays.asList(lines).stream().forEach(line -> {
            Person person = null;
            for (String expression : line.split(",")) {
                if (person == null) {
                    person = new Person(expression);
                    people.put(expression, person);
                } else {
                    person.getNotGoodPeriods().add(Period.create(expression));
                }
            }
        });
    }

    public Map<String, Person> getPeople() {
        return people;
    }
}
