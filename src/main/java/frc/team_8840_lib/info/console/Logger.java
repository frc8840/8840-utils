package frc.team_8840_lib.info.console;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import frc.team_8840_lib.info.time.TimeKeeper;
import frc.team_8840_lib.libraries.LibraryManager;
import frc.team_8840_lib.listeners.Preferences;
import frc.team_8840_lib.utils.GamePhase;
import frc.team_8840_lib.utils.logging.LogWriter;
import frc.team_8840_lib.utils.logging.Loggable;
import frc.team_8840_lib.utils.time.TimeStamp;

public class Logger implements Loggable {
    private static final String timePrefixFormat = "[%%timeStamp%%] ";

    private static final String[] funStartingMessages = new String[] {
            "Break a leg",
            "Have fun",
            "Have a good time",
            "Support your alliance",
            "Good luck",
            "Don't break the robot",
    };

    private static HashMap<String, ArrayList<String>> threads = new HashMap<>();

    public static void logCompetitionStart() {
        Log("\n-- Competition started! --", TimeStamp.All);
        String competitionName = DriverStation.getEventName();
        DriverStation.Alliance alliance = DriverStation.getAlliance().orElse(Alliance.Blue);
        int matchNumber = DriverStation.getMatchNumber();
        DriverStation.MatchType matchType = DriverStation.getMatchType();

        Log(new String[] {
            "Welcome to " + competitionName + "!",
            "Libraries Loaded: " + LibraryManager.getLoadedLibraries().size(),
            "Your Alliance: " + alliance,
            "Match Number: " + matchNumber,
            "Match Type: " + matchType,
            funStartingMessages[(int) Math.floor(Math.random() * funStartingMessages.length)] + ", " + alliance + "! Go team! 8840-utils is made by FRC Team 8840, Bay Robotics.",
            "https://team8840.org | https://github.com/frc8840",
            "",
            "--------------------",
            ""
        }, TimeStamp.None);
    }

    public static void logCompetitionEnd() {
        System.out.println("Competition ended!");
    }

    private static void addToThread(String threadName, String message) {
        if (!Logger.threads.containsKey(threadName)) {
            Logger.threads.put(threadName, new ArrayList<>());
        }

        Logger.threads.get(threadName).add(message);
    }

    private static String getTimeLogString(TimeStamp timeStamp) {
        String time = "";

        if (TimeKeeper.getInstance() == null) {
            return "PRE_INIT";
        }

        switch (timeStamp) {
            case BothRealAndGameTime:
            case RealTime:
                time = "RT: " + TimeKeeper.getInstance().getRealTimeStr();
                if (timeStamp == TimeStamp.RealTime) break;
            case GameTime:
                GamePhase gamePhase = GamePhase.getCurrentPhase();
                time += (timeStamp == TimeStamp.BothRealAndGameTime ? ", " : "") + "GT: " + TimeKeeper.getInstance().getPhaseTime(gamePhase) + "s";
                break;
            case RobotTime:
                time += "RBT: " + TimeKeeper.getInstance().getRobotTime() + "s";
                break;
            default:
                break;
        }

        return time;
    }

    private static String formatTimePrefix(String time) {
        if (time == "") return "";

        return timePrefixFormat.replace("%%timeStamp%%", time);
    }

    public static void Error(String group, Exception e) {
        e.printStackTrace();
        if (writer != null) {
            saveAndUpdate("ERROR: " + e.getMessage());
        }
    }

    public static void Log(String group, String message) {
        addToThread(group, message);
        Log("[" + group + "] " + message);
    }

    public static void Log(String message, TimeStamp timeStamp, boolean newLine) {
        p(formatTimePrefix(getTimeLogString(timeStamp)) + message, newLine);
    }

    public static void Log(String[] messages, TimeStamp timeStamp, boolean newLine) {
        p(formatTimePrefix(getTimeLogString(timeStamp)), newLine);
        for (String message : messages) p(message, true);
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

    private static String currentLine = "";

    private static void p(String raw, boolean printLN) {
        if (printLN) {
            System.out.println(raw);
            currentLine += raw;
            saveAndUpdate(
                //add a [] if there is no time stamp
                (!raw.startsWith("[") ? "[] " : "") + 
                currentLine
            );
            currentLine = "";
        } else {
            System.out.print(raw);
            currentLine += raw;
        }
    }

    private static ArrayList<Loggable> loggingClasses;

    public static void addClassToBeAutoLogged(Loggable logger) {
        if (loggingClasses == null) loggingClasses = new ArrayList<Loggable>();

        loggingClasses.add(logger);
    };

    private static LogWriter writer;

    /**
     * Sets the log writer of the Robot
     * @param writer LogWriter for the robot
     */
    public static void setWriter(LogWriter writer) {
        Logger.writer = writer;
    }

    public static String getLogWriterName() {
        return writer.getClass().getSimpleName();
    }

    private static boolean errorWhileInitializingWriter = false;

    /**
     * Initialize the LogWriter. Do not call though since this will be called in the backend of the API.
     * This method also starts saving the auto logs.
     */
    public static void initWriter() {
        if (writer == null) {
            Logger.Log("[LoggerInfo] No LogWriter was set. Skipping initialization.", TimeStamp.None);

            return;
        } else {
            Logger.Log("[LoggerInfo] LogWriter was set to " + writer.getClass().getName() + ". Initializing...", TimeStamp.None);
        }

        try {
            writer.initialize();
        } catch (Exception e) {
            Logger.Log("[LoggerError] LogWriter failed to initialize. Skipping initialization. Stacktrace:", TimeStamp.None);
            e.printStackTrace();

            errorWhileInitializingWriter = true;

            return;
        }

        Logger.Log("[LoggerInfo] LogWriter initialized successfully. Starting auto logging...", TimeStamp.None);
        
        autoLogInfoTimer = new TimerTask() {
            @Override
            public void run() {
                if (loggingClasses == null) return;
                
                try {
                    loadAndSaveAllAutoLogs();
                } catch (Exception e) {
                    //TODO: Save error to log file.

                    Logger.Log("[Logger] Error while auto logging. Skipping past this log, but this may lead to logging missing information.", TimeStamp.None);
                }
            }
        };

        logTimer = new Timer();
        logTimer.scheduleAtFixedRate(autoLogInfoTimer, logInterval, logInterval);
    }

    private static void saveAndUpdate(String nl) {
        if (writer == null || errorWhileInitializingWriter) return;

        writer.saveLine(nl);
    }

    public static void closeLogger() {
        if (autoLogInfoTimer != null) autoLogInfoTimer.cancel();

        if (writer == null || errorWhileInitializingWriter) return;

        writer.close();
    }

    private static TimerTask autoLogInfoTimer;
    private static Timer logTimer;
    private static long logInterval = 100;

    public static long getLogInterval() {
        return logInterval;
    }

    public enum LogType {
        STRING,
        INT,
        DOUBLE,
        BOOLEAN,
        STRING_ARRAY,
        DOUBLE_ARRAY,
        BYTE_ARRAY,
        UNKNOWN;

        public String smallString() {
            switch(this) {
                case STRING:
                    return "s";
                case DOUBLE:
                    return "d";
                case INT:
                    return "i";
                case STRING_ARRAY:
                    return "S";
                case DOUBLE_ARRAY:
                    return "D";
                case BYTE_ARRAY:
                    return "B";
                case BOOLEAN:
                    return "b";
                default:
                    return "s";
            }
        }

        public static LogType fromSmallString(String s) {
            switch (s) {
                case "s":
                    return STRING;
                case "d":
                    return DOUBLE;
                case "i":
                    return INT;
                case "S":
                    return STRING_ARRAY;
                case "D":
                    return DOUBLE_ARRAY;
                case "B":
                    return BYTE_ARRAY;
                case "b":
                    return BOOLEAN;
                default:
                    return STRING;
            }
        }

        public static LogType fromClass(Class<?> klass) {
            if (klass == String.class) return STRING;
            if (klass == Integer.class || klass == int.class) return INT;
            if (klass == Double.class || klass == double.class) return DOUBLE;
            if (klass == Boolean.class || klass == boolean.class) return BOOLEAN;
            if (klass == String[].class) return STRING_ARRAY;
            if (klass == Double[].class || klass == double[].class) return DOUBLE_ARRAY;
            if (klass == byte[].class) return BYTE_ARRAY;

            return UNKNOWN;
        }

        public static final byte STRING_CORRESPONDENCE = 1;
        public static final byte DOUBLE_CORRESPONDENCE = 2;
        public static final byte STRING_ARRAY_CORRESPONDENCE = 3;
        public static final byte DOUBLE_ARRAY_CORRESPONDENCE = 4;
        public static final byte BYTE_ARRAY_CORRESPONDENCE = 5;
    }

    private static int cycle = 0;

    private static HashMap<String, Integer> nameAssignedToMap = new HashMap<>();
    private static int nameAssignedTo = 0;

    private static HashMap<Integer, String> lastAssign = new HashMap<>();

    private static boolean readyToSave = false;
    public static void setReadyToSave(boolean ready) {
        readyToSave = ready;
    }

    @SuppressWarnings({"null", "deprecated"})
    private static void loadAndSaveAllAutoLogs() {
        writer.saveInfo("ALC" + cycle++ + (cycle < 3 ? "(s)" : ""));

        if (cycle < 3 || !readyToSave) return;

        try {
            for (Loggable klass : loggingClasses) {
                Field[] fields = klass.getClass().getDeclaredFields();
                Method[] methods = klass.getClass().getMethods();

                boolean movedToMethods = false;

                for (int i = 0; i < fields.length + methods.length; i++) {
                    AutoLog autoLogInfo;
                    Method method = null;
                    Field field = null;

                    if (i >= fields.length) {
                        if (!movedToMethods) {
                            movedToMethods = true;
                        }
                    }

                    boolean fieldWasAccessible = false;

                    if (!movedToMethods) {
                        autoLogInfo = fields[i].getAnnotation(AutoLog.class);
                        field = fields[i];

                        fieldWasAccessible = field.isAccessible();
                        field.setAccessible(true);
                    } else {
                        autoLogInfo = methods[i - fields.length].getAnnotation(AutoLog.class);
                        method = methods[i - fields.length];
                    }

                    if (autoLogInfo == null) continue;
                    if (method == null && movedToMethods) continue;
                    if (field == null && !movedToMethods) continue;
                    
                    String name = autoLogInfo.name();
                    
                    if (name == "" || name.length() == 0) {
                        if (movedToMethods) {
                            name = method.getName();
                        } else {
                            name = field.getName();
                        }
                    }

                    //if (!movedToMethods) System.out.println("name: " + name + ", " + field.getName());

                    LogType logType = LogType.fromClass(movedToMethods ? method.getReturnType() : field.getType());
                    
                    if (name.contains("/")) {
                        //replace all slashes with underscores, just in case.
                        name = name.replaceAll("/", "|");
                    }

                    if (autoLogInfo != null) {
                        if (nameAssignedToMap.get(name) == null) {
                            nameAssignedToMap.put(name, nameAssignedTo++);
                            writer.saveInfo("a" + klass.getBaseName() + "|" + name + logType.smallString() + "/" + nameAssignedToMap.get(name));
                        }

                        int assignedTo = nameAssignedToMap.get(name);
                        String assignable = "";

                        try {
                            switch (logType) {
                                case STRING:
                                    assignable = movedToMethods ? (String) method.invoke(klass).toString() : (String) field.get(klass).toString();
                                    break;
                                case INT:
                                    assignable = movedToMethods ? "" + (Integer) method.invoke(klass) : "" + (Integer) field.get(klass);
                                    break;
                                case DOUBLE:
                                    assignable = movedToMethods ? "" + (Double) method.invoke(klass) : "" + (Double) field.get(klass);
                                    break;
                                case BOOLEAN:
                                    assignable = movedToMethods ? "" + (Boolean) method.invoke(klass) : "" + (Boolean) field.get(klass);
                                    break;
                                case STRING_ARRAY:
                                    //save as string, separated by commas
                                    assignable = movedToMethods ? Arrays.toString((String[]) method.invoke(klass)) : Arrays.toString((String[]) field.get(klass));
                                    break;
                                case DOUBLE_ARRAY:
                                    double[] doubles = movedToMethods ? (double[]) method.invoke(klass) : (double[]) field.get(klass);

                                    //Convert to a string
                                    String doubleString = "["; 

                                    for (double d : doubles) {
                                        doubleString += d + ",";
                                    }

                                    doubleString = doubleString.substring(0, doubleString.length() - 1) + "]";

                                    assignable = doubleString;
                                    break;
                                case BYTE_ARRAY:
                                    byte[] bytes = movedToMethods ? (byte[]) method.invoke(klass) : (byte[]) field.get(klass);

                                    //Convert to a string
                                    String byteString = "";
                                    
                                    for (byte b : bytes) {
                                        byteString += (char) b;
                                    }

                                    assignable = byteString;
                                    break;
                                case UNKNOWN:
                                    throw new Exception("Unknown log type! Please only use String, int, double, boolean, String[], double[], or byte[], or encode your information into a String!");
                            }
                        } catch (Exception e) {
                            Logger.Log("e/" + assignedTo + " [Logger] Issue w/ auto log " + name + ". Skipping past this log, but this may lead to logging missing information.", TimeStamp.None);
                            e.printStackTrace();
                        }

                        if (assignable.length() > 0) {
                            if (lastAssign.get(assignedTo) != null && lastAssign.get(assignedTo).equals(assignable)) continue;

                            writer.saveInfo("d" + assignedTo + "/" + assignable);

                            lastAssign.put(assignedTo, assignable);
                        }
                    }

                    if (field != null) field.setAccessible(fieldWasAccessible);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean lockLogWriterToOnlyCode = true;

    public static boolean logWriterIsLockedToCode() {
        return lockLogWriterToOnlyCode;
    }

    public static void assignLogWriterThroughPreferences(Path preferencesFilePath, Class<LogWriter>[] logWriters, Class<LogWriter> _default) {
        if (!Preferences.loaded()) Preferences.loadPreferences(preferencesFilePath);

        lockLogWriterToOnlyCode = false;

        for (Class<LogWriter> logWriter : logWriters) {
            if (Preferences.getSelectedLogger() == logWriter.getClass().getSimpleName()) {
                try {
                    LogWriter writer = (LogWriter) logWriter.getConstructors()[0].newInstance();

                    Logger.Log("[Logger] Successfully loaded " + logWriter.getClass().getSimpleName() + " as the logger!");

                    setWriter(writer);
                    return;
                } catch (Exception e) {
                    e.printStackTrace();

                    Logger.Log("[Logger] There was an error loading " + logWriter.getClass().getSimpleName() + " as the logger! Falling back to " + _default.getClass().getSimpleName() + "...");

                    try {
                        LogWriter _defaultInstance = (LogWriter) _default.getConstructors()[0].newInstance();

                        Logger.Log("[Logger] Successfully loaded " + _default.getClass().getSimpleName() + " as the logger!");

                        setWriter(_defaultInstance);
                    } catch (Exception e2) {
                        e2.printStackTrace();
                        Logger.Log("[Logger] There was an error loading " + _default.getClass().getSimpleName() + " as the logger! Falling back to an empty logger...");

                        setWriter(new EmptyLog());

                        return;
                    }
                }

                return;
            }
        }

        try {
            LogWriter _defaultInstance = (LogWriter) _default.getConstructors()[0].newInstance();

            Logger.Log("[Logger] Successfully loaded " + _default.getClass().getSimpleName() + " as the logger!");

            setWriter(_defaultInstance);
        } catch (Exception e2) {
            e2.printStackTrace();
            Logger.Log("[Logger] There was an error loading " + _default.getClass().getSimpleName() + " as the logger! Falling back to an empty logger...");

            setWriter(new EmptyLog());

            return;
        }
    }

    @AutoLog(name = "working")
    public String getWorking() {
        return "y";
    }

    public String getBaseName() {
        return "Logger";
    }
}
