package frc.team_8840_lib;

import frc.team_8840_lib.libraries.LibraryInfo;

public class Info extends LibraryInfo {
    public static String _name() {
        return new Info().name();
    }

    public static String _version() {
        return new Info().version();
    }

    public String name() {
        return "8840-utils-core";
    }

    public String version() {
        return "v2024.1.1";
    }

    public String author() {
        return "Team 8840";
    }

    public String description() {
        return "A library of utilities for FRC teams. Core package. Made by Team 8840.";
    }

    public String repo() {
        return "frc8840/8840-utils";
    }
}
