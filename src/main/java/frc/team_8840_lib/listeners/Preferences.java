package frc.team_8840_lib.listeners;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.json.JSONObject;

import frc.team_8840_lib.info.console.Logger;

public class Preferences {
    private static String selectedEventListener = "";
    private static String selectedLogger = "";
    private static boolean readFile = false;

    public static boolean loaded() {
        return readFile;
    }

    public static Path getDefaultPreferencesPath() {
        return Path.of(System.getProperty("user.home"), "8840prefs.json");
    }

    public static void loadPreferences(Path path) {
        if (path == null) {
            path = getDefaultPreferencesPath();
        }

        File file = path.toFile();

        if (!file.exists()) {
            //Create the file
            try {
                file.createNewFile();
                Logger.Log("[Preferences] Created preferences file at " + path.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        //load content from file
        String content = "";
        
        //read file and save to content
        try {
            content = new String(Files.readAllBytes(path));
        } catch (IOException e) {
            e.printStackTrace();
        }

        //parse content
        JSONObject json = new JSONObject(content);

        selectedEventListener = json.getString("selectedEventListener");
        selectedLogger = json.getString("selectedLogger");

        readFile = true;
    }

    public static void savePreferences(Path path) {
        if (path == null) {
            path = getDefaultPreferencesPath();
        }

        File file = path.toFile();

        if (!file.exists()) {
            //Create the file
            try {
                file.createNewFile();
                Logger.Log("[Preferences] Created preferences file at " + path.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        //create json object
        JSONObject json = new JSONObject();

        //add values
        json.put("selectedEventListener", selectedEventListener);
        json.put("selectedLogger", selectedLogger);

        //write to file
        try {
            Files.write(path, json.toString().getBytes());

            Logger.Log("[Preferences] Saved preferences to " + path.toString());
        } catch (IOException e) {
            e.printStackTrace();

            Logger.Log("[Preferences] Failed to save preferences to " + path.toString());
        }
    }

    public static void setSelectedEventListener(String selectedEventListener) {
        Preferences.selectedEventListener = selectedEventListener;
    }

    public static void setSelectedLogger(String selectedLogger) {
        Preferences.selectedLogger = selectedLogger;
    }
    
    public static String getSelectedEventListener() {
        return selectedEventListener;
    }

    public static String getSelectedLogger() {
        return selectedLogger;
    }
}
