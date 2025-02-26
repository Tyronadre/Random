package data;

import java.util.ArrayList;
import java.util.List;

// Modellklasse: speichert Kontrollpunkte, Grad und Knotenvector und enthält Methoden zur Aktualisierung.
public class NURBSModel {
    private List<ControlPoint> controlPoints;
    private int degree;
    private double[] knots;

    public NURBSModel() {
        // Standard: Viertelkreis mit quadratischer NURBS (Grad = 2) und 3 Kontrollpunkten
        degree = 2;
        controlPoints = new ArrayList<>();
        controlPoints.add(new ControlPoint(1.0, 0.0, 1.0));
        controlPoints.add(new ControlPoint(Math.cos(Math.toRadians(45)), Math.sin(Math.toRadians(45)), Math.cos(Math.toRadians(45))));
        controlPoints.add(new ControlPoint(0.0, 1.0, 1.0));
        knots = generateUniformKnotVector(controlPoints.size(), degree);
    }

    public List<ControlPoint> getControlPoints() {
        return controlPoints;
    }

    public void setControlPoints(List<ControlPoint> cps) {
        this.controlPoints = cps;
    }

    public int getDegree() {
        return degree;
    }

    public void setDegree(int d) {
        this.degree = d;
    }

    public double[] getKnots() {
        return knots;
    }

    public void setKnots(double[] knots) {
        this.knots = knots;
    }

    // Generiert einen uniformen, geklammerten Knotenvector
    public double[] generateUniformKnotVector(int numControlPoints, int degree) {
        int n = numControlPoints - 1;
        int m = n + degree + 1;
        double[] kv = new double[m + 1];
        for (int i = 0; i <= m; i++) {
            if (i <= degree)
                kv[i] = 0.0;
            else if (i >= m - degree)
                kv[i] = 1.0;
            else
                kv[i] = (double) (i - degree) / (m - 2 * degree);
        }
        return kv;
    }

    // Führt Knoteneinfügung an Parameterwert u durch.
    // Dabei werden neue Kontrollpunkte in homogenen Koordinaten berechnet, sodass die Kurve unverändert bleibt.
    public void insertKnot(double u) throws IllegalArgumentException {
        double[] U = knots;
        int p = degree;
        int n = controlPoints.size() - 1;
        int m = n + p + 1;

        // Prüfe, ob u in der gültigen Domäne liegt.
        if (u < U[p] || u > U[m - p])
            throw new IllegalArgumentException("Knot value out of range.");

        // Finde k, sodass U[k] <= u < U[k+1]
        int k = p;
        while (k < m && !(U[k] <= u && u < U[k + 1])) {
            k++;
        }
        // Falls u == U[m-p], setze k = m - p - 1
        if (k >= m) k = m - p - 1;

        // Erstelle neue Knotensequenz: Länge m+2
        double[] newKnots = new double[U.length + 1];
        if (k + 1 >= 0) System.arraycopy(U, 0, newKnots, 0, k + 1);
        newKnots[k + 1] = u;
        if (U.length - (k + 1) >= 0) System.arraycopy(U, k + 1, newKnots, k + 1 + 1, U.length - (k + 1));

        // Knoteneinfügealgorithmus: arbeite in homogenen Koordinaten
        List<ControlPoint> newCP = new ArrayList<>();
        // Für i = 0 bis k-p: unverändert übernehmen.
        for (int i = 0; i < k - p + 1; i++) {
            newCP.add(controlPoints.get(i));
        }
        // Berechne neue Kontrollpunkte für i = k-p+1 bis k:
        for (int i = k - p + 1; i <= k; i++) {
            double alpha = (u - U[i]) / (U[i + p] - U[i]);
            // Umrechnung in homogene Koordinaten: H(P) = (w*x, w*y, w)
            ControlPoint Pi_1 = controlPoints.get(i - 1);
            ControlPoint Pi = controlPoints.get(i);
            double X1 = Pi_1.weight * Pi_1.x;
            double Y1 = Pi_1.weight * Pi_1.y;
            double W1 = Pi_1.weight;
            double X2 = Pi.weight * Pi.x;
            double Y2 = Pi.weight * Pi.y;
            double W2 = Pi.weight;
            double X = (1 - alpha) * X1 + alpha * X2;
            double Y = (1 - alpha) * Y1 + alpha * Y2;
            double W = (1 - alpha) * W1 + alpha * W2;
            newCP.add(new ControlPoint(X / W, Y / W, W));
        }
        // Für i = k+1 bis n+1: übernehmen (shifted um 1)
        for (int i = k + 1; i <= n + 1; i++) {
            newCP.add(controlPoints.get(i - 1));
        }

        // Aktualisiere Modell
        controlPoints = newCP;
        knots = newKnots;
    }
}
