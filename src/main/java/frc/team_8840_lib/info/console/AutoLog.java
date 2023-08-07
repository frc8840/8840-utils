package frc.team_8840_lib.info.console;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import frc.team_8840_lib.info.console.Logger.LogType;

@Retention(RetentionPolicy.RUNTIME)
public @interface AutoLog {
    public String name();

    public String replaylink() default "";
}

