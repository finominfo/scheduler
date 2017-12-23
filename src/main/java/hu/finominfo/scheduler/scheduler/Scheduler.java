package hu.finominfo.scheduler.scheduler;

import hu.finominfo.scheduler.people.Person;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

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
    private volatile boolean weekendIsScheduledNow = false;

    public Scheduler(Map<String, Person> people, LocalDate date) {
        this.people = people;
        this.numOfDays = date.lengthOfMonth();
        for (int i = -4; i < numOfDays + 5; i++) {
            scheduled.put(i, new HashSet<>());
            hated.put(i, new HashSet<>());
        }
        this.localDate = date.withDayOfMonth(1);
        countDays();
        setHated();
        setWanted();
        setWeekends();
        //if (!weekendIsScheduledNow) {
        setWeekdays();
        //}
        scheduled.forEach((key, value) -> {
            if (!value.isEmpty()) {
                System.out.println(key + " -> " + value);
            }
        });
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
                if (set.stream().allMatch(name -> !people.get(name).isExperienced())) {
                    throw new RuntimeException("Two not experienced people want the same day: " +
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
            saturday.addAll(sunday);
            sunday.addAll(saturday);
            if (saturday.size() > 2) {
                throw new RuntimeException("More than two people want the " + (i + 1) + ". weekend: " +
                        saturday.stream().map(e -> e.toString() + " ").reduce("", String::concat));
            }
        }
    }

    private void setWeekends() {
        uniteSaturdaysAndSundays();
        setMostCriticalWeekends();
        uniteSaturdaysAndSundays();
        setRemainingWeekends();
    }

    private void setMostCriticalWeekends() {
        final List<Set<String>> possibilitiesOfWeekends = new ArrayList<>();
        final List<Integer> sizeOfPossibilitiesOfWeekends = new ArrayList<>();

        for (int i = 0; i < saturdays.size(); i++) {
            int saturdayNumber = saturdays.get(i);
            Set<String> saturday = scheduled.get(saturdayNumber);
            final Set<String> possibilities = getPossibilities(saturdayNumber);
            possibilitiesOfWeekends.add(possibilities);
            if (saturday.size() + possibilities.size() < 2) {
                throw new RuntimeException("There is not enough people on " + saturdayNumber + ". " +
                        saturday.stream().map(e -> e.toString() + " ").reduce("", String::concat));
            } else if (saturday.size() + possibilities.size() == 2) {
                saturday.addAll(possibilities);
                if (saturday.stream().allMatch(name -> !people.get(name).isExperienced())) {
                    throw new RuntimeException("Two not experienced people on the same weekend: " +
                            saturday.stream().map(e -> e.toString() + " ").reduce("", String::concat));
                }
                sizeOfPossibilitiesOfWeekends.add(1000);
            } else {
                sizeOfPossibilitiesOfWeekends.add(possibilities.size());
            }
        }
        List<Integer> threeLowestSizeOfPossibilities = getThreeLowest(sizeOfPossibilitiesOfWeekends);
        Set<String> saturdayWas = null;
        int iWas = -10;

        if (threeLowestSizeOfPossibilities.get(0) < threeLowestSizeOfPossibilities.get(2)) {
            for (int i = 0; i < sizeOfPossibilitiesOfWeekends.size(); i++) {
                if (threeLowestSizeOfPossibilities.get(0) == sizeOfPossibilitiesOfWeekends.get(i)) {
                    int saturdayNumber = saturdays.get(i);
                    Set<String> saturday = scheduled.get(saturdayNumber);
                    final Set<String> possibilities = getPossibilities(saturdayNumber);
                    thereIsPossibleNames(possibilities, saturday);
                    iWas = i;
                    saturdayWas = saturday;
                    sizeOfPossibilitiesOfWeekends.set(i, 1000);
                    //System.out.println("1 weekend was set: " + saturdayNumber);
                    break;
                }
            }
        }

        if (threeLowestSizeOfPossibilities.get(1) < threeLowestSizeOfPossibilities.get(2)) {
            for (int i = 0; i < sizeOfPossibilitiesOfWeekends.size(); i++) {
                if (threeLowestSizeOfPossibilities.get(1) == sizeOfPossibilitiesOfWeekends.get(i)) {
                    int saturdayNumber = saturdays.get(i);
                    Set<String> saturday = scheduled.get(saturdayNumber);
                    final Set<String> possibilities = getPossibilities(saturdayNumber);
                    if (Math.abs(iWas - i) == 1) {
                        possibilities.removeAll(saturdayWas);
                    }
                    thereIsPossibleNames(possibilities, saturday);
                    //System.out.println("2 weekend was set: " + saturdayNumber);
                    break;
                }
            }
        }
    }

    private List<Integer> getThreeLowest(List<Integer> list) {
        int[] lowestValues = new int[3];
        Arrays.fill(lowestValues, Integer.MAX_VALUE);
        for (int n : list) {
            if (n < lowestValues[2]) {
                lowestValues[2] = n;
                Arrays.sort(lowestValues);
            }
        }
        List<Integer> intList = new ArrayList<Integer>();
        for (int index = 0; index < lowestValues.length; index++) {
            intList.add(lowestValues[index]);
        }
        return intList;
    }

    private void setRemainingWeekends() {
        for (int i = 0; i < saturdays.size(); i++) {
            int saturdayNumber = saturdays.get(i);
            Set<String> saturday = scheduled.get(saturdayNumber);
            while (saturday.size() < 2) {
                weekendIsScheduledNow = true;
                final Set<String> possibilities = getPossibilities(saturdayNumber);
                if (possibilities.isEmpty()) {
                    noPossibleNames(i, saturday);
                } else {
                    thereIsPossibleNames(possibilities, saturday);
                }
                uniteSaturdaysAndSundays();
            }
        }
    }

    private Set<String> getPossibilities(int saturdayNumber) {

        int sundayNumber = saturdayNumber + 1;
        int fridayNumber = saturdayNumber - 1;
        int mondayNumber = saturdayNumber + 2;

        final Set<String> possibilities = new HashSet<>();
        possibilities.addAll(people.keySet());
        saturdays.stream().forEach(satNum -> possibilities.removeAll(scheduled.get(satNum)));

        possibilities.removeAll(hated.get(saturdayNumber));
        possibilities.removeAll(hated.get(sundayNumber));

        possibilities.removeAll(scheduled.get(fridayNumber));
        possibilities.removeAll(scheduled.get(mondayNumber));
        return possibilities;
    }

    private void noPossibleNames(int i, Set<String> saturday) {
        int otherSaturdayNumber = getOtherSaturdayNumber(i);
        Set<String> otherSaturday = scheduled.get(otherSaturdayNumber);
        if (saturday.isEmpty()) {
            saturday.addAll(otherSaturday);
        } else {
            if (people.get(saturday.iterator().next()).isExperienced()) {
                saturday.add(otherSaturday.iterator().next());
            } else {
                Optional<String> anyExperienced = otherSaturday.stream().filter(name -> people.get(name).isExperienced()).findAny();
                if (anyExperienced.isPresent()) {
                    saturday.add(anyExperienced.get());
                } else {
                    throw new RuntimeException("There is no experienced people on " + otherSaturdayNumber + ". " +
                            otherSaturday.stream().map(e -> e.toString() + " ").reduce("", String::concat));
                }
            }
        }
    }

    //TODO: Make it more sophisticated!!!
    private int getOtherSaturdayNumber(int i) {
        int otherSaturdayNumber = saturdays.get(i < saturdays.size() / 2 ? i + 2 : i - 2);
        if (scheduled.get(otherSaturdayNumber).size() < 2) {
            otherSaturdayNumber = saturdays.get(i == 0 ? 3 : i == 3 ? 0 : i < saturdays.size() / 2 ? i + 1 : i - 1);
        }
        return otherSaturdayNumber;
    }

    private void thereIsPossibleNames(Set<String> possibilities, Set<String> saturday) {
        if (saturday.isEmpty()) {
            try2AddNoFoFirst(saturday, possibilities);
        } else {
            if (people.get(saturday.iterator().next()).isExperienced()) {
                try2AddNoFoFirst(saturday, possibilities);
            } else {
                try2AddExperienced(saturday, possibilities);
            }
        }
    }

    private void try2AddNoFoFirst(Set<String> saturday, Set<String> possibilities) {
        Optional<String> anyNoFo = possibilities.stream().filter(name -> !people.get(name).isExperienced()).findAny();
        if (anyNoFo.isPresent()) {
            saturday.add(anyNoFo.get());
        } else {
            try2AddExperienced(saturday, possibilities);
        }
    }

    private void try2AddExperienced(Set<String> saturday, Set<String> possibilities) {
        Optional<String> anyExperienced = possibilities.stream().filter(name -> people.get(name).isExperienced()).findAny();
        if (anyExperienced.isPresent()) {
            saturday.add(anyExperienced.get());
        } else {
            throw new RuntimeException("There is no experienced people in possibleNames " +
                    possibilities.stream().map(e -> e.toString() + " ").reduce("", String::concat));
        }
    }

    // --------------------------------------------------------------------------------------------------

    private void setWeekdays() {
        Map.Entry<Integer, Set<String>> dayPersons = getTheMostHatedAndNotScheduledDay();
        while (dayPersons.getValue() != null) {
            //System.out.println(entry.getKey() + " " + Arrays.toString(entry.getValue().toArray()));
            List<String> orderedPersons = getTheFewestScheduledPerson(dayPersons);
            if (scheduled.get(dayPersons.getKey()).isEmpty()) {
                scheduled.get(dayPersons.getKey()).add(orderedPersons.get(0));
                if (people.get(orderedPersons.get(0)).isExperienced()) {
                    scheduled.get(dayPersons.getKey()).add(orderedPersons.get(1));
                } else {
                    findFirstExperienced(dayPersons, orderedPersons);
                }
            } else {
                if (people.get(scheduled.get(dayPersons.getKey()).iterator().next()).isExperienced()) {
                    findFirstNotTheSame(dayPersons, orderedPersons);
                } else {
                    findFirstExperienced(dayPersons, orderedPersons);
                }
            }
            //System.out.println(Arrays.toString(scheduled.get(entry.getKey()).toArray()));
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

    private List<String> getTheFewestScheduledPerson(Map.Entry<Integer, Set<String>> dayPersons) {
        int day = dayPersons.getKey();
        Set<String> persons = dayPersons.getValue();
        final List<String> retVal = new ArrayList<>();
        final Map<String, Integer> scheduleNumbers = new HashMap<>();
        persons.stream().forEach(name -> scheduleNumbers.put(name, 0));
        scheduled.entrySet().stream().forEach(entry -> entry.getValue().stream().forEach(name -> {
            if (persons.contains(name)) {
                scheduleNumbers.put(name, scheduleNumbers.get(name) + 4);
                //System.out.println("day: " + day);
                if (scheduled.get(day - 2).contains(name) || scheduled.get(day + 2).contains(name)) {
                    scheduleNumbers.put(name, scheduleNumbers.get(name) + 2);
                }
                if (scheduled.get(day - 3).contains(name) || scheduled.get(day + 3).contains(name)) {
                    scheduleNumbers.put(name, scheduleNumbers.get(name) + 1);
                }
            }
        }));
        while (!scheduleNumbers.isEmpty()) {
            Map.Entry<String, Integer> min = Collections.min(scheduleNumbers.entrySet(), Comparator.comparingInt(Map.Entry::getValue));
            retVal.add(min.getKey());
            scheduleNumbers.remove(min.getKey());
        }
        return retVal;
    }

    private void findFirstExperienced(Map.Entry<Integer, Set<String>> entry, List<String> orderedPersons) {
        Optional<String> first = orderedPersons.stream().filter(name -> people.get(name).isExperienced()
                && !scheduled.get(entry.getKey()).contains(name)).findFirst();
        if (first.isPresent()) {
            scheduled.get(entry.getKey()).add(first.get());
        } else {
            throw new RuntimeException("There is no experienced people on " + entry.getKey() + ". " +
                    orderedPersons.stream().map(e -> e.toString() + " ").reduce("", String::concat));
        }
    }

    private void findFirstNotTheSame(Map.Entry<Integer, Set<String>> entry, List<String> orderedPersons) {
        Optional<String> first = orderedPersons.stream().filter(name -> !scheduled.get(entry.getKey()).contains(name)).findFirst();
        if (first.isPresent()) {
            scheduled.get(entry.getKey()).add(first.get());
        } else {
            throw new RuntimeException("There is not enogh people on " + entry.getKey() + ". " +
                    orderedPersons.stream().map(e -> e.toString() + " ").reduce("", String::concat));
        }
    }

    // --------------------------------------------------------------------------------------------------

    public Map<Integer, Set<String>> getScheduled() {
        return scheduled;
    }
}
