package hu.finominfo.scheduler;

import hu.finominfo.scheduler.people.People;
import hu.finominfo.scheduler.scheduler.Scheduler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Iterator;

/**
 * Created by kalman.kovacs@globessey.local on 2017.12.18.
 */
public class Main {


    public static void main(String[] args) throws IOException {
        LocalDate localDate = LocalDateTime.now().toLocalDate().plusMonths(1);
        People people = new People();
        Scheduler scheduler = new Scheduler(people.getPeople(), localDate);
        scheduler.getScheduled().forEach((key, value) -> {
            if (!value.isEmpty()) {
                Iterator<String> iterator = value.iterator();
                String name1 = iterator.next();
                String name2 = iterator.next();
                String names = people.getPeople().get(name1).isExperienced() ? name1 + " - " + name2 : name2 + ", " + name1;
                System.out.println(key + " -> " + names);
            }
        });
        scheduler.getScheduled().entrySet().stream().forEach(entry ->
                entry.getValue().stream().forEach(name -> {
                    people.getPeople().get(name).getNumOfScheduled().incrementAndGet();
                    people.getPeople().get(name).getWantedDays().add(entry.getKey());
                }));
        final StringBuilder toFile = new StringBuilder();
        people.getPeople().entrySet().forEach(entry -> {
            System.out.println(entry.getKey() + " - " + entry.getValue().getNumOfScheduled().get());
            toFile.append(entry.getKey());
            entry.getValue().getWantedDays().forEach(value -> toFile.append(", w" + value));
            toFile.append(System.lineSeparator());
        });
        System.out.println(toFile.toString());
        String fileName = "schedule-" + localDate.getYear() + "-" + localDate.getMonthValue() + ".csv";
        Files.write(Paths.get(fileName), toFile.toString().getBytes("UTF-8"), StandardOpenOption.CREATE_NEW);
    }
}
