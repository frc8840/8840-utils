package frc.team_8840_lib.utils.pathplanner;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import frc.team_8840_lib.utils.math.MathUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class TimePoint {
    private float time;

    private float x;
    private float y;
    private float velocity;
    private float angle;
    private float acceleration;
    public boolean driving;

    public TimePoint(float time) {
        this.time = time;
        this.x = 0;
        this.y = 0;
        this.velocity = 0;
        this.angle = 0;
        this.acceleration = 0;
    }

    public TimePoint() {
        this.time = 0;
        this.x = 0;
        this.y = 0;
        this.velocity = 0;
        this.angle = 0;
        this.acceleration = 0;
    }

    public Pose2d getPose() {
        return new Pose2d(MathUtils.inchesToMeters(x), MathUtils.inchesToMeters(y), new Rotation2d(angle));
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getAngle() {
        return angle;
    }

    public TimePoint parseFromJSON(JSONArray timepoint) {
        /**
         * All coordinates are in inches.
         * All angles are in radians.
         * All velocities are in inches per second.
        * Example of first timepoint (general):
        * {
        *   type: GENERAL,
        *   time,
        *   data: {
        *      time,
        *      driving: true/false,
        *      position: {
        *           x,
        *           y
        *      },
        *   }
        * }
        *
        * if driving is true, then a driving object exists in the timepoint array. Example of driving object:
        * {
        *   type: DRIVING,
        *   time,
        *   data: {
        *       x,
        *       y,
        *       velocity,
         *      angle,
        *   }
        * }
        *
        * */

        JSONObject general = timepoint.getJSONObject(0);
        JSONObject data = general.getJSONObject("data");
        this.time = data.getFloat("time");
        this.driving = data.getBoolean("driving");

        int indexOfDrivingEvent = -1;
        for (int i = 0; i < timepoint.length(); i++) {
            JSONObject event = timepoint.getJSONObject(i);
            if (event.getString("type").equals("drive")) {
                indexOfDrivingEvent = i;
                break;
            }
        }

        if (this.driving && indexOfDrivingEvent > -1) {
            JSONObject drivingEvent = timepoint.getJSONObject(indexOfDrivingEvent);
            JSONObject drivingData = drivingEvent.getJSONObject("data");
            this.x = drivingData.getFloat("x");
            this.y = drivingData.getFloat("y");
            try {
                this.velocity = drivingData.getFloat("velocity");
            } catch (Exception e) {
                this.velocity = 0;
            }
            try {
                this.angle = drivingData.getFloat("angle");
            } catch (Exception e) {
                this.angle = 0;
            }
            //need to implement acceleration later, but it's not a priority
            //this.acceleration = drivingData.getFloat("acceleration");
        } else {
            JSONObject position = data.getJSONObject("position");
            this.x = position.getFloat("x");
            this.y = position.getFloat("y");
        }

        return this;
    }
}