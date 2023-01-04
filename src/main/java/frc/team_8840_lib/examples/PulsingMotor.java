package frc.team_8840_lib.examples;

import frc.team_8840_lib.controllers.ControllerGroup;
import frc.team_8840_lib.info.time.TimeKeeper;
import frc.team_8840_lib.listeners.EventListener;
import frc.team_8840_lib.utils.GamePhase;
import frc.team_8840_lib.utils.controllers.SCType;

public class PulsingMotor extends EventListener {

    private ControllerGroup motor;

    final double divisor = 5;

    @Override
    public void robotInit() {
        motor = ControllerGroup.createSC(0, SCType.PWM_SparkMax).evolve("Motor");
    }

    @Override
    public void robotPeriodic() {
        
    }

    @Override
    public void onAutonomousEnable() {
        
    }

    @Override
    public void onAutonomousPeriodic() {
        double currentTime = TimeKeeper.getInstance().get(GamePhase.Autonomous.getTimerName());

        motor.setSpeed(Math.sin(currentTime / divisor));
    }

    @Override
    public void onTeleopEnable() {
        
    }

    @Override
    public void onTeleopPeriodic() {
        
    }

    @Override
    public void onTestEnable() {
        
    }

    @Override
    public void onTestPeriodic() {
        
    }

    @Override
    public void onDisabled() {
        
    }

    @Override
    public void onDisabledPeriodic() {
        
    }
    
}
