package data;

public class ControlPoint {
    public double x, y, z, weight;

    public ControlPoint(double x, double y, double weight) {
        this(x, y, 1.0, weight);
    }

    public ControlPoint(double x, double y, double z, double weight) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.weight = weight;
    }

    public double[] getArray() {
        return new double[] {x, y, z, weight};
    }
}
