package frc.team_8840_lib.input.communication.dashboard.components.canvas;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;

import frc.team_8840_lib.input.communication.CommunicationManager;
import frc.team_8840_lib.input.communication.dashboard.components.DashboardComponent;
import frc.team_8840_lib.input.communication.dashboard.components.canvas.CanvasSupplier.Type;
import frc.team_8840_lib.utils.http.html.EncodingUtil;

public class CanvasDisplay extends DashboardComponent {
    private int width;
    private int height;
    private String name;

    private ArrayList<CanvasCommand> drawCommands = new ArrayList<>();

    public CanvasDisplay(String name, int width, int height) {
        this.width = width;
        this.height = height;
        this.name = name;
    }

    public void draw() {
        clearCanvas();

        //Draw a red rectangle
        fillStyle(s("red"));
        rect(
            n(10), //X coordinate (number: 10)
            n(10), //Y coordinate (number: 10)
            //EXAMPLE 1: Calculate width (max_width - (number: 20)) using the CanvasDisplay#calc() method.
            calc(MAX(), CanvasSupplier.Calculation.SUBTRACT, n(20)), 
            //EXAMPLE 2: Calculate height (max_height - (number: 20)) using the CanvasSupplier#calc() method.
            MAX().subtract(n(20)),
            //Fill the rectangle.
            true
        );
    }

    public CanvasSupplier n(double value) {
        return new CanvasSupplier(String.valueOf(value), CanvasSupplier.Type.NUMBER);
    }

    public CanvasSupplier percent(double value) {
        return new CanvasSupplier(String.valueOf(value), CanvasSupplier.Type.PERCENTAGE);
    }

    public CanvasSupplier nt_value(String key) {
        return new CanvasSupplier(key, CanvasSupplier.Type.NT_VALUE);
    }

    public CanvasSupplier s(String value) {
        return new CanvasSupplier(value, CanvasSupplier.Type.STRING);
    }

    public CanvasSupplier calc(CanvasSupplier leftSide, CanvasSupplier.Calculation operator, CanvasSupplier rightSide) {
        return leftSide.calc(operator, rightSide);
    }

    public CanvasSupplier mathf(CanvasSupplier leftSide, String func) {
        if (!CanvasSupplier.allOfType(CanvasSupplier.Type.NUMBERS, leftSide)) {
            throw new IllegalArgumentException("All arguments must be numbers! (mathf)");
        }

        String value = leftSide.toString() + "|mathf|" + new CanvasSupplier(func, Type.STRING).toString();

        //Encode value to base64
        value = Base64.getEncoder().encodeToString(value.getBytes());

        return new CanvasSupplier(value, CanvasSupplier.Type.CALCULATION);
    }

    public CanvasSupplier MAX() {
        return percent(100);
    }

    public CanvasSupplier mouseX() {
        return nt_value(getCustomBaseNTPath() + this.name + getMouseXPath());
    }

    public CanvasSupplier mouseY() {
        return nt_value(getCustomBaseNTPath() + this.name + getMouseYPath());
    }

    //Colors
    public void fillStyle(CanvasSupplier color) {
        if (!CanvasSupplier.allOfType(Type.STRINGS, color)) {
            throw new IllegalArgumentException("All arguments must be strings! (color)");
        }

        HashMap<String, String> args = new HashMap<>();

        args.put("color", color.toString());
        
        addCommand(
            new CanvasCommand("fillStyle", args)
        );
    }

    public void strokeStyle(CanvasSupplier color) {
        if (!CanvasSupplier.allOfType(Type.STRINGS, color)) {
            throw new IllegalArgumentException("All arguments must be strings! (color)");
        }

        HashMap<String, String> args = new HashMap<>();

        args.put("color", color.toString());
        
        addCommand(
            new CanvasCommand("strokeStyle", args)
        );
    }

    public void lineWidth(CanvasSupplier width) {
        if (!CanvasSupplier.allOfType(Type.NUMBERS, width)) {
            throw new IllegalArgumentException("All arguments must be numbers! (width)");
        }

        HashMap<String, String> args = new HashMap<>();

        args.put("width", width.toString());
        
        addCommand(
            new CanvasCommand("lineWidth", args)
        );
    }

    //Operations
    public void clearCanvas() {
        HashMap<String, String> args = new HashMap<>();

        args.put("x", n(0).toString());
        args.put("y", n(0).toString());
        args.put("width", MAX().toString());
        args.put("height", MAX().toString());

        addCommand(
            new CanvasCommand("clearRect", args)
        );
    }

    //Shapes
    public void line(CanvasSupplier x1, CanvasSupplier y1, CanvasSupplier x2, CanvasSupplier y2) {
        if (!CanvasSupplier.allOfType(Type.NUMBERS, x1, y1, x2, y2)) {
            throw new IllegalArgumentException("All arguments must be numbers! (x1, y1, x2, y2)");
        }

        HashMap<String, String> args = new HashMap<>();

        args.put("x1", x1.toString());
        args.put("y1", y1.toString());
        args.put("x2", x2.toString());
        args.put("y2", y2.toString());
        
        addCommand(
            new CanvasCommand("line", args)
        );
    }

    public void rect(CanvasSupplier x, CanvasSupplier y, CanvasSupplier width, CanvasSupplier height, boolean fill) {
        if (!CanvasSupplier.allOfType(Type.NUMBERS, x, y, width, height)) {
            throw new IllegalArgumentException("All arguments must be numbers! (x, y, width, height)");
        }

        HashMap<String, String> args = new HashMap<>();

        args.put("x", x.toString());
        args.put("y", y.toString());
        args.put("width", width.toString());
        args.put("height", height.toString());
        
        addCommand(
            new CanvasCommand(fill ? "fillRect" : "rect", args)
        );
    }

    public void circle(CanvasSupplier x, CanvasSupplier y, CanvasSupplier radius, boolean filled) {
        if (!CanvasSupplier.allOfType(Type.NUMBERS, x, y, radius)) {
            throw new IllegalArgumentException("All arguments must be numbers! (x, y, radius)");
        }

        HashMap<String, String> args = new HashMap<>();

        args.put("x", x.toString());
        args.put("y", y.toString());
        args.put("radius", radius.toString());
        args.put("filled", filled ? "true" : "false");
        
        addCommand(
            new CanvasCommand("circle", args)
        );
    }

    public void addCommand(CanvasCommand command) {
        drawCommands.add(command);
    }

    public String getTag() {
        //Convert drawCommands to a string
        String drawCommandsString = "";
        for (CanvasCommand command : drawCommands) {
            drawCommandsString += command.toString() + ";";
        }

        //Then convert the whole thing to base64
        String base64 = Base64.getEncoder().encodeToString(drawCommandsString.getBytes());

        String encodedBase64 = EncodingUtil.encodeURIComponent(base64);

        return "SUB: CANVAS{width=" + width + ", height=" + height + ", name=" + name + ", cmds=" + encodedBase64 + "}";
    }

    public String getInnerHTML() {
        return "";
    }

    public static String getCustomBaseNTPath() {
        return CommunicationManager.base() + "/custom_component/";
    }

    public static String getMouseXPath() {
        return "/canvas/mouse_x";
    }

    public static String getMouseYPath() {
        return "/canvas/mouse_y";
    }

    public static String getMouseDownPath() {
        return "/canvas/mouse_down";
    }
}
