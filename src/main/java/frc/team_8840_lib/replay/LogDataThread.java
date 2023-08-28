package frc.team_8840_lib.replay;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import frc.team_8840_lib.info.console.Logger;
import frc.team_8840_lib.info.console.Logger.LogType;

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
    private LogType dataType;

    private ArrayList<DataCyclePair> rawData = new ArrayList<>();

    public LogDataThread(String name, LogType dataType) {
        this.name = name;
        this.dataType = dataType;

        System.out.println("new data thread " + name + " with datatype " + dataType.toString());
    }

    protected void push(String data, int cycle) {
        rawData.add(new DataCyclePair(cycle, data));
        if (name.equals("ReplayableExample|exampleVariable2")) {
            System.out.println("pushed " + data + " to " + name + " at cycle " + cycle);
        }
    }

    protected void setMethod(Method method, int cycle, Object klass) {
        try {
            method.invoke(klass, getByCycle(cycle));
        } catch (IllegalAccessException | InvocationTargetException e) {
            Logger.Log("Replayable " + getBaseName() + "/" + getSpecificName(), "Issue with setting the method variable!");
            e.printStackTrace();
        }
    }

    protected void setField(Field field, int cycle, Object klass) {
        boolean isAccessible = field.isAccessible();
        field.setAccessible(true);

        try {
            field.set(klass, getByCycle(cycle));
        } catch (IllegalArgumentException | IllegalAccessException e) {
            Logger.Log("Replayable " + getBaseName() + "/" + getSpecificName(), "Issue with setting the field variable!");
            e.printStackTrace();
        }

        field.setAccessible(isAccessible);
    }

    public String getSpecificName() {
        return name.substring(name.indexOf("|") + 1);
    }

    public String getBaseName() {
        return name.substring(0, name.indexOf("|"));
    }

    public String getAsString(int index) {
        return rawData.get(index).rawData;
    }

    public double getAsDouble(int index) {
        return Double.parseDouble(rawData.get(index).rawData);
    }

    public int getAsInt(int index) {
        return Integer.parseInt(rawData.get(index).rawData);
    }

    public boolean getAsBoolean(int index) {
        return Boolean.parseBoolean(rawData.get(index).rawData);
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
            case STRING:
                return getAsString(index);
            case DOUBLE:
                return getAsDouble(index);
            case INT:
                return getAsInt(index);
            case BOOLEAN:
                return getAsBoolean(index);
            case STRING_ARRAY:
                return getAsStringArray(index);
            case DOUBLE_ARRAY:
                return getAsDoubleArray(index);
            case BYTE_ARRAY:
                return getAsByteArray(index);
            default:
                return null;
        }
    }

    public Object getByCycle(int cycle) {
        if (cycle < rawData.get(0).cycle) {
            return getByIndex(0);
        }

        int i = 0;
        for (DataCyclePair pair : rawData) {
            if (pair.cycle == cycle) {
                return getByIndex(i);
            } else if (pair.cycle > cycle) {
                //Fill in the gaps (since data is only sent when it changes)
                return getByIndex(i - 1);
            }

            i++;
        }

        if (cycle > rawData.get(rawData.size() - 1).cycle) {
            return getByIndex(rawData.size() - 1);
        }

        return null;
    }

    public String getName() {
        return name;
    }
}
