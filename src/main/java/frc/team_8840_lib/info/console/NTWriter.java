package frc.team_8840_lib.info.console;

import frc.team_8840_lib.input.communication.CommunicationManager;
import frc.team_8840_lib.utils.logging.LogWriter;

public class NTWriter extends LogWriter {

    public NTWriter() {}

    private int messagesSent = 0;
    private int infoSent = 0;

    @Override
    public void initialize() {
        CommunicationManager.getInstance().updateInfo("logger", "msg", "");
        CommunicationManager.getInstance().updateInfo("logger", "info", "");

        CommunicationManager.getInstance().updateInfo("logger", "on", 0);
        CommunicationManager.getInstance().updateInfo("logger", "info_on", 0);

        messagesSent = 0;
        infoSent = 0;
    }

    @Override
    public void saveLine(String line) {
        messagesSent += 1;
        CommunicationManager.getInstance().updateInfo("logger", "msg", line);
        CommunicationManager.getInstance().updateInfo("logger", "on", messagesSent);
    }

    @Override
    public void saveInfo(String encodedInfo) {
        infoSent += 1;
        CommunicationManager.getInstance().updateInfo("logger", "info", encodedInfo);
        CommunicationManager.getInstance().updateInfo("logger", "info_on", infoSent);
    }

    @Override
    public void close() {
        CommunicationManager.getInstance().updateInfo("logger", "on", -1);
        CommunicationManager.getInstance().updateInfo("logger", "info_on", -1);
    }
}
