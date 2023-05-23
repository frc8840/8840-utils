package frc.team_8840_lib.utils.library;

import frc.team_8840_lib.Info;
import frc.team_8840_lib.libraries.LibraryInfo;
import frc.team_8840_lib.libraries.LibraryManager;

public class IOLibraryInfo extends LibraryInfo {
    static {
        LibraryInfo info = new IOLibraryInfo();
        LibraryManager.registerLibrary(info);
    }

    public String name = "8840-utils-core-io";
    public String version = Info.version();
    public String author = "Team 8840";
    public String description = "A library used for simulation and ease of NT communication. Core package.";
    public String repo = "frc8840/8840-utils";
}
