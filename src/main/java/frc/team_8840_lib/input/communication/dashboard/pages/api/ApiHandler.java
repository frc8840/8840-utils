package frc.team_8840_lib.input.communication.dashboard.pages.api;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.net.httpserver.HttpExchange;

import frc.team_8840_lib.Info;
import frc.team_8840_lib.libraries.LibraryInfo;
import frc.team_8840_lib.libraries.LibraryManager;
import frc.team_8840_lib.listeners.Robot;
import frc.team_8840_lib.utils.http.IP;
import frc.team_8840_lib.utils.http.Route;
import frc.team_8840_lib.various.SwerveSetup;

public class ApiHandler {
    private static String[] possibleRoutes = {
        "/api/",
        "/api/libraries",
        "/api/info",
        "/api/swerve/config",
        "/api/swerve/config/next-step"
    };

    public static Route.Resolution HandleRequest(HttpExchange req, Route.Resolution res) {
        String path = req.getRequestURI().getPath();

        final ArrayList<String> possibleRoutesAsList = new ArrayList<String>(Arrays.asList(possibleRoutes));

        if (!possibleRoutesAsList.contains(path)) {
            return res.status(404).json("{ \"message\": \"Page not found\" }");
        }

        if (path.equalsIgnoreCase("/api/libraries")) {
            return getLibraries(req, res);
        } else if (path.equalsIgnoreCase("/api/info")) {
            return getInfo(req, res);
        } else if (path.equalsIgnoreCase("/api/swerve/config")) {
            return SwerveSetup.handleSwerveSetup(req, res);
        } else if (path.equalsIgnoreCase("/api/swerve/config/next-step")) {
            return SwerveSetup.nextStep(req, res);
        }
 
        return res.status(200).json("{ \"message\": \"Page found\" }");
    }

    public static Route.Resolution getInfo(HttpExchange req, Route.Resolution res) {
        boolean inSimulation = Robot.isSimulation();

        JSONObject json = new JSONObject();

        json.put("inSimulation", inSimulation ? "true" : "false");

        json.put("version", Info._version());

        String ipAddress = IP.getIP();

        json.put("ip", ipAddress);

        json.put("os", Robot.os());

        long freeStorage = new File("/").getFreeSpace();
        long totalStorage = new File("/").getTotalSpace();

        // Convert to GB
        freeStorage = freeStorage / 1000000000;
        totalStorage = totalStorage / 1000000000;

        JSONObject storage = new JSONObject();

        storage.put("free_gb", freeStorage);
        storage.put("total_gb", totalStorage);

        json.put("storage", storage);

        return res.status(200).json(json.toString());
    }

    public static Route.Resolution getLibraries(HttpExchange req, Route.Resolution res) {
        ArrayList<LibraryInfo> libraries = LibraryManager.getLoadedLibraries();

        JSONObject json = new JSONObject();

        json.put("count", libraries.size());

        JSONArray libraryArray = new JSONArray();

        for (LibraryInfo library : libraries) {
            JSONObject libraryJson = new JSONObject();

            libraryJson.put("name", library.name());
            libraryJson.put("version", library.version());
            libraryJson.put("description", library.description());
            libraryJson.put("authors", library.author());
            libraryJson.put("repo", library.repo());
            libraryJson.put("experimental", library.experimental() ? "true" : "false");

            libraryArray.put(libraryJson);
        }

        json.put("libraries", libraryArray);

        return res.status(200).json(json.toString());
    }
}
