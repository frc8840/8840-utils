package frc.team_8840_lib.various;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.json.JSONObject;

import com.ctre.phoenix.sensors.AbsoluteSensorRange;
import com.ctre.phoenix.sensors.CANCoder;
import com.ctre.phoenix.sensors.CANCoderConfiguration;
import com.ctre.phoenix.sensors.SensorInitializationStrategy;
import com.ctre.phoenix.sensors.SensorTimeBase;
import com.revrobotics.CANSparkMax;
import com.sun.net.httpserver.HttpExchange;

import frc.team_8840_lib.info.console.Logger;
import frc.team_8840_lib.listeners.EventListener;
import frc.team_8840_lib.utils.GamePhase;
import frc.team_8840_lib.utils.http.IP;
import frc.team_8840_lib.utils.http.Route;

public class SwerveSetup extends EventListener {

    private static boolean using = false;
    private static int step = 1;

    private static JSONObject config = new JSONObject();

    public static Route.Resolution nextStep(HttpExchange req, Route.Resolution res) {
        if (!using) {
            return res.json("{ \"message\": \"Swerve Setup is not enabled!\" }").status(404);
        }

        step++;

        JSONObject json = new JSONObject();
        json.put("step", step);

        return res.json(json).status(200);
    }

    public static Route.Resolution handleSwerveSetup(HttpExchange req, Route.Resolution res) {
        if (!using) {
            return res.json("{ \"message\": \"Swerve Setup is not enabled!\" }").status(404);
        }

        if (req.getRequestMethod().equalsIgnoreCase("GET")) {
            return handleSwerveSetupGet(req, res);
        } else if (req.getRequestMethod().equalsIgnoreCase("POST")) {
            return handleSwerveSetupPost(req, res);
        }

        return res.json("{ \"message\": \"Page not found\" }").status(404);
    }
    
    public static Route.Resolution handleSwerveSetupGet(HttpExchange req, Route.Resolution res) {
        JSONObject json = new JSONObject();

        json.put("step", step);
        json.put("teleop", GamePhase.getCurrentPhase().equals(GamePhase.Teleop));

        JSONObject data = new JSONObject();

        json.put("data", data);

        return res.json(json).status(200);
    }

    public static Route.Resolution handleSwerveSetupPost(HttpExchange req, Route.Resolution res) {
        InputStream is = req.getRequestBody();
        String body = new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.joining("\n"));
        
        JSONObject json = new JSONObject(body);

        if (step == 1) {
            /*
             * {
             *  ports: {
             *      frontLeft: {
             *         drive: 0,
             *         turn: 1,
             *         encoder: 5
             *      },
             *      frontRight: {
             *        drive: 2,
             *        turn: 3,
             *        encoder: 6
             *      },
             *      //...
             *  }
             * }
             */

            JSONObject ports = json.getJSONObject("ports");

            config.put("ports", ports);

            return res.json("{ \"message\": \"Step 1 complete\", \"success\": \"true\" }").status(200);
        }

        return res.json("{ \"message\": \"Page not found\" }").status(404);
    }

    CANSparkMax frontLeftDrive;
    CANSparkMax frontLeftTurn;
    CANCoder frontLeftEncoder;

    CANSparkMax frontRightDrive;
    CANSparkMax frontRightTurn;
    CANCoder frontRightEncoder;

    CANSparkMax backLeftDrive;
    CANSparkMax backLeftTurn;
    CANCoder backLeftEncoder;

    CANSparkMax backRightDrive;
    CANSparkMax backRightTurn;
    CANCoder backRightEncoder;

    @Override
    public void robotInit() {
        using = true;

        Logger.Log("Swerve Setup", "Swerve Setup is enabled!");
        Logger.Log("Swerve Setup", "Please open your browser to http://" + IP.getIP() + "/swerve/setup");
    }

    @Override
    public void robotPeriodic() {
        
    }

    @Override
    public void onDisabled() {
        
    }


    @Override
    public void onTeleopEnable() {
        if (config.has("ports") && step == 2) {
            JSONObject ports = config.getJSONObject("ports");

            frontLeftDrive = new CANSparkMax(ports.getJSONObject("frontLeft").getInt("drive"), CANSparkMax.MotorType.kBrushless);
            frontLeftTurn = new CANSparkMax(ports.getJSONObject("frontLeft").getInt("turn"), CANSparkMax.MotorType.kBrushless);
            frontLeftEncoder = new CANCoder(ports.getJSONObject("frontLeft").getInt("encoder"));

            frontRightDrive = new CANSparkMax(ports.getJSONObject("frontRight").getInt("drive"), CANSparkMax.MotorType.kBrushless);
            frontRightTurn = new CANSparkMax(ports.getJSONObject("frontRight").getInt("turn"), CANSparkMax.MotorType.kBrushless);
            frontRightEncoder = new CANCoder(ports.getJSONObject("frontRight").getInt("encoder"));

            backLeftDrive = new CANSparkMax(ports.getJSONObject("backLeft").getInt("drive"), CANSparkMax.MotorType.kBrushless);
            backLeftTurn = new CANSparkMax(ports.getJSONObject("backLeft").getInt("turn"), CANSparkMax.MotorType.kBrushless);
            backLeftEncoder = new CANCoder(ports.getJSONObject("backLeft").getInt("encoder"));

            backRightDrive = new CANSparkMax(ports.getJSONObject("backRight").getInt("drive"), CANSparkMax.MotorType.kBrushless);
            backRightTurn = new CANSparkMax(ports.getJSONObject("backRight").getInt("turn"), CANSparkMax.MotorType.kBrushless);
            backRightEncoder = new CANCoder(ports.getJSONObject("backRight").getInt("encoder"));

            setupMotor(frontLeftDrive, true);
            setupMotor(frontLeftTurn, false);
            setupEncoder(frontLeftEncoder);
            
            setupMotor(frontRightDrive, true);
            setupMotor(frontRightTurn, false);
            setupEncoder(frontRightEncoder);

            setupMotor(backLeftDrive, true);
            setupMotor(backLeftTurn, false);
            setupEncoder(backLeftEncoder);

            setupMotor(backRightDrive, true);
            setupMotor(backRightTurn, false);
            setupEncoder(backRightEncoder);
        }
    }

    private void setupMotor(CANSparkMax motor, boolean isDrive) {
        motor.restoreFactoryDefaults();

        motor.setSmartCurrentLimit(30);
        motor.setSecondaryCurrentLimit(40);

        motor.setOpenLoopRampRate(0.5);
        
        motor.getPIDController().setFeedbackDevice(motor.getEncoder());

        motor.burnFlash();
    }

    private void setupEncoder(CANCoder canCoder) {
        canCoder.configFactoryDefault();

        CANCoderConfiguration encoderConfig = new CANCoderConfiguration();

        encoderConfig.absoluteSensorRange = AbsoluteSensorRange.Unsigned_0_to_360;
        encoderConfig.sensorDirection = false;
        encoderConfig.initializationStrategy = SensorInitializationStrategy.BootToAbsolutePosition;
        encoderConfig.sensorTimeBase = SensorTimeBase.PerSecond;

        canCoder.configAllSettings(encoderConfig);
    }


    @Override
    public void onTeleopPeriodic() {

    }

    @Override
    public void onAutonomousEnable() {

    }

    @Override
    public void onAutonomousPeriodic() {

    }

    @Override
    public void onTestEnable() {

    }

    @Override
    public void onTestPeriodic() {

    }

    @Override
    public void onDisabledPeriodic() {

    }
    
}
