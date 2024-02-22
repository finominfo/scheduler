package hu.finominfo.scheduler.scheduler;

import hu.finominfo.scheduler.people.Person;
import hu.finominfo.scheduler.people.Type;
import hu.finominfo.scheduler.util.HungarianHolidays;

import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import hu.finominfo.scheduler.util.KeyValueStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class Scheduler {

    private static final Logger LOGGER = LogManager.getLogger(Scheduler.class);

    private final Random random = new Random(System.currentTimeMillis());

    private final Map<String, Person> people;
    private final Map<Integer, String> foNames = new HashMap<>();
    private final Map<Integer, Set<String>> scheduled = new HashMap<>();
    private final Map<Integer, Set<String>> hated = new HashMap<>();
    private final List<Integer> mondays = new ArrayList<>();
    private final List<Integer> tuesdays = new ArrayList<>();
    private final List<Integer> wednesdays = new ArrayList<>();
    private final List<Integer> thursdays = new ArrayList<>();
    private final List<Integer> fridays = new ArrayList<>();
    private final List<Integer> saturdays = new ArrayList<>();
    private final List<Integer> sundays = new ArrayList<>();
    private final List<Integer> holidays = new ArrayList<>();
    private final int numOfDays;
    private final LocalDate localDate;
    List<Type> foAbleTypes = Arrays.asList(Type.FO, Type.FO_AND_BO);

    private final KeyValueStore keyValueStore;

    public Scheduler(Map<String, Person> people, LocalDate date) {
        this.people = people;
        this.numOfDays = date.lengthOfMonth();
        for (int i = -8; i < numOfDays + 10; i++) {
            scheduled.put(i, new HashSet<>());
            hated.put(i, new HashSet<>());
        }
        this.localDate = date.withDayOfMonth(1);
        int year = date.getYear();
        Month month = date.getMonth();
        this.holidays.addAll(
                HungarianHolidays
                        .getHolidaysForMonth(year, month)
                        .stream()
                        .map(LocalDate::getDayOfMonth)
                        .collect(Collectors.toList()));

        keyValueStore = new KeyValueStore();
        //keyValueStore.createDatabase();

        countDays();
        setHated();
        setWanted();
        setDesiredNumberOfFridaysAndWeekendsAndHolidays();
        setWeekendsAndHolidays();
        setWeekdays();
        balanceIMS();
    }

    public KeyValueStore getKeyValueStore() {
        return keyValueStore;
    }



    private void balanceIMS() {
        Set<Person> foPeople = people.values().stream().filter(p -> !p.isNofo()).collect(Collectors.toSet());
        for (int i = 0; i < 10; i++) {
            Person maxIMS1 = null;
            String minIMS1 = null;
            long maxValue = 0;
            long minValue = 0;
            for (Person p : foPeople) {
                long value = getIMS1Value(p.getName());
                if (value > maxValue) {
                    maxValue = value;
                    maxIMS1 = p;
                }
            }
//            System.out.println("maxValue: " + maxValue);
            if (maxValue > 1) {
                minValue = maxValue;
                Person finalMaxIMS = maxIMS1;
                List<Map.Entry<Integer, Set<String>>> collected = scheduled.entrySet().stream().filter(e -> e.getValue().contains(finalMaxIMS.getName())).filter(e2 -> foNames.get(e2.getKey()).equals(finalMaxIMS.getName())).collect(Collectors.toList());
//                System.out.println("collected: " + collected.size());
                int position = -1;
                for (Map.Entry<Integer, Set<String>> entry : collected) {
                    if (!finalMaxIMS.getType(entry.getKey()).equals(Type.FO)) {
                        Iterator<String> iterator = entry.getValue().iterator();
                        String name1 = iterator.next();
                        String name2 = iterator.next();
                        String name = finalMaxIMS.getName().equals(name1) ? name2 : name1;
                        Person person = people.values().stream().filter(p -> p.getName().equals(name)).findAny().orElse(null);
                        if (person != null && !person.getType(entry.getKey()).equals(Type.BO) && getIMS1Value(name) < minValue) {
                            minValue = getIMS1Value(name);
                            minIMS1 = name;
                            position = entry.getKey();
                        }
                    }
                }
                if (maxValue - minValue > 1) {
                    foNames.put(position, minIMS1);
                }
            }
        }
    }

    private long getIMS1Value(String name) {
        long countAll = scheduled.values().stream().filter(s -> s.contains(name)).count();
        long count = foNames.values().stream().filter(name1 -> name1.equals(name)).count();
        long value = count * 2 - countAll;
        return value;
    }

    public List<Integer> getHolidays() {
        return holidays;
    }

    public List<Integer> getSaturdays() {
        return saturdays;
    }

    public List<Integer> getSundays() {
        return sundays;
    }

    public LocalDate getDate() {
        return localDate;
    }

    public int getNumOfDays() {
        return numOfDays;
    }

    // --------------------------------------------------------------------------------------------------

    private void setHated() {
        people
                .entrySet()
                .stream()
                .forEach(entry -> {
                    Person person = entry.getValue();
                    if (person.isHatesMondays())
                        mondays.forEach(day -> person.getHatedDays().add(day));
                    if (person.isHatesTuesdays())
                        tuesdays.forEach(day -> person.getHatedDays().add(day));
                    if (person.isWantedTuesdays())
                        tuesdays.forEach(day -> person.getWantedDays().add(day));
                    if (person.isHatesWednesdays())
                        wednesdays.forEach(day -> person.getHatedDays().add(day));
                    if (person.isHatesThursdays())
                        thursdays.forEach(day -> person.getHatedDays().add(day));
                    if (person.isHatesFridays())
                        fridays.forEach(day -> person.getHatedDays().add(day));
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

        people
                .entrySet()
                .stream()
                .forEach(entry -> entry
                        .getValue()
                        .getHatedDays()
                        .forEach(hatedDay -> {
                            Set<String> set = hated.get(hatedDay);
                            set.add(entry.getKey());
                            if (set.size() > people.size() - 2) {
                                throw new RuntimeException(
                                        "Too much people hate the same day: " +
                                                set
                                                        .stream()
                                                        .map(e -> e.toString() + " ")
                                                        .reduce("", String::concat));
                            }
                        }));
        hated
                .entrySet()
                .stream()
                .forEach(entry -> {
                    Set<String> set = entry.getValue();
                    if (set.size() == people.size() - 2) { // Only 2 person remain for
                        // that day
                        final Set<String> possibleNames = new HashSet<>();
                        possibleNames.addAll(people.keySet());
                        possibleNames.removeAll(set);
                        scheduled.get(entry.getKey()).addAll(possibleNames);
                        if (scheduled.get(entry.getKey()).size() == 2) {
                            foNames.put(entry.getKey(), selectFo(scheduled.get(entry.getKey()), entry.getKey()));
                        }
                        LOGGER.info(
                                possibleNames
                                        .stream()
                                        .map(e -> e.toString() + " ")
                                        .reduce("", String::concat) +
                                        " were added to " +
                                        entry.getKey() +
                                        ", because everybody else hate that day.");
                    }
                });
    }

    private String selectFo(Set<String> names, int day) {
        Iterator<String> iterator = names.iterator();
        String name1 = iterator.next();
        String name2 = iterator.next();
        Person person1 = people.values().stream().filter(p -> p.getName().equals(name1)).filter(p2 -> p2.isNofo()).findAny().orElse(null);
        Person person2 = people.values().stream().filter(p -> p.getName().equals(name2)).filter(p2 -> p2.isNofo()).findAny().orElse(null);
        if (person1 == null && person2 != null) {
            return name1;
        }
        if (person1 != null && person2 == null) {
            return name2;
        }
        if (person1 != null && person2 != null) {
            throw new RuntimeException("Both people are nofo! " + name1 + " - " + name2);
        }
        person1 = people.values().stream().filter(p -> p.getName().equals(name1)).findAny().orElse(null);
        person2 = people.values().stream().filter(p -> p.getName().equals(name2)).findAny().orElse(null);
        if (person1.getType(day).equals(Type.BO) && person2.getType(day).equals(Type.FO)) {
            return name2;
        }
        if (person1.getType(day).equals(Type.FO) && person2.getType(day).equals(Type.BO)) {
            return name1;
        }
        if (person1.getType(day).equals(Type.FO) && person2.getType(day).equals(Type.FO)) {
            LOGGER.error("Both people wants to be IMS1 on the same day " + names);
            System.exit(0);
        }
        if (person1.getType(day).equals(Type.BO) && person2.getType(day).equals(Type.BO)) {
            LOGGER.error("Both people wants to be IMS2 on the same day " + names);
            System.exit(0);
        }
        return getIMS1Value(name1) < getIMS1Value(name2) ? name1 : name2;
        //return random.nextInt(2) == 0 ? name1 : name2;
    }
    // --------------------------------------------------------------------------------------------------

    private void setWanted() {
    people
                .entrySet()
                .stream()
                .forEach(entry -> entry
                        .getValue()
                        .getWantedDays()
                        .forEach(wantedDay -> {
                            Set<String> set = scheduled.get(wantedDay);
                            set.add(entry.getKey());
                            if (set.size() > 2) {
                                LOGGER.error("More than two people want the same day: " + set);
                                System.exit(1);
                            }
                            if (set.size() == 2) {
                                if (set
                                        .stream()
                                        .allMatch(name -> people.get(name).getType(wantedDay).equals(Type.BO))) {
                                    throw new RuntimeException(
                                            "Two BO people want the same day: " +
                                                    set
                                                            .stream()
                                                            .map(e -> e.toString() + " ")
                                                            .reduce("", String::concat));
                                }
                                if (set
                                        .stream()
                                        .allMatch(name -> people.get(name).getType(wantedDay).equals(Type.FO))) {
                                    throw new RuntimeException(
                                            "Two FO people want the same day: " +
                                                    set
                                                            .stream()
                                                            .map(e -> e.toString() + " ")
                                                            .reduce("", String::concat));
                                }
                                foNames.put(wantedDay, selectFo(set, wantedDay));
                            }
                            if (set.size() > 2) {
                                throw new RuntimeException(
                                        "More than two people want the same day: " +
                                                set
                                                        .stream()
                                                        .map(e -> e.toString() + " ")
                                                        .reduce("", String::concat));
                            }
                        }));
    }

    // --------------------------------------------------------------------------------------------------

    private void setDesiredNumberOfFridaysAndWeekendsAndHolidays() {
        people
                .entrySet()
                .stream()
                .forEach(entry -> {
                    Person person = entry.getValue();
                    final AtomicInteger num = new AtomicInteger(person.getNumOfWantedHolidays());
                    if (num.get() > 0) {
                        scheduleDesiredDays(person, num, holidays, "holiday");
                    }
                    num.set(person.getNumOfWantedFridays());
                    if (num.get() > 0) {
                        scheduleDesiredDays(person, num, fridays, "friday");
                    }
                    num.set(person.getNumOfWantedSaturdays());
                    if (num.get() > 0) {
                        scheduleDesiredDays(person, num, saturdays, "saturday");
                    }
                    num.set(person.getNumOfWantedSundays());
                    if (num.get() > 0) {
                        scheduleDesiredDays(person, num, sundays, "sunday");
                    }
                });
    }

    private void scheduleDesiredDays(Person person, final AtomicInteger num, List<Integer> specialDays, String dayName) {
        List<Integer> possibleDays1 = new ArrayList<>();
        specialDays
            .stream()
            .filter(day -> scheduled.get(day).contains(person.getName()))
            .forEach(day -> num.decrementAndGet()); // already scheduled
        specialDays
            .stream()
            .filter(day -> !scheduled.get(day).contains(person.getName()))
            .forEach(possibleDays1::add);
        List<Integer> possibleDays = new ArrayList<>();
        possibleDays1
            .stream()
            .filter(day -> scheduled.get(day).size() < 2)
            .forEach(possibleDays::add);
        if (num.get() > 0) {
            if (possibleDays.size() < num.get()) {
                throw new RuntimeException(
                        "Not enough days for " + person.getName() + " to work on " + dayName + "s.");
            }
            do {
                int day = possibleDays.get(random.nextInt(possibleDays.size()));
                if (!scheduled.get(day).contains(person.getName())) {
                    scheduled.get(day).add(person.getName());
                    if (getFoNames().get(day) == null || getFoNames().get(day).isEmpty()) {
                        foNames.put(day, person.getName());
                    }
                    num.decrementAndGet();
                }
            } while(num.get() > 0);
        }
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

    private void setWeekendsAndHolidays() {
        // uniteSaturdaysAndSundays();
        for (List<Integer> days : Arrays.asList(saturdays, sundays, holidays)) {
            Map.Entry<Integer, Set<String>> dayPersons = getTheMostHatedAndNotScheduled(
                    days);
            while (dayPersons.getValue() != null) {
                int day = dayPersons.getKey();
                List<String> orderedPersons = getWeekendOrderedPossibilities(day);
                if (scheduled.get(day).isEmpty()) {
                    scheduled.get(day).add(orderedPersons.get(0));
                }
                Person firstPerson = people.get(scheduled.get(day).iterator().next());
                findFirstGoodFor(firstPerson, orderedPersons, day);
                if (scheduled.get(day).size() == 2) {
                    foNames.put(day, selectFo(scheduled.get(day), day));
                }
                // uniteSaturdaysAndSundays();
                dayPersons = getTheMostHatedAndNotScheduled(days);
            }
        }
    }

    private List<String> getWeekendOrderedPossibilities(int saturdayNumber) {
        Set<String> persons = getWeekendPossibilities(saturdayNumber);
        final Map<String, Integer> scheduleNumbers = new HashMap<>();
        persons.stream().forEach(name -> scheduleNumbers.put(name, 0));
        scheduled
                .entrySet()
                .stream()
                .forEach(entry -> entry
                        .getValue()
                        .stream()
                        .forEach(name -> {
                            if (persons.contains(name)) {
                                scheduleNumbers.put(name, scheduleNumbers.get(name) + 14);
                                if (scheduled.get(saturdayNumber - 2).contains(name) ||
                                        scheduled.get(saturdayNumber + 3).contains(name)) {
                                    scheduleNumbers.put(name, scheduleNumbers.get(name) + 4);
                                }
                                if (scheduled.get(saturdayNumber - 3).contains(name) ||
                                        scheduled.get(saturdayNumber + 4).contains(name)) {
                                    scheduleNumbers.put(name, scheduleNumbers.get(name) + 2);
                                }
                                if (scheduled.get(saturdayNumber - 7).contains(name) ||
                                        scheduled.get(saturdayNumber + 7).contains(name)) {
                                    scheduleNumbers.put(name, scheduleNumbers.get(name) + 1000);
                                }

                                if (scheduled.get(saturdayNumber - 6).contains(name) ||
                                        scheduled.get(saturdayNumber + 8).contains(name)) {
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
            // System.out.println(entry.getKey() + " " +
            // Arrays.toString(entry.getValue().toArray()));
            List<String> orderedPersons = getTheFewestScheduledPerson(dayPersons);
            int day = dayPersons.getKey();
            if (scheduled.get(day).isEmpty()) {
                scheduled.get(day).add(orderedPersons.get(0));
            }
            Person firstPerson = people.get(scheduled.get(day).iterator().next());
            findFirstGoodFor(firstPerson, orderedPersons, day);
            if (scheduled.get(day).size() == 2) {
                foNames.put(day, selectFo(scheduled.get(day), day));
            }
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

    private Map.Entry<Integer, Set<String>> getTheMostHatedAndNotScheduled(
            List<Integer> days) {
        int mostHated = -1;
        int position = -1;
        Set<String> result = null;
        Set<String> result2 = null;
        for (int i = 1; i < numOfDays + 1; i++) {
            if (days.contains(i) && scheduled.get(i).size() < 2) {
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

    private List<String> getTheFewestScheduledPerson(
            Map.Entry<Integer, Set<String>> dayPersons) {
        int day = dayPersons.getKey();
        Set<String> persons = dayPersons.getValue();
        final Map<String, Integer> scheduleNumbers = new HashMap<>();
        persons.stream().forEach(name -> scheduleNumbers.put(name, 0));
        scheduled
                .entrySet()
                .stream()
                .forEach(entry -> entry
                        .getValue()
                        .stream()
                        .forEach(name -> {
                            if (persons.contains(name)) {
                                scheduleNumbers.put(name, scheduleNumbers.get(name) + 7);
                                if (scheduled.get(day - 2).contains(name) ||
                                        scheduled.get(day + 2).contains(name)) {
                                    scheduleNumbers.put(name, scheduleNumbers.get(name) + 2);
                                }
                                if (scheduled.get(day - 3).contains(name) ||
                                        scheduled.get(day + 3).contains(name)) {
                                    scheduleNumbers.put(name, scheduleNumbers.get(name) + 1);
                                }
                            }
                        }));
        scheduleNumbers
                .entrySet()
                .stream()
                .forEach(entry -> scheduleNumbers.put(
                        entry.getKey(),
                        entry.getValue() -
                                (people.get(entry.getKey()).getManualDayDifference().get() * 7)));
        return orderScheduleNumbers(scheduleNumbers);
    }

    private List<String> orderScheduleNumbers(
            Map<String, Integer> scheduleNumbers) {
        final List<String> retVal = new ArrayList<>();
        while (!scheduleNumbers.isEmpty()) {
            Map.Entry<String, Integer> min = Collections.min(
                    scheduleNumbers.entrySet(),
                    Comparator.comparingInt(Map.Entry::getValue));
            retVal.add(min.getKey());
            scheduleNumbers.remove(min.getKey());
        }
        return retVal;
    }

    private void findFirstGoodFor(
            final Person person,
            final List<String> orderedPersons,
            final int day) {
        Optional<String> first = orderedPersons
                .stream()
                .filter(name -> !people.get(name).equals(person) &&
                        people.get(name).getType(day).goodWith(person.getType(day)))
                .findFirst();
        if (first.isPresent()) {
            scheduled.get(day).add(first.get());
        } else {
            String strPerson = orderedPersons
                                    .stream()
                                    .map(e -> e.toString() + " ")
                                    .reduce("", String::concat);
            String message = orderedPersons.isEmpty() ? "I found no person for " + day : "I found only one person (" + strPerson + ") for " + day;
            throw new RuntimeException(message);
        }
    }

    // --------------------------------------------------------------------------------------------------

    public Map<Integer, Set<String>> getScheduled() {
        return scheduled;
    }

    public Map<Integer, String> getFoNames() {
        return foNames;
    }

    public LocalDate getLocalDate() {
        return localDate;
    }
}
