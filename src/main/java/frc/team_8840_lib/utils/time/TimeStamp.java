package frc.team_8840_lib.utils.time;

public enum TimeStamp {
    RealTime, // Real Time is the real time, i.e what's actually happening
    GameTime, // Game Time is the time the robot is in the current phase of the match, e.g "x seconds into <Autonomous, Teleop, etc.>"
    RobotTime, // Robot Time is the time since the robot code started.
    BothRealAndGameTime, // Both Real Time and Game Time
    All, // All of the above
    None;
}
