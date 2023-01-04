package frc.team_8840_lib.utils.IO;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface IOAccess {
    public IOPermission value();
}
