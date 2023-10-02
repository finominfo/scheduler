package hu.finominfo.scheduler;

import hu.finominfo.scheduler.people.People;
import hu.finominfo.scheduler.people.Person;
import hu.finominfo.scheduler.people.Type;
import hu.finominfo.scheduler.scheduler.Scheduler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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
                    Integer.valueOf(args[0].substring(2)),
                    1);
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
            String fileName2 = "schedule-" +
                    previousDate.getYear() +
                    "-" +
                    previousDate.getMonthValue() +
                    ".csv";
            File file = new File(fileName2);
            if (file.exists() && !file.isDirectory()) {
                String content = new String(Files.readAllBytes(Paths.get(fileName2)));
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
        System.out.println(toFile3.toString());
        String fileName3 = "schedule-" + localDate.getYear() + ".txt";
        Files.write(
                Paths.get(fileName3),
                toFile3.toString().getBytes("UTF-8"),
                StandardOpenOption.CREATE);
    }

    private void writeMonth(Scheduler scheduler, People people)
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

    private void writeMonthToExcel(Scheduler scheduler, People people)
            throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Monthly Schedule");

        int rowNum = 0;
        int colNum = 0;

        // Create a header row with a different color
        Row headerRow = sheet.createRow(rowNum++);
        CellStyle topLeftCellStyle = getCellStyle(
                workbook,
                IndexedColors.BLACK1.getIndex(),
                FillPatternType.DIAMONDS);
        CellStyle lightGreyStyle = getCellStyle(
                workbook,
                IndexedColors.GREY_25_PERCENT);
        CellStyle headerCellStyle = getCellStyle(
                workbook,
                IndexedColors.LIGHT_BLUE);
        CellStyle headerRedCellStyle = getCellStyle(workbook, IndexedColors.ORANGE);
        CellStyle dataCellStyle = getCellStyle(workbook, IndexedColors.YELLOW);
        CellStyle greenStyle = getCellStyle(workbook, IndexedColors.LIGHT_GREEN);

        Cell cell = headerRow.createCell(colNum++);
        LocalDate ld = scheduler.getDate();
        cell.setCellValue(ld.getYear() + " " + ld.getMonth().name());
        cell.setCellStyle(topLeftCellStyle);

        List<Integer> weekendsAndHolidays = new ArrayList<>();
        weekendsAndHolidays.addAll(scheduler.getSaturdays());
        weekendsAndHolidays.addAll(scheduler.getSundays());
        weekendsAndHolidays.addAll(scheduler.getHolidays());

        sheet.setColumnWidth(0, 20 * 256);
        for (int i = 1; i <= scheduler.getNumOfDays(); i++) {
            cell = headerRow.createCell(colNum++);
            cell.setCellValue(i);
            if (weekendsAndHolidays.contains(i)) {
                cell.setCellStyle(headerRedCellStyle);
            } else {
                cell.setCellStyle(headerCellStyle);
            }
            sheet.setColumnWidth(i, (int) (4.5 * 256));
        }

        List<String> names = people
                .getPeople()
                .values()
                .stream()
                .map(Person::getName)
                .sorted()
                .collect(Collectors.toList());

        Row row = sheet.createRow(rowNum++);
        colNum = 0;
        Cell dateCell = row.createCell(colNum++);
        for (int i = 1; i <= scheduler.getNumOfDays(); i++) {
            dateCell = row.createCell(colNum++);
            dateCell.setCellValue(
                    localDate
                            .withDayOfMonth(i)
                            .getDayOfWeek()
                            .name()
                            .toUpperCase()
                            .substring(0, 3));
            dateCell.setCellStyle(
                    weekendsAndHolidays.contains(i) ? headerRedCellStyle : lightGreyStyle);
        }

        for (String name : names) {
            List<Integer> places = scheduler
                    .getScheduled()
                    .entrySet()
                    .stream()
                    .filter(e -> e.getValue().contains(name))
                    .map(Entry::getKey)
                    .sorted()
                    .collect(Collectors.toList());
            row = sheet.createRow(rowNum++);
            colNum = 0;
            dateCell = row.createCell(colNum++);
            dateCell.setCellValue(name);
            dateCell.setCellStyle(dataCellStyle);
            for (int i = 1; i <= scheduler.getNumOfDays(); i++) {
                cell = row.createCell(colNum++);
                if (places.contains(i)) {
                    String foName = scheduler.getFoNames().get(i);
                    if (foName == null) {
                        cell.setCellValue("NULL");
                    } else {
                        cell.setCellValue(foName.equals(name) ? "IMS1" : "IMS2");
                    }
                    cell.setCellStyle(
                            weekendsAndHolidays.contains(i) ? headerRedCellStyle : greenStyle);
                } else if (weekendsAndHolidays.contains(i)) {
                    cell.setCellStyle(headerRedCellStyle);
                }
            }
        }

        String fileNameExcel = "schedule-" +
                localDate.getYear() +
                "-" +
                localDate.getMonthValue() +
                ".xlsx";
        try (FileOutputStream outputStream = new FileOutputStream(fileNameExcel)) {
            workbook.write(outputStream);
        }
        workbook.close();
    }

    private CellStyle getCellStyle(
            Workbook workbook,
            IndexedColors indexedColors) {
        return getCellStyle(
                workbook,
                indexedColors.getIndex(),
                FillPatternType.SOLID_FOREGROUND);
    }

    private CellStyle getCellStyle(
            Workbook workbook,
            short indexedColor,
            FillPatternType fillPatternType) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(indexedColor);
        style.setFillPattern(fillPatternType);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }
}
