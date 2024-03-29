package frc.team_8840_lib.replay;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import frc.team_8840_lib.info.console.FileWriter;
import frc.team_8840_lib.info.console.Logger;
import frc.team_8840_lib.utils.files.FileUtils;

public class ReplayManager {
    private static ReplayManager instance;

    public static ReplayManager getInstance() {
        if (instance == null) {
            instance = new ReplayManager();
        }
        return instance;
    }

    public static void addReplayable(Replayable replayable) {
        getInstance().replayables.add(replayable);
    }

    private ArrayList<Replayable> replayables = new ArrayList<>();

    private HashMap<Replayable, Boolean> preReplayStates = new HashMap<>();

    private int replayCycle = 0;

    private ReplayManager() {
        replayables = new ArrayList<>();
    }

    Timer replayTimer;

    public void enterReplay(File file) {
        if (!file.exists()) {
            throw new RuntimeException("Log file does not exist!");
        }

        //check file extension
        String extension = file.getName().substring(file.getName().lastIndexOf(".") + 1);
        if (!extension.equals(FileWriter.getExtension())) {
            throw new RuntimeException("Log file is not a " + FileWriter.getExtension() + " file!");
        }

        ReplayLog replayLog = new ReplayLog(FileUtils.read(file));

        for (Replayable replayable : replayables) {
            preReplayStates.put(replayable, replayable.isReal());
            replayable.setReal(false);
            replayable.replayInit();
        }

        replayCycle = 0;

        final int totalReplayCycles = replayLog.getCycles();

        replayTimer = new Timer();

        TimerTask replayFrame = new TimerTask() {
            @Override
            public void run() {
                Thread.currentThread().setName("Replay Cycle " + replayCycle);

                if (replayCycle >= totalReplayCycles) {
                    exitReplay();
                    return;
                }

                HashMap<String, LogDataThread> data = replayLog.getThreadsMap();

                for (String key : data.keySet()) {
                    LogDataThread thread = data.get(key);
                    
                    String bname = thread.getBaseName();
                    
                    for (Replayable replayable : replayables) {
                        if (replayable.getBaseName().equals(bname)) {
                            replayable.feedThread(thread, replayCycle);
                        }
                    }
                }

                replayCycle++;
            }
        };

        replayTimer.scheduleAtFixedRate(replayFrame, Logger.getLogInterval(), Logger.getLogInterval());
    }

    public void exitReplay() {
        for (Replayable replayable : replayables) {
            replayable.setReal(preReplayStates.get(replayable));
            replayable.exitReplay();
        }
        
        replayTimer.cancel();
        replayCycle = 0;
    }
}
