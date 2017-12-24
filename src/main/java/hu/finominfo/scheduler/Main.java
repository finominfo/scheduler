package hu.finominfo.scheduler;

import hu.finominfo.scheduler.common.Globals;
import hu.finominfo.scheduler.people.People;
import hu.finominfo.scheduler.scheduler.Scheduler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by kalman.kovacs@globessey.local on 2017.12.18.
 */
public class Main {


    public static void main(String[] args) throws IOException {

        LocalDate localDate = LocalDateTime.now().toLocalDate().plusMonths(1);

        People people = new People();
        Scheduler scheduler = new Scheduler(people.getPeople(), localDate);
        final StringBuilder toTxtFile = new StringBuilder();
        scheduler.getScheduled().forEach((key, value) -> {
            if (!value.isEmpty()) {
                Iterator<String> iterator = value.iterator();
                String name1 = iterator.next();
                String name2 = iterator.next();
                String names = people.getPeople().get(name1).isExperienced() ? name1 + " - " + name2 : name2 + ", " + name1;
                toTxtFile.append(key + " -> " + names);
                toTxtFile.append(System.lineSeparator());
            }
        });
        scheduler.getScheduled().entrySet().stream().forEach(entry ->
                entry.getValue().stream().forEach(name -> {
                    people.getPeople().get(name).getNumOfScheduled().incrementAndGet();
                    people.getPeople().get(name).getWantedDays().add(entry.getKey());
                }));
        final StringBuilder toFile = new StringBuilder();
        people.getPeople().entrySet().forEach(entry -> {
            toTxtFile.append(entry.getKey() + " - " + entry.getValue().getNumOfScheduled().get());
            toTxtFile.append(System.lineSeparator());
            toFile.append(entry.getKey());
            entry.getValue().getWantedDays().forEach(value -> toFile.append(", w" + value));
            toFile.append(System.lineSeparator());
        });
        toTxtFile.append(toFile);
        String fileNameTxt = "schedule-" + localDate.getYear() + "-" + localDate.getMonthValue() + ".txt";
        Files.write(Paths.get(fileNameTxt), toTxtFile.toString().getBytes("UTF-8"), StandardOpenOption.CREATE);
        String fileName = "schedule-" + localDate.getYear() + "-" + localDate.getMonthValue() + ".csv";
        Files.write(Paths.get(fileName), toFile.toString().getBytes("UTF-8"), StandardOpenOption.CREATE);


        Map<String, AtomicInteger> allScheduledAmount = new HashMap<>();
        LocalDate previousDate = localDate;
        while (previousDate.getYear() == localDate.getYear()) {
            String fileName2 = "schedule-" + previousDate.getYear() + "-" + previousDate.getMonthValue() + ".csv";
            File file = new File(fileName2);
            if (file.exists() && !file.isDirectory()) {
                String content = new String(Files.readAllBytes(Paths.get(fileName2)));
                String lines[] = content.split("\\r?\\n");
                Arrays.asList(lines).stream().forEach(line -> {
                    String[] split = line.split(",");
                    String name = split[0].trim();
                    AtomicInteger value = allScheduledAmount.get(name);
                    if (value == null) {
                        value = new AtomicInteger(0);
                        allScheduledAmount.put(name, value);
                    }
                    value.addAndGet(split.length - 1);
                });
            }
            previousDate = localDate.minusMonths(1);
        }

        StringBuilder toFile3 = new StringBuilder();
        allScheduledAmount.entrySet().forEach(entry -> {
            if (entry.getValue().get() > 0) {
                toFile3.append(entry.getKey() + " - " + entry.getValue().get());
                toFile3.append(System.lineSeparator());
            }
        });
        System.out.println(toFile3.toString());
        String fileName3 = "schedule-" + localDate.getYear() + ".txt";
        Files.write(Paths.get(fileName3), toFile3.toString().getBytes("UTF-8"), StandardOpenOption.CREATE);

    }
}
