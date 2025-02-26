package data;

// Repräsentation eines Kontrollpunktes (x, y, weight)
public class ControlPoint {
    public double x, y, weight;

    public ControlPoint(double x, double y, double weight) {
        this.x = x;
        this.y = y;
        this.weight = weight;
    }

    @Override
    public String toString() {
        return String.format("(%.3f, %.3f, %.3f)", x, y, weight);
    }
}
