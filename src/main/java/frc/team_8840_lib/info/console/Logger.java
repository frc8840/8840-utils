package frc.team_8840_lib.info.console;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.PowerDistribution;
import frc.team_8840_lib.info.time.TimeKeeper;
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

        switch (timeStamp) {
            case BothRealAndGameTime:
            case RealTime:
                time = "RT: " + TimeKeeper.getInstance().getRealTimeStr();
                if (timeStamp == TimeStamp.RealTime) break;
            case GameTime:
                GamePhase gamePhase = GamePhase.getCurrentPhase();
                time += (timeStamp == TimeStamp.BothRealAndGameTime ? ", " : "") + "GT: " + TimeKeeper.getInstance().getPhaseTime(gamePhase) + " seconds";
                break;
            case RobotTime:
                time += "RRT: " + TimeKeeper.getInstance().getRobotTime() + " seconds";
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

                loadAndSaveAllAutoLogs();
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

        public static final byte STRING_CORRESPONDENCE = 1;
        public static final byte DOUBLE_CORRESPONDENCE = 2;
        public static final byte STRING_ARRAY_CORRESPONDENCE = 3;
        public static final byte DOUBLE_ARRAY_CORRESPONDENCE = 4;
        public static final byte BYTE_ARRAY_CORRESPONDENCE = 5;
    }

    private static final byte START = 1;
    private static final byte END = 2;

    private static int cycle = 0;

    private static void loadAndSaveAllAutoLogs() {
        writer.saveInfo("AutoLog Cycle " + cycle++);

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
                                    result = new byte[] { 
                                        LogType.DOUBLE_CORRESPONDENCE,
                                        ((byte) ((double) method.invoke(klass))) 
                                    };
                                    
                                    break;
                                case DOUBLE_ARRAY:
                                    double[] preprocessResult = (double[]) method.invoke(klass);

                                    result = new byte[preprocessResult.length + 1];
                                    result[0] = LogType.DOUBLE_ARRAY_CORRESPONDENCE;

                                    for (int i = 0; i < preprocessResult.length; i++) {
                                        result[i + 1] = (byte) preprocessResult[i];
                                    }

                                    break;
                                case STRING:
                                    String str = (String) method.invoke(klass);
                                    char[] chars = str.toCharArray();

                                    result = new byte[chars.length + 1];
                                    result[0] = LogType.STRING_CORRESPONDENCE;

                                    for (int i = 0; i < chars.length; i++) {
                                        result[i + 1] = (byte) chars[i];
                                    }

                                    break;
                                case STRING_ARRAY:
                                    String[] strs = (String[]) method.invoke(klass);

                                    if (strs.length == 0) {
                                        result = new byte[] {
                                            LogType.STRING_ARRAY_CORRESPONDENCE
                                        };

                                        break;
                                    }

                                    int count = strs.length;

                                    for (String _str : strs) count += _str.length();

                                    int writer = 1;
                                    result = new byte[count];
                                    
                                    for (int i = 0; i < strs.length; i++) {
                                        char[] brokenString = strs[i].toCharArray();

                                        for (int j = 0; j < strs.length; j++) {
                                            result[writer] = (byte) brokenString[j];
                                        }
                                    }
                                    
                                    break;
                                case BYTE_ARRAY:
                                    byte[] preResult = (byte[]) method.invoke(klass);

                                    result = new byte[preResult.length + 1];
                                    result[0] = LogType.BYTE_ARRAY_CORRESPONDENCE;

                                    //I know there's a better method for this but I don't have internet so this is what ill do lol
                                    for (int i = 0; i < preResult.length; i++) {
                                        result[i + 1] = preResult[i];
                                    }
                                    
                                    break;
                                default:
                                    result = new byte[] { (byte) 0 };
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw new IllegalArgumentException("[Logger] There was an error parsing the log in " + klass.getClass().getName() + ". Are the types matched up correctly?");
                        }

                        String convertedResult = ((char) START) + (char) ((byte) 0) + (char) ((byte) 0) + (char) ((byte) 0) + name + ((char) (byte) 0) + ((char) (byte) 0) + ((char) (byte) 0);

                        for (byte _byte : result) {
                            convertedResult += (char) _byte;
                        }

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

    @AutoLog(logtype = LogType.BYTE_ARRAY, name = "Power Distribution Info")
    public byte[] logPD() {
        PowerDistribution pd = new PowerDistribution();

        int nOfChannels = pd.getNumChannels();

        double tempature = pd.getTemperature();
        double totalCurrent = pd.getTotalCurrent();
        double voltage = pd.getVoltage();

        //Format: 
        /**
         * 0: voltage
         * 1: totalCurrent
         * 2: empty (0)
         * 3: tempature
         * 4: empty (0)
         * 5: number of channels
         * 6: empty (0)
         * 7+: current of channel 0 to n
         */

        byte[] pdInfo = new byte[7 + nOfChannels];
        pdInfo[0] = (byte) voltage;
        pdInfo[1] = (byte) totalCurrent;
        pdInfo[2] = (byte) 0d;
        pdInfo[3] = (byte) tempature;
        pdInfo[4] = (byte) 0d;
        pdInfo[5] = (byte) nOfChannels;
        pdInfo[6] = (byte) 0d;
        for (int i = 0; i < nOfChannels; i++) {
            pdInfo[i + 7] = (byte) pd.getCurrent(i);
        }

        pd.close();
    
        return pdInfo;
    }
}
