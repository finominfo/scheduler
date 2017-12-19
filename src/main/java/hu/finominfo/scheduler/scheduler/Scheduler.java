package hu.finominfo.scheduler.scheduler;

import hu.finominfo.scheduler.people.Person;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

/**
 * Created by kks on 2017.12.18..
 */
public class Scheduler {
    private final Map<String, Person> people;
    private final Map<Integer, Set<String>> scheduled = new HashMap<>();
    private final Map<Integer, Set<String>> hated = new HashMap<>();
    private final List<Integer> weekends = new ArrayList<>();
    private final int numOfDays;
    private final LocalDate localDate;
    private volatile boolean weekendWasScheduled = false;

    public Scheduler(Map<String, Person> people, LocalDate date) {
        this.people = people;
        this.numOfDays = date.getDayOfMonth();
        for (int i = 0; i < numOfDays; i++) {
            scheduled.put(i, new HashSet<>());
            hated.put(i, new HashSet<>());
        }
        this.localDate = date.withDayOfMonth(1);
        setHated();
        setWanted();
        countWeekends();
        setWeekends();
    }

    private void setWeekends() {
        for (int i = 0; i < weekends.size(); i++) {
            int saturdayNumber = weekends.get(i);
            Set<String> saturday = scheduled.get(saturdayNumber);
            Set<String> sunday = scheduled.get(saturdayNumber + 1);
            saturday.addAll(sunday);
            sunday.addAll(saturday);
            if (saturday.size() > 2) {
                throw new RuntimeException("More than two people wants the " + (i + 1) + ". weekend: " +
                        saturday.stream().map(e -> e.toString() + " ").reduce("", String::concat));
            }
        }
        for (int i = 0; i < weekends.size(); i++) {
            int saturdayNumber = weekends.get(i);
            Set<String> saturday = scheduled.get(saturdayNumber);
            Set<String> sunday = scheduled.get(saturdayNumber + 1);
            while (saturday.size() < 2) {
                final Set<String> possibleNames = new HashSet<>();
                possibleNames.addAll(people.keySet());
                weekends.stream().forEach(satNum -> possibleNames.removeAll(scheduled.get(satNum)));
                if (possibleNames.isEmpty()) {
                    //TODO: Continue!!!
                }
            }
        }
    }

    private void countWeekends() {
        LocalDate result = localDate;
        while (result.getMonthValue() == localDate.getMonthValue()) {
            if (result.getDayOfWeek() == DayOfWeek.SATURDAY) {
                weekends.add(result.getDayOfMonth());
            }
            result = result.plusDays(1);
        }
    }


    private void setWanted() {
        people.entrySet().stream().forEach(entry -> entry.getValue().getWantedDays().forEach(wantedDay -> {
            Set<String> set = scheduled.get(wantedDay);
            set.add(entry.getKey());
            if (set.size() == 2) {
                if (set.stream().allMatch(name -> !people.get(name).isExperienced())) {
                    throw new RuntimeException("Two not experienced people wants the same day: " +
                            set.stream().map(e -> e.toString() + " ").reduce("", String::concat));
                }
            }
            if (set.size() > 2) {
                throw new RuntimeException("More than two people wants the same day: " +
                        set.stream().map(e -> e.toString() + " ").reduce("", String::concat));
            }
        }));
    }

    private void setHated() {
        people.entrySet().stream().forEach(entry -> entry.getValue().getHatedDays().forEach(hatedDay -> {
            Set<String> set = hated.get(hatedDay);
            set.add(entry.getKey());
            if (set.size() > people.size() - 2) {
                throw new RuntimeException("Too much people hates the same day: " +
                        set.stream().map(e -> e.toString() + " ").reduce("", String::concat));
            }
        }));
        hated.entrySet().stream().forEach(entry -> {
            Set<String> set = entry.getValue();
            if (set.size() == people.size() - 2) { //Only 2 person remain for that day
                final Set<String> possibleNames = new HashSet<>();
                possibleNames.addAll(people.keySet());
                possibleNames.removeAll(set);
                scheduled.get(entry.getKey()).addAll(possibleNames);
                System.out.println(possibleNames.stream().map(e -> e.toString() + " ").reduce("", String::concat) +
                    " were added to " + entry.getKey() + ", because everybody else hate that day.");
            }

        });
    }

    public Map<Integer, Set<String>> getScheduled() {
        return scheduled;
    }
}
