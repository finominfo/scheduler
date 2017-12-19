package hu.finominfo.scheduler.people;

import hu.finominfo.scheduler.common.Globals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kalman.kovacs@globessey.local on 2017.12.18.
 */
public class People {
    private final Map<String, Person> people = new HashMap<>();

    public People() throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(Globals.getInstance().getConfigFile())));
        String lines[] = content.split("\\r?\\n");
        List<String> keywords = Arrays.asList(new String[]{"nofo", "next"});
        Arrays.asList(lines).stream().forEach(line -> {
            Person person = null;
            for (String expression : line.split(",")) {
                final String trimmedExpression = expression.trim();
                if (person == null) {
                    person = new Person(trimmedExpression);
                    people.put(trimmedExpression, person);
                } else if (keywords.contains(trimmedExpression.toLowerCase())) {
                    switch (trimmedExpression.toLowerCase()) {
                        case "nofo" :
                            person.setExperienced(false);
                            break;
                        case "next" :
                            person.setHeWantsNextWeekend(true);
                            break;
                    }
                } else {
                    final int day = Integer.valueOf(trimmedExpression.substring(1));
                    switch (trimmedExpression.charAt(0)) {
                        case 'w' :
                            person.getWantedDays().add(day);
                            if (person.getHatedDays().contains(day)) {
                                throw new RuntimeException(person.getName() + " wants and hates the same day: " + day);
                            }
                            break;
                        case 'h' :
                            person.getHatedDays().add(day);
                            if (person.getWantedDays().contains(day)) {
                                throw new RuntimeException(person.getName() + " wants and hates the same day: " + day);
                            }
                            break;
                    }
                }
            }
        });
    }

    public Map<String, Person> getPeople() {
        return people;
    }
}
