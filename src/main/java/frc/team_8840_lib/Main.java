// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.team_8840_lib;

import edu.wpi.first.wpilibj.RobotBase;
import frc.team_8840_lib.examples.*;

public final class Main {
    private Main() {}

    /**
     * Main initialization function. Don't make any changes here, unless changing Robot#assignListener().
     * This function assigns a listener to the robot, then starts the robot.
     */
    public static void main(String... args) {
        /*
        Assign a listener to the robot.
        You can do any type of EventListener here, but only one can be assigned at a time.
        (This might change in the future, since some listeners might be better suited to be their own class. For now, use only one listener.)
        Check out the frc.robot.examples package for some examples of EventListeners.
        The example provided below is a TankDrive class, and also a class that works for our 2022 robot (team 8840).

        A few examples of assigning a listener:
        <code>
        frc.team_8840_lib.listeners.Robot.assignListener(new TankDrive()); // TankDrive is an example class that extends EventListener
        frc.team_8840_lib.listeners.Robot.assignListener(new 8840_Robot()); // 8840_Robot is a custom class (does not exist here) that extends EventListener
        frc.team_8840_lib.listeners.Robot.assignListener(new Robot()); // frc.robot.Robot is a class that extends EventListener (NOT frc.robot.listeners.Robot)
        </code>
        * */
        frc.team_8840_lib.listeners.Robot.assignListener(new AutonomousExample());

        /**
         * Also assign a log writer such as FileWriter.
         * This is not required, but this is useful for recording data.
         * This can also be called in the constructor of the EventListener.
         * 
         * In this example, it's creating a FileWriter to be writen to the default path.
         */
        frc.team_8840_lib.info.console.Logger.setWriter(new frc.team_8840_lib.info.console.FileWriter("default"));

        //Start the robot - Don't change this line
        RobotBase.startRobot(frc.team_8840_lib.listeners.Robot::new);
    }
}
