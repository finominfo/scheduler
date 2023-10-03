package hu.finominfo.scheduler.util;

import hu.finominfo.scheduler.people.People;
import hu.finominfo.scheduler.people.Person;
import hu.finominfo.scheduler.scheduler.Scheduler;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExcelExporter {

    private final Scheduler scheduler;
    private final People people;
    private final LocalDate localDate;

    public ExcelExporter(Scheduler scheduler, People people, LocalDate localDate) {
        this.scheduler = scheduler;
        this.people = people;
        this.localDate = localDate;
    }

    public void writeMonthToExcel()
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

        Font headerFont = workbook.createFont();
        headerFont.setColor(IndexedColors.WHITE.index);

        Font headerWDFont = workbook.createFont();
        headerWDFont.setColor(IndexedColors.RED.index);

        CellStyle headerCellStyle = getCellStyle(
                workbook,
                IndexedColors.BLACK);
        headerCellStyle.setFont(headerFont);
        CellStyle headerOrangeCellStyle = getCellStyle(workbook, IndexedColors.ORANGE);
        CellStyle headerRedCellStyle = getCellStyle(workbook, IndexedColors.RED);
        CellStyle dataCellStyle = getCellStyle(workbook, IndexedColors.YELLOW);
        CellStyle greenStyle = getCellStyle(workbook, IndexedColors.LIGHT_GREEN);
        CellStyle IMS1Style = getCellStyle(workbook, IndexedColors.PALE_BLUE);
        CellStyle IMS2Style = getCellStyle(workbook, IndexedColors.LIGHT_YELLOW);
        CellStyle wdStyle = getCellStyle(workbook, IndexedColors.BLACK);
        wdStyle.setFont(headerWDFont);

        Cell cell = headerRow.createCell(colNum++);
        LocalDate ld = scheduler.getDate();
        cell.setCellValue(ld.getYear() + " " + ld.getMonth().name());
        cell.setCellStyle(topLeftCellStyle);

        List<Integer> weekends = new ArrayList<>();
        weekends.addAll(scheduler.getSaturdays());
        weekends.addAll(scheduler.getSundays());
        List<Integer> holidays = new ArrayList<>();
        holidays.addAll(scheduler.getHolidays());

        sheet.setColumnWidth(0, 20 * 256);
        for (int i = 1; i <= scheduler.getNumOfDays(); i++) {
            cell = headerRow.createCell(colNum++);
            cell.setCellValue(i);
            if (holidays.contains(i)) {
                cell.setCellStyle(headerRedCellStyle);
            } else if (weekends.contains(i)) {
                cell.setCellStyle(headerOrangeCellStyle);
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
                    holidays.contains(i) ? headerRedCellStyle : weekends.contains(i) ? headerOrangeCellStyle : wdStyle);
        }

        for (String name : names) {
            List<Integer> places = scheduler
                    .getScheduled()
                    .entrySet()
                    .stream()
                    .filter(e -> e.getValue().contains(name))
                    .map(Map.Entry::getKey)
                    .sorted()
                    .collect(Collectors.toList());
            List<Integer> hatedDays = people.getPeople().get(name).getHatedDays();
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
                            // weekends.contains(i) ? headerOrangeCellStyle :
                            (foName.equals(name) ? IMS1Style : IMS2Style));
                } else if (hatedDays.contains(i)) {
                    cell.setCellValue("X");
                    cell.setCellStyle(
                            holidays.contains(i) ? headerRedCellStyle : weekends.contains(i) ? headerOrangeCellStyle : lightGreyStyle);
                } else if (holidays.contains(i)) {
                    cell.setCellStyle(headerRedCellStyle);
                } else if (weekends.contains(i)) {
                    cell.setCellStyle(headerOrangeCellStyle);
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
