package com.spyder.pao;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.SneakyThrows;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class GoogleSheetsDataFetcher {

    public static void main(String[] args) {
        refreshData();
        System.out.println("✓ Data refresh completed successfully!");
    }

    private static void refreshData() {
        // Authenticate and build Sheets service
        Sheets sheetsService = getSheetsService();

        // Fetch data from Google Sheets
        String rawData = fetchDataFromSheet(sheetsService, SheetsConfig.SPREADSHEET_ID, SheetsConfig.CELL_RANGE);

        System.out.println("✓ Fetched data from Google Sheets");

        // Write raw data to file
        writeRawDataToFile(rawData, SheetsConfig.DATA_FILE_PATH);
        System.out.println("✓ Wrote raw data to " + SheetsConfig.DATA_FILE_PATH);

        // Format the data
        DataFormatter.formatDataFile(SheetsConfig.DATA_FILE_PATH);
    }

    @SneakyThrows
    private static Sheets getSheetsService() {
        // Load service account credentials
        FileInputStream credentialsStream = new FileInputStream(SheetsConfig.CREDENTIALS_FILE_PATH);

        GoogleCredentials credentials = GoogleCredentials
                .fromStream(credentialsStream)
                .createScoped(Collections.singletonList(SheetsScopes.SPREADSHEETS_READONLY));

        // Build the Sheets service
        HttpCredentialsAdapter credentialsAdapter = new HttpCredentialsAdapter(credentials);

        return new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), credentialsAdapter)
                .setApplicationName(SheetsConfig.APPLICATION_NAME)
                .build();
    }

    @SneakyThrows
    private static String fetchDataFromSheet(Sheets service, String spreadsheetId, String range) {
        ValueRange response = service.spreadsheets().values().get(spreadsheetId, range).execute();

        List<List<Object>> values = response.getValues();

        if (values == null || values.isEmpty()) {
            throw new RuntimeException("Cell " + range + " is empty");
        }

        if (values.size() != 1 || values.get(0).isEmpty()) {
            throw new RuntimeException("Expected single cell with data, got: " + values.size() + " rows");
        }

        Object cellValue = values.get(0).get(0);
        return cellValue.toString();
    }

    private static void writeRawDataToFile(String data, String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
