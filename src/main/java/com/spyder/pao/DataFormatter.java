package com.spyder.pao;

import com.spyder.pao.model.PaoEntry;
import lombok.SneakyThrows;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class DataFormatter {

    private static final String DELIMITER = "|";
    private static final String DELIMITER_REGEX = "\\|";
    private static final int COLUMNS = 8;

    @SneakyThrows
    public static void formatDataFile(String filePath) {
        List<PaoEntry> entries = parseRawData(filePath);
        String formattedData = generateFormattedOutput(entries);
        writeFormattedData(formattedData, filePath);
        System.out.println("✓ Formatted " + entries.size() + " entries");
    }

    private static List<PaoEntry> parseRawData(String filePath) throws Exception {
        List<PaoEntry> entries = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            int row = 0;
            while ((line = br.readLine()) != null) {
                row++;
                line = line.replace("\"", ""); // Strip quotes
                String[] values = line.split(DELIMITER_REGEX, -1);

                if (values.length < COLUMNS) {
                    throw new IllegalStateException(
                        "Row " + row + " has " + values.length + " columns, expected " + COLUMNS
                    );
                }

                PaoEntry entry = createPaoEntry(values);
                entries.add(entry);
            }
        }
        return entries;
    }

    private static String generateFormattedOutput(List<PaoEntry> entries) {
        int[] maxColumnLengths = calculateMaxColumnLengths(entries);
        StringBuilder sb = new StringBuilder();

        for (PaoEntry entry : entries) {
            for (int i = 0; i < COLUMNS; i++) {
                String value = getCsvValue(entry, i);
                sb.append(padRight(value, maxColumnLengths[i] + 1))
                  .append(DELIMITER)
                  .append(" ");
            }
            // Last column (object) appended without padding
            sb.append(entry.getObject()).append("\n");
        }

        return sb.toString();
    }

    private static int[] calculateMaxColumnLengths(List<PaoEntry> entries) {
        int[] maxLengths = new int[COLUMNS];
        for (PaoEntry entry : entries) {
            for (int i = 0; i < COLUMNS; i++) {
                maxLengths[i] = Math.max(maxLengths[i], getCsvValue(entry, i).length());
            }
        }
        return maxLengths;
    }

    @SneakyThrows
    private static void writeFormattedData(String data, String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(data);
        }
    }

    private static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }

    private static String getCsvValue(PaoEntry entry, int column) {
        return switch(column) {
            case 0 -> entry.getNumberStr();
            case 1 -> entry.getPerson();
            case 2 -> entry.getAction();
            case 3 -> entry.getObject();
            case 4 -> entry.getAltNumbersStr();
            case 5 -> entry.getAltPeopleStr();
            case 6 -> entry.getAltActionsStr();
            case 7 -> entry.getAltObjectsStr();
            default -> throw new IllegalStateException("Unexpected column: " + column);
        };
    }

    private static PaoEntry createPaoEntry(String[] values) {
        PaoEntry entry = new PaoEntry();
        for (int i = 0; i < COLUMNS; i++) {
            setCsvValue(entry, i, values[i].trim());
        }
        return entry;
    }

    private static void setCsvValue(PaoEntry entry, int column, String value) {
        value = value.toLowerCase();
        switch(column) {
            case 0 -> entry.setNumberStr(value);
            case 1 -> entry.setPerson(value);
            case 2 -> entry.setAction(value);
            case 3 -> entry.setObject(value);
            case 4 -> entry.setAltNumbersStr(value);
            case 5 -> entry.setAltPeopleStr(value);
            case 6 -> entry.setAltActionsStr(value);
            case 7 -> entry.setAltObjectsStr(value);
            default -> throw new IllegalStateException("Unexpected column: " + column);
        }
    }
}
