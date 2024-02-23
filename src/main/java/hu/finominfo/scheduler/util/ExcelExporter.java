package hu.finominfo.scheduler.util;

import hu.finominfo.scheduler.people.People;
import hu.finominfo.scheduler.people.Person;
import hu.finominfo.scheduler.scheduler.Scheduler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ExcelExporter {

    private static final Logger logger = LogManager.getLogger(ExcelExporter.class);

    private final Scheduler scheduler;
    private final People people;
    private final LocalDate localDate;

    private final DecimalFormat df = new DecimalFormat("#.##");
    // private final DecimalFormat df = new DecimalFormat("#");

    public ExcelExporter(Scheduler scheduler, People people, LocalDate localDate) {
        this.scheduler = scheduler;
        this.people = people;
        this.localDate = localDate;
    }

    public void writeMonthToExcel() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Monthly Schedule");

        int rowNum = 0;
        int colNum = 0;

        // Create a header row with a different color
        Row headerRow = sheet.createRow(rowNum++);
        CellStyle topLeftCellStyle = getCellStyle(workbook, IndexedColors.BLACK1.getIndex(), FillPatternType.DIAMONDS);
        CellStyle lightGreyStyle = getCellStyle(workbook, IndexedColors.GREY_25_PERCENT);

        Font headerFont = workbook.createFont();
        headerFont.setColor(IndexedColors.WHITE.index);

        Font headerWDFont = workbook.createFont();
        headerWDFont.setColor(IndexedColors.RED.index);

        CellStyle headerCellStyle = getCellStyle(workbook, IndexedColors.BLACK);
        headerCellStyle.setFont(headerFont);
        CellStyle headerOrangeCellStyle = getCellStyle(workbook, IndexedColors.ORANGE);
        CellStyle sumCellStyle = getCellStyle(workbook, IndexedColors.CORAL);
        CellStyle headerRedCellStyle = getCellStyle(workbook, IndexedColors.RED);
        CellStyle dataCellStyle = getCellStyle(workbook, IndexedColors.CORAL);
        CellStyle IMS1Style = getCellStyle(workbook, IndexedColors.PALE_BLUE);
        CellStyle IMS2Style = getCellStyle(workbook, IndexedColors.LIGHT_YELLOW);
        CellStyle wdStyle = getCellStyle(workbook, IndexedColors.BLACK);
        wdStyle.setFont(headerWDFont);
        CellStyle headerLightOrangeCellStyle = getCellStyle(workbook, IndexedColors.AQUA);
        CellStyle headerLightGreenCellStyle = getCellStyle(workbook, IndexedColors.LIGHT_GREEN);
        CellStyle headerBlueGreyCellStyle = getCellStyle(workbook, IndexedColors.GREY_25_PERCENT);
        CellStyle headerPaleBlueCellStyle = getCellStyle(workbook, IndexedColors.PALE_BLUE);
        CellStyle headerPinkCellStyle = getCellStyle(workbook, IndexedColors.LIGHT_TURQUOISE);
        CellStyle basicStyle = getCellStyle(workbook, IndexedColors.TEAL);

        Cell cell = headerRow.createCell(colNum++);
        LocalDate ld = scheduler.getDate();
        cell.setCellValue(ld.getYear() + " " + ld.getMonth().name().substring(0, 3));
        cell.setCellStyle(topLeftCellStyle);

        List<Integer> normalWeekends = new ArrayList<>();
        normalWeekends.addAll(scheduler.getSaturdays());
        normalWeekends.addAll(scheduler.getSundays());
        List<Integer> holidays = new ArrayList<>();
        holidays.addAll(scheduler.getHolidays());

        sheet.setColumnWidth(0, 18 * 256);
        for (int i = 1; i <= scheduler.getNumOfDays(); i++) {
            cell = headerRow.createCell(colNum++);
            cell.setCellValue(i);
            if (holidays.contains(i)) {
                cell.setCellStyle(headerRedCellStyle);
            } else if (normalWeekends.contains(i)) {
                cell.setCellStyle(headerOrangeCellStyle);
            } else {
                cell.setCellStyle(headerCellStyle);
            }
            sheet.setColumnWidth(i, (int) (5 * 256));
        }

        List<String> names = people.getPeople().values().stream().map(Person::getName).sorted()
                .collect(Collectors.toList());

        Row row = sheet.createRow(rowNum++);
        colNum = 0;
        Cell dateCell = row.createCell(colNum++);
        dateCell.setCellStyle(topLeftCellStyle);
        for (int i = 1; i <= scheduler.getNumOfDays(); i++) {
            dateCell = row.createCell(colNum++);
            dateCell.setCellValue(localDate.withDayOfMonth(i).getDayOfWeek().name().toUpperCase().substring(0, 3));
            dateCell.setCellStyle(
                    holidays.contains(i) ? headerRedCellStyle : normalWeekends.contains(i) ? headerOrangeCellStyle : wdStyle);
        }

        for (String name : names) {
            List<Integer> places = scheduler.getScheduled().entrySet().stream().filter(e -> e.getValue().contains(name))
                    .map(Map.Entry::getKey).sorted().collect(Collectors.toList());
            List<Integer> hatedDays = people.getPeople().get(name).getHatedDays();
            row = sheet.createRow(rowNum++);
            colNum = 0;
            dateCell = row.createCell(colNum++);
            dateCell.setCellValue(name);
            dateCell.setCellStyle((rowNum & 1) == 0 ? headerLightGreenCellStyle : headerLightOrangeCellStyle);
            // dateCell.setCellStyle(dataCellStyle);
            for (int i = 1; i <= scheduler.getNumOfDays(); i++) {
                cell = row.createCell(colNum++);
                cell.setCellStyle((rowNum & 1) == 0 ? headerLightGreenCellStyle : headerLightOrangeCellStyle);
                // cell.setCellStyle(basicStyle);
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
                    // cell.setCellStyle(holidays.contains(i) ? headerRedCellStyle :
                    // weekends.contains(i) ? headerOrangeCellStyle : lightGreyStyle);
                    cell.setCellStyle(lightGreyStyle);
                } else if (holidays.contains(i)) {
                    cell.setCellStyle(headerRedCellStyle);
                } else if (normalWeekends.contains(i)) {
                    cell.setCellStyle(headerOrangeCellStyle);
                }

            }
        }

        for (int i = 0; i < 3; i++)
            row = sheet.createRow(rowNum++);
        colNum = 0;
        // cell = row.createCell(colNum++);
        // cell = row.createCell(colNum++);
        // cell.setCellValue("WE=WE+HD");
        // sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, colNum -
        // 1, colNum + 3));
        // sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, colNum -
        // 1, colNum));
        for (int i = 0; i < 9; i++)
            cell = row.createCell(colNum++);
        colNum = writeNewCell(colNum, headerLightOrangeCellStyle, row, "" + (scheduler.getLocalDate().getYear()) + " SUMMARIZE");
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, colNum - 1, colNum + 5));
        for (int i = 0; i < 12; i++)
            cell = row.createCell(colNum++);
        LocalDate localDatePlusMinus = scheduler.getLocalDate().plusMonths(1).minusDays(1);
        cell.setCellValue("up to " + localDatePlusMinus.getMonth().toString().substring(0, 3) + " "
                + localDatePlusMinus.getDayOfMonth());
        cell.setCellStyle(headerLightOrangeCellStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, colNum - 1, colNum + 4));

        row = sheet.createRow(rowNum++);
        colNum = 0;
        cell = row.createCell(colNum++);
        colNum = writeNewCell(colNum, headerBlueGreyCellStyle, row, "IMS1\nWE");
        colNum = writeNewCell(colNum, headerPaleBlueCellStyle, row, "IMS1\nWD");
        colNum = writeNewCell(colNum, headerBlueGreyCellStyle, row, "IMS2\nWE");
        colNum = writeNewCell(colNum, headerPaleBlueCellStyle, row, "IMS2\nWD");
        colNum = writeNewCell(colNum, headerPinkCellStyle, row, "NH");
        colNum = writeNewCell(colNum, headerPinkCellStyle, row, "WE");
        colNum = writeNewCell(colNum, headerPinkCellStyle, row, "WD");
        colNum = writeNewCell(colNum, headerPinkCellStyle, row, "ALL");
        colNum = writeNewCell(colNum, headerPaleBlueCellStyle, row, "MON-\nTHU");
        colNum = writeNewCell(colNum, headerPaleBlueCellStyle, row, "FR");
        colNum = writeNewCell(colNum, headerPaleBlueCellStyle, row, "SA");
        colNum = writeNewCell(colNum, headerPaleBlueCellStyle, row, "SU");
        colNum = writeNewCell(colNum, headerPaleBlueCellStyle, row, "WE");
        colNum = writeNewCell(colNum, headerPaleBlueCellStyle, row, "NH");
        colNum = writeNewCell(colNum, headerPaleBlueCellStyle, row,"ALL");
        colNum = writeNewCell(colNum, headerBlueGreyCellStyle, row, "MON-\nTHU");
        colNum = writeNewCell(colNum, headerBlueGreyCellStyle, row, "FRI");
        colNum = writeNewCell(colNum, headerBlueGreyCellStyle, row, "SAT");
        colNum = writeNewCell(colNum, headerBlueGreyCellStyle, row, "SUN");
        colNum = writeNewCell(colNum, headerBlueGreyCellStyle, row, "NH");

        colNum = writeNewCell(colNum, headerPinkCellStyle, row, "STANDBY\n WE                 WD               SUM");
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, colNum - 1, colNum + 4));

        // colNum = writeNewCell(colNum, headerPinkCellStyle, row, "WE\nSTAN\nDBY");
        // colNum = writeNewCell(colNum, headerPinkCellStyle, row, "WD\nSTAN\nDBY");
        // colNum = writeNewCell(colNum, headerPinkCellStyle, row, "STAN\nDBY\nSUM");

        // ************************************************************************

        List<Integer> fridays = IntStream.rangeClosed(1, scheduler.getNumOfDays())
                .filter(i -> localDate.withDayOfMonth(i).getDayOfWeek().equals(DayOfWeek.FRIDAY))
                .mapToObj(Integer::valueOf)
                .collect(Collectors.toList());

        List<Integer> sundays = IntStream.rangeClosed(1, scheduler.getNumOfDays())
                .filter(i -> localDate.withDayOfMonth(i).getDayOfWeek().equals(DayOfWeek.SUNDAY))
                .mapToObj(Integer::valueOf)
                .collect(Collectors.toList());

        List<Integer> saturdays = IntStream.rangeClosed(1, scheduler.getNumOfDays())
                .filter(i -> localDate.withDayOfMonth(i).getDayOfWeek().equals(DayOfWeek.SATURDAY))
                .mapToObj(Integer::valueOf)
                .collect(Collectors.toList());

        KeyValueStore keyValueStore = scheduler.getKeyValueStore();
        int year = scheduler.getDate().getYear();
        int monthValue = scheduler.getDate().getMonthValue();

        // int year2 = scheduler.getDate().minusMonths(1).getYear();
        // int monthValue2 = scheduler.getDate().minusMonths(1).getMonthValue();

        for (String name : names) {
            row = sheet.createRow(rowNum++);
            colNum = 0;
            colNum = writeNewCell(colNum, (rowNum & 1) == 0 ? headerLightGreenCellStyle : headerLightOrangeCellStyle,
                    row, name);
            // colNum = writeNewCell(colNum, dataCellStyle, row, name);
            List<Integer> scheduled = scheduler.getScheduled().entrySet().stream()
                    .filter(e -> e.getValue().contains(name))
                    .map(e2 -> e2.getKey())
                    .collect(Collectors.toList());
            long numOfFridays = scheduled.stream().filter(fridays::contains)
                    .filter(s2 -> !scheduler.getHolidays().contains(s2)).count();
            long numOfSaturdays = scheduled.stream().filter(saturdays::contains)
                    .filter(s2 -> !scheduler.getHolidays().contains(s2)).count();
            long numOfSundays = scheduled.stream().filter(sundays::contains)
                    .filter(s2 -> !scheduler.getHolidays().contains(s2)).count();
            long numOfFridayHolidays = scheduled.stream()
                    .filter(s -> scheduler.getHolidays().contains(s))
                    .filter(fridays::contains)
                    .count();
            long numOfSaturdayHolidays = scheduled.stream()
                    .filter(s -> scheduler.getHolidays().contains(s))
                    .filter(saturdays::contains)
                    .count();
            long numOfSundayHolidays = scheduled.stream()
                    .filter(s -> scheduler.getHolidays().contains(s))
                    .filter(sundays::contains)
                    .count();
            long numOfWeekdayHolidays = scheduled.stream()
                    .filter(s -> scheduler.getHolidays().contains(s))
                    .filter(s1 -> !fridays.contains(s1))
                    .filter(s2 -> !saturdays.contains(s2))
                    .filter(s3 -> !sundays.contains(s3))
                    .count();
            long numOfHolidays = numOfFridayHolidays + numOfSaturdayHolidays + numOfSundayHolidays + numOfWeekdayHolidays;        
            List<Integer> ims1Scheduled = scheduler.getFoNames().entrySet().stream()
                    .filter(e -> e.getValue().contains(name))
                    .map(e2 -> e2.getKey())
                    .collect(Collectors.toList());
            List<Integer> ims2Scheduled = scheduled.stream()
                    .filter(s -> !ims1Scheduled.contains(s))
                    .collect(Collectors.toList());
            long ims1Weekend = ims1Scheduled.stream().filter(s -> normalWeekends.contains(s)).count();
            
            long ims1Weekday = ims1Scheduled.size() - ims1Weekend;
            long ims2Weekend = ims2Scheduled.stream().filter(s -> normalWeekends.contains(s)).count();
            long ims2Weekday = ims2Scheduled.size() - ims2Weekend;
            colNum = writeNewCell(colNum, (rowNum & 1) == 0 ? headerLightGreenCellStyle : headerLightOrangeCellStyle,
                    row, "" + ims1Weekend);
            colNum = writeNewCell(colNum, (rowNum & 1) == 0 ? headerLightGreenCellStyle : headerLightOrangeCellStyle,
                    row, "" + ims1Weekday);
            colNum = writeNewCell(colNum, (rowNum & 1) == 0 ? headerLightGreenCellStyle : headerLightOrangeCellStyle,
                    row, "" + ims2Weekend);
            colNum = writeNewCell(colNum, (rowNum & 1) == 0 ? headerLightGreenCellStyle : headerLightOrangeCellStyle,
                    row, "" + ims2Weekday);
            colNum = writeNewCell(colNum, (rowNum & 1) == 0 ? headerLightGreenCellStyle : headerLightOrangeCellStyle,
                    row, "" + numOfHolidays);
            colNum = writeNewCell(colNum, (rowNum & 1) == 0 ? headerLightGreenCellStyle : headerLightOrangeCellStyle,
                    row, "" + (ims1Weekend + ims2Weekend));
            colNum = writeNewCell(colNum, (rowNum & 1) == 0 ? headerLightGreenCellStyle : headerLightOrangeCellStyle,
                    row, "" + (ims1Weekday + ims2Weekday));
            colNum = writeNewCell(colNum, (rowNum & 1) == 0 ? headerLightGreenCellStyle : headerLightOrangeCellStyle,
                    row, "" + scheduled.size());

            long weAll = keyValueStore.sum(name, year, monthValue, "WE") + ims1Weekend + ims2Weekend;
            long nhwdAll = keyValueStore.sum(name, year, monthValue, "NHWD") + numOfWeekdayHolidays;
            long nhfrAll = keyValueStore.sum(name, year, monthValue, "NHFR") + numOfFridayHolidays;
            long nhsaAll = keyValueStore.sum(name, year, monthValue, "NHSA") + numOfSaturdayHolidays;
            long nhsuAll = keyValueStore.sum(name, year, monthValue, "NHSU") + numOfSundayHolidays;
            long nhAll = nhwdAll + nhfrAll + nhsaAll + nhsuAll;
            long allAll = keyValueStore.sum(name, year, monthValue, "ALL") + scheduled.size(); 
            long frAll = keyValueStore.sum(name, year, monthValue, "FR") + numOfFridays;
            long mon2ThuAll = allAll - weAll - (frAll + nhfrAll) - nhwdAll;
            long suAll = keyValueStore.sum(name, year, monthValue, "SU") + numOfSundays;
            long saAll = weAll - suAll;


            /*
            List<LocalDate> holidaysForYear = HungarianHolidays.getHolidaysForYear(year);
            List<LocalDate> allHolMonToThu = new ArrayList<>();
            List<LocalDate> allHolFridays = new ArrayList<>();
            List<LocalDate> allHolSaturdays = new ArrayList<>();
            List<LocalDate> allHolSundays = new ArrayList<>();

            //LocalDate date = LocalDate.of(year, 1, 1); // Start from January 1st of the given year
            LocalDate date = LocalDate.of(year, monthValue, 1);
            LocalDate endDate = LocalDate.of(year, monthValue, YearMonth.of(year, monthValue).lengthOfMonth()); 

            while (!date.isAfter(endDate)) {
                if (scheduled.contains(date.getDayOfMonth())) {
                    if (date.getDayOfWeek() == DayOfWeek.FRIDAY && holidaysForYear.contains(date)) {
                        allHolFridays.add(date);
                    } else if (date.getDayOfWeek() == DayOfWeek.SATURDAY && holidaysForYear.contains(date)) {
                        allHolSaturdays.add(date);
                    } else if (date.getDayOfWeek() == DayOfWeek.SUNDAY && holidaysForYear.contains(date)) {
                        allHolSundays.add(date);
                    } else if (holidaysForYear.contains(date)) {
                        allHolMonToThu.add(date);
                    }
                }
                date = date.plusDays(1); // Move to the next day
            }
            int allHolMonToThuNum = allHolMonToThu.size();
            int allHolFridaysNum = allHolFridays.size();
            int allHolSaturdaysNum = allHolSaturdays.size();
            int allHolSundaysNum = allHolSundays.size();
            */

            colNum = writeNewCell(colNum, (rowNum & 1) == 0 ? headerLightGreenCellStyle : headerLightOrangeCellStyle,
                    row, "" + mon2ThuAll);
            colNum = writeNewCell(colNum, (rowNum & 1) == 0 ? headerLightGreenCellStyle : headerLightOrangeCellStyle,
                    row, "" + frAll);
            colNum = writeNewCell(colNum, (rowNum & 1) == 0 ? headerLightGreenCellStyle : headerLightOrangeCellStyle,
                    row, "" + saAll);
            colNum = writeNewCell(colNum, (rowNum & 1) == 0 ? headerLightGreenCellStyle : headerLightOrangeCellStyle,
                    row, "" + suAll);

                    colNum = writeNewCell(colNum, (rowNum & 1) == 0 ? headerLightGreenCellStyle : headerLightOrangeCellStyle,
                    row, "" + weAll);
            colNum = writeNewCell(colNum, (rowNum & 1) == 0 ? headerLightGreenCellStyle : headerLightOrangeCellStyle,
                    row, "" + nhAll);
            colNum = writeNewCell(colNum, (rowNum & 1) == 0 ? headerLightGreenCellStyle : headerLightOrangeCellStyle,
                    row, "" + allAll); 

            List<Integer> monToThu = scheduled.stream()
                    .filter(s1 -> !normalWeekends.contains(s1))
                    .filter(s2 -> !fridays.contains(s2))
                    .filter(s3 -> !holidays.contains(s3))
                    .collect(Collectors.toList());
            colNum = writeNewCell(colNum, (rowNum & 1) == 0 ? headerLightGreenCellStyle : headerLightOrangeCellStyle,
                    row, "" + monToThu.size());
            List<Integer> fridayList = scheduled.stream()
                    .filter(s1 -> !normalWeekends.contains(s1))
                    .filter(s2 -> !monToThu.contains(s2))
                    .filter(s3 -> !holidays.contains(s3))
                    .collect(Collectors.toList());
            colNum = writeNewCell(colNum, (rowNum & 1) == 0 ? headerLightGreenCellStyle : headerLightOrangeCellStyle,
                    row, "" + fridayList.size());
            colNum = writeNewCell(colNum, (rowNum & 1) == 0 ? headerLightGreenCellStyle : headerLightOrangeCellStyle,
                    row, "" + numOfSaturdays);
            colNum = writeNewCell(colNum, (rowNum & 1) == 0 ? headerLightGreenCellStyle : headerLightOrangeCellStyle,
                    row, "" + numOfSundays);
            colNum = writeNewCell(colNum, (rowNum & 1) == 0 ? headerLightGreenCellStyle : headerLightOrangeCellStyle,
                    row, "" + numOfHolidays);

            double weekendSBNum = 3.6 * frAll + 9.6 * saAll + 6 * suAll + 9.6 * (nhfrAll + nhsaAll + nhsuAll  + nhwdAll);
            double weekdaySBNum = 3.2 * mon2ThuAll  + 1.4 * frAll + 1.8 * suAll;

            /*
            long frAllNotHol = frAll - allHolFridaysNum;
            long saAllNotHol = saAll - allHolSaturdaysNum;
            long suAllNotHol = suAll - allHolSundaysNum;
            long mon2ThuAllNotHol = mon2ThuAll - allHolMonToThuNum;

            if (allHolFridaysNum + allHolSaturdaysNum + allHolSundaysNum + allHolMonToThuNum != numOfHolidays) {
                throw new RuntimeException("Holidays are not correct for " + name + " " + (allHolFridaysNum + allHolSaturdaysNum + allHolSundaysNum + allHolMonToThuNum) + " " + numOfHolidays);
            } else {
                logger.info("Holidays are correct for " + name + " " + (allHolFridaysNum + allHolSaturdaysNum + allHolSundaysNum + allHolMonToThuNum) + " " + numOfHolidays);
            }

            double weekendSBNum = 3.6 * frAllNotHol + 9.6 * saAllNotHol + 6 * suAllNotHol + 9.6 * nhAll;
            double weekdaySBNum = 3.2 * mon2ThuAllNotHol + 1.4 * frAllNotHol + 1.8 * suAllNotHol;
            */

            colNum = writeNewCell(colNum, (rowNum & 1) == 0 ? headerLightGreenCellStyle : headerLightOrangeCellStyle,
                    row, df.format(weekendSBNum));
            row.createCell(colNum++, CellType.NUMERIC);
            sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, colNum - 2, colNum - 1));
            colNum = writeNewCell(colNum, (rowNum & 1) == 0 ? headerLightGreenCellStyle : headerLightOrangeCellStyle,
                    row, df.format(weekdaySBNum));
            row.createCell(colNum++, CellType.NUMERIC);
            sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, colNum - 2, colNum - 1));
            double allSBNum = weekendSBNum + weekdaySBNum;
            colNum = writeNewCell(colNum, (rowNum & 1) == 0 ? headerLightGreenCellStyle : headerLightOrangeCellStyle,
                    row, df.format(allSBNum));
            row.createCell(colNum++, CellType.NUMERIC);
            sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, colNum - 2, colNum - 1));

            keyValueStore.writeData(name, year, monthValue, "WE", (int) (numOfSaturdays + numOfSundays));
            keyValueStore.writeData(name, year, monthValue, "NHWD", (int) numOfWeekdayHolidays);
            keyValueStore.writeData(name, year, monthValue, "NHFR", (int) numOfFridayHolidays);
            keyValueStore.writeData(name, year, monthValue, "NHSA", (int) numOfSaturdayHolidays);
            keyValueStore.writeData(name, year, monthValue, "NHSU", (int) numOfSundayHolidays);
            keyValueStore.writeData(name, year, monthValue, "ALL", scheduled.size());
            keyValueStore.writeData(name, year, monthValue, "FR", (int) numOfFridays);
            keyValueStore.writeData(name, year, monthValue, "SU", (int) numOfSundays);

        }

        row = sheet.createRow(rowNum++);
        colNum = 0;
        cell = row.createCell(colNum++);
        for (int i = 0; i < 23; i++) {
            Cell summaryCell = row.createCell(colNum++, CellType.FORMULA);
            summaryCell.setCellStyle(sumCellStyle);
            char colName = (char) (64 + colNum);
            int endNum = rowNum - 1;
            int startNum = endNum - names.size() + 1;
            if (i > 19) {
                row.createCell(colNum++, CellType.FORMULA);
                sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, colNum - 2, colNum - 1));
            }
            summaryCell.setCellFormula("SUM(" + colName + startNum + ":" + colName + endNum + ")");
            FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();
            formulaEvaluator.evaluate(summaryCell);
        }

        keyValueStore.printAll(year);
        try {
            keyValueStore.close();
        } catch (SQLException e) {
            logger.error(e);
        }

        // ************************************************************************

        String fileNameExcel = "schedule-" + localDate.getYear() + "-" + localDate.getMonthValue() + ".xlsx";
        try (FileOutputStream outputStream = new FileOutputStream(fileNameExcel)) {
            workbook.write(outputStream);
        }
        workbook.close();
    }


    public static Set<LocalDate> findSpecificFridaysOfYear(int year) {
        Set<LocalDate> fridays = new HashSet<>();

        LocalDate date = LocalDate.of(year, 1, 1); // Start from January 1st of the given year
        LocalDate endDate = LocalDate.of(year, 12, 31); // End on December 31st of the given year

        while (!date.isAfter(endDate)) {
            if (date.getDayOfWeek() == DayOfWeek.FRIDAY) {
                fridays.add(date);
            }
            date = date.plusDays(1); // Move to the next day
        }

        return fridays;
    }


    private static int writeNewCell(int colNum, CellStyle cellStyle, Row row, String str) {
        Cell cell;
        if (isNumeric(str)) {
            cell = row.createCell(colNum++, CellType.NUMERIC);
            cell.setCellValue(Double.parseDouble(str));
            cell.setCellStyle(cellStyle);
        } else {
            cell = row.createCell(colNum++);
            cell.setCellValue(str);
            cell.setCellStyle(cellStyle);
        }
        return colNum;
    }

    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str); // Use Integer.parseInt() for integers
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private CellStyle getCellStyle(Workbook workbook, IndexedColors indexedColors) {
        return getCellStyle(workbook, indexedColors.getIndex(), FillPatternType.SOLID_FOREGROUND);
    }

    private CellStyle getCellStyle(Workbook workbook, short indexedColor, FillPatternType fillPatternType) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(indexedColor);
        style.setFillPattern(fillPatternType);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }

}
