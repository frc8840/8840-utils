package frc.team_8840_lib.utils.logging;

public abstract class LogWriter {
    private String[] args;
    
    public LogWriter(String ...args) {
        this.args = args;
    };

    public abstract void initialize();

    public abstract void saveLine(String line);

    public abstract void saveInfo(String encodedInfo);

    public abstract void close();
}
