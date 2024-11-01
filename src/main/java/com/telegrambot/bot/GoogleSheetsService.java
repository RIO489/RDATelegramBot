package com.telegrambot.bot;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.telegrambot.entity.User;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.FileInputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;


public class GoogleSheetsService {

        private static Sheets sheetsService;
        private static final String APPLICATION_NAME = "Telegram Bot for RDA Problem Management";
        private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
        private static final String SPREADSHEET_ID = "1MhxCS4cEHEcUoLxYW-65pyt5dBEkYGraVBS-uIz2AME";

        public static Sheets getSheetsService() throws IOException, GeneralSecurityException {
            if (sheetsService == null) {
                GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream("bot-veru-3b4d2e802582.json"))
                        .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));
                sheetsService = new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                        .setApplicationName(APPLICATION_NAME)
                        .build();
            }
            return sheetsService;
        }

    /**
     * Adds a new row to Google Sheets with the user's problem details.
     */
    public static void addRowToSheet(User user, String problemDescription, String resources, int scale, String frequency,String solution)
            throws IOException, GeneralSecurityException {

        Sheets service = getSheetsService();

        // Format date and time for consistent record-keeping
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        // Prepare data to append as a new row in the sheet
        ValueRange appendBody = new ValueRange()
                .setValues(Arrays.asList(
                        Arrays.asList(
                                timestamp,
                                user.getRda(),
                                user.getName(),
                                user.getPhone(),
                                problemDescription,
                                resources,
                                String.valueOf(scale),
                                frequency,
                                solution
                        )
                ));

        // Execute append operation
        service.spreadsheets().values()
                .append(SPREADSHEET_ID, "Аркуш1", appendBody)
                .setValueInputOption("USER_ENTERED")
                .setInsertDataOption("INSERT_ROWS")
                .execute();

        // Optional: Log the data for confirmation
        System.out.println("Data added to Google Sheets: " + appendBody.getValues());
    }
    }