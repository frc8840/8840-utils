package frc.team_8840_lib.utils.library;

import frc.team_8840_lib.Info;
import frc.team_8840_lib.libraries.LibraryInfo;
import frc.team_8840_lib.libraries.LibraryManager;

public class AutonomousLibraryInfo extends LibraryInfo {
    static {
        LibraryInfo info = new AutonomousLibraryInfo();
        LibraryManager.registerLibrary(info);
    }

    public String name = "8840-utils-core-autonomous";
    public String version = Info.version();
    public String author = "Team 8840";
    public String description = "A library of autonomous utilities for FRC teams. Core package.";
    public String repo = "frc8840/8840-utils";
}
