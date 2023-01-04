package frc.team_8840_lib.utils.IO;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface IOMethod {
    public String name();
    public IOValue value_type();
    public IOMethodType method_type();
}
