package frc.team_8840_lib.pathing;

import java.util.HashMap;

import frc.team_8840_lib.input.communication.CommunicationManager;
import frc.team_8840_lib.pathing.PathConjugate.ConjugateType;

public class PathPlanner {
    public static class AUTOS {
        /*
            Auto Locations for the 2022 season.
            Since Charged Up has pretty clear locations where 
            the robots will be placed, we'll use those.
            These are in relation to the 8840-app path-planner view of the field.
            Lower is right next to the wall, and upper is next to the substation.
            Middle is between the two.
        */
        public static class ChargedUp {
            public static final String RED_LOWER = "red lower";
            public static final String RED_MIDDLE = "red middle";
            public static final String RED_UPPER = "red upper";
            public static final String BLUE_LOWER = "blue lower";
            public static final String BLUE_MIDDLE = "blue middle";
            public static final String BLUE_UPPER = "blue upper";
        }
    }

    private static HashMap<String, PathPlanner> autos = new HashMap<>();
    private static String selectedAuto = null;

    public static void addAuto(String name, PathPlanner auto) {
        autos.put(name, auto);
    }

    public static boolean selectedAuto() {
        return selectedAuto != null;
    }

    public static PathPlanner getSelectedAuto() {
        if (selectedAuto == null) {
            return new PathPlanner();
        }

        return autos.get(selectedAuto);
    }

    public static void selectAuto(String name) {
        if (!autos.containsKey(name)) {
            throw new IllegalArgumentException("Auto " + name + " does not exist!");
        }
        selectedAuto = name;
    }

    private PathConjugate[] conjugates;
    private int index = -1;

    public PathPlanner(PathConjugate... conjugates) {
        this.conjugates = conjugates;
    }

    public void start() {
        index = 0;

        CommunicationManager.getInstance()
            .updateInfo("auton", "i", index)
            .updateInfo("auton", "a", true);
    }

    public void next() {
        index++;

        if (!finished()) {
            conjugates[index].start();
        } else {
            CommunicationManager.getInstance()
                .updateInfo("auton", "a", false);
        }

        CommunicationManager.getInstance()
            .updateInfo("auton", "i", index);
    }

    public ConjugateType getCurrentType() {
        if (conjugates.length == 0) return null;
        if (index < 0) return null;

        return conjugates[index].getType();
    }

    public boolean finished() {
        return index >= conjugates.length;
    }

    public void fixedExecute() {
        if (finished()) return;
        if (index < 0) return;

        conjugates[index].update();

        if (conjugates[index].isFinished()) {
            next();
        }
    }

    public PathConjugate current() {
        return conjugates[index];
    }

    public PathConjugate getFirstMovement() {
        //We'll do a linear search for the first movement.
        //Not really efficient since it's O(n), but it's not like we'll have a lot of conjugates... right?
        for (PathConjugate conjugate : conjugates) {
            if (conjugate.getType() == ConjugateType.Path) {
                return conjugate;
            }
        }

        return null;
    }

    public PathConjugate[] getAllConjugates() {
        return conjugates;
    }

    public int getSelectedAutoIndex() {
        return index;
    }

    public static String[] getAutoNames() {
        return autos.keySet().toArray(new String[0]);
    }

    public static boolean validAuto(String name) {
        return autos.containsKey(name);
    }

    public static String getSelectedAutoName() {
        return selectedAuto;
    }
}
