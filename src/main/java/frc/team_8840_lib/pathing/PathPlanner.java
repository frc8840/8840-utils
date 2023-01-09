package frc.team_8840_lib.pathing;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import frc.team_8840_lib.input.communication.CommunicationManager;
import frc.team_8840_lib.listeners.FrameworkUtil;
import frc.team_8840_lib.listeners.Robot;
import frc.team_8840_lib.utils.math.MathUtils;
import frc.team_8840_lib.utils.pathplanner.TimePoint;

public class PathPlanner {
    private TimePoint[] timePoints;

    private double atTime = 0;
    private int atIndex = 0;

    private boolean finished = false;

    public PathPlanner(TimePoint[] points) {
        timePoints = points;
        this.init();
    }

    public PathPlanner() {
        timePoints = new TimePoint[0];
        this.init();
    }

    public void updateTimePoints(TimePoint[] points) {
        timePoints = points;
        this.init();
    }

    private void init() {
        CommunicationManager.getInstance().createField();

        if (timePoints.length == 0) return;

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
            System.out.println(timePoints[1].getPose().getX() + ", " + timePoints[1].getPose().getY());
            return timePoints[1].getPose();
        }
        return timePoints[atIndex - 1].getPose();
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
                MathUtils.inchesToMeters(timePoints[atIndex].getX()),
                MathUtils.inchesToMeters(timePoints[atIndex].getY()),
                new Rotation2d(timePoints[atIndex].getAngle())
        );

        CommunicationManager.getInstance().updateRobotPose(pathVector);

        //field2d.getObject("index" + atIndex).setPose(pathVector);

        return pathVector;
    }

    public boolean finished() {
        return finished;
    }
}
