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
        return "Jaiden Grimminck - 8840";
    }

    public String description() {
        return "A swerve library for FRC teams with MK4i + NEOs + CANCoders. Core package. Note: This library may not work with your robot if you're not 8840! Use at your own discretion.";
    }

    public String repo() {
        return "frc8840/8840-utils";
    }

    public boolean experimental() {
        return false;
    }
}
