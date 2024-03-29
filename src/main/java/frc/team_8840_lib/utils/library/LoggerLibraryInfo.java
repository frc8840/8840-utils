package frc.team_8840_lib.utils.library;

import frc.team_8840_lib.Info;
import frc.team_8840_lib.libraries.LibraryInfo;

public class LoggerLibraryInfo extends LibraryInfo {
    public String name() {
        return "8840-utils-core-logger";
    }

    public String version() {
        return Info._version();
    }

    public String author() {
        return "Jaiden Grimminck - 8840";
    }

    public String description() {
        return "A logging library for FRC teams. Core package.";
    }

    public String repo() {
        return "frc8840/8840-utils";
    }

    public boolean experimental() {
        return true;
    }
}
