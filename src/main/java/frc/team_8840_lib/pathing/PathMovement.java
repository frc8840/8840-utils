package frc.team_8840_lib.pathing;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.json.JSONObject;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.team_8840_lib.info.console.Logger;
import frc.team_8840_lib.input.communication.CommunicationManager;
import frc.team_8840_lib.listeners.FrameworkUtil;
import frc.team_8840_lib.utils.pathplanner.TimePoint;

public class PathMovement extends CommandBase {
    private TimePoint[] timePoints;

    private Rotation2d angleGoal = Rotation2d.fromDegrees(0);
    private boolean hasAngleGoal = false;

    private double atTime = 0;
    private int atIndex = 0;

    private boolean finished = false;

    public static PathMovement loadPath(Path path) throws IOException {
        File file = path.toFile();

        if (!file.exists()) {
            throw new IOException("Did not find path file at " + path.toString());
        }

        //Load the path from the file
        String fileContents;

        //Read file
        try {
            fileContents = new String(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            e.printStackTrace();
            Logger.Log("There was an error while reading the file. Did the file get corrupted?");
        
            throw new IOException("There was an error while reading the file. Did the file get corrupted?");
        }

        JSONObject fileJson = new JSONObject(fileContents);

        TimePoint[] points = CommunicationManager.getInstance().readAndParsePath(fileJson, true);

        if (points == null || points.length <= 0) {
            throw new IOException("The path file was empty!");
        }

        return new PathMovement(points);
    }

    public PathMovement(TimePoint[] points) {
        timePoints = points;
    }

    public PathMovement() {
        timePoints = new TimePoint[0];
    }

    public void updateTimePoints(TimePoint[] points) {
        timePoints = points;
    }

    public PathMovement addRotationGoal(Rotation2d angleGoal) {
        this.angleGoal = angleGoal;
        hasAngleGoal = true;

        return this;
    }

    public boolean hasRotationGoal() {
        return hasAngleGoal;
    }

    public Rotation2d getRotationGoal() {
        return angleGoal;
    }

    @Override
    public void initialize() {
        if (timePoints.length == 0) {
            finished = true;
            return;
        }

        reset();
        moveToNext();
    }

    public void reset() {
        //For some reason the first point is null, so just skip it
        atIndex = 0;
        atTime = -FrameworkUtil.DELTA_TIME;

        finished = false;
    }

    public Pose2d getLastPose() {
        if (atIndex <= 1) {
            //System.out.println(timePoints[1].getPose().getX() + ", " + timePoints[1].getPose().getY());
            return timePoints[1].getPose();
        }
        return timePoints[atIndex - 1].getPose();
    }

    public Pose2d getFinalPose() {
        return timePoints[timePoints.length - 1].getPose();
    }

    public Pose2d getPose() {
        return timePoints[atIndex].getPose();
    }

    public double getAtTime() {
        return atTime;
    }

    public Pose2d moveToNext() {
        if (atIndex < timePoints.length - 1) {
            atIndex++;
            atTime += FrameworkUtil.DELTA_TIME;
        } else {
            finished = true;
        }

        Pose2d pathVector = new Pose2d(
                Units.inchesToMeters(timePoints[atIndex].getX()),
                Units.inchesToMeters(timePoints[atIndex].getY()),
                new Rotation2d(timePoints[atIndex].getAngle())
        );

        return pathVector;
    }

    public TimePoint[] getAllPoints() {
        return timePoints;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }
}
