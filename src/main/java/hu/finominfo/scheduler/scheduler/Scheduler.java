package hu.finominfo.scheduler.scheduler;

import hu.finominfo.scheduler.people.Person;
import hu.finominfo.scheduler.people.Type;

import java.time.LocalDate;
import java.util.*;

/**
 * Created by kks on 2017.12.18..
 */
public class Scheduler {
    private final Random random = new Random();
    private final Map<String, Person> people;
    private final Map<Integer, Set<String>> scheduled = new HashMap<>();
    private final Map<Integer, Set<String>> hated = new HashMap<>();
    private final List<Integer> mondays = new ArrayList<>();
    private final List<Integer> tuesdays = new ArrayList<>();
    private final List<Integer> wednesdays = new ArrayList<>();
    private final List<Integer> thursdays = new ArrayList<>();
    private final List<Integer> fridays = new ArrayList<>();
    private final List<Integer> saturdays = new ArrayList<>();
    private final List<Integer> sundays = new ArrayList<>();
    private final int numOfDays;
    private final LocalDate localDate;
    List<Type> foAbleTypes = Arrays.asList(Type.FO, Type.FO_AND_BO);

    public Scheduler(Map<String, Person> people, LocalDate date) {
        this.people = people;
        this.numOfDays = date.lengthOfMonth();
        for (int i = -8; i < numOfDays + 10; i++) {
            scheduled.put(i, new HashSet<>());
            hated.put(i, new HashSet<>());
        }
        this.localDate = date.withDayOfMonth(1);
        countDays();
        setHated();
        setWanted();
        setWeekends();
        setWeekdays();

    }

    // --------------------------------------------------------------------------------------------------

    private void setHated() {
        people.entrySet().stream().forEach(entry -> {
            Person person = entry.getValue();
            if (person.isHatesMondays()) mondays.forEach(day -> person.getHatedDays().add(day));
            if (person.isHatesTuesdays()) tuesdays.forEach(day -> person.getHatedDays().add(day));
            if (person.isHatesWednesdays()) wednesdays.forEach(day -> person.getHatedDays().add(day));
            if (person.isHatesThursdays()) thursdays.forEach(day -> person.getHatedDays().add(day));
            if (person.isHatesFridays()) fridays.forEach(day -> person.getHatedDays().add(day));
            if (person.isHatesWeekends()) {
                saturdays.forEach(day -> person.getHatedDays().add(day));
                sundays.forEach(day -> person.getHatedDays().add(day));
            }
            if (person.isHatesWeekdays()) {
                mondays.forEach(day -> person.getHatedDays().add(day));
                tuesdays.forEach(day -> person.getHatedDays().add(day));
                wednesdays.forEach(day -> person.getHatedDays().add(day));
                thursdays.forEach(day -> person.getHatedDays().add(day));
                fridays.forEach(day -> person.getHatedDays().add(day));
            }
        });

        people.entrySet().stream().forEach(entry -> entry.getValue().getHatedDays().forEach(hatedDay -> {
            Set<String> set = hated.get(hatedDay);
            set.add(entry.getKey());
            if (set.size() > people.size() - 2) {
                throw new RuntimeException("Too much people hate the same day: " +
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

    // --------------------------------------------------------------------------------------------------

    private void setWanted() {

        people.entrySet().stream().forEach(entry -> entry.getValue().getWantedDays().forEach(wantedDay -> {
            Set<String> set = scheduled.get(wantedDay);
            set.add(entry.getKey());
            if (set.size() == 2) {
                if (set.stream().allMatch(name -> people.get(name).getType(wantedDay).equals(Type.BO))) {
                    throw new RuntimeException("Two BO people want the same day: " +
                            set.stream().map(e -> e.toString() + " ").reduce("", String::concat));
                }
                if (set.stream().allMatch(name -> people.get(name).getType(wantedDay).equals(Type.FO))) {
                    throw new RuntimeException("Two FO people want the same day: " +
                            set.stream().map(e -> e.toString() + " ").reduce("", String::concat));
                }
            }
            if (set.size() > 2) {
                throw new RuntimeException("More than two people want the same day: " +
                        set.stream().map(e -> e.toString() + " ").reduce("", String::concat));
            }
        }));
    }

    // --------------------------------------------------------------------------------------------------

    private void countDays() {
        LocalDate result = localDate;
        while (result.getMonthValue() == localDate.getMonthValue()) {
            switch (result.getDayOfWeek()) {
                case MONDAY:
                    mondays.add(result.getDayOfMonth());
                    break;
                case TUESDAY:
                    tuesdays.add(result.getDayOfMonth());
                    break;
                case WEDNESDAY:
                    wednesdays.add(result.getDayOfMonth());
                    break;
                case THURSDAY:
                    thursdays.add(result.getDayOfMonth());
                    break;
                case FRIDAY:
                    fridays.add(result.getDayOfMonth());
                    break;
                case SATURDAY:
                    saturdays.add(result.getDayOfMonth());
                    break;
                case SUNDAY:
                    sundays.add(result.getDayOfMonth());
                    break;
            }
            result = result.plusDays(1);
        }
    }

    private void uniteSaturdaysAndSundays() {
        for (int i = 0; i < saturdays.size(); i++) {
            int saturdayNumber = saturdays.get(i);
            Set<String> saturday = scheduled.get(saturdayNumber);
            Set<String> sunday = scheduled.get(saturdayNumber + 1);
            Set<String> sum = new HashSet<>();
            sum.addAll(saturday);
            sum.addAll(sunday);
            if (sum.size() <= 2) {
                saturday.addAll(sunday);
                sunday.addAll(saturday);
                if (saturday.size() > 2) {
                    throw new RuntimeException("More than two people want the " + (i + 1) + ". weekend: " +
                            saturday.stream().map(e -> e.toString() + " ").reduce("", String::concat));
                }
            }
        }
    }

    private void setWeekends() {
        uniteSaturdaysAndSundays();
        Map.Entry<Integer, Set<String>> saturdayPersons = getTheMostHatedAndNotScheduledSaturday();
        while (saturdayPersons.getValue() != null) {
            int saturday = saturdayPersons.getKey();
            List<String> orderedPersons = getWeekendOrderedPossibilities(saturday);
            if (scheduled.get(saturday).isEmpty()) {
                scheduled.get(saturday).add(orderedPersons.get(0));
            }
            Person firstPerson = people.get(scheduled.get(saturday).iterator().next());
            findFirstGoodFor(firstPerson, orderedPersons, saturday);
            uniteSaturdaysAndSundays();
            saturdayPersons = getTheMostHatedAndNotScheduledSaturday();
        }
    }

    private List<String> getWeekendOrderedPossibilities(int saturdayNumber) {
        Set<String> persons = getWeekendPossibilities(saturdayNumber);
        final Map<String, Integer> scheduleNumbers = new HashMap<>();
        persons.stream().forEach(name -> scheduleNumbers.put(name, 0));
        scheduled.entrySet().stream().forEach(entry -> entry.getValue().stream().forEach(name -> {
            if (persons.contains(name)) {
                scheduleNumbers.put(name, scheduleNumbers.get(name) + 14);
                if (scheduled.get(saturdayNumber - 2).contains(name) || scheduled.get(saturdayNumber + 3).contains(name)) {
                    scheduleNumbers.put(name, scheduleNumbers.get(name) + 4);
                }
                if (scheduled.get(saturdayNumber - 3).contains(name) || scheduled.get(saturdayNumber + 4).contains(name)) {
                    scheduleNumbers.put(name, scheduleNumbers.get(name) + 2);
                }
                if (scheduled.get(saturdayNumber - 7).contains(name) || scheduled.get(saturdayNumber + 7).contains(name)) {
                    scheduleNumbers.put(name, scheduleNumbers.get(name) + 1000);
                }

                if (scheduled.get(saturdayNumber - 6).contains(name) || scheduled.get(saturdayNumber + 8).contains(name)) {
                    scheduleNumbers.put(name, scheduleNumbers.get(name) + 1000);
                }
            }
        }));
        return orderScheduleNumbers(scheduleNumbers);
    }

    private Set<String> getWeekendPossibilities(int saturdayNumber) {

        int sundayNumber = saturdayNumber + 1;
        int fridayNumber = saturdayNumber - 1;
        int mondayNumber = saturdayNumber + 2;

        final Set<String> possibilities = new HashSet<>();
        possibilities.addAll(people.keySet());

        possibilities.removeAll(hated.get(saturdayNumber));
        possibilities.removeAll(hated.get(sundayNumber));

        possibilities.removeAll(scheduled.get(fridayNumber));
        possibilities.removeAll(scheduled.get(mondayNumber));
        return possibilities;
    }

    // --------------------------------------------------------------------------------------------------

    private void setWeekdays() {
        Map.Entry<Integer, Set<String>> dayPersons = getTheMostHatedAndNotScheduledDay();
        while (dayPersons.getValue() != null) {
            //System.out.println(entry.getKey() + " " + Arrays.toString(entry.getValue().toArray()));
            List<String> orderedPersons = getTheFewestScheduledPerson(dayPersons);
            int day = dayPersons.getKey();
            if (scheduled.get(day).isEmpty()) {
                scheduled.get(day).add(orderedPersons.get(0));
            }
            Person firstPerson = people.get(scheduled.get(day).iterator().next());
            findFirstGoodFor(firstPerson, orderedPersons, day);
            dayPersons = getTheMostHatedAndNotScheduledDay();
        }
    }


    private Map.Entry<Integer, Set<String>> getTheMostHatedAndNotScheduledDay() {
        int mostHated = -1;
        int position = -1;
        Set<String> result = null;
        Set<String> result2 = null;
        for (int i = 1; i < numOfDays + 1; i++) {
            if (scheduled.get(i).size() < 2) {
                Set<String> dayHated = new HashSet<>();
                dayHated.addAll(hated.get(i));
                dayHated.addAll(scheduled.get(i));
                dayHated.addAll(scheduled.get(i - 1));
                dayHated.addAll(scheduled.get(i + 1));
                if (dayHated.size() > mostHated) {
                    mostHated = dayHated.size();
                    position = i;
                    result = dayHated;
                }
                result2 = new HashSet<>();
                result2.addAll(people.keySet());
                result2.removeAll(result);
            }
        }
        return new AbstractMap.SimpleEntry<>(position, result2);
    }

    private Map.Entry<Integer, Set<String>> getTheMostHatedAndNotScheduledSaturday() {
        int mostHated = -1;
        int position = -1;
        Set<String> result = null;
        Set<String> result2 = null;
        for (int i = 1; i < numOfDays + 1; i++) {
            if (saturdays.contains(i) && scheduled.get(i).size() < 2) {
                Set<String> dayHated = new HashSet<>();
                dayHated.addAll(hated.get(i));
                dayHated.addAll(scheduled.get(i));
                dayHated.addAll(scheduled.get(i - 1));
                dayHated.addAll(scheduled.get(i + 2));
                if (dayHated.size() > mostHated) {
                    mostHated = dayHated.size();
                    position = i;
                    result = dayHated;
                }
                result2 = new HashSet<>();
                result2.addAll(people.keySet());
                result2.removeAll(result);
            }
        }
        return new AbstractMap.SimpleEntry<>(position, result2);
    }


    private List<String> getTheFewestScheduledPerson(Map.Entry<Integer, Set<String>> dayPersons) {
        int day = dayPersons.getKey();
        Set<String> persons = dayPersons.getValue();
        final Map<String, Integer> scheduleNumbers = new HashMap<>();
        persons.stream().forEach(name -> scheduleNumbers.put(name, 0));
        scheduled.entrySet().stream().forEach(entry -> entry.getValue().stream().forEach(name -> {
            if (persons.contains(name)) {
                scheduleNumbers.put(name, scheduleNumbers.get(name) + 7);
                if (scheduled.get(day - 2).contains(name) || scheduled.get(day + 2).contains(name)) {
                    scheduleNumbers.put(name, scheduleNumbers.get(name) + 2);
                }
                if (scheduled.get(day - 3).contains(name) || scheduled.get(day + 3).contains(name)) {
                    scheduleNumbers.put(name, scheduleNumbers.get(name) + 1);
                }
            }
        }));
        scheduleNumbers.entrySet().stream().forEach(entry -> scheduleNumbers.put(entry.getKey(),
                entry.getValue() - (people.get(entry.getKey()).getManualDayDifference().get() * 7)));
        return orderScheduleNumbers(scheduleNumbers);
    }

    private List<String> orderScheduleNumbers(Map<String, Integer> scheduleNumbers) {
        final List<String> retVal = new ArrayList<>();
        while (!scheduleNumbers.isEmpty()) {
            Map.Entry<String, Integer> min = Collections.min(scheduleNumbers.entrySet(), Comparator.comparingInt(Map.Entry::getValue));
            retVal.add(min.getKey());
            scheduleNumbers.remove(min.getKey());
        }
        return retVal;
    }

    private void findFirstGoodFor(final Person person, final List<String> orderedPersons, final int day) {
        Optional<String> first = orderedPersons.stream().filter(name ->
                !people.get(name).equals(person) && people.get(name).getType(day).goodWith(person.getType(day)))
                .findFirst();
        if (first.isPresent()) {
            scheduled.get(day).add(first.get());
        } else {
            throw new RuntimeException("There is no suitable people for " + day + ". " +
                    orderedPersons.stream().map(e -> e.toString() + " ").reduce("", String::concat));
        }
    }

    // --------------------------------------------------------------------------------------------------

    public Map<Integer, Set<String>> getScheduled() {
        return scheduled;
    }
}
