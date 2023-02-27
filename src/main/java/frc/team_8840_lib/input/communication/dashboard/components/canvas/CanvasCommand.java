package frc.team_8840_lib.input.communication.dashboard.components.canvas;

import java.util.HashMap;

import frc.team_8840_lib.utils.http.html.EncodingUtil;

public class CanvasCommand {
    private String command;
    private HashMap<String, String> args;

    public CanvasCommand(String command, HashMap<String, String> args) {
        this.command = command;
        this.args = args;
    }

    public String getCommand() {
        return command;
    }

    public HashMap<String, String> getArgs() {
        return args;
    }

    @Override
    public String toString() {
        String str = command;
        for (String arg : args.keySet()) {
            str += " " + EncodingUtil.encodeURIComponent(arg) + "=" + EncodingUtil.encodeURIComponent(args.get(arg));
        }
        return str;
    }
}
