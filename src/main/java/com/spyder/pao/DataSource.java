package com.spyder.pao;

import com.spyder.pao.model.PaoEntry;
import com.spyder.pao.model.QuizConfiguration;
import lombok.SneakyThrows;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DataSource {

    public static final String RESOURCES_DIR = "src/main/resources";

    private static final String DELIMITER= "|";
    private static final String DELIMITER_REGEX = "\\|";
    private static final int COLUMNS = 8;

    private final List<PaoEntry> entries;

    private static final String DATA_FILE = "data.txt";

    public DataSource() {
        entries = new ArrayList<>();
    }

    public void addEntry(PaoEntry entry) {
        entries.add(entry);
    }

    private List<PaoEntry> getEntries() {
        return new ArrayList<>(entries);
    }

    public List<PaoEntry> getEntries(QuizConfiguration quizConfiguration) {
        return getEntries().stream()
                .filter(paoEntry -> paoEntry.getNumber() >= quizConfiguration.getMinEntry()
                        && paoEntry.getNumber() <= quizConfiguration.getMaxEntry()
                )
                .collect(Collectors.toList());
    }

    public static DataSource createAndLoad() {
        DataSource ds = new DataSource();
        ds.load();
        return ds;
    }

    private void load() {
        entries.clear();
        int row = 0;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(Objects.requireNonNull(DataSource.class.getResourceAsStream("/" + DATA_FILE))))) {
            row++;
            String line;
            while ((line = br.readLine()) != null) {
                // When copying multi-line output from google sheets, it comes wrapped in quotes. Strip those out, saves a manual step.
                line = line.replace("\"", "");
                String[] values = line.split(DELIMITER_REGEX, -1);
                PaoEntry entry = new PaoEntry();
                for(int i = 0; i < COLUMNS; i++) {
                    setCsvValue(entry, i, values[i].trim());
                }
                addEntry(entry);
            }
        } catch (IOException | ArrayIndexOutOfBoundsException e) {
            System.out.println("ERROR ON ROW " + row);
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    public void writeEquallySpaced() {
        String fileLocation = RESOURCES_DIR + "/" + DATA_FILE;
        FileWriter myWriter = new FileWriter(fileLocation);
        myWriter.write(equallySpacedCsv());
        myWriter.close();
    }

    private String equallySpacedCsv() {
        int[] maxColumnLengths = new int[COLUMNS];
        StringBuilder sb = new StringBuilder();
        // Find the max length for any value in a given column
        for(PaoEntry entry : entries) {
            for(int i = 0; i < COLUMNS; i++) {
                maxColumnLengths[i] = Math.max(maxColumnLengths[i], getCsvValue(entry, i).length());
            }
        }
        // Write data file, padding a value with spaces based on the max length for that column
        for(PaoEntry entry : entries) {
            for(int i = 0; i < COLUMNS; i++) {
                sb.append(padRight(getCsvValue(entry, i), maxColumnLengths[i] + 1)).append(DELIMITER).append(" ");
            }
            sb.append(entry.getObject()).append("\n");
        }
        return sb.toString();
    }

    private void setCsvValue(PaoEntry entry, int column, String value) {
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
            default -> throw new IllegalStateException("Unexpected column number " + column);
        };
    }

    private String getCsvValue(PaoEntry entry, int column) {
        return switch(column) {
            case 0 -> entry.getNumberStr();
            case 1 -> entry.getPerson();
            case 2 -> entry.getAction();
            case 3 -> entry.getObject();
            case 4 -> entry.getAltNumbersStr();
            case 5 -> entry.getAltPeopleStr();
            case 6 -> entry.getAltActionsStr();
            case 7 -> entry.getAltObjectsStr();
            default -> throw new IllegalStateException("Unexpected column number " + column);
        };
    }

    public static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }

}
