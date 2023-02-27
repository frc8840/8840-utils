package frc.team_8840_lib.utils.math.units;

public class Unit {
    public static enum Type {
        PIXELS(0),
        INCHES(0.393701),
        FEET( .0328084),
        METERS(0.01),
        CENTIMETERS(1),
        MILLIMETERS(10),
        YARDS(0.0109361);

        private double conversionFactor;

        private Type (double conversionFactor) {
            this.conversionFactor = conversionFactor;
        }

        public double getConversionFactor() {
            return conversionFactor;
        }
    }

    private double rawValue;
    private double inCentimeters;
    private Type type;

    public Unit (double value, Type type) {
        this.type = type;
        this.set(value);
    }

    public void set(double value) {
        this.rawValue = value;
        this.inCentimeters = this.rawValue / this.type.conversionFactor;
    }

    public void set(double value, Type type) {
        this.rawValue = new Unit(value, type).get(this.type);
        this.inCentimeters = this.rawValue / this.type.conversionFactor;
    }

    public double to(Type type) {
        return this.get(type);
    }

    public double get(Type type) {
        return this.inCentimeters * type.conversionFactor;
    }

    public double get() {
        return this.get(this.type);
    }

    public double getCU(double customUnit, Type fromType) {
        return this.get(fromType) * customUnit;
    }

    public Unit add(Unit value) {
        Unit unit = new Unit(this.get(), this.type);
        unit.set(unit.get(this.type) + value.get(this.type), this.type);
        return unit;
    }
}
