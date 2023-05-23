package frc.team_8840_lib.replay;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import frc.team_8840_lib.info.console.AutoLog;
import frc.team_8840_lib.info.console.Logger;
import frc.team_8840_lib.info.console.Logger.LogType;
import frc.team_8840_lib.utils.IO.IOAccess;
import frc.team_8840_lib.utils.IO.IOLayer;
import frc.team_8840_lib.utils.IO.IOMethod;
import frc.team_8840_lib.utils.IO.IOMethodType;
import frc.team_8840_lib.utils.IO.IOPermission;
import frc.team_8840_lib.utils.IO.IOValue;
import frc.team_8840_lib.utils.logging.Loggable;

@IOAccess(IOPermission.READ_WRITE)
public abstract class Replayable extends IOLayer implements Loggable {
    private HashMap<String, String> replayLogs = new HashMap<>();
    private HashMap<String, Method> replaySaveMethods = new HashMap<>();
    //The load methods are any linked io write methods
    private HashMap<String, Method> replayLoadMethods = new HashMap<>();

    private boolean inReplay = false;

    public Replayable() {
        super();

        //Get the IOAccess annotation of this instance
        IOAccess ioAccess = this.getClass().getAnnotation(IOAccess.class);
        if (ioAccess == null) {
            throw new RuntimeException("Replayable must have IOAccess.READ_WRITE");
        }

        if (ioAccess.value() != IOPermission.READ_WRITE) {
            throw new RuntimeException("Replayable must have IOAccess.READ_WRITE");
        }

        //Get the IOAccess annotation of the parent class
        ioAccess = this.getClass().getSuperclass().getAnnotation(IOAccess.class);
        if (ioAccess == null) {
            throw new RuntimeException("Replayable must have IOAccess.READ_WRITE");
        }

        if (ioAccess.value() != IOPermission.READ_WRITE) {
            throw new RuntimeException("Replayable must have IOAccess.READ_WRITE");
        }

        //Get all methods in the super class with the AutoLog/IOMethod annotation
        Method[] methods = this.getClass().getSuperclass().getMethods();

        ArrayList<String> unlinkedWriteMethods = new ArrayList<>();
        ArrayList<String> waitingFor = new ArrayList<>();

        ArrayList<String> names = new ArrayList<>();

        //Loop through all methods
        for (Method method : methods) {
            //Get the AutoLog annotation of the method
            AutoLog autoLog = method.getAnnotation(AutoLog.class);

            //If the method has the AutoLog annotation, it's a write to the replay file method (save method)
            if (autoLog != null) {
                String link = autoLog.replaylink();

                names.add(autoLog.name());

                if (link == "" || link.length() == 0) continue;
                
                replayLogs.put(autoLog.name(), link);
                replaySaveMethods.put(autoLog.name(), method);

                if (unlinkedWriteMethods.contains(link)) {
                    unlinkedWriteMethods.remove(link);
                } else {
                    waitingFor.add(link);
                }
            }

            //Get the IOMethod annotation of the method
            IOMethod ioMethod = method.getAnnotation(IOMethod.class);

            if (ioMethod != null) {
                names.add(ioMethod.name());

                //If the method has the IOMethod annotation, it's a read from the replay file method (load method)
                if (ioMethod.method_type() != IOMethodType.WRITE) continue;

                //If the method is not linked to a write to file method yet, add it to the list of unlinked write methods
                if (!replayLogs.containsValue(ioMethod.name())) {
                    unlinkedWriteMethods.add(ioMethod.name());
                }

                if (waitingFor.contains(ioMethod.name())) {
                    waitingFor.remove(ioMethod.name());
                }
                
                replayLoadMethods.put(ioMethod.name(), method);
            }
        }

        //Check if there is any methods referenced in links, but are not found in the class.
        if (waitingFor.size() > 0) {
            String waitingForString = "";
            for (String string : waitingFor) {
                waitingForString += string + ", ";
            }

            throw new RuntimeException("Replayable methods " + waitingForString + " were not linked. Please make sure you add in the correct annotations.");
        }

        //Check if names are unique
        if (names.size() != names.stream().distinct().count()) {
            throw new RuntimeException("All methods in a replayable class must have unique names.");
        }

        Logger.Log(getBaseName(), "Added " + replayLogs.size() + " replay logs.");
        
        ReplayManager.addReplayable(this);
    }

    @AutoLog(logtype = LogType.BOOLEAN, name = "replay", replaylink = "inReplay")
    public boolean replayable() {
        return true;
    }

    @IOMethod(name = "inReplay", value_type = IOValue.BOOLEAN, method_type = IOMethodType.WRITE, toNT = false)
    public void replay(boolean inReplay) {
        this.inReplay = inReplay;
    }

    public Method getReplaySaveMethod(String name) {
        return replaySaveMethods.get(name);
    }

    public boolean replaying() {
        return inReplay;
    }

    //Overridable methods
    public abstract String getBaseName();

    public abstract void replayInit();

    public abstract void exitReplay();
}
