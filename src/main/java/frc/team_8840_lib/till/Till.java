package frc.team_8840_lib.till;

import com.sun.net.httpserver.HttpExchange;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.TimerTask;

import frc.team_8840_lib.info.console.Logger;
import frc.team_8840_lib.info.time.TimeKeeper;
import frc.team_8840_lib.listeners.EventListener;
import frc.team_8840_lib.listeners.Robot;
import frc.team_8840_lib.utils.GamePhase;
import frc.team_8840_lib.utils.http.Constructor;
import frc.team_8840_lib.utils.http.Route;
import frc.team_8840_lib.utils.math.IntRange;

public class Till extends EventListener {
    private static Till instance;

    public static Till getInstance() {
        return instance;
    }

    private enum MethodType {
        INIT,
        FIXED,
        PERIODIC;
    }

    private class MethodPair {
        private MethodType type;
        private GamePhase phase;

        public MethodPair(MethodType type, GamePhase phase) {
            this.type = type;
            this.phase = phase;
        }
        
        public MethodType getType() {
            return this.type;
        }

        public GamePhase getPhase() {
            return this.phase;
        }
    }

    private boolean hasCode = false;
    private boolean waitingForUpload = false;

    private String[] raw = new String[0];
    private Path path;

    private TillHandler handler = new TillHandler();

    private HashMap<MethodPair, IntRange> methodReferences = new HashMap<>();

    public Till(Path path) {
        super();

        instance = this;

        this.path = path;
    }

    public Till() {
        super();

        instance = this;

        waitingForUpload = true;
    }

    private void loadFromFile() {
        File file = path.toFile();

        if (file.exists()) {
            try {
                this.raw = Files.readAllLines(path).toArray(new String[0]);

                this.hasCode = true;

                Logger.Log("Till", "Loaded file!");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Logger.Log("[Till] File does not exist!");
        }
    }

    private void init() {
        Logger.Log("[Till] Till initialized!");

        if (waitingForUpload) {
            
        } else {
            loadFromFile();
        }

        if (!this.hasCode) {
            Logger.Log("[Till] No code found! Quitting...");
            throw new IllegalArgumentException("No code found!");
        }

        Logger.Log("[Till] Code found! Parsing...");

        //First, we need to identify the methods that are in the Till file.
        int start = -1;

        for (int i = 0; i < raw.length; i++) {
            String line = raw[i];
            if (line.startsWith("#DECLARE")) {
                start = i;
            } else if (line.startsWith("#ENDDECLARE")) {
                if (start == -1) {
                    Logger.Log("[Till] There was an error parsing the Till file. Quitting...");
                    return;
                }

                String startLine = raw[start];

                String methodName = startLine.substring("#DECLARE".length()).trim().replaceAll("\\(", "").replaceAll("\\)", "");

                if (methodName.equalsIgnoreCase("ROBOTINIT")) {
                    methodReferences.put(new MethodPair(MethodType.INIT, null), new IntRange(start, i));
                    continue;
                } else if (methodName.equalsIgnoreCase("ROBOTPERIODIC")) {
                    methodReferences.put(new MethodPair(MethodType.PERIODIC, null), new IntRange(start, i));
                    continue;
                }

                boolean fixed = methodName.contains("--fixed");

                if (fixed) {
                    methodName = methodName.split(" ")[0];
                }

                MethodType type = MethodType.FIXED;

                if (methodName.contains("INIT")) {
                    methodName = methodName.replaceAll("INIT", "");
                    type = MethodType.INIT;
                } else if (methodName.contains("PERIODIC")) {
                    methodName = methodName.replaceAll("PERIODIC", "");
                    type = MethodType.PERIODIC;
                }

                GamePhase phase = GamePhase.getFromName(methodName);

                //We put in the method references here - we'll use these later to call the methods.
                //It'll go from the first line (the #DECLARE line) to the last line (the #ENDDECLARE line).
                methodReferences.put(new MethodPair(type, phase), new IntRange(start, i));
            }
        }

        Logger.Log("[Till] Registered methods, found " + methodReferences.size() + " methods.");
    }

    private GamePhase lastLoggedPeriodic = null;
    private GamePhase lastSuccessPeriodic = null;

    public void handleGamePhase(MethodType type, GamePhase phase) {
        MethodPair found = null;

        for (MethodPair pair : methodReferences.keySet()) {
            if (pair.getType() == type && pair.getPhase() == phase) {
                found = pair;
            }
        }

        if (found == null) {
            if (phase != null) {
                if (type == MethodType.INIT) {
                    Logger.Log("[Till] Could not find method for " + phase.name() + " init!");
                } else if (type == MethodType.PERIODIC) {
                    if (lastLoggedPeriodic != phase) {
                        Logger.Log("[Till] Could not find method for " + phase.name() + " periodic!");
                        lastLoggedPeriodic = phase;
                    }
                }
            }

            //We don't need fixed phase since they're addons, not required.

            return;
        } else {
            if (phase != null) {
                if (lastSuccessPeriodic != phase) {
                    Logger.Log("Till", "Starting " + phase.name() + " " + type.name() + " method!");
                    lastSuccessPeriodic = phase;
                }
            }
        }

        IntRange range = methodReferences.get(found);

        for (int i = range.getMin() + 1; i < range.getMax(); i++) {
            String line = raw[i];

            if (line.startsWith("#DECLARE")) continue;

            handler.handleLine(line);
        }
    }

    @Override
    public void robotInit() {
        Logger.Log("[Till] Robot initialized!");

        init();

        Robot.getInstance().subscribeFixedPhase(new TimerTask() {
            @Override
            public void run() {
                handleGamePhase(MethodType.FIXED, GamePhase.Autonomous);
            }
        }, GamePhase.Autonomous);

        Robot.getInstance().subscribeFixedPhase(new TimerTask() {
            @Override
            public void run() {
                handleGamePhase(MethodType.FIXED, GamePhase.Teleop);
            }
        }, GamePhase.Teleop);

        Robot.getInstance().subscribeFixedPhase(new TimerTask() {
            @Override
            public void run() {
                handleGamePhase(MethodType.FIXED, GamePhase.Test);
            }
        }, GamePhase.Test);

        Robot.getInstance().subscribeFixedPhase(new TimerTask() {
            @Override
            public void run() {
                handleGamePhase(MethodType.FIXED, GamePhase.Disabled);
            }
        }, GamePhase.Disabled);

        Robot.getRealInstance().waitForFullfillConditions(3000, () -> {
            return runnable();
        }).onFinishFullfillment(() -> {
            if (runnable()) {
                handleGamePhase(MethodType.INIT, null);
            } else {
                if (!waitingForUpload) Logger.Log("Till", "Unable to successfully initialize Till! :(");
            }
        });
    }

    @Override
    public void robotPeriodic() {
        if (!runnable()) return;
        handleGamePhase(MethodType.PERIODIC, null);
    }

    @Override
    public void onAutonomousEnable() {
        if (!runnable()) return;
        handleGamePhase(MethodType.INIT, GamePhase.Autonomous);
    }

    @Override
    public void onAutonomousPeriodic() {
        if (!runnable()) return;
        handleGamePhase(MethodType.PERIODIC, GamePhase.Autonomous);
    }

    @Override
    public void onTeleopEnable() {
        if (!runnable()) return;
        handleGamePhase(MethodType.INIT, GamePhase.Teleop);
    }

    @Override
    public void onTeleopPeriodic() {
        if (!runnable()) return;
        handleGamePhase(MethodType.PERIODIC, GamePhase.Teleop);
    }

    @Override
    public void onTestEnable() {
        if (!runnable()) return;
        handleGamePhase(MethodType.INIT, GamePhase.Test);
    }

    @Override
    public void onTestPeriodic() {
        if (!runnable()) return;
        handleGamePhase(MethodType.PERIODIC, GamePhase.Test);
    }

    @Override
    public void onDisabled() {
        if (!runnable()) return;
        handleGamePhase(MethodType.INIT, GamePhase.Disabled);
    }

    @Override
    public void onDisabledPeriodic() {
        if (!runnable()) return;
        handleGamePhase(MethodType.PERIODIC, GamePhase.Disabled);
    }

    public boolean runnable() {
        return this.hasCode;
    }

    public static Constructor getTillRequest() {
        return new Constructor() {
            @Override
            public Route.Resolution finish(HttpExchange req, Route.Resolution res) {
                if (getInstance() == null) {
                    return res.json(this.error("Till is not initialized."));
                }
                

                return res;
            }
        };
    }
    
}
