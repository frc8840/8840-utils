package frc.team_8840_lib.input.communication.dashboard.components.canvas;

import java.util.Base64;

import frc.team_8840_lib.utils.http.html.EncodingUtil;

public class CanvasSupplier {
    public static enum Calculation {
        ADD("+"),
        SUBTRACT("-"),
        MULTIPLY("*"),
        DIVIDE("/"),
        MODULO("%"),
        MATHF("mathf"),
        POW("pow"),
        SQRT("sqrt"),
        ATAN2("atan2");

        private String operator;

        private Calculation(String operator) {
            this.operator = operator;
        }

        public String getOperator() {
            return operator;
        }
    }

    public static enum IfOperation {
        EQUAL("="),
        NOT_EQUAL("!"),
        GREATER_THAN(">"),
        LESS_THAN("<"),
        GREATER_THAN_OR_EQUAL("G"),
        LESS_THAN_OR_EQUAL("L");

        private String operator;

        private IfOperation(String operator) {
            this.operator = operator;
        }

        public String getOperator() {
            return operator;
        }
    }

    public static enum Type {
        NT_VALUE,
        CALCULATION,
        PERCENTAGE,
        NUMBER,
        STRING,
        IF_STATEMENT;

        public String encode() {
            switch (this) {
                case NUMBER:
                    return "n";
                case NT_VALUE:
                    return "t";
                case CALCULATION:
                    return "c";
                case PERCENTAGE:
                    return "p";
                case STRING:
                    return "s";
                case IF_STATEMENT:
                    return "i";
            }

            return "";
        }

        public static Type[] STRICT_NUMBERS = new Type[] { NUMBER, CALCULATION, PERCENTAGE };
        public static Type[] NUMBERS = new Type[] { NUMBER, CALCULATION, PERCENTAGE, NT_VALUE };
        public static Type[] LOOSE_NUMBERS = new Type[] { NUMBER, CALCULATION, PERCENTAGE, NT_VALUE, IF_STATEMENT };
        public static Type[] STRICT_STRINGS = new Type[] { STRING };
        public static Type[] STRINGS = new Type[] { STRING, NT_VALUE };
    }

    public static boolean allOfType(Type type, CanvasSupplier... suppliers) {
        for (CanvasSupplier supplier : suppliers) {
            if (supplier.type != type) {
                return false;
            }
        }

        return true;
    }

    public static boolean allOfType(Type[] type, CanvasSupplier... suppliers) {
        for (CanvasSupplier supplier : suppliers) {
            boolean found = false;
            for (Type t : type) {
                if (supplier.type == t) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                return false;
            }
        }

        return true;
    }

    private String value;
    private Type type;

    public CanvasSupplier(String value, Type type) {
        this.value = value;
        this.type = type;
    }

    public CanvasSupplier ifStatement(CanvasSupplier leftHand, CanvasSupplier rightHand, IfOperation ifOperation, CanvasSupplier trueValue) {
        String value = leftHand.toString() + "|" + ifOperation.getOperator() + "|" + rightHand.toString() + "|" + trueValue.toString();

        //Encode value to base64
        value = Base64.getEncoder().encodeToString(value.getBytes());

        return new CanvasSupplier(value, CanvasSupplier.Type.IF_STATEMENT);
    }

    public CanvasSupplier ifElseStatement(CanvasSupplier leftHand, CanvasSupplier rightHand, IfOperation ifOperation, CanvasSupplier trueValue, CanvasSupplier falseValue) {
        String value = leftHand.toString() + "|e" + ifOperation.getOperator() + "|" + rightHand.toString() + "|" + trueValue.toString() + "|" + falseValue.toString();

        //Encode value to base64
        value = Base64.getEncoder().encodeToString(value.getBytes());

        return new CanvasSupplier(value, CanvasSupplier.Type.IF_STATEMENT);
    }

    public CanvasSupplier calc(Calculation operation, CanvasSupplier other) {
        if (!CanvasSupplier.allOfType(CanvasSupplier.Type.LOOSE_NUMBERS, this, other)) {
            throw new IllegalArgumentException("All arguments must be numbers! (operate)");
        }

        String value = this.toString() + "|" + operation.getOperator() + "|" + other.toString();

        //Encode value to base64
        value = Base64.getEncoder().encodeToString(value.getBytes());

        return new CanvasSupplier(value, CanvasSupplier.Type.CALCULATION);
    }

    public CanvasSupplier calc(Calculation operation, String other) {
        if (!CanvasSupplier.allOfType(CanvasSupplier.Type.LOOSE_NUMBERS, this)) {
            throw new IllegalArgumentException("All arguments must be numbers! (operate)");
        }

        if (operation != Calculation.MATHF) {
            throw new IllegalArgumentException("Only mathf() can take a string as an argument! (operate)");
        }

        String value = this.toString() + "|" + operation.getOperator() + "|" + new CanvasSupplier(other, CanvasSupplier.Type.STRING).toString();

        //Encode value to base64
        value = Base64.getEncoder().encodeToString(value.getBytes());

        return new CanvasSupplier(value, CanvasSupplier.Type.CALCULATION);
    }

    public CanvasSupplier add(CanvasSupplier other) {
        return calc(Calculation.ADD, other);
    }

    public CanvasSupplier subtract(CanvasSupplier other) {
        return calc(Calculation.SUBTRACT, other);
    }

    public CanvasSupplier multiply(CanvasSupplier other) {
        return calc(Calculation.MULTIPLY, other);
    }

    public CanvasSupplier divide(CanvasSupplier other) {
        return calc(Calculation.DIVIDE, other);
    }

    public CanvasSupplier cos() {
        return calc(Calculation.MATHF, "cos");
    }

    public CanvasSupplier sin() {
        return calc(Calculation.MATHF, "sin");
    }

    public CanvasSupplier tan() {
        return calc(Calculation.MATHF, "tan");
    }

    @Override
    public String toString() {
        return type.encode() + ":" + EncodingUtil.encodeURIComponent(value);
    }
}
