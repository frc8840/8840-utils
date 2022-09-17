package frc.team_8840_lib.info.console;

import edu.wpi.first.wpilibj.DriverStation;
import frc.team_8840_lib.info.time.TimeKeeper;
import frc.team_8840_lib.utils.GamePhase;
import frc.team_8840_lib.utils.time.TimeStamp;

public class Logger {
    private static final String timePrefixFormat = "[%%timeStamp%%] ";

    private static final String[] funStartingMessages = new String[] {
            "Break a leg",
            "Have fun",
            "Have a good time",
            "Support your alliance",
            "Be very cool"
    };

    public static void logCompetitionStart() {
        Log("-- Competition started! --", TimeStamp.All);
        String competitionName = DriverStation.getEventName();
        DriverStation.Alliance alliance = DriverStation.getAlliance();
        int matchNumber = DriverStation.getMatchNumber();
        DriverStation.MatchType matchType = DriverStation.getMatchType();

        Log(new String[] {
            "Welcome to the " + competitionName + "!",
            "Your Alliance: " + alliance,
            "Match Number: " + matchNumber,
            "Match Type: " + matchType,
            funStartingMessages[(int) Math.floor(Math.random() * funStartingMessages.length)] + ", " + alliance + "! Go 8840!"
        }, TimeStamp.None);
    }

    public static void logCompetitionEnd() {
        System.out.println("Competition ended!");
    }

    private static String getTimeLogString(TimeStamp timeStamp) {
        String time = "";

        switch (timeStamp) {
            case BothRealAndGameTime:
            case RealTime:
                time = "RL_Time: " + TimeKeeper.getInstance().getRealTimeStr();
                if (timeStamp == TimeStamp.RealTime) break;
            case GameTime:
                GamePhase gamePhase = GamePhase.getCurrentPhase();
                time += (timeStamp == TimeStamp.BothRealAndGameTime ? ", " : "") + "GT: " + TimeKeeper.getInstance().getPhaseTime(gamePhase) + " seconds";
                break;
            case RobotTime:
                time += "RT: " + TimeKeeper.getInstance().getRobotTime() + " seconds";
                break;
            default:
                break;
        }

        return time;
    }

    private static String formatTimePrefix(String time) {
        return timePrefixFormat.replace("%%timeStamp%%", time);
    }

    public static void Log(String message, TimeStamp timeStamp, boolean newLine) {
        if (newLine) System.out.println();

        System.out.print(formatTimePrefix(getTimeLogString(timeStamp)) + message);
    }

    public static void Log(String[] messages, TimeStamp timeStamp, boolean newLine) {
        if (newLine) System.out.println();
        System.out.print(formatTimePrefix(getTimeLogString(timeStamp)));
        for (String message : messages) System.out.println(message);
    }

    public static void Log(String[] messages, TimeStamp timeStamp) {
        Log(messages, timeStamp, true);
    }

    public static void Log(String message, TimeStamp timeStamp) {
        Log(message, timeStamp, true);
    }

    public static void Log(String[] messages) {
        Log(messages, TimeStamp.BothRealAndGameTime, true);
    }

    public static void Log(String message) {
        Log(message, TimeStamp.BothRealAndGameTime, true);
    }
}
