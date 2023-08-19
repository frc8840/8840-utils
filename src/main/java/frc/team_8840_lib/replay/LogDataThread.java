package frc.team_8840_lib.replay;

import java.util.ArrayList;

public class LogDataThread {

    private class DataCyclePair {
        public int cycle;
        public String rawData;

        public DataCyclePair(int cycle, String rawData) {
            this.cycle = cycle;
            this.rawData = rawData;
        }
    }

    protected String name;
    private ReplayLog.DataType dataType;

    private ArrayList<DataCyclePair> rawData = new ArrayList<>();

    public LogDataThread(String name, ReplayLog.DataType dataType) {
        this.name = name;
        this.dataType = dataType;
    }

    protected void push(String data, int cycle) {
        rawData.add(new DataCyclePair(cycle, data));
    }

    public String getAsString(int index) {
        return rawData.get(index).rawData;
    }

    public double getAsDouble(int index) {
        return Double.parseDouble(rawData.get(index).rawData);
    }

    public String[] getAsStringArray(int index) {
        return rawData.get(index).rawData.substring(1, rawData.get(index).rawData.length() - 1).split(", ");
    }

    public double[] getAsDoubleArray(int index) {
        String[] stringArray = getAsStringArray(index);
        double[] doubleArray = new double[stringArray.length];
        for (int i = 0; i < stringArray.length; i++) {
            doubleArray[i] = Double.parseDouble(stringArray[i]);
        }
        return doubleArray;
    }

    public byte[] getAsByteArray(int index) {
        return rawData.get(index).rawData.getBytes();
    }

    public Object getByIndex(int index) {
        switch (dataType) {
            case String:
                return getAsString(index);
            case Double:
                return getAsDouble(index);
            case String_Array:
                return getAsStringArray(index);
            case Double_Array:
                return getAsDoubleArray(index);
            case Byte_Array:
                return getAsByteArray(index);
            default:
                return null;
        }
    }

    public Object getByCycle(int cycle) {
        for (DataCyclePair pair : rawData) {
            if (pair.cycle == cycle) {
                return getByIndex(rawData.indexOf(pair));
            } else if (pair.cycle < cycle) {
                //Fill in the gaps (since data is only sent when it changes)
                return getByIndex(rawData.indexOf(pair) - 1);
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }
}
