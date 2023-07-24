package frc.team_8840_lib.utils.library;

import frc.team_8840_lib.Info;
import frc.team_8840_lib.libraries.LibraryInfo;

public class SwerveLibraryInfo extends LibraryInfo {
    public String name() {
        return "8840-utils-core-swerve";
    }

    public String version() {
        return Info._version();
    }

    public String author() {
        return "Team 8840";
    }

    public String description() {
        return "A swerve library for FRC teams. Core package. Note: Experimental, use at your own risk.";
    }

    public String repo() {
        return "frc8840/8840-utils";
    }

    public boolean experimental() {
        return true;
    }
}
