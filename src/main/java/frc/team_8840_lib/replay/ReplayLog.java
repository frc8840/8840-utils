package frc.team_8840_lib.replay;

import java.util.ArrayList;
import java.util.HashMap;

import frc.team_8840_lib.info.console.Logger.LogType;

public class ReplayLog {
    private class NameTypePair {
        public String name;
        public LogType type;

        public NameTypePair(String name, LogType type) {
            this.name = name;
            this.type = type;
        }
    }

    private ArrayList<String> rawLines = new ArrayList<>();

    private HashMap<Integer, NameTypePair> references = new HashMap<>();

    private HashMap<String, LogDataThread> info = new HashMap<>();

    private ArrayList<String> messages = new ArrayList<>();

    private int cycles;
    private int earlyCycles;

    public ReplayLog(String log) {
        String[] lines = log.split("\n");
        for (String line : lines) {
            this.rawLines.add(line);
        }

        for (String line : rawLines) {
            this.analyzeLine(line);
        }
    }

    public int getCycles() {
        return cycles;
    }

    public int getEarlyCycles() {
        return earlyCycles;
    }

    public int getDataCycles() {
        return cycles - earlyCycles;
    }
    
    private void analyzeLine(String line) {
        if (line.startsWith("d")) {
            String rawReference = line.substring(1, line.indexOf("/")).trim();
            int reference = Integer.valueOf(rawReference);
            String value = line.substring(line.indexOf("/") + 1);
            
            this.analyzeDatapoint(reference, value);
        } else if (line.startsWith("a")) {
            String name = line.substring(1, line.indexOf("/") - 1);
            String type = line.substring(line.indexOf("/") - 1, line.indexOf("/"));
            String rawReference = line.substring(line.indexOf("/") + 1).trim();
            int reference = Integer.valueOf(rawReference);

            this.analyzeDeclaration(name, type, reference);
        } else if (line.startsWith("ALC")) {
            boolean isEarlyCycle = line.contains("(s)");
            this.cycles++;
            if (isEarlyCycle) this.earlyCycles++;
        } else {
            this.messages.add(line);
        }
    }

    private void analyzeDatapoint(int reference, String value) {
        if (!this.references.containsKey(reference)) {
            throw new RuntimeException("Reference " + reference + " does not exist!");
        }

        String name = this.references.get(reference).name;

        this.info.get(name).push(value, this.cycles);
    }

    private void analyzeDeclaration(String name, String type, int reference) {
        if (this.references.containsKey(reference)) {
            throw new RuntimeException("Reference " + reference + " already exists!");
        }

        LogType dataType = LogType.fromSmallString(type);

        this.references.put(reference, new NameTypePair(name, dataType));

        this.info.put(
            name, 
            new LogDataThread(name, dataType)
        );
    }

    public LogDataThread[] getThreads() {
        return this.info.values().toArray(new LogDataThread[0]);
    }

    public HashMap<String, LogDataThread> getThreadsMap() {
        return this.info;
    }
}
