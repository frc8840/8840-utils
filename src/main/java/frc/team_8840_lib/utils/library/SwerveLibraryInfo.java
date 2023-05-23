package frc.team_8840_lib.utils.library;

import frc.team_8840_lib.Info;
import frc.team_8840_lib.libraries.LibraryInfo;
import frc.team_8840_lib.libraries.LibraryManager;

public class SwerveLibraryInfo extends LibraryInfo {
    static {
        LibraryInfo info = new SwerveLibraryInfo();
        LibraryManager.registerLibrary(info);
    }

    public String name = "8840-utils-core-swerve";
    public String version = Info.version();
    public String author = "Team 8840";
    public String description = "A swerve library for FRC teams. Core package. Note: Experimental, use at your own risk.";
    public String repo = "frc8840/8840-utils";
    public boolean experimental = true;
}
