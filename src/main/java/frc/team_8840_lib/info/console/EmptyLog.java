package frc.team_8840_lib.info.console;

import frc.team_8840_lib.utils.logging.LogWriter;

public class EmptyLog extends LogWriter {
    @Override
    public void initialize() {
        //Do nothing.
    }

    @Override
    public void saveLine(String line) {
        //Do nothing.
    }

    @Override
    public void saveInfo(String encodedInfo) {
        //Quite literally do nothing.
        
    }

    @Override
    public void close() {
        //That's crazy. Do nothing.
    }
    
}
