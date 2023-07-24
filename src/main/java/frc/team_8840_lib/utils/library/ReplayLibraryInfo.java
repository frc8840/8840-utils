package frc.team_8840_lib.utils.library;

import frc.team_8840_lib.Info;
import frc.team_8840_lib.libraries.LibraryInfo;

public class ReplayLibraryInfo extends LibraryInfo {
    public String name() {
        return "8840-utils-core-replay";
    }

    public String version() {
        return Info._version();
    }

    public String author() {
        return "Team 8840";
    }

    public String description() {
        return "A library used for replaying log files. Core package.";
    }

    public String repo() {
        return "frc8840/8840-utils";
    }
}
