package frc.team_8840_lib.till;

import java.util.HashMap;
import java.util.regex.Pattern;

import frc.team_8840_lib.controllers.ControllerGroup;
import frc.team_8840_lib.info.console.Logger;
import frc.team_8840_lib.input.controls.GameController;
import frc.team_8840_lib.utils.controllers.SCType;
import frc.team_8840_lib.utils.controls.Axis;

public class TillHandler {
    private HashMap<String, ControllerGroup> controllerGroups = new HashMap<>();

    @SuppressWarnings("deprecation")
    public void handleLine(String line_) {
        String line = line_.trim();

        if (line.startsWith("gamecontroller_register")) {
            String insideParentheses = getInsideParentheses(line);

            if (!insideParentheses.startsWith("gamecontroller")) {
                throw new IllegalArgumentException("The first argument of gamecontroller_register must be a gamecontroller!");
            }

            String rawArgs = insideParentheses.substring("gamecontroller[".length()).replace("]", "");

            String[] args = rawArgs.split(",");

            int port = Integer.parseInt(args[0].trim());
            String type = args[1].trim();

            GameController.Type controllerType = GameController.Type.valueOf(type);

            if (controllerType == null) {
                if (type == "Playstation") {
                    throw new IllegalArgumentException("Playstation controllers are not supported! Will be added in the future.");
                } else if (type == "Logitech") {
                    controllerType = GameController.Type.Joystick;
                } else if (type == "Generic") {
                    controllerType = GameController.Type.Custom;
                } else {
                    throw new IllegalArgumentException("The second argument of gamecontroller_register must be a valid gamecontroller type!");
                }
            }

            GameController.expectController(port, controllerType);
            return;
        } else if (line.startsWith("speedcontroller_register")) {
            String insideParentheses = getInsideParentheses(line);

            if (!insideParentheses.startsWith("speedcontroller")) {
                throw new IllegalArgumentException("The first argument of speedcontroller_register must be a speedcontroller!");
            }

            String rawArgs = insideParentheses.substring("speedcontroller[".length()).replace("]", "");

            String[] args = rawArgs.split(",");

            int port = Integer.parseInt(args[0].trim());
            String type = args[1].trim().replaceAll(" ", "_");

            SCType controllerType;
            try {
                controllerType = SCType.valueOf(type);
            } catch (IllegalArgumentException e) {
                controllerType = null;
            }
            
            SCType alternate;
            try {
               alternate = SCType.valueOf("PWM_" + type);
            } catch (IllegalArgumentException e) {
                alternate = null;
            }

            if (controllerType == null && alternate == null) {
                if (type == "SparkMax" || type == "Spark_Max") {
                    controllerType = SCType.PWM_SparkMax;
                } else {
                    throw new IllegalArgumentException("The second argument of speedcontroller_register must be a valid speedcontroller type!");
                }
            }

            if (controllerType == null) {
                controllerType = alternate;
            }

            Logger.Log("Till", "Successfully registered speedcontroller " + port + " as " + controllerType + "!");

            controllerGroups.put("controller-" + port, ControllerGroup.createSC(port, controllerType).evolve("till-" + type + "-" + port));

            return;
        } else if (line.startsWith("speedcontroller_move")) {
            String insideParentheses = getInsideParentheses(line);

            String[] args = insideParentheses.split(",");

            String speedController = args[0].trim();
            double speed = evaluate(args[1].trim());

            if (!speedController.startsWith("speedcontroller")) {
                throw new IllegalArgumentException("The first argument of speedcontroller_move must be a speedcontroller!");
            }

            int port = Integer.parseInt(speedController.substring("speedcontroller[".length()).replace("]", ""));

            ControllerGroup group = controllerGroups.get("controller-" + port);

            if (group == null) {
                throw new IllegalArgumentException("The speedcontroller " + speedController + " is not registered!");
            }

            group.setSpeed(speed);

            return;
        } else if (line.startsWith("LOG")) {
            String insideParentheses = getInsideParentheses(line);

            if (insideParentheses.startsWith("\"")) {
                insideParentheses = insideParentheses.substring(1);

                if (insideParentheses.endsWith("\"")) {
                    insideParentheses = insideParentheses.substring(0, insideParentheses.length() - 1);
                }

                Logger.Log("Till", insideParentheses);
            }

            return;
        }
    }

    private Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");

    public double evaluate(String expression) {
        if (pattern.matcher(expression).matches()) {
            return Double.parseDouble(expression);
        }

        if (expression.startsWith("(") && expression.endsWith(")")) {
            return evaluate(expression.substring(1, expression.length() - 1));
        }
        
        if (expression.startsWith("calc") || expression.startsWith("mf")) {
            String insideParentheses = getInsideParentheses(expression);

            int level = 0;

            int indexOfFirstSplit = -1;
            int indexOfSecondSplit = -1;

            for (int i = 0; i < insideParentheses.length(); i++) {
                char c = insideParentheses.charAt(i);

                if (c == '(') {
                    level++;
                } else if (c == ')') {
                    level--;
                }

                if (level == 0) {
                    if (c == '|') {
                        if (indexOfFirstSplit == -1) {
                            indexOfFirstSplit = i;
                        } else {
                            indexOfSecondSplit = i;
                        }
                    }
                }
            }

            if (indexOfFirstSplit == -1 || indexOfSecondSplit == -1) {
                throw new IllegalArgumentException("The calc function must have 3 arguments!");
            }

            //first arg is the left side of the equation, the second arg is the operator, and the third arg is the right side of the equation
            String leftSide = insideParentheses.substring(0, indexOfFirstSplit);
            String operator = insideParentheses.substring(indexOfFirstSplit + 1, indexOfSecondSplit);
            String rightSide = insideParentheses.substring(indexOfSecondSplit + 1);

            double leftSideValue = evaluate(leftSide);
            double rightSideValue = evaluate(rightSide);

            if (expression.startsWith("mf")) {
                if (operator.equals("pow")) {
                    return Math.pow(leftSideValue, rightSideValue);
                }
            } else if (expression.startsWith("calc")) {
                if (operator.equals("ADD")) {
                    return leftSideValue + rightSideValue;
                } else if (operator.equals("MINUS")) {
                    return leftSideValue - rightSideValue;
                } else if (operator.equals("MULTIPLY")) {
                    return leftSideValue * rightSideValue;
                } else if (operator.equals("DIVIDE")) {
                    return leftSideValue / rightSideValue;
                } else {
                    throw new IllegalArgumentException("The operator must be ADD, MINUS, MULTIPLY, or DIVIDE!");
                }
            }
        } else if (expression.startsWith("gamecontroller_get_axis")) {
            String insideParentheses = getInsideParentheses(expression);
            
            String[] args = insideParentheses.split(",");

            String gameController = args[0].trim();
            String axis = args[1].trim();
            
            if (!gameController.startsWith("gamecontroller")) {
                throw new IllegalArgumentException("The first argument of gamecontroller_get_axis must be a gamecontroller!");
            }

            int port = Integer.parseInt(gameController.substring("gamecontroller[".length()).replace("]", ""));

            Axis axisType = Axis.valueOf(axis);

            if (axisType == null) {
                throw new IllegalArgumentException("The second argument of gamecontroller_get_axis must be a valid axis!");
            }

            return GameController.get(port).getAxis(axisType);
        }

        return 0;
    }

    public String getInsideParentheses(String content) {
        //Get the content inside the parentheses
        int indexOfFirstParentheses = content.indexOf("(");

        int level = 0;

        String insideParentheses = "";

        for (int i = indexOfFirstParentheses; i < content.length(); i++) {
            char c = content.charAt(i);

            if (c == '(') {
                level++;
            } else if (c == ')') {
                level--;
            }

            if (level == 0) {
                insideParentheses = content.substring(indexOfFirstParentheses + 1, i);
                break;
            }
        }

        return insideParentheses;
    }
}
