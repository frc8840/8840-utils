package frc.team_8840_lib;

import frc.team_8840_lib.libraries.LibraryInfo;
import frc.team_8840_lib.libraries.LibraryManager;

public class Info extends LibraryInfo {
    public static String name() {
        return new Info().name;
    }

    public static String version() {
        return new Info().version;
    }

    static {
        System.out.println("Loading 8840-utils-core...");

        LibraryInfo info = new Info();
        LibraryManager.registerLibrary(info);

        System.out.println("Loaded 8840-utils-core.");
    }

    public String name = "8840-utils-core";
    public String version = "v2023.3.0";
    public String author = "Team 8840";
    public String description = "A library of utilities for FRC teams. Core package. Made by Team 8840.";
    public String repo = "frc8840/8840-utils";
}
