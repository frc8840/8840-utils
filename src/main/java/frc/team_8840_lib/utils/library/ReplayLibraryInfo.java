package frc.team_8840_lib.utils.library;

import frc.team_8840_lib.Info;
import frc.team_8840_lib.libraries.LibraryInfo;
import frc.team_8840_lib.libraries.LibraryManager;

public class ReplayLibraryInfo extends LibraryInfo {
    static {
        LibraryInfo info = new ReplayLibraryInfo();
        LibraryManager.registerLibrary(info);
    }

    public String name = "8840-utils-core-replay";
    public String version = Info.version();
    public String author = "Team 8840";
    public String description = "A library used for replaying log files. Core package.";
    public String repo = "frc8840/8840-utils";
}
