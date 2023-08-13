package frc.team_8840_lib.input.communication.dashboard.pages;

import frc.team_8840_lib.input.communication.dashboard.pages.api.ApiHandler;
import frc.team_8840_lib.listeners.Robot;
import frc.team_8840_lib.utils.http.Constructor;
import frc.team_8840_lib.utils.http.Route;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;

import com.sun.net.httpserver.HttpExchange;

public class PageHandler {
    private static HashMap<String, String> pages;

    public static void addPages() {
        if (pages != null) {
            return;
        }

        pages = new HashMap<>();

        pages.put("/", "home/index.html");
        pages.put("/libraries", "libraries/library.html");
        pages.put("/finder", "finder/finder.html");
        pages.put("/swerve/setup", "swerve/config/config.html");

        pages.put("/static/library.js", "libraries/library.js");
        pages.put("/static/finder.js", "finder/finder.js");
        pages.put("/static/swerve-config.js", "swerve/config/config.js");

        pages.put("/static/style.css", "home/style.css");
        pages.put("/static/finder.css", "finder/finder.css");
        pages.put("/static/library.css", "libraries/library.css");
        pages.put("/static/swerve-config.css", "swerve/config/config.css");
    }

    public static Constructor get() {
        return (new Constructor() {
            @Override
            public Route.Resolution finish(HttpExchange req, Route.Resolution res) {
                //get path
                String path = req.getRequestURI().getPath();

                if (path.startsWith("/api")) {
                    return ApiHandler.HandleRequest(req, res);
                }

                //get page
                String page = pages.get(path);

                //if page doesn't exist, return 404
                if (page == null) {
                    return res.status(404).json("{ \"message\": \"Page not found\" }");
                }
                
                //load file from resources
                InputStream file;
                
                if (Robot.isJAR()) {
                    file = getClass().getClassLoader().getResourceAsStream(page);
                } else {
                    //we'll just open the file from the src folder directly
                    //the getLocation() returns <base>/bin/main, so we'll just exit out to <base> and go from there.
                    //works fine lol (this is only for working on the lib though, so the real thing that matters is the isJAR() path.
                    try {
                        URI base = getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
                        String addition = "../../src/main/resources/" + page;
                        File f = new File(base.getPath() + addition);
                        file = f.toURI().toURL().openStream();
                    } catch (Exception e) {
                        return res.status(500).json("{ \"message\": \"Error reading file.\", \"error\": \"" + e.getMessage() + "\" }");
                    }
                }

                //if file doesn't exist, return 500
                if (file == null) {
                    return res.status(500).json("{ \"message\": \"Page not found in files.\" }");
                }

                //get string from file
                String fileString;

                try {
                    fileString = new String(file.readAllBytes());
                } catch (Exception e) {
                    System.out.println("There was an issue reading " + path + " from " + page + ".");
                    return res.status(500).json("{ \"message\": \"Error reading file.\" }");
                }

                //get file extension
                String extension = page.substring(page.lastIndexOf('.') + 1);

                //set content type
                if (extension.equalsIgnoreCase("html")) {
                    res.setHeader("Content-Type", "text/html");
                } else if (extension.equalsIgnoreCase("js")) {
                    res.setHeader("Content-Type", "text/javascript");
                } else if (extension.equalsIgnoreCase("css")) {
                    res.setHeader("Content-Type", "text/css");
                } else if (extension.equalsIgnoreCase("png")) {
                    res.setHeader("Content-Type", "image/png");
                } else if (extension.equalsIgnoreCase("jpg")) {
                    res.setHeader("Content-Type", "image/jpeg");
                } else if (extension.equalsIgnoreCase("gif")) {
                    res.setHeader("Content-Type", "image/gif");
                } else if (extension.equalsIgnoreCase("svg")) {
                    res.setHeader("Content-Type", "image/svg+xml");
                } else {
                    res.setHeader("Content-Type", "text/plain");
                }

                //return file
                return res.setContent(fileString).status(200);
            }
        });
    }
}
