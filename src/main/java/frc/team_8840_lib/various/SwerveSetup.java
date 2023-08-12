package frc.team_8840_lib.various;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.TimerTask;
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
import frc.team_8840_lib.listeners.Robot;
import frc.team_8840_lib.utils.GamePhase;
import frc.team_8840_lib.utils.http.IP;
import frc.team_8840_lib.utils.http.Route;

public class SwerveSetup extends EventListener {

    private static boolean using = false;

    public static boolean isEnabled() {
        return using;
    }

    private static int step = 1;

    private static boolean testingPorts = false;
    private static int testingSide = 0; //0 for front-left, 1 for front-right, 2 for back-left, 3 for back-right
    private static int testingMotor = 0; //0 for drive, 1 for turn
    private static String testingMotorError = "";
    
    private static boolean startEncoderTurnTest = false;

    private static JSONObject config = new JSONObject();

    public static Route.Resolution nextStep(HttpExchange req, Route.Resolution res) {
        if (!using) {
            return res.json("{ \"message\": \"Swerve Setup is not enabled!\" }").status(404);
        }

        step++;

        if (step == 5) {
            startEncoderTurnTest = true;
        }

        Logger.Log("Moving to step " + step + " of Swerve Setup!");

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

        if (step == 5) {
            if (config.has("direction_test")) {
                data.put("finished_test", config.getJSONObject("direction_test").getBoolean("finished"));
                data.put("inversed_encoder", config.getJSONObject("direction_test").getBoolean("inversed_encoder"));
            } else {
                data.put("finished_test", false);
            }
        }

        if (step == 6) {
            data.put("offsets", config.get("offsets"));
        }

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
        } else if (step == 3) {
            if (json.has("confirm")) {
                testingPorts = true;

                Logger.Log("Starting to test ports!");

                JSONObject beginTestJSON = new JSONObject();

                beginTestJSON.put("message", "Testing ports...");
                beginTestJSON.put("testing", "Front Left Drive");

                return res.json(beginTestJSON).status(200);
            } else if (json.has("userResponse")) {
                boolean wasCorrect = json.getBoolean("userResponse");
                
                JSONObject testingJson = new JSONObject();
                                
                if (wasCorrect) {

                    if (testingMotorError.length() > 0) {
                        testingJson.put("continue", false);
                        testingJson.put("error", testingMotorError);
                        testingPorts = false;
                        testingSide = 0;
                        testingMotor = 0;
                        testingMotorError = "";
                        config.remove("ports");

                        step = 1;
                        
                        return res.json(testingJson).status(200);
                    }
                    
                    if (testingMotor == 1) {
                        testingSide++;
                        testingMotor = 0;
                    } else {
                        testingMotor++;
                    }

                    testingJson.put("continue", true);

                    if (testingSide == 4) {
                        testingPorts = false;

                        testingSide = 0;
                        testingMotor = 0;

                        testingJson.put("finished", true);
                        
                        return res.json(testingJson).status(200);
                    }

                    String testing = "";

                    if (testingSide == 0) {
                        testing = "Front Left";
                    } else if (testingSide == 1) {
                        testing = "Front Right";
                    } else if (testingSide == 2) {
                        testing = "Back Left";
                    } else if (testingSide == 3) {
                        testing = "Back Right";
                    }

                    if (testingMotor == 0) {
                        testing += " Drive";
                    } else {
                        testing += " Turn";
                    }

                    Logger.Log("Swerve Setup", "Testing: " + testing);

                    testingJson.put("testing", testing);

                    return res.json(testingJson).status(200);
                } else {
                    testingJson.put("continue", false);
                    if (testingMotorError.length() == 0) {
                        testingMotorError = "The motor did not move! Please find the correct port of the CAN Spark Max.";
                    }
                    testingJson.put("error", testingMotorError);

                    testingPorts = false;
                    testingSide = 0;
                    testingMotor = 0;
                    testingMotorError = "";

                    config.remove("ports");

                    step = 1;

                    return res.json(testingJson).status(200);
                }

            }
        } else if (step == 8) {
            if (json.getBoolean("redo")) {
                step = 6;
            }
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
        
        Robot.getInstance().subscribeFixedPhase(this::onFixedTeleop, GamePhase.Teleop);
    }

    private double startRotation = 0;
    private int encoderSameDirection = 0;
    private int encoderOppositeDirection = 0;

    @Override
    public void robotPeriodic() {
        if (step == 5) {
            if (startEncoderTurnTest) {
                startRotation = frontLeftTurn.getEncoder().getPosition();

                encoderSameDirection = 0;
                encoderOppositeDirection = 0;

                config.remove("direction_test");

                startEncoderTurnTest = false;
            }

            if (Math.abs(startRotation - frontLeftTurn.getEncoder().getPosition()) < 20 && !config.has("direction_test")) {
                if (Math.abs(frontLeftTurn.getEncoder().getVelocity()) > 0.1) {
                    if (frontLeftTurn.getEncoder().getVelocity() > 0 && frontLeftEncoder.getVelocity() > 0) {
                        encoderSameDirection++;
                    } else if (frontLeftTurn.getEncoder().getVelocity() < 0 && frontLeftEncoder.getVelocity() < 0) {
                        encoderSameDirection++;
                    } else {
                        encoderOppositeDirection++;
                    }
                }
            } else {
                JSONObject directionTest = new JSONObject();

                directionTest.put("encoder_inversed", encoderOppositeDirection > encoderSameDirection);
                directionTest.put("finished_test", true);

                if (encoderOppositeDirection > encoderSameDirection) {
                    //reverse all the encoders
                    setupEncoder(frontLeftEncoder, true);
                    setupEncoder(frontRightEncoder, true);
                    setupEncoder(backLeftEncoder, true);
                    setupEncoder(backRightEncoder, true);
                }

                config.put("direction_test", directionTest);
            }
        } else if (step == 6) {
            JSONObject offsets = new JSONObject();

            offsets.put("topLeft", frontLeftEncoder.getPosition());
            offsets.put("topRight", frontRightEncoder.getPosition());
            offsets.put("bottomLeft", backLeftEncoder.getPosition());
            offsets.put("bottomRight", backRightEncoder.getPosition());

            config.put("offsets", offsets);
        }
    }

    @Override
    public void onDisabled() {
        
    }


    @Override
    public void onTeleopEnable() {
        //2 or 3 just in case the request is somehow faster.
        if (config.has("ports") && step == 2 || step == 3) {
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

    private int testCounter = 0;
    private double testTotalMotorMovement = 0;
    private double testTotalEncoderMovement = 0;

    private String lastTestCombo = "";

    public void onFixedTeleop() {
        if (step == 3 && testingPorts) {
            CANSparkMax motor = getMotorFromNumbers();
            CANCoder encoder = getEncoderFromNumbers();

            if (lastTestCombo != testingSide + "," + testingMotor) {
                //stop the last motor
                if (lastTestCombo != "") {
                    CANSparkMax lastMotor = getMotorFromNumbers(
                        (int) Integer.parseInt(lastTestCombo.split(",")[0]), 
                        (int) Integer.parseInt(lastTestCombo.split(",")[1])
                    );

                    lastMotor.set(0);
                }

                lastTestCombo = testingSide + "," + testingMotor;
                testTotalMotorMovement = 0;
                testTotalEncoderMovement = 0;
                testCounter = 0;
            }

            if (motor != null && encoder != null) {
                motor.set(0.3);

                if (testingMotor == 1) {
                    //If the counter has been going for more than a second but there has been no movement,
                    //then there is an error.
                    if (testCounter > 32 && Math.abs(testTotalEncoderMovement / testCounter) < 0.1) {
                        if (Math.abs(testTotalMotorMovement / testCounter) < 0.1) {
                            testingMotorError = "Motor is not moving!";
                            throw new RuntimeException("Motor is not moving!");
                        } else {
                            testingMotorError = "Encoder is not updating!";
                            throw new RuntimeException("Encoder is not updating!");
                        }
                    }

                    if (testCounter >= 64) {
                        motor.set(0);
                    }
                } else {
                    if (testCounter > 32) {
                        if (Math.abs(testTotalMotorMovement / testCounter) < 0.1) {
                            testingMotorError = "Motor is not moving!";
                            throw new RuntimeException("Motor is not moving!");
                        }
                    }

                    if (testCounter >= 64) {
                        motor.set(0);
                    }
                }

                if (testCounter < 64) {
                    testTotalMotorMovement += motor.getEncoder().getVelocity();
                    testTotalEncoderMovement += encoder.getVelocity();
                    testCounter++;
                }
            } else {
                testingMotorError = "Motor or encoder is null!";
                throw new RuntimeException("Motor is null!");
            }
        }
    }

    private CANCoder getEncoderFromNumbers() {
        if (testingSide == 0) {
            return frontLeftEncoder;
        } else if (testingSide == 1) {
            return frontRightEncoder;
        } else if (testingSide == 2) {
            return backLeftEncoder;
        } else if (testingSide == 3) {
            return backRightEncoder;
        }

        return null;
    }

    private CANSparkMax getMotorFromNumbers() {
        return getMotorFromNumbers(testingSide, testingMotor);
    }

    private CANSparkMax getMotorFromNumbers(int testingSide, int testingMotor) {
        if (testingSide == 0) {
            return testingMotor == 0 ? frontLeftDrive : frontLeftTurn;
        } else if (testingSide == 1) {
            return testingMotor == 0 ? frontRightDrive : frontRightTurn;
        } else if (testingSide == 2) {
            return testingMotor == 0 ? backLeftDrive : backLeftTurn;
        } else if (testingSide == 3) {
            return testingMotor == 0 ? backRightDrive : backRightTurn;
        }

        return null;
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
        setupEncoder(canCoder, false);
    }

    private void setupEncoder(CANCoder canCoder, boolean inverted) {
        canCoder.configFactoryDefault();

        CANCoderConfiguration encoderConfig = new CANCoderConfiguration();

        encoderConfig.absoluteSensorRange = AbsoluteSensorRange.Unsigned_0_to_360;
        encoderConfig.sensorDirection = inverted;
        encoderConfig.initializationStrategy = SensorInitializationStrategy.BootToAbsolutePosition;
        encoderConfig.sensorTimeBase = SensorTimeBase.PerSecond;

        canCoder.configAllSettings(encoderConfig);
    }

    @Override
    public void onTeleopPeriodic() {}

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
