package frc.team_8840_lib.utils.IO;

public enum IOPermission {
    READ,
    READ_WRITE,
    NONE;
    
    public String shortName() {
        switch (this) {
            case READ:
                return "r";
            case READ_WRITE:
                return "rw";
            case NONE:
            default:
                return "";
        }
    }
}
