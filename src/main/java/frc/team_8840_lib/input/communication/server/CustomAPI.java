package frc.team_8840_lib.input.communication.server;

import java.util.HashMap;

import frc.team_8840_lib.utils.http.Constructor;

public class CustomAPI {
    private static HashMap<String, Constructor> constructors = new HashMap<>();

    public static final String path = "/custom";

    public static void route(String route, Constructor constructor) {
        constructors.put(path + (route.startsWith("/") ? "" : "/") + route, constructor);
    }

    public static HashMap<String, Constructor> getConstructors() {
        return constructors;
    }
}
