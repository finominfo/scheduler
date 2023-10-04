package hu.finominfo.scheduler;

import hu.finominfo.scheduler.people.People;
import hu.finominfo.scheduler.people.Type;
import hu.finominfo.scheduler.scheduler.Scheduler;
import hu.finominfo.scheduler.util.ExcelExporter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

public class MainTask {


    private static final Logger LOGGER = LogManager.getLogger(MainTask.class);
    private final String[] args;
    private volatile LocalDate localDate;

    public MainTask(String[] args) {
        this.args = args;
    }

    public void make() throws IOException {
        if (args != null && args.length == 1) {
            //LOGGER.info(args[0]);
            localDate = LocalDate.of(
                    2000 + Integer.valueOf(args[0].substring(0, 2)),
                    Integer.valueOf(args[0].substring(2)),
                    1);
        } else {
            localDate = LocalDateTime.now().toLocalDate().plusMonths(1);
        }

        People people = new People();
        Scheduler scheduler = new Scheduler(people.getPeople(), localDate);
        writeMonth(scheduler, people);
        ExcelExporter excelExporter = new ExcelExporter(scheduler, people, localDate);
        excelExporter.writeMonthToExcel();

        Map<String, AtomicInteger> allScheduledAmount = new HashMap<>();
        LocalDate previousDate = localDate;
        while (previousDate.getYear() == localDate.getYear()) {
            String fileName2 = "schedule-" +
                    previousDate.getYear() +
                    "-" +
                    previousDate.getMonthValue() +
                    ".csv";
            File file = new File(fileName2);
            if (file.exists() && !file.isDirectory()) {
                String content = new String(Files.readAllBytes(Paths.get(fileName2)), "UTF-8");
                String lines[] = content.split("\\r?\\n");
                Arrays
                        .asList(lines)
                        .stream()
                        .forEach(line -> {
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
            previousDate = previousDate.minusMonths(1);
        }

        StringBuilder toFile3 = new StringBuilder();
        allScheduledAmount
                .entrySet()
                .forEach(entry -> {
                    if (entry.getValue().get() > 0) {
                        toFile3.append(entry.getKey() + " - " + entry.getValue().get());
                        toFile3.append(System.lineSeparator());
                    }
                });
        //LOGGER.info(toFile3.toString());
        String fileName3 = "schedule-" + localDate.getYear() + ".txt";
        Files.write(
                Paths.get(fileName3),
                toFile3.toString().getBytes("UTF-8"),
                StandardOpenOption.CREATE);
    }

    public void writeMonth(Scheduler scheduler, People people)
            throws IOException {
        final StringBuilder toTxtFile = new StringBuilder();
        scheduler
                .getScheduled()
                .forEach((key, value) -> {
                    if (value.size() == 2) {
                        Iterator<String> iterator = value.iterator();
                        String name1 = iterator.next();
                        String name2 = iterator.next();
                        boolean firstFo = Type.isFirstFo(
                                people.getPeople().get(name1).getType(key),
                                people.getPeople().get(name2).getType(key));
                        String names = firstFo
                                ? name1 + " - " + name2
                                : name2 + " - " + name1;
                        toTxtFile.append(key + " -> " + names);
                        if (scheduler.getHolidays().contains(key)) {
                            toTxtFile.append(" - Official Holiday");
                        }
                        toTxtFile.append(System.lineSeparator());
                    }
                });
        scheduler
                .getScheduled()
                .entrySet()
                .stream()
                .forEach(entry -> entry
                        .getValue()
                        .stream()
                        .forEach(name -> {
                            people.getPeople().get(name).getNumOfScheduled().incrementAndGet();
                            people.getPeople().get(name).getWantedDays().add(entry.getKey());
                        }));
        final StringBuilder toFile = new StringBuilder();
        people
                .getPeople()
                .entrySet()
                .forEach(entry -> {
                    toTxtFile.append(
                            entry.getKey() + " - " + entry.getValue().getNumOfScheduled().get());
                    toTxtFile.append(System.lineSeparator());
                    toFile.append(entry.getKey());
                    entry
                            .getValue()
                            .getWantedDays()
                            .forEach(value -> toFile.append(", w" + value));
                    toFile.append(System.lineSeparator());
                });
        toTxtFile.append(toFile);
        String fileNameTxt = "schedule-" +
                localDate.getYear() +
                "-" +
                localDate.getMonthValue() +
                ".txt";
        Files.write(
                Paths.get(fileNameTxt),
                toTxtFile.toString().getBytes("UTF-8"),
                StandardOpenOption.CREATE);
        String fileName = "schedule-" +
                localDate.getYear() +
                "-" +
                localDate.getMonthValue() +
                ".csv";
        Files.write(
                Paths.get(fileName),
                toFile.toString().getBytes("UTF-8"),
                StandardOpenOption.CREATE);
    }


}
