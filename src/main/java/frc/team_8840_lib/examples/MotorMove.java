package frc.team_8840_lib.examples;

import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkBase.IdleMode;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;

import edu.wpi.first.wpilibj.TimedRobot;

public class MotorMove extends TimedRobot {
    /*
     * This is an example of how to control the robot through
     * the base WPILib library. This is the most basic way to
     * control the CANSparkMax and Neo, and is the easiest to
     * understand. This is good for things like the roller,
     * where you want it to be able to go at X% power
     * for as long as you hold the button.
     * 
     * This is also good for testing, as you can
     * just set it to a certain power and see how
     * it reacts.
     * 
     * In Main.java, in the Method "main", you'll just need
     * RobotBase.startRobot(MotorMove::new);
     * to run this.
     */
    

    //Declares a new Spark Max object.
    private CANSparkMax motor;
    
    @Override
    public void robotInit() {
        /*
            Creates a new Spark Max object.
            The first parameter is the CAN ID of the Spark Max.
            We're setting it to 33 since that's the ID of the Spark Max
            on the claw/roller, but it can be changed to whatever.
            If you want to change the ID of a Spark Max, you can do so
            through the REV Hardware Client. Just make sure to change
            the ID in the code as well, and make sure it's unique to the 
            other devices on the CAN network. (CANCoders, Spark Maxes, Pigeons, etc).
            The second parameter is the type of motor, in this case
            a NEO brushless motor (kBrushless).
        */
        motor = new CANSparkMax(33, MotorType.kBrushless);
        
        /*
            Resets all settings that might be on the Spark Max.
            This is good b/c it can get rid of anything from testing
            or playing around with it through the USB and
            REV Hardware Client. Also allows us to make sure
            we have the same settings each time.
         */
        motor.restoreFactoryDefaults();

        /*
            Set the current limits to 25amps (smart limit)
            and 30amps (secondary, hard limit).
            Generally a safety precaution, don't want to destroy
            the Spark Maxes by using wayy to much energy.
            This also helps preserve the battery
            with all of the different components running at the same
            time.
        */
        motor.setSmartCurrentLimit(25);
        motor.setSecondaryCurrentLimit(30);

        /*
            Again, another safety precaution.
            This will slow this down from going 0% to 100%
            in an instant, and rather will take a bit longer to
            get there. In this, (in theory) the motor should
            ramp up at a slower rate, taking about .2 of a second
            to get to full throttle. It's hard to predict how exactly
            this will affect the motor, but it's good to play with this
            until you reach a number that you believe is balanced with
            the mechanism. For example, with the arm this was used to slow it down
            along with a few other factors, such as position, intergral, 
            and derivative control (PID control).
        */
        motor.setOpenLoopRampRate(0.2);

        /*
            Makes the motor not freely spin whenever
            the power provided to its movement is 0%.
            This makes the motor feel like there's a
            "brake" on it, as described by the name.
            Of course, the motor can still move in the mode,
            but it's a lot harder to move by hand than
            "coast" mode, where you can freely spin it around
            with ease.
        */
        motor.setIdleMode(IdleMode.kBrake);

        /*
            Technical stuff, makes it pretty much use less of
            the CAN network. Since only a limited amount of data
            can be transmitted on the CAN, it's important to have
            low usages, but we didn't have any problems with usage %
            in 2023. (But future years, maybe.)
        */
        motor.setCANTimeout(20);

        /*
            Sends all of these settings to the Spark Max.
            This is important, because if you don't do this,
            then the Spark Max won't know what to do with the
            settings you just gave it. After this, the Spark Max
            and Neo is all ready to go!
        */
        motor.burnFlash();
    }

    @Override
    public void teleopPeriodic() {
        /*
            Sets the motor to 50% power.
            This is the most basic way to control the motor,
            and is the easiest to understand. It's also the
            most direct way to control the motor, as it's
            just saying "go at X% power" rather than
            "go at X% power, but only for 5 seconds".
            This is good for things like the roller, where
            you want it to be able to go at X% power
            for as long as you hold the button.

            This is also good for testing, as you can
            just set it to a certain power and see how
            it reacts. If it's too fast, you can lower
            the power. If it's too slow, you can increase
            the power. If it's not moving at all, you can
            check the wiring and make sure everything is
            connected properly.

            One mistake I made when I first started was I didn't
            understand how this worked. This tells the motor to keep
            going at 50% power UNTIL you tell it to stop or change speed.
            So, if you go into another game mode such as Autonomous, the
            motor might start spinning again even though you didn't tell it to.
            The way to combat this is through setting it to 0% power at the
            end of the game mode. This will tell the motor to stop, and it
            won't start again until you tell it to.
        */
        motor.set(0.5);
    }

    @Override
    public void disabledInit() {
        /*
            Sets the motor to 0% power.
            This is important, as it tells the motor to stop.
            If you don't do this, the motor will keep going
            at whatever power you last set it to. This is
            important, as it can cause problems in the future
            if you don't do this. For example, if you set the
            motor to 50% power in teleop, and then go into
            autonomous, the motor will still be going at 50%
            power. This can cause problems, as you might not
            want the motor to be going at 50% power in autonomous.
            So, it's important to set the motor to 0% power
            at the end of each game mode.

            The motor will automatically be stopped in disabled anyway
            (this is built in by FIRST), but it's good practice to
            do this anyway. (That's why you'll see arms controlled in PID
            slightly fall down when disabled, as the motor is set to 0% power
            forcefully, removing the PID control.)
        */
        motor.set(0);
    }
}
