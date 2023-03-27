package frc.team_8840_lib.pathing;

import java.io.IOException;
import java.nio.file.Path;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.team_8840_lib.info.console.Logger;
import frc.team_8840_lib.input.communication.CommunicationManager;
import frc.team_8840_lib.utils.interfaces.Callback;
import frc.team_8840_lib.utils.pathplanner.TimePoint;

public class PathConjugate {
    public static enum ConjugateType {
        Path, Action;
    }

    public static PathConjugate loadPathFromFile(Path path, PathMovement defaultPath) {
        PathMovement movement;
        try {
            movement = PathMovement.loadPath(path);
        } catch (IOException e) {
            movement = defaultPath;
            e.printStackTrace();
        }
        return new PathConjugate(movement);
    }

    public static PathConjugate loadPathFromFile(Path path) throws IOException {
        PathMovement movement = PathMovement.loadPath(path);
        return new PathConjugate(movement);
    }

    private static boolean waitForPathCalled = false;

    public static PathConjugate waitForPath() {
        if (waitForPathCalled) {
            throw new IllegalStateException("waitForPath() can only be called once!");
        }

        waitForPathCalled = true;

        PathMovement movement = new PathMovement();

        CommunicationManager.getInstance().legacyWaitForAutonomousPath((TimePoint[] points) -> {
            movement.updateTimePoints(points);
            Logger.Log("[PathConjugate] Path loaded from REST API with " + points.length + " points.");
        });

        return new PathConjugate(movement);
    }

    public static PathConjugate runOnce(Callback callback) {
        return new PathConjugate(new CommandBase() {
            @Override
            public void initialize() {
                callback.run();
            }

            @Override
            public void execute() {
            }

            @Override
            public boolean isFinished() {
                return true;
            }
        });
    }

    public static PathConjugate command(CommandBase command) {
        return new PathConjugate(command);
    }

    private ConjugateType type;
    private CommandBase command;

    private boolean finished = false;

    public PathConjugate(CommandBase command) {
        if (command instanceof PathMovement) {
            type = ConjugateType.Path;
        } else {
            type = ConjugateType.Action;
        }

        this.command = command;
    }

    public void start() {
        command.initialize();
    }

    public void update() {
        command.execute();

        if (command.isFinished()) {
            command.end(true);

            finished = true;
        }
    }

    public PathConjugate addRotationGoal(Rotation2d angle) {
        if (type != ConjugateType.Path) {
            throw new IllegalStateException("Cannot add rotation goal to non-path command!");
        }

        ((PathMovement) command).addRotationGoal(angle);

        return this;
    }

    public PathMovement getPath() {
        if (type == ConjugateType.Path) {
            return (PathMovement) command;
        }

        return null;
    }

    public ConjugateType getType() {
        return type;
    }

    public boolean isFinished() {
        return finished;
    }

    public String getName() {
        if (type == ConjugateType.Path) {
            return ((PathMovement) command).getName();
        } else {
            return command.getName();
        }
    }
}
