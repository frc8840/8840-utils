package frc.team_8840_lib.info.console;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import edu.wpi.first.wpilibj.DriverStation;
import frc.team_8840_lib.info.time.TimeKeeper;
import frc.team_8840_lib.listeners.Preferences;
import frc.team_8840_lib.utils.GamePhase;
import frc.team_8840_lib.utils.buffer.ByteConversions;
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
            "Be very cool",
            "Idk what to say. Good luck I guess",
            "Don't break the robot. Please",
            "This is a game. This is a simulation. Wake up wake up wake up wake up wake up wak k",
            "What do you call a robot pirate? Arrgghhh2-D2.",
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
            funStartingMessages[(int) Math.floor(Math.random() * funStartingMessages.length)] + ", " + alliance + "! Go team! 8840-utils made by FRC Team 8840 Bay Robotics (https://team8840.org)."
        }, TimeStamp.None);
    }

    public static void logCompetitionEnd() {
        System.out.println("Competition ended!");
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
        return timePrefixFormat.replace("%%timeStamp%%", time);
    }

    public static void Log(String group, String message) {
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
            saveAndUpdate(currentLine);
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
        logTimer.scheduleAtFixedRate(autoLogInfoTimer, 100, 100);
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

    public enum LogType {
        STRING,
        DOUBLE,
        STRING_ARRAY,
        DOUBLE_ARRAY,
        BYTE_ARRAY;

        public String smallString() {
            switch(this) {
                case STRING:
                    return "s";
                case DOUBLE:
                    return "d";
                case STRING_ARRAY:
                    return "S";
                case DOUBLE_ARRAY:
                    return "D";
                case BYTE_ARRAY:
                    return "B";
                default:
                    return "s";
            }
        }

        public static final byte STRING_CORRESPONDENCE = 1;
        public static final byte DOUBLE_CORRESPONDENCE = 2;
        public static final byte STRING_ARRAY_CORRESPONDENCE = 3;
        public static final byte DOUBLE_ARRAY_CORRESPONDENCE = 4;
        public static final byte BYTE_ARRAY_CORRESPONDENCE = 5;
    }

    private static final byte START = 1;
    private static final byte END = 2;

    private static int cycle = 0;

    private static HashMap<String, Integer> nameAssignedToMap = new HashMap<>();
    private static int nameAssignedTo = 0;

    private static HashMap<Integer, String> lastAssign = new HashMap<>();

    private static boolean readyToSave = false;
    public static void setReadyToSave(boolean ready) {
        readyToSave = ready;
    }

    private static void loadAndSaveAllAutoLogs() {
        writer.saveInfo("ALC" + cycle++ + (cycle < 3 ? "(s)" : ""));

        if (cycle < 3 || !readyToSave) return;

        try {
            for (Loggable klass : loggingClasses) {
                for (Method method : klass.getClass().getMethods()) {
                    AutoLog autoLogInfo = method.getAnnotation(AutoLog.class);

                    if (autoLogInfo == null) continue;
                    
                    String name = autoLogInfo.name();
                    LogType logType = autoLogInfo.logtype();
                    
                    if (name.contains("/")) {
                        //replace all slashes with underscores, just in case.
                        name = name.replaceAll("/", "_");
                    }

                    if (autoLogInfo != null) {
                        if (nameAssignedToMap.get(name) == null) {
                            nameAssignedToMap.put(name, nameAssignedTo++);
                            writer.saveInfo("a" + name + logType.smallString() + "/" + nameAssignedToMap.get(name));
                        }

                        int assignedTo = nameAssignedToMap.get(name);
                        String assignable = "";

                        try {
                            switch (logType) {
                                case STRING:
                                    assignable = (String) method.invoke(klass);
                                    break;
                                case DOUBLE:
                                    assignable = "" + (Double) method.invoke(klass);
                                    break;
                                case STRING_ARRAY:
                                    //save as string, separated by commas
                                    assignable = Arrays.toString((String[]) method.invoke(klass));
                                    break;
                                case DOUBLE_ARRAY:
                                    double[] doubles = (double[]) method.invoke(klass);

                                    //Convert to a string
                                    String doubleString = "[";

                                    for (double d : doubles) {
                                        doubleString += d + ",";
                                    }

                                    doubleString = doubleString.substring(0, doubleString.length() - 1) + "]";

                                    assignable = doubleString;
                                    break;
                                case BYTE_ARRAY:
                                    byte[] bytes = (byte[]) method.invoke(klass);

                                    //Convert to a string
                                    String byteString = "";
                                    
                                    for (byte b : bytes) {
                                        byteString += (char) b;
                                    }

                                    assignable = byteString;
                                    break;
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
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadAndSaveAllAutoLogsThroughBytes() {
        writer.saveInfo("AutoLog Cycle " + cycle++ + (cycle < 3 ? " (skipped due to early cycle)" : ""));

        if (cycle < 3 || !readyToSave) return;
        //CHange to not run this until the robot is fully initialized.
        //We skip the first 3 since the robot is not fully initialized yet + it usually causes annoying errors in console.
        //Should be fine after the first one.

        try {
            for (Loggable klass : loggingClasses) {
                for (Method method : klass.getClass().getMethods()) {
                    AutoLog autoLogInfo = method.getAnnotation(AutoLog.class);

                    if (autoLogInfo == null) continue;
                    
                    String name = autoLogInfo.name();
                    LogType logType = autoLogInfo.logtype();

                    if (autoLogInfo != null) {
                        byte[] result = new byte[1];

                        try {
                            switch (logType) {
                                case DOUBLE:
                                    byte[] doubleStart = new byte[] { 
                                        LogType.DOUBLE_CORRESPONDENCE
                                    };

                                    byte[] doubleConversion = ByteConversions.doubleToByteArray((double) method.invoke(klass));

                                    result = new byte[doubleStart.length + doubleConversion.length];

                                    //Join start and doubleConversion in result
                                    System.arraycopy(doubleStart, 0, result, 0, doubleStart.length);
                                    System.arraycopy(doubleConversion, 0, result, doubleStart.length, doubleConversion.length);
                                    
                                    break;
                                case DOUBLE_ARRAY:
                                    byte[] doubleArrayStart = new byte[] { 
                                        LogType.DOUBLE_ARRAY_CORRESPONDENCE
                                    };

                                    double[] preprocessResult = (double[]) method.invoke(klass);

                                    byte[][] doubleArrayConversion = new byte[preprocessResult.length][];

                                    for (int i = 0; i < doubleArrayConversion.length; i++) {
                                        doubleArrayConversion[i] = ByteConversions.doubleToByteArray(preprocessResult[i]);
                                    }

                                    int count = 0;

                                    for (byte[] _double : doubleArrayConversion) count += _double.length;

                                    result = new byte[count + doubleArrayStart.length];

                                    //Join start and doubleArrayConversion in result
                                    System.arraycopy(doubleArrayStart, 0, result, 0, doubleArrayStart.length);

                                    int index = doubleArrayStart.length;

                                    for (byte[] _double : doubleArrayConversion) {
                                        System.arraycopy(_double, 0, result, index, _double.length);

                                        index += _double.length;
                                    }

                                    break;
                                case STRING:
                                    byte[] stringStart = new byte[] { 
                                        LogType.STRING_CORRESPONDENCE
                                    };

                                    String str = (String) method.invoke(klass);

                                    byte[] stringConversion = ByteConversions.stringToByteArray(str);

                                    result = new byte[stringStart.length + stringConversion.length];

                                    //Join start and stringConversion in result
                                    System.arraycopy(stringStart, 0, result, 0, stringStart.length);
                                    System.arraycopy(stringConversion, 0, result, stringStart.length, stringConversion.length);

                                    break;
                                case STRING_ARRAY:
                                    byte[] stringArrayStart = new byte[] { 
                                        LogType.STRING_ARRAY_CORRESPONDENCE
                                    };
                                    
                                    String[] strs = (String[]) method.invoke(klass);

                                    if (strs.length == 0) {
                                        result = stringArrayStart;

                                        break;
                                    }

                                    byte[][] stringArrayConversion = new byte[strs.length][];

                                    for (int i = 0; i < stringArrayConversion.length; i++) {
                                        stringArrayConversion[i] = ByteConversions.stringToByteArray(strs[i]);
                                    }

                                    int count2 = 0;

                                    for (byte[] _string : stringArrayConversion) count2 += _string.length;

                                    result = new byte[count2 + stringArrayStart.length];

                                    //Join start and stringArrayConversion in result
                                    System.arraycopy(stringArrayStart, 0, result, 0, stringArrayStart.length);

                                    int index2 = stringArrayStart.length;

                                    for (byte[] _string : stringArrayConversion) {
                                        System.arraycopy(_string, 0, result, index2, _string.length);

                                        index2 += _string.length;
                                    }
                                    
                                    break;
                                case BYTE_ARRAY:
                                    byte[] byteArrayStart = new byte[] { 
                                        LogType.BYTE_ARRAY_CORRESPONDENCE
                                    };

                                    byte[] preResult = (byte[]) method.invoke(klass);

                                    result = new byte[preResult.length + byteArrayStart.length];

                                    //Join start and preResult in result
                                    System.arraycopy(byteArrayStart, 0, result, 0, byteArrayStart.length);
                                    System.arraycopy(preResult, 0, result, byteArrayStart.length, preResult.length);
                                    
                                    break;
                                default:
                                    result = new byte[] { (byte) 0 };
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw new IllegalArgumentException("[Logger] There was an error parsing the log in " + klass.getClass().getName() + ". Are the types matched up correctly?");
                        }

                        String convertedResult = ((char) START) + (char) ((byte) 0) + (char) ((byte) 0) + (char) ((byte) 0) + name + ((char) (byte) 0) + ((char) (byte) 0) + ((char) (byte) 0);

                        //Join result and end in convertedResult
                        convertedResult += new String(result);

                        convertedResult += (char) ((byte) 0);
                        convertedResult += (char) ((byte) 0);
                        convertedResult += (char) ((byte) 0);
                        convertedResult += END;

                        writer.saveInfo(convertedResult);
                    }
                }
            }
        } catch (Exception e) {
            if (cycle > 1) {
                e.printStackTrace();
                throw new IllegalArgumentException("[Logger] There was an error parsing the log in " + loggingClasses.get(0).getClass().getName() + ". Are the types matched up correctly?");
            }
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

    @AutoLog(name = "working", logtype = LogType.STRING)
    public String getWorking() {
        return "yep!";
    }
}
