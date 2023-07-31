package frc.team_8840_lib.controllers;

import edu.wpi.first.math.kinematics.SwerveDriveOdometry;
import frc.team_8840_lib.info.console.Logger;
import frc.team_8840_lib.replay.Replayable;
import frc.team_8840_lib.utils.IO.IOAccess;
import frc.team_8840_lib.utils.IO.IOPermission;
import frc.team_8840_lib.utils.async.Promise;
import frc.team_8840_lib.utils.controllers.swerve.ModuleConfig;
import frc.team_8840_lib.utils.controllers.swerve.SwerveSettings;

@IOAccess(IOPermission.READ_WRITE)
public class SwerveDrive extends Replayable {

    private SwerveDriveOdometry m_odometry;
    private SwerveSettings m_settings;

    private SwerveModule m_frontLeft;
    private SwerveModule m_frontRight;
    private SwerveModule m_backLeft;
    private SwerveModule m_backRight;

    private boolean m_isInitialized;

    public SwerveDrive(ModuleConfig frontLeft, ModuleConfig frontRight, ModuleConfig backLeft, ModuleConfig backRight, SwerveSettings settings) {
        super();
        m_settings = settings;

        m_frontLeft = new SwerveModule(m_settings, frontLeft, SwerveModule.Position.FRONT_LEFT);
        m_frontRight = new SwerveModule(m_settings, frontRight, SwerveModule.Position.FRONT_RIGHT);
        m_backLeft = new SwerveModule(m_settings, backLeft, SwerveModule.Position.BACK_LEFT);
        m_backRight = new SwerveModule(m_settings, backRight, SwerveModule.Position.BACK_RIGHT);

        int startTime = (int) System.currentTimeMillis();

        new Promise((res, rej) -> {
            Promise.WaitThen(() -> {
                return m_frontLeft.initalized() && m_frontRight.initalized() && m_backLeft.initalized() && m_backRight.initalized();
            }, res, rej, 10);
        }).then((res, rej) -> {
            int endTime = (int) System.currentTimeMillis();

            Logger.Log(getBaseName(), "Initialized in " + (endTime - startTime) + "ms");

            m_isInitialized = true;

            res.run();
        }).catch_err((err) -> {
            err.printStackTrace();
            Logger.Log(getBaseName(), "Failed to initialize!");
        });
    }

    public boolean isReady() {
        return this.m_isInitialized;
    }


    @Override
    public String getBaseName() {
        return "SwerveDrive";
    }

    @Override
    public void replayInit() {

    }

    @Override
    public void exitReplay() {

    }
    
}
