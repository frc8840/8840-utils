package frc.team_8840_lib.replay;

import java.util.ArrayList;
import java.util.HashMap;

public class ReplayLog {
    private class Types {
        public final static String _String = "s";
        public final static String _Double = "d";
        public final static String String_Array = "S";
        public final static String Double_Array = "D";
        public final static String Byte_Array = "B";
    }

    private class NameTypePair {
        public String name;
        public DataType type;

        public NameTypePair(String name, DataType type) {
            this.name = name;
            this.type = type;
        }
    }

    public enum DataType {
        String,
        Double,
        String_Array,
        Double_Array,
        Byte_Array;
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
        if (line.startsWith("a")) {
            int reference = Integer.getInteger(line.substring(1, line.indexOf("/")));
            String value = line.substring(line.indexOf("/") + 1);
            this.analyzeDatapoint(reference, value);
        } else if (line.startsWith("a")) {
            String name = line.substring(1, line.indexOf("/") - 1);
            String type = line.substring(line.indexOf("/") - 1, line.indexOf("/"));
            int reference = Integer.getInteger(line.substring(line.indexOf("/") + 1));
            this.analyzeDeclaration(name, type, reference);
        } else if (line.startsWith("ALC")) {
            boolean isEarlyCycle = line.contains("(s)");
            this.cycles++;
            if (isEarlyCycle) this.earlyCycles++;
        } else {
            this.messages.add(line);
        }
    }

    private DataType getValueType(String value) {
        if (value == Types._String) {
            return DataType.String;
        } else if (value == Types._Double) {
            return DataType.Double;
        } else if (value == Types.String_Array) {
            return DataType.String_Array;
        } else if (value == Types.Double_Array) {
            return DataType.Double_Array;
        } else if (value == Types.Byte_Array) {
            return DataType.Byte_Array;
        } else {
            return null;
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

        DataType dataType = this.getValueType(type);

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
