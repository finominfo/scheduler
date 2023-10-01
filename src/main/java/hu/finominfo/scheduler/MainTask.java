package hu.finominfo.scheduler;

import java.io.File;
import java.io.FileOutputStream;
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
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import hu.finominfo.scheduler.people.People;
import hu.finominfo.scheduler.people.Type;
import hu.finominfo.scheduler.scheduler.Scheduler;

public class MainTask {

    private final String[] args;
    private volatile LocalDate localDate;

    public MainTask(String[] args) {
        this.args = args;
    }

    public void make() throws IOException {
        if (args != null && args.length == 1) {
            System.out.println(args[0]);
            localDate = LocalDate.of(
                    2000 + Integer.valueOf(args[0].substring(0, 2)),
                    Integer.valueOf(args[0].substring(2)), 1);
        } else {
            localDate = LocalDateTime.now().toLocalDate().plusMonths(1);
        }

        People people = new People();
        Scheduler scheduler = new Scheduler(people.getPeople(), localDate);
        writeMonth(scheduler, people);
        writeMonthToExcel(scheduler, people);

        Map<String, AtomicInteger> allScheduledAmount = new HashMap<>();
        LocalDate previousDate = localDate;
        while (previousDate.getYear() == localDate.getYear()) {
            String fileName2 = "schedule-" + previousDate.getYear() + "-"
                    + previousDate.getMonthValue() + ".csv";
            File file = new File(fileName2);
            if (file.exists() && !file.isDirectory()) {
                String content = new String(
                        Files.readAllBytes(Paths.get(fileName2)));
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
            previousDate = previousDate.minusMonths(1);
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
        Files.write(Paths.get(fileName3), toFile3.toString().getBytes("UTF-8"),
                StandardOpenOption.CREATE);

    }

    private void writeMonth(Scheduler scheduler, People people)
            throws IOException {
        final StringBuilder toTxtFile = new StringBuilder();
        scheduler.getScheduled().forEach((key, value) -> {
            if (value.size() == 2) {
                Iterator<String> iterator = value.iterator();
                String name1 = iterator.next();
                String name2 = iterator.next();
                boolean firstFo = Type.isFirstFo(
                        people.getPeople().get(name1).getType(key),
                        people.getPeople().get(name2).getType(key));
                String names = firstFo ? name1 + " - " + name2
                        : name2 + " - " + name1;
                toTxtFile.append(key + " -> " + names);
                if (scheduler.getHolidays().contains(key)) {
                    toTxtFile.append(" - Official Holiday");
                }
                toTxtFile.append(System.lineSeparator());
            }
        });
        scheduler.getScheduled().entrySet().stream()
                .forEach(entry -> entry.getValue().stream().forEach(name -> {
                    people.getPeople().get(name).getNumOfScheduled()
                            .incrementAndGet();
                    people.getPeople().get(name).getWantedDays()
                            .add(entry.getKey());
                }));
        final StringBuilder toFile = new StringBuilder();
        people.getPeople().entrySet().forEach(entry -> {
            toTxtFile.append(entry.getKey() + " - "
                    + entry.getValue().getNumOfScheduled().get());
            toTxtFile.append(System.lineSeparator());
            toFile.append(entry.getKey());
            entry.getValue().getWantedDays()
                    .forEach(value -> toFile.append(", w" + value));
            toFile.append(System.lineSeparator());
        });
        toTxtFile.append(toFile);
        String fileNameTxt = "schedule-" + localDate.getYear() + "-"
                + localDate.getMonthValue() + ".txt";
        Files.write(Paths.get(fileNameTxt),
                toTxtFile.toString().getBytes("UTF-8"),
                StandardOpenOption.CREATE);
        String fileName = "schedule-" + localDate.getYear() + "-"
                + localDate.getMonthValue() + ".csv";
        Files.write(Paths.get(fileName), toFile.toString().getBytes("UTF-8"),
                StandardOpenOption.CREATE);
    }

    private void writeMonthToExcel(Scheduler scheduler, People people)
            throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Monthly Schedule");
        int rowNum = 0;
        // Create a header row
        Row headerRow = sheet.createRow(rowNum++);
        int colNum = 0;
        Cell cell = headerRow.createCell(colNum++);
        for (Map.Entry<Integer, Set<String>> entry : scheduler.getScheduled()
                .entrySet()) {
            cell = headerRow.createCell(colNum++);
            cell.setCellValue(entry.getKey());
        }
        for (Map.Entry<Integer, Set<String>> entry : scheduler.getScheduled()
                .entrySet()) {
            if (entry.getValue().size() == 2) {
                Iterator<String> iterator = entry.getValue().iterator();
                String name1 = iterator.next();
                String name2 = iterator.next();
                boolean firstFo = Type.isFirstFo(
                        people.getPeople().get(name1).getType(entry.getKey()),
                        people.getPeople().get(name2).getType(entry.getKey()));
                String names = firstFo ? name1 + " - " + name2
                        : name2 + " - " + name1;

                // Create a new row for each entry
                Row row = sheet.createRow(rowNum++);
                Cell dateCell = row.createCell(0);
                dateCell.setCellValue(names);
            }
        }
        // Write the workbook to a file
        String fileNameExcel = "schedule-" + localDate.getYear() + "-"
                + localDate.getMonthValue() + ".xlsx";
        try (FileOutputStream outputStream = new FileOutputStream(
                fileNameExcel)) {
            workbook.write(outputStream);
        }
        workbook.close();
    }

}
