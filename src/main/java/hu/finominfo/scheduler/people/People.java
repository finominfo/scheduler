package hu.finominfo.scheduler.people;

import hu.finominfo.scheduler.common.Globals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by kalman.kovacs@globessey.local on 2017.12.18.
 */
public class People {
    private final Map<String, Person> people = new HashMap<>();
    private final Map<Integer, Set<String>> hated = new HashMap<>();

    public People() throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(Globals.getInstance().getConfigFile())), "UTF-8");
        String lines[] = content.split("\\r?\\n");
        for (int i = 0; i < 31 + 1; i++) {
            hated.put(i, new HashSet<>());
        }
        List<String> keywords = Arrays
                .asList(new String[] { "nofo", "hend", "hweek", "hmon", "htue", "hwen", "hthu", "hfri", "wtue" });
        Arrays.asList(lines).stream().forEach(line -> {
            Person person = null;
            for (String expression : line.trim().split(" ")) {
                final String trimmedExpression = expression.trim();
                if (trimmedExpression.isEmpty()) {
                    continue;
                }
                if (person == null) {
                    String replaced = trimmedExpression.replaceAll("_", " ");
                    person = new Person(replaced);
                    people.put(replaced, person);
                } else if (keywords.contains(trimmedExpression.toLowerCase())) {
                    switch (trimmedExpression.toLowerCase()) {
                        case "nofo":
                            for (int day : person.getTypes().keySet()) {
                                person.setType(day, Type.BO);
                            }
                            person.setNofo(true);
                            break;
                        case "hend":
                            person.setHatesWeekends(true);
                            break;
                        case "hweek":
                            person.setHatesWeekdays(true);
                            break;
                        case "hmon":
                            person.setHatesMondays(true);
                            break;
                        case "htue":
                            person.setHatesTuesdays(true);
                            break;
                        case "hwen":
                            person.setHatesWednesdays(true);
                            break;
                        case "hthu":
                            person.setHatesThursdays(true);
                            break;
                        case "hfri":
                            person.setHatesFridays(true);
                            break;
                        case "wtue":
                            person.setWantedTuesdays(true);
                            break;
                    }
                } else if (trimmedExpression.length() >= 1){
                    String remaining = trimmedExpression.substring(1);
                    final List<Integer> days = new ArrayList<Integer>();
                    if (remaining.contains("-")) {
                        String[] split = remaining.split("-");
                        int start = Integer.valueOf(split[0]);
                        int end = Integer.valueOf(split[1]);
                        while (start <= end) {
                            days.add(start);
                            start++;
                        }
                    } else {
                        days.add(Integer.valueOf(remaining));
                    }
                    switch (trimmedExpression.charAt(0)) {
                        case 'w':
                            setWantedDays(person, days);
                            break;
                        case 'h':
                            setHatedDays(person, days);
                            break;
                        case 'f':
                            for (int day : days)
                                person.setType(day, Type.FO);
                            //setWantedDays(person, days);
                            break;
                        case 'b':
                            for (int day : days)
                                person.setType(day, Type.BO);
                            //setWantedDays(person, days);
                            break;
                        case '+':
                            person.getManualDayDifference().getAndSet(days.get(0));
                            break;
                        case '-':
                            person.getManualDayDifference().getAndSet(-(days.get(0)));
                            break;
                    }
                }
            }
        });
    }

    private static void setHatedDays(Person person, List<Integer> days) {
        person.getHatedDays().addAll(days);
        for (int day : days) {
            if (person.getWantedDays().contains(day)) {
                throw new RuntimeException(
                        person.getName() + " wants and hates the same day: " + day);
            }
        }
    }

    private static void setWantedDays(Person person, List<Integer> days) {
        person.getWantedDays().addAll(days);
        for (int day : days) {
            if (person.getHatedDays().contains(day)) {
                throw new RuntimeException(
                        person.getName() + " wants and hates the same day: " + day);
            }
        }
    }

    public Map<String, Person> getPeople() {
        return people;
    }
}
